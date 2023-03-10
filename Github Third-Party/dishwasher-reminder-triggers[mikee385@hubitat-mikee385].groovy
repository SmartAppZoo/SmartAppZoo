/**
 *  Dishwasher Reminder Triggers
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
 
String getVersionNum() { return "2.0.0" }
String getVersionLabel() { return "Dishwasher Reminder Triggers, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Dishwasher Reminder Triggers",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Triggers to turn on/off the reminder to start the dishwasher.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/dishwasher-reminder-triggers.groovy")

preferences {
    page(name: "settings", title: "Dishwasher Reminder Triggers", install: true, uninstall: true) {
        section {
            input "reminderSwitch", "capability.switch", title: "Reminder Switch", multiple: false, required: true
        }
        section("Turn On") {
            input "routine", "capability.switch", title: "Routine", multiple: false, required: true
        }
        section("Turn Off") {
            input "appliance", "device.ApplianceStatus", title: "Appliance", multiple: false, required: true
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
    subscribe(routine, "switch.on", routineHandler)
    
    subscribe(appliance, "state.running", runningHandler)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def routineHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (appliance.currentValue("state") == "unstarted") {
        reminderSwitch.on()
    }
}

def runningHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")

    reminderSwitch.off()
}