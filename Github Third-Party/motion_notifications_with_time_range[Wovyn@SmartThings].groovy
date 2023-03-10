/*
 *  Copyright 2018 Scott Lemon
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *
 * v1.0 - First version, initial commit
 *
 */

 definition(
    name: "Motion Notifications with Time Range",
    namespace: "Wovyn",
    author: "Scott Lemon",
    description: "A motion notification app with configurable time range",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Turn on when motion detected:") {
        input "theMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on these switches:") {
        input "theSwitches", "capability.switch", multiple: true, required: true
    }
    section("Only between these times:") {
        input "startTime", "time", title: "From Time", required: true
        input "endTime", "time", title: "To Time", required: true
    }
    section("Time beyond To Time to allow Inactive Event") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(theMotion, "motion.active", motionDetectedHandler)
    subscribe(theMotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    // Motion event arrived ... now check if the current time is within the time range
    def between = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone)
    if (between) {
		log.debug "motionNotificationWithTimeRange:  active executed - inside of time range!"
		for (aSwitch in theSwitches) {
		  aSwitch.on()
		}
    } else {
        // nothing to do ...
		log.debug "motionNotificationWithTimeRange:  active ignored - outside of time range!"
    }
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
	use( groovy.time.TimeCategory ) {
		def between = timeOfDayIsBetween(startTime, endTime + minutes.minutes, new Date(), location.timeZone)
		if (between) {
			log.debug "motionNotificationWithTimeRange:  inactive executed - inside of time range!"
			for (aSwitch in theSwitches) {
			  aSwitch.off()
			}
		} else {
			// nothing to do ...
			log.debug "motionNotificationWithTimeRange:  inactive ignored - outside of time range!"
		}
	}
    // runIn(60 * minutes, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    def motionState = theMotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
			for (aSwitch in theSwitches) {
				aSwitch.off()
			}
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}