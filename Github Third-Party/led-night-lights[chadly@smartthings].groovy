/**
 *  LED Night Lights
 *
 *  Copyright 2016 Chad Lee
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
 *	2017-07-04: Allow multiple strips
 *	2016-12-23: Initial Release
 */
definition(
	name: "LED Night Lights",
	namespace: "chadly",
	author: "Chad Lee",
	description: "When opening the bedroom door while in sleep mode, turn the LED lights to a dim red. If opening the bedroom door after sunrise but still in sleep mode, turn LEDs to police mode.",
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@3x.png")

preferences {
	input "door", "capability.contactSensor", required: true, title: "Bedroom Door"
	input "led", "capability.switch", required: true, multiple: true, title: "LED Strip"
	input "sleep", "mode", title: "Sleepy Mode"
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.isSleeping = location.mode == sleep

	subscribe(door, "contact.open", doorOpened)
	subscribe(door, "contact.closed", doorClosed)
	subscribe(location, "mode", modeChanged)
}

def doorOpened(evt) {
	def cdt = new Date(now())
	def sunsetSunrise = getSunriseAndSunset()

	def isNight = (cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)

	if (state.isSleeping) {
		if (isNight) {
			led.red()
			led.setLevel(10)
		} else {
			led.police()
		}
	}
}

def doorClosed(evt) {
	if (state.isSleeping) {
		led.off()
	}
}

def modeChanged(evt) {
	if (evt.value == sleep) {
		state.isSleeping = true
	} else if (evt.value != sleep) {
		if (state.isSleeping) {
			// we were sleeping and now we aren't, turn off the LED strip
			led.off()
		}

		state.isSleeping = false
	}
}