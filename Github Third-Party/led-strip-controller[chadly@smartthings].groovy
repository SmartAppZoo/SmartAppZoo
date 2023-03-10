/**
 *  LED Strip Light Controller
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
 *	2017-04-14: Allow control of multiple LED strips
 *	2016-11-23: Add option to force LED color
 *	2016-11-21: Initial Release
 */
definition(
	name: "LED Strip Controller",
	namespace: "chadly",
	author: "Chad Lee",
	description: "Tie the control of LED strip lights to other lights.",
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@3x.png")


preferences {
	section("Lights") {
		paragraph "When the overhead light is turned on, turn LED strip light on to full brigtness. When the island dimmer is turned on or dimmed without the overhead light on, the LED strip lights' brightness will follow the dimmer."
		input "light", "capability.switch", required: true, title: "Overhead Light"
		input "dimmer", "capability.switchLevel", required: true, title: "Island Dimmer"
		input "led", "capability.switch", required: true, multiple: true, title: "LED Strips"
	}
	section("Options") {
		paragraph "If this option is selected, when the overhead light is turned on, the LED lights will be forced to the color white and will be turned on along with the warm white LEDs. If it is not selected, the LED color will not be changed; only the warm white LEDs will be turned on."
		input "forceColor", "bool", required: true, title: "Force LEDs to White?"
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
	subscribe(light, "switch", lightChanged)
	subscribe(dimmer, "switch", dimmerAdjusted)
	subscribe(dimmer, "level", dimmerAdjusted)
}

def lightChanged(evt) {
	adjustLED()
}

def dimmerAdjusted(evt) {
	adjustLED()
}

def adjustLED() {
	if (!adjustColorFromLight()) {
		adjustWhitesFromDimmer()
	}
}

def adjustColorFromLight() {
	def switchState = light.latestValue("switch")

	if (switchState == "on") {
		if (forceColor) {
			def colorData = [:]
				colorData = [h: 0,
				s: 0,
				l: 100,
				r: 255,
				g: 255,
				b: 255,
				rh: "ff",
				gh: "ff",
				bh: "ff",
				hex: "#ffffff",
				alpha: 1]

			led.setAdjustedColor(colorData)
			led.setLevel(100)
		}

		led.setWhiteLevel(100)

		return true
	} else {
		if (forceColor) {
			led.setLevel(0)
		}

		return false
	}
}

def adjustWhitesFromDimmer() {
	// match the dimmer switch level for whites
	def dimmerState = dimmer.latestValue("switch")

	if (dimmerState == "on") {
		def dimmerLevel = dimmer.latestValue("level")
		led.setWhiteLevel(dimmerLevel.toInteger())
	} else {
		led.setWhiteLevel(0)
	}
}