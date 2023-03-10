import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

// Massive lookup tree
@Field CAPABILITY_MAP = [
    "levels": [
        name: "Switch Level",
        capability: "capability.switchLevel",
        attributes: [
            "level"
        ],
        action: "actionLevel"
    ],
    "relaySwitch": [
        name: "Relay Switch",
        capability: "capability.relaySwitch",
        attributes: [
            "switch"
        ],
        action: "actionOnOff"
    ],
    "switches": [
        name: "Switch",
        capability: "capability.switch",
        attributes: [
            "switch"
        ],
        action: "actionOnOff"
    ],
    "temperatureSensors": [
        name: "Temperature Measurement",
        capability: "capability.temperatureMeasurement",
        attributes: [
            "temperatureMeasurement"
        ],
        action: "updateTemp"
    ],    
    "humiditySensors": [
        name: "Relative Humidity Measurement",
        capability: "capability.relativeHumidityMeasurement",
        attributes: [
            "humidity"
        ],
        action: "updateHumidity"
    ]    
    
    
]

definition(
    name: "MQTT Bridge",
    namespace: "magimat",
    author: "Mathieu Girard",
    description: "_A bridge between SmartThings and MQTT",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@3x.png"
)

preferences {
    section ("Input") {
        CAPABILITY_MAP.each { key, capability ->
            input key, capability["capability"], title: capability["name"], multiple: true, required: false
        }
    }

    section ("Bridge") {
        input "bridge", "capability.notification", title: "Notify this Bridge", required: true, multiple: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    runEvery15Minutes(initialize)
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Unsubscribe from all events
    unsubscribe()
    // Subscribe to stuff
    initialize()
}


def initialize() {

    // Subscribe to events from the bridge
    subscribe(bridge, "message", bridgeHandler)

    // Update the bridge
    updateSubscription()
}

// Update the bridge"s subscription
def updateSubscription() {
    def attributes = [:]
    CAPABILITY_MAP.each { key, capability ->
        capability["attributes"].each { attribute ->
            if (!attributes.containsKey(attribute)) {
                attributes[attribute] = []
            }
            settings[key].each {device ->
                attributes[attribute].push(device.name)
            }
        }
    }
    def json = new groovy.json.JsonOutput().toJson([
        path: "/subscribe",
        body: [
            devices: attributes
        ]
    ])

    log.debug "Updating subscription: ${json}"

    bridge.deviceNotification(json)
}

// Receive an event from the bridge
def bridgeHandler(evt) {
    def json = new JsonSlurper().parseText(evt.value)
    log.debug "Received device event from bridge: ${json}"

    // @NOTE this is stored AWFUL, we need a faster lookup table
    // @NOTE this also has no fast fail, I need to look into how to do that
    CAPABILITY_MAP.each { key, capability ->
        if (capability["attributes"].contains(json.type)) {
            settings[key].each {device ->
                if (device.name == json.name) {
                    if (capability.containsKey("action")) {
                        def action = capability["action"]
						// Yes, this is calling the method dynamically
                        "$action"(device, json.type, json.value)
                    }
                }
            }
        }
    }
}




def actionOnOff(device, attribute, value) {
    if (value == "off") {
        device.setOff()
    } else if (value == "on") {
        device.setOn()
    }
}

def actionLevel(device, attribute, value) {
        device.setOn()
}

def updateTemp(device, attribute, value) {
		device.updateTemp(value)
}

def updateHumidity(device, attribute, value) {
		device.updateHumidity(value)
}



