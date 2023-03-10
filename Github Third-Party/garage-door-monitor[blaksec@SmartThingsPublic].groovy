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
 *  Author: SmartThings
 */
definition(
    name: "Garage Door Monitor",
    namespace: "blaksec",
    author: "George C.",
    description: "Monitor your garage door and get a text message if it is open too long",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("When the garage door is open...") {
		input "contact1", "capability.contactSensor", title: "Which?"
	}
	section("For too long...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
	section("Text me at (optional, sends a push notification if not specified)...") {
        input("recipients", "contact", title: "Notify", description: "Send notifications to") {
            input "phone", "phone", title: "Phone number?", required: false
        }
	}
}

def installed()
{
	log.trace "Calling installed()"
	//subscribe(multisensor, "acceleration", accelerationHandler)
}

def updated()
{
	log.trace "Calling updated()"
	unsubscribe()
    subscribe()
	//subscribe(multisensor, "acceleration", accelerationHandler)
}

def subscribe()
{
	subscribe(contact1, "contact.open", door1_Open)
	subscribe(contact1, "contact.closed", door1_Closed)
}

def door1_Open(evt)
{
	log.trace "door1_Open($evt.name: $evt.value)"
	def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
	runIn(delay, doorOpenTooLong, [overwrite: true])
}

def door1_Closed(evt)
{
	log.trace "door1_Closed($evt.name: $evt.value)"
	unschedule(doorOpenTooLong)
}

def doorOpenTooLong()
{
	def contact1State = contact1.currentState("contact")
  	//def contact2State = contact2.currentState("contact")
  	def freq = (frequency != null && frequency != "") ? frequency * 60 : 600
	if (contact1State.value == "open") {
		def elapsed = now() - contact1State.rawDateCreated.time
		def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    	if (elapsed >= threshold) {
      		log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      		sendMessage()
      		runIn(freq, doorOpenTooLong, [overwrite: false])
    	} 
        else {
      		log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    	}
  	} 
    else {
    	log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}
/*
def accelerationHandler(evt) {
	def latestThreeAxisState = multisensor.threeAxisState // e.g.: 0,0,-1000
	if (latestThreeAxisState) {
		def isOpen = Math.abs(latestThreeAxisState.xyzValue.z) > 250 // TODO: Test that this value works in most cases...
		def isNotScheduled = state.status != "scheduled"

		if (!isOpen) {
			clearSmsHistory()
			clearStatus()
		}

		if (isOpen && isNotScheduled) {
			runIn(maxOpenTime * 60, takeAction, [overwrite: false])
			state.status = "scheduled"
		}

	}
	else {
		log.warn "COULD NOT FIND LATEST 3-AXIS STATE FOR: ${multisensor}"
	}
}

def takeAction(){
	if (state.status == "scheduled")
	{
		def deltaMillis = 1000 * 60 * maxOpenTime
		def timeAgo = new Date(now() - deltaMillis)
		def openTooLong = multisensor.threeAxisState.dateCreated.toSystemDate() < timeAgo

		def recentTexts = state.smsHistory.find { it.sentDate.toSystemDate() > timeAgo }

		if (!recentTexts) {
			sendTextMessage()
		}
		runIn(maxOpenTime * 60, takeAction, [overwrite: false])
	} else {
		log.trace "Status is no longer scheduled. Not sending text."
	}
}
*/
def sendMessage() {
	log.debug "$multisensor was open too long, texting phone"

	updateSmsHistory()
	def openMinutes = maxOpenTime * (state.smsHistory?.size() ?: 1)
	def msg = "Your ${multisensor.label ?: multisensor.name} has been open for more than ${openMinutes} minutes!"
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
            sendSms(phone, msg)
        } else {
            sendPush msg
        }
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

def clearStatus() {
	state.status = null
}