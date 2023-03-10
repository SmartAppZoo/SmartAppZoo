/**
 *  Powerwall Alert
 *
 *  Copyright 2018 Robin Shipston
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
    name: "Powerwall Alert",
    namespace: "RobinShipston",
    author: "Robin Shipston",
    description: "Notifies of Powerwall II charge levels",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section {
		input(name: "battery", type: "capability.battery", title: "When This Battery...", required: true, multiple: false, description: null)
        input(name: "aboveThreshold", type: "number", title: "Reports Above...", required: true, description: "in %")
        input(name: "belowThreshold", type: "number", title: "Or Reports Below...", required: true, description: "in %")
	}
    section {
        input("recipients", "contact", title: "Send notifications to") {
            input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
            input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
        }
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
	subscribe(battery, "battery", batteryHandler)
}

// TODO: implement event handlers
def batteryHandler(evt) {

    def batteryValue = evt.value as double
	log.debug "BatteryValue = ${batteryValue}"
    if (!atomicState.lastValue) {
    	atomicState.lastValue = batteryValue
    }

    def lastValue = atomicState.lastValue as double
    atomicState.lastValue = batteryValue

    def dUnit = evt.unit ?: "%"

    def aboveThresholdValue = aboveThreshold as int
    if (batteryValue > aboveThresholdValue) {
    	if (lastValue < aboveThresholdValue) { // only send notifications when crossing the threshold
		    def msg = "${battery} has risen to ${Math.round(batteryValue)} ${dUnit} charge."
    	    sendMessage(msg)
        } else {
//        	log.debug "not sending notification for ${evt.description} because the threshold (${aboveThreshold}) has already been crossed"
        }
    }


    def belowThresholdValue = belowThreshold as int
    if (batteryValue < belowThresholdValue) {
    	if (lastValue > belowThresholdValue) { // only send notifications when crossing the threshold
		    def msg = "${battery} has fallen to ${Math.round(batteryValue)} ${dUnit} charge."
    	    sendMessage(msg)
        } else {
//        	log.debug "not sending notification for ${evt.description} because the threshold (${belowThreshold}) has already been crossed"
        }
    }
}

def sendMessage(msg) {
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sms) {
            sendSms(sms, msg)
        }
        if (pushNotification) {
            sendPush(msg)
        }
    }
}
