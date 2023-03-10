/**
 *  Garage_Lights
 *
 *  Copyright 2018 JOSHUA MASON
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
    name: "Garage_Lights",
    namespace: "safetyguy14",
    author: "JOSHUA MASON",
    description: "Controls garage door lights so they don&#39;t get left on and they turn on and stay on when people are home and using the garage.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "Devices", title: "Select your Inputs/Outputs", 
         install: true, uninstall: true, hideWhenEmpty: true) {
            section("Motion Sensors") {
            input "motions", "capability.motionSensor", title: "Motion Sensor(s)", multiple: true, required: false
        }
        section("Lights") {
            input "switches", "capability.switch", title: "Light(s)", multiple: true, required: false
        }
        section("Door(s)") {
            //input "garageDoorControls", "capability.garageDoorControl", title: "Garage Door(s)", multiple: true, required: false
            //input "doorControls", "capability.doorControl", title: "Door(s)", multiple: true, required: false
            input "contactSensors", "capability.contactSensor", title: "Misc", multiple: true, required: false
        }
        section("Not Present debounce timer [default=5 minutes]") {
            input "garageQuietThreshold", "number", title: "Time in minutes", required: false
        }
        section("want to turn on mega-debugging?") {
            input "debugMode", "bool", title: "Debug Mode?", required: false
        }
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
    // using motion sensors to detect presence
	if (motions != null && motions != "") {
		subscribe(motions, "motion", motionEvtHandler)
	}	
    // using contact sensors on doors to trigger a delayed Away action
    // and as a method of presence
	if (contactSensors != null && contactSensors != "") {
		subscribe(contactSensors, "contact", contactEvtHandler)
	}
    // using the switch transition to trigger a delayed action
    if (switches != null && switches != "") {
    	subscribe(switches, "switch", switchEvtHandler)
    }
}

def contactEvtHandler(evt) {
	if (evt.value == "open") {
    	if(debugMode) {
			log.debug "Somebody opened a door... ${evt.device}"
        }
        //turn on the lights
        turnOnLights()
    }
	if (evt.value == "closed") {
    	if(debugMode) {
			log.debug "Somebody closed a door... ${evt.device}"
        }
        //turn off the lights in 30s unless motion
        delayedTurnOff(30)
    }
}

def motionEvtHandler(evt) {
	if (evt.value == "active") {
		if(debugMode) {
        	log.debug "Motion in the garage... ${evt.device}"
		}
		//turn on the lights
		turnOnLights()
	}
	if (evt.value == "inactive") {
		if(debugMode) {
        	log.debug "No longer motion in the garage... ${evt.device}"
		}
		//turn off the lights after the threshold
		delayedTurnOff(60*garageQuietThreshold)
	}
}

def switchEvtHandler(evt) {
	if (evt.value == "on") {
    	if(debugMode) {
        	log.debug "Somebody turned on the lights... ${evt.device}"
        }
    //call delayed turn off
	delayedTurnOff(60*garageQuietThreshold)
    }
    if (evt.value == "off") {
    	if(debugMode) {
        	log.debug "Somebody turned off the lights... ${evt.device}"
        }
    //clear all scheduled handlers
    unschedule()
    }
}

def turnOffLights() {
	switches.each{
        if(it.currentSwitch == "off") {
            if(debugMode) {	
                log.debug "${it.device} garage lights already off"
            }
        }
        else { 
            if(garageIsQuiet()) {
                it.off()
                log.info "Turned off the ${it.device} garage lights"
            }
            else {
                if(debugMode) {
                    log.debug "Motion detected in garage, waiting $garageQuietThreshold minutes and trying again"            
                }
                //call the delayed turn off every delay threshold until no motion
                delayedTurnOff(60*garageQuietThreshold)
            }
        }
    }
}

def turnOnLights() {
	switches.each{
        if(it.currentSwitch == "on") {
            if(debugMode) {	
                log.debug "${it.device} garage lights already on"
            }
        }
        else {
        	it.on()
            log.info "Turning on ${it.device} garage lights"
        }
    }
}

private garageIsQuiet() {

	def threshold = garageQuietThreshold ?: 5 // debounce 5 min by default

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
    	log.debug "garageIsQuiet: $result"
    }
	return result
}

def delayedTurnOff(delay) {
    runIn(delay, turnOffLights)
}