/**
 *  Person Automation with URLs
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
String getVersionLabel() { return "Person Automation with URLs, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Person Automation with URLs",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Updates the state of a Person Status device using a presence sensor and URLs.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/person-with-urls.groovy")

preferences {
    page(name: "settings", title: "Person Automation with URLs", install: true, uninstall: true) {
        section {
            input "person", "device.PersonStatus", title: "Person Status", multiple: false, required: true
            
            input "presenceSensor", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: true
            
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

mappings {
    path("/awake") {
        action: [
            GET: "awakeUrlHandler"
        ]
    }
    path("/asleep") {
        action: [
            GET: "asleepUrlHandler"
        ]
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
    subscribe(presenceSensor, "presence", presenceHandler)
    
    if(!state.accessToken) {
        createAccessToken()
    }
    state.awakeUrl = "${getFullLocalApiServerUrl()}/awake?access_token=$state.accessToken"
    state.asleepUrl = "${getFullLocalApiServerUrl()}/asleep?access_token=$state.accessToken"
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def presenceHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")

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

def awakeUrlHandler() {
    logDebug("Awake URL called")
    
    if (person.currentValue("state") == "sleep") {
        person.awake()
        if (alertAwake) {
            notifier.deviceNotification("$person is awake!")
        }
    }
}

def asleepUrlHandler() {
    logDebug("Asleep URL called")
    
    if (person.currentValue("state") == "home") {
        person.asleep()
        if (alertAsleep) {
            notifier.deviceNotification("$person is asleep!")
        }
    }
}