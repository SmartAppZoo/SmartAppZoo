/**
 *  Camera Reminder Triggers
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
String getVersionLabel() { return "Camera Reminder Triggers, version ${getVersionNum()} on ${getPlatform()}" }

definition(
    name: "Camera Reminder Triggers",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Triggers to turn on/off the reminder to turn off the cameras.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/camera-reminder-triggers.groovy")

preferences {
    page(name: "settings", title: "Camera Reminder Triggers", install: true, uninstall: true) {
        section {
            input "reminderSwitch", "capability.switch", title: "Reminder Switch", multiple: false, required: true
        }
        section {
            input "person", "device.PersonStatus", title: "Person", multiple: false, required: true
            
            input "cameras", "capability.switch", title: "Cameras", multiple: true, required: true
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
    subscribe(person, "state", stateHandler)

    for (camera in cameras) {
        subscribe(camera, "switch", cameraHandler)
    }
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def stateHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")

    if (evt.value == "home") {
        runIn(5, checkCameras)
    } else {
        reminderSwitch.off()
    }
}

def checkCameras() {
    def anyCameraOn = false
    for (camera in cameras) {
        if (camera.currentValue("switch") == "on") {
            anyCameraOn = true
            break
        }
    }
    if (anyCameraOn == true) {
        reminderSwitch.on()
    }
}

def cameraHandler(evt) {
    logDebug("${evt.device} changed to ${evt.value}")
    
    if (evt.value == "on") {
        if (person.currentValue("state") == "home") {
            reminderSwitch.on()
        }
    } else {
        def allCameraOff = true
        for (camera in cameras) {
            if (camera.currentValue("switch") == "on") {
                allCameraOff = false
                break
            }
        }
        if (allCameraOff == true) {
            reminderSwitch.off()
        }
    }
}