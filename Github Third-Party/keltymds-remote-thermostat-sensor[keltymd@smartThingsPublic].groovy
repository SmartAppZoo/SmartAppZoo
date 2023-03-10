/**
 *  Copyright 2015 SmartThings
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
 * Remote Thermostat Sensor
 *
 *  Author: Keltymd
 */

definition(
name: "Keltymds Remote thermostat sensor",
namespace: "keltymd",
author: "keltymd",
description: "Adjusts your thermostat like Keep me Cozy II but is designed to work with Keenect and allows more accurate tempurature control",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting..." ) {
		input "heatingSetpoint", "decimal", title: "Degrees"
	}
	section("Air conditioning setting...") {
		input "coolingSetpoint", "decimal", title: "Degrees"
	}
	section("Optionally choose temperature sensor to use instead of the thermostat's... ") {
		input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: false
	}
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

private evaluate()
{
	if (sensor) {
		def threshold = 0.1
		def tm = thermostat.currentThermostatMode
		def ct = thermostat.currentTemperature
		def currentTemp = sensor.currentTemperature
		log.trace("evaluate:, mode: $tm -- temp: $ct, heat: $thermostat.currentHeatingSetpoint, cool: $thermostat.currentCoolingSetpoint -- "  +
			"sensor: $currentTemp, heat: $heatingSetpoint, cool: $coolingSetpoint")
		if (tm in ["cool","auto"]) {
			// air conditioner
			if (currentTemp - coolingSetpoint -0.9 >= threshold) {
				thermostat.setCoolingSetpoint(coolingSetpoint - 5)
				log.debug "thermostat.setCoolingSetpoint(${coolingSetpoint - 5}), ON"
			}
			else if (coolingSetpoint - currentTemp +0.2 >= threshold ) {
				thermostat.setCoolingSetpoint(coolingSetpoint + 5)
				log.debug "thermostat.setCoolingSetpoint(${coolingSetpoint + 2}), OFF"
			}
		}
		if (tm in ["heat","emergency heat","auto"]) {
			// heater
			if (heatingSetpoint - currentTemp - 0.9 >= threshold ) {
				thermostat.setHeatingSetpoint(heatingSetpoint + 5)
				log.debug "thermostat.setHeatingSetpoint(${heatingSetpoint + 5}), ON"
			}
			else if (currentTemp  - heatingSetpoint + 0.2 >= threshold ) {
				thermostat.setHeatingSetpoint(heatingSetpoint - 5)
				log.debug "thermostat.setHeatingSetpoint(${heatingSetpoint - 5}), OFF"
			}
		}
	}
	else {
		thermostat.setHeatingSetpoint(heatingSetpoint)
		thermostat.setCoolingSetpoint(coolingSetpoint)
		thermostat.poll()
        thermostat.refresh()
	}
}
