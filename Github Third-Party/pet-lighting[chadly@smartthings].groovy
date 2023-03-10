/**
 *  Pet Lighting
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
 *	2016-12-28: Initial Release
 */
definition(
	name: "Pet Lighting",
	namespace: "chadly",
	author: "Chad Lee",
	description: "Turn lights on only in Away mode only at night...for the pets",
	category: "My Apps",
	iconUrl: "http://image.flaticon.com/icons/png/512/12/12638.png",
	iconX2Url: "http://image.flaticon.com/icons/png/512/12/12638.png",
	iconX3Url: "http://image.flaticon.com/icons/png/512/12/12638.png")

preferences {
	input "lights", "capability.switchLevel", required: true, title: "Turn Lights On", multiple: true
	input "level", "number", required: true, title: "Set Level to"
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "mode", onModeChanged)
	subscribe(location, "sunset", onSunset)
	subscribe(location, "sunrise", onSunrise)
}

def onModeChanged(evt) {
	if (location.mode == "Away") {
		// wait a couple of minutes for any goodbye routines to finish executing
		runIn(60*2, turnOnPetLighting)
	}
}

def onSunset(evt) {
	turnOnPetLighting()
}

def onSunrise(evt) {
	if (location.mode == "Away") {
		lights.off()
	}
}

def turnOnPetLighting() {
	if (location.mode == "Away") {
		def cdt = new Date(now())
		def sunsetSunrise = getSunriseAndSunset()

		def isNight = (cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)

		if (isNight) {
			lights.setLevel(level)
		}
	}
}