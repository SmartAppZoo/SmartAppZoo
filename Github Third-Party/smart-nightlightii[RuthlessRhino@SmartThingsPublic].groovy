/**
 *  Smart Nightlight
 *
 *  Author: SmartThings
 		update 2015-02-06 mike maxwell
        	added selectable lux settings to UX
 *
 */
definition(
    name: "Smart NightlightII",
    namespace: "MikeMaxwell",
    author: "SmartThings",
    description: "Turns on lights when it's dark and motion is detected. Turns lights off when it becomes light or some time after motion ceases.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true
	}
	section("Turn on when it's dark and there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using this light sensor"){
		input "lightSensor", "capability.illuminanceMeasurement", required: true
	}
    section("LUX dark threshold for light sensor, defaults to 30") {
    	input "luxCutoff","enum", title: "LUX?", required: false, options: ["5","10","15","20","25","30","40","50","100","200","500","1000", "1500","2000"]
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
	subscribe(lightSensor, "illuminance", illuminanceHandler)
}


def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		if (enabled()) {
			log.debug "turning on lights due to motion"
			lights.on()
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {
		state.motionStopTime = now()
		if(delayMinutes) {
			runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
		} else {
			turnOffMotionAfterDelay()
		}
	}
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
	if (lastStatus != "off" && evt.integerValue > (this."${luxCutoff}" ?: "30").toInteger() * 1.2) {
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
	else if (lastStatus != "on" && evt.value < (this."${luxCutoff}" ?: "30").toInteger()){
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

private enabled() {
	
	def result
	result = lightSensor.currentIlluminance < (this."${luxCutoff}" ?: "30").toInteger()
	log.debug "enabled- ${result}"
    return result
}

