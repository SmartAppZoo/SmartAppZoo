/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Auto Lock Time Delay
 *
 *  Author: Tom Rivera
 *  Date: July 1, 2016
 */

definition(
    name: "Auto Lock Time Delay",
    namespace: "tomriv77",
    author: "Tom Rivera",
    description: "Locks a deadbolt or lever lock after a time delay when the door sensor state is closed.",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health7-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health7-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health7-icn@3x.png",
    oauth: true
)

preferences {
	page (name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
        section("Door/Lock Configuration") {
            input "locationName", type: "text", title: "Location name", required: true
            input "contactSensor", "capability.contactSensor", title: "Select door contact sensor", multiple: false, required: true
            input "lock","capability.lock", title: "Select lock", multiple: false, required: true
            input "closedThresholdInMin", "number", title: "Auto lock time (minutes)", description: "Number of minutes", required: true, defaultValue: "5"
        }
        
        section {
        	input(name: "smokeAlarmOverride", title: "Prevent auto locking if Smoke alarm active?", type: "bool", required: false, defaultValue: false, submitOnChange: true)
        }
        
        if(smokeAlarmOverride != null && smokeAlarmOverride) {
            section("Select Alarm(s)") {
        		input "smokeAlarm", "capability.smokeDetector", title: "Smoke Detector", multiple: true, required: "smokeAlarmOverride"
            }
    	}

        section {
            input(name: "notifyIfLeftOpen", title: "Notify if door left open?", type: "bool", required: false, defaultValue: false, submitOnChange: true)
        }
        
        if(notifyIfLeftOpen != null && notifyIfLeftOpen) {
            section("Notification options"){
                input "openThresholdInMin", "number", title: "Minutes to wait before first notification", description: "Number of minutes", required: false, defaultValue: "10"
                input "frequency", "number", title: "Minutes to wait before resending", description: "Number of minutes", required: false, defaultValue: "10"
                input("recipients", "contact", title: "Send notifications to") {
                    input "phone", "phone", title: "Phone number for SMS", required: false
                }
            }
        }
    }
}

def installed()
{
	log.trace "installed() Auto Lock Time Delay"
    subscribe()
    def startTime = new Date((now() + 120000) as long)
    runOnce(startTime, checkCurrentDeviceStates)
}

def updated()
{
	log.trace "updated() Auto Lock Time Delay"
    unschedule()
	unsubscribe()
    subscribe()
	def startTime = new Date((now() + 120000) as long)
    runOnce(startTime, checkCurrentDeviceStates)
}

def checkCurrentDeviceStates() {
	log.trace "checkCurrentDeviceStates() door status is ${contactSensor.currentState("contact").value}/${lock.currentState("lock").value}"
    
    if(contactSensor.currentState("contact").value == "open") {
        scheduleDoorOpenTooLong(openThresholdInMin)
        log.debug "checkCurrentDeviceStates() scheduled doorOpenTooLong"
    	
    } else if(contactSensor.currentState("contact").value == "closed") {
        scheduleDoorUnlockedTooLong(closedThresholdInMin)
        log.debug "checkCurrentDeviceStates() scheduled doorUnlockedTooLong"
    }
}

def subscribe() {
    subscribe(contactSensor, "contact", doorEventHandler)
    subscribe(lock, "lock", lockEventHandler)
    subscribe(smokeAlarm, "smoke", smokeAlarmEventHandler)
}

def smokeAlarmEventHandler(evt) {
}

boolean checkForSmoke() {
	def result = false
	if(smokeAlarm != null) {
        smokeAlarm.each { 
            if(it.currentState("smoke").value == "detected") {
                log.debug "${it.displayName} detected smoke"
                result = true
            }
        }
    }
    return result
}

def lockEventHandler(evt) {
	log.debug "lockEventHandler($evt.name: $evt.value), door status is ${contactSensor.currentState("contact").value}"
    
    if(evt.name == "lock") {
    	if(evt.value == "locked") {
        	if(contactSensor.currentState("contact").value == "closed") {
            	log.trace "lockEventHandler() door closed/locked"
            } else if(contactSensor.currentState("contact").value == "open") {
            	log.warn "lockEventHandler() door open/locked"
            } else {
            	log.warn "lockEventHandler() invalid door state: ${contactSensor.currentState("contact").value}"
            }
        	
        } else if (evt.value == "unlocked") {
        	if(contactSensor.currentState("contact").value == "closed") {
            	log.trace "lockEventHandler() door closed/unlocked"
                scheduleDoorUnlockedTooLong(closedThresholdInMin)
				log.debug "lockEventHandler() scheduled doorUnlockedTooLong"
            } else if(contactSensor.currentState("contact").value == "open") {
            	log.warn "lockEventHandler() door open/unlocked"
            } else {
            	log.warn "lockEventHandler() invalid door state: ${contactSensor.currentState("contact").value}"
            }
        } else {
        	log.warn "lockEventHandler($evt.name: $evt.value) invalid event!!!"
        }
    }
}

def doorEventHandler(evt) {
    log.debug "doorEventHandler($evt.name: $evt.value), lock status is ${lock.currentState("lock").value}"
    
    if(evt.name == "contact") {
    	if(evt.value == "open") {
        	if(notifyIfLeftOpen != null && notifyIfLeftOpen) {
        		scheduleDoorOpenTooLong(openThresholdInMin)
				log.debug "doorEventHandler() scheduled doorOpenTooLong"
            }
        } else if(evt.value == "closed") {
        	scheduleDoorUnlockedTooLong(closedThresholdInMin)
			log.debug "doorEventHandler() scheduled doorUnlockedTooLong"
            
        } else {
        	log.warn "doorEventHandler($evt.name: $evt.value) invalid event!!!"
        }
    } else {
    	log.warn "doorEventHandler($evt.name: $evt.value) invalid event!!!"
    }
}

/*
*/
def doorOpenTooLong() {
	def contactState = contactSensor.currentState("contact")
    def lockState = lock.currentState("lock")
    log.debug "doorOpenTooLong() door status is ${contactState.value}/${lockState.value}"

	if (contactState.value == "open" && lockState.value == "unlocked") {
    	def timeRemainingInSec = getTimeRemainingInSec(openThresholdInMin, contactState.rawDateCreated.time)
		if (timeRemainingInSec <= 0) {
        	def elapsedInMin = convertSecToMin(timeRemainingInSec * (-1)) + openThresholdInMin
			log.debug "Door open timer expired (${elapsedInMin} min):  calling sendMessage()"
			sendMessage(elapsedInMin)
            def freqInSec = getMsgResendFrequencyInSec()
            runIn(freqInSec, doorOpenTooLong, [overwrite: false])
            log.debug "doorOpenTooLong() fires again in ${convertSecToMin(freqInSec)} min"
		} else {
			log.debug "Door open timer not yet expired (${convertMinToSec(openThresholdInMin) - timeRemainingInSec} sec):  doing nothing"
            runIn(timeRemainingInSec, doorOpenTooLong, [overwrite: false])
            if(timeRemainingInSec > 60) {
            	log.debug "doorOpenTooLong() fires again in ${timeRemainingInSec} sec"
            } else {
            	log.debug "doorOpenTooLong() fires again in ${convertSecToMin(timeRemainingInSec)} min"
            }
		}
    } else if (contactState.value == "open" && lockState.value == "locked") {
    	sendPush("Door registers as open but is locked, probable false alarm please verify")
	} else {
		log.warn "doorOpenTooLong() called but contactSensor is closed:  doing nothing"
	}
}

def doorUnlockedTooLong() {
	def contactState = contactSensor.currentState("contact")
    def lockState = lock.currentState("lock")
    log.debug "doorUnlockedTooLong() door status is ${contactState.value}/${lockState.value}"

	if(smokeAlarmOverride != null && smokeAlarmOverride == true && checkForSmoke() == true) {
    	log.trace "doorUnlockedTooLong() smoke alarm(s) active, delaying auto lock 30 min"
        sendPush("doorUnlockedTooLong() smoke alarm(s) active, delaying auto lock 30 min")
        runIn(1800, doorOpenTooLong, [overwrite: false])
	} else if (contactState.value == "closed" && lockState.value == "unlocked") {
    	def timeRemainingInSec = getTimeRemainingInSec(closedThresholdInMin, contactState.rawDateCreated.time)
		if (timeRemainingInSec <= 0) {
        	log.debug "doorUnlockedTooLong() Door closed and lock timer expired (${closedThresholdInMin} min):  engaging lock"
			lock.lock()
		} else {
			log.debug "doorUnlockedTooLong() Door closed but lock timer not yet expired ($elapsed ms):  doing nothing"
            runIn(timeRemainingInSec, doorUnlockedTooLong, [overwrite: false])
            if(timeRemainingInSec > 60) {
            	log.debug "doorUnlockedTooLong() fires again in ${convertSecToMin(timeRemainingInSec)} min"
            } else {
            	log.debug "doorUnlockedTooLong() fires again in ${timeRemainingInSec} sec"
            }
		}
    } else if (contactState.value == "open") {
    	log.debug "doorUnlockedTooLong() Door open and lock timer expired (${closedThresholdInMin} min):  doing nothing"
	} else if (lockState.value == "locked") {
		log.warn "doorUnlockedTooLong() called but lock is closed:  doing nothing"
	}
}

def sendMessage(elapsedInMin)
{
	def msg = "${locationName} has been open for ${elapsedInMin} minute(s)."
	log.info msg
    
    sendPush(msg)
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
            sendSms(phone, msg)
        }
    }
}

def getDelayInSec(thresholdInMin) {
	// default to 600 seconds (10 minutes) if passed bad value
	return ((thresholdInMin != null && thresholdInMin != "" && thresholdInMin > 0) ? 
    	convertMinToSec(thresholdInMin) : getGlobalTimeDelayDefault())
}

def scheduleDoorOpenTooLong(thresholdInMin) {
	unschedule()
    runIn(getDelayInSec(thresholdInMin), doorOpenTooLong, [overwrite: false])
}

def scheduleDoorUnlockedTooLong(thresholdInMin) {
	unschedule()
	runIn(getDelayInSec(thresholdInMin), doorUnlockedTooLong, [overwrite: false])
}

def getTimeRemainingInSec(thresholdInMin, lastStateChangeTime) {
	log.debug "currTime: ${now()}, lastStateChangeTime: ${lastStateChangeTime}"
	def thresholdInSec = convertMinToSec(thresholdInMin)
	def currTimeInSec = getCurrentTimeInSec()
    def lastStateChangeTimeInSec = convertMsToSec(lastStateChangeTime)
    def elapsed = currTimeInSec - lastStateChangeTimeInSec
    if(currTimeInSec < lastStateChangeTimeInSec) {
    	log.debug "getRemainingTimeInSec() error currTime (${currTimeInSec}) < lastStateChangeTime(${lastStateChangeTimeInSec}), diff (${Math.round(currTimeinSec - lastStateChangeTimeInSec)})"
        return 0
    }
    
    return (thresholdInSec - elapsed)
}

def getMsgResendFrequencyInSec() {
	return ((frequency != null && frequency != "" && frequency > 0) ? 
    	convertMinToSec(frequency) : getGlobalTimeDelayDefault())
}

def getGlobalRetryCount() { return 5 }

def getGlobalTimeDelayDefault() { return 600 }

def getCurrentTimeInSec() { return convertMsToSec(now()) }

def convertMinToSec(minutes) { return minutes * 60 }

def convertMinToMs(minutes) { return minutes * 60000 }

def convertMsToMin(millisec) { return Math.round(millisec / 60000) }

def convertMsToSec(millisec) { return Math.round(millisec / 1000) }

def convertSecToMin(seconds) { return Math.round(seconds / 60) }