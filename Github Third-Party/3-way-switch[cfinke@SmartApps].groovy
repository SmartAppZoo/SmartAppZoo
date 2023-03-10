/**
 *  Cross-Circuit 3-Way Switch
 *
 *  Copyright 2016 Christopher Finke
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
	name: "Cross-Circuit 3-Way Switch",
	namespace: "cfinke",
	author: "Christopher Finke",
	description: "Combine two or more independent switches into a multi-switch setup where all switches control all devices.  Useful for if you have two or more different lights that you always want to turn on and off together.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switches") {
		input "switches", "capability.switch", required: true, title: "Switches?", multiple: true
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
	for ( eachSwitch in switches ) {
		subscribe(eachSwitch, "switch.on", switchOnHandler)
		subscribe(eachSwitch, "switch.off", switchOffHandler)
	}
}

def switchOnHandler(evt) {
	for ( eachSwitch in switches ) {
		if ( eachSwitch.currentSwitch == "off" ) {
			eachSwitch.on()
		}
	}
}

def switchOffHandler(evt) {
	for ( eachSwitch in switches ) {
		if ( eachSwitch.currentSwitch == "on" ) {
			eachSwitch.off()
		}
	}
}