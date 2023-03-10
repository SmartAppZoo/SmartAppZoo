/**
 *  Trigger multiple lights based on one input
 *
 *  Copyright 2018 Ajeya Tatake
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
    name: "Trigger multiple lights based on one input",
    namespace: "tatake_labs",
    author: "Ajeya Tatake",
    description: "My first smart app to trigger multiple lights based on one input",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this switch is turned on:") {
    	input "thevirtualswitch", "capability.switch", required:true
    }
    section("Turn on these lights") {
        input "theswitch1", "capability.switch", required: true
        input "theswitch2", "capability.switch", required: true
        input "theswitch3", "capability.switch", required: false
        input "theswitch4", "capability.switch", required: false
    }}

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
    subscribe (thevirtualswitch, "switch.on", turnedOnHandler)
    subscribe (thevirtualswitch, "switch.off", turnedOffHandler)
}

// TODO: implement event handlers
def turnedOnHandler (evt) {
	log.debug "Turned on handler with event: $evt"
    theswitch1.on()
    theswitch2.on()

	setSwitchOn (theswitch3)
	setSwitchOn (theswitch4)
}


def turnedOffHandler (evt) {
	log.debug "Turned off handler with event: $evt"
    theswitch1.off()
    theswitch2.off()

	setSwitchOff (theswitch3)
	setSwitchOff (theswitch4)
}


def setSwitchOn (sw)
{
	if (sw) {
    	sw.on()
    }
}


def setSwitchOff (sw)
{
	if (sw) {
    	sw.off()
    }
}