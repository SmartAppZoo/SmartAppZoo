/**
 * testing out a lights if not already on with motion
 */
definition(
    name: "Modified Smart Nightlight",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turns on lights when it's dark and motion is detected. Turns lights off some time after motion ceases.",
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
	section("And then off when there's been no movement for..."){
		input "delaySeconds", "number", title: "Seconds?"
	}
	section("Using this light sensor..."){
		input "lightSensor", "capability.illuminanceMeasurement", required: true
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
	subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		state.motionStopTime = now()
		if(delaySeconds) {
			runIn(delaySeconds, turnOffMotionAfterDelay, [overwrite: false])
		} else {
			turnOffMotionAfterDelay()
		}
	}
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
	else if (lastStatus != "on" && evt.integerValue < 30){
		lights.setLevel(5)
		state.lastStatus = "on"
	}
}

def turnOffMotionAfterDelay() {
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        log.trace "elapsed = $elapsed"
		if (elapsed >= ((delaySeconds ?: 0) * 60000L) - 2000) {
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
	if (lightSensor) {
		result = lightSensor.currentIlluminance?.toInteger() < 3
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
}