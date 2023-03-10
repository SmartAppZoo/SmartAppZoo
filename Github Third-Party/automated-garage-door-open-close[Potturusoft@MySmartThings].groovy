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
 * 	Automated Garage Door (Open / Close)
 *	Code modified from Ridiculously Automated Garage Door
 *
 *  Author: SmartThings
 *  Modified by: Taco / Trinitron79
 *  Date: 09-25-2016
 *
 * Monitors arrival and departure of car(s) and
 *
 *    1) opens door when car arrives,
 *    2) closes door after car has departed (for N minutes),
 *    3) opens door when car door motion is detected,
 *    4) closes door when door was opened due to arrival and interior door is closed.
 *
 * 
 * Modified the orginial version to have the garage door automatically close and send notifications.  Also changed to allow temporary override door open checks.
 *
 */

definition(
    name: "Automated Garage Door (Open / Close)",
    namespace: "SmartThings",
    author: "SmartThings",
    description: "Monitors arrival and departure of car(s) and 1) opens door when car arrives, 2) closes door after car has departed (for N minutes), 3) opens door when car door motion is detected, 4) closes door when door was opened due to arrival and interior door is closed. Modified the orginial version to have the garage door automatically close and send notifications.  Also changed to allow temporary override door open checks.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {

	section("Garage door") {
		input "doorSensor", "capability.contactSensor", title: "Which sensor?"
		input "doorSwitch", "capability.momentary", title: "Which switch?"
		input "openThreshold", "number", title: "Warn when open longer than (optional)",description: "Number of minutes", required: false
        input "autoCloseGarageDoor", "enum", title: "Close the garage door after being open longer than selected minutes", options: ['Yes', 'No'], required: true 
        input "skipcheck", "enum", title: "Working in the garage and want to keep the door open with no alerts? (If Yes door open checks will NOT happen, will need to reset to No when you're done in the garage to have checks happen again)", options: ['Yes', 'No'], required: true
        input("recipients", "contact", title: "Send notifications to") {
        input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
	}
	section("Car(s) using this garage door") {
		input "cars", "capability.presenceSensor", title: "Presence sensor", description: "Which car(s)?", multiple: true, required: false
		input "carDoorSensors", "capability.accelerationSensor", title: "Car door sensor(s)", description: "Which car(s)?", multiple: true, required: false
	}
	section("Interior door (optional)") {
		input "interiorDoorSensor", "capability.contactSensor", title: "Contact sensor?", required: false
	}
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "number", title: "Number of minutes", required: false
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
	log.debug "present: ${cars.collect{it.displayName + ': ' + it.currentPresence}}"
	subscribe(doorSensor, "contact", garageDoorContact)

	subscribe(cars, "presence", carPresence)
	subscribe(carDoorSensors, "acceleration", accelerationActive)

	if (interiorDoorSensor) {
		subscribe(interiorDoorSensor, "contact.closed", interiorDoorClosed)
	}
}

def doorOpenCheck()
{
	final thresholdMinutes = openThreshold
	if (thresholdMinutes) {
		def currentState = doorSensor.contactState
		log.debug "doorOpenCheck"
		if (currentState?.value == "open" & skipcheck == "No") {
			log.debug "open for ${now() - currentState.date.time}, openDoorNotificationSent: ${state.openDoorNotificationSent}"
			if (!state.openDoorNotificationSent && now() - currentState.date.time > thresholdMinutes * 60 *1000) {
                def msg = "${doorSwitch.displayName} has been open for ${thresholdMinutes} minutes"
				log.info msg
				if (autoCloseGarageDoor == "Yes") {
					def msg2 = "Closing ${doorSwitch.displayName} because it has been open for ${thresholdMinutes} minutes"
					log.debug "Auto Close Door selected, Calling to close Garage Door"
					log.info msg2
                    closeDoor()
					log.debug "Sending ST App Alert"
                    sendPush msg2
					if (phone) {
 						log.debug "Sending text to Phone number - ${phone}"                   
                        sendSms phone, msg2
                    }
                }
                else {
                    log.debug "Sending ST App Alert"
                    sendPush msg
                    if (phone) {
                        log.debug "Sending text to Phone number - ${phone}"
                        sendSms phone, msg
                    }
                }
             	state.openDoorNotificationSent = true   	
			}
			else {
				state.openDoorNotificationSent = false
			}
		}
 	}
}

def carPresence(evt)
{
	log.info "$evt.name: $evt.value"
	// time in which there must be no "not present" events in order to open the door
	final openDoorAwayInterval = falseAlarmThreshold ? falseAlarmThreshold * 60 : 600

	if (evt.value == "present") {
		// A car comes home

		def car = getCar(evt)
		def t0 = new Date(now() - (openDoorAwayInterval * 1000))
		def states = car.statesSince("presence", t0)
		def recentNotPresentState = states.find{it.value == "not present"}

		if (recentNotPresentState) {
			log.debug "Not opening ${doorSwitch.displayName} since car was not present at ${recentNotPresentState.date}, less than ${openDoorAwayInterval} sec ago"
		}
		else {
			if (doorSensor.currentContact == "closed") {
				openDoor()
                sendPush "Opening garage door due to arrival of ${car.displayName}"
                state.appOpenedDoor = now()
			}
			else {
				log.debug "door already open"
			}
		}
	}
	else {
		// A car departs
		if (doorSensor.currentContact == "open") {
			closeDoor()
			log.debug "Closing ${doorSwitch.displayName} after departure"
            sendPush("Closing ${doorSwitch.displayName} after departure")

		}
		else {
			log.debug "Not closing ${doorSwitch.displayName} because its already closed"
		}
	}
}

def garageDoorContact(evt)
{
	log.info "garageDoorContact, $evt.name: $evt.value"
	if (evt.value == "open") {
		schedule("0 * * * * ?", "doorOpenCheck")
	}
	else {
		unschedule("doorOpenCheck")
	}
}


def interiorDoorClosed(evt)
{
	log.info "interiorContact, $evt.name: $evt.value"

	// time during which closing the interior door will shut the garage door, if the app opened it
	final threshold = 15 * 60 * 1000
	if (state.appOpenedDoor && now() - state.appOpenedDoor < threshold) {
		state.appOpenedDoor = 0
		closeDoor()
	}
	else {
		log.debug "app didn't open door"
	}
}

def accelerationActive(evt)
{
	log.info "$evt.name: $evt.value"

	if (doorSensor.currentContact == "closed") {
		log.debug "opening door when car door opened"
		openDoor()
	}
}

private openDoor()
{
	if (doorSensor.currentContact == "closed") {
		log.debug "opening door"
		doorSwitch.push()
	}
}

private closeDoor()
{
	if (doorSensor.currentContact == "open") {
		log.debug "closing door"
		doorSwitch.push()
	}
}

private getCar(evt)
{
	cars.find{it.id == evt.deviceId}
}