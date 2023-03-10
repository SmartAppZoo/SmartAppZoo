/**
 *  Is the door locked?
 *
 *  Copyright 2017 Justin Walker
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
    name: "Is the door locked?",
    namespace: "augoisms",
    author: "Justin Walker",
    description: "Checks if the door is locked, if unlocked it turns on a light",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png")


preferences {
    section("Select door") {
		input name: "door", type: "capability.contactSensor", multiple: false
	}
    section("Select lights to turn on/off...") {
		input name: "bulbs", type: "capability.light", multiple: true
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
    subscribe(door, "contact.closed", onLocked)
	subscribe(door, "contact.open", onUnlocked)
    // peform checks to make sure things are in sync
    runEvery3Hours(checkStatus)
}

// TODO: implement event handlers

def onUnlocked(evt) {
	log.debug "door is unlocked, turning lights on"
    bulbs.on()
}

def onLocked(evt) {
	log.debug "door is locked, turning lights off"
    bulbs.off()
}

def checkStatus() {
    if (door.currentValue("contact") == "closed") {
    	log.debug "door is locked, turning lights off"
    	bulbs.off()
    } else {
    	log.debug "door is unlocked, turning lights on"
    	bulbs.on()
    }
}