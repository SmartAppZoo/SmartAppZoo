/**
 *  Presence Sensor
 *
 *  Copyright 2019 Matt
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

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
    name: "Presence Sensor",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitors the presence sensors on the network and makes changes and/or sends notifications based on their status.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Family/App-PhoneMinder.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-PhoneMinder@2x.png") {
    appSetting "notification_sensors"
    appSetting "notification_recipients"
}

preferences {
	section("Choose the presence sensor(s) you'd like to monitor.") {
		input "sensors", "capability.presenceSensor", required: true, multiple: true, title: "Which sensor(s) to monitor?"
	}
    section("Choose the thermostat to change when appropriate.") {
        input "thermostat", "device.myEcobeeDevice", required: true, multiple: false, title: "Which thermostat?"
    }
    section("Choose the switches you'd like to turn ON when all sensor(s) have left and OFF when one or more sensor has arrived.") {
    	input "switches", "capability.switch", required: false, multiple: true, title: "Which switches?"
    }
}

mappings {
    path("/current_presence") {
        action: [
            GET: "getCurrentPresenceViaOAuth"
        ]
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(sensors, "presence", presenceChangeHandler)
    setCurrentPresence()
}

def updated() {
    unsubscribe()
    subscribe(sensors, "presence", presenceChangeHandler)
    setCurrentPresence()
}

def presenceChangeHandler(evt) { 
    // Get the current presence for all sensors
    def current_presence = getCurrentPresence(false)
    
    // Only perform an action if the stored presence state does not match the event's presence state for this sensor
    if (current_presence[evt.device.getLabel()] != evt.value) {
        def parser = new JsonSlurper()
        def tracking_list = parser.parseText(appSettings.notification_sensors)
        if (tracking_list.contains(evt.device.getLabel())) {
            // This device is being tracked. Send a notification and update the stored presence setting
            def notification_list = parser.parseText(appSettings.notification_recipients)
            switch(evt.value) {
                case "present":
                    notification_list.each { phone_number ->
                    	sendSms(phone_number, "${evt.device.getLabel()} has arrived home.")
                    }
                    break
                case "not present":
                    notification_list.each { phone_number ->
                    	sendSms(phone_number, "${evt.device.getLabel()} has left home.")
                    }
                    break
            }
            // Update the stored presence setting for this sensor
            triggerPresenceChangeAction(evt)
        } else {
            // Update the stored presence setting for this sensor
            triggerPresenceChangeAction(evt)
        }
    }
}

def triggerPresenceChangeAction(evt) {
    // Update the current presence for all sensors
    setCurrentPresence()
    
    // Get all currently present sensors
    def current_presence = getCurrentPresence(true)
    sendNotificationEvent("[PRESENCE] Current presence: ${current_presence}")
    
    if (current_presence.size() == 0) {
    	// All sensors have left the network. Perform any desired actions here.
        
        // Set the thermostat to "Away and holding" which will hold until the next scheduled activity.
        // NOTE: Make sure the "holdType" preference in the ecobee device settings is set to "nextTransition"
        // Only do this if the current system location setting is not set to "Away", which means we are on vacation and these rules are overridden.
        if (location.currentMode.toString() != "Away") {
            sendNotificationEvent("[PRESENCE] ACTION: Setting the thermostat to Away mode.")
            try {
                thermostat.setThisTstatClimate("Away")

                // Don't send a notification if the last notification was less than 30 seconds ago (e.g. when multiple events fire at once)
                def current_timestamp = new Date().getTime() / 1000
                if (atomicState.sms_timestamp == null || (current_timestamp - atomicState.sms_timestamp) > 30) {
                    atomicState.sms_timestamp = current_timestamp

                    def parser = new JsonSlurper()
                    def notification_list = parser.parseText(appSettings.notification_recipients)
                    notification_list.each { phone_number ->
                        sendSms(phone_number, "All sensors have left the network. Setting the thermostat to Away mode.")
                    }
                }
            } catch(e) {
                sendNotificationEvent("[PRESENCE] ERROR: ${e}")
            }
            
            // Turn all of the switche(s) selected for this scene ON while all sensors are away.
            switches.each { this_switch ->
            	sendNotificationEvent("[PRESENCE] Turning ${this_switch.device.getLabel()} ON.")
            	this_switch.on()
            }
        }
    } else {
    	// At least one sensor is in the network. Perform any desired actions here.
    	// Check to see whether the Ecobee is in "Away and holding" or "Home and holding" mode.
        def program_type = thermostat.currentValue("programType").toString()
        sendNotificationEvent("[PRESENCE] Current program type: ${program_type}")
        if (program_type == "hold") {
            // The thermostat is in "Away and holding" or "Home and holding" mode. Resume its normal programming.
            sendNotificationEvent("[PRESENCE] ACTION: Thermostat is resuming its normal program.")
            try {
                thermostat.resumeProgram()
            } catch(e) {
                sendNotificationEvent("[PRESENCE] ERROR: ${e}")
            }
        }
        // Turn all of the switche(s) selected for this scene OFF while at least one sensor is in the network.
        switches.each { this_switch ->
            sendNotificationEvent("[PRESENCE] Turning ${this_switch.device.getLabel()} OFF.")
            this_switch.off()
        }
    }
}

def setCurrentPresence() {
    def current_presence = [:]
    def generator = new JsonOutput()
    sensors.each { object ->
        def label = object.getLabel()
        def value = object.currentValue("presence")
        current_presence[label] = value
    }
    
    def current_presence_json = generator.toJson(current_presence)
    // Use atomicState so that the values can be saved to the database and used within the same runtime
    atomicState.current_presence = current_presence_json
}

def getCurrentPresence(present_only=false) {
	def parser = new JsonSlurper()
    def current_presence = parser.parseText(atomicState.current_presence)
    // If the present_only flag passed in is TRUE, only return the sensors in the object that are currently present
    if (present_only == true) {
        def presence_only = [:]
        current_presence.each { label, value ->
            if (value == "present") {
                presence_only[label] = value
            }
        }
        return presence_only
    }
    // Otherwise, return the full object of all sensors and their current status
    return current_presence
}

def getCurrentPresenceViaOAuth() {
    // Send the response using text/plain as opposed to the default (text/json) since it appears this version of Groovy does not handle httpGet() calls with JSON responses well
    render contentType: "text/plain", data: atomicState.current_presence, status: 200
}