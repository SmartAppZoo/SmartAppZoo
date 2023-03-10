/**
* WebOIPi RaspberryPi Relay - Momentary Button Manager
*  Author: Brandon Artz
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
* 
*  Date: 2017-01-19
*/

//Definition section of the app (metadata)
definition(
    name: "WebOIPi RaspberryPi Relay - Momentary Button Manager",
    namespace: "brandonartz",
    author: "brandonartz",
    description: "Add RaspberryPi relay as a momentary button thing.",
    category: "Safety & Security",
    iconUrl: "http://download.easyicon.net/png/1179528/64/",
    iconX2Url: "http://download.easyicon.net/png/1179528/128/",
    iconX3Url: "http://download.easyicon.net/png/1179528/128/")

//Setup Preferences
preferences {
   section("Raspberry Pi Setup"){
     input "piIP", "text", "title": "Raspberry Pi IP", multiple: false, required: true
     input "piPort", "text", "title": "Raspberry Pi Port", multiple: false, required: true
     input "theHub", "hub", title: "On which hub?", multiple: false, required: true
   }
  
    section("Device 1") {    
	input "deviceName1", "text", title: "Device Name", required:false	        
        input "deviceConfig1", "text", title: "GPIO# or Device Name", required: false
    }
    section("Device 2") {    
	input "deviceName2", "text", title: "Device Name", required:false	        
        input "deviceConfig2", "text", title: "GPIO# or Device Name", required: false
    }  
    section("Device 3") {    
	input "deviceName3", "text", title: "Device Name", required:false	        
        input "deviceConfig3", "text", title: "GPIO# or Device Name", required: false
    }      
    section("Device 4") {    
	input "deviceName4", "text", title: "Device Name", required:false	        
        input "deviceConfig4", "text", title: "GPIO# or Device Name", required: false
    }          
    section("Device 5") {    
	input "deviceName5", "text", title: "Device Name", required:false	        
        input "deviceConfig5", "text", title: "GPIO# or Device Name", required: false
    }    
    section("Device 6") {    
	input "deviceName6", "text", title: "Device Name", required:false	        
        input "deviceConfig6", "text", title: "GPIO# or Device Name", required: false
    }    
    section("Device 7") {    
	input "deviceName7", "text", title: "Device Name", required:false	        
        input "deviceConfig7", "text", title: "GPIO# or Device Name", required: false
    }   
    section("Device 8") {    
	input "deviceName8", "text", title: "Device Name", required:false	        
        input "deviceConfig8", "text", title: "GPIO# or Device Name", required: false
    }          
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def initialize() {
    //This subscribes to listen for results of Hub commands.
    subscribe(location, null, response, [filterEvents:false])
    
    //This adds child devices to the namespace and subscribes to device events
    setupVirtualRelay(deviceName1, deviceConfig1);
    setupVirtualRelay(deviceName2, deviceConfig2);
    setupVirtualRelay(deviceName3, deviceConfig3);
    setupVirtualRelay(deviceName4, deviceConfig4);
    setupVirtualRelay(deviceName5, deviceConfig5);
    setupVirtualRelay(deviceName6, deviceConfig6);
    setupVirtualRelay(deviceName7, deviceConfig7);
    setupVirtualRelay(deviceName8, deviceConfig8);
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    
    unsubscribe();
    
    updateVirtualRelay(deviceName1, deviceConfig1);
    updateVirtualRelay(deviceName2, deviceConfig2);    
    updateVirtualRelay(deviceName3, deviceConfig3);
    updateVirtualRelay(deviceName4, deviceConfig4);
    updateVirtualRelay(deviceName5, deviceConfig5);
    updateVirtualRelay(deviceName6, deviceConfig6);
    updateVirtualRelay(deviceName7, deviceConfig7);
    updateVirtualRelay(deviceName8, deviceConfig8);
    
    subscribe(location, null, response, [filterEvents:false])
}

def setupVirtualRelay(deviceName, deviceConfig) {
    if(deviceName){
		def d = addChildDevice("brandonartz", "Raspberrypi Relay - Momentary Button Tile", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
		subscribe(d, "momentary", switchChange)   
    }
}

def updateVirtualRelay(deviceName, deviceConfig) {
    // If user didn't fill this device out, skip it
    if(!deviceName) return;
    
    def theDeviceNetworkId = getRelayID(deviceConfig);   
    def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }  
    
    if(theDevice){ // The switch already exists
    	log.debug "Found existing device which we will now update"   
        theDevice.deviceNetworkId = theDeviceNetworkId + "." + deviceConfig
        theDevice.label = deviceName
        theDevice.name = deviceName
        
        subscribe(theDevice, "momentary", switchChange)
    } else { // The switch does not exist
    	if(deviceName){ // The user filled in data about this switch
           log.debug "This device does not exist, creating a new one now"
           setupVirtualRelay(deviceName, deviceConfig);
       	}
    }
}

def String getRelayID(deviceConfig) {
	return "piRelay." + settings.piIP + "." + deviceConfig
}

def uninstalled() {
  unsubscribe()
  def delete = getChildDevices()
    delete.each {
    	unsubscribe(it)
        deleteChildDevice(it.deviceNetworkId)
    }   
}

//We subscribe to run this during initilization
def response(evt){
}

def switchChange(evt){
	log.debug "Button pressed!";

    //Message looks like piRelay.ipaddress.GPIO#.on/off
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[5];
    def state = parts[6];
    
    sendSwitchingRequest(GPIO);
    
    return;
}

def sendSwitchingRequest(GPIO) {
	 log.trace "Sending onn/off command to GPIO $GPIO"
     try { 
     	def switchActionOn = new physicalgraph.device.HubAction(
            method: "POST",
            path: "/GPIO/" + GPIO + "/value/0",
            headers: [
              HOST: getHostAddress(settings.piIP,settings.piPort)
            ])
        def switchActionOff = new physicalgraph.device.HubAction(
            method: "POST",
            path: "/GPIO/" + GPIO + "/value/1",
            headers: [
              HOST: getHostAddress(settings.piIP,settings.piPort)
            ])
        
        List actions = []
        actions.add(switchActionOn)
        actions.add(switchActionOff)
        
		sendHubCommand(actions,2000)
      
        return switchActionOn
     }
     catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
	 }
}

// gets the address of the hub
private getCallBackAddress() {
    log.trace "getCallBackAddress"
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

// gets the address of the device
private getHostAddress(ip,port) {
    return ip + ":" + port
}

private Integer convertHexToInt(hex) {
	log.trace "convertHexToInt"
    log.debug hex
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	log.trace "convertHexToIP"
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
