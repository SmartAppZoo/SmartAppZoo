/**
 *  test
 *
 *  Author: Carlo Innocenti
 *  Date: 2014-04-13
 */

// Automatically generated. Make future change here.
definition(
    name: "Timer",
    namespace: "",
    author: "Carlo Innocenti",
    description: "Timer",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)


preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section ("Switches") {
		input "switches", "capability.switch", title: "Switches", multiple: true, required: true
    }
	section ("Daily Event #1") {
    	input "timeOnAt1", "time", title: "Switch on at?", required: true
    	input "timeOffAt1", "time", title: "Switch off at?", required: true
        input "enabled1", "bool", title: "Enabled", required: true, defaultValue: true
	}
	section ("Daily Event #2") {
    	input "timeOnAt2", "time", title: "Switch on at?", required: false
    	input "timeOffAt2", "time", title: "Switch off at?", required: false
        input "enabled2", "bool", title: "Enabled", required: false
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.manualOn = false
    state.manualOff = false
	unschedule()
	//schedule("0 0/5 * * * ?", timeMonitor)    
    //runEvery5Minutes(timeMonitor)
    //runIn(300, timeMonitor)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	subscribe(app, appTouch)
    timeMonitor()
}

def pollerEvent(evt) {
	log.debug "[PollerEvent]"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 450000) {
    	log.error "Waking up timer"
    	timeMonitor()
    }
    if (state.checkOnScheduledAt && (now() - state.checkOnScheduledAt) > 60 * 1000) {
    	log.error "Waking up checkOn timer"
        checkOn()
    }
    if (state.checkOffScheduledAt && (now() - state.checkOffScheduledAt) > 60 * 1000) {
    	log.error "Waking up checkOff timer"
        checkOff()
    }
}

def checkOn() {
    state.checkOnScheduledAt = null
	def switchesAreOn = (switches[0].currentValue("switch") == "on")
   	log.debug "checkOn: switches are ${switchesAreOn ? "on" : "off"}"
    if (!switchesAreOn) {
        log.info "Switching switches ON again..."
        switches.on()
        state.checkOnScheduledAt = now() + 60 * 1000
        runIn(60, checkOn)
   	}
}

def checkOff() {
    state.checkOffScheduledAt = null
	def switchesAreOn = (switches[0].currentValue("switch") == "on")
   	log.debug "checkOff: switches are ${switchesAreOn ? "on" : "off"}"
    if (switchesAreOn) {
        log.info "Switching switches OFF again..."
        switches.off()
        state.checkOffScheduledAt = now() + 60 * 1000
        runIn(60, checkOff)
   	}
}

def timeMonitor() {
    runIn(300, timeMonitor)
    state.keepAliveLatest = now()
    def checkTime
    def checkDate
    def checked = false
    def nowTime = now()
    def switchesAreOn = (switches[0].currentValue("switch") == "on")
    log.debug "Switches are ${switchesAreOn ? "on" : "off"}; manualOn == ${state.manualOn}; manualOff == ${state.manualOff}"

    if (timeOffAt2 && enabled2) {
        checkDate = timeToday(timeOffAt2, location.timeZone)
        checkTime =checkDate.getTime()
        if (nowTime > checkTime) {
        	log.debug "Time past timeOffAt2"
            checked = true
            state.manualOff = false
            if (switchesAreOn && state.manualOn == false) {
        		log.info "Switching off switches (${checkDate})"
                switches.off()
		        state.checkOffScheduledAt = now() + 60 * 1000
                runIn(60, checkOff)
            }
        } else {
            checkDate = timeToday(timeOnAt2, location.timeZone)
            checkTime =checkDate.getTime()
            if (nowTime > checkTime) {
	        	log.debug "Time past timeOnAt2"
                checked = true
                state.manualOn = false
                if (!switchesAreOn && state.manualOff == false) {
	        		log.info "Switching on switches (${checkDate})"
                    switches.on()
			        state.checkOnScheduledAt = now() + 60 * 1000
	                runIn(60, checkOn)
                }
            }
        }
    }
    if (enabled1 && !checked) {
        checkDate = timeToday(timeOffAt1, location.timeZone)
        checkTime =checkDate.getTime()
        if (nowTime > checkTime) {
        	log.debug "Time past timeOffAt1"
            checked = true
            state.manualOff = false
            if (switchesAreOn && state.manualOn == false) {
        		log.info "Switching off switches (${checkDate})"
                switches.off()
		        state.checkOffScheduledAt = now() + 60 * 1000
                runIn(60, checkOff)
            }
        } else {
            checkDate = timeToday(timeOnAt1, location.timeZone)
            checkTime =checkDate.getTime()
            if (nowTime > checkTime) {
	        	log.debug "Time past timeOnAt1"
                checked = true
                state.manualOn = false
                if (!switchesAreOn && state.manualOff == false) {
	        		log.info "Switching on switches (${checkDate})"
                    switches.on()
			        state.checkOnScheduledAt = now() + 60 * 1000
	                runIn(60, checkOn)
                }
            }
        }
    }
    if (!checked) {
        if (switchesAreOn && state.manualOn == false) {
            state.manualOff = false
			log.info "Switching off switches (no previous timer events found for the day)"
            switches.off()
	        state.checkOffScheduledAt = now() + 60 * 1000
            runIn(60, checkOff)
        } else if (!switchesAreOn && state.manualOn == true) {
	        log.info "Switching switches ON again..."
            switches.on()
	        state.checkOnScheduledAt = now() + 60 * 1000
            runIn(60, checkOn)
        }
    }
}

def appTouch(evt) {
	log.debug "appTouch: $evt, $settings"
    def switchesAreOn = (switches[0].currentValue("switch") == "on")
    log.debug "Switches are ${switchesAreOn ? "on" : "off"}"
    try {unschedule(checkOn)} catch(e) {log.error "Ignoring error: ${e}"}
    try {unschedule(checkOff)} catch(e) {log.error "Ignoring error: ${e}"}
    state.manualOff = false
    state.manualOn = false
    if (switchesAreOn) {
    	state.manualOff = true
        log.info "Switching off switches"
        switches.off()
        state.checkOffScheduledAt = now() + 60 * 1000
        runIn(60, checkOff)
    } else {
    	state.manualOn = true
        log.info "Switching on switches"
        switches.on()
        state.checkOnScheduledAt = now() + 60 * 1000
        runIn(60, checkOn)
    }
}

// catchall
def event(evt)
{
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}


