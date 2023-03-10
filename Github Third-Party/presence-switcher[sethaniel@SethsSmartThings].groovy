/**
 *  Presence Switcher
 *
 *  Copyright 2018 Seth Munroe
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
    name: "Presence Switcher",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "This helper will use a Simulated Switch to monitor status and control of a presence sensor. This is useful so that it can be accessed by devices that do not control presences sensors (such as IFTTT).",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("The Presence Sensor") {
		input(name: "simulatedPresence", type: "capability.presenceSensor", required: true, title: "Select the simulated presence sensor that will be monitored")
        input(name: "simulatedSwitch", type: "capability.switch", required: true, title: "Select the simulated switch that will monitor and control the presence sensor.")
	}
    section("Notifications") {
    	paragraph("Notifications about the presence sensor being monitored can be sent to specific users by SMS or to everyone by SmartThings App Push notifications.", title: "SMS/Push Notifications:", required: true)
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
    log.debug "initializing"
	subscribe(simulatedPresence, "presence", simulatedPresenceHandler)
    
    subscribe(simulatedSwitch, "switch", simulatedSwitchHandler)
    log.debug "initialized"
}

def simulatedPresenceHandler(evt) {
	logEvent(evt)
    
    if (evt.stringValue == "present") {
    	if (simulatedSwitch.currentValue("switch") != "on") {
            simulatedSwitch.on()
        }
    } else { 
        if (simulatedSwitch.currentValue("switch") != "off") {
    		simulatedSwitch.off()
        }
    }
    
    sendNotifications("${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss Z, EEE, MM-dd-yyyy',location.timeZone)}", recipients, inPhone, notifyPush)
}

def simulatedSwitchHandler(evt) {
	logEvent(evt)
    
    if (evt.stringValue == "on") {
    	if (simulatedPresence.currentValue("presence") != "present") {
            simulatedPresence.arrived()
        }
    } else { 
        if (simulatedPresence.currentValue("presence") != "not present") {
    		simulatedPresence.departed()
        }
    }
}

def logEvent(evt) {
	log.debug "${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy',location.timeZone)}"
    log.debug "event.data [$evt.data]"
    log.debug "event.description [$evt.description]"
    log.debug "event.descriptionText [$evt.descriptionText]"
    log.debug "event.value [$evt.value]"
    log.debug "event.stringValue [$evt.stringValue]"
    log.debug "event.digital [$evt.digital]"
    log.debug "event.physical [$evt.physical]"
    log.debug "event.source [$evt.source]"
    try {
    	log.debug "event.jsonValue [$evt.jsonValue]"
    } catch (ex) {
    	log.debug "event.jsonValue [no valid json value]"
    }
}

def sendNotifications(message, recipContacts, recipPhones, sendPushToEveryone) {
	log.debug "sendNotifications('${message}', '${recipContacts}', '${recipPhones}', ${sendPushToEveryone})"
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