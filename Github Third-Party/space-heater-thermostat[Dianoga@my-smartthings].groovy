/**
*  Space-Heater Thermostat
*
*  <TODO: Enter some description of your smart app project here>
*
*  Copyright undefined Brian Steere
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
	name: "Space-Heater Thermostat",
	namespace: "dianoga",
	author: "Brian Steere",
	description: "Enter some description of your smart app project here",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Devices") {
		input "thermostat", "capability.thermostat", title: "Thermostat", multiple: false, required: true
		input "heaters", "capability.switch", title: "Heaters", multiple: true, required: true
		input "temperature", "capability.temperatureMeasurement", title: "Temperature", multiple: false, required: true
	}

	section("Settings") {
		input "offset", "number", title: "Temperature Offset", defaultValue: 0
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
	subscribe(temperature, "temperature", handleEvent)
	subscribe(thermostat, "heatingSetpoint", handleEvent)
	subscribe(thermostat, "thermostatMode", handleEvent)

	handleEvent()
}

def handleEvent(evt) {
	def thermostatModes = ["heat", "auto", "emergencyHeat"]

	log.debug "Thermostat Mode: ${thermostat.currentThermostatMode} | Temperature: ${temperature.currentTemperature}"

	// Is thermostat on
	if (thermostatModes.any { it == thermostat.currentThermostatMode }) {
		// Is room temperature below heatingSetpoint + offset
		if (temperature.currentTemperature < thermostat.currentHeatingSetpoint + offset) {
			log.debug "Turning heater(s) on"
			heaters?.on()
		} else {
			log.debug "Turning heater(s) off"
			heaters?.off()
		}
	} else {
		// Thermostat is off, turn off the heater
		log.debug "Turning heater(s) off"
		heaters?.off()
	}
}
