/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Greetings Earthling
 *
 *  Author: SmartThings
 *  Date: 2013-03-07
 */
definition(
    name: "Mode Automation",
    namespace: "Sanfe75",
    author: "Simone",
    description: "Monitors a set of presence detectors and triggers a mode change when someone arrives at home.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {

	section("When one of these people leave home") {
		input "people", "capability.presenceSensor", multiple: true
	}
	section("Mode for someone Away") {
		input "someoneAway", "mode", title: "Mode?"
	}
    section("Mode for anyone Away") {
		input "anyoneAway", "mode", title: "Mode?"
	}
	section("Action delay time (minutes) (defaults to 10 min)") {
		input "delay", "decimal", title: "Number of minutes", required: false
	}
    /*
	section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone", "phone", title: "Send a Text Message?", required: false
        }
	}*/
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
    subscribe(people, "presence", presence)
}

def presence(evt) {

	log.debug "evt.name: $evt.value"
    
    //def threshold = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? (falseAlarmThreshold * 60 * 1000) as Long : 10 * 60 * 1000L
    if (evt.value == "not present") {
        def secondsDelay = (delay != null && delay != "") ? (delay * 60) : 10 * 60
        runIn(getDelay(), presenceCheck, [overwrite: false])
    }
/*
	if (location.mode != someoneMode) {

		def t0 = new Date(now() - threshold)
		if (evt.value == "present") {

			def person = getPerson(evt)
			def recentNotPresent = person.statesSince("presence", t0).find{it.value == "not present"}
			if (recentNotPresent) {
				log.debug "skipping notification of arrival of Person because last departure was only ${now() - recentNotPresent.date.time} msec ago"
			}
			else {
				def message = "${person.displayName} arrived at home, changing mode to '${someoneMode}'"
				send(message)
				setLocationMode(someoneMode)
			}
		}
	}
	else {
		log.debug "mode is the same, not evaluating"
	}*/
}

def presenceCheck() {
    
    def awayPeople = 0
    for (person in people) {
        if (person.currentPresence == "not present") {
            def presenceState = person.currentState("presence")
            def elapsed = now() - presenceState.rawDateCreated.time
            if (elapsed >= getDelay() * 1000) {
                awayPeople += 1
            }
        }
    }
    
    if (awayPeople == people.size()) {
        //def message = "${app.label} changed your mode to '${newMode}' because everyone left home"
        //def message = "SmartThings changed your mode to '${newMode}' because everyone left home"
        //log.info message
        //send(message)
        setLocationMode(anyoneAway)
        log.debug "Mode changed, everyone Away"
    } else if (awayPeople == 0) {
        log.debug "False Alarm, everyone Home"
    } else {
        setLocationMode(someoneAway)
        log.debug "Mode changed, someone Away"
    }
}

private getDelay() {
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10 * 60
    (delay != null && delay != "") ? delay * 60 : 600
}

/*
private getPerson(evt)
{
	people.find{evt.deviceId == it.id}
}

private send(msg) {
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone) {
            log.debug("sending text message")
            sendSms(phone, msg)
        }
    }
}*/