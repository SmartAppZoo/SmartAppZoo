/**
 *  Simple Sleep and Wake
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
	name: "Simple Sleep and Wake",
	namespace: "erik4281",
	author: "Erik Vennink",
	description: "Automatically set mode to sleep or day/night in the evening and morning, based on motion sensors.",
	category: "Convenience",
	iconUrl: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png",
	iconX2Url: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png",
	iconX3Url: "http://icons.iconarchive.com/icons/arrioch/birdie-adium/128/Adium-Bird-Sleep-icon.png")

/**********
 * Setup  *
 **********/

preferences {
	section ("Monitor motion sensors") {
    	input "motionSensor", "capability.motionSensor", title: "If no motion here", required: true, multiple: true
	}
	section ("Go to sleep...") {
		input "sleepMode", "mode", title: "Change mode to?", required: true
		input "sleepDelay", "number", title: "After X minutes without motion.", required: false
		input "sleepAlarm", "enum", title: "Set Smart Home Monitor mode to?" , required: false, multiple:false, options: ["off","stay","away"]
		input "sleepOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sleepOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
		input "sleepStarting", "time", title: "Starting from", required: false
		input "sleepEnding", "time", title: "Ending at", required: false
		input "sleepModeCheck", "mode", title: "Only when mode is", multiple: true, required: false
	}
	section ("Wake up...") {
		input "wakeUpMode", "mode", title: "Change mode to?", required: true
		input "wakeAlarm", "enum", title: "Set Smart Home Monitor mode to?" , required: false, multiple:false, options: ["off","stay","away"]
		input "wakeUpOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "wakeUpOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
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
	subscribe(motionSensor, "motion", eventHandler)
}

/******************
 * Event handlers *
 ******************/

def eventHandler(evt) {
	log.trace "Sleep & Wake eventHandler"
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	if (motionValue) {
		log.info "Motion detected"
		state.motionStopTime = null
		if (allOkWake) {
			goToWake()
		}
	}
	else {
		log.info "Motion stopped"
		state.motionStopTime = now()
		if (sleepDelay) {
			runIn(sleepDelay*60, goToSleep)
			log.info "Delay (motion): $sleepDelay minutes"
		} 
		else {
			goToSleep()
		}
	}
}

def goToSleep() {
	log.trace "In goToSleep"
	if (state.motionStopTime) {
		if (allOkSleep) {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= ((sleepDelay ?: 0) * 60000L) - 2000) {
				sendNotificationEvent("Sleep: Alarm switched on.")
				log.debug "Changing to sleep mode"
				changeMode(sleepMode)
				if (sleepAlarm) {
					sendLocationEvent(name: "alarmSystemStatus", value: sleepAlarm)
				}
				if (sleepOn) {
					sleepOn.each {light ->
						light.on()
						pause(250)
						light.on()
						pause(250)
						light.on()
					}
				}
				if (sleepOff) {
					sleepOff.each {light ->
						light.off()
						pause(250)
						light.off()
						pause(250)
						light.off()
					}
				}
			}
		}
		else {
			runIn (300, goToSleep)
		}
	}
}

def goToWake() {
	log.trace "In goToWake"
	sendNotificationEvent("Wake: Alarm switched off.")
	log.debug "Changing to wake mode"
	changeMode(wakeUpMode)
	if (wakeAlarm) {
		sendLocationEvent(name: "alarmSystemStatus", value: wakeAlarm)
	}
	if (wakeUpOn) {
		wakeUpOn.each {light ->
			light.on()
			pause(250)
			light.on()
			pause(250)
			light.on()
		}
	}
	if (wakeUpOff) {
		wakeUpOff.each {light ->
			light.off()
			pause(250)
			light.off()
			pause(250)
			light.off()
		}
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
