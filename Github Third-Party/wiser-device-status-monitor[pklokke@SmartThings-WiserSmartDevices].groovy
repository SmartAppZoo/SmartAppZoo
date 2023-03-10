/**
 * Wiser Device Status Monitor
 *
 *   Monitors the battery and calibration status of Wiser Devices, and pushes notifications when necessary
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
 */
definition(
    name: "Wiser Device Status Monitor",
    namespace: "pklokke",
    author: "pklokke",
    description: "Monitors battery and calibration status of Wiser Devices",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png",
    pausable: true
)

preferences {
    section("Choose RTSes to monitor... ")
    {
        input "rts", "device.wiserThermostatRts", title: "RTS", multiple: true
    }
    section("Choose Radiator Thermostats to monitor")
    {
        input "vact", "device.wiserRadiatorThermostat", title: "Radiator Thermostat", multiple: true
    }
    section("Choose Battery % Warning Threshold")
    {
        input "batteryPercent", "number", required: true, defaultValue: 20, title: "Battery %"
    }
}

def installed()
{
    subscribe(rts+vact, "battery", batteryUpdate)
    subscribe(vact, "valveCalibrationStatus", valveStatusUpdate)
    log.debug "Installed: rts: $rts vact: $vact"
}

def updated()
{
    unsubscribe()
    subscribe(rts+vact, "battery", batteryUpdate)
    subscribe(vact, "valveCalibrationStatus", valveStatusUpdate)
    log.debug "Updated: rts: $rts vact: $vact"
}

def batteryUpdate(evt)
{
   if(evt.value <= batteryPercent)
   {
       sendPush("${evt.getDevice().displayName} battery low: ${evt.value}%")
   }
}

def valveStatusUpdate(evt)
{
   if(evt.value.contains("Error"))
   {
       sendPush("${evt.getDevice().displayName}: ${evt.value}")
   }
}