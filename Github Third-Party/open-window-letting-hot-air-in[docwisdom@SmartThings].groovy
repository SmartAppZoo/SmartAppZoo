/**
 *  Open window letting hot air in?
 *
 *  Copyright 2014 Brian Critchlow
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
    name: "Open window letting hot air in?",
    namespace: "docwisdom",
    author: "Brian Critchlow",
    description: "check if an open window is letting hot air in",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	section("Monitor this door or window") {
		input "contact", "capability.contactSensor"
	}
    section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature rises above...") {
		input "temperature1", "number", title: "Temperature?"
	}
	section("Via text message at this number (or via push notification if not specified") {
		input "phone", "phone", title: "Phone number (optional)", required: false
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
    
}

def doorOpen(evt)
{
	log.trace "doorOpen($evt.name: $evt.value)"
	def contactState = contact.currentState("contact")
	if (contactState.value == "open") {
    	subscribe(temperatureSensor1, "temperature", temperatureHandler)
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}

}

def doorClosed(evt)
{
	log.trace "doorClosed($evt.name: $evt.value)"
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tooHot = temperature1
	def mySwitch = settings.switch1

	// TODO: Replace event checks with internal state (the most reliable way to know if an SMS has been sent recently or not).
	if (evt.doubleValue >= tooHot) {
		log.debug "Checking how long the temperature sensor has been reporting >= $tooHot"

		// Don't send a continuous stream of text messages
		def deltaMinutes = 10 // TODO: Ask for "retry interval" in prefs?
		def timeAgo = new Date(now() - (1000 * 60 * deltaMinutes).toLong())
		def recentEvents = temperatureSensor1.eventsSince(timeAgo)
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
		def alreadySentSms = recentEvents.count { it.doubleValue <= tooHot } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent to $phone1 within the last $deltaMinutes minutes"
			// TODO: Send "Temperature back to normal" SMS, turn switch off
		} else {
			log.debug "Temperature rose above $tooHot"
			sendMessage()
			
		}
	}
}

void sendMessage()
{
	def msg = "${contact.displayName} has been left open and the temperature rose above the threshold of ${temperature1}"
	log.info msg
	if (phone) {
		sendSms phone, msg
	}
	else {
		sendPush msg
	}
}
