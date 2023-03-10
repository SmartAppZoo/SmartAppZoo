/**
 *  My Custom Auto Door Lock
 *
 *  Copyright 2017 Robert Boyd
 *  new text 2022
 */
definition(
    name: "Door Lock/Unlock by Sensor",
    namespace: "robrtb",
    author: "Robert Boyd",
    description: "Door lock/unlock by sensor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select Door lock:") {
        input "theLock", title: "Door lock", "capability.lock", required: true
    }
    section("Select Contact Sensor trigger") {
        input "theContactSensor", title: "Contact Sensor", "capability.contactSensor", required: false
        input "theContactSensorState", type: "enum", title: "Contact Sensor State", defaultValue: "closed", options: ["closed", "open"]
        input "theContactDelay", "number", title: "Delay (Minutes)", description: "Delay to wait before lock action occurs", defaultValue: 0, required: false
    }
    /*
    section("Select Presence Sensor trigger") {
        input "thePresenceSensor", title: "Presence Sensor", "capability.presenceSensor", required: false
        input "thePresenceSensorState", type: "enum", title: "Presence Sensor State", options: ["present", "not present"], defaultValue: "not present"
        input "thePresenceDelay", "number", title: "Delay (Minutes)", description: "Delay to wait before lock action occurs", defaultValue: 0, required: false
    }
    section("Select Motion Sensor trigger") {
        input "theMotionSensor", title: "Motion Sensor", "capability.motionSensor", required: false
        input "theMotionSensorState", type: "enum", title: "Motion Sensor State", options: ["active", "inactive"], defaultValue: "inactive"
        input "theMotionDelay", "number", title: "Delay (Minutes)", description: "Delay to wait before lock action occurs", defaultValue: 0, required: false
    }
    */
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	if (settings.theContactSensor) {
    	subscribe(theContactSensor, "contact", contactSensorHandler)		//   complete Handler - chk for presence state
    }
    if (settings.thePresenceSensor) {
    	subscribe(thePresenceSensor, "presence", presenceSensorHandler)		//   complete Handler - chk for presence state
    }
    if (settings.theMotionSensor) {
    	subscribe(theMotionSensor, "motion", motionSensorHandler)		//   complete Handler - chk for motion state
    }
}

def contactSensorHandler(evt) {
    if (evt.value == settings.theContactSensorState) {
    	//log.debug "Correct state!"
        if (!settings.theContactDelay || settings.theContactDelay < 1) {
        	settings.theContactDelay = 0
        }
        runIn(60 * settings.theContactDelay, performLock)
        //log.debug "RunIn is set to 5 secs for testing"
        //runIn(5, performLock)
    }
}

def presenceSensorHandler(evt) {
	log.debug "presenceSensorHandler called: $evt.value"
}

def motionSensorHandler(evt) {
	log.debug "motionSensorHandler called: $evt.value"
}


def performLock() {
	//def lockState = theLock.currentState("lock")
    
    if (getLockState() == "unlocked") {
    	log.debug "Locking lock..."
    	theLock.lock()
        if (getLockState() == "locked") {
        	log.debug "Lock has been locked!"
        }
        runIn(15, validateLocked)
    }
}

def validateLocked() {
	if (getLockState() == "unlocked") {
    	log.debug "***** Locking lock inside validateLocked()..."
    	theLock.lock()
    }
    else {
    	log.debug "***** Lock has been validated to be locked!"
    }
}

def getLockState() {
    return theLock.lockState.value
}