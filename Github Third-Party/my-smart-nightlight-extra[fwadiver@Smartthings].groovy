/**
 *  Smart Nightlight
 *
 *  Author: Chrisb
 *
 */
definition(
    name: "My Smart Nightlight Extra",
    namespace: "sirhcb",
    author: "chrisb",
    description: "Turns on lights when it's dark and motion is detected.  Turns lights off when it becomes light or some time after motion ceases.  Stay on or change off time when the switch is turned on.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: false
	}
	section("Turning on when it's dark and there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?", required: false
	}
    section("Or, turn on when one of these contacts opened"){
		input "contacts", "capability.contactSensor", multiple: false, title: "Select Contacts", required: false
	}
	section("And then off when it's light or there's been no movement for or the contact is closed for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using either on this light sensor (optional) or the local sunrise and sunset"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
        input "darkvalue", "number", title: "Luninance Value to use?"
	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
		input "zipCode", "text", required: false
	}
    section ("How much delay when switch is turned on while it is already on? (optional, if nothing is entered, won't turn off.)") {
    	input "bigDelayMinutes", "number", title: "Minutes?", required: false
    }
    section ("Specify the level of tracing to be done.  Defaults to none") {
    	input(name: "trclevel", type: "enum", title: "Trace Level", options: ["debug","trace","none"])
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
	if (motionSensor) {
    	subscribe(motionSensor, "motion", motionHandler)
        state.trigger = 1
    }
    if (contacts) {
    	subscribe(contacts, "contact", contactHandler)
        state.trigger = 2
    }
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
		state.illuminance = lightSensor.currentIlluminance
	}
    state.ontime = 0
	astroCheck()
    runIn(5, astroCheck, [overwrite: false])
	schedule("0 1 * * * ?", astroCheck) 									// check every hour since location can change without event
    subscribe(lights, "switch.on", delayChange, [filterEvents: false])
    subscribe(lights, "switch.off", turnedOff)
    state.pushed = ""
    state.offpushed = null
    if (trclevel != "debug" && trcleve != "trace" ){
    	trclevel = "none"
    }
}

def turnedOff(evt) {
	DEBUG("turnedOff: $evt.name: $evt.value SwitchOnStatus: $state.pushed")
	state.myState = "Light Turned Off"
    if (state.trigger > 1) {
    	state.trigger = 2
    }
	if (state.pushed == "pushed") {
		state.pushed = "delay"
		lights.indicatorWhenOff()
	} else {
		if (evt.isPhysical()) {
			lights.indicatorWhenOn()
			state.myState = "Light Turned Off at Switch"
			if (daytime()) {
       		 	state.ontime = state.setTime
				state.offpushed = "day"
			} else {
				state.offpushed = "night"
                if (now() < state.riseTime) {
                	state.ontime = state.riseTime
                } else {
                	state.ontime = state.riseTime + 86400000
                }
			}
		}
		else {
			lights.indicatorWhenOff()
		}
	}
	state.lastStatus = "off"
	TRACE(state.myState)
    DEBUG("turnedOff: $state.offpushed OnTime: $state.ontime")
}

def contactHandler(evt) {
	DEBUG("contactHandler: $evt.name: $evt.value")
    if (evt.value == "open") {
        if (enabled()) {
            DEBUG("Turning on lights by contact opening")
            lights.on()
            state.lastStatus = "on"
            if (state.trigger == 2) {
            	state.trigger = 3
            }
        }
        state.motionStopTime = null
        state.myState = "Leave Light On"
    } else if (evt.value == "closed") {
        if (daytime()) {
            state.motionStopTime = now()
        } else   { 
            state.motionStopTime = null
            state.myState = "Leave Light On"
        }
        if (state.pushed == "pushed") {														// The on button was pushed so...
        	if(bigDelayMinutes) {																// If the user set long delay then...
				scheduleLightOut(bigDelayMinutes*60)											// Schedule the "lights off" for later.
				state.myState = "Schedule Light Out"
			} else {																			// Otherwise...
				state.myState = "Leave Light On"												// Make sure lights don't go out.
				unschedule(turnOffMotionAfterDelay)
			}
        } else {																			// The on button was NOT pushed so...
        	if (daytime()) {																	// If daylight 
            	if(delayMinutes) {																	// If the user set a delay then...
					scheduleLightOut(delayMinutes*60)												// Schedule the "lights off" for later.
				} else {																			// Otherwise...
					scheduleLightOut(0)																// Run the lights off now.
				}
				state.myState = "Schedule Light Out"
            } else {
            	state.myState = "Leave Light On"
            }
        }
    }
    TRACE("$evt.name: $evt.value - $state.myState")
    DEBUG("motionHandler: Light Action - $state.myState")
}

def motionHandler(evt) {
	def lightsw = lights.currentSwitch
	DEBUG("motionHandler: $evt.name: $evt.value Light Is: $state.lastStatus : $lightsw")
	if (evt.value == "active") {
		if (lightsw == "off") {
        	state.myState = "Leave Light Off"
        }
		if (enabled()) {
        	if (lightsw == "off") {
            	state.myState = "Turning on lights due to motion"
				lights.on()
            }
            else {
            	state.myState = "Leave Light On"
            }
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {																					// Motion has stoped
		state.motionStopTime = now()
		if (state.pushed == "pushed") {														// The on button was pushed so...
        	if(bigDelayMinutes) {																// If the user set long delay then...
				scheduleLightOut(bigDelayMinutes*60)											// Schedule the "lights off" for later.
				state.myState = "Schedule Light Off"
			} else {																			// Otherwise...
				state.myState = "Unschedule turnOffMotionAfterDelay"							// Make sure lights don't go out.
				unschedule(turnOffMotionAfterDelay)
			}
        } else {																				// The on button was NOT pushed so...
        	if (lightsw == "on") {
            	if (enabled()) {
            		if (delayMinutes) {															// If the user set a delay then...
						scheduleLightOut(delayMinutes*60)										// Schedule the "lights off" for later.
					} else {																	// Otherwise...
						scheduleLightOut(0)														// Run the lights off now.
					}
                    state.myState = "Schedule Light Off"
            	} else {
            		lights.off()
                	state.lastStatus = 'off'
                	state.myState = "Lights turned off due to luminance"
                }
            }
        }
	}
	DEBUG("motionHandler: Light Action - $state.myState")
	TRACE("$evt.name: $evt.value - $state.myState")
}

def illuminanceHandler(evt) {
	def lightsw = lights.currentSwitch
	DEBUG("illuminanceHandler-start: $evt.name: $evt.value, light is: $lightsw, motionStopTime: $state.motionStopTime myState: $state.myState")
	state.illuminance = evt.integerValue
	if (lightsw != "on" && evt.integerValue > darkvalue && state.pushed != "pushed") {
		lights.off()
		state.lastStatus = "off"
	}
	else if (state.motionStopTime) {
		if (lightsw != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= (delayMinutes ?: 0) * 60000L) {
				lights.off()
				state.lastStatus = "off"
			}
		}
	}
	else if (lightsw != "on" && evt.integerValue < darkvalue && state.offpushed == null && state.trigger != 2){
		lights.on()
		state.lastStatus = "on"
	}
    DEBUG("illuminanceHandler-end: $evt.name: $evt.value, light is: $state.lastStatus, motionStopTime: $state.motionStopTime myState: $state.myState")
}

def turnOffMotionAfterDelay() {
	def lightsw = lights.currentSwitch
	DEBUG("turnOffMotionAfterDelay-start: Light Status: $lightsw, SwitchONStatus: $state.pushed StopTime: $state.motionStopTime")
	if (state.motionStopTime && lightsw != "off" && state.pushed != "pushed") {
		def elapsed = now() - state.motionStopTime
		def delayvalue = (delayMinutes ?: 0) * 60000L
		if (elapsed >= delayvalue) {
			lights.off()
			state.lastStatus = "off"
		}
		else {
			scheduleLightOut(60)
		}
		DEBUG("turnOffMotionAfterDelay-end: Light Status: $state.lastStatus, Elapsed Time: $elapsed, DelayValue: $delayvalue")
	}
    else {
    	DEBUG("turnOffMotionAfterDelay-end: Light Status: $lightsw, SwitchONStatus: $state.pushed")
    }
}

def scheduleCheck() {
	DEBUG("In scheduleCheck - skipping")
	//turnOffMotionAfterDelay()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
    def msg = "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
    DEBUG(msg)
	// DEBUG("rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime), nextRise: ${new Date(state.nextrise)}($state.nextrise)")
}

def delayChange(evt) {
	DEBUG("delayChange lastStatus: $state.lastStatus")
    DEBUG("event from digital actuation? ${evt.isDigital()}")
    DEBUG("event from physical actuation? ${evt.isPhysical()}")
    DEBUG("Is this event a state change? ${evt.isStateChange()}")
    DEBUG("The source of this event is: ${evt.source}")
    DEBUG("The value of this event as a string: ${evt.value}")
    DEBUG("the name of this event: ${evt.name}")
    DEBUG("event id: ${evt.id}")
    DEBUG("event raw description: ${evt.description}")
    DEBUG("event description text: ${evt.descriptionText}")
	state.offpushed = null
 	if (evt.isPhysical() && evt.isStateChange()) {
    	state.pushed = "pushed"
	} else if((delayMinutes) && state.trigger != 3) {
		scheduleLightOut(delayMinutes*60)
	}
	if (state.pushed == "pushed") {
		state.myState = "Light turned on at switch"
		lights.indicatorWhenOn()
	} else {
		state.myState = "Light turned on"
		lights.indicatorWhenOff()
	}
	state.lastStatus = "on"
	TRACE(state.myState)
}

private daytime() {
    def answer
    def t =now()
    answer = t > state.riseTime && t < state.setTime
    DEBUG("daytime - Is it Day: $answer  Rise: $state.riseTime  Set: $state.setTime")
    answer
}

private enabled() {
	def result
	def t = now()
    def diff = 0
    DEBUG("Enabled - Onpushed: $state.pushed Offpushed: $state.offpushed t: $t Rise: $state.riseTime Set: $state.setTime OnTime: $state.ontime")
	if (state.offpushed == "day" || state.offpushed == "night") {
		diff = state.ontime - now()
        DEBUG("Enabled - Diff: $diff")
    }
    if (diff < 1) {
    	state.ontime = 0
        state.offpushed = null
        if (lightSensor) {
        	if (state.illuminance == null) {
        		result = 0
        	}
        	else {
				result = lightSensor.currentIlluminance < darkvalue
        		DEBUG("Light Level: $state.illuminance : $lightSensor.currentIlluminance DarknessValue: $darkvalue")
        	}
		}
		else {
			result = t < state.riseTime || t > state.setTime
		}
	} else {
    	result = 0
    }
    if (state.offpushed == "delay") {
    	state.offpushed = ""
    	result = 0
    }
    DEBUG("Enabled - result: $result Offpushed: $state.offpushed OnTime: $state.ontime")
	result
}

private scheduleLightOut(delay) {
    DEBUG("scheduleLightOut: turnOffMotionAfterDelay scheduled Delay = $delay")
	if (delay == 0) {
		turnOffMotionAfterDelay()
		}
	else if (canSchedule()){
		runIn(delay, turnOffMotionAfterDelay, [overwrite: true])
	} else {
		DEBUG("schduleLightOut: At max scheduled jobs")
	}
}

private def TRACE(message) {
    if (trclevel == "trace" || trclevel == "debug"){
    	log.trace message
    }
}

private def DEBUG(message) {
	if (trclevel == "debug"){
		log.debug message
	}
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}