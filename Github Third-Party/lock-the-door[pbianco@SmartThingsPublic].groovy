/**
 *  Lock The Door
 *
 *  Copyright 2015 Phil Bianco
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
    name: "Lock The Door",
    namespace: "",
    author: "Phil Bianco",
    description: "App will lock the door after a configured number of minutes after it was unlocked",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	
    section("What lock"){
        input "lock1","capability.lock", multiple: true
        }
    section("Lock the door in ..."){
		input "delayMinutes", "number", title: "Minutes?"
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
    subscribe(lock1, "lock", lockHandler)
}

// TODO: implement event handlers

def lockHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "unlocked") {
        log.debug "Door will lock in $delayMinutes seconds"
		runIn(delayMinutes*60, lockDoorAfterDelay, [overwrite: false])
        log.debug "The door is unlocked"
		}
        
    if (evt.value == "locked"){
        log.debug "The door is locked"
        }
}

def lockDoorAfterDelay() {
    
    log.debug "In lockDoorAfterDelay"
	lock1.lock()
}
