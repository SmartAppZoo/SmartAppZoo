/**
 *  TemperatureLogger. Writes temperature data to Xively datastream.
 *  See http://build.smartthings.com/projects/xively/ for more information.
 *
 *  Author: @kernelhack
 *  Date: 2014-01-01
 */
preferences {
    section("Configure") {
        input "xi_apikey", "text", title: "Xively API Key"
        input "xi_feed", "number", title: "Xively Feed ID"
        input "xi_chan", "text", title: "Xively Channel Name"
        input "temperature1", "capability.temperatureMeasurement", title: "Which temperature"
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    // Subscribe to attributes, devices, locations, etc.
    subscribe(temperature1.temperature)

    // schedule cron job to run every 5 minutes
    schedule("0 0/5 * * * ?", cronJob)
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

// Post current temperature to the remote web service
def cronJob() {
    def currentTemp = temperature1.currentValue("temperature")
    writeChannelData(xi_feed, xi_chan, currentTemp)
}

// Handle temperature event
def temperature(evt) {
    log.debug "Tempreature event: $evt.value"
    //writeChannelData(xi_feed, xi_chan, evt.value)
}
