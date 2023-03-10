/**
 *  Sleep Motion detect
 *
 *  Copyright 2015 Erik Vennink
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

/************
 * Metadata *
 ************/

definition(
    name: "Sleep Motion detect",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Automatically set mode to sleep or day/night in the evening and morning, based on motion sensors. ",
    category: "SmartThings Labs",
    iconUrl: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png",
    iconX3Url: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png")

/**********
 * Setup  *
 **********/

preferences {
	section ("Sleep mode...") {
		input "sleepMode", "mode", title: "Change mode to?", required: true
		input "sleepOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sleepOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
        input "sleepMotion", "capability.motionSensor", title: "If no motion here", required: false, multiple: true
        input "sleepDelay", "number", title: "For x minutes", required: false
        input "sleepStarting", "time", title: "Starting from", required: false
        input "sleepEnding", "time", title: "Ending at", required: false
        input "sleepModeCheck", "mode", title: "Only when mode is", multiple: true, required: false
	}
	section ("Wake up mode...") {
		input "wakeUpMode", "mode", title: "Change mode to?", required: true
		input "wakeUpOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "wakeUpOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
        input "wakeUpMotion", "capability.motionSensor", title: "If motion here", required: false, multiple: true
        input "wakeUpStarting", "time", title: "Starting from", required: false
        input "wakeUpEnding", "time", title: "Ending at", required: false
        input "wakeUpModeCheck", "mode", title: "Only when mode is", multiple: true, required: false
	}
}

/*************************
 * Installation & update *
 *************************/

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
    initialize()
}

def initialize() {
	subscribe(sleepMotion, "motion", sleepHandler)
	subscribe(wakeUpMotion, "motion", wakeHandler)
}

/******************
 * Event handlers *
 ******************/

def sleepHandler(evt) {
	log.trace "sleepHandler"
	def current = sleepMotion.currentValue("motion")
	def motionValue = sleepMotion.find{it.currentMotion == "active"}
	if (motionValue) {
    	log.info "Motion, so not ready to sleep"
		state.motionStopTime = null
	}
	else {
    	log.info "No Motion, so getting ready to go to sleep whenever the time is there"
        state.motionStopTime = now()
        if(sleepDelay) {
            runIn(sleepDelay*60, goToSleep, [overwrite: false])
            log.info "Delay (motion): $sleepDelay minutes"
        } 
        else {
            goToSleep()
        }
	}
}

def wakeHandler(evt) {
	log.trace "wakeHandler"
	def current = wakeUpMotion.currentValue("motion")
	def motionValue = wakeUpMotion.find{it.currentMotion == "active"}
	if (motionValue) {
    	log.info "Motion, so will wake up if the time is there"
		state.motionStopTime = null
        if (allOkWake) {
        	goToWake()
		}
	}
	else {
    	log.info "No Motion, so not ready to go to wake up"
        state.motionStopTime = now()
    }
}

def goToSleep() {
	log.trace "In goToSleep"
	if (state.motionStopTime) {
        if (allOkSleep) {
			def elapsed = now() - state.motionStopTime
	        if (elapsed >= ((sleepDelay ?: 0) * 60000L) - 2000) {
	        	startSleepMode()
			}
		}
        else {
        	runIn (300, goToSleep)
       	}
	}
}

def startSleepMode() {
    log.info "NOW Executing sleep handler."
    log.info "Now sending SLEEP notification"
    sendNotificationEvent("Home-mode set to '${sleepMode}'.")
    changeMode(sleepMode)
    if (sleepOn) {
        sleepOn.on()
    }
    if (sleepOff) {
        sleepOff.off()
    }
}

def goToWake() {
	log.trace "In goToWake"
    startWakeMode()
}

def startWakeMode() {
    log.info "NOW Executing wake handler."
    log.info "Now sending WAKE notification"
    sendNotificationEvent("Home-mode set to '${wakeUpMode}'.")
    changeMode(wakeUpMode)
    if (wakeUpOn) {
        wakeUpOn.on()
    }
    if (wakeUpOff) {
        wakeUpOff.off()
    }
}

/******************
 * Helper methods *
 ******************/

def changeMode(newMode) {
	log.info "Now changing to mode $newMode"
    if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
		}
		else {
		}
	}
}

private getAllOkSleep() {
	modeOkSleep && timeOkSleep
}

private getModeOkSleep() {
	def result = !sleepModeCheck || sleepModeCheck.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getTimeOkSleep() {
	def result = true
	if (sleepStarting && sleepEnding) {
		def currTime = now()
		def startSleep = timeToday(sleepStarting).time
		def stopSleep = timeToday(sleepEnding).time
		result = startSleep < stopSleep ? currTime >= startSleep && currTime <= stopSleep : currTime <= stopSleep || currTime >= startSleep
	}
	log.trace "timeOk = $result"
	result
}

private getAllOkWake() {
	modeOkWake && timeOkWake
}

private getModeOkWake() {
	def result = !wakeUpModeCheck || wakeUpModeCheck.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getTimeOkWake() {
	def result = true
	if (wakeUpStarting && wakeUpEnding) {
		def currTime = now()
		def startWake = timeToday(wakeUpStarting).time
		def stopWake = timeToday(wakeUpEnding).time
		result = startWake < stopWake ? currTime >= startWake && currTime <= stopWake : currTime <= stopWake || currTime >= startWake
	}
	log.trace "timeOk = $result"
	result
}
