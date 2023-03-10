/**
 *  MQTT adapter REST Api
 *
 *  Copyright 2017 Dr1rrb
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "MQTT adapter",
	namespace: "dr1rrb",
	author: "Dr1rrb",
	description: "Adapter to expose and control Smartthings devices to/from a MQTT broker.",
	category: "My Apps",
	iconUrl: "http://mqtt.org/new/wp-content/uploads/2011/08/mqttorg-glow.png",
	iconX2Url: "http://mqtt.org/new/wp-content/uploads/2011/08/mqttorg-glow.png",
	iconX3Url: "http://mqtt.org/new/wp-content/uploads/2011/08/mqttorg-glow.png",
	oauth: true)


preferences {
	section("Bridge") {
	 	input "bridgeUri", "text", title: "Uri (https://host:port)", required: true
		input "bridgeAuth", "text", title: "Auth token", required: true
	}
	section("Connect those devices") {
	 	input "actuators", "capability.actuator", title: "Select actuators", multiple: true, required: false
		input "sensors", "capability.sensor", title: "Select sensors", multiple: true, required: false
	}
}

mappings {
	path("/infos") {
		action: [GET: "retreiveServerInfos"]
  	}
	 path("/items") {
		action: [GET: "retreiveDevicesAndRoutines"]
  	}
	path("/device/:id/:command") {
		action: [ PUT: "updateDevice" ]
	}
}

// Region: App lifecycle
def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
}

def initialize() {
	if(!state.accessToken) {
		  createAccessToken()
	}

	refresh()
	runEvery1Hour(refresh)
}

def refresh() {
	unsubscribe()
	 
	getDevices().each { device -> 
	 	device.supportedAttributes.each { attr ->
			subscribe(device, attr.name, forwardDeviceStatus)
		}
		initDeviceStatus(device)
	}
    
    subscribe(location, "routineExecuted", forwardRoutine)
    
    log.debug "Subscriptions refreshed!"
}

// Region: MQTT to ST
def retreiveServerInfos()
{
	return [version: 1]
}

def retreiveDevicesAndRoutines() {
	def details = params.details == "true" ? true : false;

	return [
		devices: getDevices().collect { getDeviceInfos(it, details) }, 
		routines: location.helloHome?.getPhrases().collect { getRoutineInfos(it, details) }
	];
}

def updateDevice() {
	def device = getDevice(params.id)
	def command = notNull("command", params.command)

	 
	try {
	 
		log.debug "Executing '${command}' on device '${device.id}' (parameters (${request.JSON?.size()}): ${request.JSON} / ${request.JSON?.find { true }?.getValue()})."
	 
	 	if (request.JSON?.size() == 0)
		 	device."$command"()
		else if (request.JSON?.size() == 1)
		 	device."$command"(request.JSON.find { true }.getValue())

	} catch (e) {
		log.error "Failed to execute '${command}': ${e}"
	}
	 
	return getDeviceInfos(device)
}


// Region: ST to MQTT
def initDeviceStatus(device)
{
	log.debug "Init ${device}"

	try {
		def request = [
			uri: "${bridgeUri}/api/smartthings",
			headers: [Authorization: "Bearer ${bridgeAuth}"],
			body: [
            	kind: "device",
				device: getDeviceInfos(device)
			]
		]
		
		httpPostJson(request) { resp -> log.debug "response: ${resp.status}." }
		
	} catch (e) {
		log.error "Failed to initialize device status: ${e}"
	}
}

def forwardDeviceStatus(eventArgs)
{
	log.debug "Forwarding ${eventArgs.name}: ${eventArgs.value}"

	try {
		def request = [
			uri: "${bridgeUri}/api/smartthings",
			headers: [Authorization: "Bearer ${bridgeAuth}"],
			body: [
            	kind: "device",
				device: getDeviceInfos(eventArgs.device),
				event: [
					date: eventArgs.isoDate, 
					value: eventArgs.value,
					name: eventArgs.name,
				]
			]
		]
        
		httpPostJson(request) { resp -> log.debug "response: ${resp.status}." }
		
	} catch (e) {
		log.error "Failed to forward event: ${eventArgs.name}"
	}
}

def forwardRoutine(eventArgs)
{
    log.debug "Forwarding routine ${eventArgs.value}"

	try {
		def request = [
			uri: "${bridgeUri}/api/smartthings",
			headers: [Authorization: "Bearer ${bridgeAuth}"],
			body: [
            	kind: "routine",
                routine: [
                	id: eventArgs.value
                ],
				event: [
					date: eventArgs.isoDate, 
					value: eventArgs.value,
					name: eventArgs.name,
				]
			]
		]
		
		httpPostJson(request) { resp -> log.debug "response: ${resp.status}." }
		
	} catch (e) {
		log.error "Failed to forward routine event: ${eventArgs.name}"
	}
}

// Region: Get device
def getDevices()
{
	return actuators + sensors;
}

def findDevice(deviceId)
{
	notNull("deviceId", deviceId);
	
	return getDevices().find { it.id == deviceId };
}

def getDevice(deviceId)
{
	def device = findDevice(deviceId);
	if (device == null)
	{
		httpError(404, "Device '${deviceId}' not found.")
	}
	return device;
}

// Region: Get infos
def getDeviceInfos(device, details = false)
{
	def infos = [
		id: device.id,
		name: device.displayName,
		properties: device.supportedAttributes.collectEntries { attr -> [ (attr.name): device.currentValue(attr.name).toString() ] }
	]
	
	if (details)
	{
	 	infos["capabilities"] = device.capabilities.collect { getCapabilityInfos(it, details) }
		infos["attributes"] = device.supportedAttributes.collect { getAttributeInfos(it, details) }
		infos["commands"] = device.supportedCommands.collect { getCommandInfos(it, details) }
	}
	
	return infos;
}

def getCapabilityInfos(capablity, details = false)
{
	def infos = [name: capablity.name]
	
	if(details)
	{
		infos["attributes"] = capablity.attributes.collect { getAttributeInfos(it, details) }
		infos["commands"] = capablity.commands.collect { getCommandInfos(it, details) }
	}
	
	return infos;
}

def getCommandInfos(command, details = false)
{
	return [
		name: command.name, 
		arguments: command.arguments
	]
}

def getAttributeInfos(attribute, details = false)
{
	return [
		name: attribute.name, 
		arguments: attribute.dataType,
		values: attribute.values
	]
}

def getRoutineInfos(routine, details = false)
{
	def infos = [
	 	id: routine.id,
		  name: routine.label
	];
	 
	 if (details)
	 {
	 	infos["hasSecureActions"] = routine.hasSecureActions;
		  infos["action"] = routine.action;
	 }
	 
	 return infos;
}

// Region: Parameters assertion helpers
def notNull(parameterName, value)
{
	if(value == null || value == "")
	{
		httpError(404, "Missing parameter '${parameterName}'.")
	}
	return value;
}