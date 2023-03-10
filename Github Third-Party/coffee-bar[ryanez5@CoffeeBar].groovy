/*
 *  Coffee Bar
 *
 *  Copyright 2019 Rudy Yanez
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
    name: "Coffee Bar",
    namespace: "ryanez5",
    author: "Rudy Yanez",
    description: "Turns on and off bar lights when coffee machine goes into/out of standby.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Devices") {
                     input(name: "meter", type: "capability.powerMeter", title: "Power Meter to Monitor.", required: true, multiple: false, description: null)
                     //input(name: "aboveThreshold", type: "number", title: "Reports Above...", required: true, description: "in either watts or kw.")
                     //input(name: "belowThreshold", type: "number", title: "Reports Below...", required: true, description: "in either watts or kw.")
                     input(name: "outlet", type: "capability.switch", title: "Outlet to Switch.", required: true, multiple: false, description: null)
                     //input(name: "delayInit", type: "number", title: "How long to delay switch-off? (in min)", required: true, description: null)
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe (meter, "power", switchMeter)
}

// TODO: implement event handlers

def switchMeter(evt) {
    def meterValue = evt.value as double
    //log.debug "the value is ${meterValue}"
    if (meterValue > 2)
        outlet.on()
        //log.debug "the value on ${meterValue}"
    else
        outlet.off([delay:1000000])
        //log.debug "the value off ${meterValue}"

}