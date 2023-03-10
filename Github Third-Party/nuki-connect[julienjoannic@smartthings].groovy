/**
 *  Nuki Smart Lock (Connect)
 *
 *  Copyright 2017 Julien JOANNIC
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
include 'asynchttp_v1'
 
definition(
    name: "Nuki (Connect)",
    namespace: "julienjoannic",
    author: "Julien JOANNIC",
    description: "Integrate the Nuki Smart Lock into your Smartthings environment",
    category: "Safety & Security",
    singleInstance: true,
    iconUrl: "https://applets.imgix.net/https%3A%2F%2Fassets.ifttt.com%2Fimages%2Fchannels%2F1521185749%2Ficons%2Fon_color_large.png%3Fversion%3D0?ixlib=rails-2.1.3&w=240&h=240&auto=compress&s=e660356db15ab2cbfab5f4131a89ec73",
    iconX2Url: "https://applets.imgix.net/https%3A%2F%2Fassets.ifttt.com%2Fimages%2Fchannels%2F1521185749%2Ficons%2Fon_color_large.png%3Fversion%3D0?ixlib=rails-2.1.3&w=240&h=240&auto=compress&s=e660356db15ab2cbfab5f4131a89ec73",
    iconX3Url: "https://applets.imgix.net/https%3A%2F%2Fassets.ifttt.com%2Fimages%2Fchannels%2F1521185749%2Ficons%2Fon_color_large.png%3Fversion%3D0?ixlib=rails-2.1.3&w=240&h=240&auto=compress&s=e660356db15ab2cbfab5f4131a89ec73")


preferences {
	page("mainPage", "Nuki (Connect)")
    page("bridgeAuthentication", "Bridge Authentication")
    page("authenticationFailed", "Authentication Failed")
}

def getBridgeById(bridgeId) {
	return state.bridges?.find { it.bridgeId == bridgeId }
}

def mainPage() {
	// Discover bridges
	if (!state.discovering && !state.bridges) {
		discoverBridges()
    }
    
    def bridgeOptions = [:]
   	state.bridges?.each { bridge ->
    	bridgeOptions[bridge.bridgeId] = bridge.bridgeId
    }
    
    // Delete locks on deselected bridges
    state.locks?.removeAll { !selectedBridges?.contains(it.bridgeId) }

	// Page generation
 	return dynamicPage(name: "mainPage", refreshInterval: 2, install: selectedLocks != null, uninstall: true) {
    	section {
        	paragraph "Smartthings will automatically discover your Nuki bridges and locks. Choose the bridges to manage, authenticate them if needed and select the locks to add to Smartthings."
        }
    	section("Bridges") {
            def text = (bridgeOptions) ? "${bridgeOptions.size()} found" : "searching ..."
            input(name: "selectedBridges", type: "enum", title: "Nuki Bridge(s) (${text})", multiple: true, options: bridgeOptions, submitOnChange: true)
        }
        
        // Check authentication of selected bridges
        selectedBridges.each { bridgeId ->
        	def bridge = getBridgeById(bridgeId)
            if (!bridge.token) {
                section() {
                	href(name: "toBridgeAuthentication",
                    	 title: "Authentication required", 
                    	 page: "bridgeAuthentication",
                         params: ["bridgeId": bridgeId],
                         description: "Authenticate bridge ${bridgeId}",
                         image: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png")
                }
            }
            else {
            	listLocks(bridgeId)
            }
        }
        
        // Display found locks
        def lockOptions = [:]
        state.locks?.each { lock ->
        	lockOptions[lock.lockId] = lock.name
        }
        
        section("Locks") {
        	def text = (!selectedBridges?.size()) ? "Select one or more bridges" : "${lockOptions?.size()} found"
            input(name: "selectedLocks", type: "enum", title: "Nuki Lock(s) (${text})", multiple: true, options: lockOptions, submitOnChange: true)
        }
    }
}

def discoverBridges() {
    /*def params = [
        uri:  'https://factory.nuki.io/discover/bridges',
        contentType: 'application/json'
    ]
    log.debug "Querying ${params.uri}"

    asynchttp_v1.get('onBridgeDiscovered', params)*/
    
    /*render contentType: 'text/html', data: "<html><body><p>miaou</p></body></html>"
    
    log.debug "Querying http://factory.nuki.io:443/discover/bridges"
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "GET",
    	path: "/",
    	headers: [
        	HOST: "216.146.38.70:80"
    	]], deviceNetworkId, [callback: "onBridgeDiscovered"]))
    
    state.discovering = true*/
    
    state.bridges = [["bridgeId":106201270,"ip":"192.168.2.3","port":8080,"dateUpdated":"2017-11-09T17:54:26Z"]]
    
    state.bridges.each { bridge ->
    	bridge.bridgeId = bridge.bridgeId.toString()
        def device = getChildDevices()?.find { it.currentValue("id") == bridge.bridgeId }
        if (device) {
        	bridge.token = device.currentValue("token")
            bridge.mac = device.deviceNetworkId
        }
    }
}

def onBridgeDiscovered(response, blip) {
    log.debug "got response data: ${response.json}"
    state.bridges = response.json.bridges
    state.bridges = [["bridgeId":106201270,"ip":"192.168.2.3","port":8080,"dateUpdated":"2017-11-09T17:54:26Z"]]
    
    state.bridges.each { bridge ->
    	bridge.bridgeId = bridge.bridgeId.toString()
        def device = getChildDevices()?.find { it.currentValue("id") == bridge.bridgeId }
        if (device) {
        	bridge.token = device.currentValue("token")
            bridge.mac = device.deviceNetworkId
        }
    }
    
    state.discovering = false
}

def bridgeAuthentication(params) {
	// Save/restore params
	if (params?.bridgeId) {
    	state.bridgeAuthenticationParams = params
    }
    else {
    	params = state.bridgeAuthenticationParams
    }
    
    // Get bridge token
    def bridge = getBridgeById(params.bridgeId)
    if (!bridge.token) {
        authenticateBridge(bridge.bridgeId)
    }
        
    return dynamicPage(name: "bridgeAuthentication", refreshInterval: 2, nextPage: "mainPage") {
        if (bridge.token) {
            section {
                paragraph "Authentication successful! Please go back to the main page to select the Nuki locks to add to Smartthings."
            }
        }
        else {
            section {
                paragraph "Your bridge will now turn its LED on for 30 seconds. Please push the button during this time to authenticate Smartthings."
            }
        }
    }
}

def authenticateBridge(bridgeId) {
	def bridge = getBridgeById(bridgeId)
    def host = "${bridge.ip}:${bridge.port}"
	log.debug "Querying http://${host}/auth"
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "GET",
    	path: "/auth",
    	headers: [
        	HOST: host
    	]], deviceNetworkId, [callback: "onAuthentication"]))
}

def onAuthentication(physicalgraph.device.HubResponse response) {
	log.debug "Received /auth response ${response} with data ${response.json}"
    
    if (response.json?.success) {
    	// Identify bridge from IP and port since we have no better info
        def ip = convertHexToIP(response.ip)
        def port = convertHexToInt(response.port)
    	def bridge = state.bridges.find { it.ip == ip && it.port == port }
        if (bridge) {
        	log.debug "Bridge ${bridge.bridgeId} is now authorized"
        	bridge.token = response.json.token
        	bridge.mac = response.mac.toString()
        }
        else {
        	log.warn "Could not find bridge with IP ${ip} and port ${port}"
        }
    }
}

def listLocks(bridgeId) {
	def bridge = getBridgeById(bridgeId)
	def host = "${bridge.ip}:${bridge.port}"
	log.debug "Querying http://${host}/list"
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "GET",
    	path: "/list",
        query: ["token": bridge.token],
    	headers: [
        	HOST: host
    	]], deviceNetworkId, [callback: "onList"]))
}

def onList(physicalgraph.device.HubResponse response) {
	log.debug "Received /list response: ${response.json}"
    
    response.json.each { lock ->
    	lock.bridgeId = state.bridges.find{ it.mac == response.mac }.bridgeId
        lock.lockId = "${lock.bridgeId}:${lock.nukiId}".toString()
    	if (!state.locks.find { it.nukiId == lock.nukiId && it.bridgeId == lock.bridgeId }) {
        	if (!state.locks) state.locks = []
        	state.locks << lock
        }
    }
}

def updateBridges() {
	selectedBridges?.each { bridgeId ->
    	def bridge = getBridgeById(bridgeId)
    	def device = getChildDevices()?.find { log.debug "${it.deviceNetworkId} == ${bridge.mac}"; it.deviceNetworkId == bridge.mac }
        
        if (!device) {
        	log.debug "Adding device for bridge ${bridgeId}"
            device = addChildDevice("julienjoannic", "Nuki Bridge", bridge.mac, location.hubs[0].id)
            device.sendEvent(name: "id", value: bridge.bridgeId.toString())
            device.sendEvent(name: "token", value: bridge.token)
            device.sendEvent(name: "host", value: "${bridge.ip}:${bridge.port}")
        }
    }
    
    getChildDevices()?.each { device ->
    	if (!selectedBridges.contains(device.currentValue("id"))) {
        	deleteChildDevice(device.deviceNetworkId)
        }
    }
}

def updateLocks() {
	log.debug "Updating locks: ${selectedLocks}"
    
    def locks = []
    selectedLocks.each { lockId ->
    	def lock = state.locks.find { it.lockId == lockId }
        locks << lock
    }
    
	getChildDevices()?.each { bridge ->
    	bridge.updateLocks(locks)
    }
}

def initialize() {
	updateBridges()
    updateLocks()
    if (getChildDevices()?.size() > 0) {
    	runEvery5Minutes("discoverBridges")
        getChildDevices().each { bridge ->
        	bridge.registerCallback()
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

def uninstalled() {
	log.debug "Uninstalled"
    
    getChildDevices()?.each { bridge ->
    	deleteChildDevice(bridge.deviceNetworkId)
    }
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}