/**
 *  Baby Temperature Monitor
 *
 *  Copyright 2017 Chad Lee
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
 *	2017-07-04: Initial Release
 */
definition(
	name: "Baby Temperature Monitor",
	namespace: "chadly",
	author: "Chad Lee",
	description: "Monitor a temperature sensor and when it gets too hot, turn a light on",
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@3x.png")

preferences {
	input "therm", "capability.temperatureMeasurement", title: "Thermostat to Monitor", required: true
	input "maxThreshold", "number", title: "if above... (default 80°)", defaultValue: 80, required: true
	input "minThreshold", "number", title: "if below... (default 60°)", defaultValue: 60, required: true
	input "lights", "capability.switch", required: true, multiple: true, title: "Lights to Turn On"
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	runEvery1Hour(doTempCheck)
	doTempCheck()
}

def doTempCheck() {
	log.trace "(0D) - doTempCheck() - settings: ${settings}"

	def thermostatLevel = therm.currentValue("temperature")

	log.trace "(0E) - Checking... ${therm.label}: ${thermostatLevel}°\n"

	if (settings.maxThreshold.toInteger() != null && thermostatLevel >= settings.maxThreshold.toInteger())
	{
		log.warn "(0F) - ${therm.label}: ${thermostatLevel}°\n"
		lights.on()
	}

	if (settings.minThreshold.toInteger() != null && thermostatLevel <= settings.minThreshold.toInteger())
	{
		log.warn "(10) - ${thermostatDevice.label}: ${thermostatLevel}°\n"
		lights.on()
	}
}