/**
 * Event Logger For Splunk
 * This was originally created by Brian Keifer I modified it to work with the Splunk HTTP Event Collector
 *
 * Copyright 2015 Brian Keifer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * Based on https://raw.githubusercontent.com/TheFuzz4/SmartThingsSplunkLogger/master/splunklogger.groovy
 *
 */
definition(
        name: "Splunk HTTP Event Logger",
        namespace: "smartthings.f12.no",
        author: "Anders Sveen",
        description: "Log SmartThings events to a Splunk HTTP Event Collector server",
        category: "Convenience",
        iconUrl: "http://apmblog.dynatrace.com/wp-content/uploads/2014/07/Splunk_thumbnail.png",
        iconX2Url: "http://apmblog.dynatrace.com/wp-content/uploads/2014/07/Splunk_thumbnail.png",
        iconX3Url: "http://apmblog.dynatrace.com/wp-content/uploads/2014/07/Splunk_thumbnail.png")

preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
    section("Log these switches:") {
        input "switches", "capability.switch", multiple: true, required: false
    }
    section("Log these switch levels:") {
        input "levels", "capability.switchLevel", multiple: true, required: false
    }
    section("Log these motion sensors:") {
        input "motions", "capability.motionSensor", multiple: true, required: false
    }
    section("Log these temperature sensors:") {
        input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these humidity sensors:") {
        input "humidities", "capability.relativeHumidityMeasurement", multiple: true, required: false
    }
    section("Log these contact sensors:") {
        input "contacts", "capability.contactSensor", multiple: true, required: false
    }
    section("Log these alarms:") {
        input "alarms", "capability.alarm", multiple: true, required: false
    }
    section("Log these indicators:") {
        input "indicators", "capability.indicator", multiple: true, required: false
    }
    section("Log these CO detectors:") {
        input "codetectors", "capability.carbonMonoxideDetector", multiple: true, required: false
    }
    section("Log these smoke detectors:") {
        input "smokedetectors", "capability.smokeDetector", multiple: true, required: false
    }
    section("Log these water detectors:") {
        input "waterdetectors", "capability.waterSensor", multiple: true, required: false
    }
    section("Log these acceleration sensors:") {
        input "accelerations", "capability.accelerationSensor", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }
    section("Log these music players:") {
        input "musicplayer", "capability.musicPlayer", multiple: true, required: false
    }
    section("Log these power meters:") {
        input "powermeters", "capability.powerMeter", multiple: true, required: false
    }
    section("Log these illuminance sensors:") {
        input "illuminances", "capability.illuminanceMeasurement", multiple: true, required: false
    }
    section("Log these batteries:") {
        input "batteries", "capability.battery", multiple: true, required: false
    }
    section("Log these buttons:") {
        input "button", "capability.button", multiple: true, required: false
    }
    section("Log these voltages:") {
        input "voltage", "capability.voltageMeasurement", multiple: true, required: false
    }
    section("Log these locks:") {
        input "lockDevice", "capability.lock", multiple: true, required: false
    }

    section("Splunk Server") {
        input "splunk_host", "text", title: "Splunk Hostname/IP", required: true
        input "use_ssl", "boolean", title: "Use SSL?", required: true
        input "splunk_port", "number", title: "Splunk Port", required: true
        input "splunk_token", "text", title: "Splunk Authentication Token", required: true
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
    doSubscriptions()
}

def doSubscriptions() {
    subscribe(alarms, "alarm", genericHandler)
    subscribe(codetectors, "carbonMonoxideDetector", genericHandler)
    subscribe(contacts, "contact", genericHandler)
    subscribe(indicators, "indicator", genericHandler)
    subscribe(modes, "locationMode", genericHandler)
    subscribe(motions, "motion", genericHandlerr)
    subscribe(presences, "presence", genericHandler)
    subscribe(relays, "relaySwitch", genericHandler)
    subscribe(smokedetectors, "smokeDetector", genericHandler)
    subscribe(switches, "switch", genericHandler)
    subscribe(levels, "level", genericHandler)
    subscribe(temperatures, "temperature", genericHandler)
    subscribe(waterdetectors, "water", genericHandler)
    subscribe(location, "location", genericHandler)
    subscribe(accelerations, "acceleration", genericHandler)
    subscribe(energymeters, "energy", genericHandler)
    subscribe(musicplayers, "music", genericHandler)
    subscribe(illuminaces, "illuminance", genericHandler)
    subscribe(powermeters, "power", genericHandler)
    subscribe(batteries, "battery", batteryHandler)
    subscribe(button, "button", genericHandler)
    subscribe(voltageMeasurement, "voltage", genericHandler)
    subscribe(lockDevice, "lock", lockHandler)
}

def genericHandler(evt) {
    def jsonMap = [
            event: [
                    date               : evt.date,
                    name               : evt.name,
                    displayName        : evt.displayName,
                    device             : evt.device,
                    deviceId           : evt.deviceId,
                    value              : evt.value,
                    isStateChange      : evt.isStateChange(),
                    id                 : evt.id,
                    description        : evt.description,
                    descriptionText    : evt.descriptionText,
                    installedSmartAppId: evt.installedSmartAppId,
                    isoDate            : evt.isoDate,
                    isDigital          : evt.isDigital(),
                    isPhysical         : evt.isPhysical(),
                    location           : evt.location,
                    locationId         : evt.locationId,
                    unit               : evt.unit,
                    event_source       : evt.source
            ]
    ]

    def ssl = use_ssl.toBoolean()
    def http_protocol

    if (ssl == true) {
        http_protocol = "https"
    } else {
        http_protocol = "http"
    }

    def params = [
            uri    : "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event",
            headers: [
                    'Authorization': "Splunk ${splunk_token}"
            ],
            body   : jsonMap
    ]
    log.debug params
    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        log.debug "Unexpected response error: ${ex.statusCode}"
    }
}

def batteryHandler(evt) {
    log.trace "$evt"
    def json = ""
    json += "{\"event\":"
    json += "{\"date\":\"${evt.date}\","
    json += "\"name\":\"${evt.name}\","
    json += "\"displayName\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"value\":\"${evt.value}\","
    json += "\"isStateChange\":\"${evt.isStateChange()}\","
    json += "\"id\":\"${evt.id}\","
    json += "\"description\":\"${evt.description}\","
    json += "\"descriptionText\":\"${evt.descriptionText}\","
    json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
    json += "\"isoDate\":\"${evt.isoDate}\","
    json += "\"isDigital\":\"${evt.isDigital()}\","
    json += "\"isPhysical\":\"${evt.isPhysical()}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"locationId\":\"${evt.locationId}\","
    json += "\"unit\":\"${evt.unit}\","
    json += "\"source\":\"${evt.source}\",}"
    json += "}"
//log.debug("JSON: ${json}")

    def ssl = use_ssl.toBoolean()
    def http_protocol
    def splunk_server = "${splunk_host}:${splunk_port}"
    def length = json.getBytes().size().toString()
    def msg = parseLanMessage(description)

//log.debug "Use Remote"
//log.debug "Current SSL Value ${use_ssl}"
    if (ssl == true) {
//log.debug "Using SSL"
        http_protocol = "https"
    } else {
//log.debug "Not Using SSL"
        http_protocol = "http"
    }

    def params = [
            uri    : "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event",
            headers: [
                    'Authorization': "Splunk ${splunk_token}"
            ],
            body   : json
    ]
    log.debug params
    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        log.debug "Unexpected response error: ${ex.statusCode}"
    }
}

def lockHandler(evt) {
    log.trace "$evt"
    def json = ""
    json += "{\"event\":"
    json += "{\"date\":\"${evt.date}\","
    json += "\"name\":\"${evt.name}\","
    json += "\"displayName\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"value\":\"${evt.value}\","
    json += "\"isStateChange\":\"${evt.isStateChange()}\","
    json += "\"id\":\"${evt.id}\","
    json += "\"description\":\"${evt.description}\","
    json += "\"descriptionText\":\"${evt.descriptionText}\","
    json += "\"installedSmartAppId\":\"${evt.installedSmartAppId}\","
    json += "\"isoDate\":\"${evt.isoDate}\","
    json += "\"isDigital\":\"${evt.isDigital()}\","
    json += "\"isPhysical\":\"${evt.isPhysical()}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"locationId\":\"${evt.locationId}\","
    json += "\"unit\":\"${evt.unit}\","
    json += "\"source\":\"${evt.source}\",}"
    json += "}"
//log.debug("JSON: ${json}")

    def ssl = use_ssl.toBoolean()
    def http_protocol
    def splunk_server = "${splunk_host}:${splunk_port}"
    def length = json.getBytes().size().toString()
    def msg = parseLanMessage(description)
    def body = msg.body
    def status = msg.status

//log.debug "Use Remote"
//log.debug "Current SSL Value ${use_ssl}"
    if (ssl == true) {
//log.debug "Using SSL"
        http_protocol = "https"
    } else {
//log.debug "Not Using SSL"
        http_protocol = "http"
    }

    def params = [
            uri    : "${http_protocol}://${splunk_host}:${splunk_port}/services/collector/event",
            headers: [
                    'Authorization': "Splunk ${splunk_token}"
            ],
            body   : json
    ]
    log.debug params
    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
