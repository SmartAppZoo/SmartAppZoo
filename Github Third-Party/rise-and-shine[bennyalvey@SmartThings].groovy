/**
 *  Rise and Shine
 *
 *  Author: SmartThings
 *  Date: 2013-03-07
 */

preferences {
	section("When there's motion on any of these sensors") {
		input "motionSensors", "capability.motionSensor", multiple: true
	}
	section("During this time window (default End Time is 4:00 PM)") {
		input "timeOfDay", "time", title: "Start Time?"
		input "endTime", "time", title: "End Time?", required: false
	}
	section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
	section("And (optionally) turn on these appliances") {
		input "switches", "capability.switch", multiple: true, required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "phoneNumber", "phone", title: "Send a Text Message?", required: false
	}
}

def installed() {
	log.debug "installed, current mode = ${location.mode}, state.actionTakenOn = ${state.actionTakenOn}"
	initialize()
}

def updated() {
	log.debug "updated, current mode = ${location.mode}, state.actionTakenOn = ${state.actionTakenOn}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.trace "timeOfDay: $timeOfDay, endTime: $endTime"
	subscribe(motionSensors, "motion.active", motionActiveHandler)
	subscribe(location, modeChangeHandler)
	if (state.modeStartTime == null) {
		state.modeStartTime = 0
	}
}

def modeChangeHandler(evt) {
	state.modeStartTime = now()
}

def motionActiveHandler(evt)
{
	// for backward compatibility
	if (state.modeStartTime == null) {
		subscribe(location, modeChangeHandler)
		state.modeStartTime = 0
	}

	def t0 = now()
	def modeStartTime = new Date(state.modeStartTime)
	def timeZone = location.timeZone ?: timeZone(timeOfDay)
	def startTime = timeTodayAfter(modeStartTime, timeOfDay, timeZone)
	def endTime = timeTodayAfter(startTime, endTime ?: "16:00", timeZone)
	log.debug "startTime: $startTime, endTime: $endTime, t0: ${new Date(t0)}, modeStartTime: ${modeStartTime},  actionTakenOn: $state.actionTakenOn, currentMode: $location.mode, newMode: $newMode "

	if (t0 >= startTime.time && t0 <= endTime.time && location.mode != newMode) {
		def message = "Good morning! SmartThings changed the mode to '$newMode'"
		send(message)
		setLocationMode(newMode)
		log.debug message

		def dateString = new Date().format("yyyy-MM-dd")
		log.debug "last turned on switches on ${state.actionTakenOn}, today is ${dateString}"
		if (state.actionTakenOn != dateString) {
			log.debug "turning on switches"
			state.actionTakenOn = dateString
			switches?.on()
		}

	}
	else {
		log.debug "not in time window, or mode is already set, currentMode = ${location.mode}, newMode = $newMode"
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
