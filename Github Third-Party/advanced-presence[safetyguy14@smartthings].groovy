/*

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    For a copy of the GNU General Public License, see <http://www.gnu.org/licenses/>.

    Advanced Presence Thermostat Control (Ecobee)
    Utilize all of your smart home information to better control your thermostat

    Note:
    This software is designed to work with the MyEcobee device available at 
    http://www.ecomatiqhomes.com/#!store/tc3yr

	This software is designed to be used with an ecobee schedule in which the user
    is "Home" during the day, it will set an Away and hold command when the occupants
    leave the house.
    
    
 */
 
// body
definition(
	name: "Advanced Presence",
	namespace: "safetyguy14",
	author: "Joshua Mason",
	description: "Advanced Presence",
	category: "My Apps",
)

// application setup, two pages; one for devices and one for software parameters
preferences {
	page(name: "Devices", title: "Select your Inputs/Outputs", 
		install: false, uninstall: true, hideWhenEmpty: true, nextPage: "Settings") {
		section("People") {
			input "people", "capability.presenceSensor", title: "People", multiple: true, required: false
		}
		section("Motion Sensors") {
			input "motions", "capability.motionSensor", title: "Motion Sensor(s)", multiple: true, required: false
		}
		section("Lights") {
			input "switches", "capability.switch", title: "Light(s)", multiple: true, required: false
		}
		section("Thermostat(s)") {
			input "ecobee", "capability.thermostat", title: "Thermostat(s)", multiple: true, required: true
		}
		section("Locks") {
			input "locks", "capability.lock", title: "Lock(s)", required: false, multiple: true
		}
		section("Door(s)") {
			input "garageDoorControls", "capability.garageDoorControl", title: "Garage Door(s)", multiple: true, required: false
			input "doorControls", "capability.doorControl", title: "Door(s)", multiple: true, required: false
			input "contactSensors", "capability.contactSensor", title: "Misc", multiple: true, required: false
		}
	}
	page(name: "Settings", title: "Application Settings", uninstall: true, install: true) {
		section("Not Present debounce timer [default=3 minutes]") {
			input "residentsQuietThreshold", "number", title: "Time in minutes", required: false
		}
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Would you like push notifications", metadata: [values: ["Yes", "No"]], required: false
			input "phone", "phone", title: "Number to send text message to", required: false
		}
		section("Detailed Notifications") {
			input "detailedNotif", "bool", title: "Do you want tons of Notifications?", required: false
		}
		section("What is your People Not Present Climate Name [default=Away]") {
			input "givenClimateNameNP", "text", title: "Climate Name", required: false
		}
		/*section("What is your People Present Climate Name [default=Home]") {
			input "givenClimateNameP", "text", title: "Climate Name", required: false
		}*/  //potential future use
        section("want to turn on mega-debugging?") {
        	input "debugMode", "bool", title: "Debug Mode?", required: false
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	initialize()

}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
	initialize()
}

private initialize() {
	// using people to detect presence and trigger an away action
    // when a person leaves or a home action when a person arrives
    if (people != null && people != ""){
		subscribe(people, "presence", presence)
	}
    
    // using motion sensors to detect presence
	if (motions != null && motions != "") {
		subscribe(motions, "motion", motionEvtHandler)
	}
    
    // using contact sensors on doors to trigger a delayed Away action
    // and as a method of presence
	if (contactSensors != null && contactSensors != "") {
		subscribe(contactSensors, "contact", contactEvtHandler)
	}
    
    // user can call both the away and home action directly from app
	subscribe(app, appTouch)    
}

/*
	Event Handlers
*/

//event handler for user app request
def appTouch(evt) {
	if (debugMode) {
    	log.debug ("App request: location.mode= $location.mode, running Away and Home routines")
	}
    //def msg 
    //msg = ecobee.getThermostatInfo(ecobee)
    //log.debug ("$ecobee.getThermostatInfo() ***")
    //log msg
    //ecobee.each {
	//	msg = it.getSupportedAttributes()
	//}

	takeActionsAway()
    takeActionsHome()
}

// event handler for motion sensor events
def motionEvtHandler(evt) {
	if (evt.value == "active") {
		state.lastIntroductionMotion = now() //I don't know what this does
		if(debugMode) {
        	log.debug "Motion at home... ${evt.device}"
		}
		//set the thermostat to resume normal schedule
		takeActionsHome()
	}
}

def contactEvtHandler(evt) {
	if (evt.value == "open") {
    	state.lastIntroductionMotion = now()//I don't know what this does
		if(debugMode) {
			log.debug "Somebody opened a door... ${evt.device}"
        }
        //set the thermostat to resume normal schedule
        takeActionsHome()
        runIn(60*5, takeActionsAway)
    }
}

/*notes: invokes residentsHaveBeenQuiet when the presence event is detected as "not present"
and takes action if no motion is detected
*/
def presence(evt) {
	if(debugMode) {
		log.debug "$evt.name: $evt.value"
    }
	if (evt.value == "not present") {
		def person = getPerson(evt)
		if (detailedNotif) {
			send("AwayFromHome> ${person.displayName} is no longer home")
		}
		if(debugMode) {
			log.debug "checking if everyone is away  and quiet at home"
        }
		if (residentsHaveBeenQuiet() && doorsHaveNotOpened() ) { 
			if (everyoneIsAway()) {
				if (detailedNotif) {
					send("AwayFromHome>Quiet at home...")
				}
				runIn(delay, "takeActionsAway")
			} else {
				if(debugMode) {
                	log.debug "Not everyone is away, doing nothing"
                }
				if (detailedNotif) {
					send("AwayFromHome>Not everyone is away, doing nothing..")
				}
			}
		} else {
            if(debugMode) {	
            	log.debug "Motion still detected, no action taken"
            }
			if (detailedNotif) {
				send("Away> Motion still detected")
			}
		}
	} else {
        if(debugMode) {
        	log.debug "Someone is present! Taking action"
        }
		//set the thermostat to the people present value if it is not already by calling the function
        takeActionsHome()
	}
}

/*
	Functions
*/
def takeActionsAway() {
	Integer thresholdMinutes = 2
	Integer delay = 60 * thresholdMinutes
	def msg
	
	//  Making sure everybody is away and no motion at home
	if (everyoneIsAway() && residentsHaveBeenQuiet() && doorsHaveNotOpened() ) {
		msg = "Nobody is at home, and it's quiet, about to take actions"
		if (detailedNotif) {
			log.info msg
			send(msg)
		}
		
		if ((givenClimateNameNP != null) && (givenClimateNameNP != "")) {
			ecobee.each {
				it.setClimate('', givenClimateNameNP) // Set to the climateName for Not Present
			}
			msg = "${ecobee} set to $givenClimateNameNP"
			log.debug msg
			send(msg)
			
		} else {
			// Set to the default value "Away"
			ecobee.each {
				it.setClimate('', "Away")
			}
			msg = "${ecobee} Set to Away"
			log.debug msg
			send(msg)
		}

 		msg = "${ecobee} thermostats' settings updated"
		if (detailedNotif ) {
			log.info msg
			send(msg)
		}

/* 		locks?.lock() // lock the locks 		
		msg = "AwayFromHome>Locked the locks"
		if ((locks) && (detailedNotif)) {
			log.info msg
			send(msg)
		} */

/* 		switches?.off() // turn off the lights		
		msg = "AwayFromHome>Switched off all switches"
		if ((switches) && (detailedNotif)) {
			log.info msg
			send(msg)
		} */

	}
}

def takeActionsHome() {
	def msg
    if (!everyoneIsAway() || !residentsHaveBeenQuiet() || !doorsHaveNotOpened() ) {
        ecobee.each {
            it.resumeThisTstat()
            msg = "${ecobee} set back to it's normal schedule"
            send(msg)
        }
    }
    else {
    	msg = "${ecobee} climate should be away, not resuming schedule"
        send(msg)
    }
/*	if ((givenClimateNameP != null) && (givenClimateNameP != "")) {
		ecobee.each {
			it.setClimate('', givenClimateNameP) // Set to the climateName for Present
		}
		msg = "${ecobee} set to $givenClimateNameP"
		send(msg)
	} else {
		// Set to the default "Home"
		ecobee.each {
			it.setClimate('', "Home")
		}
		msg = "${ecobee} set to default Home"
		send(msg)
	}*/

/* 		msg = "AwayFromHome>${ecobee} thermostats' settings are now lower"
		if (detailedNotif ) {
			log.info msg
			send(msg)
		} */

/* 		locks?.lock() // lock the locks 		
		msg = "AwayFromHome>Locked the locks"
		if ((locks) && (detailedNotif)) {
			log.info msg
			send(msg)
		} */

/* 		switches?.off() // turn off the lights		
		msg = "AwayFromHome>Switched off all switches"
		if ((switches) && (detailedNotif)) {
			log.info msg
			send(msg)
		} */
}

private everyoneIsAway() {
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
    if(debugMode) {	
		log.debug "everyoneIsAway: $result"
	}
	return result
}

private residentsHaveBeenQuiet() {

	def threshold = residentsQuietThreshold ?: 3 // debounce 3 min by default
	Integer delay = threshold * 60

	def result = true
	def t0 = new Date(now() - (threshold * 60 * 1000))
	for (sensor in motions) {
		def recentStates = sensor.statesSince("motion", t0)
		if (recentStates.find {it.value == "active"}) {
			result = false
			break
		}
	}
	if(debugMode) {	
    	log.debug "residentsHaveBeenQuiet: $result"
    }
	return result
}

private doorsHaveNotOpened() {
	
	def threshold = residentsQuietThreshold ?: 3 // debounce 3 min by default
	int delay = threshold * 60
	
	def result = true
	def t0 = new Date(now() - (threshold * 60 * 1000))
	for (sensor in contactSensors) {
		if (recentStates.find {it.value == "open"}) {
			result = false
			break
		}
	}
    if(debugMode) {
		log.debug "doorsHaveNotOpened: $result"
    }
	return result
}

private getPerson(evt) {
	people.find {
		evt.deviceId == it.id
	}
}

private send(msg) {
	if (sendPushMessage != "No") {
        if(debugMode) {
			log.debug("sending push message: $msg")
		}
		sendPush(msg)
	}

	if (phone) {
        if(debugMode) {		
			log.debug("sending text message")
		}
		sendSms(phone, msg)
	}
}