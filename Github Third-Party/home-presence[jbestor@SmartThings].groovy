/**
 *  Update Home Presence
 *
 *  Copyright 2017 William Bishop
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
    name: "Home Presence",
    namespace: "wbish",
    author: "William Bishop",
    description: "Automate Simulated Presence Sensor based on whether everyone is away or someone arrives",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("About") {
		paragraph "HomePresence sets a Simulated Presence Sensor to away when everyone is away and sets the presence to present when anyone is present."
	}
    section("Simulated Presence Sensor") {
		input "simulated", "device.simulatedPresenceSensor", multiple: false
	}
    section("People") {
		input "people", "capability.presenceSensor", multiple: true
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
    subscribe(people, "presence", presence)
    
    updatePresence();
}

def presence(evt) {
	log.debug "$evt.name: $evt.value"
    
	updatePresence();
}

private updatePresence() {
    if (everyoneIsAway())
    	simulated.departed();
    else
    	simulated.arrived();
}

private everyoneIsAway() {
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}