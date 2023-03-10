/**
 *  HVAC Auto Off
 *
 *  Author: Brian Steere
 *  Code: https://github.com/smartthings-users/smartapp.hvac-auto-off
 *
 * Copyright (C) 2013 Brian Steere <dianoga7@3dgo.net>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
definition(
    name: "HVAC Auto Off",
    namespace: "",
    author: "Brian Steere",
    description: "Set thermostat Home/Away automatically",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Control") {
        input("thermostat", "capability.thermostat", title: "Thermostat")
    }

    section("Open/Close") {
        input("sensors", "capability.contactSensor", title: "Sensors", multiple: true)
        input("delay", "number", title: "Delay (minutes)")
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    state.changed = false
    subscribe(sensors, 'contact', "sensorChange")
}

def sensorChange(evt) {
    log.debug "Desc: $evt.value , $state"
    if(evt.value == 'open' && !state.changed) {
        unschedule()
        runIn(delay * 60, 'turnOff')
    } else if(evt.value == 'closed' && state.changed) {
        // All closed?
        def isOpen = false
        for(sensor in sensors) {
            if(sensor.id != evt.deviceId && sensor.currentValue('contact') == 'open') {
                isOpen = true
            }
        }

        if(!isOpen) {
            unschedule()
            runIn(delay * 60, 'restore')
        }
    }
}

def turnOff() {
    log.debug "Turning off thermostat due to contact open"
    state.thermostatMode = thermostat.currentValue("thermostatMode")
    thermostat.off()
    state.changed = true
    log.debug "State: $state"
}

def restore() {
    log.debug "Setting thermostat to $state.thermostatMode"
    thermostat.setThermostatMode(state.thermostatMode)
    state.changed = false
}