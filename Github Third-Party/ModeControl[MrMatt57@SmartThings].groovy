/**
 *  Mode Control
 *
 *  Author: SmartThings
 *  Date: 2014-01-10
 *	Updated: mwwalker@gmail.com
 *
 *  Monitors a set of presence detectors and triggers a mode change when everyone has left.
 */

preferences {
	section("Presence") {
		input "people", "capability.presenceSensor", multiple: true, title: "Who to Monitor?"
		input "newAwayMode", "mode", title: "Mode when everyone is away?"
		input "newHomeMode", "mode", title: "Mode when someone returns home?"
	}
	section ("Time of Day") {
		input "sleepTime", "time", title: "When should sleep mode start?", required: false
        input "sleepTimeEnd", "time", title: "When should sleep mode end?", required: false
		input "newSleepMode", "mode", title: "Mode when everyone is sleeping (or should be)?", required: false
	}
	section("Settings") {
		input "falseAlarmThreshold", "decimal", title: "False alarm threshold minutes (default 10)", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "phone", "phone", title: "Send a Text Message?", required: false
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	init()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
    unschedule()
	init()
}
def init() {
	subscribe(people, "presence", presence)
    if (sleepTime) {
		schedule(sleepTime, setSleepMode)
    }
    if (sleepTimeEnd) {
    	schedule(sleepTimeEnd, setSleepModeEnd)
    }
}

def setSleepMode () {
	if (location.mode != newAwayMode) {
        if (newSleepMode) {
        	setLocationMode(newSleepMode)
            log.info("Setting mode to sleep")
        }
        else {
        	log.info("Sleep mode not set")
        }
    }
    else {
    	log.debug "Away, can't set sleep mode"
    }
}
def setSleepModeEnd () {
	if (location.mode != newAwayMode) {
        log.info("Setting mode to home")
        setLocationMode(newHomeMode)
    }
    else {
    	log.debug "Away, can't set sleep mode"
    }
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	if (evt.value == "not present") {
		if (location.mode != newAwayMode) {
			log.debug "checking if everyone is away"
			if (everyoneIsAway()) {
				log.debug "starting sequence"
				runIn(findFalseAlarmThreshold() * 60, "takeAction", [overwrite: false])
			}
		}
		else {
			log.debug "mode is the same, not evaluating"
		}
	}
	else {
		if (location.mode != newHomeMode) {
			log.debug("Checking if anyone is home")

			if (everyoneIsAway() == false) {
				log.info("Setting mode to home")
				setLocationMode(newHomeMode)
			}
		}
		else {
			log.debug("Mode is the same, not evaluating")
		}
	}
}

def takeAction()
{
	if (everyoneIsAway()) {
		def threshold = 1000 * 60 * findFalseAlarmThreshold() - 1000
		def awayLongEnough = people.findAll { person ->
			def presenceState = person.currentState("presence")
			def elapsed = now() - presenceState.rawDateCreated.time
			elapsed >= threshold
		}
		log.debug "Found ${awayLongEnough.size()} out of ${people.size()} person(s) who were away long enough"
		if (awayLongEnough.size() == people.size()) {
			// TODO -- uncomment when app label is available
			//def message = "${app.label} changed your mode to '${newAwayMode}' because everyone left home"
			def message = "Changed your mode to '${newAwayMode}' because everyone left home."
			log.info message
			send(message)
			setLocationMode(newAwayMode)
		} else {
			log.debug "not everyone has been away long enough; doing nothing"
		}
	} else {
    	log.debug "not everyone is away; doing nothing"
    }
}

private everyoneIsAway()
{
	def result = true
	for (person in people) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phone ) {
		log.debug( "sending text message" )
		sendSms( phone, msg )
	}

	log.debug msg
}

private findFalseAlarmThreshold() {
	(falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 10
}
