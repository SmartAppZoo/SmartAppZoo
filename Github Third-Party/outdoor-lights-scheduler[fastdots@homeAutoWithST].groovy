/**
 *  Outdoor Lights Scheduler
 *
 *  Copyright 2018 Ajeya Tatake
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
    name: "Outdoor Lights Scheduler",
    namespace: "tatake_labs",
    author: "Ajeya Tatake",
    description: "Turn outdoor lights (porch and walkway) on and off based on sunrise and sunset times",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Manage this switch") {
        input "theswitch", "capability.switch", required: true
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
	subscribe (location, "sunset", sunsetHandler)
    subscribe (location, "sunrise", sunriseHandler)
}

def sunsetHandler (evt) {
	log.debug "Turned on handler with event: $evt"
    theswitch.on()
}


def sunriseHandler (evt) {
	log.debug "Turned off handler with event: $evt"
    theswitch.off()
}
