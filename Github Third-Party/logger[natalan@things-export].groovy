/**
 *  SmartThings to ThingSpeak Logger
 *
 *  Author: Andrei Zharov
 *  Date: 01-02-2015
 *
 */

// Automatically generated. Make future change here.
definition(
        name: "SmartThings to ThingSpeak Logger",
        namespace: "belmass@gmail.com",
        author: "Andrei Zharov",
        description: "Log various events to ThingSpeak",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Log devices...") {
        input "temperature", "capability.temperatureMeasurement", title: "Select temperature sensors", required: false, multiple: true
        input "lock", "capability.lock", title: "Select locks", required: false, multiple: true
        input "contact", "capability.contactSensor", title: "Select contacts", required: false, multiple: true
        input "motion", "capability.motionSensor", title: "Select motions", required: false, multiple: true
        input "switches", "capability.switch", title: "Select Switches", required: false, multiple: true
        input "battery", "capability.battery", title: "Select batteries", required: false, multiple: true
        input "presence", "capability.presence", title: "Select presence sensors", required: false, multiple: true
    }

    section("ThingSpeak Info") {
        input "channelKey", "text", title: "ThingSpeak Channel Key"
        input "channelId", "number", title: "ThingSpeak Channel id"
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
    subscribe(temperature, "temperature", handler)
    subscribe(contact, "contact", handler)
    subscribe(motion, "motion", handler)
    subscribe(switches, "switch", handler)
    subscribe(lock, "lock", handler)
    subscribe(battery, "battery", handler)
    subscribe(presence, "presence", handler)

    def deviceData = allDeviceData()
    log.debug "deviceData"
    log.debug deviceData
    updateChannelInfo()
    log.debug state.fieldMap
}

def handler(e) {
    log.debug "event happened $e.description"
}

def getDeviceData(device, type) {
    [
            type: type,
            device: device.id,
            name: device.displayName,
            value: getDeviceValue(device, type)
    ]
}

def getDeviceFieldMap() {
    [
            lock: "lock",
            "switch": "switch",
            contact: "contact",
            presence: "presence",
            temperature: "temperature",
            humidity: "humidity",
            motion: "motion",
            water: "water",
            power: "power",
            energy: "energy",
            battery: "battery"
    ]
}

def getDeviceValue(device, type) {
    def field = getDeviceFieldMap()[type]
    def value = "n/a"
    try {
        value = device.respondsTo("currentValue") ? device.currentValue(field) : device.value
    } catch (e) {
        log.error "Device $device ($type) does not report $field properly. This is probably due to numerical value returned as text"
    }

    return "${roundNumber(value)}"
}

def allDeviceData() {
    def data = []
    temperature?.each{data << getDeviceData(it, "temperature")}
    contact?.each{data << getDeviceData(it, "contact")}
    motion?.each{data << getDeviceData(it, "motion")}
    switches?.each{data << getDeviceData(it, "switch")}
    lock?.each{data << getDeviceData(it, "lock")}
    battery?.each{data << getDeviceData(it, "battery")}
    presence?.each{data << getDeviceData(it, "presence")}

    return data
}

def roundNumber(num) {
    if (!roundNumbers || !"$num".isNumber()) return num
    if (num == null || num == "") return "n/a"
    else {
        try {
            return "$num".toDouble().round()
        } catch (e) {return num}
    }
}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
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
    def deviceName = evt.displayName.trim()
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
