/**
 *  Recurring System Check
 *
 *  Copyright 2017 Rasmus Agdestein
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
    name: "Kid Desktop Power",
    namespace: "rasmusagdestein",
    author: "Rasmus Agdestein",
    description: "Checks system every 5 minutes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select devices") {
    	input "DesktopPower", "capability.switch", title: "Desktop Power", multiple: true
   		input "MotionSensor", "capability.motionSensor", title: "Motion Sensor", multiple: false
	}
    section("WeekDay Turn on between what times?") {
        input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
    }
    section("Weekend Turn on between what times?") {
        input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
    }
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
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
	runEvery5Minutes(generalSystemsCheck)
    subscribe(MotionSensor, "motion", MotionHandler)
}


def generalSystemsCheck() {
	log.debug "generalSystemsCheck"
    
    def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    if (between) {
       // DesktopPower.on()
    } else {
        DesktopPower.off()
    }
}

def MotionHandler(evt) {
		log.debug evt.value
    if (evt.value == "active") {
        log.debug "Motion"
        DesktopPower.on()
    } else if (evt.value == "inactive") {
    	checkMotion()
        log.debug "No Motion"
    }
}


def checkMotion() {
    log.debug "In checkMotion scheduled method"

    def motionState = MotionSensor.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            DesktopPower.off()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}