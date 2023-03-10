/**
 *  Lights Controller
 *
 *  Copyright 2017 Nick Yantikov
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
    name: "Lights Controller",
    namespace: "ovonick",
    author: "Nick Yantikov",
    description: "Control Lights with switches, motion sensors, illuminance sensor, and more",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Select Devices and enter parameters") {
        input "switches",          "capability.switch",                 title: "Switch Devices",      required: true,  multiple: true
        input "motions",           "capability.motionSensor",           title: "Motion Sensors",      required: false, multiple: true
        input "illuminanceDevice", "capability.illuminanceMeasurement", title: "Illuminance Device",  required: false, multiple: false
        input "minutes",           "number",                            title: "Minutes to turn off", required: true,                  defaultValue: 1
        input "illuminance",       "number",                            title: "Illuminance",         required: false,                 defaultValue: 0, range: "0..10000"
    }
}

def installed() {
    log.debug "${app.label}, installed()"
    initialize()
}

def updated() {
    log.debug "${app.label}, updated()"
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "${app.label}, initialize()"

    subscribe(motions,  "motion.active",   motionActiveHandler)
    subscribe(motions,  "motion.inactive", motionInactiveHandler)
    subscribe(switches, "switch.on",       switchOnHandler)
}

def motionActiveHandler(event) {
    log.debug "${app.label}, motionActiveHandler"

    state.isMotionActive = true
    
    if (illuminanceDevice) {
        def currentIlluminance = illuminanceDevice.currentValue("illuminance")
        log.debug "${app.label}, current illuminance: ${currentIlluminance}"
        
        if (currentIlluminance <= illuminance) {
            switchesOn()
        }
    } else {
        switchesOn()
    }
}

def motionInactiveHandler(event) {
    log.debug "${app.label}, motionInactiveHandler"

    state.isMotionActive = false
    requestToTurnOff()
}

def switchOnHandler(event) {
    log.debug "${app.label}, switchOnHandler, event: ${event.value}, event.physical: ${event.physical}"

    if (!event.physical) {
        return
    }
    
    // Motion sensors may not have picked up motion when switch was turned on.
    // If switch was turned on we also assume there was a motion (that is if there are motion sensors at all)
    if (motions) {
        state.isMotionActive = true
    } else {
        state.isMotionActive = false
    }
        
    requestToTurnOff()
}

def requestToTurnOff() {
    def delaySeconds = minutes * 60

    log.debug "${app.label}, requestToTurnOff(), delaySeconds: ${delaySeconds}"

    if (delaySeconds == 0) {
        switchesOff()
    } else {
        runIn(delaySeconds, switchesOff)
    }
}

def switchesOff(event) {
    switchesOff()
}

def switchesOff() {
    log.debug "${app.label}, switchesOff(), state.isMotionActive: ${state.isMotionActive}"

    if (state.isMotionActive) {
        return
    }

    switches.off()
}

def switchesOn() {
    log.debug "${app.label}, switchesOn(), state.isMotionActive: ${state.isMotionActive}"

    switches.on()
}