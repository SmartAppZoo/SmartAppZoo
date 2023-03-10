/*
 * Copyright (C) 2014 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * On/Off Motion
 *
 * This application will turn on devices when there is motion and after a
 * specified time (or 10 minutes if not specified)
 * will turn off the devices
 */
preferences {
    section("When there's movement...") {
        input "motion1", "capability.motionSensor", title: "Where?"
    }

    section("Turn on...") {
        input "switch1", "capability.switch", multiple: true
    }

    section("Additional settings", hideable: true, hidden: true) {
        paragraph("Default timeout is 10 Minutes")
        input "turnOffTime", "decimal", title: "Turn off in... (minutes)",
                description: "Enter time in minutes", defaultValue: 10, required: false
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
    subscribe(motion1, "motion.active", motionActiveHandler)
}

def motionActiveHandler(evt) {
    switch1.on()
    def delayInMinutes = 60 * findturnOffTime()
    log.debug "Delay set to ${delayInMinutes}"
    runIn(delayInMinutes, turnOffSwitch)
}

def turnOffSwitch() {
    switch1.off()
}

private findturnOffTime() {
    (turnOffTime != null && turnOffTime != "") ? turnOffTime : 10
}