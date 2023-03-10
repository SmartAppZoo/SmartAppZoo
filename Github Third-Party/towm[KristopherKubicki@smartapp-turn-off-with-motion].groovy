/**
 *  Turn Off With Motion - This is a simple SmartApp that turns off a device if motion is detected
 *
 */
definition(
    name: "Turn Off With Motion",
    namespace: "KristopherKubicki",
    author: "Kristopher Kubicki",
    description: "Turns off a device if there is motion",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png")



preferences {
	section("Turn off when there's movement..."){
		input "motions", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("And on when there's been no movement for..."){
		input "minutes1", "number", title: "Minutes?"
	}
	section("Turn off/on light(s)..."){
		input "switches", "capability.switch", multiple: true
	}
}


def installed() {
   initialized()
}

def updated() {
	unsubscribe()
    initialized()
}

def initialized() {
    subscribe(motions, "motion", motionHandler)
}

def motionHandler(evt) {
//	log.debug "$evt.name: $evt.value"

	if (evt.value == "active") {
//		log.debug "turning off devices"
		switches.off()
		state.inactiveAt = null
	} else if (evt.value == "inactive") {
		if (!state.inactiveAt) {
			state.inactiveAt = now()
            runIn(minutes1 * 60, "scheduleCheck", [overwrite: false])
		}
	}
}

def scheduleCheck() {
//	log.debug "schedule check, ts = ${state.inactiveAt}"
	if (state.inactiveAt) {
		def elapsed = now() - state.inactiveAt
		def threshold = 1000 * 60 * minutes1
		if (elapsed >= threshold) {
//			log.debug "turning off lights"
			switches.on()
			state.inactiveAt = null
		}
		else {
//			log.debug "${elapsed / 1000} sec since motion stopped"
		}
	}
}
