/**
 *  Smart Light Timer for Vincent
 *
 *  Copyright 2016 Vincent
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
    name: "Smart Light Timer for Vincent",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Turn on/off the lights based on sensors.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Turn on when there's movement..."){
		input "motions", "capability.motionSensor", multiple: true, title: "Select motion detectors", required: false
        input "motionMinutes", "number", title: "Minutes to turn off after motion stops", defaultValue: "1"
	}
	section("Or, turn on when one of these contacts opened"){
		input "contacts", "capability.contactSensor", multiple: true, title: "Select Contacts", required: false
	}
	section("Minutes to turn off after no more triggers"){
        input "minutes1", "number", title: "Minutes?", defaultValue: "5"
    }
    section("Delay turning off if the light is manually switched off and switched back on in 3 seconds") {
    	input "minutes2", "number", title: "Minutes?", defaultValue: "30"
    }
	section("Turn on/off light..."){
		input "myswitch", "capability.switch", title: "Select Light"
	}
}


def installed()
{
	subscribe(motions, "motion", motionHandler)
	subscribe(contacts, "contact", contactHandler)
	subscribe(myswitch, "switch", switchChange)
    state.byMe = false
    state.lastManualOff = null
    state.extendedMinutes = 0
}


def updated()
{
	unsubscribe()
    installed()
}

def sinceLastManualOff() {
	state.lastManualOff == null ? 1e10 : (now() - state.lastManualOff) / 1000
}

def switchChange(evt) {
	log.debug "SwitchChange from $evt.source: $evt.name: $evt.value"
    if (evt.value == "on") {
    	if (!state.byMe) {
        	// Extend the timeout, if it's manually switched off then on in 3 seconds.
        	if (sinceLastManualOff() < 3) {
            	log.debug "Extended timeout by $minutes2 minutes"
            	state.extendedMinutes = minutes2
            }
    		scheduleTurnOff(minutes1)
        }
    } else if (evt.value == "off") {
    	state.extendedMinutes = 0
    	if (!state.byMe) {
        	state.lastManualOff = now()
        }
    }
    state.byMe = false;
}

def contactHandler(evt) {
	log.debug "contactHandler: $evt.name: $evt.value"
    if (evt.value == "open") {
    	turnOn(minutes1)
    }
}

def motionHandler(evt) {
	log.debug "motionHandler: $evt.name: $evt.value"
    if (evt.value == "active") {
    	turnOn(minutes1)
    } else if (evt.value == "inactive") {
    	scheduleTurnOff(motionMinutes) 
    }
}

def turnOn(minutes) {
	log.debug "turnOn: " + myswitch.latestValue("switch")
	if (myswitch.latestValue("switch") == "off") {
	    state.byMe = true
		myswitch.on()
    }
    scheduleTurnOff(minutes)
}

def scheduleTurnOff(minutes) {
	log.debug "scheduleTurnOff: $minutes minutes with $state.extendedMinutes minutes delay"
	runIn((minutes + state.extendedMinutes) * 60, turnOff)
}

def turnOff() {
	log.debug "turnOff: " + myswitch.latestValue("switch")
    state.extendedMinutes = 0
	if (myswitch.latestValue("switch") == "on") {
    	if (motions.find { it.latestValue("motion") == "active" } == null) {
	    	state.byMe = true
			myswitch.off()
        } else {
        	// Re-check after 1 minute.
        	scheduleTurnOff(1)
        }
    }
}