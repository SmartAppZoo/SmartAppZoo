/**
 *  Motion Efficiency
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
    name: "Motion Efficiency",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitor Ecobee remote motion sensors to increase energy efficiency.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/motion_detected.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/motion_detected@2x.png") {
    appSetting "notification_motions"
    appSetting "notification_recipients"
    appSetting "oauth_endpoint_url"
    appSetting "oauth_endpoint_path"
    appSetting "oauth_access_token"
}

preferences {
	section("Choose the motion sensors you'd like to monitor.") {
		input "motions", "capability.motionSensor", multiple: true, required: true, title: "Motion sensor(s)"
	}
    section("Choose the thermostat to change when appropriate.") {
        input "thermostat", "device.myEcobeeDevice", required: true, multiple: false, title: "Which thermostat?"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(motions, "motion", motionChangeHandler)
    setCurrentMotions()
}

def updated() {
    unsubscribe()
    subscribe(motions, "motion", motionChangeHandler)
    setCurrentMotions()
}

// Ecobee sensors reset to "inactive" after approximately 30 minutes of no motion detected.
def motionChangeHandler(evt) {
    // Rewrite the entire object each time an event occurs since it appears the Ecobee device handler misses motion change events from time to time.
    setCurrentMotions()

    // Retrieve a list of motion sensors currently detecting motion.
    def current_motions = getCurrentMotions(true)
    sendNotificationEvent("[MOTION] Current motion: ${current_motions}")
        
    def parser = new JsonSlurper()
    
    // If the sensor is no longer detecting motion, take certain actions here.
    if (evt.value == "inactive") {
        def tracking_list = parser.parseText(appSettings.notification_motions)
        // We only watch certain sensors in order to try and save energy in certain rooms.
        if (tracking_list.contains(evt.device.getLabel())) {
        	def label = evt.device.getLabel().toString().replace("(Ecobee) ", "")
            // Send a notification, but only between the hours of 8AM and 11PM
            def df = new java.text.SimpleDateFormat("H")
            // Ensure the new date object is set to local time zone
            df.setTimeZone(location.timeZone)
            def hour = df.format(new Date())
            
            if (hour.toInteger() >= 8 && hour.toInteger() <= 23) {
            	// Disabling this for now until switches can be integrated and this can be automated
                //sendPush("${label} is no longer detecting motion. Make sure the light is turned off.")
            }
        }
        
        // If all sensors on the network are no longer tracking motion, take certain actions here.
        if (current_motions.size() == 0) {
    		// Check to see whether the Ecobee is in "Home" or "Home and holding" mode.
    		def set_climate = thermostat.currentValue("setClimate").toString()
            def program_type = thermostat.currentValue("programType").toString()
            
            sendNotificationEvent("[MOTION] Set climate: ${set_climate}, program type: ${program_type}")
            
            // Do not take action if:
            // Set climate is "Sleep"
            // Set climate is "Away and holding"
            if (set_climate != "Sleep" && (program_type != "hold" || set_climate != "Away")) {
                // Set the thermostat to "Away and holding", which will hold until motion is detected at a sensor or a presence sensor enters the network.
                // Only do this if the current system location setting is not set to "Away", which means we are on vacation and these rules are overridden.
                // NOTE: If we want the holds to expire at the next scheduled activity, make sure the "holdType" preference in the ecobee device settings is set to "nextTransition"
                if (location.currentMode.toString() != "Away") {
                    sendNotificationEvent("[MOTION] ACTION: Thermostat going into Away mode.")
                    
                    try {
                        thermostat.setThisTstatClimate("Away")
						
                       	// Send a notification alerting to this change
                        // Don't send a notification if the last notification was less than 30 seconds ago (e.g. when multiple events fire at once)
                        def current_timestamp = new Date().getTime() / 1000
                        if (atomicState.sms_timestamp == null || (current_timestamp - atomicState.sms_timestamp) > 30) {
                            atomicState.sms_timestamp = current_timestamp
                            
                            parser = new JsonSlurper()
                            def notification_list = parser.parseText(appSettings.notification_recipients)
                            notification_list.each { phone_number ->
                                sendSms(phone_number, "All motion sensors are idle. Thermostat is going into Away mode.")
                            }
                        }
                    } catch(e) {
                    	sendNotificationEvent("[MOTION] ERROR: ${e}")
                    }
                }
            }
        }
    // If the sensor has detected motion, take certain actions here.
    } else if (evt.value == "active") {
    	// Check to see whether the Ecobee is in "Away and holding" mode.
    	def set_climate = thermostat.currentValue("setClimate").toString()

        // Retrieve a list of currently present sensors. (Perhaps these apps ran out of order?)
        def current_presence = getCurrentPresence(true)
        
        sendNotificationEvent("[MOTION] Current presence: ${current_presence}")
        sendNotificationEvent("[MOTION] Current climate: ${set_climate}")
        
        // Don't take any action if the current Mode is set to "Ignore Motion"
        if (location.mode != "Ignore Motion") {
            if (set_climate == "Away") {
                if (current_presence.size() > 0) {
                    // If the Ecobee is set to "Away" or "Away and holding" and a presence sensor has been detected, let's just return the Ecobee to normal programming.
                    sendNotificationEvent("[MOTION] ACTION: Thermostat is resuming its normal program.")
                    try {
                        thermostat.resumeProgram()
                    } catch(e) {
                        sendNotificationEvent("[MOTION] ERROR: ${e}")
                    }
                } else {
                    // If the Ecobee is set to "Away" or "Away and holding" and only motion is detected, someone is at the house but it's not us. Temporarily set the Ecobee to "Home and holding".
                    // Only do this if the current system location setting is not set to "Away", which means we are on vacation and these rules are overridden.
                    if (location.currentMode.toString() != "Away") {
                        // Set the thermostat to "Home and holding", which will hold until all sensors are inactive or a presence sensor is detected.
                        // NOTE: If we want the holds to expire at the next scheduled activity, make sure the "holdType" preference in the ecobee device settings is set to "nextTransition"
                        sendNotificationEvent("[MOTION] ACTION: Thermostat going into Home mode.")
                        try {
                            try {
                                thermostat.setThisTstatClimate("Home")
                            } catch(e) {
                                sendNotificationEvent("[MOTION] ERROR: ${e}")
                            }

                            // Don't send a notification if the last notification was less than 30 seconds ago (e.g. when multiple events fire at once)
                            def current_timestamp = new Date().getTime() / 1000
                            if (atomicState.sms_timestamp == null || (current_timestamp - atomicState.sms_timestamp) > 30) {
                                atomicState.sms_timestamp = current_timestamp

                                parser = new JsonSlurper()
                                def notification_list = parser.parseText(appSettings.notification_recipients)
                                notification_list.each { phone_number ->
                                    sendSms(phone_number, "Motion has been detected at home. Thermostat is going into Home mode.")
                                }
                            }
                        } catch(e) {
                            sendNotificationEvent("[MOTION] ERROR: ${e}")
                        }
                    }
                }
            }
        } else {
            sendNotificationEvent("[MOTION] Thermostat is not taking any action due to the Ignore Motion mode being set.")
        }
    }
}

def setCurrentMotions() {
    def current_motions = [:]
    def generator = new JsonOutput()
    motions.each { object ->
        def label = object.getLabel()
        def value = object.currentValue("motion")
        current_motions[label] = value
    }

    def current_motions_json = generator.toJson(current_motions)
    // Use atomicState so that the values can be saved to the database and used within the same runtime
    atomicState.current_motions = current_motions_json
}

def getCurrentMotions(motion_only=false) {
	def parser = new JsonSlurper()
    def current_motions = parser.parseText(atomicState.current_motions)
    // If the motion_only flag passed in is TRUE, only return the sensors in the object that are currently detecting motion
    if (motion_only == true) {
        def motions_only = [:]
        current_motions.each { label, value ->
            if (value == "active") {
                motions_only[label] = value
            }
        }
        return motions_only
    }
    // Otherwise, return the full object of all sensors and their current status
    return current_motions
}

def getCurrentPresence(present_only=false) {
    // For details on setting up oAuth, see: 
    // https://docs.smartthings.com/en/latest/smartapp-web-services-developers-guide/authorization.html
    def params = [
        uri: appSettings.oauth_endpoint_url,
        path: appSettings.oauth_endpoint_path,
        headers: [
        	"Authorization": "Bearer ${appSettings.oauth_access_token}"
        ]
    ]

    try {
        httpGet(params) { resp ->
            def parser = new JsonSlurper()
            // This needs to be parse() instead of parseText() because resp.data is a StringReader object, not a String
            def current_presence = parser.parse(resp.data)
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
    } catch (e) {
        sendNotificationEvent("[MOTION] ERROR: Unable to complete getCurrentPresence() oAuth call: ${e}")
    }
}