/**
 *  Alert on Power Consumption
 *
 *  Copyright 2016 Anders Sveen
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
 * Based on: https://github.com/sudarkoff/SmartThings/blob/master/BetterLaundryMonitor.groovy
 */

import groovy.time.*

definition(
        name: "Laundry Monitor",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen",
        description: "Using a switch with powerMonitor capability, monitor the laundry cycle and alert when it's done.",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn.png",
        iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Appliances/appliances8-icn@2x.png")


preferences {
    section("When this device stops drawing power") {
        input "meter", "capability.powerMeter", multiple: false, required: true
        input "cycle_start_power_threshold", "number", title: "Start cycle when power consumption goes above (W)", required: true
        input "cycle_end_power_threshold", "number", title: "Stop cycle when power consumption drops below (W) ...", required: true
        input "cycle_end_wait", "number", title: "... for at least this long (min)", required: true
    }

    section("Send this message") {
        input "message", "text", title: "Notification message", description: "Laudry is done!", required: true
        input "phone", "phone", title: "SMS Notification", description: "Optional phone number to send to", required: false
    }

    section("Flash lights (optional)") {
        input "switches", "capability.switch", title: "These lights", multiple: true, required: false
        input "numFlashes", "number", title: "This number of times (default 3)", required: false
        input "onFor", "number", title: "On for milliseconds (default 1000)", required: false
        input "offFor", "number", title: "Off for milliseconds (default 1000)", required: false
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
    subscribe(meter, "power", handler)
}

def handler(evt) {
    def latestPower = meter.currentValue("power")
    log.trace "Power: ${latestPower}W"

    if (!state.cycleOn && latestPower > cycle_start_power_threshold) {
        cycleOn(evt)
    } else if (state.cycleOn && latestPower <= cycle_end_power_threshold) {
        // If power drops below threshold, wait for a few minutes.
        log.debug "Power below threshold. Scheduling end cycle"
        runIn(cycle_end_wait * 60, cycleOff)
    }
}

private cycleOn(evt) {
    state.cycleOn = true
    log.debug "Cycle started."
}

def cycleOff(evt) {
    def latestPower = meter.currentValue("power")
    log.trace "Power: ${latestPower}W"

    // If power is still below threshold, end cycle.
    if (state.cycleOn && latestPower <= cycle_end_power_threshold) {
        state.cycleOn = false
        log.debug "Cycle ended."

        sendPush(message)
        if (phone) {
            sendSms(phone, message)
        }
        if (switches && switches.size() > 0) {
            flashLights()
        }
        unschedule()
    } else {
        log.debug "Cycle continuing as power is above threshold."
    }
}

private flashLights() {
    def onFor = onFor ?: 1000
    def offFor = offFor ?: 1000
    def numFlashes = numFlashes ?: 3

    log.debug "FLASHING $numFlashes times"
    def initialActionOn = switches.collect { it.currentSwitch != "on" }
    def delay = 0L
    numFlashes.times {
        log.trace "Switch on after $delay msec"
        switches.eachWithIndex { s, i ->
            if (initialActionOn[i]) {
                s.on(delay: delay)
            } else {
                s.off(delay: delay)
            }
        }
        delay += onFor
        log.trace "Switch off after $delay msec"
        switches.eachWithIndex { s, i ->
            if (initialActionOn[i]) {
                s.off(delay: delay)
            } else {
                s.on(delay: delay)
            }
        }
        delay += offFor
    }
}
