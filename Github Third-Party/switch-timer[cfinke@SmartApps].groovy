/**
 *  Switch Timer
 *
 *  Copyright 2018 Christopher Finke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Switch Timer",
	namespace: "cfinke",
	author: "Christopher Finke",
	description: "Turn a switch off a certain amount of time after it's turned on.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section {
		input "theSwitch", "capability.switch", required: true, title: "Switch?", multiple: false
		input "minutes", "number", required: true, title: "Turn off after X minutes?"
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
	subscribe(theSwitch, "switch.on", switchOnHandler)
}

def switchOnHandler(evt) {
	runIn(minutes * 60, autoSwitchOff)
}

def autoSwitchOff(){
	theSwitch.off()
}