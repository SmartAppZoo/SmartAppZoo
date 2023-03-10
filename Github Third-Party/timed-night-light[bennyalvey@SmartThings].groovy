/**
 *  Timed Night Light
 *
 *  Author: zach@beamlee.com
 *  Date: 2013-06-17
 */
preferences {

	section("Sensor"){
		input "motionSensors", "capability.motionSensor", title: "Select your motion sensor", multiple: true


	}
	section("Choose the outlets of your light(s)"){
		input "lights", "capability.switch", title: "Light Outlet(s)", multiple: true
	}

	section("Choose the time window during which the lights will be activated."){
		input "startTime", "time", title: "Beginning Time"
		input "endTime", "time", title: "Ending Time"
		input "freq", "number", title: "Inactive period before lights are switched off (in minutes)"
	}

}


def installed()
{
	subscribe(motionSensors, "motion.active", motionActiveHandler)
	subscribe(motionSensors, "motion.inactive", motionInactiveHandler)
	subscribe(lights, "switch.off", switchOffHandler)


}

def updated()
{
	unsubscribe()
	subscribe(motionSensors, "motion.active", motionActiveHandler)
	subscribe(motionSensors, "motion.inactive", motionInactiveHandler)
	subscribe(lights, "switch.off", switchOffHandler)

}

def motionActiveHandler(evt) {

	if(now() > timeToday(startTime).time && now() < timeToday(endTime).time){
		lights.on()
		if (state.motionStoppedAt) {
			state.motionStoppedAt = null
		}
	}
	else if(now() < timeToday(startTime).time || now() > timeToday(endTime).time){
		log.debug "Not in time window.  Lights will not turn on."
		lights.off()
		if (state.motionStoppedAt) {
			state.motionStoppedAt = null
		}
	}


}

def motionInactiveHandler(evt) {
	if(now() > timeToday(startTime).time && now() < timeToday(endTime).time){
		if (allQuiet()) {

			state.motionStoppedAt = now()
			log.debug state.motionStoppedAt

			def difTime = now() - state.motionStoppedAt
			log.debug "difTime is $difTime"
			schedule("0 0/$freq * * * ?", checkElapsed)


		}
	}
}

def checkElapsed() {
	if (state.motionStoppedAt) {
		def elapsed = now() - state.motionStoppedAt
		log.debug "Elapsed is $elapsed"
		def threshold = 300
		log.debug threshold
		log.debug elapsed - threshold
		if (elapsed > threshold) {
			if(now() > timeToday(startTime).time && now() < timeToday(endTime).time){
				log.debug "Made it through TimeCheck"
				lights.off()
				unschedule("checkElapsed")
			}
		}
	}
}


private allQuiet(){
	def result = true
	for (sensor in motionSensors) {
		if (sensor.currentMotion == "active") {
			result = false
			break
		}
	}
	result
}
