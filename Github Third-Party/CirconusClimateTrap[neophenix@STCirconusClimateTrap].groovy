/**
 *  Circonus Climate Trap
 *
 *  Copyright 2017 Brian Clapper
 *
 *  This will send your climate sensor data to a Circonus HTTPTrap check every minute
 *  at 30 seconds past the minute, allowing you to trend, analyze, and alert on potential
 *  issues in your home
 */
 
// Below is the Data Submission URL from your Circonus HTTPTrap check
// Fill this out before saving / publishing / installing this app
// NOTE: change the https to http if copying from the Circonus UI as you can't import
// self signed certs into smartapps
def getCirconusTrapUrl() {
    return ""
}

def isDebug() {
    return false
}
 
definition(
    name: "Circonus Climate Trap",
    namespace: "neophenix",
    author: "Brian Clapper",
    description: "App that will send data to a Circonus HTTPTrap check with climate sensor data (temp, uv, humidity, lux)",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
    section("Temperature Sensors") {
        input "tempsensors", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Illuminance Sensors") {
        input "luxsensors", "capability.illuminanceMeasurement", multiple: true, required: false
    }
    section("Relative Humidity Sensors") {
        input "humiditysensors", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("UV Index Sensors") {
        input "uvsensors", "capability.ultravioletIndex", multiple: true, required: false
    }
    section("Thermostats") {
        input "thermostats", "capability.thermostat", multiple: true, required: false
    }
}

def buildSensorData() {
    def resp = [:]
    tempsensors.each {
        resp[it.displayName] = [:]
        resp[it.displayName]["temperature"] = it.currentValue("temperature")
    }
    
    luxsensors.each {
        if (!resp[it.displayName]) {
            resp[it.displayName] = [:]
        }
        resp[it.displayName]["lux"] = it.currentValue("illuminance")
    }
    
    humiditysensors.each {
        if (!resp[it.displayName]) {
            resp[it.displayName] = [:]
        }
        resp[it.displayName]["humidity"] = it.currentValue("humidity")
    }
    uvsensors.each {
        if (!resp[it.displayName]) {
            resp[it.displayName] = [:]
        }
        resp[it.displayName]["uvindex"] = it.currentValue("ultravioletIndex")
    }
    thermostats.each {
        if(!resp[it.displayName]) {
            resp[it.displayName] = [:]
        }
        resp[it.displayName]["state"] = it.currentValue("thermostatOperatingState")
    }
    return resp
}

def trapHandler() {
    debug("running trap handler")
    if (getCirconusTrapUrl() != "" && getCirconusTrapUrl() != null) {
        def data = buildSensorData()
        def params = [
            uri: getCirconusTrapUrl(),
            body: data
        ]
        
        debug("sending data $params.body to $params.uri")
        try {
            httpPostJson(params) { resp ->
                debug("response: ${resp.data}")
            }
        }
        catch (e) {
            debug("Exception $e")
        }
    }
}

def installed() {
    debug("Installed with settings: ${settings}")
    initialize()
}

def updated() {
    debug("Updated with settings: ${settings}")
    unsubscribe()
    initialize()
}

def initialize() {
    schedule("30 * * * * ?", trapHandler)
}

def debug(str) {
    if (isDebug()) {
        log.debug(str)
    }
}
