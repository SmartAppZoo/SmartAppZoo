/**
 *  Log SmartThings events to Correl8.me using ECS
 *
 *  Copyright 2019 Samuel Rinnetmäki
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
    name: "correl8_ecs",
    namespace: "Correl8",
    author: "Samuel Rinnetmäki",
    description: "New version of correl8",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)
preferences {
    section("Log devices...") {
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
        input "airqualitysensors", "capability.airQualitySensor", title: "Air quality sensors", required: false, multiple: true
        input "alarms", "capability.alarm", title: "Alarms", required: false, multiple: true
        input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
        input "buttons", "capability.button", title: "Buttons", required: false, multiple: true
        input "co2measurements", "capability.carbonDioxideMeasurement", title: "CO2 measurements", required:false, multiple: true
        input "codetectors", "capability.carbonMonoxideDetector", title: "CO detectors", required:false, multiple: true
        input "comeasurements", "capability.carbonMonoxideMeasurement", title: "CO measurements", required:false, multiple: true
        input "colorcontrols", "capability.colorControl", title: "Color controls", required:false, multiple: true
        input "colortemperatures", "capability.colorTemperature", title: "Color temperatures", required:false, multiple: true
        input "contacts", "capability.contactSensor", title: "Doors/windows", required: false, multiple: true
        input "doorcontrols", "capability.doorControl", title: "Door controls", required:false, multiple: true
        input "energies", "capability.energyMeter", title: "Energy Meters", required:false, multiple: true
        input "illuminances", "capability.illuminanceMeasurement", title: "Illuminances", required: false, multiple: true
        input "infrareds", "capability.infraredLevel", title: "Infrared levels", required: false, multiple: true
        input "locks", "capability.lock", title: "Locks", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "powermeters", "capability.powerMeter", title: "Power Meters", required:false, multiple: true
        input "powersources", "capability.powerSource", title: "Power Sources", required:false, multiple: true
        input "presences", "capability.presenceSensor", title: "Presence Sensors", required: false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
        input "signalstrengths", "capability.signalStrength", title: "Signal strengths", required: false, multiple: true
        input "smokedetectors", "capability.smokeDetector", title: "Smoke detectors", required:false, multiple: true
        input "soundSensors", "capability.soundSensor", title: "Sound sensors", required: false, multiple: true
        input "switchlevels", "capability.switchLevel", title: "Switch levels", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "tamperalerts", "capability.tamperAlert", title: "Tamper alerts", required:false, multiple: true
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "thermostatcoolingsetpoints", "capability.thermostatCoolingSetpoint", title: "Thermostat cooling setpoints", required:false, multiple: true
        input "thermostatfanmodes", "capability.thermostatFanMode", title: "Thermostat fan modes", required:false, multiple: true
        input "thermostatheatingsetpoints", "capability.thermostatHeatingSetpoint", title: "Thermostat heating setpoints", required:false, multiple: true
        input "thermostatmodes", "capability.thermostatMode", title: "Thermostat modes", required:false, multiple: true
        input "thermostatoperatingstates", "capability.thermostatOperatingState", title: "Thermostat operating states", required:false, multiple: true
        input "thermostatsetpoints", "capability.thermostatSetpoint", title: "Thermostat setpoints", required:false, multiple: true
        input "ultravioletindexes", "capability.ultravioletIndex", title: "Ultraviolet indexes", required: false, multiple: true
        input "valves", "capability.valve", title: "Valves", required: false, multiple: true
        input "voltages", "capability.voltageMeasurement", title: "Voltages", required:false, multiple: true
        input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
    }
    section ("Correl8 API key...") {
        input "apiKey", "text", title: "API key"
    }
}

def installed() {
    def logMsg = "Installed."
    sendEvent(name:"correl8", value:"installed",descriptionText:logMsg, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
    def logMsg = "Configuration updated."
    sendEvent(name:"correl8", value:"updated",descriptionText:logMsg, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)
}

def initialize() {
    def logMsg = "Instalizing SmartThings."
    sendEvent(name:"correl8", value:"initializing",descriptionText:logMsg, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)

    def initObj = [
      "device": [:],
      "event": [
        "device_name": "keyword",
        "attribute": "keyword",
        "value": "keyword"
      ]
    ]

    accelerations.each{ initObj.device["${it}.acceleration"] = "boolean" }
    initObj.event["acceleration"] = "boolean"
    airqualitysensors.each{ initObj.device["${it}.airQuality"] = "long" }
    initObj.event["airQuality"] = "long"
    alarms.each{ initObj.device["${it}.alarm"] = "keyword" }
    initObj.event["alarm"] = "keyword"
    batteries.each{ initObj.device["${it}.battery"] = "long" }
    initObj.event["battery"] = "long"
    buttons.each{ initObj.device["${it}.button"] = "keyword" }
    initObj.event["button"] = "keyword"
    co2measurements.each{ initObj.device["${it}.carbonDioxide"] = "long" }
    initObj.event["carbonDioxide"] = "long"
    codetectors.each{ initObj.device["${it}.detected"] = "boolean" }
    initObj.event["detected"] = "boolean"
    comeasurements.each{ initObj.device["${it}.carbonMonoxideLevel"] = "float" }
    initObj.event["carbonMonoxideLevel"] = "float"
    colorcontrols.each{ initObj.device["${it}.hue"] = "long" }
    initObj.event["hue"] = "long"
    colorcontrols.each{ initObj.device["${it}.saturation"] = "long" }
    initObj.event["saturation"] = "long"
    colortempereatures.each{ initObj.device["${it}.colorTemperature"] = "long" }
    initObj.event["colorTemperature"] = "long"
    contacts.each{ initObj.device["${it}.open"] = "boolean" }
    initObj.event["open"] = "boolean"
    doorcontrols.each{ initObj.device["${it}.door"] = "keyword" }
    initObj.event["door"] = "keyword"
    energies.each{ initObj.device["${it}.energy"] = "float" }
    initObj.event["energy"] = "float"
    illuminances.each{ initObj.device["${it}.illuminance"] = "float" }
    initObj.event["illuminance"] = "float"
    infrareds.each{ initObj.device["${it}.infraredLevel"] = "float" }
    initObj.event["infraredLevel"] = "float"
    locks.each{ initObj.device["${it}.locked"] = "boolean" }
    initObj.event["locked"] = "boolean"
    motions.each{ initObj.device["${it}.motion"] = "boolean" }
    initObj.event["motion"] = "boolean"
    powermeters.each{ initObj.device["${it}.power"] = "float" }
    initObj.event["power"] = "float"
    powersources.each{ initObj.device["${it}.source"] = "keyword" }
    initObj.event["source"] = "keyword"
    presences.each{ initObj.device["${it}.presence"] = "boolean" }
    initObj.event["presence"] = "boolean"
    humidities.each{ initObj.device["${it}.humidity"] = "float" }
    initObj.event["humidity"] = "float"
    signalstrengths.each{ initObj.device["${it}.lqi"] = "long" }
    initObj.event["lqi"] = "long"
    signalstrengths.each{ initObj.device["${it}.rssi"] = "float" }
    initObj.event["rssi"] = "float"
    smokedetectors.each{ initObj.device["${it}.detected"] = "boolean" }
    initObj.event["detected"] = "boolean"
    soundsensors.each{ initObj.device["${it}.detected"] = "boolean" }
    initObj.event["detected"] = "boolean"
    switchlevels.each{ initObj.device["${it}.level"] = "long" }
    initObj.event["level"] = "long"
    switches.each{ initObj.device["${it}.switch"] = "boolean" }
    initObj.event["switch"] = "boolean"
    tamperalerts.each{ initObj.device["${it}.detected"] = "boolean" }
    initObj.event["detected"] = "boolean"
    temperatures.each{ initObj.device["${it}.temperature"] = "float" }
    initObj.event["temperature"] = "float"
    thermostatcoolingsetpoints.each{ initObj.device["${it}.coolingSetpoint"] = "float" }
    initObj.event["coolingSetpoint"] = "float"
    thermostatfanmodes.each{ initObj.device["${it}.thermostatFanMode"] = "keyword" }
    initObj.event["thermostatFanMode"] = "keyword"
    thermostatheatingsetpoints.each{ initObj.device["${it}.heatingSetpoint"] = "float" }
    initObj.event["heatingSetpoint"] = "float"
    thermostatmodes.each{ initObj.device["${it}.mode"] = "keyword" }
    initObj.event["mode"] = "keyword"
    thermostatoperatingstates.each{ initObj.device["${it}.mode"] = "keyword" }
    initObj.event["mode"] = "keyword"
    thermostatsetpoints.each{ initObj.device["${it}.thermostatSetpoint"] = "float" }
    initObj.event["thermostatSetpoint"] = "float"
    ultravioletindexes.each{ initObj.device["${it}.ultravioletIndex"] = "float" }
    initObj.event["ultravioletIndex"] = "float"
    valves.each{ initObj.device["${it}.open"] = "boolean" }
    initObj.event["open"] = "boolean"
    voltages.each{ initObj.device["${it}.voltage"] = "float" }
    initObj.event["voltage"] = "float"
    watersensors.each{ initObj.device["${it}.detected"] = "boolean" }
    initObj.event["detected"] = "boolean"

    logMsg = "Initializing correl8 with: ${initObj}"
    sendEvent(name:"correl8", value:"initializing", descriptionText:logMsg, data:initObj, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)

    def params = [
        // uri: "http://api.correl8.me",
        uri: "http://correl8.me:3030",
        path: "/smartthings-ecs/init/",
        body: initObj
    ]
    try {
        httpPutJson(params) { resp ->
            logMsg = "Correl8 initialization done: ${resp.status}"
            sendEvent(name:"correl8", value:"initialized", descriptionText:logMsg, data:resp, eventType:"SMART_APP_EVENT", displayed: true)
            log.info(logMsg)
        }
    } catch (e) {
        logMsg = "Correl8 initialization failed: ${e}"
        sendEvent(name:"correl8", value:"initialization failed",descriptionText:logMsg, data:e, eventType:"SMART_APP_EVENT", displayed: true)
        log.warn(logMsg)
    }

    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    subscribe(airqualitysensors, "airQuality", handleInteger)
    subscribe(alarms, "alarm", handleString)
    subscribe(batteries, "battery", handleFloat)
    subscribe(buttons, "button", handleString)
    subscribe(co2measurements, "carbonDioxide", handleInteger)
    subscribe(codetectors, "carbonMonoxide", handleDetected)
    subscribe(comeasurements, "carbonMonoxideLevel", handleFloat)
    subscribe(colorcontrols, "hue", handleInteger)
    subscribe(colorcontrols, "saturation", handleInteger)
    subscribe(colortemperatures, "colorTemperature", handleInteger)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(doorcontrols, "door", handleString)
    subscribe(energies, "energy", handleFloat)
    subscribe(illuminances, "illuminance", handleFloat)
    subscribe(infrareds, "infraredLevel", handleFloat)
    subscribe(locks, "lock", handleLockEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(powermeters, "power", handleFloat)
    subscribe(powersources, "power", handleString)
    subscribe(presences, "presence", handlePresenceEvent)
    subscribe(humidities, "humidity", handleFloat)
    subscribe(signalstrengths, "lqi", handleInteger)
    subscribe(signalstrengths, "rssi", handleFloat)
    subscribe(humidities, "humidity", handleFloat)
    subscribe(smokedetectors, "smoke", handleDetected)
    subscribe(soundsensors, "sound", handleDetected)
    subscribe(switches, "switch", handleSwitchEvent)
    subscribe(tamperalerts, "tamper", handleDetected)
    subscribe(temperatures, "temperature", handleFloat)
    subscribe(thermostatcoolingsetpoints, "coolingSetpoint", handleFloat)
    subscribe(thermostatfanmodes, "thermostatFanMode", handleString)
    subscribe(thermostatheatingsetpoints, "heatingSetpoint", handleFloat)
    subscribe(thermostatmodes, "thermostatMode", handleString)
    subscribe(thermostatoperatingstates, "thermostatOperatingState", handleString)
    subscribe(thermostatsetpoints, "thermostatSetpoint", handleFloat)
    subscribe(ultravioletindexes, "ultravioletIndex", handleFloat)
    subscribe(valves, "valve", handleContactEvent)
    subscribe(voltages, "voltage", handleFloat)
    subscribe(waterSensors, "water", handleWaterEvent)

    logMsg = "Subscribed to events."
    sendEvent(name:"correl8", value:"subscribed",descriptionText:logMsg, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)

}

def handleAccelerationEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: evt.value == "active" ? true : false
    ]
    sendValue(e)
}

def handleButtonEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: "pushed",
        value: evt.value == "held" ? '"long"' : '"short"'
    ]
    sendValue(e)
}

def handleContactEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: "open",
        value: evt.value == "open" ? true : false
    ]
    sendValue(e)
}

def handleDetected(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: "detected",
        value: evt.value == "detected" ? true : false
    ]
    sendValue(e)
}

def handleFloat(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: Float.parseFloat(evt.value)
    ]
    sendValue(e)
}

def handleInteger(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: Integer.parseInt(evt.value)
    ]
    sendValue(e)
}

def handleLockEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: "locked",
        value: evt.value == "locked" ? true : false
    ]
    sendValue(e)
}

def handleMotionEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: evt.value == "active" ? true : false
    ]
    sendValue(e)
}

def handlePresenceEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: evt.value == "present" ? true : false
    ]
    sendValue(e)
}

def handleString(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: evt.value.trim()
    ]
    sendValue(e)
}

def handleSwitchEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: evt.name,
        value: evt.value == "on" ? true : false
    ]
    sendValue(e)
}

def handleWaterEvent(evt) {
    sendEvent(name:evt.name, value:evt.value,descriptionText:"${evt.displayName}: ${evt.name}=${evt.value}", data:evt, eventType:"SMART_APP_EVENT", displayed: false)
    def e = [
        sensor: evt.displayName.trim(),
        name: "wet",
        value: evt.value == "wet" ? true : false
    ]
    sendValue(e)
}

private sendValue(event) {
    def sensor = event.sensor
    def name = event.name
    def value = event.value

    def logMsg = "Logging ${sensor} to Correl8: ${name} = ${value}"
    sendEvent(name:name, value:value, descriptionText:logMsg, data:$event, eventType:"SMART_APP_EVENT", displayed: true)
    log.info(logMsg)

    def msgBody = [:]
    msgBody.device = [:]
    msgBody.device[sensor] = [:]
    msgBody.device[sensor][name] = value
    msgBody.event = [:]
    msgBody.event[name] = value
    msgBody.event.device_name = sensor
    msgBody.event.attribute = name
    msgBody.event.value = value

    log.debug("${msgBody}")
    sendEvent(name:name, value:value, descriptionText:msgBody, eventType:"SMART_APP_EVENT", displayed: false)

    def params = [
        // headers: ["Content-Type": "application/json"],
        // uri: "https://api.correl8.me",
        uri: "http://correl8.me:3030",
        path: "/smartthings-ecs",
        body: msgBody
    ]

    try {
        httpPostJson(params) { resp ->
            logMsg = "Logged ${msgBody}: ${resp.status}"
            sendEvent(name:"correl8", value:resp.status, descriptionText:logMsg, data:$resp, eventType:"SMART_APP_EVENT", displayed: false)
            log.info(logMsg)
        }
    } catch (e) {
        logMsg = "Logging to Correl8.me failed: ${e}"
        sendEvent(name:"correl8", value:"error", descriptionText:logMsg, data:$e, eventType:"SMART_APP_EVENT", displayed: true)
        log.warn(logMsg)
    }
}
