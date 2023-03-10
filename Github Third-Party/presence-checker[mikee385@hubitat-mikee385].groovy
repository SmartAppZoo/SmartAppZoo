/**
 *  Presence Checker
 *
 *  Copyright 2021 Michael Pierce
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
 
String getVersionNum() { return "1.0.0" }
String getVersionLabel() { return "Presence Checker, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Presence Checker",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Checks to make sure presence sensors are consistent and reliable.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/presence-checker.groovy")

preferences {
    page(name: "settings", title: "Presence Checker", install: true, uninstall: true) {
        section {
            input "person", "device.PersonStatus", title: "Person Status", multiple: false, required: true
            input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: true
        }
        section {
            input "personToNotify", "device.PersonStatus", title: "Person to Notify", multiple: false, required: true
            input name: "logEnable", type: "bool", title: "Enable debug logging?", defaultValue: false
            label title: "Assign a name", required: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    subscribe(person, "presence", personHandler)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def personHandler(evt) {
    logDebug("personHandler: ${evt.device} changed to ${evt.value}")

    runIn(30, presenceCheck)
}

def presenceCheck() {
    def presenceValue = person.currentValue("presence")
    for (presenceSensor in presenceSensors) {
        if (presenceSensor.currentValue("presence") != presenceValue) {
            personToNotify.deviceNotification("$presenceSensor failed to change to $presenceValue!")
        }
    }
}