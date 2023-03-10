/**
 *  Copyright 2015 SmartThings
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
 *  Turn It On For 5 Minutes
 *  Turn on a switch when a contact sensor opens and then turn it back off 5 minutes later.
 *
 *  Author: SmartThings
 */
definition(
    name: "Timed Turn Off",
    namespace: "smartthings",
    author: "SmartThings",
    description: "When a switch is turned on, it will be turned off after a certain amount of seconds",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When switch turns on..."){
		input "switch0", "capability.switch", required: true
	}
    section("Turn off switch after seconds..."){
    	input "time", "number", required: true, title: "Seconds?"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(switch0, "switch.on", contactOpenHandler)
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(switch0, "switch.on", contactOpenHandler)
}

def contactOpenHandler(evt) {
	switch0.on()
	def delay = time
    log.debug(delay)
	runIn(delay, turnOffSwitch)
}

def turnOffSwitch() {
	switch0.off()
}