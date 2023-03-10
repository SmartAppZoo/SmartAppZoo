/**
 *  ThermostatLogger. Logs thermostat temperature and operating state to Xively feed.
 *  See http://build.smartthings.com/projects/xively/ for more information.
 *
 *  Author: @kernelhack
 *  Date: 2014-01-21
 */

// Automatically generated. Make future change here.
definition(
    name: "Log to Xively",
    namespace: "docwisdom",
    author: "Brian Critchlow",
    description: "Logging thermostat to Xively",
    category: "Green Living",
    iconUrl: "https://mbed.org/media/thumbs/f5/1b/f51b2107e58df9cffd95cfdec1a9bdec.jpg",
    iconX2Url: "https://mbed.org/media/thumbs/f5/1b/f51b2107e58df9cffd95cfdec1a9bdec.jpg")

preferences {
    section("Configure") {
        input "xi_apikey", "text", title: "Xively API Key"
        input "xi_feed", "number", title: "Xively Feed ID"
        input "thermostat1", "capability.thermostat", multiple: true, title: "Select thermostats" 
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
    subscribe(location, "mode", handlePeriodic)
    handlePeriodic()
}

// read device info every hour, push data
def handlePeriodic() {
    log.debug "polling thermostats"
	thermostat1.each {
    	
        writeChannelData(xi_feed, "Temperature_" + it.displayName, it.currentValue("temperature"))
        
        def mode = it.currentValue("thermostatMode")
        
		def opState = 0
    	if (mode == "heating") {
        	opState = 1
    	} else if (mode == "cooling") {
        	opState = -1
    	}
    	writeChannelData(xi_feed, "OperatingState_" + it.displayName, opState)
    }
    
    // run again in 10 min
    runIn(60*10, handlePeriodic)
}

def parseHttpResponse(response) {
    log.debug "HTTP Response: ${response}"
}

def writeChannelData(feed, channel, value) {
	channel = channel.replaceAll(' ', '')
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
    writeChannelData(xi_feed, "Temperature_" + evt.device.displayName, evt.value)
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

    writeChannelData(xi_feed, "OperatingState_" + evt.device.displayName, opState)
}
