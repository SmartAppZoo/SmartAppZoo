/**
 *  Person Automation with Power Meter
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
 
String getVersionNum() { return "2.1.1" }
String getVersionLabel() { return "Person Automation with Power Meter, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Person Automation with Power Meter",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Updates the state of a Person Status device using a presence sensor and a power meter.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/person-with-power.groovy")

preferences {
    page(name: "settings", title: "Person Automation with Power Meter", install: true, uninstall: true) {
        section {
            input "person", "device.PersonStatus", title: "Person Status", multiple: false, required: true
            input "presenceSensor", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: true
            input "powerMeter", "capability.powerMeter", title: "Power Meter", multiple: false, required: true
            input "powerLevelForSleep", "decimal", title: "Power Level for Sleep", required: true
            input "guest", "capability.presenceSensor", title: "Guest", multiple: false, required: true
            input "bedroomDoor", "capability.contactSensor", title: "Bedroom Door", multiple: false, required: true
            input "exteriorDoors", "capability.contactSensor", title: "Exterior Doors", multiple: true, required: true
            input "backupButton", "capability.pushableButton", title: "Backup Button", multiple: false, required: false
        }
        section("Notifications") {
        
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
    state.wakingUp = false

    subscribe(presenceSensor, "presence", presenceHandler)
    
    subscribe(powerMeter, "power", powerMeterHandler)
    
    subscribe(bedroomDoor, "contact", bedroomDoorHandler)
    for (door in exteriorDoors) {
        subscribe(door, "contact", exteriorDoorHandler)
    }
    
    if (backupButton) {
        subscribe(backupButton, "pushed", exteriorDoorHandler)
    }
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def presenceHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    state.wakingUp = false
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

def powerMeterHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (powerMeter.currentValue("power") >= powerLevelForSleep) {
        if (person.currentValue("state") == "home") {
            person.asleep()
            if (alertAsleep) {
                notifier.deviceNotification("$person is asleep!")
            }
        } else if (state.wakingUp == true) {
            state.wakingUp = false
            if (alertAsleep) {
                notifier.deviceNotification("$person went back to bed!")
            }
        }
    }
}

def bedroomDoorHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (guest.currentValue("presence") == "not present") {
        if (powerMeter.currentValue("power") < powerLevelForSleep && person.currentValue("state") == "sleep" && state.wakingUp == false) {
            state.wakingUp = true
            if (alertAwake) {
                notifier.deviceNotification("$person is out of bed!")
            }
        }
    }
}

def exteriorDoorHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (guest.currentValue("presence") == "not present") {
        if (state.wakingUp == true || bedroomDoor.currentValue("contact") == "open") {
            state.wakingUp = false
            if (person.currentValue("state") == "sleep") {
                person.awake()
                if (alertAwake) {
                    notifier.deviceNotification("$person is awake!")
                }
            }
        }
    } else {
        if (powerMeter.currentValue("power") < powerLevelForSleep && person.currentValue("state") == "sleep") {
            person.awake()
            if (alertAwake) {
                notifier.deviceNotification("$person is awake!")
            }
        }
    }
}