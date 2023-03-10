/**
 *  Serre ThingSpeak
 *
 *  Based on "ThingSpeak Logger by florianz
 *
 *  Author: David Mongeau-Petitpas
 *  Date: 2016-10-11
 *
 *  Report data from sensors to ThingSpeak
 *
 */

// Automatically generated. Make future change here.
definition(
    name: "Serre ThingSpeak",
    namespace: "dmongeau/smartthings",
    author: "Nicholas Wilde",
    description: "Log serre events to ThingSpeak",
    category: "Convenience",
    iconUrl: "https://github.com/dmongeau/smartthings/raw/master/smartapps/serre-thingspeak/icon.png",
    iconX2Url: "https://github.com/dmongeau/smartthings/raw/master/smartapps/serre-thingspeak/icon.png")

preferences {
    section("Log devices...") {
    	input "illuminants", "capability.illuminanceMeasurement", title: "Illuminants", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Relative Humidities", required:false, multiple: true
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
    }

    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel ID"
    }

    section ("ThinkSpeak write key...") {
        input "channelKey", "password", title: "Channel Key"
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
	subscribe(illuminants, "illuminance", handleIlluminanceEvent)
	subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(temperatures, "temperature", handleTemperatureEvent)

    updateChannelInfo()
    log.debug state.fieldMap
}

def handleIlluminanceEvent(evt) {
    logField(evt, "illuminace") { it.toString() }
}

def handleHumidityEvent(evt) {
    logField(evt, "humidity") { it.toString() }
}

def handleTemperatureEvent(evt) {
    logField(evt, "temperature") { it.toString() }
}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim().toLowerCase()] = it.key }
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "http://api.thingspeak.com/channels/${channelId}/feed.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
        	log.debug "ThingSpeak data retrieval successful, status = ${response.status}"
            state.channelInfo = response.data?.channel
        }
    }
    
    state.fieldMap = getFieldMap(state.channelInfo)
}

private logField(evt, name, Closure c) {
    def deviceName = evt.displayName.trim().toLowerCase() 
    def fieldNum = state.fieldMap[name]
    if (!fieldNum) {
        log.debug "Channel has no field '${name}'"
        return
    }

    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        } else {
        	log.debug "ThingSpeak logging successful, status = ${response.status}"
        }
    }
}
