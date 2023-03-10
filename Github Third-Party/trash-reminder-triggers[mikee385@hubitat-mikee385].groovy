/**
 *  Trash Reminder Triggers
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
String getVersionLabel() { return "Trash Reminder Triggers, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Trash Reminder Triggers",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Triggers to turn on/off the reminder to take out the trash.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/trash-reminder-triggers.groovy")

preferences {
    page(name: "settings", title: "Trash Reminder Triggers", install: true, uninstall: true) {
        section {
            input "reminderSwitch", "capability.switch", title: "Reminder Switch", multiple: false, required: true
        }
        section("Turn On") {
            input "person", "device.PersonStatus", title: "Person", multiple: false, required: true
        }
        section("Turn Off") {
            input "overheadDoor", "capability.contactSensor", title: "Garage Door", multiple: false, required: true
            
            input "trashDays", "enum", title: "Trash Days", multiple: true, required: true, options: ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"]
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
    subscribe(person, "sleeping.not sleeping", awakeHandler)
    subscribe(person, "state", stateHandler)
    
    subscribe(overheadDoor, "contact", overheadDoorHandler)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def awakeHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (person.currentValue("state") == "home") {
        def df = new java.text.SimpleDateFormat("EEEE")
        df.setTimeZone(location.timeZone)

        def day = df.format(new Date())
        if (trashDays.contains(day)) {
            reminderSwitch.on()
        }
    }
}

def stateHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (person.currentValue("state") != "home") {
        if (reminderSwitch.currentValue("switch") == "on") {
            reminderSwitch.off()
        }
    }
}

def overheadDoorHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    reminderSwitch.off()
}