/**
 *  Smart Switch
 *
 *  Version: 1.0-vSwitch
 *
 *  Copyright 2015 Rob Landry
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
	name:		"Smart Switch1",
	namespace:	"roblandry",
	author:		"Rob Landry",
	description:	"Turn on/off switch with a time delay.",
	category:	"Mode Magic",
	iconUrl:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
   	iconX2Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
	iconX3Url:	"https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png")


preferences {
	section("Info") {
		paragraph "Author:  Rob Landry"
		paragraph "Version: 1.0-vSwitch"
		paragraph "Date:    2/12/2015"
	}
	section("Devices") {
		input "switches", "capability.switch", title: "Device to turn on/off using a time delay",required: true, multiple: true
		input "vSwitch", "capability.switch", title: "The Virtual Switch", required: false, multiple: true

	}
	section("Preferences") {
		input "onOff", "bool", title: "Do you want the device to turn on or off", required: true, defaultValue: 1
		input "triggerModes", "mode", title: "System Changes Mode to...", required: false, multiple: true
		input "delayMinutes", "number", title: "Minutes", required: false, defaultValue: 0
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
	unschedule ("turnOnOffAfterDelay")
	subscribe(switches, "switch", switchHandler)
	subscribe(vSwitch, "switch", vSwitchHandler)
	subscribe(location, modeChangeHandler)
}

def modeChangeHandler(evt) {
	log.debug "Mode Change Handler: ${evt.name}: ${evt.value}, ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def vSwitchHandler(evt) {
	log.debug "Virtual Switch Handler: ${evt.name}: ${evt.value}"
	if (evt.value == "on" || evt.value == "off") {
		eventHandler(evt)
	}
}

def eventHandler(evt) {
	log.debug "Event Handler: ${evt.name}: ${evt.value}, State: ${state}"
	state.startTimer = now()
	if(delayMinutes) {
		// This should replace any existing off schedule
		unschedule("turnOnOffAfterDelay")
		runIn(delayMinutes*60, "turnOnOffAfterDelay", [overwrite: false])
	} else {
		turnOnOffAfterDelay()
	}
}


def switchHandler(evt) {
	log.debug "Switch Handler: ${evt.name}: ${evt.value}, State: ${state}"
	if (evt.value == "off") {
		log.info "Turning off."
	} else if (evt.value == "on") {
		log.info "Turning on."
	}

}

def turnOnOffAfterDelay() {
	log.debug "turnOnOffAfterDelay: State: ${state}, ${onOff}"

	if (state.startTimer) {
		def elapsed = now() - state.startTimer
		if (elapsed >= (delayMinutes ?: 0) * 60000L) {
			if (onOff) {
				switches.on()
			} else {
				switches.off()
			}
		}
	}
}