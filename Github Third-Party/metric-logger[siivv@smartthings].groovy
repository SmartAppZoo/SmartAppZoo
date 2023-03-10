/**
 *  Event Logger
 *
 *  Copyright 2016 Nic Jansma
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
    name: "metric-logger",
    namespace: "siivv",
    author: "siivv",
    description: "log all ST events to aws dynamodb",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@3x.png")

preferences {
    section("Log these presence sensors:") {
        input "presences", "capability.presenceSensor", multiple: true, required: false
    }
    section("Log these switches:") {
        input "switches", "capability.switch", multiple: true, required: false
    }
    section("Log these locks:") {
        input "locks", "capability.lock", multiple: true, required: false
    }
    section("Log these switch levels:") {
        input "levels", "capability.switchLevel", multiple: true, required: false
    }
    section("Log these motion sensors:") {
        input "motions", "capability.motionSensor", multiple: true, required: false
    }
    section("Log these garage doors:") {
        input "garages", "capability.garageDoorControl", multiple: true, required: false
    }
    section("Log these temperature sensors:") {
        input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false
    }
    section("Log these thermostats:") {
        input "thermostats", "capability.thermostat", multiple: true, required: false
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
    section("Log these power meters:") {
        input "powermeters", "capability.powerMeter", multiple: true, required: false
    }
    section("Log these energy meters:") {
        input "energymeters", "capability.energyMeter", multiple: true, required: false
    }

    section ("HTTP Server") {
        input "httpUrl", "text", title: "HTTP URL";
        input "xApiKey", "text", title: "'x-api-key' header";
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}";
    initialize();
}

def updated() {
    log.debug "Updated with settings: ${settings}";
    unsubscribe();
    unschedule();
    initialize();
}

def initialize() {
    unsubscribe();
    unschedule();
    doSubscriptions();
    runEvery15Minutes(metricPollingHandler);
}

def doSubscriptions() {
    subscribe(contacts, "contact", metricHandler);
    subscribe(indicators, "indicator", metricHandler);
    subscribe(modes, "locationMode", metricHandler);
    subscribe(motions, "motion", metricHandler);
    subscribe(presences, "presence", metricHandler);
    subscribe(relays, "relaySwitch", metricHandler);
    subscribe(smokedetectors, "smokeDetector", metricHandler);
    subscribe(switches, "switch", metricHandler);
    subscribe(locks, "lock", metricHandler);
    subscribe(levels, "level", metricHandler);
    subscribe(garages,"door", metricHandler);
    subscribe(temperatures, "temperature", metricHandler);
    subscribe(humidities, "humidity", metricHandler);
    subscribe(thermostats, "thermostatSetpoint", metricHandler);
    subscribe(thermostats, "thermostatMode", metricHandler);
    subscribe(thermostats, "thermostatFanMode", metricHandler);
    subscribe(thermostats, "thermostatOperatingState", metricHandler);
    subscribe(waterdetectors, "water", metricHandler);
    subscribe(location, "location", metricHandler);
    subscribe(accelerations, "acceleration", metricHandler);
    subscribe(powermeters, "power", metricHandler);
    subscribe(energymeters, "energy", metricHandler);
}

def metricPollingHandler() {
    def mDevice = [:];
    settings.thermostats.eachWithIndex { dev,i ->
        mDevice = [:];
        mDevice['device'] = dev.displayName;
        mDevice['deviceId'] = dev.id;
        mDevice['location'] = dev.currentValue("location");
        mDevice['temperature'] = dev.currentValue("temperature");
        mDevice['humidity'] = dev.currentValue("humidity");
        mDevice['thermostatSetpoint'] = dev.currentValue("thermostatSetpoint");
        mDevice['thermostatMode'] = dev.currentValue("thermostatMode");
        mDevice['thermostatFanMOde'] = dev.currentValue("thermostatFanMode");
        mDevice['thermostatOperatingState'] = dev.currentValue("thermostatOperatingState");
        //log.debug("processed ${dev.displayName}");
        logMetric(metricJSONBuilder(mDevice));
    }
    settings.temperatures.eachWithIndex { dev,i ->
        if (!dev.hasCapability("Thermostat")) {
            mDevice = [:];
            mDevice['device'] = dev.displayName;
            mDevice['deviceId'] = dev.id;
            mDevice['temperature'] = dev.currentValue("temperature");
            logMetric(metricJSONBuilder(mDevice));
        }
    }
}

def metricJSONBuilder(mDevice) {
    def json = "{";

    mDevice.each { mkey, mvalue ->
        json += "\"${mkey}\":\"${mvalue}\",";
    }

    json += "\"location\":\"${location.name}\",";
    json += "\"event\":\"smartthings\"";
    json += "}"    
    log.debug("metricJSONBuilder: ${json}");

    return json;
}

def metricHandler(evt) {
    def json = "{"
    json += "\"device\":\"${evt.device}\","
    json += "\"deviceId\":\"${evt.deviceId}\","
    json += "\"${evt.name}\":\"${evt.value}\","
    json += "\"location\":\"${evt.location}\","
    json += "\"event\":\"smartthings\""
    json += "}"
    log.debug("metricHandler: ${json}");
    logMetric(json);
}

def logMetric(json) {
    def params = [
        uri: httpUrl,
        headers: [
            "x-api-key": xApiKey,
            "content-type": "application/json"
        ],
        body: json
    ]

    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        if (ex.statusCode != 200) {
            log.debug "Unexpected response error: ${ex.statusCode}"
            log.debug ex
            log.debug ex.response.contentType
        }
    }
}

/*
    log.debug("------------------------------");
    log.debug("date: ${evt.date}");
    log.debug("name: ${evt.name}");
    log.debug("displayName: ${evt.displayName}");
    log.debug("device: ${evt.device}");
    log.debug("deviceId: ${evt.deviceId}");
    log.debug("value: ${evt.value}");
    log.debug("isStateChange: ${evt.isStateChange()}");
    log.debug("id: ${evt.id}");
    log.debug("description: ${evt.description}");
    log.debug("descriptionText: ${evt.descriptionText}");
    log.debug("installedSmartAppId: ${evt.installedSmartAppId}");
    log.debug("isoDate: ${evt.isoDate}");
    log.debug("isDigital: ${evt.isDigital()}");
    log.debug("isPhysical: ${evt.isPhysical()}");
    log.debug("location: ${evt.location}");
    log.debug("locationId: ${evt.locationId}");
    log.debug("source: ${evt.source}");
    log.debug("unit: ${evt.unit}");
*/
