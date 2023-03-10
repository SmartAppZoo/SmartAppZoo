/**
 *  AppMovimientoActiveInactive-v2 (Parte 1)
 *
 *  Copyright 2016 Monica Pinto
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
    name: "(Parte 1) 3. AppMovimientoActiveInactive-v2",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Esta aplicaci\u00F3n hace lo mismo que AppMovimientoActiveInactive, pero implementando los manejadores de distinta forma",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Listen to this motion detector:") {
        input "themotion", "capability.motionSensor", required: true, title: "When?"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
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
    subscribe(themotion,"motion.active",motionDetectedHandler)
    subscribe(themotion,"motion.inactive",motionDetectedHandler)
}

// TODO: implement event handlers
def motionDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt"
    
    if (evt.value == "active")
    	theswitch.on()
    else if (evt.value == "inactive")
    	theswitch.off()
}

