definition(
    name: "Cloud Datastore Collector",
    namespace: "mholtzscher",
    author: "Michael Holtzscher",
    description: "Log SmartThings events to Google Cloud Datastore",
    category: "Convenience",
    iconUrl: "https://lh3.googleusercontent.com/-t4xyiyz5qx0/UzEkRjbrWII/AAAAAAAAOWU/Il9T4wyApJ85qIzDJRQHEvgLEiDSFuDvACHM/s251/CloudPlatform_128px_Retina.png",
    iconX2Url: "https://lh3.googleusercontent.com/-t4xyiyz5qx0/UzEkRjbrWII/AAAAAAAAOWU/Il9T4wyApJ85qIzDJRQHEvgLEiDSFuDvACHM/s251/CloudPlatform_128px_Retina.png",
    iconX3Url: "https://lh3.googleusercontent.com/-t4xyiyz5qx0/UzEkRjbrWII/AAAAAAAAOWU/Il9T4wyApJ85qIzDJRQHEvgLEiDSFuDvACHM/s251/CloudPlatform_128px_Retina.png")


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

    section("Collection Server") {
        input "collector_host", "text", title: "Collector URL"
        input "collector_key", "text", title: "Collector API Key"
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
    subscribe(alarms, "alarm", eventHandler)
    subscribe(codetectors, "carbonMonoxideDetector", eventHandler)
    subscribe(contacts, "contact", eventHandler)
    subscribe(indicators, "indicator", eventHandler)
    subscribe(modes, "locationMode", eventHandler)
    subscribe(motions, "motion", eventHandler)
    subscribe(presences, "presence", eventHandler)
    subscribe(relays, "relaySwitch", eventHandler)
    subscribe(smokedetectors, "smokeDetector", eventHandler)
    subscribe(switches, "switch", eventHandler)
    subscribe(levels, "level", eventHandler)
    subscribe(temperatures, "temperature", eventHandler)
    subscribe(waterdetectors, "water", eventHandler)
    subscribe(location, "location", eventHandler)
    subscribe(accelerations, "acceleration", eventHandler)
    subscribe(energymeters, "power", eventHandler)
    subscribe(location, "routineExecuted", eventHandler)
    subscribe(location, "sunset", eventHandler)
    subscribe(location, "sunrise", eventHandler)
}

def eventHandler(evt) {
    def json = "{"
    json += "\"date\":\"${evt.date}\","
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
    json += "\"source\":\"${evt.source}\","
    json += "\"program\":\"SmartThings\""
    json += "}"
    log.debug("JSON: ${json}")

    def params = [
        uri: "${collector_host}",
        body: json,
        headers: ["API_KEY": "${collector_key}"]
    ]
    try {
        httpPostJson(params)
    } catch (groovyx.net.http.HttpResponseException ex) {
        log.debug "Unexpected response error: ${ex.statusCode}"
    }
}
