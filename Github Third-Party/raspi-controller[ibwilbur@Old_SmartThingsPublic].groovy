/**
 *  RasPi Controller
 *
 *  Copyright 2016 Will Shelton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field

definition (
    name: "RasPi Controller",
    namespace: "ibwilbur",
    author: "Will Shelton",
    description: "Manages interaction betwen SmartThings and a Raspberry Pi",
    category: "SmartThings Labs",
    iconUrl: "http://download.easyicon.net/png/559404/32/",
    iconX2Url: "http://download.easyicon.net/png/559404/64/",
    iconX3Url: "http://download.easyicon.net/png/559404/128/"
)

preferences {   
    section("Control things") {
		input "switches", "capability.switch", title: "Switches", multiple: true, required: false
        input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
		input "doors", "capability.doorControl", title: "Doors", multiple: true, required: false
		input "music", "capability.musicPlayer", title: "Music Players", multiple: true, required: false
    }

    section("View things") {		
		input "presence", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
		input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
		input "motion", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
		input "temperature", "capability.temperatureMeasurement", title: "Temperature", multiple: true, required: false
		input "battery", "capability.battery", title: "Battery Status", multiple: true, required: false      
    }
    
	section("Raspberry Pi setup") {
        input("ip", "string", title: "IP Address", description: "RasPi IP Address", required: false)
        input("port", "string", title: "Port", description: "Listening Port", required: false)        
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
    if (!state.accessToken) { createAccessToken() }
    
    subscribe(switches, "switch", internalEventHandler)
    subscribe(dimmers, "level", internalEventHandler)
    subscribe(doors, "door", internalEventHandler)    
	subscribe(music, "status", internalEventHandler)
	subscribe(music, "level", internalEventHandler)
	subscribe(music, "trackDescription", internalEventHandler)
	subscribe(music, "trackData", internalEventHandler)
	subscribe(music, "mute", internalEventHandler)
    subscribe(presence, "presence", internalEventHandler)
    subscribe(contacts, "contact", internalEventHandler)
    subscribe(motion, "motion", internalEventHandler)
    subscribe(temperature, "temperature", internalEventHandler)
    subscribe(battery, "battery", internalEventHandler)
    
    //schedule("0 0/1 * * * ?", sendTemperatures)
    
    sendCommand("GET", "/reload", null)
}

def getDevices() {
    def data = []    
	switches?.each{data << getDeviceData(it)}
	dimmers?.each{data << getDeviceData(it)}
	doors?.each{data << getDeviceData(it)}
	music?.each{data << getDeviceData(it)}
	presence?.each{data << getDeviceData(it)}
	contacts?.each{data << getDeviceData(it)}
	motion?.each{data << getDeviceData(it)}
	temperature?.each{data << getDeviceData(it)}	
	battery?.each{data << getDeviceData(it)} 

	return data.unique()
}

def internalEventHandler(e) {
    log.debug "Internal event: Name: $e.displayName, Type: $e.name, Value: $e.value"
    
    def data = [name: e.displayName, type: e.name, value: e.value]
    sendCommand("PUT", "/push", data)
    /*
	switch (e.name) {
    	case "temperature":
        	sendCommand("PUT", "/temperature", data)
        	break
            
		default:
        	sendCommand("PUT", "/push", data)
        	break
    } 
	*/
}

def sendTemperatures() {
    def data = []
    temperature?.each { data << getTemperatureData(it) }
    sendCommand("PUT", "/temperature", data)    
}

def getTemperatureData(it) {
	[
    	name: it.displayName,
        type: "temperature",
        value: it.currentValue("temperature")
    ]
}

def updateDevice() {
	def name = request.JSON?.name
    def type = request.JSON?.type
    def value = request.JSON?.value
	log.debug "Update received: Name: $name, Type: $type, Value: $value"

    def device   
    switch (type) {
    	case "temperature":
        	device = temperature?.find { it.name == name }
            device?.update(value)
        	break
            
		case "door":
        	device = doors?.find { it.name == name }
            device?.update(value)
        	break
            
		case "switch":        	
        	device = switches?.find { it.name == name }
            device?.update(value)
        	break
		default:
            break
    }
}

def getDeviceData(device) {
    [
    	deviceId: device.id,
        deviceNetworkId: device.deviceNetworkId,
        name: device.displayName,
        attributes: getDeviceAttributes(device),
        commands: getDeviceCommands(device)
    ]
}

def getDeviceCommands(device) {
	def skippedCommands = ["configure", "enrollResponse"]
    def data = []
	device.supportedCommands.collect { command ->
    	if (!skippedCommands.contains(command.name)) {
        	data << (command.name)
        }
    }
    return data
}

def getDeviceAttributes(device) {
	def deviceData = [:]
    def skippedAttributes = ["checkInterval", "indicatorStatus", "mute"]
    device?.supportedAttributes?.each { attribute ->
    	try
        {
        	if (!skippedAttributes.contains(attribute.name)) {
                deviceData << [(attribute.name): roundNumber(device.currentValue(attribute.name))]
            }
        }
        catch (e){}
    }
    return deviceData
}

def getDeviceValue(device, type) {
	def unitMap = [temperature: "°", humidity: "%", level: "%", luminosity: "lx", battery: "%", power: "W", energy: "kWh"]
	def value = device.currentValue(type)
    
    if (unitMap.containsKey(type)) {
        if (value == null) { value = 0 }
    	return "${roundNumber(value)}${getUnit(type) ?: ""}"
        return
    } else {
    	return value
    }
}

def getUnit(attributeName) {
	def unitMap = [temperature: "°", humidity: "%", level: "%", luminosity: "lx", battery: "%", power: "W", energy: "kWh"]
    return unitMap[attributeName]
}

def roundNumber(num) {
	if (!roundNumbers || !"$num".isNumber()) return num
	if (num == null || num == "") return "n/a"
	else {
    	try {
            return "$num".toDouble().round()
        } catch (e) {return num}
    }
}

def sprinklerEventHandler(e) {
    log.debug "Sprinkler event from: $e.displayName, Value: $e.value, Source: $e.source, ID: $e.deviceId, Name: $e.name"
    if (e.value != "off" && e.value != "on") {
    	def data = [deviceid: e.deviceId, attribute: e.name, value: e.value]  
    	sendCommand("PUT", "/sprinkler", data)    
    }
}

def sendCommand(method, path, data) {
	def hubAction = new physicalgraph.device.HubAction(
    	method: method,
        path: path,
        headers: [HOST: "$settings.ip:$settings.port"],
        body: data
    )
    //log.debug "Result $hubAction"
    sendHubCommand(hubAction)
}

mappings {
    path("/devices") { 
    	action: [
        	GET: "getDevices",
            PUT: "updateDevice"
		]  
    }
}

private getCallBackAddress(hub) {
    return hub.localIP + ":" + hub.localSrvPortTCP
}

private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}