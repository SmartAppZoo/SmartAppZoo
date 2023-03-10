/**
 *  Slightly Smarter Night Light
 */
definition(
    name: "Slightly Smarter Night Light",
    namespace: "JoeCraddock",
    author: "Joe Craddock",
    description: "Trying to make a motion detection night light that we like",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("Which dimmers to turn on") {
    input "dimmers", "capability.switchLevel", title: "Which Dimmers?", multiple: true, required: false
	input "level", "number", title: "How bright?, 0-99", required:false
  } 
  
  section("And then off when it's light or there's been no movement for..."){
    input "delayMinutes", "number", title: "Minutes?"
  }
  
  section("Which motion sensors") {
    input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
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
	subscribe(motionSensors, "motion", motionHandler)
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		for (dimmer in dimmers) {
            if (dimmer.currentSwitch == "off") {
                dimmer.setLevel(level)
		state.lastStatus = "active"
		log.debug "motion detected, light is off, turning light on"
            } else {
		log.debug "motion detected, light is on, doing nothing"
            }
        }
	}
	else {
        state.lastStatus = "inactive"
        runIn(60 * delayMinutes ?: 0, inactiveHandler)
	}
}

def inactiveHandler() {
    if (state.lastStatus == "inactive") {
	// check each dimmer to see if the level has been changed. if it has, don't turn it off
        for (dimmer in dimmers) {
            if (dimmer.currentLevel == level) {
                dimmer.off()
		log.debug "no motion, turning light off"
            } else {
		log.debug "no motion, but leaving the light on because someone changed the dim level"
            }
        }
    }
}
