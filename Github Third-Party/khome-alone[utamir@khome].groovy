/**
 *  Add Khome Thing
 *
 *  Copyright 2017 ipsumdomus
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
    name: "Khome Alone",
    namespace: "ipsumdomus-com",
    author: "ipsumdomus",
    description: "Khome Smart Things discovery and initialization",
    category: "",
    iconUrl: "https://cdn-images-1.medium.com/fit/c/50/50/1*Wo6B-sVfY7EP3xuv4dI8iw.png",
    iconX2Url: "https://cdn-images-1.medium.com/fit/c/100/100/1*Wo6B-sVfY7EP3xuv4dI8iw.png",
    iconX3Url: "https://cdn-images-1.medium.com/fit/c/50/50/1*Wo6B-sVfY7EP3xuv4dI8iw.png")


preferences {
	page(name: "deviceDiscovery", title: "Setup", content: "deviceDiscovery")
}

def deviceDiscovery() {

	def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.ssdpUSN
		options["${key}"] = value
	}

	ssdpSubscribe()

	ssdpDiscover()
	verifyDevices()
	
    ssdpSubscribe()
    ssdpDiscover()
    
    
	return dynamicPage(name: "deviceDiscovery", title: "Khome Things", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Discovering Khome Things on your network. Please hold on.") {
			input "selectedDevices", "enum", required: true, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

void ssdpDiscover() {
	log.debug "Starting SSDP discovery."
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery upnp:rootdevice", physicalgraph.device.Protocol.LAN))
    
}

void ssdpSubscribe() {
	subscribe(location, "ssdpTerm.upnp:rootdevice", ssdpHandler)
}

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
            def dni = parsedEvent.ssdpUSN.split(':')[1]
			def child = getChildDevice(dni)            
			if (child) {
				child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
			} else {
            	def monitor = getChildDevice(parsedEvent.mac)
                if(monitor){
                	monitor.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
                }
            }
		}
	} else {
		devices << ["${ssdpUSN}": parsedEvent]
	}
}

Map verifiedDevices() {
	def devices = getVerifiedDevices()
	def map = [:]
	devices.each {
		def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
		def key = it.value.ssdpUSN
		map["${key}"] = value
	}
	map
}

def getVerifiedDevices() {
	getDevices().findAll{ it.value.verified == true }
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

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
	state.devices
}

def getRequests() {
	if (!state.requests) {
		state.requests = [:]
	}
	state.requests
}

def getSubscriptions() {
	if (!state.subscriptions) {
		state.subscriptions = [:]
	}
	state.subscriptions
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml    
	def devices = getDevices()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) {
    	def dndi = body?.device?.UDN?.text().split(':')[1];
    	def devType = body?.device?.deviceType?.text().split(':')[3];
    	def valid = (body?.device?.deviceType?.text().split(':')[1] == 'ipsumdomus-com')
        //discover services of valid devices only
        log.debug "Device '${body.device.friendlyName}' is valid: ${valid} [${devType}]"    	
        
        device.value << [
        name: body.device.friendlyName.text(), 
        model:body.device.modelName.text(), 
        serialNumber:body.device.serialNumber.text(), 
        manufacturer:body.device.manufacturer.text(), 
        deviceType:devType, 
        id:dndi, 
        services: [],
        verified: valid];
            
        if(valid) {
        	log.debug "Discovering services of '${body.device.friendlyName}'"
            //def reqs = []            
            body.device.serviceList.each { s ->            	
                /* TODO: Fix proper services discovery
                def req = new physicalgraph.device.HubAction(
                    method: "GET",
                    path: s.service.SCPDURL,
                    headers: [
                        HOST: getDeviceAddress(device),
                        "Content-Type": 'text/xml; charset="utf-8"'
                    ],
                    dndi,
                    [callback: serviceDescriptionHandler]);
                reqs << req;
                sers << [reqid: req.requestId]  
                */
               device.value.services << [
                	id: s.service.serviceId.text().split(':')[3],
                    control: s.service.controlURL.text(),
                    subscription: s.service.eventSubURL.text()]
            }
            //if(reqs.size() > 0) sendHubCommand(reqs)
            //else valid = false
        }
	}
}

void serviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse){
	//hubResponse.properties.each { log.debug "$it.key -> $it.value" }
    def body = hubResponse.xml
    //def header = hubResponse.header
    //log.debug "REQ: ${hubResponse.requestId}"
    def devices = getDevices()
    //log.debug "$devices"
    def device = devices.find { it.value.services.find {it.reqid == hubResponse.requestId} }
    if(device){
    	log.debug "BEFORE: $device"
    	def actions = [] 
    	body.actionList.each { a ->
        	def args = []
        	a.action.argumentList.each { ar -> 
            	args.add(name: ar.argument.name, statevar: ar.argument.relatedStateVariable, dir: ar.argument.direction)
            }
        	actions.add(name: a.action.name, arguments: args)
        }
        def states = []
        body.serviceStateTable.each { sv -> 
        	states.add(name: sv.stateVariable.name, type: sv.stateVariable.dataType, default: sv.stateVariable.defaultValue)
        }
        
    	def tgt = device.value.services.collect { 
        	if(it.reqid == hubResponse.requestId) {
            	it = [actions: actions, states: states]
            }
        }
        device.value.services = tgt;
        
        device.value.verified = !device.value.services.any{it.reqid}
        
        log.debug "AFTER: $device"
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
    runEvery5Minutes("checkSubsStatus")
    
}

def addDevices() {
	def devices = getDevices()

	selectedDevices.each { dni ->
		def selectedDevice = devices.find { it.value.ssdpUSN == dni }
		def d
        def ndni = selectedDevice.value.ssdpUSN.split(':')[1];
        log.debug "adding device for ${ndni}"
		if (selectedDevice) {
			d = getChildDevices()?.find {
				it.deviceNetworkId == ndni
			}
		}

		if (!d) {
			//log.debug "Creating Generic UPnP Device with dni: ${selectedDevice.value.ssdpUSN}"
            log.debug "Serial number ${selectedDevice.value.serialNumber} Device type: ${selectedDevice.value.deviceType}"
			
            def deviceHandler = null
            if(selectedDevice.value.deviceType == 'SonoffSwitch' || selectedDevice.value.deviceType == 'BroadlinkSmartPlug') {
            	log.debug "Device type OK"
                deviceHandler = 'BinarySwitch'
            } else {
            	log.debug "Device type ${selectedDevice.value.deviceType}"
            }
            def nd = addChildDevice("ipsumdomus-com", deviceHandler, ndni, selectedDevice?.value.hub, [
				"label": selectedDevice?.value?.name ?: "Generic UPnP Device",
				"data": [
					"mac": selectedDevice.value.mac,
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress,
                    "sn": selectedDevice.value.serialNumber,
                    "id": ndni
				]
			])
            nd.refresh()
            nd.subscribe();
            //check if monitor exists and add if needed
            def monitor = getChildDevices()?.find {
				it.deviceNetworkId == selectedDevice.value.mac
			}
            if(!monitor){
            	log.debug('Adding monitor')
                addChildDevice("ipsumdomus-com", "Khome hub", selectedDevice.value.mac, selectedDevice?.value.hub, [
				"label": "Khome Hub",
				"data": [
					"ip": selectedDevice.value.networkAddress,
					"port": selectedDevice.value.deviceAddress
				]
			])
            }
		} else {
        	log.debug "Child device is already exists."
        }
	}
}

def execChildGet(id, args = ''){
	log.debug "Poll children ${id} -> ${app.id} "
    def dni = getVerifiedDevices().find { it.value.id == id }
    def polls = getRequests()
    def reqs = dni.value.services.collect { 
    	def req = new physicalgraph.device.HubAction(
        method: "GET",
        path: it.control + args,
        requestContentType: "application/json",
        headers: [
            HOST: getDeviceAddress(dni)
        ],
        it.id,
        [callback: execChildHandler]);
        polls << ["${req.requestId}": id]
        it = req
    }
    sendHubCommand(reqs)
}

def routeChildNotification(msg){
	def headerString = msg.header
	
    def sid = parseSid(headerString)
    if (headerString?.contains("NOTIFY /notify")) {
    	def did = (headerString =~ /NOTIFY\s\/notify\/?(.*)\s/) ? ( headerString =~ /NOTIFY\s\/notify\/?(.*)\s/)[0][1] : "0"
        did -= " HTTP/1.1".trim();
        log.debug "Target ID '${did}'"
        
        if(did?.trim()){
        	def dni = getVerifiedDevices().find { it.value.id == did.trim() }
            //log.debug "DNI ${dni}"
            def child = getChildDevice(dni.value.id)
            if (child) {
            	//def xml = parseXml(msg.body)
                //log.debug "RAW: ${msg.body}"
            	//log.debug "XML: ${xml}"
            	child.notify(msg)
                
            }
        }
    }
}

void checkSubsStatus(){
	log.debug "Starting check subscriptions"
	def subs = getSubscriptions()
    subs.each {sub ->
    	execChildSubscibe(sub.key)
    }
}

def execChildUnsubscibe(id){
	log.debug "Unubscribe children ${id} -> ${app.id} "
    def dni = getVerifiedDevices().find { it.value.id == id }
    def polls = getRequests()
    def address = location.hubs[0].localIP + ":" + location.hubs[0].localSrvPortTCP
    log.debug "MAC: ${dni.value.mac}"
    def reqs = dni.value.services.collect { 
        def req = new physicalgraph.device.HubAction(
        method: "UNSUBSCRIBE",
        path: it.subscription,
        headers: [
            HOST: getDeviceAddress(dni),
            SID: "uuid:${sub.value[0]}" //TODO : Support more then one resubscribe
        ],
        dni.value.mac,
        [callback: execChildHandler]);
        polls << ["${req.requestId}": id]
        it = req
    }
    sendHubCommand(reqs)
}

def execChildSubscibe(id){
	log.debug "Subscribe children ${id} -> ${app.id} "
    def dni = getVerifiedDevices().find { it.value.id == id }
    def polls = getRequests()
    def address = location.hubs[0].localIP + ":" + location.hubs[0].localSrvPortTCP
    log.debug "MAC: ${dni.value.mac}"
    def subs = getSubscriptions()
    def sub = subs.find {it.key == id}
    def headers = null
    if(sub) {
    	log.debug "Subscription for ${id} already exists. Resubscribe"
        headers = [
            HOST: getDeviceAddress(dni),
            SID: "uuid:${sub.value[0]}", //TODO : Support more then one resubscribe
            TIMEOUT: "Second-28800"
        ]
    } else {
    	log.debug "Creating new subscription for ${id}"
        headers = [
            HOST: getDeviceAddress(dni),
            CALLBACK: "<http://${address}/notify/${id}>",
            NT: "upnp:event",
            TIMEOUT: "Second-28800"
        ]
    }
    def reqs = dni.value.services.collect { 
        def req = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        path: it.subscription,
        headers: headers,
        dni.value.mac,
        [callback: execChildHandler]);
        polls << ["${req.requestId}": id]
        it = req
    }
    sendHubCommand(reqs)
}

void execChildHandler(physicalgraph.device.HubResponse hubResponse){
	//def body = hubResponse.json
    def polls = getRequests();
    def dni = polls.find {it.key == hubResponse.requestId}
    if(dni) {
    //manage subscriptions
    def sid = parseSid(hubResponse.header)
    if(sid){
    //log.debug "HEADER: ${hubResponse.header}"
    	def subs = getSubscriptions()
        def sub = subs.find {it.key == dni.value}
        
    	//handle lost subscriptions
    	if(hubResponse.header?.contains("HTTP/1.1 555")){
        	log.debug "Remote hub lost subscription. Create new"
            subs.remove(dni.value)
            execChildSubscibe(dni.value)
        } else {    	
            if(!sub){
                subs << ["${dni.value}": [sid]]
            } else {
                subs["${dni.value}"] << sid
            }
        }
        log.debug "subs: ${subs}"
       } 
    
        def child = getChildDevice(dni.value)
        if (child) {
            child.notify(hubResponse)
            polls.remove(hubResponse.requestId)
        } else {
            log.debug "Device ${dni} not found"
        }
    }
 	
}

private parseSid(headerString){
	def sid = null
	if (headerString?.contains("SID: uuid:")) {
        sid = (headerString =~ /SID: uuid:.*/) ? ( headerString =~ /SID: uuid:.*/)[0] : "0"
        sid -= "SID: uuid:".trim()        
 	}
    sid
}

private getDeviceAddress(dni) {
    def ip = dni.value.networkAddress
    def port = dni.value.deviceAddress

    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}