/**
GroveStreams
*
Copyright 2014 Jason Steele
*
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:
*
http://www.apache.org/licenses/LICENSE-2.0
*
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.
*
*/
definition(
name: "GroveStreams",
namespace: "JasonBSteele",
author: "Jason Steele",
description: "Log to GroveStreams",
category: "My Apps",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
preferences {
section("Log devices...") {
input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
input "switches", "capability.switch", title: "Switches", required: false, multiple: true
}

section ("GroveStreams Feed PUT API key...") {
    input "channelKey", "text", title: "API key"
}
}

def installed() {
initialize()
}

def updated() {
unsubscribe()
unschedule()
log.debug "Attempted to Unschedule"
if (canSchedule()) {log.debug "all unscheduled."}
else {log.debug "still waiting for unschedule."}

initialize()
}

def initialize() {
subscribe(temperatures, "temperature", handleTemperatureEvent)
subscribe(humidities, "humidity", handleHumidityEvent)
subscribe(contacts, "contact", handleContactEvent)
subscribe(accelerations, "acceleration", handleAccelerationEvent)
subscribe(motions, "motion", handleMotionEvent)
subscribe(presence, "presence", handlePresenceEvent)
subscribe(switches, "switch", handleSwitchEvent)
subscribe(batteries, "battery", handleBatteryEvent)
state.queueCount = 0
state.queue = []
runEvery5Minutes(processQueue)
}

def handleTemperatureEvent(evt) {
queueValue(evt) { it.toString() }
}

def handleHumidityEvent(evt) {
queueValue(evt) { it.toString() }
}

def handleBatteryEvent(evt) {
queueValue(evt) { it.toString() }
}

def handleContactEvent(evt) {
queueValue(evt) { it == "open" ? "true" : "false" }
}

def handleAccelerationEvent(evt) {
queueValue(evt) { it == "active" ? "true" : "false" }
}

def handleMotionEvent(evt) {
queueValue(evt) { it == "active" ? "true" : "false" }
}

def handlePresenceEvent(evt) {
queueValue(evt) { it == "present" ? "true" : "false" }
}

def handleSwitchEvent(evt) {
queueValue(evt) { it == "on" ? "true" : "false" }
}

private queueValue(evt, Closure convert) {
def jsonPayload = [compId: evt.displayName, streamId: evt.name, data: convert(evt.value), time: now()]
log.debug "Appending to queue ${jsonPayload}"

state.queue << jsonPayload

state.queueCount = state.queueCount + 1
log.debug "Current queueCount = ${state.queueCount}"
if(state.queueCount > 10){
	processQueue()
	}
}

def processQueue() {
def url = "https://grovestreams.com/api/feed?api_key=${channelKey}"
log.debug "processQueue"
if (state.queue != []) {
log.debug "Events: ${state.queue}"

    try {
        httpPutJson([uri: url, body: state.queue ]) { 
            response -> 
            if (response.status != 200 ) {
                log.debug "GroveStreams logging failed, status = ${response.status}"
            } else {
                log.debug "GroveStreams accepted event(s)"
                state.queue = []
                state.queueCount = 0
            }
        }
    } catch(e) {
        def errorInfo = "Error sending value: ${e}"
        log.error errorInfo
    }
}
}