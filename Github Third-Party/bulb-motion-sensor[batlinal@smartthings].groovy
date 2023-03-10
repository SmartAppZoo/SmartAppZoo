/**
 *  Bulb Motion Sensor
 *
 *  Copyright 2017 Alex Batlin
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
    name: "Bulb Motion Sensor",
    namespace: "alex.batlin",
    author: "Alex Batlin",
    description: "Polls to see a light bulb is switched on and if so trigger motion sensor.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Motion sensor bulbs") {
		input "bulbs","capability.switch", multiple: true
	}
    section("Motion sensor switch") {
		input "motionSwitch","capability.sensor", title: "Switch"
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
	subscribe(bulbs, "switch.on", bulbsHandler)
}

def bulbsHandler(evt) {
	log.debug "one of the configured switches changed states"
    motionSwitch.on()
}