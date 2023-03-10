/**
 *  Sensor Debounce
 *
 *  Copyright 2017 Andrew Baur
 *
 */
definition(
    name: "Motion Sensor Debounce",
    namespace: "baurandr",
    author: "Andrew Baur",
    description: "Use to debounce sensor(s) and combine into one output value",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Motion Sensor Input to Debounce"){
		input "motionIn", "capability.motionSensor", title: "Inputs"
	}
    section("Motion Sensor To Use as Output"){
		input "motionOut", "capability.motionSensor", title: "Output"
	}
	section("Motion Inactive After How Many Seconds?"){
		input "seconds1", "number", title: "Seconds?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(motionIn, "motion", motionHandler)
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    subscribe(motionIn, "motion", motionHandler)
	initialize()
}

def initialize() {
	log.debug "Initializing"
}

def motionHandler(evt) {
    log.debug "$evt.device : $evt.name : $evt.value"
	
    
    if (evt.value == "active"){ //Motion has started
        def motionState = motionOut.currentState("motion")
        if (motionState.value == "inactive") {
    		motionOut.active()
        }
    } else if (evt.value == "inactive") {
        runIn(seconds1, scheduleCheck, [overwrite: false])
    }
}

def scheduleCheck() {
	log.debug "schedule check"
	def motionState = motionIn.currentState("motion")
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
    	def threshold = 1000 * seconds1 - 1000
    	if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  set motion inactive"
            motionOut.inactive()
    	} else {
        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Motion is active, do nothing and wait for inactive"
    }
}