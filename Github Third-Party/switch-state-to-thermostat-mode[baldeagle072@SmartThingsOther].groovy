/**
 *  Switch state to thermostat mode
 *
 *  Copyright 2015 Eric Roberts
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
    name: "Switch state to thermostat mode",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "If the switch turns on - sets the thermostat mode to desired mode. If the switch turns off - sets the thermostat mode to off",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Devices") {
		input "masterSwitch", "capability.switch", title: "Master Switch", required: true, multiple: false
        input "slaveTstat", "capability.thermostat", title: "Slave Thermostat", required: true
	}
    
    section("Preferences") {
		input "setMode", "enum", title: "Thermostat Mode when On", options:["cool", "heat"], required: true, multiple: false
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
	subscribe(masterSwitch, "switch", "switchHandler")
}

def switchHandler(evt) {
	if (evt.value == "on") {
    	slaveTstat.setThermostatMode(setMode)
    } else {
    	slaveTstat.setThermostatMode("off")
    }
}


