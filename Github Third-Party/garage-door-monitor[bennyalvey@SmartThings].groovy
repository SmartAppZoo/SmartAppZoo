/**
 *  Garage Door Monitor
 *
 *  Author: SmartThings
 */
preferences {
	section("When the garage door is open...") {
		input "multisensor", "capability.threeAxis", title: "Which?"
	}
	section("For too long...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
	section("Text me at (optional, sends a push notification if not specified)...") {
		input "phone", "phone", title: "Phone number?", required: false
	}
}

def installed()
{
	subscribe(multisensor, "acceleration", accelerationHandler, [filterEvents: false])
}

def updated()
{
	unsubscribe()
	subscribe(multisensor, "acceleration", accelerationHandler, [filterEvents: false])
}

/*
The "acceleration" message comes during acceleration, but also is reported every 2.5 minutes, so we listen for
that and then check if the garage door has been open for longer than the threshold.
*/
def accelerationHandler(evt) {
	def latestThreeAxisState = multisensor.threeAxisState // e.g.: 0,0,-1000

	if (latestThreeAxisState) {
		def latestThreeAxisDate = latestThreeAxisState.dateCreated.toSystemDate()
		def isOpen = Math.abs(latestThreeAxisState.xyzValue.z) > 250 // TODO: Test that this value works in most cases...

		if (isOpen) {
			def deltaMillis = 1000 * 60 * maxOpenTime
			def timeAgo = new Date(now() - deltaMillis)
			def openTooLong = latestThreeAxisDate < timeAgo
			log.debug "openTooLong: $openTooLong"
			def recentTexts = state.smsHistory.find { it.sentDate.toSystemDate() > timeAgo }
			log.debug "recentTexts: $recentTexts"

			if (openTooLong && !recentTexts) {
				def openMinutes = maxOpenTime * (state.smsHistory?.size() ?: 1)
				sendTextMessage(openMinutes)
			}
		}
		else {
			clearSmsHistory()
		}
	}
	else {
		log.warn "COULD NOT FIND LATEST 3-AXIS STATE FOR: ${multisensor}"
	}
}

def sendTextMessage(openMinutes) {
	log.debug "$multisensor was open too long, texting $phone"

	updateSmsHistory()
	def msg = "Your ${multisensor.label ?: multisensor.name} has been open for more than ${openMinutes} minutes!"
	if (phone) {
		sendSms(phone, msg)
	}
	else {
		sendPush msg
	}
}

def updateSmsHistory() {
	if (!state.smsHistory) state.smsHistory = []

	if(state.smsHistory.size() > 9) {
		log.debug "SmsHistory is too big, reducing size"
		state.smsHistory = state.smsHistory[-9..-1]
	}
	state.smsHistory << [sentDate: new Date().toSystemFormat()]
}

def clearSmsHistory() {
	state.smsHistory = null
}
