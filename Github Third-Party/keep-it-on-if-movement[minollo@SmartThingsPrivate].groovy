/**
 *  Brighten My Path
 *
 *  Author: SmartThings
 */

// Automatically generated. Make future change here.
definition(
    name: "Keep it on if movement",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Keep it on if there is movement",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("When there's movement...") {
		input "motion1", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("Turn on something...") {
		input "switch1", "capability.switch", title: "Switches?", multiple: true
	}
	section("Keep it on when I leave for...") {
		input "offTimeout", "number", title: "Minutes?"
	}
    section("Turn off if away...") {
		input "awayMode", "mode", title: "Away mode", require: false
    }
}

def installed()
{
	subscribe(motion1, "motion.active", motionActiveHandler)
	subscribe(motion1, "motion.inactive", motionInactiveHandler)
    subscribe(location, modeChangeHandler)
}

def updated()
{
	unsubscribe()
	subscribe(motion1, "motion.active", motionActiveHandler)
	subscribe(motion1, "motion.inactive", motionInactiveHandler)
    subscribe(location, modeChangeHandler)
}

def modeChangeHandler(evt)
{
	if (evt.value == awayMode) {
		try {unschedule(doOff)} catch(e) {log.error "Ignoring error: ${e}"}
    	switch1.off()
    }
}

def motionActiveHandler(evt) {
	try {unschedule(doOff)} catch(e) {log.error "Ignoring error: ${e}"}
	switch1.on()
}

def motionInactiveHandler(evt) {
	runIn(offTimeout * 60, doOff)
}

def doOff() {
	switch1.off()
}

