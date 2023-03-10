/*
 *
 *  Virtual Thermostat Control
 *
 *  Copyright 2015 Michael Pfammatter
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
 *
*/

definition(
    name: "Virtual Thermostat Controller",
    namespace: "GnomeSoup",
    author: "Michael J. Pfammatter",
    description: "A virtual thermostat to control a remote thermostat using an independent temperature sensor",
    category: "Climate Control",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Choose a thermostat...") {
        input "thermostat", "capability.thermostat", title: "Thermostat"
    }
    section("Choose a temperature sensor..."){
        input "sensor", "capability.temperatureMeasurement", title: "Sensor"
    }
}

def installed()
{
    log.debug "running installed"
    stat.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
    stat.lastTemp = null
    stat.contact = true
}

def createDevice() {
    def thermostat
    def label = app.getLabel()
    log.debug "create device with id: gnome$stat.deviceID, named: $label, "
    try {
        thermostat = addChildDevice("gnomesoup", "Virtual Thermostat", "gnome" + state.deviceID, null,
                                    [label: label, name: label, completedSetup: true])
    } catch(e) {
        log.error("Caught exception", e)
    }
}

def getThermostat() {
    return getChildDevice("gnome" + stat.deviceID)
}

def updated() {
    log.debug "running updated: $app.label"
    unsubscribe()
    def thermostat = getThermostat()
    if(thermostat == null) {
        thermostat = createDevice()
    }
    state.lastTemp = null
    subscribe(sensor, "temperature", temperatureHandler)
    // subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
    // subscribe(thermostat, "thermostatMode", thermostatModeHandler)
}
