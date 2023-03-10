/**
 *  Copyright 2015 SmartThings
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
 *  Over automated toilet lights
 *
 *  Author: Keyur
 *  Date: 2019-01-21
 *
 * Automate your toilet light with a switch and door sensor, acceleration optional.
 * 1. If no acceleration sensor is selected, Door open just toggles the light
 * 2. With accceleration, acceleration turns on the lights if they were turned off within 30seconds
 */

definition(
    name: "Over automated lights",
    namespace: "keyurbhatnagar",
    author: "Keyur Bhatnagar",
    description: "Controls toilet lights",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light13-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light13-icn@2x.png"
)

preferences {
	input "doorSensor", "capability.contactSensor", title: "Which sensor?"
	input "lightSwitch", "capability.switch", title: "Which Light?"
    input "doorAcceleration", "capability.accelerationSensor", title: "Acceleration sensor?", required: false
	input "openThreshold", "number", title: "Turn Off when open longer than (optional)", description: "Number of minutes", required: false
}

def installed() {
	log.trace "installed()"
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	log.debug "present: ${cars.collect{it.displayName + ': ' + it.currentPresence}}"
	subscribe(doorSensor, "contact", garageDoorContact)
    subscribe(doorAcceleration, "acceleration", accelerationActive)
}

def garageDoorContact(evt)
{
	log.info "garageDoorContact, $evt.name: $evt.value"
    
    if(evt.value == "open") {
    	log.info "lightSwitch.currentSwitch : $lightSwitch.currentSwitch"
    	if(lightSwitch.currentSwitch == "on") {
        	turnOffLight()
        } else {
            turnOnLight()
        }
    }
}

def turnOnLight()
{
	log.info "Turning On lights"
    state.reTrigger = false
	lightSwitch.on()
    if(openThreshold) {
        runIn(openThreshold*60, turnOffLight);
    }
}

def turnOffLight()
{
	log.info "Turning Off lights"
	lightSwitch.off()
    if(openThreshold && doorSensor.currentContact == "closed") {
    	log.info "Setting trigger"
    	state.reTrigger = true
    	runIn(30, clearTrigger);
    }
}

def clearTrigger()
{
	log.info "Clearing Trigger"
	state.reTrigger = false
}

def accelerationActive(evt)
{
	log.info "accelerationActive, $evt.name: $evt.value"
    if(evt.value == "active") {
		log.info "state.reTrigger: $state.reTrigger"
    	if(openThreshold) {
			log.info "2"
    		if(state.reTrigger == true) {
    			turnOnLight()
    		}
    	}
    }
}