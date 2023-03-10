/**
 *  Thermostat Mode Changer
 *
 *  Copyright 2018 Patrik Karlsson
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
    name: "Thermostat Mode Changer",
    namespace: "nevdull77",
    author: "Patrik Karlsson",
    description: "Changes the thermostat operational mode of a thermostat on hub mode changes. As an example, it can set the operational mode to off, when mode is set to away.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	// This could probably be taken out and replaced by "set for specific mode(s)" only, however it's not as intuitive.
	section("When the mode changes to...") {
		input("newMode", "mode", multiple: true, required: true)
	}
    section("Change the following thermostats ...") {
		input("thermostat", "capability.thermostat", title: "Thermostat", multiple: true, required: true)
	}
    section("Mode") {
            input "thermostatMode", "enum",
                title: "What thermostat mode?",
                required: true,
                options: [
                    'auto',
                    'cool',
                    'heat',
                    'off'
                ]
    }
	section("Set the thermostat to the following temps") {
		input("heatingTemp", type: "number", title: "Heating Temp?", required: false)
        input("coolingTemp", type: "number", title: "Cooling Temp?", required: false)
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

def modeChangeHandler(evt) {
	log.debug "Mode changed: ${location.mode}, monitoring: ${newModes}"
    for (mode in newMode) {
    	if (location.mode == mode) {
        	log.debug "Changing thermostat mode to: ${thermostatMode}"
            thermostat?.setThermostatMode(thermostatMode)
            log.debug "Setting cooling point to: ${coolingTemp}"
			thermostat?.setCoolingSetpoint(coolingTemp)
            log.debug "Setting heating point to: ${heatingTemp}"
			thermostat?.setHeatingSetpoint(heatingTemp) 
        }
    }
}

def initialize() {
    subscribe(location, modeChangeHandler)
}
