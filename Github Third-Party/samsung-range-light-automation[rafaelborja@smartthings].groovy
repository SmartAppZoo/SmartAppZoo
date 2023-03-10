/**
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
 *  Range Lights/Fan automation based on Samsung connected ranges event triggers.
 *  https://github.com/rafaelborja/smartthings
 *
 *  Copyright 2017 Rafael Borja
 */
definition(
    name: "Samsung Range Light Automation",
    namespace: "rafaelborja",
    author: "Rafael Borja",
    description: """
    Lights automation app based on Samsung connected Range events.
    https://github.com/rafaelborja/smartthings
    """,
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances4-icn@2x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Select your range") {
        input "therange", "device.SamsungRange", required: true
    }
    section("Turn on the following lights light") {
        input "theswitch", "capability.switch", required: true
    }
    section("Delay before turning off lights") {                    
		input "delaySecs", "number", title: "Seconds after turning off cooktop?"
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(therange, "operationStateCooktop", cooktopDetectedHandler)
    subscribe(therange, "operationStateCooktop.Ready", cooktopDetectedHandler)
}

// TODO: implement event handlers

def cooktopDetectedHandler (evt) {
    log.debug "cooktopDetectedHandler called: $evt"
    if (evt) {
        log.debug "cooktopDetectedHandler $evt.name"
        log.debug "cooktopDetectedHandler $evt.value"
        
        if (evt.value == "Run") {
        	theswitch.on()
        } else {
        	// theswitch.off()
			runIn(delaySecs, scheduleCheck, [overwrite: false])
        }
    } 
}

/**
 * Checks state of range after a certain delay
 * (adapted from naissan : Lights Off with No Motion and Presence app)
 */
def scheduleCheck() {
	log.debug "Running scheduled check for turning off lights after delay"
	def rangeState = therange.currentState("operationStateCooktop")
    if (rangeState.value == "Ready") {
    	log.debug "Turning switch off"
    	theswitch.off()
    } else {
    	log.debug "Range is active: do nothing"
    }
}