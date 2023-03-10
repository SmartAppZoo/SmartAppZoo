/**
 *  Watch Winder
 *
 *  Copyright 2017 Darin Spivey
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

import groovy.time.TimeCategory
import java.util.TimeZone
import java.text.SimpleDateFormat

definition(
    name: "Watch Winder",
    namespace: "darinspivey",
    author: "Darin Spivey",
    description: "A more advanced timer for controlling automatic watch winders such that the watch mechanics are fully exercised.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png"
)


preferences {
	section("Power Outlet") {
		input "watchWinder", "capability.outlet", title: "This is the power outlet that the watch winder is connected to."
	}
	section("Timer Configuration") {
    	paragraph "According to your watch and how long it stays wound, set the 'Hours Off' to a number of hours just shy of when the watch will die."
        input name: "hoursOff", title: "Hours Off", type: "number", defaultValue: 37, required: false
        paragraph "Set the 'Hours On' to be the amount of time it takes to fully wind your watch."
        input name: "hoursOn", title: "Hours On", type: "number", defaultValue: 5, required: false
    }
    section("Options") {
    	input name: "startInOff", title: "Start with winder off?", type: "bool", defaultValue: false
        input name: "logToNotifications", title: "Show status in the log?", type: "bool", defaultValue: true
        input name: "statusFrequency", title: "Status every N hours:", type: "number", defaultValue: 1, required: false
        input name: "DST", title: "is Daylight Savings on?", type: "bool", defaultValue: true
    }
}

def installed() {
	logit "Installed..."
	initialize()
}
 
def updated() {
	logit "Updated..."
	initialize()
}

def initialize() {
	logit "Off: $hoursOff hrs, On: $hoursOn hrs"
    
    if (startInOff) {
    	state.winding = true
    }
    else {
    	state.winding = false     
    }
    toggleState()
    if (logToNotifications) {
        logit "Status every $statusFrequency hrs.  Next: ${state.nextStatus}"
    }
}

def toggleState() {
   	def now = new Date()
	state.lastCheck = formatDate(now)
   
   	// Start/Stop the winder
    if (state.winding) {
        watchWinder.off()
        state.winding = false
    }
    else {
    	watchWinder.on()
        state.winding = true
    }
    
    // Set the next check based on current status
    use(TimeCategory) {
    	def nextCheck
    	if (state.winding) {
            nextCheck = now + hoursOn.hours
        }
        else {
            nextCheck = now + hoursOff.hours
		}
        def delay = (nextCheck.time - now.time) / 1000
        runIn(delay, toggleState)
        state.nextCheck = formatDate(nextCheck)
    }
	status()
}

def status() {
    def windingStr = state.winding ? "Winding!" : "Not winding."
    logit "$windingStr Next change is ${state.nextCheck}.  Last change was ${state.lastCheck}"
    if (logToNotifications) {
	    use(TimeCategory) {
        	def now = new Date()
            def nextStatus = now + statusFrequency.hours
            def delay = (nextStatus.time - now.time) / 1000
            state.nextStatus = formatDate(nextStatus)
            runIn(delay, status)
        }
    }
}

def formatDate(dateInput) {
    def formatter = new SimpleDateFormat("E MMMM d h:mm a")
    def offset = DST ? "04" : "05"
    formatter.setTimeZone(TimeZone.getTimeZone("GMT-${offset}:00"))
    return formatter.format(dateInput)
}

def logit(msg) {
	log.debug msg
    if (logToNotifications) {
    	sendNotificationEvent("[Watch Winder] $msg")
    }
}
