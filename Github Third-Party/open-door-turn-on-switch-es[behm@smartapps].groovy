/**
 *  Open Door Turn on Switch(es)
 *
 *  Copyright 2017 Brian Behm
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
    name: "Open Door Turn on Switch(es)",
    namespace: "behm",
    author: "Brian Behm",
    description: "Open a door and turn on one or more switches",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select the devices") {
		input "contact1", "capability.contactSensor", title: "Select a contact sensor"
        	input "switches", "capability.switch", title: "Select a light", multiple: true
	}
	section("Options") {
		turnOffOnClose
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
	subscribe contact1, "contact.open", openHandler
    	/*subscribe contact1, "contact.closed", closedHandler*/
}

def openHandler(evt) {
	switches.on()
}

def closedHandler(evt) {
	switches.off()
}
