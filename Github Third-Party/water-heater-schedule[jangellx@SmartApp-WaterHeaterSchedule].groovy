/**
 *  Water Heater Schedule
 *
 *  Copyright 2017 Joe Angell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
// Multiple instances of this can be installed, for situations wher eyou have multiple water heaters.
//  Each water heater can have different schedules.
definition(
    name: "Water Heater Schedule",
    namespace: "jangellx",
    author: "Joe Angell",
    description: "Create a schedule for when to turn on and off the hot water heater to conserve energy.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
{
	appSetting "vactionModeHeatedRecently"
    appSetting "childTurnedHeaterOn"
}

preferences {
	// Main page, for accessing Vacation Mode and scheduling
	page name: "mainPage", title: "Water Heater Schedules"

	// Vacation Mode page, which is a sub-page of the Main Page
	page name: "vmPage"
}

// Main page, for accessing vacation mode and adding schedules
def mainPage() {
	def vacationModeLabel = "Vacation mode is " + (settings[ "vacationMode" ] == true ? "ON" : "OFF")

	dynamicPage(name: "mainPage") {
		section {
        	// Vacation Mode Page
        	href name: "vmPage", title: vacationModeLabel, required: false, page: "vmPage"
        }

        section {
        	// Select which water heaters are controlled
            input "waterHeaters", "device.rheemEconetWaterHeater", title: "Controlled Water Heaters", multiple: true, submitOnChange: true
        }

        section( "Schedules" ) {
        	// Individual schedules as child SmartApps
            app name: "waterHeaterScheduleChild", appName: "Water Heater Schedule (Child)", namespace: "jangellx", title: "New Schedule", multiple: true
        }
    }
}

// Vacation Mode page, for turning Vacation Mode on and off
def vmPage() {
	dynamicPage( name: "vmPage", title: "Vacation Mode" ) {
        section {
            input "vacationMode", "bool", title: "Vacation Mode", defaultValue: false
            paragraph "When in vacation mode, all schedules are suspended, and the water heater is only turned on for a couple of hours every two days at 140 degrees."
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "there are ${childApps.size()} child smartapps"

	if( vacationMode == true ) {
    	// Vacation Mode is on; set up cron for every 2 days at 140 degrees from 2 AM to 4 AM
        log.debug( "Vacation mode on" );
        schedule( "0 0 2 1/2 * ? *", vacationMode_TurnOnHeater  );
        schedule( "0 0 4 1/2 * ? *", vacationMode_TurnOffHeater );

		// Turn off the water heaters
		waterHeaters.off

		// Mark that we heated in the last 5 hours, since we just went into vacation mode.
        //  This just keeps us from heating again if we were just turned off
		apiSettings.vactionModeHeatedInLast5Hours = "true";
        apiSettings.childTurnedHeaterOn           = "false";

	} else {
    	// Vacation Mode is off; cancel the cron jobs
        log.debug( "Vacation mode off" );
		unschedule( vacationMode_TurnOn );
		unschedule( vacationMode_TurnOff );
        
        // If it has been long enough since we entered vacation mode or last ran the heater, set the temp to 140 for 2 hours
		if( apiSettings.vactionModeHeatedRecently != "true" ) {
			waterHeaters.on
		    waterHeaters.setHeatingSetpoint( 140 )

			// Add 2 hours to the current time and turn off the heater again after that has passed
			Calendar cal = Calendar.getInstance();
            cal.setTime( Date() );					 // Set to the current time
            cal.add( Calendar.HOUR_OF_DAY, 2 );		 // Add two hours
			runOnce( cal.getTime(), vacationMode_TurnOffHeater )

	        log.debug( "Turning on heater at 140 degrees until ${cal.getTime()}" );
		}
	}

	// Update all the child apps
	childApps.each {child ->
		child.updateForVacationMode( vacationMode );
	}
}

// Preiodic vacation mode event to kill anything in the water.
//  Turn on the heater and set the temperature to 140 degrees.
def vacationMode_TurnOnHeater() {
	waterHeaters.on
    waterHeaters.setHeatingSetpoint( 140 )

    log.debug( "Vacation Mode: turning on heater at 140 degrees for 2 hours" );

	// Clear the recent flag.  If this si truen when the off event is hit, we know that
    //  a child scheduled event turned the heater on, and we don't want to override that.
	apiSettings.vactionModeHeatedRecently = "false"
}

// Turn off the heater off again.
def vacationMode_TurnOffHeater() {
	// If a child turned the heater on, we don't want to turn it off on them
	if( apiSettings.childTurnedHeaterOn == "true" )
    	return

	waterHeaters.setHeatingSetpoint( 130 )
	waterHeaters.off

	// Set this after we shut off the heater (as opposed to turnning it on) to ensure that
    //  it got at least 2 hours of heat
	apiSettings.vactionModeHeatedRecently = "true"
}


// Called by the child application to get the vacation mode
def getVacationMode() {
	vacationMode
}

// Called by the child application when to change its power and temperature.
def setWaterHeaterTo( Boolean power, Number temperature ) {
	if( power ) {
		log.debug "Scheduled power on to ${tempearture}"

    	waterHeaters.on
        waterHeaters.setHeatingSetpoint( temperature );

		apiSettings.vactionModeHeatedRecently = "true"
        apiSettings.childTurnedHeaterOn       = "true"

	} else {
		log.debug "Scheduled power off"

    	waterHeaters.off
    }
}