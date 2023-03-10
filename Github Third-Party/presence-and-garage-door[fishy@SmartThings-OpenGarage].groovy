/**
 *  Presence and Garage Door.
 *
 *  Copyright 2016 Yuxuan Wang
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
	name: "Presence and Garage Door",
	namespace: "fishy",
	author: "Yuxuan Wang",
	description: "Use presence sensor to automate Garage Door",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
	section("Garage Door") {
		input(
			"door",
			"capability.garageDoorControl",
			title: "Which One?",
		)
	}
	section("Presence Sensors") {
		input(
			"cars",
			"capability.presenceSensor",
			title: "Cars?",
			multiple: true,
		)
	}
	section("Real close threshold") {
		input(
			"seconds",
			"number",
			title: "Seconds?",
			required: false,
		)
		input(
			"phone",
			"text",
			title: "Send SMS to this number?",
			required: false,
		)
		input(
			"contact",
			"capability.contactSensor",
			title: "Optional helper contact sensor",
			required: false,
		)
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
	state.lastClose = now()
	state.shouldOpen = false
	subscribe(cars, "presence", presenceHandler)
	subscribe(door, "door", doorHandler)
	if (contact) {
		subscribe(contact, "contact", contactHandler)
	}
}

def doorHandler(evt) {
	log.debug "doorHandler: $evt.value: $evt, $settings"
	if ("closing" == evt.value || "closed" == evt.value) {
		state.lastClose = now()
	}
}

def contactHandler(evt) {
	log.debug "contactHandler: $evt.value: $evt, $settings"
	if ("closed" == evt.value) {
		state.lastClose = now()
	}
}

def presenceHandler(evt) {
	def carName = evt.getDevice().displayName
	log.debug "presenceHandler: $evt.value, $carName, $state, $settings"
	def doorState = door.latestValue("door")
	log.debug "Current garage state: ${doorState}"
	if ("present" == evt.value) {
		if (state.shouldOpen) {
			def msg = "Opening ${door.displayName} because $carName is home."
			log.debug "${msg}"
			sendPush(msg)
			door.open()
		} else {
			def msg = "NOT auto opening ${door.displayName} as we didn't auto close it earlier"
			log.debug "${msg}"
			if (phone) {
				sendSms(phone, msg)
			}
		}
	} else if ("not present" == evt.value) {
		def shouldClose = true
		def now = now()
		def elapsed = now - state.lastClose
		if (seconds) {
			def milliSec = seconds * 1000
			shouldClose = ("closed" != doorState) || (milliSec > elapsed)
		}
		state.shouldOpen = shouldClose
		if (shouldClose) {
			log.debug "Closing at ${state.lastClose}"
			def msg = "Closing ${door.displayName} because $carName is leaving."
			log.debug "${msg}"
			sendPush(msg)
			door.close()
		} else {
			def sec = elapsed / 1000
			def msg = "NOT auto closing as ${door.displayName} was ${doorState} and last closing was ${sec}s ago"
			log.debug "${msg}"
			if (phone) {
				sendSms(phone, msg)
			}
		}
	}
}
