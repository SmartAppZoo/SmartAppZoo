/**
 *  Device Status Monitor
 *
 *  Copyright 2016 Kyle Rupert
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
    name: "Device Status Monitor",
    namespace: "krrupert5",
    author: "Kyle Rupert",
    description: "This app checks to make sure that devices have reported a status change (temperature, battery, etc.) within a certain time period.  This helps to catch devices that have been disconnected from the network and need to be reset.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Temperatures to monitor") {
    	input "temperatures", "capability.temperatureMeasurement", required: false, title: "Temperatures: ", multiple: true
    }
    
    section("Contacts to monitor") {
    	input "contacts", "capability.contactSensor", required: false, title: "Contacts: ", multiple: true
    }
    
    section("Check Every") {
    	input "hours", "enum", required: true, title: "Hours: ", options: [24, 48, 72, 96]
    }
    
    section("Notifications") {
    	input("recipients", "contact", title: "Send notifications to", required: false) {
        	input "sendPushMessage", "enum", title: "Send a Push Notification?", options: ["Yes", "No"], required: false
            input "phone", "phone", title: "Send a Text Message?", required: false
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
    unschedule()
	initialize()
}

def initialize() {   
	// actual hours 
    switch(hours) {
    	case "24":
        	schedule("0 0 0 1/1 * ? *", handlerMethod)
            break
        case "48":
        	schedule("0 0 0 1/2 * ? *", handlerMethod)
            break
        case "72":
        	schedule("0 0 0 1/3 * ? *", handlerMethod)
            break
        case "96":
        	schedule("0 0 0 1/4 * ? *", handlerMethod)
            break
    }
}

def handlerMethod() {  
    def hourInt = 24

    switch(hours) {
    	case "24":
        	hourInt = 24
            break
        case "48":
        	hourInt = 48
            break
        case "72":
        	hourInt = 72
            break
        case "96":
        	hourInt = 96
            break
    }
        
    temperatures.each { temp ->
    	checkDevice(temp, "temperature", hourInt)
    }
    
    contacts.each { contact ->
    	checkDevice(contact, "contact", hourInt)
    }
}

private checkDevice(device, checkState, hourInt)
{
    def state  = device.currentState(checkState)
    def time = state.date.time
    def elapsedTime = now() - time
    def thresholdTime = hourInt * 1000 // ms = hours * 60 (min/hours) * 60 (sec/min) * 1000 (ms/sec) 

    log.debug "Device Status Check - Device: $device , Elapsed Time: $elapsedTime , Threshold: $thresholdTime"

    if (elapsedTime >= thresholdTime) {
        // Device has stopped responding
        def message = "$device Stopped Responding, Reset Device"
        send(message)
    }
}

private send(msg) {
	if ( sendPushMessage == "Yes" ) {
    	log.debug("sending push message")
        sendPush (msg)
    }
    
    if ( phone ) {
    	log.debug("sending text message to $phone")
        sendSms(phone, msg)
    }
    
    log.debug msg
}
