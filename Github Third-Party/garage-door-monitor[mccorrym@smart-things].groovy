/**
 *  Garage Door Monitor
 *
 *  Copyright 2020 Matt
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
	name: "Garage Door Monitor",
	namespace: "smartthings",
	author: "Matt",
	description: "Monitor the switch attached to the garage door to turn the switch back off after a few seconds.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the switch that controls the garage door.") {
		input "garage_door", "capability.switch", required: true, multiple: false, title: "Choose the garage door switch"
	}
}

def installed() {
    subscribe(garage_door, "switch", switchChangeHandler)
}

def updated() {
	unsubscribe()
    subscribe(garage_door, "switch", switchChangeHandler)
}

def switchChangeHandler(evt) {
	if (evt.value.toString() == "on") {
    	runIn(3, toggleSwitch, [overwrite: false])
    }
}

def toggleSwitch() {
    garage_door.off()
}