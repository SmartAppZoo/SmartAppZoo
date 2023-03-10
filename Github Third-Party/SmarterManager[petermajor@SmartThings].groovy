/**
 *  Smarter Manager
 *
 *  Copyright 2017 Peter Major
 *
 *  Version 1.0.0   17 Apr 2017		Initial Release
 *  Version 1.1.0   22 Apr 2017		Notify user when start brew fails
 *  Version 1.1.1   29 Apr 2017		Update device on rediscovery
 *
 *	Version History
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

definition(
		name: "Smarter Manager",
		namespace: "petermajor",
		author: "Peter Major",
		description: "Smarter Service Manager - discovers Smarter devices on your network",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "deviceDiscovery", title: "Device Discovery", content: "deviceDiscovery")
}

def deviceDiscovery() {

	def devices = getDevices()
	def options = [:]

	if (!devices) {

		ssdpSubscribe()
		ssdpDiscover()

		return dynamicPage(name: "deviceDiscovery", title: "Discovery Started...", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		}
	
	} else {

		devices.each {
			def value = it.value.name
			def key = it.key
			options["$key"] = value
		}

		return dynamicPage(name: "deviceDiscovery", title: "Discovery Finished", nextPage: "", install: true, uninstall: true) {
			section("Found ${options.size() ?: 0} Smarter " + (options.size() == 1 ? "device" : "devices") + " on the network") {
				input "selectedDevices", "enum", required: false, title: "Select Devices", multiple: true, options: options
			}
			section("Notifications") {
				input "notifyOnCarafeNotReady", "bool", required: true, title: "Carafe not ready", default: true
			}
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
}

def initialize() {

	unsubscribe()
	unschedule()

	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	// check devices for ip change
	runEvery5Minutes("ssdpDiscover")

	log.trace "subscribe to location"
	subscribe(location, null, locationHandler, [filterEvents:false])
}

def locationHandler(evt) {

	def description = evt.description

	def map = stringToMap(evt.stringValue)

	def headersString = map.get("headers")
	if (headersString == null) {
		return
	}

	def headers = getHttpHeaders(headersString);
	if (headers.get("POST /smarter-coffee-callback HTTP/1.1") == null) {
		return;
	}

	def bodyString = map.get("body")
	if (bodyString == null) {
		return
	}

	def body = getHttpBody(bodyString)

	def id = body?.id
	if (!id) {
		return
	}

	def child = getChildDevice(id)
	if (!child) {
		return
	}

	def status = body.status
	if (status) {
		log.debug "updating status for device ${id}"
		child.updateStatus(status)
		return
	}

	def error = body.error
	if (error) {
		log.debug "sending error to device ${id}"
		child.notifyError(error)

		if (error == "No carafe" && notifyOnCarafeNotReady) {
			sendPush('Unable to brew coffee... Carafe not ready.')
		}
		return
	}
}

void ssdpSubscribe() {
	log.debug "Subscribed to discovery response"
	subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:SmartThingsSmarterCoffee:1", ssdpHandler)
}

void ssdpDiscover() {
	log.debug "Sending discovery request"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:SmartThingsSmarterCoffee:1", physicalgraph.device.Protocol.LAN))
}

def ssdpHandler(evt) {

	def description = evt.description
	log.debug "ssdpHandler description: ${description}"

	def parsedEvent = parseLanMessage(description)
	log.debug "ssdpHandler parsedEvent: ${parsedEvent}"

	def serverAddress = convertHexToIP(parsedEvent?.networkAddress)
	def serverPort = "2080";

	discoverDevices(serverAddress, serverPort)
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def discoverDevices(serverAddress, serverPort) {

 	log.debug "discoverDevices"

	try {
		def action = new physicalgraph.device.HubAction("""GET /api/device HTTP/1.1\r\nHOST: $serverAddress:$serverPort\r\n\r\n""", physicalgraph.device.Protocol.LAN, "$serverAddress:$serverPort", [callback: discoverDevicesCallback]);
 		log.debug "action {$action}"
		sendHubCommand(action)
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $action"
	}
}

void discoverDevicesCallback(physicalgraph.device.HubResponse hubResponse) {

 	log.debug "discoverDevicesCallback {$hubResponse}"

 	if (hubResponse.status != 200) return

 	def serverMac = hubResponse.mac
 	def serverAddress = convertHexToIP(hubResponse.ip)
 	def serverPort = Integer.toString(convertHexToInt(hubResponse.port))
 	def hubId = hubResponse.hubId

	def body = hubResponse.json
 	log.debug "body {$hubResponse.json}"

	def devices = getDevices()

	body.values().each {
		def id = it.id

		def device = [
			id: id,
			serverMac: serverMac,
			serverAddress: serverAddress,
			serverPort: serverPort,
			hubId: hubId,
			name: it.name,
			mac: it.mac, 
			ip: it.ip]

		devices["$id"] = device

		def child = getChildDevice(id)
		if (child) {
			log.debug "device known $id, syncing..."
			child.sync(device.serverAddress, device.serverPort, device.serverMac, device.id)
		}
	}

 	log.debug "discoverDevicesCallback {$devices}"
}

def addDevices() {
	def devices = getDevices()

	selectedDevices.each { id ->
		def selectedDevice = devices["$id"]
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == id
			}
		}

		if (!d) {
			log.debug "Creating device with dni: $id in hub: $selectedDevice.hubId"
			addChildDevice("petermajor", "Smarter Coffee", id, selectedDevice.hubId, [
				"name": selectedDevice.name,
				"label": selectedDevice.name,
				"data": [
					"serverAddress": selectedDevice.serverAddress,
					"serverPort": selectedDevice.serverPort,
					"serverMac": selectedDevice.serverMac,
					"deviceId": id
				]
			])
		}
	}
}

private getHttpHeaders(headers) {
	def obj = [:]
	new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
	}
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

