/**
 *
 *  Door Light Knock
 *
 *  Author: ONarvaez
 *
 *  Turns on and optionally blinks a light when vibration is detected.
 *
 *  Date: 2020-02-20
 */

definition(
	name: "Door Light Knock",
	namespace: "onarvaez3",
	author: "ONarvaez",
	description: "Turns on and optionally blinks a light when vibration is detected.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections%402x.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner%402x.png"
)

preferences {
	section("When this sensor detects vibration..."){
		input "sensor1", "capability.accelerationSensor"
	}
	section("Turn on these lights...") {
		input "switches", "capability.switch", required: true, multiple: true, title: "Which lights?"
		input "lightMode", "enum", options: ["Only Turn On Lights", "Flash and Turn On Lights"], required: true, defaultValue: "Only Turn On Lights", title: "Action?"
	}
	section("And turn off lights after (in minutes, optional)"){
		input "turnOffAfter", "decimal", title: "Turn Off After", required: false, defaultValue: 10
	}
	section("Notify via SMS?") {
		input "phoneOne", "phone", required: false, multiple: true, title: "To which phone number?"
		input "phoneTwo", "phone", required: false, multiple: true, title: "To which other phone number?"
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "acceleration.active", accelerationActiveHandler)
	subscribe(sensor1, "acceleration.inactive", accelerationInactiveHandler)
	state.isEventOngoing = false
}

def accelerationActiveHandler(evt) {
	if(location.currentMode == "Away" || location.currentMode == "Night") {
		log.trace "Vibration detected"
		if (!state.isEventOngoing || (now() - state.startedAt) > 60000) {
			log.info "New event"
			state.isEventOngoing = true
			state.startedAt = now()

			if(location.currentMode == "Away") {
				sendNotification()
			}

	        turnOnLights()
		}

		state.stoppedAt = null
	} else {
		log.trace "Vibration detected but incorrect Mode"
	}
}

def accelerationInactiveHandler(evt) {
	log.trace "no vibration, isEventOngoing: $state.isEventOngoing"
}

def sendNotification() {
	def msg = "Vibration detected on ${sensor1.displayName}"
    log.info msg

    sendPush msg
	if(phoneOne) {
		sendSms(phoneOne, msg)
	}

	if(phoneTwo) {
		sendSms(phoneTwo, msg)
	}
}

def turnOnLights() {
    if (switches) {
		def delay = 0
		def lightsOriginalState = switches.collect{it.currentSwitch != "on"}
		def lightsOriginalLevel = switches.collect{it.currentLevel}
		
        log.trace "flash mode is ${lightMode}"
        if(lightMode?.equals("Flash and Turn On Lights") || lightMode?.equals("1")) {
            log.trace "entering flashing mode"
            delay = flashLights()
        }
        log.trace "turning on"

		switches.each {s ->
			s.on(delay: delay)
		}

		log.trace "lights will turn off in ${turnOffAfter} min"
		runIn(turnOffAfter * 60, turnOffLights, [data: [originalState: lightsOriginalState, originalLevel: lightsOriginalLevel]])
    }
}

def finishEvent() {
	state.isEventOngoing = false
	log.trace "finishing event"
}

def turnOffLights(data) {
	switches.eachWithIndex {s, i ->
		if(data.originalState[i]) {
			s.off()
		} else {
			s.on()

			if(s.hasCommand("setLevel")) {
				if(location.currentMode == "Away")
				{
					s.setLevel(100)
				} else {
					s.setLevel(data.originalLevel[i])
				}
			}			
		}
	}

	finishEvent()
}

def flashLights() {
	def doFlash = true
	def onFor = onFor ?: 1000
	def offFor = offFor ?: 1000
	def numFlashes = numFlashes ?: 5

	if (state.lastFlashed) {
		def elapsed = now() - state.lastFlashed
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
	}

	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastFlashed = now()
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 1L
		numFlashes.times {
			log.trace "Flash!"
			switches.eachWithIndex {s, i ->
				log.trace "current level value: ${s.currentLevel}"

				if(s.hasCommand("setLevel"))
				{
					s.setLevel(100)
				}
                
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}

		return delay
	}
}
