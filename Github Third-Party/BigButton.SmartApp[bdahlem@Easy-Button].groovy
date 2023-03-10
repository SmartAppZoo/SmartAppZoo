/**
 *  BigButton
 *
 *  Copyright 2014 Brian Dahlem
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
    name: "BigButton",
    namespace: "bdahlem",
    author: "Brian Dahlem",
    description: "Toggle lights based on a button press and the state of a main light",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When this button is pressed:") {
    	input "button", "capability.button"
    }

	section("Toggle this light:") {
    	input "light", "capability.switch"
    }

    section("And toggle these lights as well (Optional):") {
    	input "togglelights", "capability.switch", multiple: true, required: false
    }
    
    section("And only turn on these lights:") {
    	input "onlights", "capability.switch", multiple: true, required: false
    }
    
    section("And only turn off these lights:") {
    	input "offlights", "capability.switch", multiple: true, required: false
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
	subscribe(button, "button", pushHandler)
}

def pushHandler(evt) {
    // Handle the button being pushed
    
	log.debug "$evt.name: $evt.value"
    
    if ((evt.value == "released") || (evt.value == "off")) {
    	if (light.currentSwitch == "off") {
            log.debug "Turn on the lights!"
        	turnLightsOn()
        }
        else {
            log.debug "Turn off the lights!"
        	turnLightsOff()
        }
    }
}

def turnLightsOff() {
	log.debug "turning off main light: ${light}"
	light.off()
    log.debug "turning off slave lights: ${togglelights}"
    togglelights?.off()
    log.debug "turning off remaining lights: ${offlights}"
    offlights?.off()
}

def turnLightsOn() {
	log.debug "turning on main light: ${light}"
	light.on()
    log.debug "turning on slave lights: ${togglelights}"
    togglelights?.on()
    log.debug "turning on remaining lights: ${onlights}"
    onlights?.on()
}