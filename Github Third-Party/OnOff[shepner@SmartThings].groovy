
definition(
    name: "OnOff",
    namespace: "shepner",
    author: "Stephen Hepner",
    description: "Turn things on or off",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("Title") {
    section("Turn on when motion detected:") {
      input "motion1", "capability.motionSensor", required: true, title: "Where?"
    }
    section("turn on this switch") {
      input "switch1", "capability.switch", required: true
		}
	}
}

def installed() {
  subscribe(motion1, "motion.active", motionDetectedHandler)
  subscribe(switch1, "switch.on", switchOnHandler)
  subscribe(switch1, "switch.off", switchOffHandler)

  log.debug "Installed with settings: ${settings}"
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unsubscribe()
  initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}


// Event handlers /////////////////////////////////////////////////////////////////////////////

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
}

def switchOnHandler(evt) {
    log.debug "switchOnHandler called: $evt"
}

def switchOffHandler(evt) {
    log.debug "switchOffHandler called: $evt"
}



