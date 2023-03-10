/**
 *  Turn on A when B is drawing power
 *
 *  Copyright 2019 Evan Weaver, copyright 2015 Keith Croshaw
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
    name: "Turn on A when B is drawing power",
    namespace: "evan",
    author: "Evan Weaver",
    description: "Turn on the subwoofers when the receiver is drawing power.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  	section ("Disable?") {
    	input "disableLogic", "bool", title: "Disable Logic?"
  	}
    section ("When this device is drawing power") {
    	input "meter", "capability.powerMeter", multiple: false, required: true
        input "DeviceRunning", "number", title: "Device running when power is above (W)", description: "8", required: true
        input "ShutOffDelay", "number", title: "Shut off after N minutes", description: "0", required: true
    }
    section("Turn on these switches..."){
		input "switches", "capability.switch", multiple: true
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
	subscribe(meter, "power", eventHandler)
    state.scheduled = 0
}

def eventHandler(evt) {
  if (!disableLogic) {
    if (meter.currentValue("power") >= DeviceRunning){
        unschedule()
        state.scheduled = 0
    	switches?.on()
        log.debug "ST Turned on the ${switches}"
    } else {
      if (ShutOffDelay > 0) {
        if (state.scheduled == 0) {
            log.debug "ST will turn off the ${switches} in ${ShutOffDelay} minutes"
  			runIn(ShutOffDelay * 60, shutOff)
    		state.scheduled = 1
        }
      } else {
        shutOff()
      }
    }
  }
}

def shutOff() {
  if (meter.currentValue("power") < DeviceRunning) {
    switches?.off()
  	state.scheduled = 0
    log.debug "ST Turned off the ${switches}"
  }
}
