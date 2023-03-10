definition(
    name: "Feed the kitties",
    namespace: "augoisms",
    author: "augoisms",
    description: "Turn on one or more switches at a specified time and turn them off at a later time.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png"
)

preferences {
	section("Select switches to dim...") {
		input name: "dimmers", type: "capability.switchLevel", multiple: true
	}
    section("Select switches to turn on/off...") {
		input name: "switches", type: "capability.switch", multiple: true
	}
	section("Turn them all on at...") {
		input name: "startTime", title: "Turn On Time?", type: "time"
	}
	section("And turn them off at...") {
		input name: "stopTime", title: "Turn Off Time?", type: "time"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(startTime, "startTimerCallback")
	schedule(stopTime, "stopTimerCallback")

}

def updated(settings) {
	unschedule()
	schedule(startTime, "startTimerCallback")
	schedule(stopTime, "stopTimerCallback")
}

def startTimerCallback() {
	log.debug "Turning on switches"
	//switches.on()
    dimmers.setLevel(30)

}

def stopTimerCallback() {
	log.debug "Turning off switches"
    dimmers.setLevel(100)
	switches.off()
}