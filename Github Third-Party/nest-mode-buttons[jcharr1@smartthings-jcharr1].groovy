/**
 *  Nest Mode Buttons
 *
 *  Copyright 2016 Jason Charrier
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
    name: "Nest Mode Buttons",
    namespace: "jcharr1",
    author: "Jason Charrier",
    description: "Nest Mode Buttons",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	section("Choose the button") {
        input "theButton", "capability.button", required: true
    }

	section("Choose Thermostat") {
        input "theThermostat", "capability.thermostat", required: true
    }
    
    section("Choose mode") {
        input(name: "theMode", type: "enum", title: "Mode", options: ["Cool","Heat","Off"], required: true)
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
    log.debug "Subscribe to button.pushed"
    subscribe(theButton, "button.pushed", pushHandler)
}

// TODO: implement event handlers
def pushHandler(evt) {
    log.debug "Button pushed: $evt"
    switch(theMode){
    	case "Cool":
    		log.debug "Turning on cooling..."
    		theThermostat.cool()
            break
    	case "Heat":
    	log.debug "Turning on heating..."
    		theThermostat.heat()
            break
    	case "Off":
    		log.debug "Turning off..."
    		theThermostat.off()
            break
    }	
}