/**
 *
 * UPNP Comfort Connect SmartApp
 * Copyright Matt Brain (matt.brain@gmail.com)
 * https://github.com/mattbrain/Cytech-Comfort
 *
 *
 *
 */
definition(
		name: "Cytech Zone UPNP Manager v1",
		namespace: "mattbrain",
		author: "Matt Brain",
		description: "This is a discovery App for instantiating Cytech Comfort UPNP devices",
		category: "SmartThings Labs",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
        singleInstance: true


preferences {
	page(name: "searchTargetSelection", title: "UPnP Search Target", nextPage: "deviceDiscovery") {
		section("Search Target") {
			input "searchTarget", "string", title: "Search Target", defaultValue: "urn:www.cytech.com:service", required: true
		}
	}
	page(name: "deviceDiscovery", title: "UPnP Device Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {
	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
	//	def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
    	def value = it.value.name ?: "UPnP Device ${it.value.name}"
	//	def key = it.value.mac
    	def key = it.value.ssdpPath
		options["${key}"] = value
	}
	log.debug "Starting ssdpSubscribe"
	ssdpSubscribe()
	log.debug "Starting sspdDiscover"
	ssdpDiscover()
    log.debug "Starting VerifyDevices"
	verifyDevices()

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your Cytech Comfort UPnP Devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", submitOnChange: true, multiple: true, options: options
		}
	}
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
	unsubscribe()
	unschedule()

	ssdpSubscribe()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("ssdpDiscover")
}

def uninstalled() {
	if(getChildDevices()) {
    	removeChildDevices(getChildDevices())
    }
}      

void ssdpDiscover() {
	log.debug "looking for devices ${searchTarget}"
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
}

Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		// def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
        def value = it.value.name ?: "UPnP Device ${it.value.name}"
		//def key = it.value.mac
        def key = it.value.ssdpPath
		map["${key}"] = value
	}
	map
}

void verifyDevices() {
	def devices = getDevices().findAll { it?.value?.verified != true }
	devices.each {
		int port = convertHexToInt(it.value.deviceAddress)
		String ip = convertHexToIP(it.value.networkAddress)
		String host = "${ip}:${port}"
        log.debug "Pulling xml from ${it.value.ssdpPath} $host"
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
	def devices = getDevices()
   
	selectedDevices.each { 
    	log.debug "Looking for ${it}"
        def findme = it
		def selectedDevice = devices.find { it.value.ssdpPath == findme }
        
		if (selectedDevice) {
        	// Check and create master device
            def masterDevice = getChildDevices()?.find { it.deviceNetworkId == selectedDevice.value.mac }
            if (!masterDevice) {
                log.debug "Unable to find master device"
                masterDevice = addChildDevice("mattbrain","ComfortAlarm Bridge", selectedDevice.value.mac, selectedDevice?.value.hub, [
                    "label": "Comfort Bridge",
                ])
                masterDevice.setbridgeaddress(selectedDevice.value.networkAddress, selectedDevice.value.deviceAddress)
            } else {
                log.debug "Found a master device ${masterDevice}"
            }
			// Check and create child device
        	def d
			d = getChildDevices()?.find {
				it.deviceNetworkId == "${selectedDevice.value.networkAddress}:${selectedDevice.value.deviceAddress}"
			}
            if (!d) {
            	def deviceType = "None"
            	if (selectedDevice.value.urn == "urn:www.cytech.com:service:zone:1") {
                	deviceType = "ComfortAlarm Zone"
                
                } else if (selectedDevice.value.urn == "urn:www.cytech.com:service:output:1") {
                	deviceType = "ComfortAlarm Output"
                } else if (selectedDevice.value.urn == "urn:www.cytech.com:service:counter:1") {
                	deviceType = "ComfortAlarm Counter"
                } else if (selectedDevice.value.urn == "urn:www.cytech.com:service:flag:1") {
                	deviceType = "ComfortAlarm Flag"
                } else if (selectedDevice.value.urn == "urn:www.cytech.com:service:alarm:1") {
                	deviceType = "ComfortAlarm Control"
                }
               	if (deviceType != "None") {
                	log.debug "Creating ${deviceType}: ${selectedDevice.value.ssdpPath}"
                	
                	d = addChildDevice("mattbrain", deviceType, "${selectedDevice.value.networkAddress}:${selectedDevice.value.deviceAddress}", selectedDevice?.value.hub, [
                	"label": selectedDevice?.value?.name ?: "Generic UPnP Device",
                    "data": [
                    	"id": selectedDevice.value.ssdpPath,
                        "bridge": selectedDevice.value.mac,
                        "ip": selectedDevice.value.networkAddress,
                        "port": selectedDevice.value.deviceAddress,
						"urn": selectedDevice.value.urn,
                        "subURL": "/upnp/service/events?usn=${selectedDevice.value.ssdpSubUSN}",
                        "controlURL": "/upnp/service/control?usn=${selectedDevice.value.ssdpSubUSN}",
                        "SCPDURL": "/upnp/service/desc.xml?usn=${selectedDevice.value.ssdpSubUSN}", 
                    ]
                	])
                	log.debug "MasterDevice ${masterDevice}"                      
           			d.subscribe()
                }
            }
        }
	}
}

def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    String ssdpSubUSN = ssdpUSN
    ssdpSubUSN -= "uuid:".trim()
    parsedEvent << ["ssdpSubUSN":ssdpSubUSN]
	def devices = getDevices()
	if (devices."${ssdpUSN}") {
    	// has anything changed since we last saw it
		def d = devices."${ssdpUSN}"
        //log.debug "SSDP SubUSN: ${ssdpSubUSN}"
        //d.ssdpSubUSN = ssdpSubUSN
		//if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
		//	d.networkAddress = parsedEvent.networkAddress
		//	d.deviceAddress = parsedEvent.deviceAddress
			//def child = getChildDevice(parsedEvent.mac)
			//if (child) {
			//	child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			//}
		//}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getDevices()
    log.debug "-> response ${body?.device?.UDN?.text()}"
	//def device = devices.find{it.key == "${body?.device?.UDN?.text()}::urn:www.cytech.com:service:zone:1"}
    def device = devices.find {it?.key?.contains("${body?.device?.UDN?.text()}::urn:www.cytech.com:service")}
	if (device) {
    	log.debug "Found device: ${device.key}"
		device.value << [name: body?.device?.friendlyName.text()]
        device.value << [urn: body?.device?.serviceList?.service?.serviceType?.text()] 
        //device.value << [subURL: body?.device?.serviceList?.service?.eventSubURL?.text()]
        //device.value << [scpdURL: body?.device?.serviceList?.service?.SCPDURL?.text()]
        //device.value << [controlURL: body?.device?.serviceList?.service?.controlURL?.text()]
        device.value << [verified: true]
		log.debug "Found device description: ${device.value}"
    }
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

void renewSubscriptions() {
	log.debug "Subscription renewal requested"
    getChildDevices().each {
    	it.renewSubscription()
    }
}
void processEvent(childId, sid, item, value)	{
	log.debug "->processEvent ${childId}, ${item}, ${value}"
	def childDevice = getChildDevices()?.find { it.deviceNetworkId == childId }
    if (childDevice) {
    	log.debug "sending event ${item}:${value} to ${childId}"
    	childDevice.processEvent(sid, item, value)
    } else {
    	log.debug "unable to find child device"
    }
}

void addResponse() {
	log.debug "->addResponse"
    addChildDevice("mattbrain", "ComfortAlarm Response", "ComfortAlarm Response:"+now(),location.hubs[0].id)
}

void processResponse(response) {
	def controlDevice = getChildDevices()?.find { it.name == "ComfortAlarm Control" }
    if (controlDevice) {
    	log.debug "Found ComfortAlarm Controller"
 		controlDevice.processResponse(response)
 	}
    else {
    	log.debug "Unable to find ComfortAlarm Controller"
    }
}    