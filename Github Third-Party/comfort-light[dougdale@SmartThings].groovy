/**
 *  Comfort Light
 *
 *  Author: Doug Dale
 *
 *  Turns a light on when dark and leaves it on until a specific time. Based on "Smart(er) Nightlight" with is 
 *  in turn based largely on the SmartThings "Smart Nightlight" code.
 *
 */
definition(
    name: "Comfort Light",
    namespace: "dougdale",
    author: "Doug Dale",
    description: "Like Smart Nightlight [and Smart(er) Nightlight] but leaves the light on for an extended time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true
	}
	section("Turning on when it's dark and there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?"
	}
    section("Evening off time..."){
    	input "eveningOffTime", "time"
    }
    section("Morning on time..."){
    	input "morningOnTime", "time"
    }
	section("Overnight off delay when there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using either on this light sensor (optional) or the local sunrise and sunset"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
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
	subscribe(motionSensor, "motion", motionHandler)
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
	else {
		subscribe(location, "position", locationPositionChange)
//		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
//		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		// Rather than use the sunriseTime and sunsetTime events (which don't seem to work
        // consistently), update the sunrise and sunset times at 3 am.
		schedule("0 0 3 ? * *", sunriseSunsetTimeHandler)
		astroCheck()
	}
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

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		if (state.lastStatus != "on" && isDark()) {
			log.debug "turning on lights due to motion"
			lights.on()
			state.lastStatus = "on"
            
            // Schedule to turn off at a specific time if in the 'on' period in the morning or
            // evening.
	        if (isDuringMorningOnTime()) {
    	    	runOnce(state.setTime, onTimeEnd)
        	}
	        else if (isDuringEveningOnTime()) {
    	    	runOnce(timeToday(eveningOffTime).time, onTimeEnd)
        	}
		}
		state.motionStopTime = null        
	}
	else {
        // Ignore motion sensor off events during the 'on' time for the light. The light will be turned off
        // when the off time event fires.
    	if (!isDuringOnTime()) {
			state.motionStopTime = now()
			if(delayMinutes) {
				runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
			} else {
				turnOffMotionAfterDelay()
			}
        }
	}
}

// Force the light off at the end of the 'on' period.
def onTimeEnd() {
	lights.off()
    state.lastStatus = "off"
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
	if (lastStatus != "off" && evt.integerValue > 50) {
		lights.off()
		state.lastStatus = "off"
	}
	else if (state.motionStopTime) {
		if (lastStatus != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
				lights.off()
				state.lastStatus = "off"
			}
		}
	}
	else if (lastStatus != "on" && evt.value < 30){
		lights.on()
		state.lastStatus = "on"
	}
}

def turnOffMotionAfterDelay() {
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        log.trace "elapsed = $elapsed"
		if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
        	log.debug "Turning off lights"
			lights.off()
			state.lastStatus = "off"
		}
	}
}

def scheduleCheck() {
	log.debug "In scheduleCheck - skipping"
	//turnOffMotionAfterDelay()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}

private isDark() {
	def result
	if (lightSensor) {
		result = lightSensor.currentIlluminance < 30
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
}

//
// Returns true if the time is between the sunset (plus offset) and the evening off time or if the time is 
// between the morning on time and sunrise (plus offset).
//
private isDuringMorningOnTime() {
	def t = now()
    def morningOnTimeToday = timeToday(morningOnTime).time
    
    return (t > morningOnTimeToday && t < state.riseTime)
}

private isDuringEveningOnTime() {
	def t = now()
    def eveningOffTimeToday = timeToday(eveningOffTime).time
    
    return (t > state.setTime && t < eveningOffTimeToday)
}

private isDuringOnTime() {
    return isDuringMorningOnTime() || isDuringEveningOnTime()
}

private getSunriseOffset() {
    log.debug "In getSunriseOffset"
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	log.debug "In getSunsetOffset"
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}