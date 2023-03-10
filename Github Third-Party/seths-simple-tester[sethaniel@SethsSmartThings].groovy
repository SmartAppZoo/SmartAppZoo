/**
 *  Seths Simple Tester
 *
 *  Copyright 2016 Seth Munroe
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
    name: "Seths Simple Tester",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "This is used for testing code snippets",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Devices") {
		input(name: "simulatedSwitch", type: "capability.switch", required: true, title: "Select the simulated switch that will be used for testing.")
	}
    section("Notifications") {
    	paragraph("Notifications can be sent to specific users by SMS or to everyone by SmartThings App Push notifications.", title: "SMS/Push Notifications:", required: true)
        input(name: "recipients", type: "contact", title: "Send notifications to:", multiple: true) {
            input(name: "inPhone", type: "phone", title: "Send SMS notifications to: (optional: comma separated list of phone numbers)",
                description: "Phone Number", required: false, multiple: true)
        }
        input(name: "notifyPush", type: "bool", title: "Send notifications to all logged in devices for all users of location: ${location.name}?", defaultValue: false)
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
    subscribe(simulatedSwitch, "switch", simulatedSwitchHandler)
}

def simulatedSwitchHandler(evt) {
	log.debug "GMT: ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy', TimeZone.getTimeZone('GMT'))}"
	log.debug "PST: ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy', location.timeZone)}"
	log.debug "${evt.stringValue.toUpperCase()} : ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy')} : ${evt.displayName} : ${evt.descriptionText}. This happened at ${evt.date.format('HH:mm:ss.SSS zzzz')} on ${evt.date.format('EEE, MMM dd, yyyy')}."
    
    sendNotifications("${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss.SSSZ, EEE, MM-dd-yyyy',location.timeZone)}", recipients, inPhone, notifyPush)
}

def sendNotifications(message, recipContacts, recipPhones, sendPushToEveryone) {
    if (location.contactBookEnabled && recipContacts) {
        log.debug "contact book enabled!"
        sendNotificationToContacts(message, recipients)
    } else {
        log.debug "contact book not enabled"
        if (recipPhones) {
            if (recipPhones instanceof java.util.List) {
                recipPhones.each {onePhone ->
                    sendSms(onePhone, message)
                    log.debug "sendSms('${onePhone}', '${message}')"
                }
            } else {
                sendSms(recipPhones, message)
                log.debug "sendSms('${recipPhones}', '${message}')"
            }
        }
    }
    
    if (sendPushToEveryone) {
    	sendPush(message)
        log.debug "sendPush('${message}')"
    }
}