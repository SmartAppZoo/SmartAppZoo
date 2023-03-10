/**
 *  testSmartApp
 *
 *  Copyright 2017 Jacob Carroll
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
	
    name: "Motion Light Switch",
    namespace: "jcCarroll",
    author: "Jacob Carroll",
    description: "Use motion turn on a light with dimmer set.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light6-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light6-icn@2x.png")

preferences {
	section("What time would you like the motion sensor to take control of the light?") {
    	input "theTimeStart", "time", required: true, title: "From"
        input "theTimeStop", "time", required: true, title: "To"
    }
	section("Select which motion sensor would you like to use?") {
        input "themotion", "capability.motionSensor", required: true, title: "Select Sensor"
    }
    section("Turn off when there's been no movement for") {
    	input "minutes", "number", repuired: true, title: "Minutes?"
    }
    section("Turn on this light") {
        input "light", "capability.switch", title: "Select Light", required: false, multiple: false
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
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
	log.debug "motionDetectedHandler called: $lightLevel"
    def between = timeOfDayIsBetween(theTimeStart, theTimeStop, new Date(), location.timeZone)
    if (between) {
		light.on()
	}
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.date.time

        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            light.off()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
            log.debug "Motion is active, do nothing and wait for inactive"
    }
}
