/**
 *  Bathroom Fan Automation
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
 
String getVersionNum() { return "1.0.0-beta14" }
String getVersionLabel() { return "Bathroom Fan Automation, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Bathroom Fan Automation",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Turns a bathroom exhaust fan on/off using a nearby humidity sensor.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/bathroom-fan-automation.groovy")

preferences {
    page(name: "settings", title: "Bathroom Fan Automation", install: true, uninstall: true) {
        section {
            input "bathroomSensor", "capability.relativeHumidityMeasurement", title: "Bathroom Humidity Sensor", multiple: false, required: true
        }
        section {
            input "bathroomFan", "capability.switch", title: "Bathroom Fan", multiple: false, required: true
            input "manualRuntime", "number", title: "Manual runtime (in minutes)", required: true
            input "maximumRuntime", "number", title: "Maximum runtime (in minutes)", required: true
        }
        section("Rapid Change") {
            input "rapidIncrease", "decimal", title: "Humidity increase for shower start", required: true
            input "rapidDecrease", "decimal", title: "Humidity decrease for shower end", required: true
            input "rapidTime", "number", title: "Time period to check for rapid change (in minutes)", required: true
        }
        section("Baseline") {
            input "baselineSensor", "capability.relativeHumidityMeasurement", title: "Baseline Humidity Sensor", multiple: false, required: true
            input "baselineIncrease", "decimal", title: "Humidity above baseline for fan on", required: true
            input "baselineDecrease", "decimal", title: "Humidity above baseline for fan off", required: true
        }
        section("Threshold") {
            input "thresholdIncrease", "decimal", title: "Humidity threshold for fan on", required: true
            input "thresholdDecrease", "decimal", title: "Humidity threshold for fan off", required: true
        }
        section ("Notifications") {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
            input "prefix", "text", title: "Message Prefix", multiple: false, required: true
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
    state.runningState = "off"
    
    state.humidityActive =  false
    state.rapidState = "off"
    state.baselineState = "below"
    state.thresholdState = "below"

    state.previousHumidity = bathroomSensor.currentValue("humidity")
    state.currentHumidity = bathroomSensor.currentValue("humidity")

    subscribe(bathroomSensor, "humidity", humidityHandler)
    subscribe(bathroomFan, "switch", switchHandler)
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def switchHandler(evt) {
    if (evt.value == "on") {
        turnOn()
    } else {
        turnOff()
    }
}

def humidityHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    state.previousHumidity = state.currentHumidity
    state.currentHumidity = bathroomSensor.currentValue("humidity")
    
    checkRapidChange()
    checkBaseline()
    checkThreshold()
}

def checkRapidChange() {
    if (state.currentHumidity >= state.previousHumidity + rapidIncrease) {
        if (state.rapidState != "rising") {
            state.rapidState = "rising"
            
            notifier.deviceNotification(prefix + " - Rapid Increase")
            logDebug("Rapid Increase")
            
            turnOnHumidity()
        }
    } else if (state.currentHumidity <= state.previousHumidity - rapidDecrease) {
        if (state.rapidState == "rising") {
            state.rapidState = "falling"
            
            notifier.deviceNotification(prefix + " - Rapid Decrease")
            logDebug("Rapid Decrease")
        }
    } else {
        if (state.rapidState == "falling") {
            state.rapidState = "off"
            
            notifier.deviceNotification(prefix + " - Rapid Finished")
            logDebug("Rapid Finished")
            
            turnOffHumidity()
        }
    }
}

def checkBaseline() {
    def baselineHumidity = baselineSensor.currentValue("humidity")

    if (state.currentHumidity >= baselineHumidity + baselineIncrease) {
        if (state.baselineState == "below") {
            state.baselineState = "above"
            
            notifier.deviceNotification(prefix + " - Baseline Increase")
            logDebug("Baseline Increase")
            
            turnOnHumidity()
        }
    } else if (state.currentHumidity <= baselineHumidity + baselineDecrease) {
        if (state.baselineState == "above") {
            state.baselineState = "below"
            
            notifier.deviceNotification(prefix + " - Baseline Decrease")
            logDebug("Baseline Decrease")
            
            turnOffHumidity()
        }
    }
}

def checkThreshold() {
    if (state.currentHumidity >= thresholdIncrease) {
        if (state.thresholdState == "below") {
            state.thresholdState = "above"
            
            notifier.deviceNotification(prefix + " - Threshold Increase")
            logDebug("Threshold Increase")
            
            turnOnHumidity()
        }
    } else if (state.currentHumidity <= thresholdDecrease) {
        if (state.thresholdState == "above") {
            state.thresholdState = "below"
            
            notifier.deviceNotification(prefix + " - Threshold Decrease")
            logDebug("Threshold Decrease")
            
            turnOffHumidity()
        }
    }
}

def turnOnHumidity() {
    if (state.humidityActive == false) {
        state.humidityActive = true
        
        if (bathroomFan.currentValue("switch") == "off") {
            bathroomFan.on()
        } else {
            turnOn()
        }
    }
}

def turnOffHumidity() {
    if (state.humidityActive == true) {
        state.humidityActive = false
        
        if (bathroomFan.currentValue("switch") == "on") {
            bathroomFan.off()
        } else {
            turnOff()
        }
    }
}

def turnOn() {
    if (state.humidityActive == true) {
        if (state.runningState != "humidity") {
            unschedule("turnOff")
            state.runningState = "humidity"
            runIn(60*maximumRuntime, turnOffHumidity)
            
            notifier.deviceNotification(prefix + " - Fan Turned On - Humidity")
            logDebug("Fan Turned On - Humidity")
        }
    } else {
        if (state.runningState == "off") {
            state.runningState = "manual"
            runIn(60*manualRuntime, turnOff)
            
            notifier.deviceNotification(prefix + " - Fan Turned On - Manual")
            logDebug("Fan Turned On - Manual")
        }
    }
}

def turnOff() {
    if (state.runningState != "off") {
        unschedule("turnOff")
        state.runningState = "off"
    
        notifier.deviceNotification(prefix + " - Fan Turned Off")
        logDebug("Fan Turned Off")
    }
}