/**
 *  Reactions to Waking at Home (Sleeping in vs. Not sleeping in)
 *
 *  Copyright 2017  Greg Doornink
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
    name: "Reactions to Waking at Home (Sleeping in vs. Not sleeping in)",
    namespace: "GTDoor",
    author: "Greg Doornink",
    description: "If person is home and wakes up, you can use this app to select switches to turn on and off based on the state of a sleeping-in indicator switch.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Home/Sleeping Indicators") {
    	input "homeIndicator", "capability.presenceSensor", title:"Presence indicator", multiple: false, required: true
    	input "awakeIndicator", "capability.switch", title:"Switch indicating Awake", multiple: false, required: true
		input "sleepinIndicator", "capability.switch", title:"Sleep In Switch (on = sleeping in)", multiple: false, required: true
    }
    section("If sleeping in") {
        input "sleepinOnSwitches", "capability.switch", title:"Turn ON", multiple: true, required: false
        input "sleepinOffSwitches", "capability.switch", title:"Turn OFF", multiple: true, required: false
	}
    section("If NOT sleeping in") {
        input "normalOnSwitches", "capability.switch", title:"Turn ON", multiple: true, required: false
        input "normalOffSwitches", "capability.switch", title:"Turn OFF", multiple: true, required: false
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
	subscribe(awakeIndicator, "switch.on", awakeHandler)
}

def awakeHandler(evt) {
	if(homeIndicator.currentValue("presence") == "present") {
		if(sleepinIndicator.currentValue("switch") == "on") {
	    	sleepinOnSwitches.on()
	        sleepinOffSwitches.off()
	        log.debug "Greg woke up after sleeping in."
	    } else {
	    	normalOnSwitches.on()
	        normalOffSwitches.off()
	        log.debug "Greg woke up after not sleeping in."
	    }
	    sleepinIndicator.off()
	    log.debug "Turned sleep in indicator off."
	} else {
    	log.debug "Person is away."
    }
}
