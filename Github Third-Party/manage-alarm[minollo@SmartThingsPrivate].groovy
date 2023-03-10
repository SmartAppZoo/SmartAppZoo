/**
 *  Manage Alarm
 *
 *  Author: Carlo Innocenti
 *  Date: 2014-04-02
 */

// Automatically generated. Make future change here.
definition(
    name: "Manage Alarm",
    namespace: "",
    author: "Carlo Innocenti",
    description: "Manage Alarm",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Settings") {
		input "alarm", "capability.Alarm", title: "Alarm device"
		input "awayMode", "mode", title: "Away mode"
	}
	section("Night management") {
		input "sleepAlarmMode", "mode", title: "Sleep with alarm mode", required: false
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
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
    subscribe(location, modeChangeHandler)
    updateState(location.currentMode.name)
}

def pollerEvent(evt) {
    log.debug "[PollerEvent] checkAlarmCommand==${state.checkAlarmCommandLatest}; poll==${state.pollLatest}; now()==${now()}"
    if (state.checkAlarmCommandLatest && (now() - state.checkAlarmCommandLatest) > 660 * 1000) {
        log.error "Checking Alarm Command (timer was asleep?)"
        checkAlarmCommand()
    }
    if (state.pollLatest && (now() - state.pollLatest) > 180 * 1000) {
        log.error "Polling (timer was asleep?)"
        poll()
    }
}

def modeChangeHandler(evt) {
	updateState(evt.value)
}

private updateState(mode) {
	log.debug "updateState(${mode})"
    try {unschedule(checkAlarmCommand)} catch(e) {log.error "Ignoring error: ${e}"}
	if (mode == awayMode) {
    	if (alarm.currentValue("enabled") == "true") {
            alarm.alarmOn()
            state.requested = "away"
            state.checkAlarmCommandLatest = now()
            runIn(600, checkAlarmCommand)
        } else {
        	log.info("Ignoring mode change; alarm is disabled")
        }
    } else if (sleepAlarmMode && mode == sleepAlarmMode) {
    	if (alarm.currentValue("enabled") == "true") {
            alarm.alarmStay()
            state.requested = "stay"
            state.checkAlarmCommandLatest = now()
            runIn(600, checkAlarmCommand)
        } else {
        	log.info("Ignoring mode change; alarm is disabled")
        }
    } else {
    	alarm.alarmOff()
        state.requested = "off"
        state.checkAlarmCommandLatest = now()
        runIn(1200, checkAlarmCommand)
    }
    state.pollLatest = now()
    runIn(120, poll)
}

def checkAlarmCommand() {
	if (alarm.currentValue("alarmStatus") != state.requested) {
    	def msg = "Alarm ${alarm.displayName} switch to ${state.requested} is failing (alarm is ${alarm.currentValue("alarmStatus")})"
        log.error msg
    	sendNotification(msg)
    } else {
    	def msg = "Alarm ${alarm.displayName} successfully switched or confirmed to ${state.requested}"
        log.info msg
    }
    state.checkAlarmCommandLatest = null
}

def poll() {
	log.debug "poll"
	alarm.poll()
    state.pollLatest = null
}

