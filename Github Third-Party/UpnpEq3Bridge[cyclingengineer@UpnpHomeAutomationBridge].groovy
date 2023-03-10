/**
 *  UPnP Eq3 Bridge
 *
 *  Copyright 2015 Paul Hampson
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
    name: "UPnP EQ3 Bridge",
    namespace: "cyclingengineer",
    author: "Paul Hampson",
    description: "This smart app interfaces with the UPnP Home Automation bridge software to support EQ-3 MAX! Heating Control System",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name:"setupPage1", title:"UPnP EQ-3 MAX! Bridge Setup", content:"setupPage1", refreshTimeout:5, nextPage:"setupPage2")
    page(name:"setupPage2", title:"UPnP EQ-3 MAX! Bridge Setup", content:"setupPage2", refreshTimeout:5)
}

// PAGES
def setupPage1()
{
	log.trace "setupPage1"
	if(canInstallLabs())
	{
		int upnpHabRefreshCount = !state.upnpHabRefreshCount ? 0 : state.upnpHabRefreshCount as int
		state.upnpHabRefreshCount = upnpHabRefreshCount + 1
		def refreshInterval = 3

		def options = eq3BridgesDiscovered() ?: []

		def numFound = options.size() ?: 0

		if(!state.subscribe) {
			log.trace "subscribe to location"
			subscribe(location, null, locationHandler, [filterEvents:false])
			state.subscribe = true
		}

		//sonos discovery request every 5 //25 seconds
		if((upnpHabRefreshCount % 8) == 0) {
			discoverUpnpHAB()
		}

		//setup.xml request every 3 seconds except on discoveries
		if(((upnpHabRefreshCount % 1) == 0) && ((upnpHabRefreshCount % 8) != 0)) {
			verifyDiscoveredHvacDevices()
		}

		return dynamicPage(name:"setupPage1", title:"Discovery Started!", nextPage:"setupPage2", refreshInterval:refreshInterval, install:false, uninstall: true) {
			section("Please wait while we discover your EQ3 Systems. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selectedEq3Systems", "enum", required:false, title:"Select EQ3 System (${numFound} found)", multiple:true, options:options
			}
		}
	}
	else
	{
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

		return dynamicPage(name:"setupPage1", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section("Upgrade") {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

def setupPage2() {
	log.trace "setupPage2"
	def options = eq3GetSelectedSystemDiscoveredRoomList( ) ?: []
	def numFound = options.size() ?: 0

	return dynamicPage(name:"setupPage2", title:"Select Rooms", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
			section("Please select the rooms you wish to control") {
				input "selectedEq3Rooms", "enum", required:false, title:"Select Rooms (${numFound} found)", multiple:true, options:options
			}
    }
}


private discoverUpnpHAB() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:HVAC_System:1", physicalgraph.device.Protocol.LAN))
}

private verifyDiscoveredHvacDevices() {
	log.trace "verifyDiscoveredHvacDevices"
	def devices = getEq3BridgeSystemList().findAll { it?.value?.verified != true }

	if(devices) {
		log.warn "UNVERIFIED DEVICES!: $devices"
	}

	devices.each {
		verifyEq3Bridge((it?.value?.ip + ":" + it?.value?.port), it.value.ssdpPath)
	}
}

private verifyEq3Bridge(String deviceNetworkId, String descPath) {
	log.trace "verifyEq3Bridge"
	log.trace "dni: $deviceNetworkId"
	String ip = getHostAddress(deviceNetworkId)

	log.trace "ip:" + ip
    log.trace "descPath:" + descPath

	sendHubCommand(new physicalgraph.device.HubAction("""GET ${descPath} HTTP/1.1\r\nHOST: ${ip}\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

private Map eq3BridgesDiscovered() {
	log.trace "eq3BridgesDiscovered"
	def verifiedBridges = getVerifiedEq3Bridges()
	def map = [:]
	verifiedBridges.each {
		def value = "${it.value.name}"
		def key = it.key
		map["${key}"] = value
	}
	map
}

private def getEq3BridgeSystemList()
{
	log.trace "getEq3BridgeSystemList"
	state.eq3BridgeHvacSystems = state.eq3BridgeHvacSystems ?: [:]
}

private def getVerifiedEq3Bridges()
{
	log.trace "getVerifiedEq3Bridges"
	getEq3BridgeSystemList().findAll{ it?.value?.verified == true }
}

// Return details all of the eq3 bridges that have been selected by the user
private def getSelectedEq3Bridges()
{
	log.trace "getSelectedEq3Bridges"
    
    // condition selected systems into a list
    def selectedEq3SystemsSettingsList = getSelectedSystemsAsList()
    
    def verifiedSystems = getVerifiedEq3Bridges() ?: [:]
    def selectedSystemsList = []
    selectedEq3SystemsSettingsList.each {
    	def systemUdn = ""+it?.value                
    	def system = verifiedSystems.find{ it?.key == systemUdn }
        selectedSystemsList << system
    }
    selectedSystemsList
}

// Get a list of rooms discovered for each bridge system
private Map eq3GetSelectedSystemDiscoveredRoomList( )
{
	log.trace "eq3GetSelectedSystemRoomList"
	
    def matchingSystems = getSelectedEq3Bridges()    
    
    def map = [:]
	matchingSystems.each {    	
        it?.value?.rooms?.each {
        	def value = "${it.name}"
			def key = "${it.udn}"
			map["${key}"] = value
        }
    }
    map
}

private def eq3GetRoomList( )
{
	log.trace "eq3GetRoomList"
	state.eq3BridgeRoomList = state.eq3BridgeRoomList ?: [:]
}

// condition selected systems into a list
private def getSelectedSystemsAsList()
{
    def selectedEq3SystemsSettingsList = [] 
    if (settings.selectedEq3Systems instanceof List) {
    	selectedEq3SystemsSettingsList = settings.selectedEq3Systems
    } else {    	
        selectedEq3SystemsSettingsList.add(settings.selectedEq3Systems)
    }
    return selectedEq3SystemsSettingsList
}

// condition selected rooms into a list
private def getSelectedRoomsAsList()
{
    def selectedEq3RoomsSettingsList = [] 
    if (settings.selectedEq3Rooms instanceof List) {
    	selectedEq3RoomsSettingsList = settings.selectedEq3Rooms
    } else {    	
        selectedEq3RoomsSettingsList.add(settings.selectedEq3Rooms)
    }
    return selectedEq3RoomsSettingsList
}

def addZones() {
	def selectedEq3Systems = getSelectedSystemsAsList()
    def selectedEq3Rooms = getSelectedRoomsAsList()
    def devices = getVerifiedEq3Bridges()
    
    selectedEq3Systems.each { systemDni ->
    	def b = getChildDevice(systemDni)
        def newBridge = devices.find { it.key == systemDni }
        if (!b) {
        	// create new bridge device that handles catching the returning messages
        	b = addChildDevice("cyclingengineer", "UPnP Home Automation Bridge", systemDni, newBridge?.value.hub, 
            	["label": newBridge?.value.name,
                 "data":[
                 	"mac": newBridge?.value.mac,  
                    "ip": newBridge?.value.ip,  
                    "port": newBridge?.value.port,  
                    "bridge": true
                   	]
                ]
            )
            log.trace "Created ${b.displayName} with id ${systemDni}"
        }
        
        b.sendEvent(name: "udn", value: newBridge?.key)
        b.sendEvent(name: "networkAddress", value: newBridge?.value.ip +":" + newBridge?.value.port)
    	selectedEq3Rooms.each { roomUdn ->
        	def dni = roomUdn
        	def d = getChildDevice(dni)
            def emptyString = ","
        	if (!d) {
    			// add each room as a child device
        		log.trace "Add child device for "+dni     
                log.trace newBridge
                def newRoom = newBridge.value.rooms.find{ it.udn == roomUdn }
                d = addChildDevice("cyclingengineer", "Upnp Bridge Eq3 Room Heating Controller", dni, newBridge?.value.hub, 
                					["label":newRoom?.name,
                                     "data":[
                                     	"secretip":newBridge?.value.ip,
                                        "secretport":newBridge?.value.port,
                                        "secretmac":newBridge?.value.mac,
                					 	"udn":roomUdn, 
                                     	"ZoneTemperatureServiceEventUrl":newRoom?.ZoneTemperatureServiceEventUrl,
                                     	"HeatingSetpointServiceEventUrl":newRoom?.HeatingSetpointServiceEventUrl,
                                     	"HeatingValveServiceEventUrl":newRoom?.HeatingValveServiceEventUrl,
                                        "requestList":emptyString, // turns out it doesn't actually support lists, so we'll need to do it in a string
                                        ]
                                     ])                
    		}
        }
	}
}

def subscribeToDevices() {
	log.debug "subscribeToDevices() called"
	def devices = getAllChildDevices()
	devices.each { d ->
		//d.subscribe()
	}
}

def pollDevices() {
	log.debug "pollDevices() called"
	def devices = getAllChildDevices()
	devices.each { d ->
		log.debug "Calling poll() on device: ${d.id}"
		d.poll()
	}
    // refresh automatically every 5 minutes
    runIn(300, "pollDevices")
}

def refreshDevices() {
	log.debug "refreshDevices() called"
	def devices = getAllChildDevices()
	devices.each { d ->
		log.debug "Calling refresh() on device: ${d.id}"
		d.refresh()
	}
    // refresh automatically every 5 minutes
    runIn(300, "pollDevices")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	
    //runIn(5, "subscribeToDevices") // subscribe to all children, delayed by 5 seconds
    //runIn(10, "refreshDevices") //refresh devices, delayed by 10 seconds
    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
	initialize()
    
    runIn(5, "subscribeToDevices") // resubscribe to all children, delayed by 5 seconds
    runIn(10, "refreshDevices") // refresh devices, delayed by 10 seconds
}

def initialize() {
	unsubscribe()
    state.subscribe = false
    
    if (selectedEq3Systems && selectedEq3Rooms)
    {
    	addZones()
    }
}

def locationHandler(evt) {
	log.trace "locationHandler"
	def description = evt.description
	def hub = evt?.hubId

	def parsedEvent = parseEventMessage(description)
	parsedEvent << ["hub":hub]

	if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:HVAC_System:1"))
	{ //SSDP DISCOVERY EVENTS

		log.trace "HVAC system found"
		def eq3BridgeSystems = getEq3BridgeSystemList()

		if (!(eq3BridgeSystems."${parsedEvent.udn.toString()}"))
		{ //hvac system does not exist
			eq3BridgeSystems << ["${parsedEvent.udn.toString()}":parsedEvent]
		}
		else
		{ // update the values

			log.trace "Device was already found in state..."

			def d = eq3BridgeSystems."${parsedEvent.udn.toString()}"
			boolean deviceChangedValues = false

			if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
				d.ip = parsedEvent.ip
				d.port = parsedEvent.port
                d.mac = parsedEvent.mac
                d.ssdpPath = parsedEvent.ssdpPath
				deviceChangedValues = true
				log.trace "Device's port or ip changed..."
			}

			if (deviceChangedValues) {
            	log.trace "Updating child device"
				def children = getChildDevices()
				children.each {
					if (it.getDeviceDataByName("secretmac") == parsedEvent.mac) {
						log.trace "updating up & port for device ${it} with mac ${parsedEvent.mac}"
						it.setDeviceDataByName("secretip", parsedEvent.ip)
                        it.setDeviceDataByName("secretport", parsedEvent.port)
                        if (it.getDeviceDataByName("bridge")) {                        	
        					it.sendEvent(name: "networkAddress", value: parsedEvent.ip +":" + parsedEvent.port)
                        }
					}
                }
			}
		}
	} else if (parsedEvent.headers && parsedEvent.body)
	{ // BRIDGED EQ3 RESPONSES
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())

		def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*?/)[0] : null
		def body
        
		if (type?.contains("xml")||headerString?.contains("xml"))
		{ // description.xml response (application/xml)
			body = new XmlSlurper().parseText(bodyString)			
            
			if (body?.device?.modelName?.text().contains("EQ3 Bridge"))
			{
				def eq3Bridges = getEq3BridgeSystemList()                
				def eq3System = eq3Bridges.find {it?.key?.contains(body?.device?.UDN?.text()-"uuid:")}
				if (eq3System)
				{
                	def foundRoomRaw = body?.device?.deviceList?.device?.find { it?.deviceType?.text().contains( "HVAC_ZoneThermostat" ) }
                    log.trace "Found " + foundRoomRaw.size() +" thermostat zones"
                    def foundRoomList = []
                    foundRoomRaw.each {
                    	def foundRoomDataMap = [:]
                        def roomZoneTemperatureService = it?.serviceList?.service?.find { it?.serviceType?.text() == "urn:schemas-upnp-org:service:TemperatureSensor:1" &&  it?.serviceId?.text() == "urn:upnp-org:serviceId:ZoneTemperature" }
                        def roomHeatingSetpointService = it?.serviceList?.service?.find { it?.serviceType?.text() == "urn:schemas-upnp-org:service:TemperatureSetpoint:1" &&  it?.serviceId?.text() == "urn:upnp-org:serviceId:HeatingSetpoint" }
                        def roomHeatingValveService = it?.serviceList?.service?.find { it?.serviceType?.text() == "urn:schemas-upnp-org:service:ControlValve:1" &&  it?.serviceId?.text() == "urn:upnp-org:serviceId:HeatingValve" }
                        foundRoomDataMap << ["name":it?.friendlyName?.text(), 
                        					 "udn":it?.UDN?.text(), 
                                             "ZoneTemperatureServiceEventUrl":roomZoneTemperatureService?.eventSubURL?.text(), 
                                             "HeatingSetpointServiceEventUrl":roomHeatingSetpointService?.eventSubURL?.text(), 
                                             "HeatingValveServiceEventUrl":roomHeatingValveService?.eventSubURL?.text(),                                              
                                             ]
                        foundRoomList << foundRoomDataMap
                    }
					eq3System.value << ["name":body?.device?.friendlyName?.text(), "verified": true, "rooms":foundRoomList]                                         
                    eq3System.value.rooms.each {
                    	log.trace "Found thermostat zone: "+it.name
                    }                    
				}
				else
				{
					log.error "XML descriptors returned a device that didn't exist"
				}
			}
		}
		else if(type?.contains("json"))
		{ //(application/json)
			body = new groovy.json.JsonSlurper().parseText(bodyString)
			log.trace "GOT JSON $body"
		}
        else {
        	log.trace "Unknown response: $type"
        	log.trace "Body: ${bodyString}"
        }

	}
	else {
		log.trace "cp desc: " + description
		//log.trace description
	}
    log.trace("end locationHandler")
}
private def parseEventMessage(Map event) {
	//handles attribute events
	return event
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
                if (valueString.startsWith('uuid:')) {
                	valueString -= "uuid:"
                    def valueUdn = valueString.split(":")[0].trim()
                    if (valueUdn) {
                    	event.udn = valueUdn
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
		else if (part.startsWith('headers')) {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString) {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body')) {
			part -= "body:"
			def valueString = part.trim()
			if (valueString) {
				event.body = valueString
			}
		}
        else if (part.startsWith('requestId')) {
			part -= "requestId:"
			def valueString = part.trim()
			if (valueString) {
				event.requestId = valueString
			}
		}
        //else log.trace "part = ${part}"
	}

	event
}    

/////////CHILD DEVICE METHODS
def parse(childDevice, description) {
	log.trace "parent parse"
    
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		log.trace "parse() - ${bodyString}"

		def type = (headerString =~ /Content-Type:.*/) ? (headerString =~ /Content-Type:.*?/)[0] : null
		def body
        
		if (type?.contains("xml")||headerString?.contains("xml"))
		{ // XML response
			body = new XmlSlurper().parseText(bodyString)
            // search through child devices to see which one this request belongs to
            getAllChildDevices().each { d ->
            	//if (d.getDeviceDataByName("reqList")?.contains("${parsedEvent.requestId}")) {                	
                	d.requestResponse(parsedEvent.requestId, bodyString)                	
                //}
            }
            return [requestId:parsedEvent.requestId, body: bodyString]
            // parse the body and trigger event on correct child device
        }        
        
	} else {
		log.trace "parse - got something other than headers,body..."
		return []
	}
}

// execute a Upnp action - only from actual child devices, not bridge devices
def doUpnpAction(action, service, path, Map body, childDevice) {
	log.debug "doUpnpAction(${action}, ${service}, ${path}, ${body}, ...)"
    def mac = childDevice.getDeviceDataByName("secretmac")
    def ip = convertHexToIP(childDevice.getDeviceDataByName("secretip"))
    def port = convertHexToInt(childDevice.getDeviceDataByName("secretport"))
    Map soapMap = [
    	path:    path,
        urn:     "urn:schemas-upnp-org:service:$service:1",
        action:  action,
        body:    body,
        headers: [Host:"${ip}:${port}", CONNECTION: "close"]
        ]
    def hubaction = new physicalgraph.device.HubSoapAction(soapMap, mac)
    log.trace hubaction
    sendHubCommand(hubaction)    
    return hubaction.requestId // our response event description will have a matching requestId    
}

// subscribe to a UPnP event
private subscribeUpnpAction(path, callbackAddress, callbackPath="") {
    log.trace "subscribe($path, $callbackAddress, $callbackPath)"        
    def ip = getHostAddress()
	Map actionMap = [
    	method: "SUBSCRIBE",
        path: path,
        headers: [
            HOST: ip,
            CALLBACK: "<http://${callbackAddress}/notify$callbackPath>",
            NT: "upnp:event",
            TIMEOUT: "Second-28800"
        	]
        ]
    def action = new physicalgraph.device.HubAction(actionMap, mac)    
	
    log.trace "SUBSCRIBE $path"
	
    sendHubCommand(action)
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress(d) {
	def parts = d.split(":")
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}

// gets the address of the hub
private getCallBackAddress(childDevice) {
    return childDevice.device.hub.getDataValue("localIP") + ":" + childDevice.device.hub.getDataValue("localSrvPortTCP")
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