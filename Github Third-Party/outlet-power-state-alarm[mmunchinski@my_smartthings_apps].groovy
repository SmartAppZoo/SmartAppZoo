/**
 *  Outlet Power State Alarm
 *
 *  Copyright 2018 Matt Munchinski
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
    name: "Outlet Power State Alarm",
    namespace: "mmunchinski",
    author: "Matt Munchinski",
    description: "Sends alert when power consumption of outlet drops below certain criteria.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section ("Version 1.0 4/15/2018") { }
	section("Select Outlets") {
		input "powerMeters", "capability.powerMeter", title: "Outlers", multiple: true, required: true
        input(name: "belowThreshold", type: "number", title: "Low Threshold", required: true, description: "In Watts")
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
	subscribe(powerMeters, "power", meterHandler)   
}

def meterHandler(evt) {
	
    log.debug evt.value
    
    def meterValue = evt.value as double
    
    if (!atomicState.lastValue) {
    	atomicState.lastValue = meterValue
    }

    def lastValue = atomicState.lastValue as double
    atomicState.lastValue = meterValue

    def dUnit = evt.unit ?: "Watts"

    def belowThresholdValue = belowThreshold as int
    if (meterValue < belowThresholdValue) {
    	if (lastValue > belowThresholdValue) { // only send notifications when crossing the threshold
		    def msg = "${meter} reported ${evt.value} ${dUnit} which is below your threshold of ${belowThreshold}."
    	    sendMessage(msg)
        } else {
        }
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