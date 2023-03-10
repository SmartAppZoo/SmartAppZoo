/*
 *  UPnP Service Manager
 *  // Direct port of Smartthings published UPNP service manager
 *  // Extend with comments to serve as a tutorial SmartApp
 *  // Syntax info provided from Groovy Noobies and Smartthings dummies (like me)
 */
definition(
		name: "Generic UPnP Service Manager",
		namespace: "milksteakmatt",
		author: "Matthew Nichols",
		description: "UPnP Service Manager SmartApp for home automation projects",
		category: "My Apps",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    // Preferences page for capturing search target of UPNP device
	page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
		section("Search Target") {
			input "searchTarget", "string", title: "Search Target", defaultValue: "urn:schemas-upnp-org:device:ZonePlayer:1", required: true
		}
	}
    // Preferences page for auto finding and adding new UPNP devices
	page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {
    // Create empty options map 
	def options = [:]
    
    // Get a map of varified devices from the app state variable
	def devices = getVerifiedDevices()

    // Populate the options map with verified devices
    devices.each {
        // Append "UPnP Device" to the USN
        // GROOVY: the parameter for the "each" function is a closure.  When a function parameter is a closure, () do not wrap the parameter
        // GROOVY: ${} will insert a variable into a string
        // GROOVY: split accepts a delineator.  In this case ":"
        // GROOVY: "it" is an keyword that refers to the first unnamed closure parameter.  In this case, each device is the parameter for the closure
        // GROOVY: "?:" is a shortened ternary operator called the Elvis operator. If the device name is null, create the name from the SSDP USN
        // GROOVY: 
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.mac
        // Use the device mac address as the map key and the 
		options["${key}"] = value
	}
    
    // Subscribe to SSDP events so if a response comes in, they can be sent to a handler
	ssdpSubscribe()

    // Multicast a UPNP discovery message so devices can respond and be sent to the handler
	ssdpDiscover()
    
    // Make sure all the devices are still present and active
	verifyDevices()

    // SMARTTHINGS: Create a dynamic page with the map of devices
	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait. Retry set to 5 minutes. Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

// 
def installed() {
	log.debug "Installed with settings: ${settings}"

    // 
	initialize()
}

// 
def updated() {
	log.debug "Updated with settings: ${settings}"

    //
	unsubscribe()
    
    // 
	initialize()
}

// 
def initialize() {
    //
	unsubscribe()
    
    //
	unschedule()

    //
	ssdpSubscribe()

    //
	if (selectedDevices) {
		addDevices()
	}

    //
	runEvery5Minutes("ssdpDiscover")
}

// Convenience method to multicast a UPNP discovery request for the search target
// SMARTTHINGS: sendHubCommand uses HubAction to wrap a command and send it to the LAN protocol
void ssdpDiscover() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

// Convenience method to subscribe current hub to ssdpTerm events for the specified search target and fire ssdpHandler when found
// SMARTTHINGS: All SmartApps and Device Handlers are injected with a "location" property that is the Location into which the SmartApp is installed.
void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
}

// 
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

// 
void verifyDevices() {
    // 
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

// Get all of the verified devices
def getVerifiedDevices() {
    // Return all devices that have a verified value of "true"
    // GROOVY: pass a closure into "findAll" function which returns a subset based on the closure criteria
	getDevices().findAll{ it.value.verified == true }
}

// Get all of the devices
def getDevices() {
    // If no devices exist in the app state variable, create an empty map
	if (!state.devices) {
		state.devices = [:]
	}
    // Return the map of devices from the state variable 
    // GROOVY: "return" is not required.  Last statement is returned by default.
	state.devices
}

// Add devices
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
			log.debug "Creating Generic UPnP Device with dni: ${selectedDevice.value.mac}"
			addChildDevice("smartthings", "Generic UPnP Device", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Generic UPnP Device",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
		}
	}
}

// Parse SSDP event
def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]

	def devices = getDevices()
	String ssdpUSN = parsedEvent.ssdpUSN.toString()
	if (devices."${ssdpUSN}") {
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
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

// 
void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) {
		device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
	}
}

// Convenience method to convert hexidecimal to int
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

// Convenience method to convert hexidecimal to an IP address
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
