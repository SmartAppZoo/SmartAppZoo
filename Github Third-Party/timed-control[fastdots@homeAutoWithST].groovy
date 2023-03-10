/**
 *  Camera control
 *
 *  Copyright 2020 AJEY TATAKE
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
    name: "Timed control",
    namespace: "tatake_labs",
    author: "AJEY TATAKE",
    description: "Timed arming and disarming of Blink cameras (via associated virtual switch)",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn on cameras at:") {
        input "onTime", "time", title: "Time to turn on every day"
	}
	section("Turn off cameras at:") {
        input "offTime", "time", title: "Time to turn off every day"
	}
    section("Turn on these cameras") {
        input "theswitch1", "capability.switch", required: false
        input "theswitch2", "capability.switch", required: false
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
	schedule(onTime, armCameras)
	schedule(offTime, disarmCameras)
}

def armCameras (evt) {
	log.debug "armed cameras with event: $evt"
	if (theswitch1) {theswitch1.on()}
    if (theswitch2) {theswitch2.on()}
}

def disarmCameras (evt) {
	log.debug "disarmed cameras with event: $evt"
	if (theswitch1) {theswitch1.off()}
    if (theswitch2) {theswitch2.off()}
}