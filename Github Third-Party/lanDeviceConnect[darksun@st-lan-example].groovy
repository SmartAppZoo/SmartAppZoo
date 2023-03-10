/**
 *  LAN Device Discovery Example
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
 *
 */
definition(
	name: "Node LAN Example (Connect)",
	namespace: "darksun",
	author: "adam",
	description: "Example connecting to lan connected device",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	oauth: true)


preferences {
	page(name: "deviceDiscovery", title:"Lan Device Setup", content:"deviceDiscovery", refreshTimeout:3)
	page(name: "setNameDevice", title: "Set name of Device")
	page(name: "addDevice", title: "Add New Device")
}

def deviceDiscovery() {
	int deviceRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
	state.bridgeRefreshCount = deviceRefreshCount + 1

	def options = devicesDiscovered() ?: []
	def numFound = options.size() ?: 0

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	// Audio discovery request every 15 seconds
	if((deviceRefreshCount % 5) == 0) {
		findDevice()
		state.lanDevices.clear()
	}

	return dynamicPage(name:"deviceDiscovery", title:"Device Search Started!", nextPage:"setNameDevice", refreshInterval:5, uninstall: true) {
		section("Please wait while we discover your device. Select your device below once discovered."){
		}
		section("on this hub...") {
			input "theHub", "hub", multiple: false, required: true
		}
		section("") {
			input "selectedDevice", "enum", required:false, title:"Select Device (${numFound} found)", multiple:false, options:options
		}
	}
}

def setNameDevice() {
	log.debug "setNameDevice"
	return dynamicPage(name:"setNameDevice", title:"Set the name of the device", nextPage:"addDevice") {
		section{
			input "nameDevice", "text", title:"Enter the name of the device", required:false
		}
	}
}

def addDevice() {
	log.debug "addDevice"
	def existingDevice  = getChildDevice(selectedDevice)
	def entry = state.lanDevices[selectedDevice]
	if(!existingDevice && theHub && entry)
	{
		existingDevice = addChildDevice("adam", "lanDevice", selectedDevice, theHub.id, [name:nameDevice, label:name])
		existingDevice.updateDataValue("mac", entry.mac)
		existingDevice.updateDataValue("ip", entry.ip)
		existingDevice.updateDataValue("port", entry.port)
		existingDevice.updateDataValue("uuid", entry.uuid)
	}
	else if(existingDevice && entry)
	{
		if(entry.ip != existingDevice.getDataValue("ip"))
		{
			existingDevice.updateDataValue("ip", entry.ip)
		}
	}
	else
	{
		log.debug "Device already created"
	}
		return dynamicPage(name:"addDevice", title:"Adding LAN Device (${nameDevice}) is done.", install:true) {
	}
}

Map devicesDiscovered() {
	def lanDevices = getLANDevices()
	def map = [:]

	//TODO commented out
	lanDevices.each {
		def key = it.value.mac
		def value = "LAN Device (${convertHexToIP(it.value.ip)})"
		map["${key}"] = value
	}
	map
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
	// TODO: subscribe to attributes, devices, locations, etc.
	unsubscribe()
	state.subscribe = false

	//Don't schedule a cron refresh
	//scheduleActions()


	def existingDevice  = getChildDevice(selectedDevice)
	def entry = state.lanDevices[selectedDevice]
	if(!existingDevice && theHub && entry)
	{
		existingDevice = addChildDevice("adam", "lanDevice", selectedDevice, theHub.id, [name:nameDevice, label:name])
		existingDevice.updateDataValue("mac", entry.mac)
		existingDevice.updateDataValue("ip", entry.ip)
		existingDevice.updateDataValue("uuid", entry.uuid)
	}
	else if(existingDevice && entry)
	{
		if(entry.ip != existingDevice.getDataValue("ip"))
		{
			existingDevice.updateDataValue("ip", entry.ip)
		}
	}
	else
	{
		log.debug "Device already created"
	}

	//Don't call refresh
	//scheduledActionsHandler()
}

def scheduledActionsHandler() {
	log.trace "scheduledActionsHandler()"
	refreshAll()
}

private scheduleActions() {
	def sec = Math.round(Math.floor(Math.random() * 60))
	//def cron = "$sec 0/5 * * * ?"	// every 5 min
	def cron = "$sec 0/5 * * * ?"	// every 1 min
	log.debug "schedule('$cron', scheduledActionsHandler)"
	schedule(cron, scheduledActionsHandler)
}

private refreshAll(){
	log.trace "refreshAll()"
	childDevices*.refresh()
	log.trace "/refreshAll()"
}

def uninstalled() {
	unsubscribe()
	state.subscribe = false
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def getLANDevices()
{
	state.lanDevices = state.lanDevices ?: [:]
	log.warn state.lanDevices
	return state.lanDevices
}

private findDevice() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaServer:1", physicalgraph.device.Protocol.LAN))
}

def locationHandler(evt) {
	log.debug evt.description
	def upnpResult = parseEventMessage(evt.description)

	if (upnpResult?.ssdpTerm?.contains("urn:schemas-upnp-org:device:MediaServer:1")) {
		log.debug "upnpResult: ${upnpResult}"

		def lanDevices = getLANDevices()
		lanDevices << ["${upnpResult.mac.toString()}" : [mac:upnpResult.mac, ip:upnpResult.ip, port: upnpResult.port, uuid:upnpResult.uuid]]
	}
}

private def parseEventMessage(String description) {
	def event = [:]
	def parts = description.split(',')

	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.port = valueString
			}
		}
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpUSN = valueString
				def splitedValue = valueString.split('::')
				splitedValue.each { value ->
					value = value.trim()
					if (value.startsWith('uuid:'))
					{
						value -= "uuid:"
						def uuidString = value.trim()
						if (uuidString) {
							event.uuid = uuidString

						}
					}
				}
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpTerm = valueString
			}
		}
	}
	event
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}