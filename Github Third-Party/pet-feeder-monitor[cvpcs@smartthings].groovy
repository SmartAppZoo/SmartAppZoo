/**
 *  Pet Feeder Monitor
 *
 *  Copyright 2018 Austen Dicken
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
    name: "Pet Feeder Monitor",
    namespace: "cvpcs",
    author: "Austen Dicken",
    description: "Monitors an accelerometer and turns on an indicator switch to indicate that a pet has been fed based on movement, then turns the switch off after a given amount of time.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/cvpcs/smartthings/master/devicetypes/cvpcs/pet-feeder-indicator-virtual-switch.src/pet-feeder-indicator-fed.png",
    iconX2Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/devicetypes/cvpcs/pet-feeder-indicator-virtual-switch.src/pet-feeder-indicator-fed.png")


preferences {
    section("Monitor Settings") {
    	input("sensor", "capability.accelerationSensor", title: "Acceleration Sensor", description: "Select an accelerometer to monitor", required: true, displayDuringSetup: true)
        input("switches", "capability.switch", title: "Indicator Switches", description: "Select one or more switches to control based on the feeder sensor", required: true, multiple: true, displayDuringSetup: true)
        input("resetBreakfast", "time", title: "Breakfast reset", description: "Set a time to reset the indicator switch for breakfast", required: true, displayDuringSetup: true)
        input("resetLunch", "time", title: "Lunch reset", description: "Set a time to reset the indicator switch for lunch", required: true, displayDuringSetup: true)
        input("resetDinner", "time", title: "Dinner reset", description: "Set a time to reset the indicator switch for dinner", required: true, displayDuringSetup: true)
        input("ignoreDelay", "number", title: "Ignore delay", description: "Set a length of time (in seconds) to ignore subsequent acceleration activity after processing the first one", required: true, displayDuringSetup: true)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor, "acceleration.active", accelerationActiveHandler)
        
    log.debug "Scheduling shutoff for indicator switches at ${resetBreakfast}, ${resetLunch}, ${resetDinner}"

    schedule(resetBreakfast, resetHandlerBreakfast)
    schedule(resetLunch, resetHandlerLunch)
    schedule(resetDinner, resetHandlerDinner)
}

def accelerationActiveHandler(evt) {
	log.trace "$evt.value: $evt, $settings"

	// Don't send a continuous stream of notifications
	def recentEvents = sensor.eventsSince(new Date(now() - (1000 * ignoreDelay)))
	log.trace "Found ${recentEvents?.size() ?: 0} events in the last ${ignoreDelay} seconds"
	def alreadyProcessedEvents = recentEvents.count { it.value && it.value == "active" } > 1

	if (alreadyProcessedEvents) {
		log.debug "Ignoring events that have already been processed within the last ${ignoreDelay} seconds"
	} else {
    	log.debug "Turning on indicator switches"
        
    	switches.each { it.on() }
	}
}

def resetHandlerBreakfast() { resetHandler() }
def resetHandlerLunch() { resetHandler() }
def resetHandlerDinner() { resetHandler() }

def resetHandler() {
    switches.each { it.off() }
}