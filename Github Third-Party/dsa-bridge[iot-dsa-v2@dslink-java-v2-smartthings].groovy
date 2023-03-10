/**
 *  DSA Bridge
 *
 *  Copyright 2018 Daniel Shapiro
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
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

@Field COMMAND_MAP = [
    "setAirConditionerMode": [
        [
            "enum": [
                "auto", 
                "cool", 
                "dry", 
                "coolClean", 
                "dryClean", 
                "fanOnly", 
                "heat", 
                "heatClean", 
                "notSupported"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setColor": [
        [
            "name": "color hue", 
            "type": "number"
        ], 
        [
            "name": "color saturation", 
            "type": "number"
        ], 
        [
            "maxLength": 7, 
            "name": "color hex", 
            "type": "string"
        ], 
        [
            "maxLength": 3, 
            "name": "color switch", 
            "type": "string"
        ], 
        [
            "name": "color level", 
            "type": "number"
        ]
    ], 
    "setColorTemperature": [
        [
            "max": 30000, 
            "min": 1, 
            "name": "temperature", 
            "type": "number"
        ]
    ], 
    "setCoolingSetpoint": [
        [
            "max": 10000, 
            "min": -460, 
            "name": "setpoint", 
            "type": "number"
        ]
    ], 
    "setDishwasherMode": [
        [
            "enum": [
                "auto", 
                "quick", 
                "rinse", 
                "dry"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setDryerMode": [
        [
            "enum": [
                "regular", 
                "lowHeat", 
                "highHeat"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setFanSpeed": [
        [
            "min": 0, 
            "name": "speed", 
            "type": "number"
        ]
    ], 
    "setHeatingSetpoint": [
        [
            "max": 10000, 
            "min": -460, 
            "name": "setpoint", 
            "type": "number"
        ]
    ], 
    "setHue": [
        [
            "min": 0, 
            "name": "hue", 
            "type": "number"
        ]
    ], 
    "setInfraredLevel": [
        [
            "max": 100, 
            "min": 0, 
            "name": "level", 
            "type": "number"
        ]
    ], 
    "setInputSource": [
        [
            "enum": [
                "AM", 
                "CD", 
                "FM", 
                "HDMI", 
                "HDMI1", 
                "HDMI2", 
                "HDMI3", 
                "HDMI4", 
                "HDMI5", 
                "HDMI6", 
                "digitalTv", 
                "USB", 
                "YouTube", 
                "aux", 
                "bluetooth", 
                "digital", 
                "melon", 
                "wifi"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setLevel": [
        [
            "max": 100, 
            "min": 0, 
            "name": "level", 
            "type": "number"
        ], 
        [
            "min": 0, 
            "name": "rate", 
            "type": "number"
        ]
    ], 
    "setLightingMode": [
        [
            "enum": [
                "reading", 
                "writing", 
                "computer", 
                "night"
            ], 
            "name": "lightingMode", 
            "type": "enum"
        ]
    ], 
    "setMachineState": [
        [
            "enum": [
                "pause", 
                "run", 
                "stop"
            ], 
            "name": "state", 
            "type": "enum"
        ]
    ], 
    "setMute": [
        [
            "enum": [
                "muted", 
                "unmuted"
            ], 
            "name": "state", 
            "type": "enum"
        ]
    ], 
    "setOvenMode": [
        [
            "enum": [
                "heating", 
                "grill", 
                "warming", 
                "defrosting"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setOvenSetpoint": [
        [
            "min": 0, 
            "name": "setpoint", 
            "type": "number"
        ]
    ], 
    "setPlaybackRepeatMode": [
        [
            "enum": [
                "all", 
                "off", 
                "one"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setPlaybackShuffle": [
        [
            "enum": [
                "disabled", 
                "enabled"
            ], 
            "name": "shuffle", 
            "type": "enum"
        ]
    ], 
    "setPlaybackStatus": [
        [
            "enum": [
                "pause", 
                "play", 
                "stop", 
                "fast forward", 
                "rewind"
            ], 
            "name": "status", 
            "type": "enum"
        ]
    ], 
    "setRapidCooling": [
        [
            "enum": [
                "off", 
                "on"
            ], 
            "name": "rapidCooling", 
            "type": "enum"
        ]
    ], 
    "setRefrigerationSetpoint": [
        [
            "max": 10000, 
            "min": -460, 
            "name": "setpoint", 
            "type": "number"
        ]
    ], 
    "setRobotCleanerCleaningMode": [
        [
            "enum": [
                "auto", 
                "part", 
                "repeat", 
                "manual", 
                "stop"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setRobotCleanerMovement": [
        [
            "enum": [
                "homing", 
                "idle", 
                "charging", 
                "alarm", 
                "powerOff", 
                "reserve", 
                "point", 
                "after", 
                "cleaning"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setRobotCleanerTurboMode": [
        [
            "enum": [
                "on", 
                "off", 
                "silence"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setSaturation": [
        [
            "min": 0, 
            "name": "saturation", 
            "type": "number"
        ]
    ], 
    "setThermostatFanMode": [
        [
            "enum": [
                "auto", 
                "circulate", 
                "followschedule", 
                "on"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setThermostatMode": [
        [
            "enum": [
                "auto", 
                "cool", 
                "eco", 
                "rush hour", 
                "emergency heat", 
                "heat", 
                "off"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ], 
    "setTvChannel": [
        [
            "maxLength": 255, 
            "name": "channel", 
            "type": "string"
        ]
    ], 
    "setVolume": [
        [
            "max": 100, 
            "min": 0, 
            "name": "volume", 
            "type": "number"
        ]
    ], 
    "setWasherMode": [
        [
            "enum": [
                "regular", 
                "heavy", 
                "rinse", 
                "spinDry"
            ], 
            "name": "mode", 
            "type": "enum"
        ]
    ]
]
 
definition(
    name: "DSA Bridge",
    namespace: "iot-dsa-v2",
    author: "Daniel Shapiro",
    description: "bridge to DSA",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)
    }

    section ("Input") {
    	input "sensors", "capability.sensor", title: "Sensors", multiple: true, required: false
        input "actuators", "capability.actuator", title: "Actuators", multiple: true, required: false
    }

    section ("Bridge") {
        input "bridge", "capability.notification", title: "Notify this Bridge", required: true, multiple: false
    }
    
    section("Async?") {
    	input "async", "bool", title: "Send device info asynchronously", required: true, multiple: false, defaultValue: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unsubscribeBridge()
	initialize()
}

def uninstalled() {
	log.debug "Uninstalled"

	unsubscribeBridge()
}

def allDevices() {
	def sensors = settings["sensors"] != null ? settings["sensors"] : []
    return settings["actuators"] != null ? sensors + settings["actuators"] : sensors
}

def initialize() {
	// Subscribe to events from the bridge
    subscribe(bridge, "message", bridgeHandler)
    log.debug "subscribed to bridge messages"

    // Update the bridge
    subscribeBridge()
	// Subscribe to new events from devices
    def deviceBatch = []
    def delay = 3
    allDevices().each { device ->
        if (async) {
        	deviceBatch.add(device.getId())
            if (deviceBatch.size() >= 15) {
				runIn(delay, forwardDeviceByIds, [overwrite: false, data: [devices: deviceBatch]])
                deviceBatch = []
                delay = delay + 1
			}
        } else {
        	forwardDevice(device)
        }
    }
    if (!deviceBatch.isEmpty()) {
    	runIn(delay, forwardDeviceByIds, [overwrite: false, data: [devices: deviceBatch]])
    }
}

def forwardDeviceByIds(data) {
	log.debug "forwardDeviceById called with ${data}"
    def deviceIdList = data.devices
	allDevices().each { device ->
    	if (deviceIdList.contains(device.getId())) {
        	forwardDevice(device)
            return
        }
    }
}

def forwardDevice(device) {
	def attrs = device.supportedAttributes
    def body = [:]
    attrs.each { attr ->
        subscribe(device, attr.name, inputHandler)
        body.put(attr.name, dsaValueNode("string", device.currentValue(attr.name)))
    }
    body.put("meta", getMeta(device))
    def commands = device.getSupportedCommands()
    commands.each { command ->
        def params = COMMAND_MAP."$command.name"
        if (params == null) {
            params = []
        }
        body.put(command.name, [
            '$invokable' : "read",
            '$params': params
        ])
    }
    def toSend = [
        path: "/" + java.net.URLEncoder.encode(device.displayName, "UTF-8"),
        body: body
    ]
	def json = new JsonOutput().toJson(toSend)
    log.debug "Forwarding device to bridge: ${json}"
    bridge.deviceNotification(json)
}

def getMeta(device) {
	def meta = [:]	
	safePutValueNode(meta, "Capabilities", "array", device.getCapabilities()*.getName())
    safePutValueNode(meta, "Network ID", "string", device.getDeviceNetworkId())
    safePutValueNode(meta, "ID", "string", device.getId())
    safePutValueNode(meta, "Label", "string", device.getLabel())
    safePutValueNode(meta, "Last Activity", "string", device.getLastActivity())
    safePutValueNode(meta, "Manufacturer Name", "string", device.getManufacturerName())
    safePutValueNode(meta, "Model Name", "string", device.getModelName())
    safePutValueNode(meta, "Status", "string", device.getStatus())
    safePutValueNode(meta, "Name", "string", device.getName())
    safePutValueNode(meta, "Type Name", "string", device.getTypeName())
    return meta
}

def safePutValueNode(body, name, type, value) {
	if (value != null) {
    	body.put(name, dsaValueNode(type, value))
    }
}

def dsaValueNode(type, value) {
	return [
    	'$type' : type,
        //'$writable' : "write",
        '?value' : value
    ]
}

// Update the bridge"s subscription
def subscribeBridge() {
    def json = new groovy.json.JsonOutput().toJson([
        path: "/",
        method: "SUBSCRIBE",
        body: ""
    ])

    log.debug "Subscribing to DSA: ${json}"

	subscribe(location, null, lanResponseHandler, [filterEvents:false])
    bridge.deviceNotification(json)
}

def unsubscribeBridge() {
	def json = new groovy.json.JsonOutput().toJson([
        path: "/",
        method: "UNSUBSCRIBE",
        body: ""
    ])

    log.debug "Unsubscribing from DSA: ${json}"

    bridge.deviceNotification(json)
}

//recieve an update from the bridge
def lanResponseHandler(evt) {
    def msg = parseLanMessage(evt.stringValue)
    log.debug "got lan evt: ${msg}"
	def json = new JsonSlurper().parseText(msg.body)
    
    def path = json.path
    path = path.charAt(0) == '/' ? path.substring(1) : path
    def arr = path.split("/")
    def deviceName = URLDecoder.decode(arr[0], "UTF-8")
    log.debug "want to invoke ${json.action} on ${deviceName}"
    allDevices().each { device ->
    	log.debug "is ${deviceName} == ${device.displayName}?" 
        if (device.displayName.equals(deviceName)) {
            if (json.action != null) {
            	log.debug "want to invoke ${json.action}"
                if (device.hasCommand(json.action)) {
                	log.debug "going to invoke ${json.action}"
                    if ("setColor".equals(json.action)) {
                        def objParam = [:]
                        def argmap = json.arguments
                        argMap.each {name, val ->
                            objParam.put(name.replaceAll("color ", ""), val)
                        }
                        device."$json.action"(objParam)
                    } else {
                        def paramDefs = COMMAND_MAP."$json.action"
                        if (paramDefs == null) {
                            device."$json.action"()
                        } else {
                            def params = [] 
                            def argmap = json.arguments
                            paramDefs.each { paramDef ->
                                def val = argmap."$paramDef.name"
                                params.add(val)
                            }
                            device."$json.action"(*params)
                        }
                    }
                }
            } else {
                def attr = arr[1]
                if (device.getSupportedCommands().any {it.name == "setStatus"}) {
                    log.debug "Setting state ${attr} = ${json.value}"
                    device.setStatus(attr, json.value)
                    state.ignoreEvent = json;
                }
            }
        }
    }
}

// Receive an event from the bridge
def bridgeHandler(evt) {
    def json = new JsonSlurper().parseText(evt.value)
    log.debug "Received device event from bridge: ${json}"    
}

// Receive an event from a device
def inputHandler(evt) {
    if (
        state.ignoreEvent
        && state.ignoreEvent.name == evt.displayName
        && state.ignoreEvent.type == evt.name
        && state.ignoreEvent.value == evt.value
    ) {
        log.debug "Ignoring event ${state.ignoreEvent}"
        state.ignoreEvent = false;
    }
    else {
        def json = new JsonOutput().toJson([
        	method: "PUT",
            path: "/" + java.net.URLEncoder.encode(evt.device.displayName, "UTF-8") + "/" + java.net.URLEncoder.encode(evt.name, "UTF-8"),
            body: dsaValueNode("string", evt.value)
        ])

        log.debug "Forwarding device event to bridge: ${json}"
        bridge.deviceNotification(json)
    }
}