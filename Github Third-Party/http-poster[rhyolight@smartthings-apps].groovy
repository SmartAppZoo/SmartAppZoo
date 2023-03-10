/**
 *  HTTP POSTer
 *
 *  Copyright 2015 Matthew Taylor
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
    name: "HTTP POSTer",
    namespace: "rhyolight",
    author: "Matthew Taylor",
    description: "Relays all device data as POST requests to a given URL.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
    section ("Devices to POST") {
        input "powers", "capability.powerMeter", title: "Power Meters", required: false, multiple: true
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required: false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
        input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
        input "batteries", "capability.battery", title: "Batteries", required: false, multiple: true
        input "energies", "capability.energyMeter", title: "Energy Meters", required: false, multiple: true
    }
    section ("URL to POST to") {
        input "url", "text", title: "The URL to relay data into.", required: true
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
    subscribe(powers, "power", handlePowerEvent)
    subscribe(temperatures, "temperature", handleTemperatureEvent)
    subscribe(waterSensors, "water", handleWaterEvent)
    subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(presence, "presence", handlePresenceEvent)
    subscribe(switches, "switch", handleSwitchEvent)
    subscribe(batteries, "battery", handleBatteryEvent)
    subscribe(energies, "energy", handleEnergyEvent)
}

def handlePowerEvent(evt) {
    sendValue(evt) { it.toFloat() }
}

def handleTemperatureEvent(evt) {
    sendValue(evt) { it.toFloat() }
}
 
def handleWaterEvent(evt) {
    sendValue(evt) { it == "wet" ? true : false }
}
 
def handleHumidityEvent(evt) {
    sendValue(evt) { it.toFloat() }
}
 
def handleContactEvent(evt) {
    sendValue(evt) { it == "open" ? true : false }
}
 
def handleAccelerationEvent(evt) {
    sendValue(evt) { it == "active" ? true : false }
}
 
def handleMotionEvent(evt) {
    sendValue(evt) { it == "active" ? true : false }
}
 
def handlePresenceEvent(evt) {
    sendValue(evt) { it == "present" ? true : false }
}
 
def handleSwitchEvent(evt) {
    sendValue(evt) { it == "on" ? true : false }
}
 
def handleBatteryEvent(evt) {
    sendValue(evt) { it.toFloat() }
}
 
def handleEnergyEvent(evt) {
    sendValue(evt) { it.toFloat() }
}

private sendValue(evt, Closure convert) {
    def compId = URLEncoder.encode(evt.displayName.trim())
    def streamId = evt.name
    def value = convert(evt.value)
    def date = new Date().format("yyyy-MM-dd HH:m:s.S")

    log.debug "Sending ${compId}/${streamId} data to ${url}..."

	def payload = [
        component: compId,
        stream: streamId,
        time: date,
        timezone: location.timeZone.getID(),
        value: value
    ]

    def params = [
        uri: url,
        contentType: "application/json",
        body: payload
    ]

    try {
        httpPostJson(params) { resp ->
        	log.debug resp.status
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }

}
