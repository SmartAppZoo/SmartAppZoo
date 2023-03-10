/**
 *  ThermostatLogger. Logs thermostat temperature and operating state to Xively feed.
 *  See http://build.smartthings.com/projects/xively/ for more information.
 *
 *  Author: @kernelhack
 *  Date: 2014-01-21
 */
preferences {
    section("Configure") {
        input "xi_apikey", "text", title: "Xively API Key"
        input "xi_feed", "number", title: "Xively Feed ID"
        input "thermostat1", "capability.thermostat ", title: "Select thermostat"
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
    // Subscribe to attributes, devices, locations, etc.
    subscribe(thermostat1, "temperature", handleThermostatTemperature)
    subscribe(thermostat1, "thermostatOperatingState", handleThermostatOperatingState)
}

def parseHttpResponse(response) {
    log.debug "HTTP Response: ${response}"
}

def writeChannelData(feed, channel, value) {
    def uri = "https://api.xively.com/v2/feeds/${feed}.json"
    def json = "{\"version\":\"1.0.0\",\"datastreams\":[{\"id\":\"${channel}\",\"current_value\":\"${value}\"}]}"

    def headers = [
        "X-ApiKey" : "${xi_apikey}"
    ]

    def params = [
        uri: uri,
        headers: headers,
        body: json
    ]

    httpPutJson(params) {response -> parseHttpResponse(response)}
}

// Handle temperature event
def handleThermostatTemperature(evt) {
    log.debug "Tempreature event: $evt.value"
    writeChannelData(xi_feed, "Temperature", evt.value)
}

// Handle thermostatOperatingState event
def handleThermostatOperatingState(evt) {
    log.debug "OperatingState event: $evt.value"

    def opState = 0
    if (evt.value == "heating") {
        opState = 1
    } else if (evt.value == "cooling") {
        opState = -1
    }

    writeChannelData(xi_feed, "OperatingState", opState)
}