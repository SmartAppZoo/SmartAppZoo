/**
 *  Switch To Thermostat
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
    name: "Switch To Thermostat",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "Thermostat is on with switch only",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Preferences") {
		input "thermostat", "capability.thermostat", title: "Thermostat", multiple: true, required: true
        input "heatOrCool", "enum", title: "Heat or Cool?", options: ["Heat", "Cool"]
        input "switch1", "capability.switch", title: "Switch", required: true
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
	subscribe(switch1, "switch.on", "onSwitchHandler")
    subscribe(switch1, "switch.off", "offSwitchHandler")
}

def onSwitchHandler(evt) {
	if (heatOrCool == "Heat") {
    	thermostat.heat()
    }
    
    if (heatOrCool == "Cool") {
    	thermostat.cool()
    }
}

def offSwitchHandler(evt) {
    thermostat.off()
}