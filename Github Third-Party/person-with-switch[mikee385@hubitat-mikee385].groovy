/**
 *  Person Automation with Switch
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
 
String getVersionNum() { return "2.1.0-beta.3" }
String getVersionLabel() { return "Person Automation with Switch, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Person Automation with Switch",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Updates the state of a Person Status device using a presence sensor and a switch.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/person-with-switch.groovy")

preferences {
    page(name: "settings", title: "Person Automation with Switch", install: true, uninstall: true) {
        section {
            input "person", "device.PersonStatus", title: "Person Status", multiple: false, required: true
            input "presenceSensor", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: true
            input "switchSensor", "capability.switch", title: "Switch", multiple: false, required: true
        }
        section("Alerts") {
            input "alertArrived", "bool", title: "Alert when Arrived?", required: true, defaultValue: false
            input "alertDeparted", "bool", title: "Alert when Departed?", required: true, defaultValue: false
            input "alertAwake", "bool", title: "Alert when Awake?", required: true, defaultValue: false
            input "alertAsleep", "bool", title: "Alert when Asleep?", required: true, defaultValue: false
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
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
    // Person Status
    subscribe(presenceSensor, "presence", presenceHandler_PersonStatus)
    subscribe(switchSensor, "switch", switchHandler_PersonStatus)
    
    // Switch
    subscribe(location, "mode", modeHandler_Switch)
    
    // Away Alert
    subscribe(switchSensor, "switch.on", handler_AwayAlert)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def presenceHandler_PersonStatus(evt) {
    logDebug("presenceHandler_PersonStatus: ${evt.device} changed to ${evt.value}")

    if (presenceSensor.currentValue("presence") == "present") {
        if (person.currentValue("presence") == "not present") {
            person.arrived()
            if (alertArrived) {
                notifier.deviceNotification("$person is home!")
            }
        }
    } else {
        if (person.currentValue("presence") == "present") {
            person.departed()
            if (alertDeparted) {
                notifier.deviceNotification("$person has left!")
            }
        }
    }
}

def switchHandler_PersonStatus(evt) {
    logDebug("switchHandler_PersonStatus: ${evt.device} changed to ${evt.value}")
    
    if (switchSensor.currentValue("switch") == "on") {
        if (person.currentValue("state") == "home") {
            person.asleep()
            if (alertAsleep) {
                notifier.deviceNotification("$person is asleep!")
            }
        }
    } else {
        if (person.currentValue("state") == "sleep") {
            person.awake()
            if (alertAwake) {
                notifier.deviceNotification("$person is awake!")
            }
        }
    }
}

def modeHandler_Switch(evt) {
    logDebug("modeHandler_Switch: ${evt.device} changed to ${evt.value}")
    
    if (evt.value == "Away") {
        switchSensor.off()
    }
}

def handler_AwayAlert(evt) {
    logDebug("handler_AwayAlert: ${evt.device} changed to ${evt.value}")
    
    if (location.mode == "Away") {
        notifier.deviceNotification("${evt.device} is ${evt.value} while Away!")
    }
}