/**
 *  Smart Home Monitor Actions
 *
 *  Copyright 2016 Randy Shaddach
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
	name: "Smart Home Monitor Actions",
	namespace: "rjshadd",
	author: "Randy Shaddach",
	description: "Perform various actions when the Smart Home Monitor changes state",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)


preferences {
	page(name: "mainPage", title: "Smart Home Monitor Actions") {
        // away section
        section("When Smart Home Monitor is set to away...") {
            input "awayMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "awaySwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "awaySwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        // stay section
        section("When Smart Home Monitor is set to stay...") {
            input "stayMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "staySwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "staySwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        // disarm section
        section("When Smart Home Monitor is set to disarm...") {
            input "disarmMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "disarmSwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "disarmSwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
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

// subscribe to alarm state changes
def initialize() {
	subscribe(location, "alarmSystemStatus", alarmHandler)
}

// alarm state change event handler
def alarmHandler(evt) {
	//log.debug "Alarm Handler value: ${evt.value}"
	//log.debug "alarm state: ${location.currentState("alarmSystemStatus")?.value}"
	
	if (evt.value == "away") {
		log.debug "SHM Actions: shm set to away"
        if (settings.awayMode) {
        	changeMode(settings.awayMode)
            awaySwitchesOn.on()
            awaySwitchesOff.off()
        }
	} else if (evt.value == "stay") {
		log.debug "SHM Actions: shm set to stay"
        if (settings.stayMode) {
        	changeMode(settings.stayMode)
            staySwitchesOn.on()
            staySwitchesOff.off()
        }
	} else if (evt.value == "off") {
		log.debug "SHM Actions: shm set to disarmed"
        if (settings.disarmMode) {
        	changeMode(settings.disarmMode)
            disarmSwitchesOn.on()
            disarmSwitchesOff.off()
        }
	} else {
    	log.debug "SHM Actions: unkown shm state: ${evt.value}"
    }
}

// function to change the mode
def changeMode(newMode) {
	log.debug "changeMode, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"

	if (location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
		} else {
			log.warn "Tried to change to undefined mode '${newMode}'"
		}
	}
}