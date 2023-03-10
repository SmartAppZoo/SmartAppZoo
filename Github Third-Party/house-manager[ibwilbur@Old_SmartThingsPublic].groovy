/**
 *  House Manager
 *
 *  Copyright 2016 Will Shelton
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
definition (
    name: "House Manager",
    namespace: "ibwilbur",
    author: "Will Shelton",
    description: "Manages interaction betwen SmartThings and other devices",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Raspberry Pi Setup") {
        input "ip", "text", "title": "IP Address", multiple: false, required: false
        input "port", "text", "title": "Port", multiple: false, required: false
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
    
    //subscribe(location, handler)
    
    subscribe(lights, "switch.on", eventHandler, [filterEvents: false])
    subscribe(lights, "switch.off", eventHandler, [filterEvents: false])
    subscribe(doors, "door", eventHandler, [filterEvents: false])
    subscribe(dimmers, "level", eventHandler, [filterEvents: false])    
    subscribe(momentaries, "push", eventHandler, [filterEvents: false])
    subscribe(momentaries, "momentary", eventHandler, [filterEvents: false])
    
    subscribe(thermostats, "temperature", eventHandler, [filterEvents: false])
    subscribe(thermostats, "heatingSetpoint", eventHandler, [filterEvents: false])
    subscribe(thermostats, "coolingSetpoint", eventHandler, [filterEvents: false])
    subscribe(thermostats, "thermostatFanMode", eventHandler, [filterEvents: false])
    subscribe(thermostats, "thermostatOperatingState", eventHandler, [filterEvents: false])
    
	subscribe(presence, "presence", eventHandler, [filterEvents: false])
    subscribe(contacts, "contact", eventHandler, [filterEvents: false])
    subscribe(motion, "motion", eventHandler, [filterEvents: false])    
    subscribe(temperature, "temperature", eventHandler, [filterEvents: false])   
    subscribe(humidity, "humidity", eventHandler, [filterEvents: false])
    subscribe(water, "water", eventHandler, [filterEvents: false])
    subscribe(battery, "battery", eventHandler, [filterEvents: false])
    subscribe(energy, "energy", eventHandler, [filterEvents: false])
    subscribe(power, "power", eventHandler, [filterEvents: false])         
   	subscribe(acceleration, "acceleration", eventHandler, [filterEvents: false])
    subscribe(luminosity, "luminosity", eventHandler, [filterEvents: false])
    
    subscribe(locks, "lock", eventHandler, [filterEvents: false])
	subscribe(music, "status", eventHandler, [filterEvents: false])
	subscribe(music, "level", eventHandler, [filterEvents: false])
	subscribe(music, "trackDescription", eventHandler, [filterEvents: false])
    subscribe(music, "trackData", eventHandler, [filterEvents: false])
	subscribe(music, "mute", eventHandler, [filterEvents: false])
    subscribe(camera, "image", eventHandler, [filterEvents: false])    
}

preferences {

    section("Control things") {
        input "lights", "capability.switch", title: "Lights", multiple: true, required: false
        input "doors", "capability.doorControl", title: "Doors", multiple: true, required: false
        //input "thermostats", "capability.thermostat", title: "Thermostats", multiple: true, required: false
        //input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "music", "capability.musicPlayer", title: "Music Players", multiple: true, required: false        
    }

    section("View things") {
    	input "camera", "capability.imageCapture", title: "Cameras (Image Capture)", multiple: true, required: false
        input "presence", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
        input "motion", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
        input "temperature", "capability.temperatureMeasurement", title: "Temperature", multiple: true, required: false
        input "humidity", "capability.relativeHumidityMeasurement", title: "Hygrometer", multiple: true, required: false
        //input "water", "capability.waterSensor", title: "Water Sensors...", multiple: true, required: false
        input "battery", "capability.battery", title: "Battery Status", multiple: true, required: false
        //input "energy", "capability.energyMeter", title: "Energy Meters...", multiple: true, required: false
        //input "power", "capability.powerMeter", title: "Power Meters...", multiple: true, required: false
        //input "acceleration", "capability.accelerationSensor", title: "Vibration Sensors...", multiple: true, required: false
        //input "luminosity", "capability.illuminanceMeasurement", title: "Luminosity Sensors...", multiple: true, required: false       
    }

}

def getDevices() {
    def data = []
    
    lights?.each{data << getDeviceData(it)}
    doors?.each{data << getDeviceData(it)}
    //thermostats?.each{data << getDeviceData(it)}
    //locks?.each{data << getDeviceData(it)}
    music?.each{data << getDeviceData(it)}
    camera?.each{data << getDeviceData(it)}
    presence?.each{data << getDeviceData(it)}
    contacts?.each{data << getDeviceData(it)}
    motion?.each{data << getDeviceData(it)}
    temperature?.each{data << getDeviceData(it)}
    humidity?.each{data << getDeviceData(it)}
    //water?.each{data << getDeviceData(it)}
    battery?.each{data << getDeviceData(it)}
    //energy?.each{data << getDeviceData(it)}
    //power?.each{data << getDeviceData(it)}
    //acceleration?.each{data << getDeviceData(it)}
    //luminosity?.each{data << getDeviceData(it)}
  
    return data.unique()
}

def updateDevice() {
	log.debug "updateDevice(), DeviceID: $params.deviceId, Type: $params.type, Value: $params.value"
 
 	def deviceId = params.deviceId
    def type = params.type
    def value = params.value
    def device
    
    switch (type) {
    	case "temperature":
        	device = temperature?.find { it.id == deviceId }
            if (device) {
            	device.update(value)
            }
        	break
            
		case "door":
        	device = doors?.find { it.id == deviceId }
            if (device) {
            	device.update(value)
            }
        	break
            
		default:
            break
    }

	def timeZone = TimeZone.getTimeZone('America/Chicago')
    state.lastContact = new Date().format('MM/dd/yyyy HH:mm a', timeZone)
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

def eventHandler(e) {
    log.debug "Event from: $e.displayName, Value: $e.value, Source: $e.source, ID: $e.deviceId, Name: $e.name"
    def data = [deviceid: e.deviceId, attribute: e.name, value: e.value ]
    
    def headers = [:]
    def method = "PUT"
    def value = e.value
    headers.put("HOST", "$settings.ip:$settings.port")
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    
    if (e.value.contains('refresh.')) { 
    	method = "GET" 
        data.value = "refresh"
	}
		
    def hubAction = new physicalgraph.device.HubAction(
        method: method, 
        path: "/devices",  
        headers: headers,
        body: data
	)

    sendHubCommand(hubAction) 
}

mappings {
    path("/devices") { 
    	action: [
        	GET: "getDevices"
		]  
    }
    path ("/devices/:deviceId/:type/:value") {
    	action: [
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