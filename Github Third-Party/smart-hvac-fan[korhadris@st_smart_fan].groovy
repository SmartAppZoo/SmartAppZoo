/**
 *  Smart HVAC Fan
 *
 *  Copyright 2017 Josh M
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
    name: "Smart HVAC Fan",
    namespace: "korhadris",
    author: "Josh M",
    description: "Turns on fan for circulation when there is a difference between temperature sensors and thermostat.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Temperature diffference to turn on fan") {
        input "max_delta", "number", required: true, title: "Max temp difference"
        input "hysteresis", "number", required: true, title: "Hysteresis", default: 0
    }
    section("Fans") {
        input "thermostat_fans", "capability.thermostat", required: false, title: "Thermostat Fans", multiple: true
        input "switch_fans", "capability.switch", required: false, title: "Switched Fans", multiple: true
    }
    section("Temperature sensors") {
        input "temps", "capability.temperatureMeasurement", required: true, title: "Sensors", multiple: true
    }
}


def installed() {
    // log.debug "Installed with settings: ${settings}"
    initialize()
}


def updated() {
    // log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}


def initialize() {
    temps.each() {
        subscribe(it, "temperature", temperatureChangeHandler)
    }
    checkTemperatures()
}


def temperatureChangeHandler(evt) {
    // log.debug "Temperature change: ${evt}"
    checkTemperatures()
}


def fansOn() {
    // log.debug("fansOn")
    thermostat_fans.each() {
        if (it.currentValue("thermostatFanMode") != "on") {
            // log.debug("Turning thermostat fan on")
            it.fanOn()
        }
    }
    switch_fans.each() {
        if (it.currentValue("switch") != "on") {
            // log.debug("Turning switched fan on")
            it.on()
        }
    }
}


def fansOff() {
    // log.debug("fansOff")
    thermostat_fans.each() {
        if (it.currentValue("thermostatFanMode") != "auto") {
            // log.debug("Turning thermostat fan auto")
            it.fanAuto()
        }
    }
    switch_fans.each() {
        if (it.currentValue("switch") != "off") {
            // log.debug("Turning switched fan off")
            it.off()
        }
    }
}


def checkTemperatures() {
    // log.debug "check"
    //def temp_1 = 0//thermostat.currentValue("temperature")
    def max_diff = 0//temp_diff.abs()
    temps.each() {
        def temp_1 = it.currentValue("temperature")
        log.debug "temp_1 ${temp_1}"
        temps.each() { it2 ->
            def temp_diff = it2.currentValue("temperature") - temp_1
            temp_diff = temp_diff.abs()
            // log.debug "Temp diff: ${temp_diff}"
            if (temp_diff > max_diff) {
                max_diff = temp_diff;
                log.debug "Max diff: ${max_diff}"
            }
        }
    }

    if (max_diff > max_delta) {
        fansOn()
    } else if (max_diff < (max_delta - hysteresis)) {
        fansOff()
    } else {
        // log.debug "Leave fan alone"
    }
}
