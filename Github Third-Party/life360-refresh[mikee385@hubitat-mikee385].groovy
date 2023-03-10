/**
 *  Life360 Refresh
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
 
String getVersionNum() { return "1.1.0" }
String getVersionLabel() { return "Life360 Refresh, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Life360 Refresh",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Refreshes Life360 if presence sensors are inconsistent.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/life360-refresh")

preferences {
    page(name: "settings", title: "Life360 Refresh", install: true, uninstall: true) {
        section {
            input "refreshButton", "device.ApplicationRefreshButton", title: "Refresh Button", multiple: false, required: true
            input "alertRefreshed", "bool", title: "Alert when refreshed?", required: true, defaultValue: false
        }
        section("Presence 1") {
            input "presence1", "capability.presenceSensor", title: "Life360 Presence", multiple: false, required: true
            input "otherPresence1", "capability.presenceSensor", title: "Other Presence Sensors", multiple: true, required: true
            input "alertInconsistent1", "bool", title: "Alert when inconsistent?", required: true, defaultValue: false
        }
        section("Presence 2") {
            input "presence2", "capability.presenceSensor", title: "Life360 Presence", multiple: false, required: true
            input "otherPresence2", "capability.presenceSensor", title: "Other Presence Sensors", multiple: true, required: true
            input "alertInconsistent2", "bool", title: "Alert when inconsistent?", required: true, defaultValue: false
        }
        section {
            input "person", "device.PersonStatus", title: "Person to Notify", multiple: false, required: true
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
    for (presenceSensor in otherPresence1) {
        subscribe(presenceSensor, "presence.present", presenceHandler_Presence1)
    }
    
    for (presenceSensor in otherPresence2) {
        subscribe(presenceSensor, "presence.present", presenceHandler_Presence2)
    }
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def presenceHandler_Presence1(evt) {
    logDebug("presenceHandler_Presence1: ${evt.device} changed to ${evt.value}")

    runIn(30, refresh_Presence1)
}

def refresh_Presence1() {
    if (presence1.currentValue("presence") != "present") {
        refreshButton.refresh()
        if (alertRefreshed) {
            person.deviceNotification("Refreshing Life360! ($presence1)")
        }
        if (alertInconsistent1) {
            runIn(60, alert_Presence1)
        }
    }
}

def alert_Presence1() {
    if (presence1.currentValue("presence") != "present") {
        person.deviceNotification("$presence1 may be incorrect!")
    }
}

def presenceHandler_Presence2(evt) {
    logDebug("presenceHandler_Presence2: ${evt.device} changed to ${evt.value}")

    runIn(30, refresh_Presence2)
}

def refresh_Presence2() {
    if (presence2.currentValue("presence") != "present") {
        refreshButton.refresh()
        if (alertRefreshed) {
            person.deviceNotification("Refreshing Life360! ($presence2)")
        }
        if (alertInconsistent2) {
            runIn(60, alert_Presence2)
        }
    }
}

def alert_Presence2() {
    if (presence2.currentValue("presence") != "present") {
        person.deviceNotification("$presence2 may be incorrect!")
    }
}