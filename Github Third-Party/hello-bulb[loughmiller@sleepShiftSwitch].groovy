/**
 *  Hello Bulb
 *
 *  Copyright 2016 Scott loughmiller
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
definition(
    name: "Hello Bulb",
    namespace: "loughmiller",
    author: "Scott loughmiller",
    description: "Set bulb to proper brightness when turned on.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
    section("Virtual Switch?") {
        input "virtualSwitch", "capability.switch", required: true
    }

    section("Lights?") {
        input "lLevel", "capability.switchLevel", title: "level", required: false, multiple: true
        input "lTemp", "capability.colorTemperature", title: "temp", required: false, multiple: true
        input "lSwitch", "capability.switch", title: "switch", required: false, multiple: true
    }

    section("Nightlight?") {
        input "nlLevel", "capability.switchLevel", title: "level", required: false, multiple: true
        input "nlTemp", "capability.colorTemperature", title: "temp", required: false, multiple: true
        input "nlSwitch", "capability.switch", title: "switch", required: false, multiple: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(virtualSwitch, "switch.on", switchOnHandler)
    subscribe(virtualSwitch, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
    log.info "switch on"
    def currentMode = location.mode
    log.info "mode: ${currentMode}"

    if (currentMode == "Bedtime") {
        log.info "switch turned on, and it's bedtime"
        nlLevel.setLevel(2)
        nlTemp.setColorTemperature(1900)
    } else if (currentMode == "Night") {
        log.info "switch turned on, and it's nighttime!"
        nlLevel.setLevel(100)
        nlTemp.setColorTemperature(2300)
    } else if (currentMode == "Evening") {
        log.info "switch turned on, and it's evening!"
        nlLevel.setLevel(100)
        nlTemp.setColorTemperature(2700)
        lLevel.setLevel(100)
        lTemp.setColorTemperature(2700)
    } else {
        log.info "switch turned on, and it's daytime!"
        nlLevel.setLevel(100)
        nlTemp.setColorTemperature(4500)
        lLevel.setLevel(100)
        lTemp.setColorTemperature(4500)
    }
}
def switchOffHandler(evt) {
    log.info "switch turned off!"
    nlTemp.setColorTemperature(1900)
    nlLevel.setLevel(1)
    // nlSwitch.off()

    lTemp.setColorTemperature(1900)
    lLevel.setLevel(1)
    // lSwitch.off()

    runIn(3, delayOff)
}

def delayOff() {
    nlLevel.setLevel(0)
    lLevel.setLevel(0)
}
