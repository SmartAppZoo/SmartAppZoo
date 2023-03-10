/**
 *  Motion detector + light + switch
 *
 *  Copyright 2016 Diego
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
    name: "Motion detector + light + switch",
    namespace: "DiegoAntonino",
    author: "Diego",
    description: "Turn on/off light with motion detector \r\nTurn on/off extractor switch if there are movement for 5 min.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn this Light On/Off") {
        input "dimmer", "capability.switchLevel", title: "Which dimmer?", required: true
    }
    section("Turn this Switch On/Off") {
        input "switchSwitch", "capability.switch", required: true
    }
    section("Turn-Switch On when there's been  movement for") {
        input "SwitchSecondsOn", "number", required: true, title: "Seconds?"
    }
    section("Turn-Light Off when there's been no movement for") {
        input "LightSecondsOff", "number", required: true, title: "Seconds?"
    }
    section("Turn-Switch Off when there's been no movement for") {
        input "SwitchSecondsOff", "number", required: true, title: "Seconds?"
    }
    section("DO NOT run when Home Modes...") {
        input "HomeMode", "mode", required: true, title: "Home Modes?", multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    def curMode = location.mode
    
    if (!HomeMode?.find{it == curMode}) {
	    log.debug "Switch Light on"
	    dimmer.setLevel(100)
    }
    runIn(SwitchSecondsOn, checkMotion)
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called from ${evt?.displayName}"
    runIn(LightSecondsOff, checkMotion)    
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    def motionState = themotion.currentState("motion")
    
	if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
    	def elapsed = now() - motionState.date.time + 1000
    	// elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * LightSecondsOff
        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms)"
            dimmer.setLevel(0)
            log.debug "turning Light off"
            
            def SecondsOff = SwitchSecondsOff-LightSecondsOff
            runIn(SecondsOff, checkMotionSwitch)
        }
        else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else { // Motion State is Active
        // get the time elapsed between now and when the motion reported inactive
    	def elapsed = now() - motionState.date.time + 1000
    	// elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * SwitchSecondsOn
        
        if (elapsed >= threshold) {
            log.debug "Motion has stayed active long enough since last check ($elapsed ms)"
            switchSwitch.on()
            log.debug "turning Switch on"
        }
        else {
            log.debug "Motion has not stayed active long enough since last check ($elapsed ms):  doing nothing"
        }
    }
}

def checkMotionSwitch() {
    def motionState = themotion.currentState("motion")
	log.debug "-------checkMotionSwitch"
	if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
    	def elapsed = now() - motionState.date.time + 1000
    	// elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * SwitchSecondsOff
        if (elapsed >= threshold) {
        	log.debug "Motion has stayed inactive long enough since last check ($elapsed ms)"
        	switchSwitch.off()
            log.debug "turning Switch off"
        }
        else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    }
}