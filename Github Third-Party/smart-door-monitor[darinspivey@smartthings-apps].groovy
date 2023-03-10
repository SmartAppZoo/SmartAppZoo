/**
 *  Smart Door Monitor
 *
 *  Copyright 2017 Darin Spivey
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
import groovy.time.TimeCategory

definition(
    name: "Smart Door Monitor",
    namespace: "darinspivey",
    author: "Darin Spivey",
    description: "When someone leaves, check specified contact sensors to see if they're open.",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation13-icn@2x.png")


preferences {
	section("Description") {
		paragraph "When the mode changes, check a list of contact sensors after departure, and send a notification if they are open (e.g. did you leave your garage open?)"
    }
	section("Contact Sensors") {
		input "contactSensors", "capability.contactSensor", required: true, title: "Which sensors to monitor?", multiple: true
	}
    section("Wait for departure of ANY of...") {
		input "presenceSensors", "capability.presenceSensor", required: true, title: "These presence sensors", multiple: true
	}
    section("Verbose logging") {
    	paragraph "For debugging, you may log all the app's decisions to the notifications log."
        input name: "logToNotifications", title: "Log to notifications?", type: "bool", defaultValue: false
    }
}

def installed() {
	logit "Installed..."
	initialize()
}
 
def updated() {
	logit "Updated..."
	initialize()
}

def initialize() {
	turnOff()
	unsubscribe()
    subscribe(presenceSensors, "presence", presenceHandler)
    logit "Initialized.  Listen for departures from ${presenceSensors.toString()}."
}

def turnOff() {
    state.stillExecuting = false
    logit "Done."
}

def presenceHandler(evt) {
    def person = evt.displayName
    logit "Presence Event: $person is $evt.value"
    
	if (hasLeft(person)) {
	    if (state.stillExecuting) return
        state.stillExecuting = true
    	findOpenSensors()
    }
    else {
    	// Just in case something is out of sync
    	turnOff()
    }
}

def hasLeft(person) {
	presenceSensors.find {
    	it.currentPresence == 'not present' && it.displayName == person
    }
}

def findOpenSensors() {
	def openSensors = contactSensors.findAll {
        it.currentValue("contact") == "open"
    }
    if (openSensors) {
    	def message = "WARNING! These sensors are OPEN: ${openSensors.join(', ')}"
 		sendPush message
        logit message
    }
    else {
    	logit "All OK!  Sensors are closed."
    }
    turnOff()
}

def logit(msg) {
	log.debug msg
    if (logToNotifications) {
    	sendNotificationEvent("[Smart Door Monitor] $msg")
    }
}