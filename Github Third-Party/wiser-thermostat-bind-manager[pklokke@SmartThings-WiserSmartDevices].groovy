/**
 * Wiser Thermostat Bind Manager
 *
 * Copyright 2020 P. Klokke
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
    name: "Wiser Thermostat Bind Manager",
    namespace: "pklokke",
    author: "pklokke",
    description: "Bind a Wiser RTS Thermostat to an H-Relay",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png",
    pausable: true
)

preferences
{
    section("Choose a Thermostat RTS... ")
    {
        input "sensor", "device.wiserThermostatRts", title: "Thermostat RTS"
    }
    section("Select the H-Relay... ")
    {
        input "actuator", "device.wiserHeatingActuators", title: "H-Relay"
    }
}

def installed()
{
    subscribe(sensor, "temperature", temperatureReading)
    if( actuator && sensor)
    {
        log.debug "Installed: sensor $sensor actuator: $actuator"
        sensor.bind(actuator.device.zigbeeId,actuator.device.endpointId)
    }
}

def updated()
{
    if( actuator && sensor)
    {
        log.debug "Updated: sensor $sensor actuator: $actuator"
        sensor.bind(actuator.device.zigbeeId,actuator.device.endpointId)
    }
}

def uninstalled()
{
    sensor.unbind(actuator.device.zigbeeId,actuator.device.endpointId)
}

def temperatureReading(evt)
{
   actuator.setActualTemperature(sensor.currentTemperature)
}
