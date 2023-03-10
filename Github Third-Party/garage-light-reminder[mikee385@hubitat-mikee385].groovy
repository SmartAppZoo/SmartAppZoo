/**
 *  Garage Light Reminder
 *
 *  Copyright 2020 Michael Pierce
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
String getVersionLabel() { return "Garage Light Reminder, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Garage Light Reminder",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Sends reminders if the garage light is left on.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/garage-light-reminder.groovy")

preferences {
    page(name: "settings", title: "Garage Light Reminder", install: true, uninstall: true) {
        section {
            input "light", "capability.switch", title: "Light", multiple: false, required: true
            input "doors", "capability.contactSensor", title: "Doors", multiple: true, required: true
        }
        section {
            input "initialDuration", "number", title: "Time before Initial Reminder (in minutes)", required: true
            input "repeatDuration", "number", title: "Time between Repeated Reminders (in minutes)", required: true
        }
        section {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
            input "message", "text", title: "Message Text", multiple: false, required: true
        }
        section {
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
    subscribe(light, "switch", handler)
    for (door in doors) {
        subscribe(door, "contact", handler)
    }
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def handler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    unschedule()
    if (light.currentValue("switch") == "on") {
            runIn(60*initialDuration, reminder)
    }
}

def reminder() {
    notifier.deviceNotification(message)
    runIn(60*repeatDuration, reminder)
}