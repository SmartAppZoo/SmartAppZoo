/**
 *  Copyright 2015 SmartThings
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
 *  Once a Day
 *
 *  Author: SmartThings
 *
 *  Turn on one or more switches at a specified time and turn them off at a later time.
 */

definition (
    name: "Once a Day",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Control Thermostat settings.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Select thermostat to control...") {
		input name: "Thermostat", type: "capability.thermostat", multiple: false
	}
	section("Turn it up at...") {
		input name: "startTime", title: "Turn Up Time?", type: "time"
	}
    section("High temp 80 - 106 ...") {
        input name: "highTemp", title: "High Temp?", type: "number", range:"(80..106)"
    }
	section("And turn it down at...") {
		input name: "stopTime", title: "Turn Down Time?", type: "time"
	}
    section("Low temp 70 - 90 ...") {
        input name: "lowTemp", title: "Low Temp?", type: "number", range:"(70..90)"
    }

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(startTime, "startTimerCallback")
	schedule(stopTime, "stopTimerCallback")

}

def updated(settings) {
	unschedule()
	schedule(startTime, "startTimerCallback")
	schedule(stopTime, "stopTimerCallback")
}

def startTimerCallback() {
	log.debug "Turning up the temperature"
	thermostat.setHeatingSetpoint(highTemp)

}

def stopTimerCallback() {
	log.debug "Turning down the temperature"
    thermostat.setHeatingSetpoint(lowTemp)
}