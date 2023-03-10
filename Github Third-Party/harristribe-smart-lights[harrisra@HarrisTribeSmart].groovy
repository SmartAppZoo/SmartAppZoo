/**
 *  HarrisTribe Smart Lights
 *
 *  Copyright 2019 Robert Harris
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
    name: "HarrisTribe Smart Lights",
    namespace: "harrisra",
    author: "Robert Harris",
    description: "Single automation for parent app HarrisTribe Smart Lighting",
    category: "My Apps",
    parent: "harrisra:HarrisTribe Smart Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn on these lights...") {
		input "switches", "capability.switch", multiple: true
	}
	section("When there's movement here...") {
		input "motionSensor", "capability.motionSensor", title: "Where?"
	}
 	section("And low light is measured here") {
		input "luxSensor", "capability.illuminanceMeasurement", required: false
	}   
	section("And off when there's been no movement for...") {
		input "minutes", "number", title: "Minutes?"
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
	log.debug "Initialising..."
	subscribe(motionSensor, "motion", motionHandler)
	subscribe(luxSensor, "illuminance", luxHandler)  
    log.debug "Initialising...Done"
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active" && lightingIsNeeded()) {     
		log.debug "turning on lights"
		switches.on()
	} else if (evt.value == "inactive") {
    	log.debug "lights will turn off in $minutes mins"
		runIn(minutes * 60, scheduleCheck)
	}
}

def lightingIsNeeded() {

    // only check for light levels if configured to do so
    if (!luxSensor) {
    	log.debug "No luxSensory configured so assuming that lighting is needed"
    	return true
    }
    
	// find last reported light level
 	def currentLuxValue = luxSensor.currentValue("illuminance")

    // if its dark outside we need lights to turn
	if (currentLuxValue < 1350) {
    	log.debug "Current Lux Level is $currentLuxValue - lighting is needed"
		return true
    } else { 
    	log.debug "Current Lux Level is $currentLuxValue - lighting is not needed"
    	return false
    }
}

def luxHandler(evt) {
	if (!lightingIsNeeded()) {
    	log.debug "turning off lights"
    	switches.off()
    }
}

def scheduleCheck() {
	log.debug "schedule check"
	def motionState = motionSensor.currentState("motion")
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
    	def threshold = 1000 * 60 * minutes - 1000
    	if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning lights off"
            switches.off()
    	} else {
        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Motion is active, do nothing and wait for inactive"
    }
}
