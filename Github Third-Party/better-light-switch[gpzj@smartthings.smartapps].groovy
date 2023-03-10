/**
 *  Better Light Switch
 *
 *  Copyright 2019 Justin Wildeboer
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
    name: "Better Light Switch",
    namespace: "gpzj",
    author: "Justin Wildeboer",
    description: "Apply this to a virtual switch (or any switch really) so that if you turn off a targeted light/switch, it will turn off all others, as well. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Toggle this switch:") {
		// TODO: put inputs here
        input "theswitch", "capability.switch", required: true, title: "Which Switch?"
	}
    section("Toggle this light") {
        input "thelight", "capability.switch", required: true, multiple: true, title: "Which Lights?"
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
    subscribe(theswitch, "switch.on", switchOnHandler)
    subscribe(theswitch, "switch.off", switchOffHandler)
    subscribe(thelight, "switch.on", lightOnHandler)
    subscribe(thelight, "switch.off", lightOffHandler)
}

// TODO: implement event handlers
def switchOnHandler(evt) {
    log.debug "switchOnHandler called: $evt"
    thelight.on()
}

def switchOffHandler(evt) {
    log.debug "switchOffHandler called: $evt"
    thelight.off()
}

def lightOnHandler(evt) {
    log.debug "switchOnHandler called: $evt"
    theswitch.on()
}

def lightOffHandler(evt) {
    log.debug "switchOffHandler called: $evt"
    theswitch.off()
}