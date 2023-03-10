/**
 *  Water Heater Schedule (Child)
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
definition(
    name: "Water Heater Schedule (Child)",
    namespace: "jangellx",
    author: "Joe Angell",
    description: "Child app representing a specific water heater schedule",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    parent: "jangellx:Water Heater Schedule")


preferences {
    page name: "mainPage", title: "Water Heater Schedule", install: false, uninstall: true, nextPage: "namePage"
	page name: "namePage", title: "Water Heater Schedule", install: true,  uninstall: true
}

// Page for choosing the water heater, on/off state and temperature
def mainPage() {
	dynamicPage(name: "mainPage") {
		section( "Power and Temperature" ) {
			input "power",          "bool",   title: "Turn heater on?",                  required: true,  defaultValue: true
			input "temperature",    "number", title: "Temperature",                      required: false, defaultValue: 120
        }

        section( "Time and Day" ) {
            input "setAtTime", "time", title: "Change the temperature at what time?",                  required: true
            input "days",      "enum", title: "On which days of the week?",            multiple: true, required: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
        }
    }
}

// page for allowing the user to give the automation a custom name
def namePage() {
	if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }

	dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Schedule name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Schedule name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit schedule name", defaultValue: false, required: false, submitOnChange: true
        }
    }
}

// a method that will set the default label of the automation.
// It uses the heater name, scheduled time and temperature to make a name.
def defaultLabel() {
	def label = "Turn " + (power ? "on" : "off")
    if( power )
    	label += " and set to " + temperature + " degrees"

	// Build the days string
	def daysLabel
    if( days.size() == 7 ) {
		daysLabel = "Every Day"
    } else if( days[0] == "Saturday" && days[1] == "Sunday" && days.size() == 2) {
    	daysLabel = "Weekends"
    } else if( days[0] == "Monday" && days[1] == "Tuesday" && days[2] == "Wednesday" && days[3] == "Thursday" && days[4] == "Friday" && days.size() == 5) {
		daysLabel = "Weekdays"
    } else if( days.size() != 7 ) {
    	daysLabel = ""
    	days.each { day ->
        	if( daysLabel.size() != 0 ) {
            	if( days[ days.size() - 1] == day )
                	daysLabel += " and ";
                else
	            	daysLabel += ", "
            }

			daysLabel += day
        }
    }

	def dateAtTime = new Date().parse("yyy-MM-dd'T'HH:mm:ss.SSSZ", setAtTime )
	label + " at " + dateAtTime.format( "hh:mm a", location.timeZone ) + " on " + daysLabel
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

// Turn the water heater on or off and set the temperature.
def scheduledEvent() {
	parent.setWaterHeaterTo( power ? false : true, temperature )
}

// Called by updated() and by the parnet app.
def updateForVacationMode( vacationMode ) {
	log.debug "Vacation mode is " + (vacationMode == true ? "on" : "off") + "; updating \"" + defaultLabel + "\""

	// Cancel any existing cron task
	unschedule( "0 0 2 1/2 * ? *", scheduledEvent );

	// Schedule a new cron job only if vacation mode is off
	if( vacationMode == false )
		schedule( "0 0 2 1/2 * ? *", scheduledEvent );
}

def initialize() {
	updateForVacationMode( parent.getVacationMode() );
}

// TODO: implement event handlers