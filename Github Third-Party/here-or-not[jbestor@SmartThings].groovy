/**
 *  Set Mode According To Presence
 *
 *  Copyright 2014 Bruno Botvinik
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
 */
definition(
    name: "Here or Not",
    namespace: "botvinik",
    author: "Bruno Botvinik",
    description: "Change mode automatically according to family's members presence",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select people's presence that will control your home's mode") {
		input "people", "capability.presenceSensor", multiple: true
	}
    
	section("Mode settings") {
		input "awayMode", "mode", title: "When everybody is away change to this mode", required: true
		input "homeMode", "mode", title: "When someone is at home change to this mode", required: true
		input "awayModeDelay", "decimal", title: "Delay before switching to 'away' mode (defaults to ${awayModeDelayDefault()} min.)", required: false
		input "ignoreModes", "mode", title: "Ignore presence changes when your home's mode is set to:", required: false, multiple: true
		input "modeNotify", "enum", title: "Send notification when mode changes?", metadata:[values:["Yes", "No"]], required: false
	}
	
	section("Select contact sensors to check when everybody is away") {
		input "contacts", "capability.contactSensor", multiple: true, required: false
	}

	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "smsNotification", "phone", title: "Send a text message?", required: false
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
	subscribe(people, "presence", presence)
}

// TODO: implement event handlers

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	if (ignorePresenceChange() == true) {
    	log.debug "current mode is one of the ignoreModes; not evaluating"
        return
    }
    
    if (evt.value == "not present") {
		if (location.mode != awayMode) {
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
		if (location.mode != homeMode) {
        	takeAction()
        } else {
			log.debug "mode is the same, not evaluating"
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
			setLocationMode(awayMode)
            notifyChangePush(true)
            notifyChangeSms(true)
            checkContacts()
		} else {
			log.debug "not everyone has been away long enough; doing nothing"
		}
	} else {
		setLocationMode(homeMode)
        notifyChangePush(false)
        notifyChangeSms(false)
		log.debug "someone is at home; changing to 'home' mode"
	}
}

private ignorePresenceChange() {
	def result = false
    
    if (ignoreModes != null) {
    	def modes = []
        if ((ignoreModes instanceof String) == true)
        	modes = [ignoreModes]
        else
        	modes =  ignoreModes   
        
        for (mode in modes) {
            if (location.mode == mode)
            	return true
        }
    }
    return result
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

private checkContacts() {

	def warning = ""
    for (contact in contacts) {
        if (contact.currentContact == "open") {
        	warning = warning + "${contact.displayName} is open. "
        }
    }
    
    if (warning != "") {
    	if (sendPushMessage != null && sendPushMessage == "Yes") {
        	sendPush("WARNING: ${warning}")
        }
        
        if (smsNotification != null && smsNotification != "") {
        	sendSms(smsNotification, "WARNING: ${warning}")
        }
	}
}

private findFalseAlarmThreshold() {
	(awayModeDelay != null && awayModeDelay != "") ? awayModeDelay : awayModeDelayDefault()
}

private notifyChangePush(isAway) {
	
    if (modeNotify != null && modeNotify == "Yes" && sendPushMessage != null && sendPushMessage == "Yes") {
		log.debug "Sending push notification"
		if (isAway)
            sendPush("See you soon! Mode is now ${awayMode}")
        else
            sendPush("Welcome back! Mode is now ${homeMode}")    
	}
}

private notifyChangeSms(isAway) {

	if (modeNotify != null && modeNotify == "Yes" && smsNotification != null && smsNotification != "") {
		log.debug "Sending sms notification"
		if (isAway)
        	sendSms(smsNotification, "See you soon! Mode is now ${awayMode}")
    	else
    		sendSms(smsNotification, "Welcome back! Mode is now ${homeMode}")
	}
}


private awayModeDelayDefault() {
	return 3
}