/**
 *  SwitchIn PulseOut
 *
 *  Copyright 2018 Rasmus Agdestein
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
    name: "SwitchIn PulseOut",
    namespace: "rasmusagdestein",
    author: "Rasmus Agdestein",
    description: "Send Pulse when switch turns on. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When a switch turns on...") {
		input "theSwitch", "capability.switch"
	}
    section("Pulse this...") {
		input "thePulse", "capability.switch"
	}
	section("Turn it off how many SECONDS later?") {
		input "secondsLater", "number", title: "When?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(theSwitch, "switch.on", switchOnHandler, [filterEvents: false])
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe(theSwitch, "switch.on", switchOnHandler, [filterEvents: false])
}

def switchOnHandler(evt) {
	log.debug "Switch ${theSwitch} turned: ${evt.value}"
    thePulse.on()
	def delay = secondsLater * 1000 /*Because delay is counted in mill-seconds, multiple seconds by 1000 to get the proper delay. */
	log.debug "Turning off in ${secondsLater} minutes (${delay}ms)"
	thePulse.off(delay: delay)
}