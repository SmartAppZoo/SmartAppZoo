/**
 *  Copyright 2018 SmartThings
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
 *  Smart Outdoor Light
 *
 *  Author: Rodney Rowen based on SmartThings
 *
 */

import groovy.transform.Field
 
@Field final Map      LIGHT_STATE = [
    OFF:       "off",
    ON:        "on",
    DIM:       "dim"
]

definition(
    name: "My Light Manager",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Turns on lights when it's dark and motion is detected. Turns lights off when it becomes light or after a time out.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true
	}
	section("Set Normal Dim Level..."){
		input "dimLevel", "number", title: "Dim Level?"
	}
	section("Turning bright when it's dark and this happens..."){
		input "closeSensors", "capability.contactSensor", title: "Sensor Closes", multiple: true, required: false
		input "openSensors", "capability.contactSensor", title: "Sensor Opens", multiple: true, required: false
		input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
	}
	section("And then off when it's light or there's been no change for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using either on this light sensor (optional) or the local sunrise and sunset"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
		input "luxLevel", "number", title: "Lux level?", required: false
	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
		input "zipCode", "text", title: "Zip code", required: false
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
	state.lastStatus = LIGHT_STATE.OFF
	state.motionStopTime = null
    if (closeSensors) {
        subscribe(closeSensors, "contact.closed", closedHandler)
    }
    if (openSensors) {
    	subscribe(openSensors, "contact.open", openedHandler)
    }
    if (motionSensors) {
    	subscribe(motionSensors, "motion.inactive", inactiveHandler)
        subscribe(motionSensors, "motion.active", activeHandler)
    }
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
	else {
		subscribe(location, "position", locationPositionChange)
		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		astroCheck()
	}
}

def activeHandler(evt){
    log.trace "motion active fired via [${evt.displayName}] UTC: ${evt.date.format("yyyy-MM-dd HH:mm:ss")}"
    eventHandler()
}

def inactiveHandler(evt){
    log.debug "Got Motion inactive event"
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}

def closedHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "closed") {
        log.debug "Got Close Sensor event"
        eventHandler()
	}
}

def openedHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "open") {
        log.debug "Got Open Sensor event"
        eventHandler()
	}
}

def eventHandler() {
    if (isDark()) {
        log.debug "Starting Motion Timer Now"
        state.motionStopTime = now()
        if(delayMinutes) {
            runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: true])
        } else {
            turnOffMotionAfterDelay()
        }
        processState()
    } else {
        if (state.motionStopTime) {
            log.debug "Motion when light"
            state.motionStopTime = null
            processState()
        }
    }
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	processState()
}

def turnOffMotionAfterDelay() {
	log.debug "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime) {
		// Turn the timer off and do the state processing
		state.motionStopTime = null
		processState()
	}
}

def processState() {

	// Look for the various state transitions
	def darkOutside = isDark()

	switch (state.lastStatus) {
		case LIGHT_STATE.ON:
			if (!darkOutside) {
				setLightState(LIGHT_STATE.OFF)
				state.motionStopTime = null
			} else if (state.motionStopTime == null) {
				log.debug "Bright Timer expired - Back to normal"
				setLightState(LIGHT_STATE.DIM)
			} else {
				def elapsed = now() - state.motionStopTime
				log.trace "elapsed = $elapsed"
				if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
					log.debug "Bright Timer expired - Back to normal"
					setLightState(LIGHT_STATE.DIM)
					state.motionStopTime = null
				}
			}
			break
		case LIGHT_STATE.DIM:
			if (!darkOutside) {
				setLightState(LIGHT_STATE.OFF)
			} else if (state.motionStopTime) {
				log.debug "Bright Trigger Detected - Start Bright"
				setLightState(LIGHT_STATE.ON)
			}
			break
		case LIGHT_STATE.OFF:
		default:
			if (darkOutside) {
                if (state.motionStopTime == null) {
                    log.debug "Got Dark - Turn lights to Dim"
                    setLightState(LIGHT_STATE.DIM)
                } else {
                    log.debug "Bright Event Detected - Start Bright"
                    setLightState(LIGHT_STATE.ON)
                }
			}
			break
	}
}

private isDark() {
	def result
	if (lightSensor) {
		if (state.lastStatus != LIGHT_STATE.OFF) {
			result = lightSensor.currentIlluminance?.toInteger() < (luxLevel + 10)
		} else {
			result = lightSensor.currentIlluminance?.toInteger() < luxLevel
		}
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
}

def setLightState(newState) {
	if (newState != state.lastStatus) {
		if (newState == LIGHT_STATE.ON) {
			lights.setLevel(100)
		} else if (newState == LIGHT_STATE.DIM) {
            if (dimLevel > 0) {
            	lights.setLevel(dimLevel)
            } else {
				lights.off()
            }
		} else {
			lights.off()
		}
		state.lastStatus = newState
    }
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}


private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
