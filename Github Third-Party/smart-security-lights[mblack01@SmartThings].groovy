/**
 *  Smart Security Light
 *
 *  Author: SmartThings
 *
 *
 * Adapted from SmartThings' Smart NightLight by Barry Burke
 *
 * Changes:
 *		2014/09/23		Added support for physical override:
 *						* If lights turned on manually, don't turn them off if motion stops
 *						  but DO turn them off at sunrise (in case the are forgotten)
 *						* Double-tap ON will stop the motion-initiated timed Off event
 *						* Double-tap OFF will keep the lights off until it gets light, someone manually
 *						  turns on or off the light, or another app turns on the lights.
 * 						* TO RE-ENABLE MOTION CONTROL: Manually turn OFF the lights (single tap)
 *		2014/09/24		Re-enabled multi-motion detector support. Still only a single switch (for now).
 *						Added option to flash the light to confirm double-tap overrides
 *						* Fixed the flasher resetting the overrides
 *		2014/09/25		More work fixing up overrides. New operation mode:
 *						* Manual ON any time overrides motion until next OFF (manual or programmatic)
 *						* Manual OFF resets to motion-controlled
 *						* Double-tap manual OFF turns lights off until next reset (ON or OFF) or tomorrow morning
 *						  (light or sunrise-driven)
 *		2014/09/26		Code clean up around overrides. Single ON tap always disables motion; OFF tap re-enables
 *						motion. Double-OFF stops motion until tomorrow (light/sunrise)
 *
 *
 */
definition(
	name: 		"Smart Security Lights",
	namespace: 	"smartthings",
	author: 	"SmartThings & Barry Burke",
	description: "Turns on lights when it's dark and motion is detected.  Turns lights off when it becomes light or some time after motion ceases. Optionally allows for manual override.",
	category: 	"Convenience",
	iconUrl: 	"https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
	iconX2Url: 	"https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true, required: true
	}
	section("Turning on when it's dark and there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true, required: true
	}
	section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using either on this light sensor (optional) or the local sunrise and sunset"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
        input "luxLevel", "number", title: "Darkness Lux level?", defaultValue: 50, required: true
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
	section ("Overrides") {
    	paragraph "Manual ON disables motion control. Manual OFF re-enables motion control."
		input "physicalOverride", "bool", title: "Physical override?", required: true, defaultValue: false
		paragraph "Double-tap OFF to lock light off until next ON or sunrise. Single-tap OFF to re-enable to motion-controlled."
		input "doubleTapOff", "bool", title: "Double-Tap OFF override?", required: true, defaultValue: true
        paragraph ""
        input "flashConfirm", "bool", title: "Flash lights to confirm overrides?", required: true, defaultValue: false
	}
}

def installed() {
	log.debug "Installed with settings: $settings"
	initialize()
}

def updated() {
	log.debug "Updated with settings: $settings"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	
	// Let's ignore the current state, and figure it out as we go...
	state.physical = false
	state.lastStatus = "off"
    state.keepOff = false
    state.flashing = false
    lights.off()
	
	subscribe(motionSensor, "motion", motionHandler)
	
	if (physicalOverride) {
		subscribe(lights, "switch.on", lightsOnHandler)
		subscribe(lights, "switch.off", lightsOffHandler)
	}
	if (doubleTapOn || doubleTapOff) {
		subscribe(lights, "switch", switchHandler, [filterEvents: false])
	}

	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
	else {
		astroCheck()
		def sec = Math.round(Math.floor(Math.random() * 60))
		def min = Math.round(Math.floor(Math.random() * 60))
		def cron = "$sec $min * * * ?"
		schedule(cron, astroCheck) // check every hour since location can change without event?
	}
}

def lightsOnHandler(evt) {				// if ANYTHING (besides me) turns ON the light, then exit "keepOff" mode
    if ( state.flashing ) { return }
    
    state.keepOff = false
}

def lightsOffHandler(evt) {				// if anything turns OFF the light, then reset to motion-controlled
    if ( state.flashing ) { return }
    
	state.physical = false
    state.lastStatus = "off"
}
 
def switchHandler(evt) {
    if ( state.flashing ) { return }
    
	log.debug "switchHandler: $evt.name: $evt.value"

	if (evt.isPhysical()) {
		if (evt.value == "on") {

        	if (physicalOverride) {
                log.debug "Override ON, disabling motion-control"
                state.flashing = true
                lights.on()						// Turn ALL of them on
                state.flashing = false
            	state.keepOff = false
        		if (delayMinutes) { unschedule ("turnOffMotionAfterDelay") }
            	if (flashConfirm) { flashTheLight() }
            	state.physical = true							// have to set this AFTER we flash the lights :)
			}
		} 
		else if (evt.value == "off") {
			if (physicalOverride) {
				state.flashing = true
				lights.off()
				state.flashing = false
				state.physical = false									// Somebody physically turned off the light
			}
	        state.keepOff = false									// Single off resets keepOff & physical overrides

			// use Event rather than DeviceState because we may be changing DeviceState to only store changed values
			def recentStates = lights.eventsSince(new Date(now() - 4000), [all:true, max: 10]).findAll{it.name == "switch"}
			log.debug "${recentStates?.size()} states found, last at ${recentStates ? recentStates[0].dateCreated : ''}"

			if (lastTwoStatesWere("off", recentStates, evt)) {
				log.debug "detected two OFF taps, override motion w/lights OFF"
                
				if (doubleTapOff) { 								// Double tap enables the keepOff
					if (delayMinutes) { unschedule("turnOffMotionAfterDelay") }
					if (flashConfirm) { flashTheLight() }
                    state.keepOff = true							// Have to set this AFTER we flash the lights :)
                }
            }
		}
	}
}

private lastTwoStatesWere(value, states, evt) {
	def result = false
	if (states) {
		log.trace "unfiltered: [${states.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
		def onOff = states.findAll { it.isPhysical() || !it.type }
		log.trace "filtered:   [${onOff.collect{it.dateCreated + ':' + it.value}.join(', ')}]"

		// This test was needed before the change to use Event rather than DeviceState. It should never pass now.
		if (onOff[0].date.before(evt.date)) {
			log.warn "Last state does not reflect current event, evt.date: ${evt.dateCreated}, state.date: ${onOff[0].dateCreated}"
			result = evt.value == value && onOff[0].value == value
		}
		else {
			result = onOff.size() > 1 && onOff[0].value == value && onOff[1].value == value
		}
	}
	result
}

def motionHandler(evt) {
	log.debug "motionHandler: $evt.name: $evt.value"

	if (state.physical) { return }	// ignore motion if lights were most recently turned on manually

	if (evt.value == "active") {
		if (enabled()) {
			log.debug "turning on light due to motion"
			lights.on()
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {
    	if (state.keepOff) {
        	if (delayMinutes) { unschedule("turnOffMotionAfterDelay") }
        	return 
        }
        
		state.motionStopTime = now()
		if(delayMinutes) {
			unschedule("turnOffMotionAfterDelay")				// This should replace any existing off schedule
			runIn(delayMinutes*60, "turnOffMotionAfterDelay", [overwrite: false])
		} else {
			turnOffMotionAfterDelay()
		}
	}
}

def illuminanceHandler(evt) {
    if ( state.flashing ) { return }
    
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"

	def lastStatus = state.lastStatus					// its getting light now, we can turn off
    
    if (state.keepOff && evt.integerValue >= luxLevel) { state.keepOff = false } // reset keepOff
    
	if ((lastStatus != "off") && (evt.integerValue >= luxLevel)) {	// whether or not it was manually turned on
		lights.off()
		state.lastStatus = "off"
		state.physical = false
        state.keepOff = false							// it's a new day
	}
	else if (state.motionStopTime) {
		if (state.physical) { return }					// light was manually turned on

		if (lastStatus != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= (delayMinutes ?: 0) * 60000L) {
				lights.off()
				state.lastStatus = "off"
                state.keepOff = false
			}
		}
	}
	else if (lastStatus != "on" && evt.integerValue < luxLevel) {
        if ( state.keepOff || state.physical ) { return }					// or we locked it off for the night
		lights.on()
		state.lastStatus = "on"
	}
}

def turnOffMotionAfterDelay() {
	log.debug "In turnOffMotionAfterDelay"

    if (state.keepOff) {
        if (delayMinutes) { unschedule("turnOffMotionAfterDelay") }
        return 
    }						// light was manually turned on
    													// Don't turn it off

	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
		if (elapsed >= (delayMinutes ?: 0) * 60000L) {
			lights.off()
			state.lastStatus = "off"
		}
	}
}

//def scheduleCheck() {
//	log.debug "In scheduleCheck - skipping"
	//turnOffMotionAfterDelay()
//}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}

private flashTheLight() {
    def doFlash = true
    def onFor = onFor ?: 200
    def offFor = offFor ?: 200
    def numFlashes = numFlashes ?: 2
    
    state.flashing = true

    if (state.lastActivated) {
        def elapsed = now() - state.lastActivated
        def sequenceTime = (numFlashes + 1) * (onFor + offFor)
        doFlash = elapsed > sequenceTime
    }

    if (doFlash) {
        state.lastActivated = now()
        def initialActionOn = light.currentSwitch != "on"
        def delay = 1L
        numFlashes.times {
            if (initialActionOn) {
                light.on(delay: delay)
            }
            else {
                light.off(delay:delay)
            }
            delay += onFor
            if (initialActionOn) {
                light.off(delay: delay)
            }
            else {
                light.on(delay:delay)
            }
            delay += offFor
        }
    }
    state.flashing = false
}

private enabled() {
	def result
    
    if (state.keepOff || state.flashing) {
    	result = false								// if OFF was double-tapped, don't turn on
    }
    else if (lightSensor) {
		result = (lightSensor.currentIlluminance as Integer) < luxLevel
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}