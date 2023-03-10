/**
 *  I am home
 *
 *  Author: mwwalker@gmail.com
 *  Date: 2014-02-11
 */
preferences {
	section("When this person(s) arive...") {
		input "person", "capability.presenceSensor", title: "Who to Monitor:"
        input "falseAlarmThreshold", "decimal", title: "False alarm threshold minutes (default 10):", required: false
	}
    section("Turn on these switches...") {
		input "switchDevices", "capability.switch", title: "Switch(s):", required: false, multiple: true
        input "switchHowLongMinutes", "number", title: "Minutes (default 5):", required: false
	}
    section("Turn on these dimmers...") {
		input "dimmerDevices", "capability.switchLevel", title: "Dimmer(s):", required: false, multiple: true
		input "dimmerLevel", "number", required: false, title: "Dimmer level (default 100):"
        input "dimmerHowLongMinutes", "number", title: "How many minutes to leave on (default 5):", required: false
	}
    section("Turn off when this device is switched/opened...") {
    	input "TurnOffContactDevices", "capability.contactSensor", title: "Contact(s):", required: false, multiple: true
		input "TurnOffSwitchDevices", "capability.switch", title: "Swtiches(s):", required: false, multiple: true
	}
    
    section("Options...") {
		input "preserveState", "enum", title: "Preserve switch/dimmer state?", metadata:[values:["Yes","No"]]
        input "activeAfterSunset", "enum", title: "Only active after sunset?", metadata:[values:["Yes","No"]]
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
    if (person) {
    	subscribe(person, "presence", someoneArived)
    }
    if(TurnOffContactDevices) {
    	subscribe(TurnOffContactDevices, "contact.open", turnOffActived)
    }
    if(TurnOffSwitchDevices) {
    	subscribe(TurnOffSwitchDevices, "switch.on", turnOffActived)
    }
}

def someoneArived(evt) {
	log.debug "$evt.name: $evt.value"
    
	if(evt.value == "present") {
    	def findFalseAlarmThreshold = findFalseAlarmThreshold ?: 10
    	def threshold = 1000 * 60 * findFalseAlarmThreshold - 1000
    	def currentDate = new Date()
        log.debug("currentDate: $currentDate")
        def lastLeft = state.lastLeft ?: now()
        lastLeft = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", lastLeft)
        log.debug("lastLeft: $lastLeft")
        def elapsed = now() - lastLeft.time
        if (elapsed >= threshold) {
        	log.debug("Away long enough, activated arrival")
            activateArrival()
        }
       	else {
        	log.info("Not gone long enough, false positive?")
        }
    }
   	else {
        state.lastLeft = new Date()
        log.debug("Last left: ${state.lastLeft}")
    }
}

def activateArrival() {

    if(activeAfterSunset == "Yes") {
        def sunInfo = getSunriseAndSunset()
        def currentTime = new Date()
        log.debug("Sunrise: ${sunInfo.sunrise}")
        log.debug("Current time: $currentTime")
        if(currentTime.before(sunInfo.sunrise)) {
            log.info("Time is before sunset, exiting")
            return
        }
    }

    if(preserveState == "Yes") {
    	saveState()
    }
    
    state.active = "Yes"
    def dimmerLevel = dimmerLevel ?: 100
    switchDevices?.on()
    dimmerDevices?.setLevel(dimmerLevel)
    
    if (switchDevices) {
        def switchHowLongMinutes = switchHowLongMinutes ?: 5
        runIn(switchHowLongMinutes * 60, switchTimeout)
    }
    
    if(dimmerDevices) {
    	def dimmerHowLongMinutes = dimmerHowLongMinutes ?: 5
    	runIn(dimmerHowLongMinutes * 60, dimmerTimeout)
    }
}

def switchTimeout() {
	if(preserveState == "Yes") {
    	restoreSwitchState()
    }
    else {
    	switchDevices.off()
    }
    if(switchHowLongMinutes > dimmerHowLongMinutes) {
    	state.active = "No"
    }
}

def dimmerTimeout() {
	if(preserveState == "Yes") {
    	restoreDimmerState()
    }
    else {
    	dimmerDevices.off()
    }
    if(dimmerHowLongMinutes > switchHowLongMinutes) {
    	state.active = "No"
    }
}

def turnOffActived(evt) {
	if(state.active == "Yes") {
    	switchTimeout()
    	dimmerTimeout()
    }
}

private restoreSwitchState()
{
	def mode = "App"
	log.info "restoring state"
	def map = state[mode] ?: [:]
	switchDevices?.each {
		def value = map[it.id]
		if (value?.switch == "on") {
        	log.debug "leaving on $it.label"
		}
		else {
			log.debug "turning $it.label off"
			it.off()
		}
	}
}

private restoreDimmerState()
{
	def mode = "App"
	log.info "restoring state"
	def map = state[mode] ?: [:]
    
    dimmerDevices?.each {
		def value = map[it.id]
		if (value?.switch == "on") {
			def level = value.level
			if (level != it.currentLevel) {
				log.debug "setting $it.label level to $level"
				it.setLevel(level)
			}
			else {
				log.debug "leaving on $it.label"
			}
		}
		else {
			log.debug "turning $it.label off"
			it.off()
		}
	}
}

private saveState()
{
	def mode = "App"
	def map = state[mode] ?: [:]

	switchDevices?.each {
		map[it.id] = [switch: it.currentSwitch]
	}
    
    dimmerDevices?.each {
		map[it.id] = [switch: it.currentSwitch, level: it.currentLevel]
	}

	state[mode] = map
	log.debug "saved state: ${state[mode]}"
	log.debug "state: $state"
}

