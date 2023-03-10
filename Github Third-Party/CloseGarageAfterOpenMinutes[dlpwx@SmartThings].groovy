/**
 *  Close garage door after xx minutes, optionally using separate tilt sensor
 *
 *  Copyright 2015 dlee
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
 *
 */

definition(
    name: "Close garage door after XX minutes",
    namespace: "dlee",
    author: "SmartThings",
    description: "Close garage after xx minutes, use door controller sensor or other tilt sensor.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {

	section("Close this garage door") {
		input "door", "capability.doorControl", title: "Which garage door controller?"
        input "contact", "capability.contactSensor", title: "Which garage door open/close sensor?"
		input "openThreshold", "number", title: "Close when open longer than",description: "Number of minutes", required: true
		input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
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
	subscribe(contact, "contact", contactState)
	subscribe(door, "door", garageDoorState)
}

def doorOpenCheck()
{
	final thresholdMinutes = openThreshold
	if (thresholdMinutes) {
		def currentState = contact.contactState
		log.debug "doorOpenCheck"
		if (currentState?.value == "open") {
			log.debug "open for ${now() - currentState.date.time}, openDoorNotificationSent: ${state.openDoorNotificationSent}"
			if (!state.openDoorNotificationSent && now() - currentState.date.time > thresholdMinutes * 60 * 1000) {
				def msg = "${door.displayName} has been open for ${thresholdMinutes} minutes"
				log.info msg
				sendPush msg
				if (phone) {
					sendSms phone, msg
				}
                closeDoor()
				log.debug "Closing ${door.displayName} left open"
				sendPush("Closing ${door.displayName} left open")
				state.openDoorNotificationSent = true
			}
		}
		else {
			state.openDoorNotificationSent = false
		}
	}
}

def contactState(evt)
{
	log.info "contactState, $evt.name: $evt.value"
	if (evt.value == "open") {
		schedule("0 * * * * ?", "doorOpenCheck")
	}
	else {
		unschedule("doorOpenCheck")
	}
}

private closeDoor()
{
	door.close()
}
