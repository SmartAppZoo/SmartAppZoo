/**
 *  Garage Door Monitor
 *
 *  Copyright 2018 Audi McAvoy
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
 *  This is the sister app for the Z-Wave momentary relay device hanlder, which turns a normal Z-Wave relay into
 *	a garage door opener button. Setup a relay with the sister device handler first. Then install this app, and select
 *	the door and relay you would like to link together.
 *
 *	This app simply subscribes to the door's events, and when it sees the door opening or closing, it updates
 *	the relay handler with the current state. This way the relay device hanlder can intelligently open/close
 *	the door.
 *
 *	Note: I renamed my relay "Garage Door Button"
 *
 */
definition(
    name: "Garage Door Monitor",
    namespace: "mcavoya",
    author: "Audi",
    description: "Monitor the position of the garage door.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Garage Door Devices") {
		input "door", "capability.doorControl", title: "Select door"
        input "relay", "capability.momentary", title: "Select relay"
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
	def doorState = door.currentValue("door")
    log.debug "Initial garage door state: $doorState"
	subscribe(door, "door", doorHandler)
}

def doorHandler(evt) {
	def doorState = evt.value
    log.debug "New garage door state: $doorState"
    if ("closing" == doorState || "closed" == doorState) {
		relay.doorClosed()
	}
    if ("opening" == doorState || "open" == doorState) {
		relay.doorOpen()
	}
}
