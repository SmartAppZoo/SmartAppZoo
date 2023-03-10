/**
 *  Camera LED Controller
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
 *	2016-07-10: Initial Release
 */
definition(
	name: "Camera LED Controller",
	namespace: "chadly",
	author: "Chad Lee",
	description: "Turn Foscam LEDs on/off in response to other lights being turned on/off. e.g. when the front porch light is turned off at night, turn the front porch camera night vision LED on and vice versa.",
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@3x.png")


preferences {
	section("When this light is toggled at night") {
		input "light", "capability.switch", required: true
	}
	section("Toggle these camera LEDs") {
		input "camera", "capability.imageCapture", required: true, multiple: true
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
	subscribe(location, "sunset", onSunset)
	subscribe(location, "sunrise", onSunrise)

	subscribe(light, "switch.on", lightOn)
	subscribe(light, "switch.off", lightOff)
}

def onSunset(evt) {
	if (light.currentSwitch == "off") {
		log.info "Turning on camera LED at sunset since light is off"
		camera.ledOn()
	} else {
		log.info "Turning off camera LED at sunset since light is on"
		camera.ledOff()
	}
}

def onSunrise(evt) {
	log.info "Turning off camera LED since the sun is out now"
	camera.ledOff()
}

def lightOn(evt) {
	log.info "$evt.displayName was turned on, turning off camera LED"
	camera.ledOff()
}

def lightOff(evt) {
	log.debug "the light is off: $evt"

	def cdt = new Date(now())
	def sunsetSunrise = getSunriseAndSunset()

	log.trace "Current DT: $cdt, Sunset $sunsetSunrise.sunset, Sunrise $sunsetSunrise.sunrise"

	if ((cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)) {
		log.info "$evt.displayName was turned off at night, turning on camera LED"
		camera.ledOn()
	} else {
		log.info "$evt.displayName was turned off during daytime, turning off camera LED"
		camera.ledOff()
	}
}
