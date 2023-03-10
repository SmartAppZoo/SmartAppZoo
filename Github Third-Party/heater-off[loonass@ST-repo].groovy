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
 *  Virtual Thermostat
 *
 *  Author: SmartThings
 */
definition(
    name: "Heater Off",
    namespace: "loonass",
    author: "Mike Harvey",
    description: "Turn off heater when temperature is reached.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png",
    pausable: true
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the heater outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the maximum temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
}

def installed()
{
	log.debug "Installed"
	subscribe(sensor, "temperature", temperatureHandler)
}

def updated()
{
	log.debug "Updated ${settings}"
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
}

def temperatureHandler(evt)
{
	log.debug "Evaluate (Is ${sensor.currentTemperature} >= ${settings.setpoint})"
    
			if (sensor.currentTemperature >= setpoint) {
            log.debug "Yes"
			outlets.off()
            log.debug "Off"
		} else {
        	log.debug "No"
		}
}