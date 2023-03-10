/**
 *  Samsung TV Service Manager
 *
 *  Author: SmartThings (Juan Risso - juano23@gmail.com)
 */
 
definition(
	name: "LAN Device (Connect)",
	namespace: "smartthings",
	author: "SmartThings",
	description: "Allows you to control your device from the SmartThings app.",
	category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Samsung/samsung-remote%402x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Samsung/samsung-remote%403x.png"
)

preferences {
	page(name:"deviceDiscovery", title:"Device Setup", content:"deviceDiscovery", refreshTimeout:5)
}

def getDeviceType() {
	return "urn:samsung.com:device:RemoteControlReceiver:1" //Samsung TV
	//return "urn:dial-multiscreen-org:device:dial:1" //Chromecast
    //return "urn:schemas-upnp-org:device:basic:1" //Hue
}

def getDeviceName() {
	return "Samsung Smart TV"
	//return "Google Chromecast"  
	//return "Philips Hue"    
}

def getNameSpace() {
	return "smartthings"
}

//PAGES
def deviceDiscovery()
{
	if(canInstallLabs())
	{
		int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
		state.deviceRefreshCount = deviceRefreshCount + 1
		def refreshInterval = 3

		def options = devicesDiscovered() ?: []

		def numFound = options.size() ?: 0
		
        if(!state.subscribe) {
            subscribe(location, null, locationHandler, [filterEvents:false])
            state.subscribe = true
        }

		//device discovery request every 5 //25 seconds
		if((deviceRefreshCount % 5) == 0) {
        	log.trace "Discovering..."			
			discoverDevices()
		}

		//setup.xml request every 3 seconds except on discoveries
		if(((deviceRefreshCount % 1) == 0) && ((deviceRefreshCount % 8) != 0)) {
            log.trace "Verifing..."			
			verifyDevices()
		}

		return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your ${getDeviceName()}. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selecteddevice", "enum", required:false, title:"Select ${getDeviceName()} (${numFound} found)", multiple:true, options:options
			}
		}
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"deviceDiscovery", title:"Upgrade needed!", nextPage:"", install:true, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

def installed() {
	log.trace "Installed with settings: ${settings}"   
	initialize()
}

def updated() {
	log.trace "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def uninstalled() {
	def devices = getChildDevices()
	log.trace "deleting ${devices.size()} device"
	devices.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def initialize() {
	state.subscribe = false
	if (selecteddevice) {
		addDevice()
        runEvery5Minutes("discoverDevices")
	}	
}

//CHILD DEVICE METHODS
def addDevice(){
	def devices = getVerifiedDevice()
    log.trace "Adding childs" 
	selecteddevice.each { dni ->
		def d = getChildDevice(dni)
		if(!d) {
			def newDevice = devices.find { (it.value.mac) == dni }
            def deviceName = getDeviceName() + "[${newDevice?.value.name}]"    
			d = addChildDevice(getNameSpace(), getDeviceName(), dni, newDevice?.value.hub, [label:"${deviceName}"])
			log.trace "Created ${d.displayName} with id $dni"
		} else {
			log.trace "${d.displayName} with id $dni already exists"
		}
	}
}

private tvAction(key,deviceNetworkId) {
    log.debug "Executing ${key}"
    
	def tvs = getVerifiedDevice()
	def thetv = tvs.find { (it.value.mac) == deviceNetworkId }
    
    // Standard Connection Data
    def appString = "iphone..iapp.samsung"
    def appStringLength = appString.getBytes().size()

    def tvAppString = "iphone.UN60ES8000.iapp.samsung"
    def tvAppStringLength = tvAppString.getBytes().size()

    def remoteName = "SmartThings".encodeAsBase64().toString()
    def remoteNameLength = remoteName.getBytes().size()

    // Device Connection Data
    def ipAddress = convertHexToIP(thetv?.value.networkAddress).encodeAsBase64().toString()
    def ipAddressHex = thetv?.value.networkAddress
    def ipAddressLength = ipAddress.getBytes().size()
    
    def macAddress = thetv?.value.mac.encodeAsBase64().toString()
    def macAddressLength = macAddress.getBytes().size()

    // The Authentication Message
    def authenticationMessage = "${(char)0x64}${(char)0x00}${(char)ipAddressLength}${(char)0x00}${ipAddress}${(char)macAddressLength}${(char)0x00}${macAddress}${(char)remoteNameLength}${(char)0x00}${remoteName}"
    def authenticationMessageLength = authenticationMessage.getBytes().size()
    
    def authenticationPacket = "${(char)0x00}${(char)appStringLength}${(char)0x00}${appString}${(char)authenticationMessageLength}${(char)0x00}${authenticationMessage}"

    // If our initial run, just send the authentication packet so the prompt appears on screen
    if (key == "AUTHENTICATE") {
	    sendHubCommand(new physicalgraph.device.HubAction(authenticationPacket, physicalgraph.device.Protocol.LAN, "${ipAddressHex}:D6D8"))
    } else {
        // Build the command we will send to the Samsung TV
        def command = "KEY_${key}".encodeAsBase64().toString()
        def commandLength = command.getBytes().size()

        def actionMessage = "${(char)0x00}${(char)0x00}${(char)0x00}${(char)commandLength}${(char)0x00}${command}"
        def actionMessageLength = actionMessage.getBytes().size()

        def actionPacket = "${(char)0x00}${(char)tvAppStringLength}${(char)0x00}${tvAppString}${(char)actionMessageLength}${(char)0x00}${actionMessage}"

        // Send both the authentication and action at the same time
        sendHubCommand(new physicalgraph.device.HubAction(authenticationPacket + actionPacket, physicalgraph.device.Protocol.LAN, "${ipAddressHex}:D6D8"))
    }
}

//DISCOVERY METHODS
def parse(description) {
	def msg = parseLanMessage(description)
    log.info "Here: $msg"
}

def locationHandler(evt) {
	def description = evt.description   
    log.trace "Location: $description"    
    
	def hub = evt?.hubId
 	def parsedLanEvent = parseLanMessage(description, true)    
	parsedLanEvent << ["hub":hub] 
    
	if (parsedLanEvent?.ssdpTerm?.contains(getDeviceType())) { 
    	//SSDP DISCOVERY EVENTS
		log.trace "Device found"
		def devices = getDevice()
		if (!(devices."${parsedLanEvent.ssdpUSN.toString()}")) { 
        	//device does not exist
        	log.trace "Adding Device to state..."
			devices << ["${parsedLanEvent.ssdpUSN.toString()}":parsedLanEvent]
		} else { 
        	// update the values
			log.trace "Device was already found in state..."
			def d = devices."${parsedLanEvent.ssdpUSN.toString()}"
			if(d.ip != parsedLanEvent.networkAddress || d.port != parsedLanEvent.deviceAddress) {
				d.ip = parsedLanEvent.networkAddress
				log.trace "Device's port or ip changed..."
			}
		}
	}
	else if (parsedLanEvent.headers && parsedLanEvent.body) { 
    	// device RESPONSES
		def type = parsedLanEvent.headers."content-type" 
		def body
		log.trace "REPONSE TYPE: $type"
		if (type?.contains("xml")) { 
        	// description response (application/xml)
			body = new XmlSlurper().parseText(parsedLanEvent.body)
            def devicet = getDeviceType().toLowerCase()
            def devicetxml = body.device.deviceType.text().toLowerCase()
            log.trace "$devicetxml == $devicet"
			if (devicetxml == devicet) {
				def devices = getDevice()
				def device = devices.find {it?.key?.contains(body?.device?.UDN?.text())}
				if (device) {
					device.value << [name:body?.device?.friendlyName?.text(),model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
				} else {
					log.error "The xml file returned a device that didn't exist"
				}
			}
		}
		else if(type?.contains("json")) { 
        	//(application/json)
			body = new groovy.json.JsonSlurper().parseText(bodyString)
			log.trace "GOT JSON $body"
		}
	}
	else {
		log.trace "Device not found..."
		//log.trace description
	}
}

Map devicesDiscovered() {
	def vdevices = getVerifiedDevice()
	def map = [:]
    log.trace "Discovered $vdevices"
	vdevices.each {
		def value = "${it.value.name}"
		def key = it.value.mac
		map["${key}"] = value
	}
	map
}

private discoverDevices() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${getDeviceType()}", physicalgraph.device.Protocol.LAN))
}

private verifyDevices() {
	def devices = getDevice().findAll { it?.value?.verified != true }

	if(devices) {
		log.warn "UNVERIFIED DEVICES: $devices"
	}
	devices.each {
		verifyDevice(it?.value?.mac, it?.value?.networkAddress, it?.value?.deviceAddress , it?.value?.ssdpPath)
	}
}

private verifyDevice(String deviceNetworkId, String ip, String port, String devicessdpPath) {
    def address = convertHexToIP(ip) + ":" + convertHexToInt(port)
    if(ip){
        sendHubCommand(new physicalgraph.device.HubAction([
            method: "GET",
            path: devicessdpPath,
            headers: [
                HOST: address
            ]], deviceNetworkId))
    }        
}

def getVerifiedDevice() {
	getDevice().findAll{ it?.value?.verified == true }
}

def getDevice() {
	state.devices = state.devices ?: [:]
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Boolean canInstallLabs()
{
	return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware)
{
	return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions()
{
	return location.hubs*.firmwareVersionString.findAll { it }
}