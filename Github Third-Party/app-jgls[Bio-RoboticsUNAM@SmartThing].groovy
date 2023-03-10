/**
 *  App JGLS
 *
 *  Copyright 2016 FI UNAM
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
    name: "App JGLS",
    namespace: "Bio-RoboticsUNAM",
    author: "FI UNAM",
    description: "Example App. Objetivo: encender algun switch cuando se detecta movimiento",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Turn On When Motion Detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where the motion sensor is?"
    }
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    section("Turn On/Off This Light") {
        input "theswitch", "capability.switch", required: true, title: "Which light do you want to turn on?", multiple: true
    }
	
    //section("Title") {
	//	// TODO: put inputs here
	//}
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
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
    /*log.debug "Name : ${theswitch.switch.name}"
    log.debug "Name: ${theswitch.displayName}"*/
    
    theswitch.each {
    	log.debug "${it.displayName}, attribute: ${it.supportedAttributes.name}, values: ${it.supportedAttributes.values[0][0]}, dataType: ${it.supportedAttributes.dataType}"
    }
    /*log.debug "${theswitch.displayName}, attribute: ${theswitch.supportedAttributes.name}, values: ${theswitch.supportedAttributes.values}"
    
    def attrs = thetemp.supportedAttributes
    attrs.each {
        log.debug "${thetemp.displayName}, attribute ${it.name}, values: ${it.values}"
        log.debug "${thetemp.displayName}, attribute ${it.name}, dataType: ${it.dataType}"
    }*/
    
}

// TODO: implement event handlers

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
    //theswitch[0].on()
    //theswitch[1].on()
    //theswitch[2].on()
    //sendPush("Motion Detected!")
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    //theswitch.off()
    runIn(60 * minutes, checkMotion)
    def State = themotion.currentState("motion")
    log.debug "Tiempo inicial: ${State.date.time}, Estado: ${State.value}"
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    
    // get the current state object for the motion sensor
    def motionState = themotion.currentState("motion")
    log.debug "Tiempo uno: ${motionState.date.time}, Estado: ${motionState.value}"

    if (motionState.value == "inactive") {
            // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time
		log.debug "Tiempo dos: ${now()}"
        
        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

            if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
            } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
            // Motion active; just log it and do nothing
            log.debug "Motion is active, do nothing and wait for inactive"
    }
}