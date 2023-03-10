/**
 *  Greenlight Driver
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.transform.Field



definition(
    name: "Greenlight Driver",
    namespace: "janusnetworks",
    author: "Kevin Corbin",
    description: "A driver to monitor Smartthings with www.greenlightcontrol.com",
    category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Connections/Cat-Connections@3x.png"
)

// SmartApp Installation Screen
preferences {
    section("Greenlight Setup"){
    input "name", "text", "title": "Name for this System/Job", multiple: false, required: true
  	input "key", "text", "title": "Your dealer API key for Greenlight", multiple: false, required: true
  }

    section ("Input") {
        CAPABILITY_MAP.each { key, capability ->
            input key, capability["capability"], title: capability["name"], multiple: true, required: false
        }
    } 
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    

    initialize()
}

// registers hub with greenlight engine
def registerHub() {
	def hub = location.hubs[0]
    
    log.debug("Registering System: ${hub}")
    def name = "$settings.name".toString()
    def system_id = generateSystemId(hub)
	def data = [
            "ip": hub.getLocalIP(),
        	"ctl_mfg": "Samsung",
			"device": [uniqueid: "0",
	                 status: "Ok",
                     dev_name: hub.getName(),
                     firmware: hub.getFirmwareVersionString()
                     ],
            "job_name": name,
            "system_id": generateSystemId(hub),
            "mod_ver": "0.1",
            "boot_flag": 0
            
        ]
    log.debug("Data is: ${data}")
	sendPost("/api/v1/system", data)
    
}

// register/update device with greenlight, called from event handlers
def registerDevice(dev) {
	log.debug("Registering Device with Greenlight: ${dev}")
	def name = "$settings.name".toString()
	def hub = dev.getHub()

	def system_id = generateSystemId(hub)
	def data = [
            "job_name": name,
            "system_id": generateSystemId(hub),
           	"devices": [
            			[uniqueid: getDevId(dev),
	                     status: "Ok",
                         dev_name: dev.getDisplayName(),
                         firmware: hub.getFirmwareVersionString(),
                         note: dev.latestValue("switch")
                     	]
                        ],
           
        ]
    log.debug("Data is: ${data}")
	sendPost("/api/v1/devices", data)
}
              

// Receive an event from a device
def inputHandler(evt) {
	device = evt.getDevice()
	log.debug "Got Event: ${device}"
    registerDevice(device)
    // bridge.deviceNotification(json)
}

def initialize() {
    // Subscribe to new events from devices
	log.debug("Initializing..")
	CAPABILITY_MAP.each { key, capability ->
        capability["attributes"].each { attribute ->
            subscribe(settings[key], attribute, inputHandler)
        }
    }

    // Subscribe to events from the bridge
    // subscribe(bridge, "message", bridgeHandler)

    // Update the bridge
    // updateSubscription()
    runIn(60*3, initialize)
    registerHub()

}

// Update the bridge"s subscription
def updateSubscription() {
    def attributes = [
        notify: ["Contacts", "System"]
    ]
    CAPABILITY_MAP.each { key, capability ->
        capability["attributes"].each { attribute ->
            if (!attributes.containsKey(attribute)) {
                attributes[attribute] = []
            }
            settings[key].each {device ->
                attributes[attribute].push(device.displayName)
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

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Unsubscribe from all events
    unsubscribe()
    // Subscribe to stuff
    initialize()
}


// HELPER FUNCTIONS

// zigbee device ID's are too long for greenlight schema 
def getDevId(dev) {
	log.debug("length is: ${dev.deviceNetworkId.length()}")
	def result = (dev.deviceNetworkId.length()>10) ? dev.deviceNetworkId[-9..-1] : dev.deviceNetworkId
    log.debug("Greenlight unique_id: ${result}")
    return result
}

// MAC address isn't exposed via hub, so we'll have to fake it using guid
def generateSystemId(hub) {
	def guid = hub.getId().toString()
    def value = guid.split('-')[4]
    log.debug("lsb of guid is: ${value}")
    def mac = "${value[0..1]}:${value[2..3]}:${value[3..4]}:${value[4..5]}:${value[5..6]}:${value[7..8]}"
	return mac.toString()
}

// this is used for sending posts to internet connected sites e.g Greenlight API
def sendPost(path, data) {
    def headers = [:]
    // remove static value
    headers.put("Content-Type", "application/json")
    headers.put("Authorization", "$settings.key")

	def params = [
        uri: "http://devapi.greenlight.interthings.io" + path,
        body: data,
    	headers: headers
    ]
	log.debug("Sending POST request with: ${params}")
    
    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

// used for sending local LAN requests
def executeRequest(Path, method, body) {
	log.debug "Executing REST request"
    def headers = [:]
    // remove static value
    headers.put("HOST", "devapi.greenlight.interthings.io")
    headers.put("Content-Type", "application/json")
    headers.put("Authorization", "$settings.key")
    try {
    def resp = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers,
            body: body)
			log.debug(resp)
        	sendHubCommand(resp)
    }
    
    
    
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}
// Massive lookup tree
@Field CAPABILITY_MAP = [
    "accelerationSensors": [
        name: "Acceleration Sensor",
        capability: "capability.accelerationSensor",
        attributes: [
            "acceleration"
        ]
    ],
    "alarm": [
        name: "Alarm",
        capability: "capability.alarm",
        attributes: [
            "alarm"
        ],
        action: "actionAlarm"
    ],
    "battery": [
        name: "Battery",
        capability: "capability.battery",
        attributes: [
            "battery"
        ]
    ],
    "beacon": [
        name: "Beacon",
        capability: "capability.beacon",
        attributes: [
            "presence"
        ]
    ],
    "button": [
        name: "Button",
        capability: "capability.button",
        attributes: [
            "button"
        ]
    ],
    "carbonDioxideMeasurement": [
        name: "Carbon Dioxide Measurement",
        capability: "capability.carbonDioxideMeasurement",
        attributes: [
            "carbonDioxide"
        ]
    ],
    "carbonMonoxideDetector": [
        name: "Carbon Monoxide Detector",
        capability: "capability.carbonMonoxideDetector",
        attributes: [
            "carbonMonoxide"
        ]
    ],
    "colorControl": [
        name: "Color Control",
        capability: "capability.colorControl",
        attributes: [
            "hue",
            "saturation",
            "color"
        ],
        action: "actionColor"
    ],
    "colorTemperature": [
        name: "Color Temperature",
        capability: "capability.colorTemperature",
        attributes: [
            "colorTemperature"
        ],
        action: "actionColorTemperature"
    ],
    "consumable": [
        name: "Consumable",
        capability: "capability.consumable",
        attributes: [
            "consumable"
        ],
        action: "actionConsumable"
    ],
    "contactSensors": [
        name: "Contact Sensor",
        capability: "capability.contactSensor",
        attributes: [
            "contact"
        ]
    ],
    "doorControl": [
        name: "Door Control",
        capability: "capability.doorControl",
        attributes: [
            "door"
        ],
        action: "actionOpenClosed"
    ],
    "energyMeter": [
        name: "Energy Meter",
        capability: "capability.energyMeter",
        attributes: [
            "energy"
        ]
    ],
    "garageDoors": [
        name: "Garage Door Control",
        capability: "capability.garageDoorControl",
        attributes: [
            "door"
        ],
        action: "actionOpenClosed"
    ],
    "illuminanceMeasurement": [
        name: "Illuminance Measurement",
        capability: "capability.illuminanceMeasurement",
        attributes: [
            "illuminance"
        ]
    ],
    "imageCapture": [
        name: "Image Capture",
        capability: "capability.imageCapture",
        attributes: [
            "image"
        ]
    ],
    "levels": [
        name: "Switch Level",
        capability: "capability.switchLevel",
        attributes: [
            "level"
        ],
        action: "actionLevel"
    ],
    "lock": [
        name: "Lock",
        capability: "capability.lock",
        attributes: [
            "lock"
        ],
        action: "actionLock"
    ],
    "mediaController": [
        name: "Media Controller",
        capability: "capability.mediaController",
        attributes: [
            "activities",
            "currentActivity"
        ]
    ],
    "motionSensors": [
        name: "Motion Sensor",
        capability: "capability.motionSensor",
        attributes: [
            "motion"
        ],
        action: "actionActiveInactive"
    ],
    "musicPlayer": [
        name: "Music Player",
        capability: "capability.musicPlayer",
        attributes: [
            "status",
            "level",
            "trackDescription",
            "trackData",
            "mute"
        ],
        action: "actionMusicPlayer"
    ],
    "pHMeasurement": [
        name: "pH Measurement",
        capability: "capability.pHMeasurement",
        attributes: [
            "pH"
        ]
    ],
    "powerMeters": [
        name: "Power Meter",
        capability: "capability.powerMeter",
        attributes: [
            "power"
        ]
    ],
    "presenceSensors": [
        name: "Presence Sensor",
        capability: "capability.presenceSensor",
        attributes: [
            "presence"
        ]
    ],
    "humiditySensors": [
        name: "Relative Humidity Measurement",
        capability: "capability.relativeHumidityMeasurement",
        attributes: [
            "humidity"
        ]
    ],
    "relaySwitch": [
        name: "Relay Switch",
        capability: "capability.relaySwitch",
        attributes: [
            "switch"
        ],
        action: "actionOnOff"
    ],
    "shockSensor": [
        name: "Shock Sensor",
        capability: "capability.shockSensor",
        attributes: [
            "shock"
        ]
    ],
    "signalStrength": [
        name: "Signal Strength",
        capability: "capability.signalStrength",
        attributes: [
            "lqi",
            "rssi"
        ]
    ],
    "sleepSensor": [
        name: "Sleep Sensor",
        capability: "capability.sleepSensor",
        attributes: [
            "sleeping"
        ]
    ],
    "smokeDetector": [
        name: "Smoke Detector",
        capability: "capability.smokeDetector",
        attributes: [
            "smoke"
        ]
    ],
    "soundSensor": [
        name: "Sound Sensor",
        capability: "capability.soundSensor",
        attributes: [
            "sound"
        ]
    ],
    "stepSensor": [
        name: "Step Sensor",
        capability: "capability.stepSensor",
        attributes: [
            "steps",
            "goal"
        ]
    ],
    "switches": [
        name: "Switch",
        capability: "capability.switch",
        attributes: [
            "switch"
        ],
        action: "actionOnOff"
    ],
    "soundPressureLevel": [
        name: "Sound Pressure Level",
        capability: "capability.soundPressureLevel",
        attributes: [
            "soundPressureLevel"
        ]
    ],
    "tamperAlert": [
        name: "Tamper Alert",
        capability: "capability.tamperAlert",
        attributes: [
            "tamper"
        ]
    ],
    "temperatureSensors": [
        name: "Temperature Measurement",
        capability: "capability.temperatureMeasurement",
        attributes: [
            "temperature"
        ]
    ],
    "thermostat": [
        name: "Thermostat",
        capability: "capability.thermostat",
        attributes: [
            "temperature",
            "heatingSetpoint",
            "coolingSetpoint",
            "thermostatSetpoint",
            "thermostatMode",
            "thermostatFanMode",
            "thermostatOperatingState"
        ],
        action: "actionThermostat"
    ],
    "thermostatCoolingSetpoint": [
        name: "Thermostat Cooling Setpoint",
        capability: "capability.thermostatCoolingSetpoint",
        attributes: [
            "coolingSetpoint"
        ],
        action: "actionCoolingThermostat"
    ],
    "thermostatFanMode": [
        name: "Thermostat Fan Mode",
        capability: "capability.thermostatFanMode",
        attributes: [
            "thermostatFanMode"
        ],
        action: "actionThermostatFan"
    ],
    "thermostatHeatingSetpoint": [
        name: "Thermostat Heating Setpoint",
        capability: "capability.thermostatHeatingSetpoint",
        attributes: [
            "heatingSetpoint"
        ],
        action: "actionHeatingThermostat"
    ],
    "thermostatMode": [
        name: "Thermostat Mode",
        capability: "capability.thermostatMode",
        attributes: [
            "thermostatMode"
        ],
        action: "actionThermostatMode"
    ],
    "thermostatOperatingState": [
        name: "Thermostat Operating State",
        capability: "capability.thermostatOperatingState",
        attributes: [
            "thermostatOperatingState"
        ]
    ],
    "thermostatSetpoint": [
        name: "Thermostat Setpoint",
        capability: "capability.thermostatSetpoint",
        attributes: [
            "thermostatSetpoint"
        ]
    ],
    "threeAxis": [
        name: "Three Axis",
        capability: "capability.threeAxis",
        attributes: [
            "threeAxis"
        ]
    ],
    "timedSession": [
        name: "Timed Session",
        capability: "capability.timedSession",
        attributes: [
            "timeRemaining",
            "sessionStatus"
        ],
        action: "actionTimedSession"
    ],
    "touchSensor": [
        name: "Touch Sensor",
        capability: "capability.touchSensor",
        attributes: [
            "touch"
        ]
    ],
    "valve": [
        name: "Valve",
        capability: "capability.valve",
        attributes: [
            "contact"
        ],
        action: "actionOpenClosed"
    ],
    "voltageMeasurement": [
        name: "Voltage Measurement",
        capability: "capability.voltageMeasurement",
        attributes: [
            "voltage"
        ]
    ],
    "waterSensors": [
        name: "Water Sensor",
        capability: "capability.waterSensor",
        attributes: [
            "water"
        ]
    ],
    "windowShades": [
        name: "Window Shade",
        capability: "capability.windowShade",
        attributes: [
            "windowShade"
        ],
        action: "actionOpenClosed"
    ]
]
