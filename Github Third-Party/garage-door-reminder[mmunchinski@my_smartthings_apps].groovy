/**
 *  Garage Door Reminder v1.1 (1/12/2017)
 *
 *  Copyright 2016 Matt Munchinski
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
 *	Changelog
 *	1.0 (12/29/2016) - Initial Release
 *	1.1 (1/12/2017) - Added logic to subscribe to open/close contact sensor events so notify mechanism is not entirely dependent on presence sensors
 *
 *
 */
definition(
    name: "Garage Door Reminder",
    namespace: "mmunchinski",
    author: "Matt Munchinski",
    description: "Reminder that garage door is open based on presence sensor status.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section ("Version 1.1 1/12/2017") { }
	section("Select Presence Sensors") {
		input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: true
	}
    section("Select Contact Sensors") {
		input "contactSensors", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: true
	}

    section("Notifications") { 
		input "sendPush", "bool", title: "Push notification", required: false, defaultValue: "true"
        input "phone", "phone", title: "Phone number", required: false
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
	subscribe(presenceSensors, "presence.not present", presenceHandler)
    subscribe(contactSensors, "contact.open", presenceHandler)
}

def presenceHandler(evt) {
    // Get total number of presence and contact sensors in use
    def totalPresenceSensors = presenceSensors.size()
    def totalContactSensors = contactSensors.size()
    
    // Returns current state of presence and contact sensors
    def currPresenceSensors = presenceSensors.currentPresence
    def currContactSensors = contactSensors.currentContact
    
    // Find all presence sensors currently not present
    def awayPresenceSensors = currPresenceSensors.findAll { currentPresence -> currentPresence == "not present" ? true : false }
    
    // Find all contact sensors currently open
    def openContactSensors = currContactSensors.findAll { currentContact -> currentContact == "open" ? true : false }
    
    // Get total number of away presence sensors and open contact sensors
    def awaySize = awayPresenceSensors.size()
    def openSize = openContactSensors.size()
	
    // For debugging sensor states
    log.debug "Presence Sensor States: $currPresenceSensors"
    log.debug "Contact Sensor States: $currContactSensors"
    log.debug "Presence Sensor State:  Total Sensors=$totalPresenceSensors, Total Away=$awaySize"
    log.debug "Contact Sensor State:  Total Sensors=$totalContactSensors, Total Open=$openSize"
    
    if (awaySize == totalPresenceSensors) {
    	if (openSize!=0) {
        	// If door is left open send notification
            log.debug "Door Open"
            notificationHandler("Garage Door Is Open")
        }
        else {
        	// If door is closed send notification, uncomment for debugging
            log.debug "Door Closed"
            // notificationHandler("Garage Door Is Closed")
        }
	}
    else {
     	log.debug "Someone still home"
	}
}

def notificationHandler(toSend) { 
    def message = toSend 
    if (sendPush) {
        sendPush(message)
    }
    if (phone) {
        sendSms(phone, message)
    }
}