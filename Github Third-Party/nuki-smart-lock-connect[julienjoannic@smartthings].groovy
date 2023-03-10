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
    name: "Nuki Smart Lock (Connect)",
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

def initialize() {
	updateBridges()
    updateLocks()
    if (getChildDevices?.size() > 0) {
    	runEvery5Minutes("discoverBridges")
    }
}

def mainPage() {
	// Discover bridges
	if (!state.discovering && !state.bridges) {
		discoverBridges()
    }
    
    // Register selected bridges
    updateBridges()

	// Page generation
 	return dynamicPage(name: "mainPage", refreshInterval: 1, install:true, uninstall: true) {
    	section {
        	paragraph "Smartthings will automatically discover your Nuki bridges and locks. Choose the bridges to manage, authenticate them if needed and select the locks to add to Smartthings."
        }
    	section("Bridges") {
            def text = (state.bridges == null) ? "searching ..." : "${state.bridges.size()} found"
            input(name: "selectedBridges", type: "enum", title: "Nuki Bridge(s) (${text})", multiple: true, options: state.bridgeOptions, submitOnChange: true)
        }
        
        // Check authentication of selected bridges
        state.locks = [:]
        def lockOptions = [:]
        selectedBridges.each { selectedBridge ->
        	def device = getBridgeDevice(selectedBridge)
            if (!device.authenticated()) {
                section() {
                	href(name: "toBridgeAuthentication",
                    	 title: "Authentication required", 
                    	 page: "bridgeAuthentication",
                         params: ["bridgeId": selectedBridge],
                         description: "Authenticate bridge ${selectedBridge}",
                         image: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png")
                }
            }
            else {
            	if (!device.state.locks && !device.state.busy) {
                	device.list()
                }
                else {
                	device.state.locks.each { lock ->
                    	def lockId = "${device.deviceNetworkId}:${lock.nukiId}"
                    	state.locks[lockId] = lock
                        lockOptions[lockId] = lock.name
                    }
                }
            }
        }
        
        section("Locks") {
        	def text = (!selectedBridges?.size()) ? "Select one or more bridges" : "${lockOptions?.size()} found"
            input(name: "selectedLocks", type: "enum", title: "Nuki Lock(s) (${text})", multiple: true, options: lockOptions, submitOnChange: true)
        }
    }
}

def discoverBridges() {
    def params = [
        uri:  'https://factory.nuki.io/discover/bridges',
        contentType: 'application/json'
    ]
    log.debug "Querying ${params.uri}"

    asynchttp_v1.get('onBridgeDiscovered', params)
    state.discovering = true
}

def onBridgeDiscovered(response, data) {
    log.debug "got response data: ${response.getData()}"
    //state.bridges = response.json.bridges
    def bridges = [ [
 		"bridgeId": 2117604523,
        "ip": "192.168.2.170",
        "port": 8080,
        "dateUpdated": "2017-06-14T06:53:44Z"
 	] ]
    
    state.bridgeOptions = [:]
    state.bridges = [:]
    bridges.each { bridge ->
    	state.bridges[bridge.bridgeId] = bridge
    	state.bridgeOptions[bridge.bridgeId] = bridge.bridgeId
        
        def device = getBridgeDevice(bridge.bridgeId)
        if (device) {
        	//device.setHost(bridge.ip, bridge.port)
            device.sendEvent(name: "host", value: "${bridge.ip}:${bridge.port}")
        }
    }
    
    state.discovering = false
}

def getBridgeDevice(bridgeId) {
	return getChildDevices()?.find { it.currentValue("id") == bridgeId }
}

def updateBridges() {
	selectedBridges?.each { bridgeId ->
    	def bridge = state.bridges[bridgeId]
    	def device = getBridgeDevice(bridgeId)
        
        if (!device) {
        	log.debug "Adding device for bridge ${bridgeId}"
            device = addChildDevice("julienjoannic", "Nuki Bridge", bridgeId, location.hubs[0].id)
            //device.setHost(bridge.ip, bridge.port)
            device.sendEvent(name: "id", value: bridgeId)
            device.sendEvent(name: "host", value: "${bridge.ip}:${bridge.port}")
        }
        else {
        	log.debug "Updating DNI of ${device} to ${device.currentValue("mac")}"
        }
    }
    
    getChildDevices()?.each { device ->
    	if (!selectedBridges.contains(device.currentValue("id"))) {
        	deleteChildDevice(device.deviceNetworkId)
        }
    }
}

def bridgeAuthentication(params) {
	if (params?.bridgeId) {
    	state.bridgeAuthenticationParams = params
    }
    else {
    	params = state.bridgeAuthenticationParams
    }
    
    def device = getBridgeDevice(params.bridgeId)
	def token = device.currentValue("token")

    if (!token && !device.state.authenticating) {
        device.authenticate()
    }
        
    return dynamicPage(name: "bridgeAuthentication", refreshInterval: 1) {
        if (token) {
            section {
                paragraph "Authentication successful! Please go back to the main page to select the Nuki locks to add to Smartthings."
            }
        }
        else if (device.state.authenticating) {
            section {
                paragraph "Your bridge will now turn its LED on for 30 seconds. Please push the button during this time to authenticate Smartthings."
            }
        }
        else {
            section {
                paragraph "The authentication process could not complete. Please try again."
            }
        }
    }
}

def updateLocks() {
	log.debug "Updating locks: ${selectedLocks}"
	getChildDevices()?.each { bridge ->
    	bridge.updateLocks(selectedLocks)
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