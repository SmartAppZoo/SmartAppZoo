/**
 *  Open vent on Thermostat Cool, Close on Heat
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
    name: "Open vent on Thermostat Cool, Close on Heat",
    namespace: "jcharr1",
    author: "Jason Charrier",
    description: "Open vent on Thermostat Cool, Close on Heat",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Thermostat") {
		// TODO: put inputs here
        input "thermostat", "capability.thermostat", required: true
	}
    section("Select Vents (on/off)") {
    	// TODO: put inputs here
    	input "theVents", "capability.switch", required: true, multiple: true
    }
    section("Select Vents (levels)") {
    	// TODO: put inputs here
    	input "theVentsLevels", "capability.switchLevel", required: true, multiple: true
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
    log.debug "Subscribe to thermostatMode"
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)
    subscribe(thermostat, "thermostatOperatingState", thermostatOSHandler)
}

// TODO: implement event handlers
def thermostatModeHandler(evt){
	log.debug "Thermostat Mode Changed: $evt.value"
	if(evt.value == "heat")
    {
    	log.debug "Closing vents..."
    	theVents.off()
    }
    else if(evt.value == "cool")
    {
    	log.debug "Opening vents..."
    	theVents.on()
		for(ventLevel in theVentsLevels) {
			if(ventLevel.level == 0) {
				ventLevel.setLevel(100)
			}
		}
    }
}

def thermostatOSHandler(evt){
	log.debug "Thermostat Operating State Changed: $evt.value"
	if(evt.value == "heating")
    {
    	log.debug "Closing vents..."
    	theVents.off()
    }
    else if(evt.value == "cooling")
    {
    	log.debug "Opening vents..."
    	ttheVents.on()
		for(ventLevel in theVentsLevels) {
			if(ventLevel.level == 0) {
				ventLevel.setLevel(100)
			}
		}
    }
}