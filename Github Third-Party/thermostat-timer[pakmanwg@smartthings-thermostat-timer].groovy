/**
 *  Copyright 2017 pakmanw@sbcglobal.net
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
 *  Thermostat Timer
 *
 *  Author: pakmanw@sbcglobal.net
 *
 *  Change Log
 *  2017-9-17  - v01.01 Created
 *  2017-11-25 - v01.02 Add Switch Off function, single target temperature
 *  2018-06-23 - v01.03 Remove support for button, set default temperature
 *  2018-07-07 - v01.04 Fix for NST which reset cool/heat point to thermostat point if thermostat point is not set during turn off
 *
 */

definition(
    name: "Thermostat Timer",
    namespace: "pakmanwg",
    author: "pakmanw@sbcglobal.net",
    description: "Turn off Themostat after certain amount of time",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png"
)

preferences {
    section("When switch on..."){
	    input "switch1", "capability.switch"
    }
    section("Choose Thermostat(s)"){
	    input "thermostat1", "capability.thermostat"
    }
    section("Minutes..."){
        input "minutes", "number", range: "1..*", title: "Minutes", required: true, defaultValue: 30
    }
    section("Set Target temperature") {
        input "opSet", "decimal", title: "Temperature", required: true, defaultValue: 70
    }
    section("Default temperatures") {
        input "defHeatSet", "decimal", title: "When Heating", description: "Default Heating temperature", required: true, defaultValue: 55
        input "defCoolSet", "decimal", title: "When Cooling", description: "Default Cooling temperature", required: true, defaultValue: 85
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(switch1, "switch.on", turnOnThermostat)
    subscribe(switch1, "switch.off", turnOffThermostat)
    state.lastStatus = "off"
}

def updated(settings) {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(switch1, "switch.on", turnOnThermostat)
    subscribe(switch1, "switch.off", turnOffThermostat)
}

def turnOnThermostat(evt) {
    state.lastStatus = "on"
    if (opSet < thermostat1.currentValue("temperature")) {
        thermostat1.setCoolingSetpoint(opSet)	
        state.lastSet = "cool"
    } else {
        thermostat1.setHeatingSetpoint(opSet)
        state.lastSet = "heat"
    }
    def delay = 60 * minutes
    runIn(delay, switchOff)
}

def turnOffThermostat(evt) {
    state.lastStatus = "off"
    if (state.lastSet == "cool") {
        thermostat1.setCoolingSetpoint(defCoolSet)
    } else {
        thermostat1.setHeatingSetpoint(defHeatSet)
    }
}

def switchOff() {
    switch1.off()
}
