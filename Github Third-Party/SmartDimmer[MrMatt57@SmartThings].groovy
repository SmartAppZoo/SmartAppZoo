/**
 *  Smart Dimmer
 *
 *  Derrived from: SmartThings
 *  Author: mwwalker@gmail.com
 *  Date: 2014-01-12
 */
preferences {
    section("Control these dimmers..."){
		input "dimmers", "capability.switchLevel", multiple: true
		input "dimmerDayLevel", "number", title: "Dimmer Day Level?"
        input "dimmerNightLevel", "number", title: "Dimmer Night Level?"
        input "dayBegin", "time", title: "Day Begin?"
        input "dayEnd", "time", title: "Day End?"
	}
	section("Turning on when there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?"
	}
	section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	subscribe(motionSensor, "motion", motionHandler)
	schedule("0 * * * * ?", scheduleCheck)
}


def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
        log.debug "setting dimmer level"
        setAllDimmers()
        state.lastStatus = "on"
		state.motionStopTime = null
	}
	else {
		state.motionStopTime = now()
	}
}

def scheduleCheck() {
	log.debug "Checking status"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
		if (elapsed >= (delayMinutes ?: 0) * 60000L) {
			dimmers?.setLevel(0)
			state.lastStatus = "off"
		}
	}
}

def setAllDimmers() {
    def startDay = timeToday(dayBegin)
    def endDay = timeToday(dayEnd)
    def now = new Date()
    
    dimmers.each { dimmer ->
        
        if(startDay.before(now) && endDay.after(now))
        {
            log.debug "setAllDimmers: Day Dimmer Level setting dimmer to level ${dimmerDayLevel}"
            dimmer.setLevel(dimmerDayLevel)
        }
        else {
            log.debug "setAllDimmers: Night Dimmer Level setting dimmer to level ${dimmerNightLevel}"
            dimmer.setLevel(dimmerNightLevel)
        }
        
    }
}