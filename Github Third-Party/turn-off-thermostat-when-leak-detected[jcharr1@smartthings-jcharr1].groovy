/**
 *  Turn off Thermostat when Leak Detected
 *
 *  Copyright 2016 Jason Charrier
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
    name: "Turn off Thermostat when Leak Detected",
    namespace: "jcharr1",
    author: "Jason Charrier",
    description: "Turn off Thermostat when Leak Detected",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Choose water leak sensors") {
        input "leakSensors", "capability.waterSensor", multiple: true, required: true
    }
    
    section("Choose the thermostat") {
        input "thermostat", "capability.thermostat", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def leakHandler(evt) {
    log.debug "one of the configured leak sensors changed states: $evt"
    thermostat.off()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "Subscribing to leakSensors: water.wet"
    subscribe(leakSensors, "water.wet", leakHandler)
}

// TODO: implement event handlers