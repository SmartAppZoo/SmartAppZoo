/**
 *  Author: Baur
 */

definition(
    name: "Flash Lighting",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Flash selected lights repeatedly",
    category: "Convenience",
    parent: "baurandr:Baur Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Flash lights when this switch is active..."){
		input "activationSwitch", "capability.switchState", title: "Which switch?"
	}
	section("How long to flash for..."){
		input "minutes1", "number", title: "Minutes?"
	}
	section("Flash these light(s)..."){
		input "switches", "capability.switchLevel", multiple: true
	}
}

def installed() {
	initFlash()
	subscribe(activationSwitch, "switch", switchHandler)
}

def updated() {
	unsubscribe()
    initFlash()
	subscribe(activationSwitch, "switch", switchHandler)
}

def switchHandler(evt) {
	log.debug "$evt.name: $evt.value"

	def offSwitches = state.switchesToTurnOff
    if (evt.value == "on"){ //flash switch turned on
    		log.debug "flash switches"
            switches.flash()
	} else if (evt.value == "off") {
                //runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
	}
}

def initFlash() {
    log.debug "Initializing" 
}