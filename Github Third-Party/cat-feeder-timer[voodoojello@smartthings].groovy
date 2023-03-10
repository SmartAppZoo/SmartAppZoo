/**
 *
 * ============================================ 
 *  Cat Feeder Timer
 * ============================================ 
 *
 *  Copyright (c)2017 Mark Page (mark@very3.net)
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
definition (
  name: "Cat Feeder Timer",
  namespace: "cat-feeder-timer",
  author: "Mark Page",
  description: "Turn off selected device after XX minutes",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
	section("Select Cat Feeder Device:") {
		input "theSwitch", "capability.switch", title: ""
	}
	section("Set Run Time (minutes)") {
		input "minutesLater", "number", title: ""
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
    sendPush("Turned ON the ${theSwitch} switch.")
	log.debug "Switch ${theSwitch} turned: ${evt.value}"
	def delay = minutesLater * 60
	log.debug "Turning off in ${minutesLater} minutes (${delay}seconds)"
	runIn(delay, turnOffSwitch)
}

def turnOffSwitch() {
	theSwitch.off()
    sendPush("Turned OFF the ${theSwitch} switch.")
}