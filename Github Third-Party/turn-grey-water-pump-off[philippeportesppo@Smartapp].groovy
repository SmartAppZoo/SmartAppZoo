/**
 *  Turn Grey Water pump off
 *
 *  Copyright 2017 Philippe Portes. Adapted from the Energy Save SmartThings SmartApp
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
    name: "Turn Grey Water pump off",
    namespace: "philippeportesppo",
    author: "Philippe Portes",
	description: "Turn pump off once it sucked everything it could",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        input(name: "thresholdhigh", type: "number", title: "Consumes above pump sucking power (water sucked)...", required: true, description: "in either watts or kw.")
        input(name: "thresholdlow", type: "number", title: "Then goes below pump sucking power (air sucked)...", required: true, description: "in either watts or kw.")
	}
    section {
    	input(name: "switches", type: "capability.switch", title: "Turn Off These Switches", required: true, multiple: false, description: null)
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
	subscribe(meter, "power", meterHandler)
    state.upperThresholdPassed = 0
    subscribe(switches, "switch.on", switchOnHandler)
}

def meterHandler(evt) {
    def meterValue = evt.value as double
    def thresholdValueHigh = thresholdhigh as int
    def thresholdValueLow = thresholdlow as int

	// Check if pump is sucking i.e. wattage is above the threshold high measured while sucking liquid
    if (meterValue > thresholdValueHigh) {
    	state.upperThresholdPassed =1
        log.debug "${meter} reported energy consumption above threshold ${threshold}. Pump is sucking."
        }
    // Check if pump is no longer sucking i.e. wattage is below the threshold low measured while pump is sucking air
    if (meterValue < thresholdValueLow && state.upperThresholdPassed == 1) {
	    log.debug "${meter} reported energy consumption below min threshold ${threshold}. Turning off pump."
    	switches.off()
        state.upperThresholdPassed =0
    }
}

// Set to 0 the parameter in order to avoid false detection after 1st usage or if user stops the pump while still sucking water.
def switchOnHandler(evt) {
	state.upperThresholdPassed =0 
}