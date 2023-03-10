/**
 *  Numerous Power Logger
 *
 *  Copyright 2015 Caleb Packard
 *  Logs an energy meter's power events to a Numerous metric
 *
 */
 definition(
    name: "Numerous Power Logger",
    namespace: "",
    author: "Caleb Packard",
    description: "Logs an energy meter's power events to a Numerous metric",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "page1", title: "Enter your Numerous API key", nextPage: "page2", uninstall: true) {
        section {
            input(name: "numerousApiKey", type: "text", required: true, title: "Please enter your Numerous API Key")
        }
        
        log.debug "Key entered was ${numerousApiKey}"
    }

    page(name: "page2", title: "Choose your metric and energy meter", install: true, uninstall: true)
}

def page2() {
    def optionData = loadNumerousMetrics(numerousApiKey)

    return dynamicPage(name: "page2") {
        section("Choose your metric and energy meter") {
                input(name: "metric", type: "enum", title: "Choose a Numerous metric to update", options: optionData)
                input "meter", "capability.energyMeter"
        }
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
    subscribe(meter, "power", powerEventHandler)
}

def powerEventHandler(evt) {
    log.debug "Power event value is $evt.value"
    postToNumerous (metric, evt.value)
}

private loadNumerousMetrics(numerousApiKey)
{
    log.debug "Numerous API key ${numerousApiKey}"
    def encodedApiKey = (numerousApiKey + ":").encodeAsBase64().toString()

    def authHeader = "Basic ${encodedApiKey}"
    
    log.debug "Auth Header: ${authHeader}"

    def metrics = [:]

    try {
        httpGet(uri: "https://api.numerousapp.com/v2/users/me/metrics", headers: ["Authorization": authHeader]) { response ->
            if (response?.data?.metrics) {
                response.data.metrics.each {metric ->
                    metrics.put(metric.id, metric.label)
                }
                log.debug "Metrics found: ${metrics}"
                state.encodedApiKey = encodedApiKey
            }
            else {
                log.debug "Response had no data, response: ${response}"
            }
        }
    }
    catch(e) {
        log.debug "Exception from Numerous was ${e}"
    }
    
    return metrics
}

private postToNumerous(metricId, val) {
    def postBody = "{\"value\": ${val}}"
    def authHeader = "Basic ${state.encodedApiKey}"
    
    log.debug "Post Body: ${postBody}"
    log.debug "Auth Header: ${authHeader}"
    
    def result = null
    
    try {
        httpPost(uri: "https://api.numerousapp.com/v1/metrics/${metricId}/events", body: postBody, headers: ["Authorization": authHeader]) {response -> result = response}
    }
    catch(e) {
        log.debug e
    }
    log.debug "Response=${result.data}"
}