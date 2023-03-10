/**
 *  Night and Day 2
 *
 *  Author: Minollo
 *
 *  Date: 2013-11-04
 */

// Automatically generated. Make future change here.
definition(
    name: "Night and Day 2",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Night and Day 2",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section ("Day modes...") {
        input "awayMode", "mode", title: "Away mode?", required: true
		input "dayMode", "mode", title: "Day mode?", required: true
		input "nightMode", "mode", title: "Night mode?", required: true
		input "sleepMode", "mode", title: "Sleep mode?", required: false
		input "sleepAlarmMode", "mode", title: "Sleep with alarm mode?", required: false
    }
	section ("Weekdays mode change times...") {
    	input "dayTimeWeek", "time", title: "Switch to day mode at?", required: true
    	input "nightTimeWeek", "time", title: "Switch to night mode at?", required: true
    	input "sleepTimeWeek", "time", title: "Switch to sleep mode at?", required: false
    	input "sleepAlarmTimeWeek", "time", title: "Switch to sleep with alarm mode at?", required: false
	}
	section ("Weekend mode change times...") {
    	input "dayTimeWeekend", "time", title: "Switch to day mode at?", required: true
    	input "nightTimeWeekend", "time", title: "Switch to night mode at?", required: true
    	input "sleepTimeWeekend", "time", title: "Switch to sleep mode at?", required: false
    	input "sleepAlarmTimeWeekend", "time", title: "Switch to sleep with alarm mode at?", required: false
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
	unschedule()
	if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	subscribe(location, modeChange)
	//schedule("0 0/5 * * * ?", timeMonitor)    
    //runEvery5Minutes(timeMonitor)
    timeMonitor()
}

def pollerEvent(evt) {
	log.debug "[PollerEvent]"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 450000) {
    	log.error "Waking up timer"
    	timeMonitor()
    }
}

def timeMonitor() {
	log.debug "[Night&Day2] timeMonitor request..."
    runIn(300, timeMonitor)
	state.keepAliveLatest = now()
    if (state.latestMode != awayMode) {	//check and process current mode only if latest set mode was not away
        if (state.latestMode != location.mode) {
            if (state.modifiedLatestMode != location.mode) {
                state.modifiedLatestMode = location.mode
                log.warn "Mode seems to have changed (to ${location.mode}); but let's wait one more polling cycle..."
            } else {
                log.warn "Mode has changed to ${location.mode} (checked twice); processing change"
                processCurrentMode(location.currentMode.name)
            }
        } else {
            processCurrentMode(location.mode)
        }
	} else {
    	state.modifiedLatestMode = null
    }
}


def modeChange(evt) {
	log.debug "[Night&Day2] modeChange: ${evt}, ${evt.source}, ${evt.value}..."
//    runIn(10, verifyCurrentMode)
    processCurrentMode(evt.value)
}

def verifyCurrentMode() {
	log.debug "[Night&Day2] verifyCurrentMode request..."
    processCurrentMode(location.currentMode.name)
}

private processCurrentMode(oldMode) {
	log.debug "[Night&Day2] processCurrentMode(${oldMode})..."
    log.debug "[Night&Day2] now: ${now()}, state: ${state}"
    log.debug "SleepAlarmMode == ${sleepAlarmMode}"
    state.modifiedLatestMode = null
	state.latestMode = oldMode
	getCurrentDateInfo()
    if (oldMode == dayMode || oldMode == nightMode || (sleepMode && oldMode == sleepMode) || (sleepAlarmMode && oldMode == sleepAlarmMode)) {
        def nowTime = now()
        if (!state.IsWeekend) {
        	if (sleepMode && state.sleepTimeWeekTime && nowTime > state.sleepTimeWeekTime) {
            	log.info "check1"
                changeMode(oldMode,sleepMode)
            } else if(nowTime > state.nightTimeWeekTime) {
            	log.info "check2"
            	changeMode(oldMode,nightMode)
            } else if(nowTime > state.dayTimeWeekTime) {
            	log.info "check3"
            	changeMode(oldMode,dayMode)
            } else if (sleepAlarmMode && state.sleepAlarmTimeWeekTime && nowTime > state.sleepAlarmTimeWeekTime) {
            	log.debug "check4"
            	changeMode(oldMode,sleepAlarmMode)
            } else {
            	if (sleepMode) {
            		log.debug "check5"
            		changeMode(oldMode,sleepMode)
                } else {
            		log.debug "check6"
                	changeMode(oldMode,nightMode)
                }
            }
		} else {
        	if (sleepMode && state.sleepTimeWeekendTime && nowTime > state.sleepTimeWeekendTime) {
            	log.debug "check7"
            	changeMode(oldMode,sleepMode)
            } else if(nowTime > state.nightTimeWeekendTime) {
            	log.debug "check7"
            	changeMode(oldMode,nightMode)
            } else if(nowTime > state.dayTimeWeekendTime) {
            	log.debug "check8"
            	changeMode(oldMode,dayMode)
            } else if (sleepAlarmMode && state.sleepAlarmTimeWeekendTime && nowTime > state.sleepAlarmTimeWeekendTime) {
            	log.debug "check9"
            	changeMode(oldMode,sleepAlarmMode)
            } else {
            	if (sleepMode) {
	            	log.debug "check10"
            		changeMode(oldMode,sleepMode)
                } else {
	            	log.debug "check11"
                	changeMode(oldMode,nightMode)
                }
            }
        }
	}
    log.debug "[Night&Day2] state: $state"
}

def changeMode(oldMode,newMode) {
	log.debug "[Night&Day2] Processing change mode to '${newMode}'"
	if (oldMode == dayMode || oldMode == nightMode || (sleepMode && oldMode == sleepMode) || (sleepAlarmMode && oldMode == sleepAlarmMode)) {
        if (newMode) {	// && oldMode != newMode) {
            if (location.modes?.find{it.name == newMode}) {
            	state.latestMode = newMode
                location.setMode(newMode)
                log.debug "[Night&Day2] has changed the mode to '${newMode}'"
            }
            else {
                log.debug "[Night&Day2] tried to change to undefined mode '${newMode}'"
            }
        }
    } else {
    	log.debug "[Night&Day2] Ignoring change to '${newMode}'; current mode is away"
    }
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getCurrentDateInfo() {
    log.debug "[Night&Day2] getCurrentDateInfo..."
	
    state.dayTimeWeekTime = timeToday(dayTimeWeek, location.timeZone).getTime()
	state.nightTimeWeekTime = timeToday(nightTimeWeek, location.timeZone).getTime()
    if (sleepTimeWeek) {
		state.sleepTimeWeekTime = timeToday(sleepTimeWeek, location.timeZone).getTime()
    } else {
    	state.sleepTimeWeekTime = null
	}
    if (sleepAlarmTimeWeek) {
		state.sleepAlarmTimeWeekTime = timeToday(sleepAlarmTimeWeek, location.timeZone).getTime()
    } else {
    	state.sleepAlarmTimeWeekTime = null
    }
    
	state.dayTimeWeekendTime = timeToday(dayTimeWeekend, location.timeZone).getTime()
	state.nightTimeWeekendTime = timeToday(nightTimeWeekend, location.timeZone).getTime()
    if (sleepTimeWeekend) {
		state.sleepTimeWeekendTime = timeToday(sleepTimeWeekend, location.timeZone).getTime()
    } else {
    	state.sleepTimeWeekendTime = null
    }
    if (sleepAlarmTimeWeekend) {
		state.sleepAlarmTimeWeekendTime = timeToday(sleepAlarmTimeWeekend, location.timeZone).getTime()
    } else {
    	state.sleepAlarmTimeWeekendTime = null
    }

	def today = new Date(now())
    def dayOfWeek = today.toString().substring(0,3)
    isWeekend(dayOfWeek)
    log.debug "[Night&Day2] Is today a weekend day? ${state.IsWeekend}"
}

private isWeekend(day) {
	if (day == "Sat" || day == "Sun")
    	state.IsWeekend = true;
    else
    	state.IsWeekend = false;
}
