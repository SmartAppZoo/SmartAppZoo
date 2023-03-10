/**
 *  Smart Family Presence
 *
 *  Copyright 2016 Darin Spivey
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
    name: "Smart Family Presence",
    namespace: "darinspivey",
    author: "Darin Spivey",
    description: "Smart arrival and departure push messages for couples/families that are traveling together.  When family members arrive and depart together, there is no need to send an individual push alert for each.",
    category: "Family",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png")


preferences {
	section("Family Members") {
		input "familySensors", "capability.presenceSensor", required: true, title: "Who's in your family?", multiple: true
	}
    section("Threshold") {
    	paragraph "Set the time in seconds to allow for group arrival/departure"
    	input "timeThreshold", "text", required: false, title: "Default is ${defaultThreshold}."
    }
	section("Smart departure alerts") {
    	paragraph "When family members are home together, departure push alerts may not be necessary because most of the time, people are aware when their family members are leaving.  This feature will only send a push alert if the the entire family was previously apart."
		paragraph "For example, when my wife and I are home together, I know when she's leaving; I don't need an alert for that.  If this feature is off, it will send a departure alert when she leaves."
        input name: "smartDepartureFeature", title: "Smart departure alerts?", type:"bool", defaultValue: true
	}
    section("Silent mode") {
    	input name: "silentMode", title: "Silence all notifications?", type:"bool", defaultValue: false
    }
    section("Verbose logging") {
    	paragraph "For debugging, you may log all the app's decisions to the notifications log."
        input name: "logToNotifications", title: "Log to notifications?", type: "bool", defaultValue: false
    }
}

/****************************
	Auto-getters and setters
*****************************/

def getDefaultThreshold() {
	60
}

def startMonitor() {
	startTime = new Date()
}

def stopMonitor() {
	startTime = null
}

def getDateFormat() {
	"M-d-y H:m:s.S"
}

def setStartTime(value) {
	/* Dates seems to get auto-serialized at some point in the state object.  So,
       just treat them as strings all the time.  Account for when we're resetting to null.
    */
	if (value instanceof Date) {
		def serializedDate = value.format(dateFormat)
		state.startTime = serializedDate
    }
    else {
    	state.startTime = value
    }
}

def getStartTime() {
	if (state.startTime) {
		def parsedDate = Date.parse(dateFormat, state.startTime)
        return parsedDate
    }
    else {
    	// Must be null.  Just return
        return state.startTime
    }
}

def getSmartDeparture() {
	settings.smartDepartureFeature
}

def getLogToNotifications() {
	settings.logToNotifications
}

def getThreshold() {
	settings.timeThreshold ? settings.timeThreshold.toInteger() : defaultThreshold
}

/****************************
 Framework methods
*****************************/

def installed() {
	logit "Installed..."
	initialize()
}
 
def updated() {
	logit "Updated..."
	unsubscribe()
	initialize()
}

def initialize() {
	stopMonitor()
    subscribe(familySensors, "presence", presenceHandler)
    logit "Initialize. Subscribed ${familySensors.toString()} to presenceHandler"

	/*
    	Regular usage shows that, during certain cases such as the hub going offline,
        or power outages, the app may lose state and not send alerts.  This will ensure that
        it re-evaluates its state at least once per hour.
    */
    
	logit "Scheduling a re-calibration at the top of every hour."
    schedule("0 0 0/1 1/1 * ? *", reset)
	reset()
}

/*****************************
 Smart Family Presence methods
******************************/

def inProgress() {
	/* 
    	Analyze whether or not we're inside of startMonitor()
    */
    if ( !(startTime instanceof Date) ) {
    	logit "inProgress? No."
    	return false
    }
    else {
        /* To make sure our timer isn't somehow abandoned, we'll set a maxAge of
           double whatever the user set their threshold at.  Multiply by 1k for milliseconds.
           Anything greater than this value, we can call the ball as an error and reset.
        */
        
        def now = new Date()
        def delta = now.time - startTime.time
        def maxAge = timeThreshold.toInteger() * 2
        
        if (delta <= maxAge * 1000) {
            logit "inProgress? Yes. Time delta from ${now} to ${startTime} is under maxAge of ${maxAge} secs."
        	return true
        }
        else {
        	logit "inProgress? Error condition detected.  Timer appears abandoned.  Set at ${startTime.format(dateFormat)}.  Resetting."
  			stopMonitor()
            return false
        }
    } 
}

def logit(msg) {
	log.debug msg
    if (logToNotifications) {
    	sendNotificationEvent("[Smart Family Presense] $msg")
    }
}

def reset() {
    if (inProgress()) {
    	logit "Skipping re-calibration, execution in progress!"
        return
    }
    logit "Re-calibrating."
    state.baseCase = null
    state.changedThisTime = []
    wasApart()
}

def isFamilyTogether() {
	// Check to see if the entire family has arrived/departed together

	if (state.changedThisTime.size() == 0) {
    	logit "No changes."
        stopMonitor()
        reset()
    	return
	}
    
	logit "People who changed presence: ${state.changedThisTime}"

	def theirState = state.baseCase
  	def notTogether = statusNotEquals(theirState)
    
  	if (notTogether) {
    	// The family is not together, send an alert as normal
        
    	logit "${notTogether.join(", ")} is not with the rest of the family (who are $theirState)"
        sendPushAlert()
    }
    else {
    	/* 
        	Special case - When everyone is gone, but they *previously* weren't together,
        	then technically they were apart to begin with and are still apart upon leaving
        */
            
        if (state.wasApart && theirState == 'not present') {
           	logit "Family was previously apart and now all gone.  Alert."
        	sendPushAlert()
        }
        else {    
    		logit "OK!  Everyone has arrived/departed together.  The family is $theirState"
        }
    }
    stopMonitor()
    reset()
}

def wasApart() {
	// This is true if everyone is gone, or some were home
	def allGone = statusEquals('not present') == familySensors
    def someHome = statusEquals('present') != familySensors
    if (allGone || someHome) {
    	state.wasApart = true
    }
    else {
    	state.wasApart = false
    }
}


def presenceHandler(evt) {
    def person = evt.displayName

   	if (settings.silentMode) {
    	logit "Ignoring presence handler.  Silent mode is ${settings.silentMode}."
        return
    }
    
	if (inProgress() == false) {
    	startMonitor()
    	state.baseCase = evt.value
        logit "First person sensed (${person}).  Checking for others to be $evt.value in ${threshold} seconds"
        runIn(threshold, isFamilyTogether, [overwrite: false])
	}
    else {
    	logit "Presence Event: $person is $evt.value"
	}
    
	if (! state.changedThisTime.contains(person)) {    
    	state.changedThisTime.push person
    }
    else {
    	// Special case - presence has changed within the threshold.  Remove this person.
        state.changedThisTime = state.changedThisTime - person
		logit "Ignoring flapping presence event for $person"
    }
}

def statusEquals(status) {
	if (status == null) return
    
	familySensors.findAll {
        it.currentPresence == status
    }
}

def statusNotEquals(status) {
	if (status == null) return
      
	familySensors.findAll {
        it.currentPresence != status 
    }
}

def sendPushAlert() {

   	if (settings.silentMode) {
    	logit "Ignoring push alert.  Silent mode is ${settings.silentMode}."
        return
    }
    
	def baseCase = state.baseCase
    def changedPeople = state.changedThisTime
    
    if (baseCase == 'not present' && state.wasApart == false && smartDeparture) {
    	logit "Not sending departure alert because smartDeparture is $smartDeparture"
    	return
    }
    
	def statuses = [ 'present':[], 'not present':[] ]

	for (sensor in familySensors) {
    	def person = sensor.toString()       
        if (changedPeople.contains(person)) {
        	def currentState = sensor.currentPresence
        	log.debug "$person is now $currentState"
            statuses[currentState].push person
        } 
	}
    logit "Alerting.  Statuses: $statuses"
    
    // Construct the message payload
    
    def pushMsg = ""    
    def home = statuses.present
    def notHome = statuses['not present']
 	String adVerb;
   
    if (home.size() > 0) {
    	adVerb = home.size > 1 ? "have" : "has"
    	pushMsg += "${home.join(", ")} $adVerb arrived $location.name. "
    }
    if (notHome.size() > 0) {
    	adVerb = notHome.size > 1 ? "have" : "has"
    	pushMsg += "${notHome.join(", ")} $adVerb left $location.name"
    }    	
    
    sendPush pushMsg
}