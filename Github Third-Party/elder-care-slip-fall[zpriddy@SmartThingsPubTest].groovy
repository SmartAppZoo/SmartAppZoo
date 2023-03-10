/**
 *  Elder Care: Slip & Fall
 *
 *  Author: SmartThings
 *  Date: 2013-04-07
 * 
 */

definition(
    name: "Elder Care: Slip & Fall",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Monitors motion sensors in bedroom and bathroom during the night and detects if occupant does not return from the bathroom after a specified period of time.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer@2x.png"
)

preferences {
	section("Bedroom motion detector(s)") {
		input "bedroomMotion", "capability.motionSensor", multiple: true
	}
	section("Bathroom motion detector") {
		input "bathroomMotion", "capability.motionSensor"
	}
    section("Active between these times") {
    	input "startTime", "time", title: "Start Time"
        input "stopTime", "time", title: "Stop Time"
    }
    section("Send message when no return within specified time period") {
    	input "warnMessage", "text", title: "Warning Message"
        input "threshold", "number", title: "Minutes"
    }
    section("To these contacts") {
		input("recipients", "contact", title: "Recipients", description: "Send notifications to") {
			input "phone1", "phone", required: false
			input "phone2", "phone", required: false
			input "phone3", "phone", required: false
		}
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	state.active = 0
	subscribe(bedroomMotion, "motion.active", bedroomActive)
    subscribe(bathroomMotion, "motion.active", bathroomActive)
}

def bedroomActive(evt) {
	def start = timeToday(startTime)
    def stop = timeToday(stopTime)
    def now = new Date()
    log.debug "bedroomActive, status: $state.ststus, start: $start, stop: $stop, now: $now"
    if (state.status == "waiting") {
    	log.debug "motion detected in bedroom, disarming"
    	unschedule("sendMessage")
        state.status = null
    }
    else {
        if (start.before(now) && stop.after(now)) {
            log.debug "motion in bedroom, look for bathroom motion"
            state.status = "pending"
        }
        else {
            log.debug "Not in time window"
        }
    }
}

def bathroomActive(evt) {
	log.debug "bathroomActive, status: $state.status"
	if (state.status == "pending") {
    	def delay = threshold.toInteger() * 60
    	state.status = "waiting"
        log.debug "runIn($delay)"
        runIn(delay, sendMessage)
    }
}

def sendMessage() {
	log.debug "sendMessage"
	def msg = warnMessage
    log.info msg

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients)
	}
	else {
		sendPush msg
		if (phone1) {
			sendSms phone1, msg
		}
		if (phone2) {
			sendSms phone2, msg
		}
		if (phone3) {
			sendSms phone3, msg
		}
	}
    state.status = null
}
