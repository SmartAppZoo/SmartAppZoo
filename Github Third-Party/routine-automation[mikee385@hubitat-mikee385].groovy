/**
 *  Routine Automation
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
String getVersionLabel() { return "Routine Automation, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Routine Automation",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Turn switches on/off when a routine (switch) is activated.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/routine-automation.groovy")

preferences {
    page(name: "settings", title: "Routine Automation", install: true, uninstall: true) {
        section {
            input "routine", "capability.switch", title: "Routine", multiple: false, required: true
        }
        section {
            input "mode", "mode", title: "Set Mode To", multiple: false, required: false
        }
        section {
            input "switchOn", "capability.switch", title: "Switches to Turn On", multiple: true, required: false
            input "switchOff", "capability.switch", title: "Switches to Turn Off", multiple: true, required: false
        }
        section {
            input "notifier", "capability.notification", title: "Send Message To", multiple: false, required: false
            input "message", "text", title: "Message Text", multiple: false, required: false
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
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def routineHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (mode) {
        location.setMode(mode)
    }
    for (device in switchOn) {
        device.on()
    }
    for (device in switchOff) {
        device.off()
    }
    if (notifier && message) {
        notifier.deviceNotification(message)
    }
}