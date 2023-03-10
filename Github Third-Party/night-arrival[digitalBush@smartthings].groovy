/**
 *  Night Arrival
 *
 *  Copyright 2016 Josh Bush
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
	name: "Night Arrival",
	namespace: "digitalbush",
	author: "Josh Bush",
	description: "When coming home at night, turn on some switches for a short period of time.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switches to turn on") {
		input "switches", "capability.switch", title: "Choose Switches", multiple: true
	}
	section("How many minutes to leave on?") {                    
		input "activeMins", "number", title: "Turn back off after", range: "1..60"
	}
	section("Turn on when there is presence") {
		input "presenceSensors", "capability.presenceSensor", title: "Choose presence sensors", multiple: true
	}
	section("After I've been gone at least this many minutes") {                    
		input "delayMins", "number", title: "I have to be gone at least", range: "1..60"
	}
	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
		input "zipCode", "text", title: "Zip code", required: false
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
	subscribe(presenceSensors, "presence", presenceHandler)
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	findSunriseSunset()
}

def locationPositionChange(evt) {
	findSunriseSunset()
}

def sunriseSunsetTimeHandler(evt) {
	findSunriseSunset()
}

def presenceHandler(evt) {
	if (evt.value == "present" && hasBeenGoneLongEnough(evt.getDevice()) && isDark()) {
		log.debug("Welcome Back!");
		def offSwitches = findOffSwitchIds()
		switches.on()
		startTimer(offSwitches)
	}
}

def findSunriseSunset() {
	def s = getSunriseAndSunset(zipCode: zipCode)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
}

def isDark() {
	def t = now()
	t < state.riseTime || t > state.setTime
}

def hasBeenGoneLongEnough(device) {
	def start = now() - (delayMins * 1000 * 60)
	def states = device.statesSince("presence", new Date(start))
	!states.any{it.value == "not present"}
}

def findOffSwitchIds() {
	def currSwitches = switches.currentSwitch
	switches.findAll { it.currentSwitch == "off" }.collect{ it.id }
}

def turnOffSwitches(data) {
	data.switches.each { id ->
		def theSwitch = switches.find { it.id == id }
		theSwitch.off()
	}
}

def startTimer(offSwitches) {
	runIn(activeMins * 60, turnOffSwitches, [overwrite: false, data:[switches: offSwitches]])
}
