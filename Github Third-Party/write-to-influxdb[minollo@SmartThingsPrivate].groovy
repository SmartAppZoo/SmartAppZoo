/**
 *  Write to InfluxDB
 *
 *  Copyright 2016 Carlo Innocenti
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
    name: "Write to InfluxDB",
    namespace: "minollo",
    author: "Carlo Innocenti",
    description: "Write to InfluxDB",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "serverURL"
    appSetting "database"
    appSetting "user"
    appSetting "pwd"
}


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
    runIn(180, processQueue)
//    atomicState.queue = ""
}

def pollerEvent(evt) {
	log.debug "[PollerEvent] keepAliveLatest == ${atomicState.keepAliveLatest}; now == ${now()}"
    if (atomicState.keepAliveLatest && now() - atomicState.keepAliveLatest > 200000) {
    	log.error "Waking up timer"
    	processQueue()
    }
}

def handleAlarmStatusEvent(evt) {
    queueValue(evt) { (it == "stay" || it == "away") ? "1" : "0" }
}

def handleThermostatOperatingStateEvent(evt) {
    queueValue(evt) { it == "idle" ? "0" : "1" }
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
    queueValue(evt) { it == "open" ? "1" : "0" }
}

def handleAccelerationEvent(evt) {
    queueValue(evt) { it == "active" ? "1" : "0" }
}

def handleMotionEvent(evt) {
    queueValue(evt) { it == "active" ? "1" : "0" }
}

def handlePresenceEvent(evt) {
    queueValue(evt) { it == "present" ? "1" : "0" }
}

def handleSwitchEvent(evt) {
    queueValue(evt) { it == "on" ? "1" : "0" }
}

private queueValue(evt, Closure convert) {
	def line = "${evt.name},device=${evt.displayName.replaceAll(" ", "\\\\ ").replaceAll(",", "\\\\,")},location=${location.name.replaceAll(" ", "\\\\ ").replaceAll(",", "\\\\,")} value=${convert(evt.value)} ${now()}000000"
    log.debug "Appending to queue ${line}"
    
    if (atomicState.queue == null) atomicState.queue = ""
    atomicState.queue = atomicState.queue + line + "\n"
}

def processQueue() {
    runIn(180, processQueue)
    atomicState.keepAliveLatest = now()
	def url = "${appSettings.serverURL}/write?db=${appSettings.database}&u=${appSettings.user}&p=${appSettings.pwd}"
//    log.debug "URL: ${url}; Body: ${atomicState.queue}"
    if (atomicState.queue != "" && atomicState.queue != null) {
        log.debug "Events: ${atomicState.queue}"
      
        try {
            httpPost(["uri": url, "body": atomicState.queue]) {
                response -> 
                if (response.status != 204 ) {
                    log.debug "InfluxDB logging failed, status = ${response.status}"
                } else {
                	log.debug "InfluxDB accepted event(s)"
                    atomicState.queue = ""
                }
            }
        } catch(e) {
        	if (e.toString().contains("groovyx.net.http.ResponseParseException")) {
            	log.warn "Error parsing return value: \"${e.getMessage}\""
                atomicState.queue = ""
            } else {
            	log.error "Error sending items: \"${e.getMessage()}\""
            }
		}
    }
}


def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
	processQueue()
}


