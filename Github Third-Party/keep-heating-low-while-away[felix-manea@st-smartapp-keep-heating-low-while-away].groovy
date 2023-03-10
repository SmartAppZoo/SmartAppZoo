/**
 *  Copyright 2020 Felix Manea
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
 *  Adaptation of "It's Too Cold" by SmartThings (https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/smartapps/smartthings/its-too-cold.src/its-too-cold.groovy)
 *
 *  Author: Felix Manea
 */
definition(
        name: "Keep heating low while away",
        namespace: "smartthings",
        author: "SmartThings",
        description: "Monitor the temperature and when it drops below your setting get a text and/or turn on a heater or additional appliance.",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png",
        pausable: true
)

preferences {
    section("Devices") {
        input "presenceSensor", "capability.presenceSensor", title: "Presence sensor(s):", multiple: true
        input "temperatureSensor", "capability.temperatureMeasurement", title: "Temperature sensor:"
        input "heaterSwitch", "capability.switch", title: "Heater switch:"
    }
    section("Settings") {
        input "temperatureMin", "number", title: "Turn on when temperature under:"
        input "temperatureMax", "number", title: "Turn off when temperature over:"
    }
    section("Notifications") {
        input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], defaultValue: "No"
    }
}

def installed() {
    subscribe(temperatureSensor, "temperature", temperatureHandler)
    subscribe(presenceSensor, "presence", presenceHandler)
}

def updated() {
    unsubscribe()
    subscribe(temperatureSensor, "temperature", temperatureHandler)
    subscribe(presenceSensor, "presence", presenceHandler)
}

def temperatureHandler(evt) {
    def tooCold = temperatureMin
    def tooHot = temperatureMax
    def presenceState = presenceSensor.currentState("presence")
    def tempScale = location.temperatureScale ?: "C"

    // Check if any presence sensor.
    if (presenceState.value.find { it == 'present' } == 'present') {
        log.debug "Users present, turning heater back on and exit"

        heaterSwitch.on()

        return
    }

    log.debug "No users present, checking states"

    // If too cold.
    if (evt.doubleValue <= tooCold) {
        log.debug "Temperature dropped below $tooCold, activating $heaterSwitch"
        heaterSwitch.on()
        send("${temperatureSensor.displayName} is too cold (temperature: ${evt.value}${evt.unit ?: tempScale}), turning heating on (if not already on).")
    }

    // If too hot.
    if (evt.doubleValue >= tooHot) {
        log.debug "Temperature dropped below $tooCold, activating $heaterSwitch"
        heaterSwitch.off()
        send("${temperatureSensor.displayName} is too hot (temperature: ${evt.value}${evt.unit ?: tempScale}), turning heating off.")
    }
}

def presenceHandler(evt) {
    log.debug "presenceHandler(${evt.id}, ${evt.value})"

    // Check if presence sensor arrived or left.
    if (evt.value == "present") {
        log.debug "${evt.id} has arrived at the ${location}, turning heater on"
        heaterSwitch.on()
    }

    // If not present do nothing until temperature gets modified.
}

private send(msg) {
    if (sendPushMessage == "Yes") {
        log.debug("sending push message")
        sendPush(msg)
    }

    log.debug msg
}