/**
 *  DailyRoutineMonitor
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
 */
definition(
    name: "DailyRoutineMonitor",
    namespace: "mbeynon",
    author: "Michael Beynon",
    description: "Inspired by the SmartThings SmartApp, but supporting enhanced logic with multiple sensors, multiple checks during the time interval, and can be configured to send to multiple phone numbers.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/calendar_contact-accelerometer@2x.png")


preferences {
	page(name: "pageOne", title: "Describe the people involved", nextPage: "pageTwo", uninstall: true) {
        section("Who is being monitored?") {
            input "personMonitor", "text", title: "Name?"
        }
        section("Who should be alerted when anomalies are detected?") {
            input("recipients", "contact", title: "People to notify", description: "Send notifications to") {
                input "notifyPhone1", "phone", title: "Phone number?", required: false
                input "notifyPhone2", "phone", title: "Phone number?", required: false
                input "notifyPhone3", "phone", title: "Phone number?", required: false
            }
        }
    }
    page(name: "pageTwo", title: "Describe the sensors to monitor", nextPage: "pageThree", uninstall: true) {
		section("Choose which sensors to monitor") {
            input "motionSensors", "capability.motionSensor", title: "Motion sensors?", multiple: true
            input "contactSensors", "capability.contactSensor", title: "Open/close sensors", multiple: true
        }
    }
    page(name: "pageThree", title: "Describe when to monitor the sensors", nextPage: "pageFour", uninstall: true) {
    	// TODO: check to ensure time1 < time2 and show some kind of error message
        // BUGBUG: perhaps better would be to just get starting and ending hours?
		section("Choose when to monitor sensor activity") {
			input "timeStart", "time", title: "Starting time each day?", required: true
			input "timeEnd", "time", title: "Ending time each day?", required: true
			input "repeatMinutes", "number", title: "Repeat check after how many minutes?", required: true
        }
		section("Choose how much non-activity is required to trigger an alert") {
        	input "minutesNoActivity", "number", title: "Alert after how many minutes?", required: true
        }
    }
    page(name: "pageFour", title: "Name app and configure modes", uninstall: true, install: true) {
		section() {
            label title: "Assign a name", required: false
            mode name: "modeMultiple", title: "Choose modes for monitoring", required: false
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
	initialize()
}

def initialize() {
    schedule(timeStart, "firstDayHandler")
}

/**
 *  Once per day this runs, which is the start of the monitoring interval
 */
def firstDayHandler() {
    scheduleRepeat()
	checkForActivityAndAlert()
}

/**
 *  This runs one or more times per day, depending on how many repeatMinutes fit it the monitoring interval.
 *  It is a separate handler to avoid replacing the first day handler at timeStart.
 */
def repeatDayHandler() {
    scheduleRepeat()
    checkForActivityAndAlert()
}

def scheduleRepeat() {
    def nextTime = now() + repeatMinutes * 60000
	if (nextTime <= timeEnd) {
        log.debug "scheduleRepeatHandler(): next call set for ${nextTime}"
    	schedule(nextTime, repeatDayHandler)
	}
}

def checkForActivityAndAlert() {
	if(noRecentContact() && noRecentMotion()) {
        if (notifyPhone1) {
        	sendNoActivityAlert(notifyPhone1)
        }
        if (notifyPhone2) {
        	sendNoActivityAlert(notifyPhone2)
        }
        if (notifyPhone3) {
        	sendNoActivityAlert(notifyPhone3)
        }
	} else {
		log.debug "There has been activity ${timePhrase}, not sending alert"
	}
}

def sendNoActivityAlert(notifyPhone) {
    def person = person1 ?: "your elder"
    def msg = "No Activity Alert!  There has been no activirty at ${person}'s home ${timePhrase}"
    log.debug msg

    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    } else {
        if (notifyPhone) {
            sendSms(notifyPhone, msg)
        } else {
            sendPush(msg)
        }
    }
}

private noRecentMotion() {
	if (motion1) {
		def motionEvents = motion1.eventsSince(sinceTime)
		log.trace "Found ${motionEvents?.size() ?: 0} motion events"
		if (motionEvents.find { it.value == "active" }) {
			log.debug "There have been recent 'active' events"
			return false
		} else {
			log.debug "There have not been any recent 'active' events"
			return true
		}
	} else {
		log.debug "Motion sensor not enabled"
		return true
	}
}

private noRecentContact() {
	if (contact1) {
		def contactEvents = contact1.eventsSince(sinceTime)
		log.trace "Found ${contactEvents?.size() ?: 0} door events"
		if (contactEvents.find { it.value == "open" }) {
			log.debug "There have been recent 'open' events"
			return false
		} else {
			log.debug "There have not been any recent 'open' events"
			return true
		}
	} else {
		log.debug "Contact sensor not enabled"
		return true
	}
}

private getSinceTime() {
	if (time0) {
		return timeToday(time0, location?.timeZone)
	}
	else {
		return new Date(now() - 21600000)
	}
}

private getTimePhrase() {
	def interval = now() - sinceTime.time
	if (interval < 3600000) {
		return "in the past ${Math.round(interval/60000)} minutes"
	}
	else if (interval < 7200000) {
		return "in the past hour"
	}
	else {
		return "in the past ${Math.round(interval/3600000)} hours"
	}
}