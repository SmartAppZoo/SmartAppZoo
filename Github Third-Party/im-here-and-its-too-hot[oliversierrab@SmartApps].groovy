definition(
    name: "I'm here and it's too hot",
    namespace: "oliversierrab",
    author: "Oliver Sierra",
    description: "Turning Fan switch on when motion detected and it's hot",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png"
)

preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Monitor the temperature") {
		input "thetemp", "capability.temperatureMeasurement"
	}
	section("When the temperature rises above") {
		input "temperature1", "number", title: "Temperature?"
	}
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    section("Turn on this switch") {
        input "theswitch", "capability.switch", required: true
    }
    section("Turn on between what times?") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
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
    def turnOn = checkTempAndTime()
    log.debug "Should it turn on? ${turnOn}"
    
    if (turnOn) {
        theswitch.on()
    }
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

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

def checkTempAndTime() {
	def tempState = thetemp.currentState("temperature")
    log.trace "temperature: $tempState.doubleValue"
    def tooHot = temperature1

    if (tempState.doubleValue > tooHot) {
        def between = (fromTime && toTime) ? timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone) : true
        
        if (between) {
            log.debug "Temperature rose above $tooHot:  activating $theswitch"
			return true
        }
    }
    
    return false
}