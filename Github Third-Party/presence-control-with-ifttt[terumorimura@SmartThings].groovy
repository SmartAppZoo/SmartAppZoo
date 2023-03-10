/**
 *  Presence Control with IFTTT
 *
 *  Copyright 2015 Teru Morimura
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
    name: "Presence Control with IFTTT",
    namespace: "terumorimura",
    author: "Teru Morimura",
    description: "Presence Control with IFTTT",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Switch to a virtual monitor"){
		input "theSwitch", "capability.switch"
	}
	section("Select Presense Sensor to control"){
		input "thePresence", "capability.presenceSensor"
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
	subscribe(theSwitch, "switch.On", onHandler)
    subscribe(theSwitch, "switch.Off", offHandler)
}

def onHandler(evt) {
	log.debug "Received on from ${theSwitch}"

    thePresence.arrived()
}

def offHandler(evt) {
	log.debug "Received on from ${theSwitch}"

    thePresence.departed()
}
