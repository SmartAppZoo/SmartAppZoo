/**
 *  Washer Automation
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
String getVersionLabel() { return "Washer Automation, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Washer Automation",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Updates the state of an Appliance Status device representing a washing machine.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/washer-automation.groovy")

preferences {
    page(name: "settings", title: "Washer Automation", install: true, uninstall: true) {
        section {
            input "appliance", "device.ApplianceStatus", title: "Washer Status", multiple: false, required: true
        }
        section {
            input "powerMeter", "capability.powerMeter", title: "Power Meter", multiple: false, required: true
            input "powerLevelWhileRunning", "decimal", title: "Power Level while Running", required: true
        }
        section {
            input "resetTime", "time", title: "Reset Time", required: true
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
    subscribe(powerMeter, "power", powerMeterHandler)
    
    def resetToday = timeToday(resetTime)
    def currentTime = new Date()
    schedule("$currentTime.seconds $resetToday.minutes $resetToday.hours * * ? *", dailyReset)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def powerMeterHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (powerMeter.currentValue("power") >= powerLevelWhileRunning) {
        if (appliance.currentValue("state") != "running") {
            appliance.start()
        }
    } else {
        if (appliance.currentValue("state") == "running") {
            appliance.finish()
        }
    }
}

def dailyReset() {
    logDebug("Received daily reset time")
    
    if (appliance.currentValue("state") == "finished") {
        appliance.reset()
    }
}