/**
 *  SC Smart Charger
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
 *  Version 1.1 - added enable/disable switch
 *  Version 1.2 - added delay, there are instance when start to charge, power was below threshold & outlet get turn off
 *				  this update will wait for 1 mins & if the power go above the threshold, it will cancel the turn off scheduler
 *  Version 1.21 - output the current voltage to log
 */
definition(
    name: "Smart Charger",
    namespace: "ScSmartCharger",
    author: "SoonChye",
    description: "Get notified & turn-off power outlet when your battery is fully charged",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Outlet...", required: true, multiple: false, description: null)
        //input(name: "aboveThreshold", type: "number", title: "Reports Above...", required: true, description: "in either watts or kw.")
        input(name: "belowThreshold", type: "text", title: "Energy Consumption Falls Below", required: true, description: "in W (watts).")
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
     section( "Enabled/Disabled" ) {
        input "enabled","bool", title: "Enabled?", required: true, defaultValue: true
     }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
    if (settings.enabled == true) {
		initialize()
    }
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    if (settings.enabled == true) {
		initialize()
    }
}

def initialize() {
	if (settings.enabled == true) {
		subscribe(meter, "power", meterHandler)
    }
}

def meterHandler(evt) {

    def meterValue = evt.value as double

    if (!atomicState.lastValue) {
    	atomicState.lastValue = meterValue
    }

    def lastValue = atomicState.lastValue as double
    atomicState.lastValue = meterValue

    def belowThresholdValue = belowThreshold as double
    if (meterValue < belowThresholdValue) {
    	if (lastValue > belowThresholdValue) { // only send notifications when crossing the threshold
		    def msg = "Smart Charger: ${meter} reported ${evt.value} W which is below your threshold of ${belowThreshold} W. Turning off in 1 mins."
    	    sendMessage(msg)
            //outlet.off()
            runIn(1*60, scheduledHandler)
        } else {
//        	log.debug "not sending notification for ${evt.description} because the threshold (${belowThreshold}) has already been crossed"
        }
    } else {
    	log.debug "${meter} - charging in progress (${meterValue} W). Cancelling turn off scheduler (if any)."
        unschedule(scheduledHandler)
    }
}

def scheduledHandler() {
	log.debug "Smart Charger: scheduledHandler executed at ${new Date()}"    
	def msg = "Smart Charger: Turning off the ${meter}!"
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
