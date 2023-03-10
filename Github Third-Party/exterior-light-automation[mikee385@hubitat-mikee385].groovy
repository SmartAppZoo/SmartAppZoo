/**
 *  Exterior Light Automation
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
 
String getVersionNum() { return "1.0.0-beta.10" }
String getVersionLabel() { return "Exterior Light Automation, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Exterior Light Automation",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Turns on an exterior light when a door opens, depending on the time of day.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/exterior-light-automation.groovy")

preferences {
    page(name: "settings", title: "Exterior Light Automation", install: true, uninstall: true) {
        section {
            input "exteriorLights", "capability.switch", title: "Exterior Lights", multiple: true, required: true

            input "exteriorDoor", "capability.contactSensor", title: "Exterior Door", multiple: false, required: true
            
            input "sunlight", "capability.switch", title: "Sunlight", multiple: false, required: true
        }
        section("Reminder") {
            input "person", "device.PersonStatus", title: "Person", multiple: false, required: true
            
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
    subscribe(exteriorDoor, "contact", exteriorDoorHandler)
    
    subscribe(sunlight, "switch.on", sunlightHandler)
    
    subscribe(location, "mode", modeHandler)
    
    subscribe(person, "state", personHandler)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def exteriorDoorHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (evt.value == "open") {
        if (sunlight.currentValue("switch") == "off") {
            for (light in exteriorLights) {
                light.on()
            }
        }
    
        runIn(60*5, reminderAlert)
    } else {
        unschedule("reminderAlert")
    }
}

def sunlightHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    for (light in exteriorLights) {
        light.off()
    }
}

def modeHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (evt.value == "Sleep") {
        for (light in exteriorLights) {
            light.off()
        }
    }
}

def reminderAlert() {
    notifier.deviceNotification("Should the $exteriorDoor still be open?")
    runIn(60*30, reminderAlert)
}

def personHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")

    if (evt.value != "home") {
        unsubscribe("reminderAlert")
        
        if (exteriorDoor.currentValue("contact") == "open") {
            notifier.deviceNotification("$exteriorDoor is still open!")
        }
    }
}