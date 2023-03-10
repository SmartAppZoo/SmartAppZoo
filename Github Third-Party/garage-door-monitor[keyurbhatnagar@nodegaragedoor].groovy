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
 *  Garage Door Monitor
 *
 *  Author: Keyur Bhatnagar
 */
definition(
    name: "Garage Door Monitor",
    namespace: "keyurbhatnagar",
    author: "Keyur Bhatnagar",
    description: "Monitor your garage door and get a text message if it is open too long",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("When the garage door is open...") {
		input "multisensor", "capability.contactSensor", title: "Which?"
	}
	section("For too long...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}    
    section {
			input "sonos", "capability.speechSynthesis", title: "On this Speaker player", required: true, multiple: true
	}
}

def installed()
{
	subscribe(multisensor, "contact", accelerationHandler)
}

def updated()
{
	unsubscribe()
	subscribe(multisensor, "contact", accelerationHandler)
}

def accelerationHandler(evt) {
	log.warn "Door open event = $evt.value"
	if (evt.value == "open") {
		runIn(maxOpenTime * 60, sendTextMessage, [overwrite: false])
		state.status = "scheduled"
		log.warn "Door open event"
	}
	else {
        unschedule()
		log.warn "Door open event"
	}
}

def takeAction(){
	log.warn "taking action"
	def deltaMillis = 1000 * 60 * maxOpenTime
	def timeAgo = new Date(now() - deltaMillis)
	def openTooLong = multisensor.threeAxisState.dateCreated.toSystemDate() < timeAgo

	def recentTexts = state.smsHistory.find { it.sentDate.toSystemDate() > timeAgo }
	log.warn "Recent text =${recentTexts}"
    sendTextMessage()
	runIn(maxOpenTime * 60, takeAction, [overwrite: false])
}

def sendTextMessage() {
	log.debug "$multisensor was open too long, texting phone"
    
	def openMinutes = maxOpenTime
	def msg = "Your ${multisensor.label ?: multisensor.name} has been open for more than ${openMinutes} minutes!"
    sonos.speak(msg)
    sendPush msg
}