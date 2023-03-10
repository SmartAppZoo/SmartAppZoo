/**
 *  BlueBolt (Connect)
 *
 *  Copyright 2017 Kevin Corbin
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
    name: "BlueBolt (Connect)",
    namespace: "JanusNetworks",
    author: "Kevin Corbin",
    description: "Add Bluebolt devices to Smartthings",
    category: "SmartThings Labs",
    iconUrl: "http://www.iconsdb.com/icons/download/icon-sets/web-2-blue/bolt-128.jpg",
    iconX2Url: "http://www.iconsdb.com/icons/download/icon-sets/web-2-blue/bolt-128.jpg",
    iconX3Url: "http://www.iconsdb.com/icons/download/icon-sets/web-2-blue/bolt-128.jpg")


preferences {

  section("Bridge Setup"){
  	input "IP", "text", "title": "BlueBolt API Bridge IP", multiple: false, required: true
    input "port", "text", "title": "BlueBolt API Bridge Port", multiple: false, required: true
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
                             
    section("Outlet 1") {
		input "deviceName1", "text", title: "Device Name", required:false
    }
    section("Outlet 2") {
		input "deviceName2", "text", title: "Device Name", required:false
    }
        section("Outlet 3") {
		input "deviceName3", "text", title: "Device Name", required:false
    }
    section("Outlet 4") {
		input "deviceName4", "text", title: "Device Name", required:false
    }
 	section("Outlet 5") {
		input "deviceName5", "text", title: "Device Name", required:false
    }
    section("Outlet 6") {
		input "deviceName6", "text", title: "Device Name", required:false
    }
	section("Outlet 7") {
		input "deviceName7", "text", title: "Device Name", required:false
    }
	section("Outlet 8") {
		input "deviceName8", "text", title: "Device Name", required:false
    }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def initialize() {
    subscribe(location, null, response, [filterEvents:false])
	setupDevice(deviceName1, 1);
    setupDevice(deviceName2, 2);
    setupDevice(deviceName3, 3);
	setupDevice(deviceName4, 4);
    setupDevice(deviceName5, 5);
    setupDevice(deviceName6, 6);
    setupDevice(deviceName7, 7);
    setupDevice(deviceName8, 8);
}

def updated() {
	// called when changes are made to the smartapp configuration
    log.debug "Updated with settings: ${settings}"
	setupDevice(deviceName1, 1);
    setupDevice(deviceName2, 2);
    setupDevice(deviceName3, 3);
	setupDevice(deviceName4, 4);
    setupDevice(deviceName5, 5);
    setupDevice(deviceName6, 6);
    setupDevice(deviceName7, 7);
    setupDevice(deviceName8, 8);
}

def setupDevice(deviceName, deviceConfig) {
	log.debug "Bluebolt Manager setupDevice()"
	if(deviceName){
        log.debug deviceName
	   
        def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(GetDeviceNetworkID(deviceConfig)) }
        if(theDevice){ 
        // can't really update device if it's type changes, as this could break smartapps, etc - best to just delete/recreate it
        	log.debug "device already exists - updating" // The device already exists
            deleteChildDevice(GetDeviceNetworkID(deviceConfig))
        }
        def d = addChildDevice("JanusNetworks", "BlueBolt Outlet", GetDeviceNetworkID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
        // subscribe to events if necessary
        // subscribe(d, <capability>, callbackmethod)
	}
}
   
def uninstalled() {
	// used when the service manager smartapp is uninstalled, deletes all children
    def delete = getChildDevices()
    delete.each {
    	log.trace "Removing device: " + it.deviceName
        deleteChildDevice(it.deviceNetworkId)
        }
}

def response(evt){
	def msg = parseLanMessage(evt.description);
    
    if (msg.status == 200) {
        msg.json.each { item ->
        	def dev = getChildDevice("$settings.IP:$settings.port:$item.key")
            def outletId = item.key
            def outletState = item.value
            log.debug(outletId)
            log.debug(outletState)
 
        }
        log.trace "Finished Processing Response"
    }
}


def pollChildren(){
	log.debug("service manager polling children")
    executeRequest('/switch/status', "GET")
}

def on(device) {
	log.debug('Turning on device ' + device.getName())
    def id = device.deviceNetworkId.split(":")[2]
    executeRequest('/switch/' + id + '/ON', "POST")

}

def off(device) {
	log.debug('Turning off device ' + device.getName())
    def id = device.deviceNetworkId.split(":")[2]
    executeRequest('/switch/' + id + '/OFF', "POST")


}

// HELPER FUNCTIONS

// request function
def executeRequest(Path, method) {
	
    def headers = [:]
    headers.put("HOST", "$settings.IP:$settings.port")
    try {
        def resp = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers)
		log.debug method + " $settings.IP:$settings.port" + Path 
       
        sendHubCommand(resp)
    }
    
    
    
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

/* Helper functions to get the network device ID */



def String GetDeviceNetworkID(deviceConfig) {
	// used to set the IP address on child devices so they know where to point REST calls to
	return settings.IP + ":" + settings.port + ":" + deviceConfig
}