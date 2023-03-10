/**
 *  Sunrise, Sunset
 *
 *  Author: SmartThings
 *
 *  Date: 2013-04-30
 */
preferences {
	section ("At sunrise...") {
		input "sunriseMode", "mode", title: "Change mode to?", required: false
		input "sunriseOn", "capability.switch", title: "Turn on?", required: false, multiple: true
		input "sunriseOff", "capability.switch", title: "Turn off?", required: false, multiple: true
	}
	section ("At sunset...") {
		input "sunsetMode", "mode", title: "Change mode to?", required: false
		input "sunsetOn", "capability.switch", title: "Turn on?", required: false, multiple: true
		input "sunsetOff", "capability.switch", title: "Turn off?", required: false, multiple: true
	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "phoneNumber", "phone", title: "Send a text message?", required: false
	}

}

def installed() {
	initialize()
}

def updated() {
	unschedule()
	initialize()
}

def initialize() {
	scheduleAstroCheck()
	astroCheck()
}

def scheduleAstroCheck() {
	def min = Math.round(Math.floor(Math.random() * 60))
	def exp = "0 $min * * * ?"
    log.debug "$exp"
	schedule(exp, astroCheck) // check every hour since location can change without event?
    state.hasRandomSchedule = true
}

def astroCheck() {
	if (!state.hasRandomSchedule && state.riseTime) {
    	log.info "Rescheduling random astro check"
        unschedule("astroCheck")
    	scheduleAstroCheck()
    }
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
	if (state.riseTime != riseTime.time || state.setTime != setTime.time) {
		state.riseTime = riseTime.time
		state.setTime = setTime.time

		unschedule("sunriseHandler")
		unschedule("sunsetHandler")

		if (riseTime.after(now)) {
			log.info "scheduling sunrise handler for $riseTime"
			runOnce(riseTime, sunriseHandler)
		}

		if (setTime.after(now)) {
			log.info "scheduling sunset handler for $setTime"
			runOnce(setTime, sunsetHandler)
		}
	}
}

def sunriseHandler() {
	log.info "Executing sunrise handler"
	if (sunriseOn) {
		sunriseOn.on()
	}
	if (sunriseOff) {
		sunriseOff.off()
	}
	changeMode(sunriseMode)
	unschedule("sunriseHandler") // Temporary work-around for scheduling bug
}

def sunsetHandler() {
	log.info "Executing sunset handler"
	if (sunsetOn) {
		sunsetOn.on()
	}
	if (sunsetOff) {
		sunsetOff.off()
	}
	changeMode(sunsetMode)
	unschedule("sunsetHandler") // Temporary work-around for scheduling bug
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phoneNumber ) {
		log.debug( "sending text message" )
		sendSms( phoneNumber, msg )
	}

	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

