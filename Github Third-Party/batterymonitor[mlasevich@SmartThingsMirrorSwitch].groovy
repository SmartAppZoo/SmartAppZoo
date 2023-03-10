/**
 *  Copyright 2019 Michael Lasevich
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
 *  Notify Me When Battery Is Low
 */
definition(
		name: "BatteryMonitor",
		namespace: "com.legrig",
		author: "Michael Lasevich",
		description: "Receive notifications when device battery is low",
		category: "Convenience",
		iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn.png",
		iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn@2x.png",
		iconX3Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn@3x.png",
		pausable: true
		)

preferences {
	section("Choose one or more, when..."){
		input "sensor", "capability.battery", title: "Sensor(s) To Monitor", required: false, multiple: true
	}
	section("Level To Alert At or Below(in percent)") {
		input "threshold", "decimal", title: "Percent", required: false, defaultValue: 77
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
	}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(sensor, "battery", batteryLevelHandler)
}

def batteryLevelHandler(evt) {
	log.debug "Battery Level is ${evt.value} - threshold is ${threshold}"
	if (evt.value.isInteger() && (evt.value.toInteger() <= threshold)){
		log.debug "Battery Level is below threshold ${evt.value} <= ${threshold}"
		if (frequency) {
			def lastTime = state[evt.deviceId]
			if (lastTime == null || now() - lastTime >= frequency * 60000) {
				sendMessage(evt)
			}
		} else {
			sendMessage(evt)
		}
	}
}

private sendMessage(evt) {
	String msg = messageText
	Map options = [:]

	if (!messageText) {
		msg = defaultText(evt)
		options = [translatable: true, triggerEvent: evt]
	}
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private defaultText(evt) {
	return "{{ triggerEvent.linkText }} battery level is low (${evt.value}%)"
}