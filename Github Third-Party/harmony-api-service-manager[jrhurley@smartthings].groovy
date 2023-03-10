/**
 *  Harmony API - Service Manager
 *
 *
 *  Version 1.1
 *   - 1.0 Initial version
 *   - 1.1 Version bump
 *   - 1.2 Moved searchTarget to state variable rather than having it on the first page as an input
 *
 *  Copyright 2017 Jonathon Hurley
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
    name: "Harmony API - Service Manager",
    namespace: "jrhurley",
    author: "Jonathon Hurley",
    description: "Detect Harmony API servers via UPnP and add them as devices",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	//page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
	//	section("Search Target") {
	//		input "searchTarget", "string", title: "Search Target", defaultValue: "urn:a101-org-uk:service:HarmonyAPI:1", required: true
	//	}
	//}
	page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
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
	state.searchTarget = "urn:a101-org-uk:service:HarmonyAPI:1"
    
	unsubscribe()
	unschedule()

	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("ssdpDiscover")
}


// deviceDiscovery()
// Set up the dynamic page to display discovered UPnP devices
def deviceDiscovery() {
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		options["${key}"] = value
	}

	ssdpSubscribe()

	ssdpDiscover()
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 3, install: true, uninstall: true) {
		section("Please wait while we discover your UPnP Device. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

// ssdpSubscribe
// Subscribe to SSDP messages
void ssdpSubscribe() {
	log.debug("ssdpSubscribe ssdpTerm.${state.searchTarget}")
	subscribe(location, "ssdpTerm.${state.searchTarget}", ssdpHandler)
}

// ssdpDiscover
// Discover SSDP devices on the LAN
void ssdpDiscover() {
	log.debug("ssdpDiscover ${state.searchTarget}")
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${state.searchTarget}", physicalgraph.device.Protocol.LAN))
}

// ssdpHandler
// Handles SSDP messages - we subscribed to these in ssdpSubscribe()
def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
    
    log.debug("ssdpHandler ${parsedEvent}")

	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
	if (devices."${ssdpUSN}") {
    	// Device already exists in list, update network address if needed
		def d = devices."${ssdpUSN}"
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			def child = getChildDevice(parsedEvent.mac)
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
	} else {
    	// Add device to list
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

// verifyDevices
// Verify that devices found through SSDP exist by trying to GET their SSDP device description
void verifyDevices() {
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
        log.debug("verifyDevices ${host}")
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

// deviceDescriptionHandler
// Callback for verifyDevices
void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
    log.debug("deviceDescriptionHandler ${body}")
    log.debug("deviceDescriptionHandler device ${body.device}")
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) {
		device.value << [name: body?.device?.friendlyName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNumber?.text(), verified: true]
        log.debug("device verified ${device.value}")
	}
}

// verifiedDevices
// Return a map of the devices that have been verified
Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
    	log.debug("verifiedDevices ${it}")
		def value = it.value.name ?: "Harmony API Server ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

// getVerifiedDevices
// Return the list of devices which have been verified
def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

// getDevices
// Return the list of all devices
def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

// addDevices
// Add selected devices as child devices
def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.mac == dni }
		def d
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
		}

		if (!d) {
			log.debug "Creating UPnP Device with dni: ${selectedDevice.value.mac}"
            log.debug "Values ${selectedDevice}"
			addChildDevice("jrhurley", "Harmony API - Device Handler", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Harmony API Server",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
		}
	}
}




// Helper functions
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

