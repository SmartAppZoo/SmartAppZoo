/**
 * SmartThings example Code for GroveStreams
 * A full "how to" guide for this example can be found at https://www.grovestreams.com/developers/getting_started_smartthings.html
 *
 * Copyright 2015 Jason Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 */
 
definition(
                name: "GroveStreams Logging",
                namespace: "GnomeSoup",
                author: "Jason Steele",
                description: "Log to GroveStreams",
                category: "My Apps",
                iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
                iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
 
preferences {
        section("Log devices...") {
                input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
                input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        		input "humidity", "capability.relativeHumidityMeasurement", title: "Humidity", required:false, multiple: true
                input "switches", "capability.switch", title: "Switch", required:false, multiple:true
                input "power", "capability.powerMeter", title: "Power Meters", required:false, multiple:true
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
        initialize()
}
 
def initialize() {
        subscribe(temperatures, "temperature", handleTemperatureEvent)
        subscribe(contacts, "contact", handleContactEvent)
        subscribe(humidity, "humidity", handleHumidityEvent)
        subscribe(switches, "switch", handleSwitchEvent)
        subscribe(power, "power", handlePowerEvent)
}
 
def handleTemperatureEvent(evt) {
        sendValue(evt) { it.toString() }
}
 
def handleContactEvent(evt) {
        sendValue(evt) { it == "open" ? "true" : "false" }
}

def handleSwitchEvent(evt) {
		sendValue(evt) { it == "on" ? "true" : "false" }
}

def handleHumidityEvent(evt) {
		sendValue(evt) { it.toString() }
}

def handlePowerEvent(evt) {
		sendValue(evt) { it.toString() }
}
 
private sendValue(evt, Closure convert) {
        def compId = URLEncoder.encode(evt.displayName.trim())
        def streamId = evt.name
        def value = convert(evt.value)
       
        log.debug "Logging to GroveStreams ${compId}, ${streamId} = ${value}"
       
        def url = "https://grovestreams.com/api/feed?api_key=${channelKey}&compId=${compId}&${streamId}=${value}"
        def putParams = [
                uri: url,
                body: []]
 
        httpPut(putParams) { response ->
                if (response.status != 200 ) {
                        log.debug "GroveStreams logging failed, status = ${response.status}"
                }
        }
}