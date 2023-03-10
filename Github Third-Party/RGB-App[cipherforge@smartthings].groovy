/**
 *  ESP-01 WS2812B RBG LED Service Manager
 *
 */
definition(
		name: "RGB WIFI",
		namespace: "cipherforge",
		author: "cipherforge",
		description: "rgb",
		category: "SmartThings Labs",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
		section("Search Target") {
			input "searchTarget", "string", title: "Search Target", defaultValue: "urn:schemas-upnp-org:device:rgb:1", required: true
		}
	}
	page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
}

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

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your UPnP Device. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}


def updated() {
    log.debug "bork Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    log.debug "bork init"
	unsubscribe()
	unschedule()
	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("ssdpDiscover")
}

void ssdpDiscover() {
	log.debug "bork ssdp discover"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:rgb:1", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
    log.debug "bork ssdp subscribe"
	//subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandle)
    subscribe(location, null, ssdpHandle, [filterEvents:false])
}


Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

// call device and get info from info
void verifyDevices() {
	log.debug "bork verifyDevices"
	def devices = getDevices().findAll { it?.value?.verified != true }
    log.debug devices
    log.debug getDevices();
    
	devices.each {
        log.debug "verify devices each" 
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress) 
		String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET /description.xml HTTP/1.1\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
    
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
    log.debug "bork getDevices()"
	if (!state.devices) {
        log.debug "no devices"
		state.devices = [:]
	}
    state.devices
}

def addDevices() {
	def devices = getDevices()
	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.mac == dni }
		def d
		if (selectedDevice) {
        	log.debug "updating"
			d = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
		}

		if (!d) {
			log.debug "Creating generic RGH with dni: ${selectedDevice.value.mac}"
			addChildDevice("smartthings", "Generic UPnP Device", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "RGB",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": convertHexToIP(selectedDevice.value.networkAddress),
					"port": convertHexToInt(selectedDevice.value.deviceAddress)
				]
			])
		}
	}
}

// Process Response from SSDP Search
def ssdpHandle(evt) {
    log.debug "bork ssdpHandle"
    
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
    log.debug parsedEvent
    
    
    //  [devicetype:04, mac:DC4F2211EAB0, networkAddress:C0A8018C, deviceAddress:0050, stringCount:03, ssdpPath:/description.xml, ssdpUSN:uuid:38323636-4558-4dda-9188-cda0e611eab0, ssdpTerm:urn:schemas-upnp-org:device:Basic:1]
    
	parsedEvent << ["hub":hub]
	def devices = getDevices()
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
	if (devices?."${ssdpUSN}") {
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
        log.debug "saving new device"
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

// callback from verifyDevices
void deviceDescriptionHandler(hubResponse) {
	def body = hubResponse.xml

    
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.USN?.text()) }

    if (device) {
		device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
