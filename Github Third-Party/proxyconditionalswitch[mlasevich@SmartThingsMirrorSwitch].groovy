/**
 *  ProxyConditionalSwitch
 *
 *  Copyright 2015 Michael Lasevich
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
		name: "ProxyConditionalSwitch",
		namespace: "com.legrig",
		author: "Michael Lasevich",
		description: "Provides Proxy Conditional Switch with memory. Only turn off real device if it was turned on via this proxy switch.",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
		)

preferences {
	section("Real Switch...") {
		input "realswitch", "capability.switch",
		title: "Actual Switch To Control...",
		required: true
	}
	section("Virtual Confitional Switch") {
		input "standin", "capability.switch",
		title: "Virtual Confitional Switch...",
		required: true
	}
}

def installed() {
	log.debug "Installed for "
	subscribe(standin, "switch.on", switchOnHandler)
	subscribe(standin, "switch.off", switchOffHandler)
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	subscribe(standin, "switch.on", switchOnHandler)
	subscribe(standin, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
	log.debug "Realswitch is: " + realswitch.displayName
	state.wasOff = realswitch.currentValue("switch") == "off"
	log.debug "switchOnCondHandler: wasOff="+state.wasOff
	if(state.wasOff) realswitch.on()
}

def switchOffHandler(evt) {
	log.debug "switchOffCondHandler: wasOff="+state.wasOff
	if(state.wasOff)realswitch.off()
}