/**
 *  Smart Garage Door Opener
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

definition(
    name: "Smart Garage Door Opener",
    namespace: "darinspivey",
    author: "Darin Spivey",
    description: "When the selected Mode is activated, open the garage if all family members are home",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png")


preferences {
	section("Description") {
		paragraph "When away mode is activated, it reasons that someone is leaving.  If all presense sensors are home, and garage door is closed, open it."
    }
	section("Garage Door Contact Sensors") {
		input "garageSensor", "capability.contactSensor", required: true, title: "What is your garage door sensor?"
	}
	section("Garage Door Opener") {
		input "garageOpener", "capability.switch", required: true, title: "Which switch opens your garage door?"
	}
    section("Who is in your family?") {
		input "presenceSensors", "capability.presenceSensor", required: true, title: "These presence sensors", multiple: true
	}
    section("Which mode triggers the process?") {
    	input "watchMode", "mode", title: "select a mode", multiple: false
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
	unsubscribe()
	subscribe(location, "mode", modeChangeHandler)
	logit "Initialized.  Listen for mode change to $watchMode."
}

def modeChangeHandler(evt) {
	if (evt.value == watchMode) {
    	logit "$watchMode detected...running!"
        openDoorIfHome()
    }
}

def openDoorIfHome() {
	def gone = notEveryoneHome()
	if (gone) {
    	logit "$gone is gone.  Abort."
        return
    }
    if (garageSensor.currentValue("contact") == 'open') {
    	logit "Garage door is already open.  Abort."
        return
    }
    logit "Everyone is home!  Opening garage door!"
    garageOpener.on()
}

def notEveryoneHome() {
	presenceSensors.find {
    	it.currentPresence == 'not present'
    }
}

def logit(msg) {
	log.debug msg
    if (logToNotifications) {
    	sendNotificationEvent("[Smart Garage Door Opener] $msg")
    }
}