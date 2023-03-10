/**
 *  SC - No Standby
 *  Usage scenario: Fan connected to power outlet but didn't on, so must as well off the outlet after sometime
 *  User define say 5 mins then turn off if the fan not on. So when get trigger, it wait for 5 mins then turn the power off! 
 * 
 *  Copyright 2016 SoonChye
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
    name: "No Standby",
    namespace: "ScNoStandby",
    author: "SoonChye",
    description: "Get notified & turn-off power outlet when device is left in standby mode after a period of time",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Outlet...", required: true, multiple: false, description: null)
        input(name: "executeTime", type: "number", title: "Time (in min) before turning off power outlet:")
        input(name: "belowThreshold", type: "number", title: "Energy Consumption Falls Below", required: true, description: "in W (watts).")
	}
    section {
        input("recipients", "contact", title: "Send notifications to") {
        input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
        input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
        }
    }
	section{
    	input(name: "outlet", type: "capability.switch", title: "Turn Off This Power Outlet...", required: true, multiple: false)
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
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {

    def meterValue = evt.value as double
	log.debug "DS - meterValue: ${meterValue}"

    if (!state.lastValue) {
    	state.lastValue = meterValue
    }

    state.lastValue = meterValue

    def belowThresholdValue = belowThreshold as double
    def executeTimeValue = executeTime as int
    
    if (meterValue < belowThresholdValue) {
		    def msg = "${meter} reported ${evt.value} W which is below your threshold of ${belowThreshold} W"
            log.debug "DS - ${msg}"
         
            if (meterValue != 0.0){
            	log.debug "DS - set a ${executeTimeValue} min scheduler to power off the ${meter}"
				runIn(executeTimeValue*60, scheduledHandler)
            }
	} else {
			log.debug "DS - Power not below threshold / ${meter} turned on. Cancelling schedule (if any)."
            unschedule(scheduledHandler)
	}
}

def scheduledHandler() {
	log.debug "DS - scheduledHandler executed at ${new Date()}"    
	def msg = "Turning off the ${meter}!"
	log.debug "DS - ${msg}"
    sendMessage(msg)
    outlet.off()
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
