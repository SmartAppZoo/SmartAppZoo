/**
 *  Bedtime Routine
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
String getVersionLabel() { return "Bedtime Routine, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Bedtime Routine",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Trigger routine for bedtime.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/bedtime-routine.groovy")

preferences {
    page(name: "settings", title: "Bedtime Routine", install: true, uninstall: true) {
        section {
            input "routine", "capability.switch", title: "Routine", multiple: false, required: true
        }
        section("Trigger") {
            input "door", "capability.contactSensor", title: "Door", multiple: false, required: true
            input "startTime", "time", title: "Start Time", required: true
            input "endTime", "time", title: "End Time", required: true
        }
        section {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
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
    // Routine Switch
    subscribe(door, "contact.closed", doorHandler_RoutineSwitch)
    
    // Away Alert
    subscribe(door, "contact", handler_AwayAlert)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def doorHandler_RoutineSwitch(evt) {
    logDebug("doorHandler_RoutineSwitch: ${evt.device} changed to ${evt.value}")
    
    if (location.mode != "Away" && timeOfDayIsBetween(timeToday(startTime), timeToday(endTime), new Date(), location.timeZone)) {
        routine.on()
    }
}

def handler_AwayAlert(evt) {
    logDebug("handler_AwayAlert: ${evt.device} changed to ${evt.value}")
    
    if (location.mode == "Away") {
        notifier.deviceNotification("${evt.device} is ${evt.value} while Away!")
    }
}