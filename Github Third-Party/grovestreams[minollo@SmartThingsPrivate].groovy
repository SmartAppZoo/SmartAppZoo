/**
 *  GroveStreams
 *
 *  Copyright 2014 Jason Steele
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
    name: "GroveStreams",
    namespace: "JasonBSteele",
    author: "Jason Steele",
    description: "Log to GroveStreams",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")



preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
    section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required:false, multiple: true
        input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "batteries", "capability.battery", title: "Batteries", required: false, multiple: true
        input "thermostats", "capability.thermostat", title: "Thermostats", required: false, multiple: true
        input "alarms", "capability.Alarm", title: "Alarms", required: false, multiple: true
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
    subscribe(thermostats, "thermostatOperatingState", handleThermostatOperatingStateEvent)
    subscribe(alarms, "alarmStatus", handleAlarmStatusEvent)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	subscribe(app, appTouch)
    runIn(600, processQueue)
}

def pollerEvent(evt) {
	log.debug "[PollerEvent] keepAliveLatest == ${atomicState.keepAliveLatest}; now == ${now()}"
    if (atomicState.keepAliveLatest && now() - atomicState.keepAliveLatest > 660000) {
    	log.error "Waking up timer"
    	processQueue()
    }
}

def handleAlarmStatusEvent(evt) {
    queueValue(evt) { (it == "stay" || it == "away") ? "true" : "false" }
}

def handleThermostatOperatingStateEvent(evt) {
    queueValue(evt) { it == "idle" ? "false" : "true" }
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
	def jsonPayload = ["compId": evt.displayName, "streamId": evt.name, "data": convert(evt.value), "time": now()]
    log.debug "Appending to queue ${jsonPayload}"
    
    if (atomicState.queue == null) atomicState.queue = []
    atomicState.queue = atomicState.queue + jsonPayload
}

def processQueue() {
    runIn(600, processQueue)
    atomicState.keepAliveLatest = now()
	def url = "https://grovestreams.com/api/feed?api_key=${channelKey}"
    def customHeader = ["X-Forwarded-For": app.id]
    //log.debug "URL: ${url}; Header: ${customHeader}; Body: ${atomicState.queue}"
    if (atomicState.queue != []) {
        log.debug "Events: ${atomicState.queue}"
      
        try {
            httpPutJson(["uri": url, "header": customHeader, "body": atomicState.queue]) {
                response -> 
                if (response.status != 200 ) {
                    log.debug "GroveStreams logging failed, status = ${response.status}"
                } else {
                	log.debug "GroveStreams accepted event(s)"
                    atomicState.queue = []
                }
            }
        } catch(e) {
        	if (e.toString().contains("groovyx.net.http.ResponseParseException")) {
            	log.warn "Error parsing return value: \"${e}\""
                atomicState.queue = []
            } else {
            	log.error "Error sending items: \"${e}\""
            }
		}
    }
}


private sendValue(evt, Closure convert) {
    def compId = URLEncoder.encode(evt.displayName.trim())
    def streamId = evt.name
    def value = convert(evt.value)

    log.debug "Logging to GroveStreams ${compId}, ${streamId} = ${value}"

	def url = "https://grovestreams.com/api/feed?api_key=${channelKey}&compId=${compId}&${streamId}=${value}"

	try {
        def putParams = [
            uri: url,
            body: []
        ]

        httpPut(putParams) { 
            response -> 
            if (response.status != 200 ) {
                log.debug "GroveStreams logging failed, status = ${response.status}"
            }
        }
    } catch(e) {
    	def errorInfo = "Error sending value: ${e}"
		log.error errorInfo
    }
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
	processQueue()
}


