/**
 *  ISY Connect
 *
 *  Copyright 2021 David Hegland
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
    name: "ISY 994i (Connect)",
    namespace: "raven42",
    author: "David Hegland",
    description: "Link ISY hub",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Preferences Section
preferences {
	page(name: "mainPage")
    page(name: "discoveryPage")
    page(name: "modeMapPage")
    page(name: "clearNodesPage")
    page(name: "clearNodesOpPage")
    page(name: "queryNodesPage")
    page(name: "clearVariablesPage")
    page(name: "clearVariablesOpPage")
    page(name: "queryVariablesPage")
}

// This block defines an endpoint, and which functions will fire depending on which type
// of HTTP request you send
mappings {
    // The path is appended to the endpoint to make requests
    path("/node") {
        // These actions link HTTP verbs to specific callback functions in your SmartApp
        action: [
            GET: "nodeStatusGet",
            PUT: "nodeStatusPut",
            POST: "nodeStatusPost",
        ]
    }
}

// Callback functions
def nodeStatusGet() {
    log.debug "nodeStatusGet() request:${request}"
}
def nodeStatusPut() {
    log.debug "nodeStatusPut() request:${request}"
}
def nodeStatusPost() {
    log.debug "nodeStatusPost() request:${request}"
}

def initPreferences() {
    if (!state.initialized) {
        log.debug "init()"
    	state.initialized = true
        settings.debug = true
    	subscribe(location, "ssdpTerm.urn:udi-com:device:X_Insteon_Lighting_Device:1", ssdpHandler)
        // subscribe(location, null, responseHandler, [filterEvents:false])
    }
}

def mainPage() {
	if (settings.debug) { log.debug "mainPage()" }
    def refreshInterval = 10
    def refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    
    initPreferences()
    
    if (!selectedDevice) {
   		discover()
        
        return dynamicPage(name:"mainPage", title:"ISY Connect", nextPage:"", refreshInterval:refreshInterval, install:false, uninstall:true) {
            section ("ISY Hub Selection") {
                href(name: "deviceDiscovery", title: "Search for ISY", description: "Perform discovery of ISY using UPnP", page: "discoveryPage")
            }
        }
    } else {
        state.refreshCount = refreshCount + 1
        
      	if ((refreshCount % 5) == 0) {
            queryNodes()
        }
        if (refreshCount == 0) {
        	//queryStatus()
            //queryDefinitions()
        }

        def nodes = getNodes()
        def nodeNames = nodes.findAll { it.value.name }.collect{ key, value -> value.name}
        nodeNames.sort()
        if (settings.debug) {
        	log.debug "found ${nodeNames.size()} nodes"
        }
        
        def scenes = getScenes()
        def sceneNames = scenes.findAll { it.value.name }.collect{ key, value -> value.name}
        sceneNames.sort()
        if (settings.debug) {
        	log.debug "found ${sceneNames.size()} nodes"
        }
        
        return dynamicPage(name:"mainPage", title:"ISY Connect", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall:true) {
        	section("ISY Global Settings") {
                input "debug", "bool", required: true, title: "Enable debug", submitOnChange:true
                if (settings.debug) {
            		input "updateInterval", "number", required: true, title: "Update Interval\nSeconds between polling intervals", defaultValue:30
                }
            }
            section("Select nodes...") {
                input "selectedNodes", "enum", required:false, title:"Select Nodes \n(${nodeNames.size() ?: 0} found)", multiple:true, options:nodeNames
            }
			section("Select scenes...") {
            	input "selectedScenes", "enum", required:false, title:"Select Scenes \n(${sceneNames.size() ?: 0} found)", multiple:true, options:sceneNames
			}
			section("SmartThings Mode Handling") {
            	href "modeMapPage", title:"SmartThings Mode Mapping", required:false, page:"modeMapPage"
			}
            if (settings.debug) {
                section("ISY Node Settings") {
                	input "queryNodesOnRefresh", "bool", title:"Include NODE in polling interval", defaultValue:true, submitOnChange:true 
                	href "clearNodesPage", title:"Clear NODE data", required:false, page:"clearNodesPage"
                    href "queryNodesPage", title:"Initiate NODE query", required:false, page:"queryNodesPage"
                    input "nodeQueryMethod", "enum", required:false, title:"Node refresh method", multiple:false, options:["/rest/status", "/rest/nodes"]
                }
                section("ISY Variable Settings") {
                   	input "queryVariablesOnRefresh", "bool", title:"Include VARIABLES in polling interval", defaultValue:true, submitOnChange:true
                   	href "clearVariablesPage", title:"Clear VARIABLE data", required:false, page:"clearVariablesPage"
                   	href "queryVariablesPage", title:"Initiate VARIABLE query", required:false, page:"queryVariablesPage"
                }
            }
        }
    }
}

def discoveryPage() {
	log.debug "discoveryPage()"
    
	int hubRefresh = !state.hubRefresh ? 0 : state.hubRefresh as int
	def refreshInterval = 3
	state.hubRefresh = hubRefresh + refreshInterval
    
    if ((hubRefresh % 15) == 0) {
 		discover()
    }
 	
    def devices = getDevices()
 	def deviceNames = devices.collect { it.value.ip }
    deviceNames.sort()
    if (settings.debug) {
    	log.debug "found ${deviceNames.size()} devices"
    }
     
 	return dynamicPage(name:"discoveryPage", title:"Discovery Started", nextPage:"", refreshInterval:refreshInterval, install:false, uninstall:false) {
 		section("Please wait while we discover your device. Select your device below once discovered.") {
 			input "selectedDevice", "enum", required:true, title:"Select Devices \n(${deviceNames.size() ?: 0} found)", multiple:false, options:deviceNames
 		}
        section("ISY Credentials") {
        	input "username", "text", title:"ISY Username", required:true
            input "password", "password", title:"ISY Password", required:true
        }
 	}
}

def modeMapPage() {
	if (settings.debug) { log.debug "modeMapPage()" }
    def refreshInterval = 5
    def refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    state.refreshCount = refreshCount + 1
        
    if ((refreshCount % 5) == 0) {
        queryVariables()
    }
    
	def variables = getVariables()
	def variableNames = variables.collect { "${it.value.name} (${it.value.type=='1'?'Var':'State'}:${it.value.id} Value:${it.value.value})" }
    variableNames.sort()
    if (settings.debug) {
    	log.debug "found ${variableNames.size()} variables"
    }
    
    return dynamicPage(name:"modeMapPage", title:"Mode Map Handling", nextPage:"", refreshInterval:refreshInterval, install:false, uninstall:false) {
    	section("SmartThings Mode Handling") {
        	paragraph "These mappings will set the specified ISY variable/state when the given SmartThings mode is changed."
        }
        section("ISY Mode Change Variable") {
        	paragraph "First select the ISY variable you want modified when the mode changes."
        	input "modeMapVariable", "enum", required:false, title:"Mode Map Variable", multiple:false, options:variableNames
        }
        section("Mode Values") {
        	paragraph "Now select the ISY variable you want to set the \"ISY Mode Change Variable\" to for each available SmartThings mode."
 			location.modes.each { mode ->
				input "modeMap${mode}", "enum", required:false, title:"[${mode}] Mode Mapping", multiple:false, options:variableNames
			}
		}
    }
}

def clearNodesPage() {
	return dynamicPage(name:"clearNodesPage", title:"Clear NODE data...", nextPage:"", install:false, uninstall:false) {
        section() {
            paragraph "This will clear all node data and flush all nodes from the app. This might potentially break any connection to any child devices. Use with caution."
        }
        section("Are you sure?") {
            href "clearNodesOpPage", title: "Proceed...", page:"clearNodesOpPage"
            href "main", title:"Cancel Operation", page:"main"
        }
    }
}

def clearNodesOpPage() {
	state.nodes = [:]
    settings.remove("selectedNodes")
	
    log.debug "settings:${settings}"
    
	return mainPage()
}

def queryNodesPage() {
	queryStatus()
    
    return mainPage()
}

def clearVariablesPage() {
	return dynamicPage(name:"clearVariablesPage", title:"Clear VARIABLE data...", nextPage:"", install:false, uninstall:false) {
        section() {
            paragraph "This will clear all variable data and flush all variables from the app. Use with caution."
        }
        section("Are you sure?") {
            href "clearVariablesOpPage", title: "Proceed...", page:"clearVariablesOpPage"
            href "main", title:"Cancel Operation", page:"main"
        }
    }
}

def clearVariablesOpPage() {
	state.variables = [:]
    settings.remove("modeMapVariable")
	location.modes.each { mode ->
		settings.remove("modeMap${mode}")
	}
    log.debug "settings:${settings}"
    
    return mainPage()
}

def queryVariablesPage() {
    queryDefinitions()
    
    return mainPage()
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Network Layer Handler and Parser Routines

def ssdpHandler(evt) {
    log.debug('ssdpHandler() event received:' + evt.description)

    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    // Force port 80 (0x50)
    parsedEvent.port = '0050'
    parsedEvent << ["ip" : convertHexToIP(parsedEvent.networkAddress)]

    if (parsedEvent?.ssdpTerm?.contains("udi-com:device:X_Insteon_Lighting_Device:1")) {
        def devices = getDevices()

        if (!(devices."${parsedEvent.ssdpUSN.toString()}")) { //if it doesn't already exist
            log.debug('Parsed Event: ' + parsedEvent)
            devices << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
        } else { // just update the values
            def d = devices."${parsedEvent.ssdpUSN.toString()}"
            boolean deviceChangedValues = false

            if (d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
                d.ip = parsedEvent.ip
                d.port = parsedEvent.port
                deviceChangedValues = true
            }

            if (deviceChangedValues) {
                def children = getAllChildDevices()
                children.each {
                    if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
                        //it.subscribe(parsedEvent.ip, parsedEvent.port)
                    }
                }
            }

        }
    }
}

def parse(evt) {
	log.debug "parse() evt:[${evt}]"
}

def statusUpdate(evt) {
	if (settings.debug) {
    	log.debug "statusUpdate() evt:[${evt}]"
    }
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(evt.description)
    if (parsedEvent) {
        parsedEvent << ["hub":hub]
        if (parsedEvent?.headers?."content-type".equals("application/xml")) {
            //log.debug "statusUpdate() parsedEvent:${parsedEvent}"

            def xml = new XmlParser().parseText(parsedEvent?.body)
            xml.nodes.node.each { xmlNode ->
                def child = getChildDevices()?.find { (it.getDataValue("nodeAddress").equals(xmlNode?.id?.text())) }
                if (child) {
                    log.debug "statusUpdate() id:${xmlNode.id.text()} status:${xmlNode.status.text()} sending statusUpdate() to ${child}"
                    child.statusUpdate(xmlNode)
                }
            }
        } else {
        	log.debug "statusUpdate() ignoring event:${parsedEvent}"
        }
    }
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////
// ISY Hub (device) Routines

def discover() {
	log.debug('Performing discovery')
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:udi-com:device:X_Insteon_Lighting_Device:1", physicalgraph.device.Protocol.LAN))
}

def getIsyHub() {
    def selDev
    def devices = getDevices()
    selDev = devices.find { it.value.ip == selectedDevice }
    selDev
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////
// ISY Node Query Routines

def parseQueryNodes(resp) {
	def nodes = getNodes()
    def scenes = getScenes()
    
    if (settings.debug) {
    	log.debug "parseQueryNodes() ${resp.description}"
        //log.trace "parseQueryNodes() body:${resp.body}"
    }
    
    def xml = new XmlParser().parseText(resp.body)
    //log.debug "parseQueryNodes() xml:[${xml}]"
    def printed = 0
    def printInterval = 150
    log.debug "parseQueryNodes() xmlNodes:[${xmlNodes}]"
    xml?.node?.each { xmlNode ->
        if (settings.debug && (printed % printInterval) == 0) {
    		log.debug "Looking for node:${xmlNode.address.text()} xmlNode:${xmlNode}"
        }
    	if (!(nodes[xmlNode.address.text()])) {
        	def node = [:]
        	node.address = xmlNode.address.text()
        	node.name = xmlNode.name.text()
            node.type = xmlNode.type.text()
            node.deviceNetworkId = "ISY:${node.address.replaceAll(" ", ".")}"
            node.status = 0
    		xmlNode?.property.each { prop ->
            	if (settings.debug) { log.debug "node:${node.name} property:${prop}" }
            	if (prop.@id.equals('ST') && prop.@value) {
            		node.status = isy2stLevel(prop.@value)
                }
            }
        
            state.nodes[node.address] = node
            
            if (settings.debug && (printed % printInterval) == 0) {
                log.debug "adding node:[${node}]"
            }
        } else {
        	// Update existing info with data from ISY
            def node = nodes[xmlNode.address.text()]
            node.name = xmlNode.name.text()
        }
        printed += 1
    }
    
    printed = 0
    xml?.group?.each { xmlScene ->
        if (settings.debug && (printed % printInterval) == 0) {
    		log.debug "Looking for scene:${xmlScene.address.text()} xmlScene:${xmlScene}"
        }
    	if (!(scenes[xmlScene.address.text()])) {
        	def scene = [:]
        	scene.address = xmlScene.address.text()
        	scene.name = xmlScene.name.text()
            scene.type = xmlScene.type.text()
            scene.deviceNetworkId = "ISY:SCN:${scene.address.replaceAll(" ", ".")}"
            scene.status = 0
    		xmlScene?.property.each { prop ->
            	if (settings.debug) { log.debug "scene:${scene.name} property:${prop}" }
            	if (prop.@id.equals('ST') && prop.@value) {
            		scene.status = isy2stLevel(prop.@value)
                }
            }
        
            state.scenes[scene.address] = scene
            
            if (settings.debug && (printed % printInterval) == 0) {
                log.debug "adding scene:[${scene}]"
            }
        } else {
        	// Update existing info with data from ISY
            def scene = scenes[xmlScene.address.text()]
            scene.name = xmlScene.name.text()
        }
    	printed += 1
    }
    log.debug "parseQueryNodes() complete"
}

def queryNodes() {
    def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
    def auth = getAuthorization()
        
    if (settings.debug) { log.debug "query nodes from ${host}" }
    
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/nodes',
            'protocol' : physicalgraph.device.Protocol.LAN,
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseQueryNodes]))
}

def parseStatus(msg) {
	def nodes = getNodes()
    
    // log.debug "parseStatus() desc: ${msg.description}"
	// log.debug "parseStatus() body: ${msg.body}"
    
    def xml = new XmlParser().parseText(msg.body)
    def xmlNodes = xml.nodes
    xml.each { xmlNode ->
    	def node = nodes.find { it.value.address == xmlNode.@id }
        if (node) {
            xmlNode?.property.each { prop ->
                if (prop.@id.equals("OL") && prop.@value) {
                    node.value.onLevel = isy2stLevel(prop.@value)
                    log.debug "dm.parseStatus() onLevel:${prop.@value} for node:${node}"
                } else if (prop.@id.equals("RR") && prop.@value) {
                    node.value.rampRate = prop.@value
                    log.debug "dm.parseStatus() rampRate:${prop.@value} for node:${node}"
                } else if (prop.@id.equals("ST") && prop.@value) {
                    node.value.status = isy2stLevel(prop.@value)
                    log.debug "dm.parseStatus() status:${prop.@value} for node:${node}"
                }
            }
        }
    }
}

def queryStatus() {
    def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
    def auth = getAuthorization()
    
    if (settings.debug) {
    	log.debug "queryStatus() from ${host}"
    }
    
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/status',
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseStatus]))
}

def queryStatusLoop() {
	if (settings?.queryNodesOnRefresh) {
        if (settings?.nodeQueryMethod?.equals("/rest/nodes")) {
            queryNodes()
        } else {
            queryStatus()
        }
    }
    if (settings.updateInterval < 10) {
    	settings.updateInterval = 10
    }
    //runIn(settings.updateInterval, queryStatusLoop)
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Variable and StateVar Routines

def parseQueryVariables(resp) {
	def variables = getVariables()
    
    //log.debug "parseQueryVariables() ${resp.description}"
    
    def xml = new XmlParser().parseText(resp.body)
    //log.debug "parseQueryVariables() xml:[${xml}]"
    //def xmlVariables = xml.vars
    def printed = 0
    //log.debug "xmlVariables:[${xmlVariables}]"
    xml.var.each { xmlVar ->
    	def uniqueId = "${xmlVar.@type}.${xmlVar.@id}"
    	if (!variables[uniqueId]) {
        	def variable = [:]
            variable.uniqueId = uniqueId
            variable.id = xmlVar.@id
            variable.type = "scene"
            variable.value = xmlVar.val.text()
    
            state.variables[uniqueId] = variable
        }
        def variable = variables[uniqueId]
    	variable.value = xmlVar.val.text()
    
        if (settings.debug) {
            if ((printed % 50) == 0) {
                log.debug "variable:${variable} xmlVar:[${xmlVar}]"
            }
            printed += 1
        }
    }
}

def queryVariables() {
    def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
    def auth = getAuthorization()
    
    if (settings.debug) {
    	log.debug "attempting to variables from ${host}"
    }
    
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/vars/get/1',
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseQueryVariables]))
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/vars/get/2',
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseQueryVariables]))
}

def parseQueryIntDefinitions(resp) {
	def variables = getVariables()
    
    //log.debug "parseQueryDefinitions() ${resp.description}"
    
    def xml = new XmlParser().parseText(resp.body)
    //log.debug "parseQueryDefinitions() xml:[${xml}]"
    //def xmlVariables = xml.vars
    def printed = 0
    //log.debug "xmlVariables:[${xmlVariables}]"
    xml.e.each { xmlVar ->
    	def uniqueId = "1.${xmlVar.@id}"
    	if (!variables[uniqueId]) {
        	def variable = [:]
            variable.uniqueId = uniqueId
            variable.id = xmlVar.@id
            variable.type = "1"
            variable.name = xmlVar.@name
    
            state.variables[uniqueId] = variable
        }
    	def variable = variables[uniqueId]
        variable.name = xmlVar.@name
        
    	if (settings.debug && (printed % 50) == 0) {
        	log.debug "parseQueryIntDefinitions() variable:${variable} xmlVar:[${xmlVar}]"
        }
        printed += 1
    }
}

def parseQueryStateDefinitions(resp) {
	def variables = getVariables()
    
    //log.debug "parseQueryDefinitions() ${resp.description}"
    
    def xml = new XmlParser().parseText(resp.body)
    //log.debug "parseQueryDefinitions() xml:[${xml}]"
    //def xmlVariables = xml.vars
    def printed = 0
    //log.debug "xmlVariables:[${xmlVariables}]"
    xml.e.each { xmlVar ->
    	def uniqueId = "2.${xmlVar.@id}"
    	if (!variables[uniqueId]) {
        	def variable = [:]
            variable.uniqueId = uniqueId
            variable.id = xmlVar.@id
            variable.type = "2"
            variable.name = xmlVar.@name
    
            state.variables[variable.uniqueId] = variable
        }
    	def variable = variables[uniqueId]
        variable.name = xmlVar.@name
        
    	if (settings.debug && (printed % 50) == 0) {
        	log.debug "parseQueryStateDefinitions() variable:${variable} xmlVar:[${xmlVar}]"
        }
        printed += 1
    }
}

def queryDefinitions() {
    def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
    def auth = getAuthorization()
    
    if (settings.debug) {
    	log.debug "attempting to get variable definitions from ${host}"
    }
    
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/vars/definitions/1',
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseQueryIntDefinitions]))
    sendHubCommand(new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': '/rest/vars/definitions/2',
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], null, [callback:parseQueryStateDefinitions]))
}

def queryVariablesLoop() {
    if (settings.queryVariablesOnRefresh) {
    	queryVariables()
    }
    if (settings.updateInterval < 10) {
    	settings.updateInterval = 10
    }
    //runIn(settings.updateInterval, queryVariablesLoop)
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Child Device Interfaces

// restGet() Create a new HubAction() as a GET request for a given device to the ISY hub to be
//			 used by the child device command routines.
//	device - The child device instance
//	path - The path to post the GET request to
//	args - Any additional options as supported by HubAction() such as a callback routine
def restGet(device, path, args=[:]) {
    def isy = getIsyHub()
    if (!isy) { 
    	log.warning "restGet() - unable to perform GET request. No ISY hub found"
        return
    }
    def host = getHost()
    def auth = getAuthorization()
    
    log.debug "restGet() ${host}${path}"
    
    new physicalgraph.device.HubAction(
        	'method': 'GET',
        	'path': path,
        	'headers': [
        	    'HOST': host,
        	    'Authorization': auth
        	], device.deviceNetworkId, args)
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Helper funtions
def getHost() {
	def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
}
def getAuthorization() {
    def userpassascii = settings.username + ":" + settings.password
    "Basic " + userpassascii.encodeAsBase64().toString()
}
private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
def getDevices() {
    if (!state.devices) { state.devices = [:] }
    state.devices
}
def getNodes() {
    if (!state.nodes) { state.nodes = [:] }
    state.nodes
}
def getScenes() {
    if (!state.scenes) { state.scenes = [:] }
    state.scenes
}
def getVariables() {
	if (!state.variables) { state.variables = [:] }
    state.variables
}
def getDebug() {
	settings.debug
}
def isy2stLevel(isyLevel) {
	def stLevel = isyLevel.toFloat() * 100.0 / 255.0
    stLevel as int
}
def st2isyLevel(stLevel) {
	def isyLevel = stLevel.toFloat() * 255.0 / 100.0
    isyLevel as int
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Mode Handling routines

def parseModeChange(resp) {
	if (settings.debug) { log.debug "parseModeChange() resp:[${resp}]" }
    def description = resp.description
    def msg = parseLanMessage(description)
    
    if (settings.debug) { log.trace "parsedResponse:[${msg}]" }
    if (msg.status == 200) {
    	log.debug "parseModeChange() success"
    } else {
    	log.warn "parseModeChange() failed status:${msg.status} resp:[${msg}]"
    }
}

def modeChangeHandler(evt) {
	log.debug "modeChangeHandler() mode changed ${evt.value}"
    def isy = getIsyHub()
    if (!isy) { return }
    def host = isy.value.networkAddress + ":" + isy.value.port
    def auth = getAuthorization()
    def variables = getVariables()
	
    if (!(settings?.modeMapVariable)) {
    	log.debug "modeChangeHandler() modeMapVariable not set"
        return
    }
    
    //log.debug "settings:${settings} looking for modeMap${evt.value}"
	if (settings?."modeMap${evt.value}") {
    	def mapping = settings."modeMap${evt.value}"
        def modeMapVariable = variables.find { settings.modeMapVariable.startsWith(it.value.name) }
        def mappingVariable = variables.find { mapping.startsWith(it.value.name) }
        if (modeMapVariable && mappingVariable) {
           	def path = "/rest/vars/set/${modeMapVariable.value.type}/${modeMapVariable.value.id}/${mappingVariable.value.value}"
			log.debug "modeMap${evt.value}: setting ${modeMapVariable.value.name} to ${mappingVariable.value.name}"
            sendHubCommand(new physicalgraph.device.HubAction(
           	        'method': 'GET',
           	        'path': path,
           	        'headers': [
           	            'HOST': host,
           	            'Authorization': auth
           	        ], null, [callback:parseModeChange]))
        }
	}
}

def printDebugInfo() {
	if (settings.debug) {
    	log.debug "printDebugInfo()"
        def hub = location.hubs[0]
        log.debug "id: ${hub.id}"
        log.debug "zigbeeId: ${hub.zigbeeId}"
        log.debug "zigbeeEui: ${hub.zigbeeEui}"

        // PHYSICAL or VIRTUAL
        log.debug "type: ${hub.type}"

        log.debug "name: ${hub.name}"
        log.debug "firmwareVersionString: ${hub.firmwareVersionString}"
        log.debug "localIP: ${hub.localIP}"
        log.debug "localSrvPortTCP: ${hub.localSrvPortTCP}"
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// SmartApp routines

def installed() {
    // remove location subscription
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug('Initializing')

    settings.remove("eventVariableOnCloset")
    settings.remove("triggerVariable")
    settings.remove("eventVariableOn")
    settings.remove("eventVariableOff")
    settings.remove("triggerVariableCloset")
    settings.remove("eventVariableOnCloset")
    settings.remove("eventVariableOffCloset")
    settings.remove("triggerVariableBlue")
    settings.remove("eventVariableOnBlue")
    settings.remove("eventVariableOffBlue")
    
    state.refreshCount = 0
	state.hubRefresh = 0
    
    def isyHub = getIsyHub()
    def nodes = getNodes()
    def scenes = getScenes()
    def nodeTypes = [
    	'1.32.68.0':	'ISY Dimmer',		// Dimmer Switch
    	'1.66.68.0':	'ISY Dimmer',		// Dimmer Switch
        '1.32.69.0':	'ISY Dimmer',		// Dimmer Switch
        '1.14.67.0':	'ISY Dimmer',		// Dimmer Plugin Module
        '1.45.68.0':	'ISY Dimmer',		// 1000W Wall Dimmer Switch
        '16.8.68.0':	'ISY Leak Sensor',	// Leak Sensor
        '2.55.72.0':	'ISY Switch',		// On/Off Plugin Module
        '5.11.16.0':	'ISY Thermostat',	// Thermostat
        '2.42.68.0':	'ISY Switch',		// On/Off Switch
        '2.56.67.0':	'ISY Switch',		// On/Off Plugin Module (exterior)
        '7.0.65.0':		'ISY Switch',		// I/O Link Replay
    ]

	if (!isyHub) { return }
    
    settings.selectedNodes.each { nodeAddr ->
        def node = nodes.find { it.value.name == nodeAddr }
        def d = getChildDevices()?.find { it.device.deviceNetworkId == node.value.deviceNetworkId }
        if (!d) {
            if (nodeTypes[node.value.type]) {
            	def data = [
                    	"name": node.value.name,
                        "nodeAddress": node.value.address,
                        "deviceNetworkId": node.value.deviceNetworkId,
                        "status": node.value.status,
                        "onLevel": node.value.onLevel,
                        "rampRate": node.value.rampRate,
                    ]
            	log.debug("Adding node [${node.value.name}] to [${isyHub.value.hub}] as [${node.value.deviceNetworkId}]: ${data}")
                d = addChildDevice("raven42", nodeTypes[node.value.type], node.value.deviceNetworkId, isyHub?.value.hub, [
                    "label": node.value.name,
                    "data": data,
                ])
                d.update()
           } else {
            	log.warn "Unknown device type ${node}"
           }
        }
    }
    
    settings.selectedScenes.each { sceneAddr ->
        def scene = scenes.find { it.value.name == sceneAddr }
        def d = getChildDevices()?.find { it.device.deviceNetworkId == scene.value.deviceNetworkId }
        if (!d) {
            def data = [
                "name": scene.value.name,
                "nodeAddress": scene.value.address,
                "deviceNetworkId": scene.value.deviceNetworkId,
                "status": scene.value.status,
            ]
            log.debug("Adding scene [${scene.value.name}] to [${isyHub.value.hub}] as [${scene.value.deviceNetworkId}]: ${data}")
            d = addChildDevice("raven42", 'ISY Scene', scene.value.deviceNetworkId, isyHub?.value.hub, [
                "label": scene.value.name,
                "data": data,
            ])
            d.update()
        }
    }
    
    subscribe(location, null, statusUpdate, [filterEvents:false])
	subscribe(location, "mode", modeChangeHandler)
    //runEvery1Minute(queryVariablesLoop)
    //runEvery1Minute(queryStatusLoop)
   	//runEvery1Minute(printDebugInfo)
    //runIn(5, queryVariablesLoop)
    //runIn(15, queryStatusLoop)
}
