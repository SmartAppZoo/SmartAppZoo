/** Based on "Left It Open" smartapp from SmartThings.
 * 
 * This differs from "Left It Open" because that smartapp did not handle mode
 * changes. For instance, user configures it to only trigger when mode is night.
 * The door opens during mode=home and sunset changes mode to night. The original
 * would not trigger in this case to alert the owner that the door was left open.
 */

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
 *  Left It Open
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 */
definition(
	name: "Left It Open Long Time",
	namespace: "dsg",
	author: "SmartThings/Scott Gregory",
	description: "Notifies you when you have left a door or window open longer that a specified amount of time. Supports crossing modes.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

	section("Monitor this door or window") {
		input "contact", "capability.contactSensor"
	}
	section("And notify me if it's open for more than this many minutes (default 10)") {
		input "openThreshold", "number", description: "Number of minutes", required: false
	}
	section("Delay between notifications (default 10 minutes") {
		input "frequency", "number", title: "Number of minutes", description: "", required: false
	}
	section("Via text message at this number (or via push notification if not specified") {
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Phone number (optional)", required: false
		}
	}
}

def installed() {
	log.trace "installed()"
	subscribe()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	subscribe()
}

def subscribe() {
	subscribe(contact, "contact.open", doorOpen)
	subscribe(contact, "contact.closed", doorClosed)
	subscribe(location, "mode", modeChangeHandler)
}

/** Check the state of the door if we've crossed into a mode where we should only run.
 * For example, the door was opened in a mode that excluded this app and still open
 * in a new mode not excluded by this app. We only get called by SmartThings in
 * modes the user has not excluded.
*/
def modeChangeHandler(evt)
{
	def newMode = evt.value
	if(newMode != null && newMode!='') {
		def contactState = contact.currentState("contact")
		log.trace "mode changed to ${newMode}. Current door state is ${contactState.value}."
		if(contactState.value == "open") {
			/* WARNING: we can't determine the modes this app is valid and unschedule()
			  is expensive right now (June2016), so we may get a duplicate
			  check scheduled.
			*/
			scheduleOpenCheck()
		}
	}
}

def doorOpen(evt)
{
	log.trace "doorOpen($evt.name: $evt.value)"
	scheduleOpenCheck()
}

def scheduleOpenCheck()
{
	def t0 = now()
	def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
	runIn(delay, doorOpenTooLong, [overwrite: false])
	log.debug "scheduled doorOpenTooLong in ${delay} sec"
}

def doorClosed(evt)
{
	log.trace "doorClosed($evt.name: $evt.value)"
}

def doorOpenTooLong() {
	def contactState = contact.currentState("contact")
	def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

	if (contactState.value == "open") {
		def elapsed = now() - contactState.rawDateCreated.time
		def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
		if (elapsed >= threshold) {
			log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
			sendMessage()
			runIn(freq, doorOpenTooLong, [overwrite: false])
		} else {
			log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
		}
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}

void sendMessage()
{
	def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
	def msg = "${contact.displayName} has been left open for ${minutes} minutes."
	log.info msg
	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients)
	} else {
		if (phone) {
			sendSms phone, msg
		} else {
			sendPush msg
		}
	}
}