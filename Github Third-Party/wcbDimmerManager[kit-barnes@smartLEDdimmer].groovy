/**
 *  WCB Dimmer Manager
 *  SmartThings Smart App
 *  finds WCBdimmers, associates them with device hander and periodicly syncs them
 */
definition(
	name: "WCB Dimmer Manager",
	namespace: "kit-barnes",
	author: "Kit Barnes",
	description: "UPnP Service Manager finds WCBdimmers, associates them with device handler and periodicly syncs them",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "dimmerDiscovery")
}

def dimmerDiscovery() {
	log.debug "In dimmerDiscovery"
	ssdpSubscribe()
	ssdpDiscover()
	verifyDevices()
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name
		def key = it.value.mac
//		if (value.startsWith("wcbDimmer")) { options["${key}"] = value }
		options["${key}"] = value
	}
	return dynamicPage(name: "dimmerDiscovery", title: "Searching for dimmers",
	  nextPage: "", refreshInterval: 10, install: true, uninstall: true) {
		section("Please wait while we discover your dimmer(s). Discovery may take a few minutes. Select your device(s) below once discovered.") {
			input "selectedDevices", "enum", required: false,
			title: "Select Devices (${options.size() ?: 0} found)",
			multiple: true, options: options
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
	if (selectedDevices) {
		addDevices()
	}
	ssdpSubscribe()
	runEvery15Minutes("ssdpDiscover")
}

void ssdpDiscover() {
	def searchTarget = "urn:schemas-upnp-org:device:DimmableLight:1"
	log.debug "lan discovery ${searchTarget}"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
	def searchTarget = "urn:schemas-upnp-org:device:DimmableLight:1"
	log.debug "subscribe location = ${location}"
	subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
}

void verifyDevices() {
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def addDevices() {
	log.debug "WCB svc mgr in addDevices"
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
			log.debug "Creating WCB 12vLED Dimmer Device with dni: ${selectedDevice.value.mac}"
			log.trace "selectedDevice: ${selectedDevice}"
			addChildDevice("kit-barnes", "WCB 12vLED Dimmer",
				selectedDevice.value.mac, selectedDevice.value.hub, [
				"label": selectedDevice.value.name ?: "MAC:${selectedDevice.value.mac}",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress,
					"manufacturer": selectedDevice.value.manufacturer,
					"model": selectedDevice.value.model
				]
			]).refresh();		// send hub address and get back current attribute values
		}
	}
}

def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
	if (devices."${ssdpUSN}") {
		log.debug "Discovered existing device"
		def d = devices."${ssdpUSN}"
		def child = getChildDevice(parsedEvent.mac)
		if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
			log.debug "Discovered changed device address"
			d.networkAddress = parsedEvent.networkAddress
			d.deviceAddress = parsedEvent.deviceAddress
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			}
		}
		if (child) {
			log.debug "Refreshing existing child device"
			child.refresh()
		}
	} else {
		log.debug "Discovered new device"
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "In deviceDescriptionHandler"
	def body = hubResponse.xml
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) {
		log.debug "device verified"
		device.value << [
			name: body?.device?.friendlyName?.text(),
			model: body?.device?.modelName?.text(),
			manufacturer: body?.device?.manufacturer?.text(),
			verified: true
		]
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[ convertHexToInt(hex[0..1]), convertHexToInt(hex[2..3]),
		convertHexToInt(hex[4..5]), convertHexToInt(hex[6..7]) ].join(".")
}