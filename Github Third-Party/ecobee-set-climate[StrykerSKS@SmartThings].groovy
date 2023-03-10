/**
 *  ecobee Set Climate
 *
 *  Copyright 2014 Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
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
 * N.B. You may want to create multiple instances of this smartapp (and rename them in SmartSetup) for each time
 * you want to set a different Climate at a given day and time during the week.*
 */
definition(
	name: "ecobee Set Climate",
	namespace: "eco-community",
	author: "Yves Racine",
	description: "This SmartApp allows an ecobee user to set a Climate at a given day & time",
	category: "My Apps",
 	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)



preferences {

	page(name: "selectThermostats", title: "Thermostats", install: false, uninstall: true, nextPage: "selectProgram") {
		section("About") {
			paragraph "ecobeeSetClimate, the smartapp that sets your ecobee thermostat to a given climate at a given day & time"
			paragraph "Version 1.1.4" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yracine%40yahoo%2ecom&lc=US&item_name=Maisons%20ecomatiq&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest",
					title:"Paypal donation..."
			paragraph "Copyright©2014 Yves Racine"
				href url:"http://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information..."  
					description: "http://github.com/yracine/device-type.myecobee/blob/master/README.md"
		}
		section("Set the ecobee thermostat(s)") {
			input "ecobee", "device.myEcobeeDevice", title: "Which ecobee thermostat(s)?", multiple: true

		}
		section("Configuration") {
			input "dayOfWeek", "enum",
				title: "Which day of the week?",
				multiple: false,
				metadata: [
					values: [
						'All Week',
						'Monday to Friday',
						'Saturday & Sunday',
						'Monday',
						'Tuesday',
						'Wednesday',
						'Thursday',
						'Friday',
						'Saturday',
						'Sunday'
					]
				]
			input "begintime", "time", title: "Beginning time"
		}

	}
	page(name: "selectProgram", title: "Ecobee Programs", content: "selectProgram")
	page(name: "Notifications", title: "Notifications Options", install: true, uninstall: true) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
        	section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}


def selectProgram() {
	def ecobeePrograms = ecobee.currentClimateList.toString().minus('[').minus(']').tokenize(',')
	log.debug "programs: $ecobeePrograms"


	return dynamicPage(name: "selectProgram", title: "Select Ecobee Program", install: false, uninstall: true, nextPage:
		"Notifications") {
		section("Select Program") {
			input "givenClimate", "enum", title: "Which program?", options: ecobeePrograms, required: true
		}
	}
}



def installed() {
	// subscribe to these events
	initialize()
}

def updated() {
	// we have had an update
	// remove everything and reinstall
	unsubscribe()
	unschedule()    
	initialize()
}

def initialize() {

	log.debug "Scheduling setClimate for day " + dayOfWeek + " at begin time " + begintime
	subscribe(ecobee, "climateList", climateListHandler)

	schedule(begintime, setClimate)
	subscribe(app, setClimateNow)    

}
def climateListHandler(evt) {
	log.debug "thermostat's Climates List: $evt.value, $settings"
}

def setClimateNow(evt) {
	setClimate()
}

def setClimate() {
	def climateName = (givenClimate ?: 'Home').capitalize()


	def doChange = IsRightDayForChange()

	// If we have hit the condition to schedule this then lets do it

	if (doChange == true) {
		log.debug "setClimate, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"

		ecobee.setThisTstatClimate(climateName)
		send("ecobeeSetClimate>set ecobee thermostat(s) to ${climateName} program as requested")
	} else {
		log.debug "climate change to ${climateName} not scheduled for today."
	}
	log.debug "End of Fcn"
}


def IsRightDayForChange() {

	def makeChange = false
	Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
	int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK);

	// Check the condition under which we want this to run now
	// This set allows the most flexibility.
	if (dayOfWeek == 'All Week') {
		makeChange = true
	} else if ((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.MONDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.TUESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.THURSDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') && currentDayOfWeek == Calendar.instance.FRIDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SATURDAY) {
		makeChange = true
	} else if ((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && currentDayOfWeek == Calendar.instance.SUNDAY) {
		makeChange = true
	}


	// some debugging in order to make sure things are working correclty
	log.debug "Calendar DOW: " + currentDayOfWeek
	log.debug "SET DOW: " + dayOfWeek

	return makeChange
}


private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)
	}

	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, msg)
	}

	log.debug msg
}



// catchall
def event(evt) {
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}
