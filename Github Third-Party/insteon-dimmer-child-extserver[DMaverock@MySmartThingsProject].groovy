/**
*  Insteon Dimmer Child with External Server
*
*  Copyright 2016 DMaverock
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
    name: "Insteon Dimmer Child ExtServer",
    namespace: "DMaverock",
    author: "DMaverock",
    description: "Child Insteon Dimmer SmartApp with External Server",
    category: "My Apps",
    parent: "DMaverock:Insteon (Connect)",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png")


preferences {
    page(name: "mainPage", title: "Install Insteon Dimmer Device", install: true, uninstall:true) {
        section("Insteon Dimmer Name") {
            label(name: "label", title: "Name This Insteon Dimmer Device", required: true, multiple: false, submitOnChange: true)
        }          
        section("Hub Settings") {
        	input("hubName", "hub", title:"Hub", description: "Please select your Hub", required: true, multiple: false, displayDuringSetup: true)
        }
        section("Add an Insteon Dimmer Device") {
			input("InsteonID","string", title: "Insteon Device ID", description: "Please enter your Insteon Device's ID", required:true, submitOnChange: true, displayDuringSetup: true)           
        	input("InsteonIP","string", title: "Insteon Hub IP", description: "Please enter your Insteon Hub's IP Address", required:true, submitOnChange: true, displayDuringSetup: true)           
            input("ExternalIP","string", title: "Insteon Hub External IP", description: "Please enter your Insteon Hub's External IP Address", required:true, submitOnChange: true, displayDuringSetup: true)                                      
            input("ExternalStatusServerIP","string", title: "Polling Server IP", description: "Please enter the IP Address of your Polling Server", required:true, submitOnChange: true, displayDuringSetup: true)                                      
        	input("port","string", title: "Insteon Hub Port", description: "Please enter your Insteon Hub's Port Number", required:true, submitOnChange: true, defaultValue: "25105", displayDuringSetup: true)           
        	input("InsteonHubUsername","string", title: "Insteon Hub Username", description: "Please enter your Insteon Hub's Username", required:true, submitOnChange: true, displayDuringSetup: true)           
        	input("InsteonHubPassword","password", title: "Insteon Hub Password", description: "Please enter your Insteon Hub's Password", required:true, submitOnChange: true, displayDuringSetup: true)                                      
        }        
    }
    
}

def installed() {
    log.debug "Installed"	
    initialize()    
    log.debug "Values are: $InsteonID -- $InsteonIP -- $port -- $InsteonHubUsername -- $InsteonHubPassword -- $hubName -- "
}

def updated() {
    log.debug "Updated"
    unsubscribe()
    initialize()    
    log.debug "Values are: $InsteonID -- $InsteonIP -- $port -- $InsteonHubUsername -- $InsteonHubPassword -- $hubName -- "
}

def initialize() {    
    log.debug "initializing"  
    
    state.InsteonID = InsteonID
    state.InsteonIP = InsteonIP
    state.port = port
    state.InsteonHubUsername = InsteonHubUsername
    state.InsteonHubPassword = InsteonHubPassword    
    //state.hubName = hubName    
    log.debug "hub ID is " + hubName.id
 
    try {
        def DNIRaw = (Math.abs(new Random().nextInt()) % 99999 + 1)//.toString()
        //def DNI = (InsteonID + hex(DNIRaw)).toString()        
        //DNI = InsteonID
        def DNI = hex(DNIRaw).toString()
        def insteon = getChildDevices()        
        log.debug "Current DNI is $DNI"        
        if (insteon) {
        	log.debug "found ${insteon.displayName} already exists"
            log.debug "DNI is ${insteon.deviceNetworkId}"
            insteon[0].name = app.label
            insteon[0].label = app.label
            insteon[0].displayName = app.label
            insteon[0].configure()
        }
        else {
        	def childDevice = addChildDevice("DMaverock", "Insteon Dimming Device SA (LOCAL)", DNI, hubName.id, [name: app.label, label: app.label, completedSetup: true])
            log.debug "created ${app.label} with id $DNI"
        }
    } catch (e) {
    	log.error "Error creating device: ${e}"
    }
    log.debug "end initialize"

/*  
    def devices = label.collect { dni ->
		def d = getChildDevice(dni)
		if(!d) {
        	d = addChildDevice("DMaverock", "Insteon Dimming Device (LOCAL)", dni, null, [name: app.label, label: app.label, completedSetup: true])
			//d = addChildDevice(app.namespace, getChildName(), dni, null, ["label":"${atomicState.thermostats[dni]}" ?: "Ecobee Thermostat"])
			log.debug "created ${d.displayName} with id $dni"
		} else {
			log.debug "found ${d.displayName} with id $dni already exists"
            log.debug "deleting ${d.deviceNetworkId}"
            removeChildDevices(d)
		}

		return d
	}
*/
/*
    def delete  // Delete any that are no longer in settings
	if(!label) {
		log.debug "delete devices"
		delete = getAllChildDevices() //inherits from SmartApp (data-management)
	} else { //delete only thermostat
		log.debug "delete individual device"
		delete = getChildDevices().findAll { !label.contains(it.deviceNetworkId) }
	}
    delete.each { deleteChildDevice(it.deviceNetworkId) }
*/    
}

def getHubID(){
    def hubID
    if (myHub){
        hubID = myHub.id
    } else {
    	hubID = null
        /*
        def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL } 
        log.debug "hub count: ${hubs.size()}"
        if (hubs.size() == 1) hubID = hubs[0].id 
        */
    }
    //log.debug "hubID: ${hubID}"
    return hubID
}

def uninstalled() {
	log.debug "in uninstalled"
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	log.debug "removeChildDevices"
/*
	def del = getChildDevices().findAll { !settings.devices.contains(it.deviceNetworkId) }

        del.each {
        	//log.debug "del id is " del.deviceNetworkId
            //deleteChildDevice(it.deviceNetworkId)
            deleteChildDevice(del.deviceNetworkId)
        }
*/
    delete.each {
        deleteChildDevice(it.deviceNetworkId)        
        //deleteChildDevice(device.deviceNetworkId)        
    }
/*    
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
*/
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}	

def doRefresh() {

	//state.InsteonID = InsteonID
    //state.InsteonIP = InsteonIP
    //state.port = port
    //state.InsteonHubUsername = InsteonHubUsername
    //state.InsteonHubPassword = InsteonHubPassword
    //state.hubName = hubName
	
    log.debug "Start Refreshing"
    
    def host = InsteonIP
    //def port = it.port
    //log.debug "host is $host, port is $port"
    def hosthex = convertIPToHex(host)
    def porthex = Long.toHexString(Long.parseLong((port)))
    if (porthex.length() < 4) { porthex = "00" + porthex }

    //def currDNI = it.deviceNetworkId
    //log.debug "currDNI is $currDNI"

    //log.debug "Port in Hex is $porthex"
    //log.debug "Hosthex is : $hosthex"    
    //def deviceNetworkId = "$hosthex:$porthex" 
    //def instID = "$InsteonID"
    //instID = instID.toLowerCase()
    //log.debug "ID is $instID"
    //def deviceNetworkId = "$instID:$porthex" 
    //log.debug "newDNI is $deviceNetworkId"
    //device.deviceNetworkId = "$deviceNetworkId"
    //device.deviceNetworkId = "22653A"
    
    sendRefresh()    
    //def result = buffStatus()        
    return buffStatus()
    
    //device.deviceNetworkId = currDNI
    //log.debug "After DNI is " + device.deviceNetworkId    
	      
}

def sendRefresh() {

	log.debug "Refreshing Status"    
    def host = InsteonIP
    //def port = parent.port
    
    def path = "/3?0262" + InsteonID  + "0F1900=I=3"
    log.debug "RefreshPath is: $path"

    def userpassascii = parent.InsteonHubUsername + ":" + parent.InsteonHubPassword
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def headers = [:] //"HOST:" 
    headers.put("HOST", "$host:$port")
    headers.put("Authorization", userpass)

    def method = "GET"
    
    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: method,
    	path: path,
    	headers: headers
        )
        return hubAction
    }
    catch (Exception e) {
    log.debug "Hit Exception on $hubAction"
    log.debug e
    }
}

def buffStatus() {
    
    def host = InsteonIP
    //def port = parent.port
    def path = "/buffstatus.xml"
    log.debug "buffPath is $path"    
    
    //def instID = parent.InsteonID
    //instID = instID.toLowerCase()
    //log.debug "instID is $instID"
    
    def hosthex = convertIPToHex(host)
    def porthex = Long.toHexString(Long.parseLong((port)))
    if (porthex.length() < 4) { porthex = "00" + porthex }
    
    def userpassascii = parent.InsteonHubUsername + ":" + parent.InsteonHubPassword
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def headers = [:] //"HOST:" 
    headers.put("HOST", "$host:$port")
    headers.put("Authorization", userpass)

    def method = "GET"
    
    try {
    def hubAction = new physicalgraph.device.HubAction(
    	method: method,
    	path: path,
    	headers: headers
        )
        return hubAction
    }
    catch (Exception e) {
    log.debug "Hit Exception on $hubAction"
    log.debug e
    }    
}

def parse(description) {
   
    def msg = parseLanMessage(description)

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
    
    log.debug "Dimmer Child Parsing"
    
    log.debug "status: $status -- body: $body -- json: $json -- xml: $xml -- data: $data"
    log.debug "DNI is " + device.deviceNetworkId
    
    //log.debug "left xml is " + left(xml,23)

}

// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

// gets the address of the device
private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPToHex(ipAddress) {
	return Long.toHexString(converIntToLong(ipAddress));
}

private Long converIntToLong(ipAddress) {
	long result = 0
	def parts = ipAddress.split("\\.")
    for (int i = 3; i >= 0; i--) {
        result |= (Long.parseLong(parts[3 - i]) << (i * 8));
    }

    return result & 0xFFFFFFFF;
}