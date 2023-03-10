/**
 *  Smartass Garage Door
 *
 *  Copyright 2016 Jon Scheiding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Smartass Garage Door",
	namespace: "jonscheiding",
	author: "Jon Scheiding",
	description: "Garage door app with the smarts to get by in this world.",
	category: "My Apps",
	iconUrl: "https://png.icons8.com/material/30/000000/garage-closed.png",
	iconX2Url: "https://png.icons8.com/material/60/000000/garage-closed.png",
	iconX3Url: "https://png.icons8.com/material/90/000000/garage-closed.png")


preferences {
	section("Garage Door") {
		input "doorSwitch", "capability.momentary", title: "Opener", required: true
		input "doorContactSensor", "capability.contactSensor", title: "Open/Close Sensor", required: true
		input "doorAccelerationSensor", "capability.accelerationSensor",  title: "Movement Sensor", required: false
	}
	section("Car / Driver") {
		input "driver", "capability.presenceSensor", title: "Presence Sensor", required: true
	}
	section("Interior Door") {
		input "interiorDoor", "capability.contactSensor", title: "Open/Close Sensor", required: false
	}
	section("Notifications") {
		input "shouldSendPush", "enum", title: "Push Notifications", defaultValue: "All", options: ["None", "All", "Notices"]
	}
	section("Behavior") {
		input "openOnArrival", "bool", title: "Open On Arrival", defaultValue: true
        input "arrivalDebounceMinutes", "number", title: "... Except Minutes After Departure", defaultValue: 0
		input "closeOnDeparture", "bool", title: "Close On Departure", defaultValue: true
        input "closeOnDepartureDelayMinutes", "number", title: "... After Minutes", defaultValue: 0
		input "closeOnEntry", "enum", title: "Close On Interior Door Entry", defaultValue: "Never", options: ["Never", "Open", "Closed"]
        input "closeOnEntryDelayMinutes", "number", title: "... After Minutes", defaultValue: 0
		input "closeOnModes", "mode", title: "Close When Entering Mode", multiple: true, required: false
	}
}

def minutesToSeconds() {
	return 60;
}

def minutesToMilliseconds() {
	return minutesToSeconds() * 1000;
}

def onDriverArrived(evt) {
	state.lastArrival = now()

	if(openOnArrival) {
    	if(now() < state.lastDeparture + (arrivalDebounceMinutes * minutesToMilliseconds())) {
        	notifyIfNecessary "${doorSwitch.displayName} will not be triggered because ${driver.displayName} left less than ${arrivalDebounceMinutes} minutes ago.", true
            return
        }

		pushDoorSwitch("open", "Opening ${doorSwitch.displayName} due to arrival of ${driver.displayName}.")
    }
}

def onDriverDeparted(evt) {
	state.lastDeparture = now()

	if(closeOnDeparture) {
		if (closeOnDepartureDelayMinutes <= 0) {
			pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to departure of ${driver.displayName}.", true)
		} else {
			runIn(closeOnDepartureDelayMinutes * minutesToSeconds(), onDepartureDelayExpired)
		}
	}
}

def onInteriorDoorEntry(evt) {
	def expirationMinutes = 15

	if(state.lastArrival < state.lastClosed)
		return
	if(state.lastArrival < (now() - (expirationMinutes * minutesToMilliseconds())))
		return

    if(closeOnEntryDelayMinutes <= 0) {
		pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to entry into ${interiorDoor.displayName}.", true)
		state.lastArrival = 0
	} else {
		state.lastEntry = now()
		notifyIfNecessary("${doorSwitch.displayName} will close in ${closeOnEntryDelayMinutes} minutes due to entry into ${interiorDoor.displayName}.")
		runIn(closeOnEntryDelayMinutes * minutesToSeconds(), onEntryDelayExpired)
		state.lastArrival = 0
	}
}

def onEntryDelayExpired() {
	if(state.lastEntry + (closeOnEntryDelayMinutes * minutesToMilliseconds()) > now())
		return

	if(state.lastClosed > state.lastEntry)
		return

    pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to recent entry into ${interiorDoor.displayName}.", true)
	state.lastEntry = 0
}

def onDepartureDelayExpired() {
	if(state.lastDeparture + (closeOnDepartureDelayMinutes * minutesToMilliseconds) > now())
		return

	if(state.lastClosed > state.lastDeparture)
		return

	pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} due to recent departure of ${driver.displayName}.", true)
	state.lastDeparture = 0
}

def onGarageDoorClosed(evt) {
	state.lastClosed = now()
}

def onGarageDoorOpen(evt) {
	state.lastOpened = now()
}

def onModeChanged(evt) {
	if(!closeOnModes) return

	if(closeOnModes?.find { it == evt.value }) {
		if(state.lastOpened > now() - (1 * minutesToMilliseconds())) {
			notifyIfNecessary("Mode changed to ${evt.value}, but not closing ${doorSwitch.displayName} because it was just opened.", true)
		}

		pushDoorSwitch("closed", "Closing ${doorSwitch.displayName} because mode changed to ${evt.value}.")
	}
}

def pushDoorSwitch(desiredState, msg, isNotice = false) {
	if(doorContactSensor.currentContact == desiredState) {
		notifyIfNecessary "${doorSwitch.displayName} will not be triggered because it is already ${desiredState}.", false
		return
	}
	if(doorAccelerationSensor && doorAccelerationSensor.currentAcceleration == "active") {
		notifyIfNecessary "${doorSwitch.displayName}  will not be triggered because it is currently in motion.", true
		return
	}

	notifyIfNecessary msg, isNotice
	doorSwitch.push()
}

def notifyIfNecessary(msg, isNotice = false) {
	log.info msg

	def sendEverything = shouldSendPush == "1" || shouldSendPush == "All"
	def sendNoticesOnly = sendEverything || shouldSendPush == "2" || shouldSendPush == "Notices"

	log.debug("shouldSendPush=${shouldSendPush}, isNotice=${isNotice}")
	if(sendEverything || (sendNoticesOnly && isNotice)) {
		sendPush msg
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(driver, "presence.present", onDriverArrived)
	subscribe(driver, "presence.not present", onDriverDeparted)

	subscribe(doorContactSensor, "contact.closed", onGarageDoorClosed)
	subscribe(doorContactSensor, "contact.open", onGarageDoorOpen)

	subscribe(location, "mode", onModeChanged)

	if(interiorDoor && closeOnEntry != "0") {
    	def contactEvent = (closeOnEntry == "1") ? "open" : "closed"
		subscribe(interiorDoor, "contact.${contactEvent}", onInteriorDoorEntry)
	}
}
