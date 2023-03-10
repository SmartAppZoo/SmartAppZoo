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
 *  Author: Anders Sveen based on the SmartThings Virtual Thermostat
 */
definition(
        name: "Virtual Thermostat",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen",
        description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    section("Choose a temperature sensor... ") {
        input "sensor", "capability.temperatureMeasurement", title: "Sensor"
    }
    section("Select the heater or air conditioner outlet(s)... ") {
        input "outlets", "capability.switch", title: "Outlets", multiple: true
    }
    section("Set the desired temperature...") {
        input "setpoint", "decimal", title: "Set Temp"
    }
    section("Shut off heat when doors open (optional)") {
        input "doors", "capability.contactSensor", title: "Doors", multiple: true, required: false
    }
}

def subscribeAll() {
    subscribe(sensor, "temperature", temperatureHandler)
    if (doors) {
        subscribe(doors, "contact", doorHandler)
    }
}

def installed() {
    subscribeAll()
}

def updated() {
    unsubscribe()
    subscribeAll()
}

def doorHandler(evt) {
    log.debug("Door state changed '$evt.value'")
    evaluateTemperatureRules(sensor.currentTemperature, setpoint)
}

def temperatureHandler(evt) {
    log.debug("Temperature event is received")
    evaluateTemperatureRules(evt.doubleValue, setpoint)
}

def evaluateTemperatureRules(currentTemp, desiredTemp) {
    log.debug("Evaluating. Current: ${currentTemp}, Desired: ${desiredTemp}")
    if (isDoorsOpen()) {
        log.debug("Doors are open, so keeping everything off...")
        flipState("off")
    } else {
        def threshold = 0.5
        if (desiredTemp - currentTemp >= threshold) {
            log.debug("Current temp (${currentTemp}) is lower than desired (${desiredTemp}). Switching on.")
            flipState("on")
        } else if (currentTemp - desiredTemp >= threshold) {
            log.debug("Current temp (${currentTemp}) is higher than desired (${desiredTemp}). Switching off.")
            flipState("off")
        }
    }
}

def isDoorsOpen() {
    def openDoors = doors.findAll { it.currentState("contact").value == "open" }
    return openDoors.size() > 0
}

private flipState(desiredState) {
    def wrongState = outlets.findAll { outlet -> outlet.currentValue("switch") != desiredState }

    log.debug "FLIPSTATE: Found ${wrongState.size()} outlets in wrong state (Target state: $desiredState) ..."
    wrongState.each { outlet ->
        log.debug "Flipping '$outlet' ..."
        if (desiredState == "on") {
            log.debug "On"
            outlet.on()
        } else {
            log.debug "Off"
            outlet.off()
        }
    }
}
