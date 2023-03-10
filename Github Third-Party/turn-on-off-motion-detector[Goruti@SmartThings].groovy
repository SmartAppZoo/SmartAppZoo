/**
 *  Turn On/Off motion Detector
 *
 *  Copyright 2016 Diego
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
    name: "Turn On/Off motion Detector",
    namespace: "DiegoAntonino",
    author: "Diego",
    description: "Turn On when Motion detect and turn of when there is not movement for x minutes",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    section("Turn on this light when Mode is Home") {
        input "switchHome", "capability.switch", required: true
    }
    section("Dimmer to make bright when Mode is Night") {
	    input "dimmerNight", "capability.switchLevel", title: "Which dimmer?", required: false
	    input "brightness", "number", title: "Light Level", required: false
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    def curMode = location.mode

    if (curMode == "Home") {
    	switchHome.on()
        log.debug "In Home Mode"
    }
    
    else if (curMode == "Night") {
	    if (dimmerNight && brightness) {
		    dimmerNight.setLevel(brightness)
		    log.debug "In Night Mode; setting brightness: $brightness"
	    }
    }
    else { //Home is in away mode, do nothing
    	log.debug "Home is in Away mode, doing nothing"
    }
       
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    def curMode = location.mode

    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms)"
			if (curMode == "Home") {
                switchHome.off()
                log.debug "turning switch off"
            	}
			else if (curMode == "Night") {
                dimmerNight.setLevel(0)
                log.debug "turning dimmer off"
            	}
            else {
            log.debug "Current mode is $location.currentMode"
                }			
            }
		else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
