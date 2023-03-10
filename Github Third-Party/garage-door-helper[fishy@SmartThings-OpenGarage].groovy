/**
 *  Garage Door Helper.
 *
 *  Copyright 2018 Yuxuan "fishy" Wang
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
	name: "Garage Door Helper",
	namespace: "fishy",
	author: "Yuxuan Wang",
	description: "Use contact sensor to better refresh garage door state",
	category: "Safety & Security",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
)


preferences {
	section("Garage Door") {
		input(
			"door",
			"capability.refresh",
			title: "Pick a garage door",
			multiple: false,
		)
	}
	section("Contact Sensor") {
		input(
			"contact",
			"capability.contactSensor",
			title: "Pick a contact sensor",
			multiple: false,
			required: false,
		)
		input(
			"refresh_rate",
			"number",
			title: "refresh for every N seconds (default 5)",
			required: false,
		)
		input(
			"max_refresh",
			"number",
			title: "stop refreshing after N minutes (default 2)",
			required: false,
		)
	}
	section("Notification") {
		input(
			"push_notif",
			"bool",
			title: "Send push notification when Garage Door device handler failed",
		)
		input(
			"phone",
			"text",
			title: "Phone number to send SMS notification",
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
	if (contact) {
		subscribe(contact, "contact", contactHandler)
	}
	if (push_notif || phone) {
		subscribe(door, "lastHttpStatus", httpHandler)
	}
}

def getRefreshRate() {
	if (refresh_rate) {
		return refresh_rate
	}
	return 5
}

def getMaxRefresh() {
	if (max_refresh) {
		return max_refresh
	}
	return 2
}

def forceRefreshGarageState(data) {
	def timestamp = now()
	log.debug "forceRefreshGarageState: ${new Date()}, timestamp: $timestamp, stops at ${data.stopAt}"
	try {
		door.refresh()
	} catch (Exception e) {
		// Sometimes SmartThings platform will throw
		// groovy.lang.MissingMethodException: No signature of method: ...
		// here, but the door actually got the refresh command.
		// If we don't catch the exception here we will stop after the first
		// refresh, which is usually not enough.
		// As a result, log and ignore.
		log.debug "refresh failed with: $e"
	}
	if (timestamp >= data.stopAt) {
		log.debug "stopping refreshing..."
		unschedule(forceRefreshGarageState)
	} else {
		def options = [
			overwrite: true,
			data: data,
		]
		runIn(getRefreshRate(), forceRefreshGarageState, options)
	}
}

def contactHandler(evt) {
	log.debug "contactHandler: $evt.value: $evt, $settings"
	if (evt.isStateChange()) {
		def timestamp = now() + 60 * 1000 * getMaxRefresh()
		def data = [stopAt: timestamp]
		forceRefreshGarageState(data)
	}
}

def httpHandler(evt) {
	log.debug "httpHandler: $evt.value: $evt, $settings"
	if (evt.isStateChange() && evt.value == "failed") {
		def msg = "${door.displayName} http request failed, please check your settings."
		if (push_notif) {
			sendPush(msg)
		}
		if (phone) {
			sendSms(phone, msg)
		}
	}
}
