/**
 *  Security light
 *
 *  Copyright 2019 Simon Cross
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
    name: "Security light",
    namespace: "sicross",
    author: "Simon Cross",
    description: "Allows a sensor to turn on a light, and off again, but respects the previous setting -- if the light was already on, the sensor going off won&#39;t turn it off.",
    category: "Safety & Security",
    iconUrl: "https://github.com/sicross/smartthings/blob/master/motion-sensor-icon-16.png",
    iconX2Url: "https://github.com/sicross/smartthings/blob/master/motion-sensor-icon-16.png",
    iconX3Url: "https://github.com/sicross/smartthings/blob/master/motion-sensor-icon-16.png")


preferences {
	section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: false, title: "Choose a motion sensor"
        input "thecontact", "capability.contactSensor", required: false, title: "or a contact sensor"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true, title: "Choose a light or switch"
    }
    section("Delay") {
    	input "motionDelay", type: "number", description: "Minutes", title: "After motion was detected, how long (in minutes) would you like to keep the light on for?"
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
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionCompleteHandler)
    subscribe(thecontact, "contact.open", motionDetectedHandler) // I think this means motion
    subscribe(thecontact, "contact.closed", motionCompleteHandler)
    state.triggered = false
}

def motionDetectedHandler(evt) {
        
    def switchState = theswitch.currentValue("switch")
      
    if (switchState == "off") {
    	log.debug "Motion detected, switch was off, turning switch ON, starting timer"
        theswitch.on()
        state.triggered = true
        if (motionDelay.toInteger()<1){
        	motionDelay = 1 // minimum 1 min
        }
        runIn(motionDelay.toInteger()*60, "lightOffHandler")
        
    } else {
    	// switch is on, now we have to determine why...
        if (state.triggered == true) {
        	log.debug "Motion detected, but switch was already on because of previous movement, restarting timer again."
            if (motionDelay.toInteger()<1){
        		motionDelay = 1 // minimum 1 min
       		}
        	runIn(motionDelay.toInteger()*60, "lightOffHandler")
		} else {
        	log.debug "Motion detected, but switch was already manually switched on, so leaving switch on."
        	state.triggered = false
        }
    }
}

def motionCompleteHandler(evt) {

	// this doesn't matter, all done on the timer
	
}

def lightOffHandler() {

    if (state.triggered == true) {
    	log.debug "Timer done, switch was off when motion detected, so turning switch OFF"
    	theswitch.off()
        state.triggered = false
    } else {
    	log.debug "Timer done, but switch was on when motion detected, so leaving switch in current state."
    }
    
}
