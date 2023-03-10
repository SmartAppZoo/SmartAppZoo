/**
 *  Check for open Garage Doors
 *
 *  Copyright 2016 Dieter Rothhardt
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
    name: "Garage Door Supervisor",
    namespace: "dasalien",
    author: "Dieter Rothhardt",
    description: "Check for open Garage Doors",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When the garage door is open...") {
		input "GarageDoor", "capability.garageDoorControl", title: "Which?"
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
	subscribe(GarageDoor, "door", doorHandler)
}

def updated()
{
	unsubscribe()
	subscribe(GarageDoor, "door", doorHandler)
}

def doorHandler(evt) {
	def doorState = GarageDoor.currentValue("door")
    def isNotScheduled = false
    //log.warn "doorState  ${doorState}"

	if (doorState == "open") {
		isNotScheduled = state.status != "scheduled"
	}
    
	if (doorState == "closed") {
            clearSmsHistory()
			clearStatus()
	}

	if (isNotScheduled) {
			runIn(maxOpenTime * 60, takeAction, [overwrite: false])
			state.status = "scheduled"
	}
}


def takeAction() {
	if (state.status == "scheduled")
	{
		def deltaMillis = 1000 * 60 * maxOpenTime
		def timeAgo = new Date(now() - deltaMillis)

		def recentTexts = state.smsHistory.find { it.sentDate.toSystemDate() > timeAgo }

		if (!recentTexts) {
			sendTextMessage()
		}
		runIn(maxOpenTime * 60, takeAction, [overwrite: false])
	} else {
		log.trace "Status is no longer scheduled. Not sending text."
	}
}

def sendTextMessage() {
	updateSmsHistory()
	def openMinutes = maxOpenTime * (state.smsHistory?.size() ?: 1)
	def msg = "Your ${GarageDoor.label ?: GarageDoor.name} has been open for more than ${openMinutes} minutes!"
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