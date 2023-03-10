/**
 *  Nest presence detect
 *
 *  Copyright 2015 Erik Vennink
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

/************
 * Metadata *
 ************/

definition(
    name: "Nest presence detect",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Change the presence mode of a Nest thermostat based on the presence of (multiple) presence sensor(s) or phones.",
    category: "SmartThings Labs",
    iconUrl: "http://icons.iconarchive.com/icons/iconshock/vector-twitter/128/twitter-nest-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/iconshock/vector-twitter/128/twitter-nest-icon.png",
    iconX3Url: "http://icons.iconarchive.com/icons/iconshock/vector-twitter/128/twitter-nest-icon.png")

/**********
 * Setup  *
 **********/

preferences {
  	section("When these people arrive and leave..."){
		input "people", "capability.presenceSensor", title: "Who?", multiple: true, required: false
	}
	section("... or this switch is switched...") {
		input "nestSwitch", "capability.switch", multiple: false, required: false
	}
	section("... change modes for these thermostat(s)...") {
		input "thermostats", "capability.thermostat", multiple: true
	}
}

/*************************
 * Installation & update *
 *************************/

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	if (people) {
    	subscribe(people, "presence", peopleHandler)
    }
    if (nestSwitch) {
		subscribe(nestSwitch, "switch", switchHandler)
    }
}

/******************
 * Event handlers *
 ******************/

def switchHandler(evt) {
	log.debug "evt.name: $evt.value"
    def switchStatus = evt.value
    if (state.quietSwitch) {
    }
    else {
        if (switchStatus == "on") {
            thermostats?.present()
            log.debug "Somebody arrived, Nest is set to Present."
            sendNotificationEvent("Nest set to 'Present'")
            state.thermostat = "present"
        }
        else {
            thermostats?.away()
            log.debug "Everybody left, Nest is set to Away."
            sendNotificationEvent("Nest set to 'Away'")
            state.thermostat = "away"
        }
	}
}

def peopleHandler(evt) {
    log.debug "evt.name: $evt.value"
    if (everyoneIsAway()) {
        thermostats?.away()
        log.debug "Everybody left, Nest is set to Away."
        state.thermostat = "away"
        if (nestSwitch) {
        	state.quietSwitch = true
        	nestSwitch.off()
		}
        else {
        	sendNotificationEvent("Nest set to 'Away'")
        }
    }
    else if (state.thermostat == "away") {
        thermostats?.present()
        log.debug "Somebody arrived, Nest is set to Present."
        state.thermostat = "present"
        if (nestSwitch) {
        	state.quietSwitch = true
            nestSwitch.on()
		}
        else {
        	sendNotificationEvent("Nest set to 'Present'")
        }
    }
    else {
        thermostat?.present()
        log.debug "Somebody arrived or left, at least 1 person present. Nest set to Home."
        state.thermostat = "present"
        if (nestSwitch) {
        	state.quietSwitch = true
            nestSwitch.on()
		}

    }
    state.quietSwitch = false
}

/******************
 * Helper methods *
 ******************/

private everyoneIsAway() {
    def result = true
    for (person in people) {
        if (person.currentPresence == "present") {
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
}
