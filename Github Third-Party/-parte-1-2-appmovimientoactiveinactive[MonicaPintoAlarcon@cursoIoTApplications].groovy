/**
 *  AppMovimientoActiveInactive
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
    name: "(Parte 1) 2. AppMovimientoActiveInactive",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Con esta aplicaci\u00F3n las luces se encienden cuando se detecta movimiento y se apagan cuando se detecta que no hay ning\u00FAn movimiento",
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
    subscribe(themotion,"motion.active",motionActiveDetectedHandler)
    subscribe(themotion,"motion.inactive",motionInactiveDetectedHandler)
}

// TODO: implement event handlers
def motionActiveDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
}

def motionInactiveDetectedHandler(evt){
	log.debug "motionDetectedHandler called: $evt"
    theswitch.off()
}