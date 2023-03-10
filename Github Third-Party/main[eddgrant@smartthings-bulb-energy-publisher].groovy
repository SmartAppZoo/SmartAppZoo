/**
 *  SmartThings Bulb Energy Publisher
 *
 *  Copyright 2022 Edd Grant
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
    name: "BulbEnergyPublisher",
    namespace: "eddgrant",
    author: "Edd Grant",
    description: "Subscribes to energy events emitted from your Bulb Energy Meter and publishes the values to an InfluxDB API endpoint.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("BulbEnergyPublisher") {
        input "bulbEnergyMonitor", "capability.energyMeter", required: true, title: "Select your Bulb Energy Monitor"
    }
    section("InfluxDB API") {
        input "metricsEndpoint", "string", required: true, title: "InfluxDB API Endpoint."
        input "influxDBOrgName", "string", required: true, title: "InfluxDB Organisation Name."
        input "influxDBBucketName", "string", required: true, title: "InfluxDB Bucket Name."
        input "influxDBAuthorisationHeader", "string", required: true, title: "InfluxDB Authorisation Header."
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
    subscribe(bulbEnergyMonitor, "energy", energyEventPublishedHandler)
    subscribe(bulbEnergyMonitor, "gasMeter", gasEventPublishedHandler)
}

def gasEventPublishedHandler(evt) {
    log.debug("gasMeter event - event.value: $evt.value")
    publishEvent(evt)
}

def energyEventPublishedHandler(evt) {
    log.debug "Energy event - event.value: $evt.value"
    publishEvent(evt)
}

def publishEvent(evt) {
    debugEvent(evt)
    def data = parseJson(evt.data)

    def uri = "${metricsEndpoint}?bucket=${influxDBBucketName}&org=${influxDBOrgName}&precision=ms"
    log.debug("URI is: ${uri}")
    def params = [
        uri: uri,
        body: asInfluxDbLineProtocol(evt),
        headers: [
            Authorization: influxDBAuthorisationHeader
        ]
    ]
    try {
        httpPostJson(params) { resp ->
            log.debug "HTTP request made to ${metricsEndpoint}. Response status code: ${resp.status}"
        }
    } catch (e) {
        log.error "Unable to make HTTP Request: ${e}"
    }
}

def debugEvent(evt) {
    log.debug("Event Date: $evt.date")
    log.debug("Event Location: $evt.location")
    log.debug("Event Name: $evt.name")
    log.debug("Event Numeric Value: $evt.numericValue")
    log.debug("Event Source: $evt.source")
    log.debug("Event Unit: $evt.unit")
    log.debug("Event digital: $evt.digital")
    log.debug("Event physical: $evt.physical")
    log.debug("Event stateChange: $evt.stateChange")
}

def asInfluxDbLineProtocol(event) {
    log.debug("Event Name: ${event.name}")
    def measurementName = null
    if(event.name == "energy") {
        measurementName = "electricity"
    }
    if(event.name == "gasMeter") {
        measurementName = "gas"
    }
    log.debug("Measurement Name: ${measurementName}")
    final String epochInMilliseconds = event.date.time
    final String line = """${measurementName},location=${event.location},source=${event.source},unit=${event.unit},digital=${event.digital},physical=${event.physical} value=${event.numericValue} ${epochInMilliseconds}"""
    log.debug("Line: ${line}")
    return line
}
