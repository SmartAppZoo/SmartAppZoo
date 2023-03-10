/**
 *  Log Bedroom Temp
 *
 *  Author: steve.sell@gmail.com
 *  Date: 2013-12-02
 */

// Automatically generated. Make future change here.
definition(
    name: "Log to ThingSpeak",
    namespace: "steve28",
    author: "steve.sell@gmail.com",
    description: "This will log the temperature of my bedroom Multi",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    section("Log devices...") {
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
        input "humidities", "capability.relativeHumidityMeasurement", title: "Humidity", required:false, multiple: true
        input "contacts", "capability.contactSensor", title: "Contacts", required: false, multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
        input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
        input "switches", "capability.switch", title: "Switches", required: false, multiple: true
        input "batteries", "capability.battery", title: "Battery Levels", required: false, multiple: true

    }
    
    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel id"
    }
    
    section ("ThinkSpeak write key...") {
        input "channelKey", "text", title: "Channel key"
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
    subscribe(humidities, "humidity", handleHumidityEvent)
    subscribe(contacts, "contact", handleContactEvent)
    subscribe(accelerations, "acceleration", handleAccelerationEvent)
    subscribe(motions, "motion", handleMotionEvent)
    subscribe(switches, "switch", handleSwitchEvent)
    subscribe(batteries, "battery", handleBatteryEvent)

    updateChannelInfo()
    log.debug state.fieldMap
    //state.lastUpdate = now()
    //log.debug "initializing lastUpdate: ${state.lastUpdate}"
}






def handleTemperatureEvent(evt) {
    logField(evt) { it.toString() }
}

def handleHumidityEvent(evt) {
    logField(evt) { it.toString() }
}

def handleContactEvent(evt) {
    logField(evt) { it == "open" ? "1" : "0" }
}

def handleAccelerationEvent(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}

def handleMotionEvent(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}

def handleSwitchEvent(evt) {
    logField(evt) { it == "on" ? "1" : "0" }
}

def handleBatteryEvent(evt) {
	logField(evt) { it.toString() }
}


private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    log.debug fieldMap
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
            state.channelInfo = response.data?.channel
        }
    }
    
    state.fieldMap = getFieldMap(state.channelInfo)
}

private logField(evt, Closure c) {
   	
    def deviceName = evt.displayName.trim()+"-"+evt.name
    def fieldNum = state.fieldMap[deviceName]
    if (!fieldNum) {
        log.debug "Device '${deviceName}' has no field"
        return
    }
    
    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "http://api.thingspeak.com/update?key=${channelKey}&${fieldNum}=${value}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
}
