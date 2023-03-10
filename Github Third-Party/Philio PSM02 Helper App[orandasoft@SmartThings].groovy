/**
 *  Philio PSM02 Helper App
 *
 *  Copyright 2014 Paul Spee All Rights Reserved
 *
 */
definition(
    name: "Philio PSM02 Helper App",
    namespace: "pspee",
    author: "Paul Spee",
    description: "The Philio PSM02 Helper App will subscribe to motion active events from the Philio PSM02 multi-sensor and send a motion inactive event.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select Philio motion sensor...") {
	    input "motion1", "capability.motionSensor", title: "Which sensor?", required: false, multiple: true
    }
}

def installed() {
	log.debug "PSM02: Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "PSM02: Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(motion1, "motion.active", motionActiveHandler)
}

// Because the Philio PSM02 does not send an motion inactive event,
// we send an inactive event to the device when motion is active
def motionActiveHandler(evt) {
	log.debug "PSM02: motionActiveHandler called with event ${evt.descriptionText} from ${evt.displayName} with value ${evt.value}"
    
    def dev = settings.motion1.find() { it.id == evt.deviceId }
    if (dev) {
		sendEvent(dev, [name: "motion", value: "inactive"])
	}
}
