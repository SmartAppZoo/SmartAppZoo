/**
 *  Copyright 2016 LimeNinja
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
 *  SSR302 Virtual Thermostat
 *
 *  Author: LimeNinja
 */
 
definition(
    name: "SSR302 Virtual Thermostat",
    namespace: "LimeNinja",
    author: "LimeNinja",
    description: "Use a seperate temperature/humidity sensor to control the SSR302 Z-Wave Boiler Actuator.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the SSR302 unit... "){
		input "outlet", "capability.thermostat", title: "SSR302", multiple: false
	}
}

def installed()
{
	log.debug "Installed"
	subscribe(sensor, "temperature", temperatureHandler)
    subscribe(sensor, "humidity", humidityHandler)

    // Set setPoint
    if (!atomicState.setPoint) {
    	atomicState.setPoint = 20.0
    }
}

def updated()
{
	log.debug "Updated"
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
    subscribe(sensor, "humidity", humidityHandler)
    
    // Set setPoint
    if (!atomicState.setPoint) {
    	atomicState.setPoint = 20.0
    }
}

def temperatureHandler(evt)
{
	log.info "Setting temperature: $evt.doubleValue"
    outlet.setTemperature(evt.doubleValue)
}

def humidityHandler(evt)
{
	log.info "Setting humidity: $evt.doubleValue"
    outlet.setHumidity(evt.doubleValue)
}