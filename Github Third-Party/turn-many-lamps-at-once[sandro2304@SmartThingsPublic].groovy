/**
 *  Turn many lamps at once
 *
 *  Copyright 2019 Sandro Herrera
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
    name: "Turn many lamps at once",
    namespace: "Sandro2304",
    author: "Sandro Herrera",
    description: "Turning office lamp turns on Sandro Lamp",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Receive turn on/off command:") {
        input "masterlamp", "capability.switch", required: true
    }    
    section("Turn on/off when master lamp is turned on:") {
        input "slavelamp", "capability.switch", required: true
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
	log.debug "Initializing"
	subscribe(masterlamp, "switch", masterHandler)
//    subscribe(masterlamp, "switch", masterOffHandler)
}

def masterHandler(evt) {
    log.debug "MasterHandler: $evt, $evt.value"
    
    def masterLampState = masterlamp.currentState("switch")
    log.debug "State of the master lamp ${masterLampState.value}"
    log.debug "State of evt ${evt.value}"
    // log.debug "State of masterlamp.switch.value: ${masterlamp.switch.value}" <- This does not work
    
    
    if (masterLampState.value == "on") {
    	log.debug "Turning on lamps"
        masterlamp.on()
    	slavelamp.on()
    } else if (masterLampState.value == "off") {
		masterlamp.off()
    	slavelamp.off()
    }
}