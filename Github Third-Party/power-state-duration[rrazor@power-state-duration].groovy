/**
 * Power Meter State Duration SmartApp for SmartThings
 * Copyright ©2016, Matt Gray <https://github.com/rrazor>.
 */

definition(
    name: "Power Meter State Duration",
    namespace: "rrazor",
    author: "Matt Gray",
    description: "Log device state changes and state duration using a power meter.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Record from power meter:") {
        input(name: "d_meter", type: "capability.powerMeter", title: "Power Meter", required: true, multiple: true)
    }
    section("Consider ON above threshold:") {
        input(name: "c_threshold", type: "number", title: "ON Threshold (Watts)", required: true)
    }
    section("API information:") {
        input("apiUrl", "text", title: "POST URL")
        input("apiAuthHeader", "text", title: "HTTP x-api-key: header value")
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
    // A map of device IDs => maps
    state.lastChange = [:]
    subscribe(d_meter, "power", "onDeviceEvent")
}

def onDeviceEvent(evt) {
    def eventJson = eventToJson(evt)
    if (!eventJson.power) {
        return
    }

    def deviceId = eventJson.device
    def deviceOn = (eventJson.power > c_threshold)
    def unixTime = eventJson.unixTime

    if (!state.lastChange.containsKey(deviceId)) {
        state.lastChange[deviceId] = [deviceOn: deviceOn, unixTime: unixTime]
        return
    }

    def lastChange = state.lastChange[deviceId]
    def unixTimeElapsed = unixTime - lastChange.unixTime

    if (unixTimeElapsed < 0) {
        // This event is old and arrived out-of-order. Discard it.
        return
    }
    else if (lastChange.deviceOn == deviceOn) {
        // Device hasn't changed state. Ignore.
        return
    }

    log.trace("onDeviceEvent: ${evt.device.label}.deviceOn changed from ${lastChange.deviceOn} to ${deviceOn}, elapsed: ${unixTimeElapsed}")

    // The "type" of duration is the opposite of the current state
    if (deviceOn) {
        eventJson.type = "off"
    }
    else {
        eventJson.type = "on"
    }

    eventJson.unixTimeElapsed = unixTimeElapsed
    publishEvent(eventJson)

    state.lastChange[deviceId] = [deviceOn: deviceOn, unixTime: unixTime]
}

def publishEvent (eventJson) {
    def params = [uri: apiUrl, headers: ["x-api-key": apiAuthHeader], body: eventJson]

    try {
        httpPostJson(params) { resp -> }
    }
    catch (e) {
    }
}

private eventToJson (evt) {
    def device = evt.device
    if (! device) {
        return
    }

    def json = [:]

    json['device']      = device.id
    json['deviceLabel'] = device.label
    json['name']        = evt.name

    if (json.name == "power") {
        def p = device.currentState('power')
        json['unixTime']  = p?.date.getTime()
        json['power']     = p?.value.toDouble()
    }

    return json
}