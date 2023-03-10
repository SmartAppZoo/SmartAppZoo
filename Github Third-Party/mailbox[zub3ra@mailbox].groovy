/**
 *  Mailbox
 *
 *  Copyright 2020 Clas Karlebrink
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
    name: "Mailbox",
    namespace: "CKMailbox",
    author: "Clas Karlebrink",
    description: "Mailbox with one sensor for getting mail, another for fetching mail.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Getting mail sensor"){
		input "contactGetMail", "capability.contactSensor", title: "Get mail contact that closes", required: false, multiple: true
	}
	section("Fetching mail sensor"){
		input "contactFetchMail", "capability.contactSensor", title: "Fetch mail contact that closes", required: false, multiple: true
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageTextGet", type: "text", title: "Mail Delivered?", required: false, defaultValue: "Posten har kommit"
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageTextFetch", type: "text", title: "Mail Fetched?", required: false, defaultValue: "Posten är hämtad"
	}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
}

def initialize() {
    state.isThereMail = false
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(contactGetMail, "contact.closed", eventHandlerGotMail)
	subscribe(contactFetchMail, "contact.closed", eventHandlerFetchMail)
}

def uninstalled() {
	log.debug "Uninstalled with settings: ${settings}"
	unsubscribe()
}

def subscribeToEvents() {
	subscribe(contactGetMail, "contact.closed", eventHandlerGotMail)
	subscribe(contactFetchMail, "contact.closed", eventHandlerFetchMail)
}


def eventHandlerGotMail(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"

	state.isThereMail = true

	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessageGet(evt)
		}
	}
	else {
		sendMessageGet(evt)
	}
}

def eventHandlerFetchMail(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"

	state.isThereMail = false

    if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessageFetch(evt)
		}
	}
	else {
		sendMessageFetch(evt)
	}
}

private sendMessageGet(evt) {
	def msg = messageTextGet ?: defaultText(evt)
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

    def myjson = '{"gotmail": true}'
	sendCommand(myjson)
    sendPush(msg)

	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private sendMessageFetch(evt) {
	def msg = messageTextFetch ?: defaultText(evt)
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"
    def myjson = '{"gotmail": false}'
	sendCommand(myjson)
	if (!phone || pushAndPhone != "No") {
		log.debug "sending push"
		sendPush(msg)
	}
	if (phone) {
		log.debug "sending SMS"
		sendSms(phone, msg)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private sendCommand(myjson) {
    def headers = [:] 
    headers.put("Host", "192.168.1.82:5000")
    headers.put("Content-Type", "application/json")
    headers.put("Accept", "*/*")
    def path = "/api/mail"
    def method = "POST"

    log.debug "The Header is $headers"

    try {
        def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: path,
            body: myjson,
            headers: headers,
        )

        log.debug hubAction
        sendHubCommand(hubAction)
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
  
}
