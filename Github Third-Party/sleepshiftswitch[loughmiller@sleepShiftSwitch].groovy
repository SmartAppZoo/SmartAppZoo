/**
 *  Sleep Shift Switch
 *
 *  Copyright 2016 Scott loughmiller
 *
 * MIT License
 */
definition(
    name: "Sleep Shift Switch",
    namespace: "loughmiller",
    author: "Scott loughmiller",
    description: "Set bulb to proper brightness and temperature when turned on.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Title") {
		// TODO: put inputs here
	}

    section("Virtual Switch?") {
        input "virtualSwitch", "capability.switch", required: true
    }

    section("Lights?") {
        input "lLevel", "capability.switchLevel", title: "level", required: false, multiple: true
        input "lTemp", "capability.colorTemperature", title: "temp", required: false, multiple: true
    }

	  section("Nightlight?") {
        input "nlLevel", "capability.switchLevel", title: "level", required: false, multiple: true
        input "nlTemp", "capability.colorTemperature", title: "temp", required: false, multiple: true
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
	  subscribe(virtualSwitch, "switch.on", switchOnHandler)
    subscribe(virtualSwitch, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
    log.info "switch on"
	def currentMode = location.mode
    log.info "mode: ${currentMode}"
    if (currentMode == "Bedtime") {
        log.info "switch turned on, and it's bedtime"
		nlLevel.setLevel(2)
        nlTemp.setColorTemperature(1900)
	} else if (currentMode == "Night") {
		log.info "switch turned on, and it's nighttime!"
		nlLevel.setLevel(100)
        nlTemp.setColorTemperature(2300)
	} else if (currentMode == "Evening") {
		log.info "switch turned on, and it's evening!"
		nlLevel.setLevel(100)
        nlTemp.setColorTemperature(2700)
		lLevel.setLevel(100)
        lTemp.setColorTemperature(2700)
    } else {
		log.info "switch turned on, and it's daytime!"
        nlLevel.setLevel(100)
        nlTemp.setColorTemperature(4500)
		lLevel.setLevel(100)
        lTemp.setColorTemperature(4500)
    }
}
def switchOffHandler(evt) {
    log.info "switch turned off!"
    nlLevel.setLevel(0)
	  lLevel.setLevel(0)
}
