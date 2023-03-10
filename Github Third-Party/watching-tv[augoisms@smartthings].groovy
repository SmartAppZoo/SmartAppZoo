/**
 *  Watching TV
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
    name: "Watching TV",
    namespace: "augoisms",
    author: "Justin Walker",
    description: "Checks if the TV is on, if TV is on it turns on one light and off another",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics3-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics3-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics3-icn@2x.png")


preferences {
    section("Select TV Sensor") {
		input name: "tv", type: "capability.contactSensor", multiple: false
	}
    section("Lights ON when TV ON") {
		input name: "onWhenTVOnSwitches", type: "capability.switch", multiple: true
	}
    section("Lights OFF when TV ON") {
		input name: "offWhenTVOnSwitches", type: "capability.switch", multiple: true
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
	// subscribe to attributes, devices, locations, etc.
    subscribe(tv, "contact.closed", onTVOn)
	subscribe(tv, "contact.open", onTVOff)
    // run initial check to make sure things are in sync
    checkStatus()
}

def onTVOn(evt) {
	log.debug "TV is ON"
    onWhenTVOnSwitches.on()
    // the on lights are slow to illuminate so
    // pause a beat before turning the off lights off
    offWhenTVOnSwitches.off(delay: 2500)
}

def onTVOff(evt) {
	log.debug "TV is OFF"
    onWhenTVOnSwitches.off()
    offWhenTVOnSwitches.on()
}

def checkStatus() {
    if (tv.currentValue("contact") == "closed") {
    	log.debug "TV is ON"
    	onWhenTVOnSwitches.on()
    	offWhenTVOnSwitches.off()
    } else {
    	log.debug "TV is OFF"
    	onWhenTVOnSwitches.off()
    	offWhenTVOnSwitches.on()
    }
}