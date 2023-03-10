/**
 *  Good Night Good Morning
 *
 *  Copyright 2020 Jason Bestor
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
    name: "Good Night Good Morning",
    namespace: "Jbestor",
    author: "Jason Bestor",
    description: "Replacing Things Quiet Down and Things Start to Happen",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {    
    section("When things have quieted down...") {
    	input "nightMode", "mode", title: "Change to this mode", required: true
		input "nightMotion", "capability.motionSensor", multiple: true, title: "Where should there be no motion?", required: true
        input "delayMinutes", "number", title: "How long there should be no movement for in Minutes", required: true
		input "nightTime", "time", title: "Start after...", required: true
	}
    
    section("When things start to happen...") {
    	input "dayMode", "mode", title: "Change to this mode", required: true
		input "dayMotion", "capability.motionSensor", multiple: true, title: "Where is morning motion detected?", required: true
        input "dayTime", "time", title: "Start after...", required: true
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
	subscribe(dayMotion, "motion.active", dayMotionDetectedHandler)
    subscribe(nightMotion, "motion", nightMotionHandler)
    schedule(dayTime, dayTimeHandler)
    schedule(nightTime, nightTimeHandler)
}

def dayMotionDetectedHandler(evt) {
	log.debug "dayMotionDetectedHandler called: $evt"
    def between = timeOfDayIsBetween(dayTime, nightTime, new Date(), location.timeZone)
    if (location.mode == nightMode && between) {
		changeDay()
    }
    else {
    	log.debug "${dayMode} not detected, Current Mode: ${location.mode} and day time: $between"
    }
}

def dayTimeHandler() {
	log.debug "dayTimeHandler called at ${new Date()}"
    if (daySensorsActive()) {
    	changeDay()
    }
    else {
    	log.debug "day sensors inactive"
    }
}

def nightMotionHandler(evt) {
    if (evt.value == "active") {
        nightMotionDetectedHandler(evt)
    } else if (evt.value == "inactive") {
        nightMotionStoppedHandler(evt)
    }
}

def nightMotionDetectedHandler(evt) {
	log.debug "nightMotionDetectedHandler called: $evt"
    unschedule(checkNight)
    log.debug "checkNight unscheduled"
}

def nightMotionStoppedHandler(evt) {
	log.debug "nightMotionStoppedHandler called: $evt"
    def between = timeOfDayIsBetween(nightTime, dayTime, new Date(), location.timeZone)
    if (between) {
    	log.debug "checkNight will run in ${delayMinutes} minutes"
	    runIn(delayMinutes*60, checkNight)
	}
    else {
    	log.debug "night time: $between"
    }
}

def nightTimeHandler() {
	log.debug "nightTimeHandler called at ${new Date()}"
    if (nightSensorsInactive()) {
    	log.debug "checkNight will run in ${delayMinutes} minutes"
    	runIn(delayMinutes*60, checkNight)
    }
    else {
    	log.debug "night sensors active"
    }
}

def checkNight() {
	if (location.mode == dayMode) {
		changeNight()
    }
    else {
    	log.debug "${nightMode} not detected, Current Mode: ${location.mode}"
    }
}

def changeNight() {
	setLocationMode(nightMode)
	notifyChangePush(true)
	notifyChangeSms(true)
    log.info "Night inactivity detected; changing to 'night' mode"
}

def changeDay() {
	setLocationMode(dayMode)
	notifyChangePush(false)
	notifyChangeSms(false)
    log.info "Morning motion detected; changing to 'day' mode"
}

private daySensorsActive() {
	def result = true
    for (sensor in dayMotion) {
    	log.debug "${sensor.displayName}:${sensor.currentState("motion").value}"
        if (sensor.currentState("motion").value == "inactive") {
			result = false
            break
    	}
    }
    log.debug "Day sensors are active: $result"
    return result
}

private nightSensorsInactive() {
	def result = true
    for (sensor in nightMotion) {
    	log.debug "${sensor.displayName}:${sensor.currentState("motion").value}"
        if (sensor.currentState("motion").value == "active") {
			result = false
            break
    	}
    }
    log.debug "Night sensors are inactive: $result"
    return result
}

private notifyChangePush(isNight) {
	
    if (sendPushMessage != null && sendPushMessage == "Yes") {
		log.debug "Sending push notification"
		if (isNight)
            sendPush("Good Night! Mode is now ${nightMode}")
        else
            sendPush("Good Morning! Mode is now ${dayMode}")    
	}
}

private notifyChangeSms(isNight) {

	if (smsNotification != null && smsNotification != "") {
		log.debug "Sending sms notification"
		if (isNight)
        	sendSms(smsNotification, "Good Night! Mode is now ${nightMode}")
    	else
    		sendSms(smsNotification, "Good Morning! Mode is now ${dayMode}")
	}
}
