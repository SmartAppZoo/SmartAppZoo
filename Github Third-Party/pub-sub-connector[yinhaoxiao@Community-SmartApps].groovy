/*
 * SmartThings example Code for Google Cloug Pub/Sub Connector
 *
 * Copyright 2018 Ian Maddox
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
import groovy.json.JsonBuilder
import org.apache.commons.codec.binary.Base64

definition(
  name: "${appName()}",
  namespace: "ianmaddox",
  author: "Ian Maddox",
  description: "Google Cloud Pub/Sub Connector",
  category: "My Apps",
  singleInstance: false, // Log different devices to different endpoints
  iconUrl:   "https://raw.githubusercontent.com/ianmaddox/smartthings-gcppubsub/master/smartapps/ianmaddox/pub-sub-connector.src/img/pubsub-logo-60.png",
  iconX2Url: "https://raw.githubusercontent.com/ianmaddox/smartthings-gcppubsub/master/smartapps/ianmaddox/pub-sub-connector.src/img/pubsub-logo-120.png",
  iconX3Url: "https://raw.githubusercontent.com/ianmaddox/smartthings-gcppubsub/master/smartapps/ianmaddox/pub-sub-connector.src/img/pubsub-logo-240.png"
)

preferences {
  page(name: "startPage")
  page(name: "parentPage")
  page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
    return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Create a new PubSub automation.") {
            app(name: "childApps", appName: appName(), namespace: "ianmaddox", title: "Pub/Sub Topic", multiple: true)
        }

        section("About") {
            paragraph "Version 1.2"
            href url:"https://github.com/ianmaddox/smartthings-gcppubsub", style:"embedded", required:false, title:"Installation instructions"
        }
    }
}

def childStartPage() {
    return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
        section("Contact Sensors to Log") {
            input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
            input "contactLogType", "enum", title: "Values to log", options: ["open/closed", "true/false", "1/0"], defaultValue: "open/closed", required: true, multiple: false
        }

        section("Motion Sensors to Log") {
            input "motions", "capability.motionSensor", title: "Motion Sensors", required: false, multiple: true
            input "motionLogType", "enum", title: "Values to log", options: ["active/inactive", "true/false", "1/0"], defaultValue: "active/inactive", required: true, multiple: false
		}

        section("Other Sensors") {
			input "accelerationSensor", "capability.accelerationSensor", title: "Acceleration", required: false, multiple: true
			input "carbonMonoxide", "capability.carbonMonoxideDetector", title: "Carbon Monoxide", required: false, multiple: true

 		}

        section("Thermostat Settings") {
            input "airConditioner", "capability.airconditioner", title: "Air Conditioner", required: false, multiple: true
            input "coolingSetPoints", "capability.thermostat", title: "Cooling Setpoints", required: false, multiple: true
            input "heatingSetPoints", "capability.thermostat", title: "Heating Setpoints", required: false, multiple: true
            input "thermOperatingStates", "capability.thermostat", title: "Operating States", required: false, multiple: true
        }

        section("Locks to Log") {
            input "locks", "capability.lock", title: "Locks", multiple: true, required: false
            input "lockLogType", "enum", title: "Values to log", options: ["locked/unlocked", "true/false", "1/0"], defaultValue: "locked/unlocked", required: true, multiple: false
        }

		section("Power Meters") {
            input "energy", "capability.energyMeter", title: "Energy Meters", required: false, multiple: true
            input "power", "capability.powerMeter", title: "Power Meters", required: false, multiple: true
            input "current", "capability.currentMeter", title: "Current Meters", required: false, multiple: true
            input "voltage", "capability.voltageMeasurement", title: "Voltage Measurement", required: false, multiple: true
        }

        section("Log Other Devices") {
            input "alarm", "capability.alarm", title: "Alarm", required: false, multiple: true
            input "batteries", "capability.battery", title: "Batteries", multiple: true, required: false
            input "beacon", "capability.beacon", title: "Beacon", required: false, multiple: true
            input "buttons", "capability.button", title: "Buttons", multiple: true, required: false
            input "colorControl", "capability.colorControl", title: "Color Control", multiple: true, required: false
            input "dimmerSwitches", "capability.switchLevel", title: "Dimmer Switches", required: false, multiple: true
            input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity Sensors", required: false, multiple: true
            input "illuminances", "capability.illuminanceMeasurement", title: "Illuminance Sensors", required: false, multiple: true
            input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", required: false, multiple: true
            input "sensorAttributes", "text", title: "Sensor Attributes (comma delimited)", required: false
            input "sensors", "capability.sensor", title: "Sensors", required: false, multiple: true
            input "switches", "capability.switch", title: "Switches", required: false, multiple: true
            input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required: false, multiple: true
        }

        section ("Google Cloud") {
            input "topic", "text", title: "Pub/Sub Topic", required: true
            input "apikey", "password", title: "Pub/Sub API Key", required: true
        }

        section ("Technical settings") {
            input "bufferTime", "enum", title:"Time to buffer events before pushing to Pub/Sub (in minutes)", options: ["0", "1", "5", "10", "15"], defaultValue:"5"
            input "resetVals", "enum", title:"Reset the state values (buffer, schedule, etc)", options: ["yes", "no"], defaultValue: "no"
        }

        section([mobileOnly:true], "Options") {
            label(title: "Assign a name", required: true)
        }
    }
}

private def appName() {
    return "${parent ? "Google Cloud Pub/Sub Connector" : "Pub/Sub Connector"}"
}

def installed() {
    log.debug "installed"
    setOriginalState()
    initialize()
}

def updated() {
    log.debug "Updated"
    unsubscribe()
    initialize()
    if (settings.resetVals == "yes") {
        setOriginalState()
        settings.resetVals = "no"
    }
}

def initialize() {
    if (parent) {
        initChild()
    } else {
        initParent()
    }

    log.debug "End initialize()"
}

def initParent() {
    log.debug "initParent()"
}

def initChild() {
    log.debug "initChild()"

    unsubscribe()

// TODO: Add support for the following capabilities
// Format: capabilityname subscription1 subscription2
//
// capability.doorControl door
// capability.illuminanceMeasurement illuminance
// capability.imageCapture image
// capability.lock lock
// capability.mediaController activities currentActivity
// capability.momentary	momentary
// capability.motionSensor motion
// capability.musicPlayer status level trackDescription trackData mute
// capability.signalStrength lqi rssi
// capability.sleepSensor sleeping
// capability.smokeDetector smoke
// capability.stepSensor steps goal
// capability.thermostat thermostatSetpoint thermostatMode thermostatFanMode thermostatOperatingState
// capability.threeAxis threeAxis
// capability.touchSensor touch
// capability.touchSensor touch
// capability.waterSensor water

    subscribe(current, "current", handleEnergyEvent)
    subscribe(energy, "energy", handleEnergyEvent)
    subscribe(voltage, "voltage", handleEnergyEvent)
    subscribe(power, "power", handleEnergyEvent)

    subscribe(accelerationSensor, "acceleration", handleStringEvent)
    subscribe(airConditioner, "airConditioner", handleStringEvent)
    subscribe(alarm, "alarm", handleStringEvent)
    subscribe(batteries, "battery", handleNumberEvent)
    subscribe(beacon, "beacon", handleStringEvent)
    subscribe(button, "button", handleStringEvent)
    subscribe(carbonMonoxide, "carbonMonoxide", handleStringEvent)
    subscribe(colorControl, "Color Control", handleStringEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(coolingSetPoints, "coolingSetpoint", handleNumberEvent)
    subscribe(dimmerSwitches, "level", handleNumberEvent)
    subscribe(dimmerSwitches, "switch", handleStringEvent)
    subscribe(heatingSetPoints, "heatingSetpoint", handleNumberEvent)
    subscribe(humidities, "humidity", handleNumberEvent)
    subscribe(illuminances, "illuminance", handleNumberEvent)
    subscribe(locks, "lock", handleLockEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(powerMeters, "power", handleNumberEvent)
    subscribe(presenceSensors, "presence", handleStringEvent)
    subscribe(switches, "switch", handleStringEvent)
    subscribe(temperatures, "temperature", handleNumberEvent)
    subscribe(thermOperatingStates, "thermostatOperatingState", handleStringEvent)
    subscribe(voltageMeasurements, "voltage", handleEnergyEvent)

	if (sensors != null && sensorAttributes != null) {
        sensorAttributes.tokenize(',').each {
            subscribe(sensors, it, handleStringEvent)
        }
    }
}

def setOriginalState() {
    log.debug "Set original state"
    unschedule()
    atomicState.buffer = ""
    atomicState.eventsBuffered = 0
    atomicState.failureCount = 0
    atomicState.scheduled = false
    atomicState.lastSchedule = 0
}

def handleEnergyEvent(evt) {
//    log.debug "energy event ${evt}"
    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt) { it }
    } else {
        sendValue(evt) { it }
    }
}
def handleStringEvent(evt) {
//    log.debug "handling string event ${evt}"
    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt) { it }
    } else {
        sendValue(evt) { it }
    }
}

def handleNumberEvent(evt) {
//    log.debug "handling number event ${evt}"
    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt) { it.toString() }
    } else {
        sendValue(evt) { it.toString() }
    }
}

def handleContactEvent(evt) {
    // default to open/close, the value of the event
    def convertClosure = { it }
    if (contactLogType == "true/false")
        convertClosure = { it == "open" ? "true" : "false" }
    else if ( contactLogType == "1/0")
        convertClosure = { it == "open" ? "1" : "0" }

    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt, convertClosure)
    } else {
        sendValue(evt, convertClosure)
    }
}

def handleMotionEvent(evt) {
    // default to active/inactive, the value of the event
    def convertClosure = { it }
    if (motionLogType == "true/false")
        convertClosure = { it == "active" ? "true" : "false" }
    else if (motionLogType == "1/0")
        convertClosure = { it == "active" ? "1" : "0" }

    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt, convertClosure)
    } else {
        sendValue(evt, convertClosure)
    }
}

def handleLockEvent(evt) {
    // default to locked/unlocked, the value of the event
    def convertClosure = { it }
    if (lockLogType == "true/false") {
        convertClosure = { it == "locked" ? "true" : "false" }
    } else if (lockLogType == "1/0") {
        convertClosure = { it == "locked" ? "1" : "0" }
    }
    if (settings.bufferTime.toInteger() > 0) {
        bufferValue(evt, convertClosure)
    } else {
        sendValue(evt, convertClosure)
    }
}

private def getUrl() {
    String url = "https://pubsub.googleapis.com/v1/${topic}:publish?key=${apikey}"
    return url
}

private getEventMessage(evt, Closure convert) {
  def key = evt.displayName.trim()+ ":" +evt.name
  def name = evt.displayName.trim()
  def val = convert(evt.value)
  def payload = [
        id: evt.id,
        key: key,
        name: name,
        desc: evt.description,
        property: evt.name,
        value: val,
        source: evt.source,
        unit: evt.unit,
        location: evt.location,
        date: evt.isoDate,
        hub: evt.hubId,
        device: evt.device,
        deviceId: evt.deviceId
  ]
    def attribs = ""
    def sep = ''
    payload.each { k, v ->
      k = k.replaceAll('"', '\"')
      v = "${v}".replaceAll('"', '\"')
      attribs += "${sep}\"${k}\":\"${v}\""
      sep = ','
    }
    return '{"attributes":{'+attribs+'}}'
}

private sendValue(evt, Closure convert) {
  def msg = getEventMessage(evt, convert)
	def body = '{"messages":['+msg+']}'

	log.debug "Pub/Sub JSON: ${body}"

  publishMessage(body)
}

private publishMessage(body) {
  def params = [
        uri:  getUrl(),
        requestContentType: "application/json",
        body: body
    ]
    try {
      httpPost(params)
      return true
    } catch (e) {
        log.debug "${topic} publish error: ${e}"
        return false
    }
}

private bufferValue(evt, Closure convert) {
    checkAndProcessBuffer()
    if ( evt?.value ) {
        def msg = getEventMessage(evt, convert)
        addToBuffer(msg)
        scheduleBuffer()
    }
}

private addToBuffer(value) {
  def sep = atomicState.eventsBuffered > 0 ? ',' : ''
  atomicState.buffer = atomicState.buffer + sep + value
  atomicState.eventsBuffered = atomicState.eventsBuffered + 1
  log.debug "buffered event #${atomicState.eventsBuffered}: ${value}"
}

private checkAndProcessBuffer() {
    if (atomicState.scheduled && ((now() - atomicState.lastSchedule) > (settings.bufferTime.toInteger()*120000))) {
        // if event has been buffered for twice the amount of time it should be, then we are probably stuck
        sendEvent(name: "scheduleFailure", value: now())
        unschedule()
        processBuffer()
    }
}

def scheduleBuffer() {
    if (atomicState.failureCount >= 30) {
        log.debug "Too many failures, clearing buffer"
        sendEvent(name: "bufferFailure", value: now())
        resetState()
    }

    if (!atomicState.scheduled) {
        runIn(settings.bufferTime.toInteger() * 60, processBuffer)
        atomicState.scheduled=true
        atomicState.lastSchedule=now()
    }
}


private resetState() {
    atomicState.buffer = ""
    atomicState.eventsBuffered = 0
    atomicState.failureCount = 0
    atomicState.scheduled = false
}

def processBuffer() {
    atomicState.scheduled=false
    if (atomicState.eventsBuffered == 0) {
      // Buffer is empty
      log.debug "Buffer empty. Skipping processing."
      return
    }

    log.debug "Publishing " + atomicState.eventsBuffered + " events"
    def body = '{"messages":[' + atomicState.buffer + ']}'
    if(publishMessage(body)) {
        log.debug "Sent ${atomicState.eventsBuffered} events OK"
        resetState()
    } else {
        log.debug "Buffer flush failure!"
        atomicState.failureCount = atomicState.failureCount + 1
        scheduleBuffer()
    }
}
