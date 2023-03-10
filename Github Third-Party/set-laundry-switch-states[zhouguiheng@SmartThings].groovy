/**
 *  Set laundry switch states according to power meters.
 *
 *  Copyright 2017 Vincent
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
    name: "Set Laundry Switch States",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Set the states of laundry switches according to power meters.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Washer:") {
		input "washer", "capability.switch", required: true
        input "minWasherPower", "number", title: "Minimum watts to turn on:", defaultValue: "20"
        input "delayOffMinutes", "number", title: "Minutes at low power before turning off:", defaultValue: "3"
	}
	section("Dryer:") {
		input "dryer", "capability.switch", required: true
        input "minDryerPower", "number", title: "Minimum watts to turn on:", defaultValue: "20"
	}
    section("Power meter:") {
    	input "power", "capability.powerMeter", required: true
        input "washerAttribute", "string", title: "Attribute for washer:", defaultValue: "powerOne"
        input "dryerAttribute", "string", title: "Attribute for dryer:", defaultValue: "powerTwo"
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(power, washerAttribute, washerHandler)
	subscribe(power, dryerAttribute, dryerHandler)
    state.tryingWasherOff = false
}

def washerHandler(evt) {
  try {
    def p = evt.doubleValue
    log.debug "Washer power: ${p}"
    if (p >= minWasherPower) {
      state.tryingWasherOff = false
      unschedule(washerOff)
      washerOn()
    } else {
      if (washer.currentSwitch == "on" && !state.tryingWasherOff) {
        log.debug "Trying washer off"
        state.tryingWasherOff = true
        runIn(delayOffMinutes * 60, washerOff)
      }
    }
  } catch (e) {
    log.debug("Failed to get double value for ${evt.name}", e)
  }
}

def washerOn() {
  if (washer.currentSwitch != "on") {
    log.debug "Turn on washer"
    washer.on()
  }
}

def washerOff() {
  state.tryingWasherOff = false
  if (washer.currentSwitch != "off") {
    log.debug "Turn off washer"
    washer.off()
  }
}

def dryerHandler(evt) {
  try {
    def p = evt.doubleValue
    log.debug "Dryer power: ${p}"
    if (p >= minDryerPower) {
      if (dryer.currentSwitch != "on") {
        log.debug "Turn on dryer"
        dryer.on()
      }
    } else {
      if (dryer.currentSwitch != "off") {
        log.debug "Turn off dryer"
        dryer.off()
      }
    }
  } catch (e) {
    log.debug("Failed to get double value for ${evt.name}", e)
  }
}