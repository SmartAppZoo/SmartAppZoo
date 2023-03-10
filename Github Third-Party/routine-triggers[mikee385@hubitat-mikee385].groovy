/**
 *  Routine Triggers
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
 
String getVersionNum() { return "1.0.1" }
String getVersionLabel() { return "Routine Triggers, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Routine Triggers",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Trigger routines based on presence sensors and sleep sensors.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/routine-triggers.groovy")

preferences {
    page(name: "settings", title: "Routine Triggers", install: true, uninstall: true) {
        section("Sensors") {
            input "presenceSleepSensors", "capability.sleepSensor", title: "Presence+Sleep Sensors", multiple: true, required: false
            
            input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
        }
        section("Routines") {
            input "arrivedWhenAway", "capability.switch", title: "Arrived when Away", multiple: false, required: true
            
            input "arrivedWhenAsleep", "capability.switch", title: "Arrived when Asleep", multiple: false, required: true
            
            input "departedWhenAway", "capability.switch", title: "Departed when Away", multiple: false, required: true
            
            input "departedWhenAsleep", "capability.switch", title: "Departed when Asleep", multiple: false, required: true
            
            input "awake", "capability.switch", title: "Awake", multiple: false, required: true
            
            input "asleep", "capability.switch", title: "Asleep", multiple: false, required: true
        }
        section("Backup Times") {
            input "awakeTime", "time", title: "Awake Time", required: false
            input "asleepTime", "time", title: "Asleep Time", required: false
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
    for (sensor in presenceSleepSensors) {
        subscribe(sensor, "presence.present", arrivedHandler)
        subscribe(sensor, "presence.not present", departedHandler)
        subscribe(sensor, "sleeping.sleeping", asleepHandler)
        subscribe(sensor, "sleeping.not sleeping", awakeHandler)
    }
    
    for (sensor in presenceSensors) {
        subscribe(sensor, "presence.present", arrivedHandler)
        subscribe(sensor, "presence.not present", departedHandler)
    }
    
    if (awakeTime) {
        schedule(awakeTime, awakeTimeHandler)
    }
    if (asleepTime) {
        schedule(asleepTime, asleepTimeHandler)
    }
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def arrivedHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (location.mode == "Away") {
        arrivedWhenAway.on()
    } else if (location.mode == "Sleep") {
        arrivedWhenAsleep.on()
    }
}

def departedHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    def anyonePresent = false
    def anyoneAwake = false
    for (sensor in presenceSleepSensors) {
        if (sensor.currentValue("presence") == "present") {
            anyonePresent = true
            if (sensor.currentValue("sleeping") == "not sleeping") {
                anyoneAwake = true
            }
        }
    }
    for (sensor in presenceSensors) {
        if (sensor.currentValue("presence") == "present") {
            anyonePresent = true
        }
    }
    if (anyonePresent == false) {
        if (location.mode != "Away") {
            departedWhenAway.on()
        }
    } else if (anyoneAwake == false) {
        if (location.mode != "Sleep") {
            departedWhenAsleep.on()
        }
    }
}

def asleepHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (evt.device.currentValue("presence") == "present") {
        def anyoneAwake = false
        for (sensor in presenceSleepSensors) {
            if (sensor.currentValue("presence") == "present" && sensor.currentValue("sleeping") == "not sleeping") {
                anyoneAwake = true
            }
        }
        if (anyoneAwake == false) {
            if (location.mode != "Sleep") {
                asleep.on()
            }
        }
    }
}

def awakeHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (evt.device.currentValue("presence") == "present") {
        if (location.mode == "Sleep") {
            awake.on()
        }
    }
}

def awakeTimeHandler(evt) {
    logDebug("Received awake time event")
    
    if (location.mode == "Sleep") {
        awake.on()
    }
}

def asleepTimeHandler(evt) {
    logDebug("Received asleep time event")
    
    if (location.mode == "Home") {
        asleep.on()
    }
}