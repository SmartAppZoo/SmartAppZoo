/**
 *  PowerFeedAlert
 *
 *  Copyright 2016 Michael Beynon
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
 *  Inspired by: https://github.com/gouldner/ST-Projects/blob/master/SmartThings/src/AeonDSC06/Apps/AlertOnPowerDetect.groovy
 *
 */
definition(
    name: "PowerFeedAlert",
    namespace: "mbeynon",
    author: "Michael Beynon",
    description: "This app is intended to monitor a power meter and report if one of several conditions occur, including a power drop below a minimum (can be used to detect a breaker tripping).\r\n\r\nFuture revs will add checking for normal cyclic patterns of power usage over time to catch equipment malfunction.\r\n",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select power meter to monitor...") {
        input name: "checkPowerMeter", type: "capability.powerMeter", title: "PowerMeter", multiple: false
    }
    section("Report power less than...") {
        input name: "powerMinimum", type: "number", title:"PowerMinimum", required: true, defaultValue:2, multiple: false
    }
    section("Only report once every N minutes...") {
        input name: "reportEveryMinutes", type: "number", title:"Minutes", required: true, defaultValue:30, multiple: false
    }
    section("Who should be alerted when anomalies are detected?") {
        input("recipients", "contact", title: "People to notify", description: "Send notifications to") {
            input "notifyPhone1", "phone", title: "Phone number?", required: false
            input "notifyPhone2", "phone", title: "Phone number?", required: false
            input "notifyPhone3", "phone", title: "Phone number?", required: false
        }
    }
    section("Debugging...") {
        input name: "debugOutput", type: "boolean", title: "Enable debug output?", defaultValue: false
    }
}

def logDebug(message) {
    if (state.debugOutput) {
        log.debug "dbg: ${message}"
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
    log.info "PowerFeedAlert initialize(): meter=${checkPowerMeter}, alert if power < ${powerMinimum} W"
	subscribe(checkPowerMeter, "power", powerCheck)
	// Last reported time (in minutes since epoch)
	state.reportedTime = 0
    // Number of consecutive readings before notification is sent
    state.powerMinimumCount = 0
    state.debugOutput = (debugOutput == "true")
}

def powerCheck(evt) {
    def meterValue = evt.value as double
    def minValue = powerMinimum as double

	if (meterValue < minValue) {
    	state.powerMinimumCount = state.powerMinimumCount + 1
        if (state.powerMinimumCount > 0) {
	        log.info "powerCheck(): found meterValue=${meterValue} < minValue=${minValue} -- possible power outage"
            def msg = "Meter \"${checkPowerMeter}\" reporting too low power usage : ${meterValue} W"
            checkSendPowerNotification(msg)
        } else {
        	logDebug("powerCheck(): accumulating < min readings (count=${state.powerMinimumCount})")
        }
    } else {
	    logDebug("powerCheck(): skip good meterValue=${meterValue} >= minValue=${minValue}")
    	// have a good value, reset count and reset last reported time to force at least one report per outage
    	state.powerMinimumCount = 0
		state.reportedTime = 0
    }
}

def checkSendPowerNotification(msg) {
    def reportedTime = state.reportedTime as int
    def now = new Date();
    def nowMinutes = Math.round(now.getTime() / 60000);
        
    if ((nowMinutes - reportedTime) > reportEveryMinutes) {
    	log.info "checkSendPowerNotification(): send alert notification: ${msg}"
		// send SMS and/or push notifications
		if (notifyPhone1) {
            logDebug("sendNotification(): sending sms to ${notifyPhone1} msg=${msg}")
            sendSms(notifyPhone1, msg)
        }
        if (notifyPhone2) {
            logDebug("sendNotification(): sending sms to ${notifyPhone2} msg=${msg}")
            sendSms(notifyPhone2, msg)
        }
        if (notifyPhone3) {
            logDebug("sendNotification(): sending sms to ${notifyPhone3} msg=${msg}")
            sendSms(notifyPhone3, msg)
        }
        logDebug("sendNotification(): sending push msg=${msg}")
        sendPush(msg)

		state.reportedTime = nowMinutes
    } else {
        logDebug("checkSendPowerNotification(): skip redundant notify until ${reportEveryMinutes} minutes have passed")
    }
}
