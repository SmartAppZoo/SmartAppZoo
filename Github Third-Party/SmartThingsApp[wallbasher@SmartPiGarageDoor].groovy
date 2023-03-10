/**
 *  WebOIPi Manager
 *
 *  Copyright 2016 iBeech
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
 * 	==== INSTRUCTIONS ===
	1) For UK go to: https://graph-eu01-euwest1.api.smartthings.com
	2) For US go to: https://graph.api.smartthings.com
	3) Click 'My SmartApps'
	4) Click the 'From Code' tab
	5) Paste in the code from here, into SmartThings
	6) Click 'Create'
	7) Click 'Publish -> For Me'
 * 
 */
definition(
    name: "WebOIPi Garage Door Manager",
    namespace: "Wallbasher",
    author: "Wallbasher",
    description: "Add each Pi Relay as an individual thing.",
    category: "Safety & Security",
    iconUrl: "http://download.easyicon.net/png/1179528/64/",
    iconX2Url: "http://download.easyicon.net/png/1179528/128/",
    iconX3Url: "http://download.easyicon.net/png/1179528/128/")


preferences {

  section("Raspberry Pi Setup"){
  	input "piIP", "text", "title": "Raspberry Pi IP", multiple: false, required: true
    input "piPort", "text", "title": "Raspberry Pi Port", multiple: false, required: true
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
  
    section("Device 1") {    
		input "deviceName1", "text", title: "Device Name", required:false	        
        input "deviceType1", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "relayTrigger":"Relay Trigger",
                "garageDoor":"Garage Door",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig1", "text", title: "GPIO# or Device Name", required: false
        input "deviceConfigRead1", "text", title: "Read GPIO#", required: false
    }
    section("Device 2") {    
		input "deviceName2", "text", title: "Device Name", required:false	        
        input "deviceType2", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "relayTrigger":"Relay Trigger",
                "garageDoor":"Garage Door",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig2", "text", title: "GPIO# or Device Name", required: false
        input "deviceConfigRead2", "text", title: "Read GPIO#", required: false
    }  
    section("Device 3") {    
		input "deviceName3", "text", title: "Device Name", required:false	        
        input "deviceType3", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "relayTrigger":"Relay Trigger",
                "garageDoor":"Garage Door",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig3", "text", title: "GPIO# or Device Name", required: false
        input "deviceConfigRead3", "text", title: "Read GPIO#", required: false
    }      
    section("Device 4") {    
		input "deviceName4", "text", title: "Device Name", required:false	        
        input "deviceType4", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "relayTrigger":"Relay Trigger",
                "garageDoor":"Garage Door",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig4", "text", title: "GPIO# or Device Name", required: false
        input "deviceConfigRead4", "text", title: "Read GPIO#", required: false
    }          
    section("Device 5") {    
		input "deviceName5", "text", title: "Device Name", required:false	        
        input "deviceType5", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig5", "text", title: "GPIO# or Device Name", required: false
    }    
    section("Device 6") {    
		input "deviceName6", "text", title: "Device Name", required:false	        
        input "deviceType6", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig6", "text", title: "GPIO# or Device Name", required: false
    }    
    section("Device 7") {    
		input "deviceName7", "text", title: "Device Name", required:false	        
        input "deviceType7", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig7", "text", title: "GPIO# or Device Name", required: false
    }   
    section("Device 8") {    
		input "deviceName8", "text", title: "Device Name", required:false	        
        input "deviceType8", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig8", "text", title: "GPIO# or Device Name", required: false
    }        
    section("Device 9") {    
		input "deviceName9", "text", title: "Device Name", required:false	        
        input "deviceType9", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig9", "text", title: "GPIO# or Device Name", required: false
    }        
    section("Device 10") {    
		input "deviceName10", "text", title: "Device Name", required:false	        
        input "deviceType10", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig10", "text", title: "GPIO# or Device Name", required: false
    }  
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def initialize() {
	log.debug "Initialized with settings: ${settings}"
    subscribe(location, null, response, [filterEvents:false])    
   
	setupDevice(deviceName1, deviceType1, deviceConfig1,deviceConfigRead1);
    setupDevice(deviceName2, deviceType2, deviceConfig2,deviceConfigRead2);
    setupDevice(deviceName3, deviceType3, deviceConfig4,deviceConfigRead3);
    setupDevice(deviceName4, deviceType4, deviceConfig4,deviceConfigRead4);
    //setupDevice(deviceName5, deviceType5, deviceConfig5);
    //setupDevice(deviceName6, deviceType6, deviceConfig6);
    //setupDevice(deviceName7, deviceType7, deviceConfig7);
    //setupDevice(deviceName8, deviceType8, deviceConfig8);
    //setupDevice(deviceName9, deviceType9, deviceConfig9);
    //setupDevice(deviceName10, deviceType10, deviceConfig10);
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    unsubscribe();
    
    updateDevice(deviceName1, deviceType1, deviceConfig1,deviceConfigRead1);
    updateDevice(deviceName2, deviceType2, deviceConfig2,deviceConfigRead2);    
    updateDevice(deviceName3, deviceType3, deviceConfig3,deviceConfigRead3);
    updateDevice(deviceName4, deviceType4, deviceConfig4,deviceConfigRead4);
    //updateDevice(deviceName5, deviceType5, deviceConfig5);
    //updateDevice(deviceName6, deviceType6, deviceConfig6);
    //updateDevice(deviceName7, deviceType7, deviceConfig7);
    //updateDevice(deviceName8, deviceType8, deviceConfig8);
    //updateDevice(deviceName9, deviceType9, deviceConfig9);
    //updateDevice(deviceName10, deviceType10, deviceConfig10);
    
    subscribe(location, null, response, [filterEvents:false])
    updateGPIOState();
}

def updateDevice(deviceName, deviceType, deviceConfig, deviceConfigRead) {
    // If user didn't fill this device out, skip it
    if(!deviceName) return;
    
	log.trace "updateDevice(" + deviceName+", "+deviceType+", "+deviceConfig+", "+deviceConfigRead+")"
    
    def theDeviceNetworkId = "";
    switch(deviceType) {
    	case "switch":
        	theDeviceNetworkId = getRelayID(deviceConfig);
            break;
        case "relayTrigger":
        	theDeviceNetworkId = getRelayTriggerID(deviceConfig, deviceConfigRead);
            break;
        case "temperatureSensor":
        	theDeviceNetworkId = getTemperatureID(deviceConfig);
            break;
        case "garageDoor":
        	theDeviceNetworkId = getRelayTriggerID(deviceConfig, deviceConfigRead);
        	break;
    }
    
    log.trace "Searching for: $theDeviceNetworkId";
    
  	def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }  
    
    if(theDevice){ // The switch already exists
    	log.debug "Found existing device which we will now update"   
        
        theDevice.label = deviceName
        theDevice.name = deviceName
        
        switch(deviceType) {
            case "switch":
            	log.debug "Changing Device to switch";
                theDevice.deviceNetworkId = getRelayID(deviceConfig)
            	// Actions specific for the switch type
            	subscribe(theDevice, "switch", switchChange)
            	log.debug "Setting initial state of $deviceName to off"
            	//setDeviceState(deviceConfig, "off");
            	theDevice.off();
            	break;
            case "relayTrigger":
            	log.debug "Changing Device to relayTrigger";
                theDevice.deviceNetworkId = getRelayTriggerID(deviceConfig, deviceConfigRead)
            	// Actions specific for the relayTrigger device type
            	subscribe(theDevice, "switch", switchToggle)
            	log.debug "Setting initial state of $deviceName to off"
            	//setDeviceState(deviceConfig, "off");
            	theDevice.off();
           		break;
            case "garageDoor":
                log.trace "Found a relay trigger called $deviceName on GPIO #$deviceConfig"
                theDevice.deviceNetworkId = getRelayTriggerID(deviceConfig, deviceConfigRead)
                subscribe(theDevice, "switch", switchToggle);
                //subscribe(d, "finishOpening", null);
                //subscribe(d, "finishClosing", null);
            	
            	break;
            case "temperatureSensor":
            	log.debug "Changing Device to temperatureSensor";
                theDevice.deviceNetworkId = getTemperatureID(deviceConfig)
            	updateTempratureSensor();
            break;
        }
        
    } else { // The switch does not exist
    	if(deviceName){ // The user filled in data about this switch
    		log.debug "This device does not exist, creating a new one now"
        	/*setupVirtualRelay(deviceId, gpioName);*/
            setupDevice(deviceName, deviceType, deviceConfig, deviceConfigRead);
       	}
    }

}
def setupDevice(deviceName, deviceType, deviceConfig, deviceConfigRead) {
	if(!deviceName) return;
    
    log.trace "setupDevice(" + deviceName+", "+deviceType+", "+deviceConfig+", "+deviceConfigRead+")"

    log.debug deviceName
    log.debug deviceType
    log.debug deviceConfig

    switch(deviceType) {
        case "switch":
            log.trace "Found a relay switch called $deviceName on GPIO #$deviceConfig"
            def d = addChildDevice("ibeech", "Virtual Pi Relay", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
            subscribe(d, "switch", switchChange);

            log.debug "Setting initial state of $gpioName to off"
            //setDeviceState(deviceConfig, "off");
            d.off();
        	break;
        case "relayTrigger":
            log.trace "Found a relay trigger called $deviceName on GPIO #$deviceConfig"
            def d = addChildDevice(
            	"Wallbasher", 
                "Virtual Pi RelayToggle", 
                getRelayTriggerID(deviceConfig, deviceConfigRead), 
                theHub.id, 
                [label:deviceName, name:deviceName])
            subscribe(d, "switch", switchToggle);

            //setDeviceState(deviceConfig, "off");
            d.off();
        	break; 
        case "garageDoor":
            log.trace "Found a relay trigger called $deviceName on GPIO #$deviceConfig"
            def d = addChildDevice(
            	"smartthings/testing", 
                "Simulated Garage Door Opener", 
                getRelayTriggerID(deviceConfig, deviceConfigRead), 
                theHub.id, 
                [label:deviceName, name:deviceName])
            subscribe(d, "switch", switchToggle);
            //subscribe(d, "finishOpening", null);
            //subscribe(d, "finishClosing", null);

            //setDeviceState(deviceConfig, "off");
        	break;  
        case "temperatureSensor":
            log.trace "Found a temperature sensor called $deviceName on $deviceConfig"
            def d = addChildDevice("ibeech", "Virtual Pi Temperature", getTemperatureID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])                 
            state.temperatureZone = deviceConfig
            updateTempratureSensor();
        	break;
    }	    
	
}

def String getRelayID(deviceConfig) {
	log.trace "Creating DeviceID: " + "piRelay." + settings.piIP + "." + deviceConfig
	return "piRelay." + settings.piIP + "." + deviceConfig
}
def String getRelayTriggerID(deviceConfig, deviceConfigRead) {
	log.trace "Creating DeviceID: " + "piRelay." + settings.piIP + "." + deviceConfig + "." + deviceConfigRead
	return "piRelay." + settings.piIP + "." + deviceConfig + "." + deviceConfigRead
}
def String getTemperatureID(deviceConfig){
    log.trace "Creating DeviceID: " + "piTemp." + settings.piIP + "." + deviceConfig
    return  "piTemp." + settings.piIP + "." + deviceConfig
}

def uninstalled() {
  unsubscribe()
  def delete = getChildDevices()
    delete.each {
    	unsubscribe(it)
    	log.trace "about to delete device"
        deleteChildDevice(it.deviceNetworkId)
    }   
}

def response(evt){
 def msg = parseLanMessage(evt.description);
    if(msg && msg.body){
    
        if(msg.json && msg.json.GPIO)
        {
        	log.trace "Update State Device List"
            getChildDevices().each { device -> 
                log.trace "Device: "+ device.name +", "+device.typeName +", " + device.deviceNetworkId
                if(device.typeName == "Virtual Pi RelayToggle") {
                    log.trace "readPin:" + device.deviceNetworkId.tokenize('.')[6] 
                    log.trace "readPinValue:" + msg.json.GPIO.find {x->x.key==device.deviceNetworkId.tokenize('.')[6]} .value.value   
                    if(msg.json.GPIO.find {x->x.key==device.deviceNetworkId.tokenize('.')[6]} .value.value==1){
                        device.changeSwitchState(1)
                    }
                    else
                    {
                        device.changeSwitchState(0)
                    }
                }
                if(device.typeName == "Simulated Garage Door Opener") {
                    log.trace "readPin" + device.deviceNetworkId.tokenize('.')[6] 
                    log.trace "readPinValue" + msg.json.GPIO.find {x->x.key==device.deviceNetworkId.tokenize('.')[6]} .value.value  
                    if(msg.json.GPIO.find {x->x.key==device.deviceNetworkId.tokenize('.')[6]} .value.value==1){
                        device.changeDoorState("open")
                    }
                    else
                    {
                        device.changeDoorState("closed")
                    }
                }
            }
        }
        
        
    	/*if(msg.json) {
           msg.json.GPIO.each { item ->
                updateRelayDevice(item.key, item.value.value, children);
            }
            
            log.trace "Finished Getting GPIO State"
        }
        
        def tempContent = msg.body.tokenize('.')
        if(tempContent.size() == 2 && tempContent[0].isNumber() && tempContent[1].isNumber() ) {
            
        	// Got temperature response            
            def networkId = getTemperatureID(state.temperatureZone);
            def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(networkId) }  
            
            if(theDevice) {
                theDevice.setTemperature(msg.body, state.temperatureZone);
                log.trace "$theDevice set to $msg.body"
            }
        }
        */
    }
}

/*def response(evt){
 def msg = parseLanMessage(evt.description);
    if(msg && msg.body){
    
    	// This is the GPIO headder state message
        def children = getChildDevices(false)
    	if(msg.json) {
           msg.json.GPIO.each { item ->
                updateRelayDevice(item.key, item.value.value, children);
            }
            
            log.trace "Finished Getting GPIO State"
        }
        
        def tempContent = msg.body.tokenize('.')
        if(tempContent.size() == 2 && tempContent[0].isNumber() && tempContent[1].isNumber() ) {
            
        	// Got temperature response            
            def networkId = getTemperatureID(state.temperatureZone);
            def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(networkId) }  
            
            if(theDevice) {
                theDevice.setTemperature(msg.body, state.temperatureZone);
                log.trace "$theDevice set to $msg.body"
            }
        }
    }
}*/



def updateRelayDevice(GPIO, state, childDevices) {
	log.trace "updateRelayDevice("+GPIO+", "+state+", "+childDevices+")"
    
  	def theSwitch = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$GPIO") }  
    if(theSwitch) { 
    	log.debug "Updating switch $theSwitch for GPIO $GPIO with value $state" 
        theSwitch.changeSwitchState(state)
    }
}

def updateTempratureSensor() {
	log.trace "updateTempratureSensor()"
	
	executeRequest("/devices/" + state.temperatureZone  + "/sensor/temperature/c", "GET", false, null);
    
    runIn(60, updateTempratureSensor);
}

def updateGPIOState() { 
	refreshState()
    runIn(15, updateGPIOState);
}

def switchChange(evt){
	log.trace "switchChange("+ evt +")";
    if(evt.value == "on" || evt.value == "off") return;    
	
    
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[5];
    def state = parts[6];
    
    log.debug state;
    
    switch(state){
    	case "refresh":
        	refreshState();
        return;        
    }
    
    setDeviceState(GPIO, state, "OUT");
    refreshState();
    
    return;
}

def switchToggle(evt){
	log.trace "switchToggle("+ evt +")";
    log.trace evt.value.tokenize('.');
    
    if(evt.value == "on" || evt.value == "off") return;    
	
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[5];
    def state = parts[6];
    
    log.debug state;
    
    setDeviceState(GPIO, "on", "OUT");
    setDeviceState(GPIO, "off", "IN");
    refreshState();
            
    return;
}


def setDeviceState(GPIO, state, setGPIODirection) {
	log.trace "setDeviceState("+GPIO+", "+state+", "+setGPIODirection+")"
    def path = "/GPIO/" + GPIO  + "/function/" + setGPIODirection
    executeRequest(path, "POST");
}

def refreshState() {
    executeRequest("/*", "GET")
}

def executeRequest(path, method) {	   
	log.trace "executeRequest("+path+", "+method+")"
	    
    def headers = [:] 
    headers.put("HOST", "$settings.piIP:$settings.piPort")
    
    try {
    	def setDirection = new physicalgraph.device.HubAction(
            	method: method,
            	path: path,
            	headers: headers)
            
            log.trace "sendHubCommand(" + setDirection +")"
        	sendHubCommand(setDirection);
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

/* Helper functions to get the network device ID */
private String NetworkDeviceId(){
    def iphex = convertIPtoHex(settings.piIP).toUpperCase()
    def porthex = convertPortToHex(settings.piPort)
    return "$iphex:$porthex" 
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    //log.debug hexport
    return hexport
}
