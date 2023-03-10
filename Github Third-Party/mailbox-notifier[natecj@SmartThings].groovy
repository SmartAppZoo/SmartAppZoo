/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
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

/**
 *  Original Author: Edgar Santana
 *  Source: https://github.com/ersantana3/Smart-Mail-Box
 *
 */

definition(
  name: "Mailbox Notifier",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Get notifications about new mail",
  category: "Convenience",
  iconUrl: "http://www.iconpng.com/png/city/mail.png",
  iconX2Url: "http://4.bp.blogspot.com/_bqCxQKDqifI/S9sp2PIWS4I/AAAAAAAADgE/mU7FFUtIm6k/s1600/mailbox.png"
)

preferences {
	section("Select the sensor that detects the mailbox being open/closed"){
		input "contact", "capability.contactSensor", title: "Sensor", required: true
	}
  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
    input "sendTextMessage", "phone", title: "Phone number for text notifications.", required: false
  }
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
	subscribe(contact, "contact.open", eventHandler)
	//subscribe(contact, "contact.closed", eventHandler)
}

def eventHandler(evt) {
	def message = defaultText(evt)
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessage(message)
			state[evt.deviceId] = now()
		}
	} else {
		sendMessage(message)
	}
}

private sendMessage(message) {
	if (sendPushMessage == "Yes")
		sendPush(message)
  if (sendTextMessage && sendTextMessage != "" && sendTextMessage != "0")
    sendSms(sendTextMessage, message)
}
