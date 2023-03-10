/**
 *  Automatic Entry Lighting
 *
 *  Copyright 2017 Stephan Ross
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
    name: "Automatic Entry Lighting",
    namespace: "rosswerks",
    author: "Stephan Ross",
    description: "Logic for automatically lighting the hallway when the front door is opened from the outside.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When this door opens:") {
        input "thedoor", "capability.contactSensor", required: true, title: "Which door?"
    }
    section("And motion is not detected here:") {
        input "themotion", "capability.motionSensor", required: true, title: "Motion where?"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true, title: "Which light?"
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
	//subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(thedoor, "contactSensor.open", contactOpenedHandler)
}

def contactOpenedHandler(evt){
	log.debug "contactOpenedHandler called: $evt"
    def motionState = themotion.currentState("motionSensor")
    if (motionState.value == "inactive") {
    		// Motion inactive; just log it and do nothing
    		log.debug "There is no motion inside the door, so turn on the lights!"
            //sendLocationEvent(name: "alarmSystemStatus", value: "away")
            //sendLocationEvent(name: "alarmSystemStatus", value: "stay")
            sendLocationEvent(name: "alarmSystemStatus", value: "off")
            theswitch.on();
            
    } else {
            // Motion active; just log it and do nothing
            log.debug "Motion is active inside the door, so do nothing."
    }
}

// TODO: implement event handlers