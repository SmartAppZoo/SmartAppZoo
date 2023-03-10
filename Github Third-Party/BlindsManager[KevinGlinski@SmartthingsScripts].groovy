
definition(
    name: "Blinds Manager",
    namespace: "glinski",
    author: "glinski",
    description: "Add each Pi Relay as an individual thing.",
    category: "Safety & Security",
    iconUrl: "https://www.easyicon.net/download/png/1189777/64/",
    iconX2Url: "https://www.easyicon.net/download/png/1189777/128/",
    iconX3Url: "https://www.easyicon.net/download/png/1189777/128/")


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
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig1", "text", title: "GPIO# or Device Name", required: false
    }
     
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])    
   
	setupVirtualRelay(deviceName1, deviceType1, deviceConfig1);
    setupVirtualRelay(deviceName2, deviceType2, deviceConfig2);
    setupVirtualRelay(deviceName3, deviceType3, deviceConfig3);
    setupVirtualRelay(deviceName4, deviceType4, deviceConfig4);
    setupVirtualRelay(deviceName5, deviceType5, deviceConfig5);
    setupVirtualRelay(deviceName6, deviceType6, deviceConfig6);
    setupVirtualRelay(deviceName7, deviceType7, deviceConfig7);
    setupVirtualRelay(deviceName8, deviceType8, deviceConfig8);
    setupVirtualRelay(deviceName9, deviceType9, deviceConfig9);
    setupVirtualRelay(deviceName10, deviceType10, deviceConfig10);
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    updateGPIOState();
    unsubscribe();
    
    updateVirtualRelay(deviceName1, deviceType1, deviceConfig1);
    updateVirtualRelay(deviceName2, deviceType2, deviceConfig2);    
    updateVirtualRelay(deviceName3, deviceType3, deviceConfig3);
    updateVirtualRelay(deviceName4, deviceType4, deviceConfig4);
    updateVirtualRelay(deviceName5, deviceType5, deviceConfig5);
    updateVirtualRelay(deviceName6, deviceType6, deviceConfig6);
    updateVirtualRelay(deviceName7, deviceType7, deviceConfig7);
    updateVirtualRelay(deviceName8, deviceType8, deviceConfig8);
    updateVirtualRelay(deviceName9, deviceType9, deviceConfig9);
    updateVirtualRelay(deviceName10, deviceType10, deviceConfig10);
    
    subscribe(location, null, response, [filterEvents:false])
}

def updateVirtualRelay(deviceName, deviceType, deviceConfig) {
    
    // If user didn't fill this device out, skip it
    if(!deviceName) return;
    
    def theDeviceNetworkId = "";
    switch(deviceType) {
    	case "switch":
        	theDeviceNetworkId = getRelayID(deviceConfig);
            break;
            
        case "temperatureSensor":
        	theDeviceNetworkId = getTemperatureID(deviceConfig);
            break;
    }
    
    log.trace "Searching for: $theDeviceNetworkId";
    
  	def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }  
    
    if(theDevice){ // The switch already exists
    	log.debug "Found existing device which we will now update"   
        theDevice.deviceNetworkId = theDeviceNetworkId + "." + deviceConfig
        theDevice.label = deviceName
        theDevice.name = deviceName
        
        if(deviceType == "switch") { // Actions specific for the relay device type
            subscribe(theDevice, "switch", switchChange)
            log.debug "Setting initial state of $deviceName to off"
            setDeviceState(deviceConfig, "off");
            theDevice.off();
        } else {
        	updateTempratureSensor();
        }
        
    } else { // The switch does not exist
    	if(deviceName){ // The user filled in data about this switch
    		log.debug "This device does not exist, creating a new one now"
        	/*setupVirtualRelay(deviceId, gpioName);*/
            setupVirtualRelay(deviceName, deviceType, deviceConfig);
       	}
    }

}
def setupVirtualRelay(deviceName, deviceType, deviceConfig) {

	if(deviceName){
        log.debug deviceName
	    log.debug deviceType
        log.debug deviceConfig
        
        switch(deviceType) {
        	case "switch":
            	log.trace "Found a relay switch called $deviceName on GPIO #$deviceConfig"
				def d = addChildDevice("ibeech", "Virtual Pi Relay", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
	    		subscribe(d, "switch", switchChange)
                
	    		log.debug "Setting initial state of $gpioName to off"
        		setDeviceState(deviceConfig, "off");
	    		d.off();
            	break;
                    
            case "temperatureSensor":
			  	log.trace "Found a temperature sensor called $deviceName on $deviceConfig"
                def d = addChildDevice("ibeech", "Virtual Pi Temperature", getTemperatureID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])                 
                state.temperatureZone = deviceConfig
                updateTempratureSensor();
            	break;
        }	    
	}
}

def String getRelayID(deviceConfig) {

	return "piRelay." + settings.piIP + "." + deviceConfig
}
def String getTemperatureID(deviceConfig){
    
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
}

def updateRelayDevice(GPIO, state, childDevices) {

  	def theSwitch = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$GPIO") }  
    if(theSwitch) { 
    	log.debug "Updating switch $theSwitch for GPIO $GPIO with value $state" 
        theSwitch.changeSwitchState(state)
    }
}

def updateTempratureSensor() {

	log.trace "Updating temperature for $state.temperatureZone"
	
	executeRequest("/devices/" + state.temperatureZone  + "/sensor/temperature/c", "GET", false, null);
    
    runIn(60, updateTempratureSensor);
}

def updateGPIOState() { 

	log.trace "Updating GPIO map"
	
	executeRequest("/*", "GET", false, null);
    
    runIn(10, updateGPIOState);
}

def switchChange(evt){

	log.debug "Switch event!";
    log.debug evt.value;
    if(evt.value == "on" || evt.value == "off") return;    
	
    
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[5];
    def state = parts.last();
    
    log.debug state;
    
    switch(state){
    	case "refresh":
        // Refresh this switches button
        log.debug "Refreshing the state of GPIO " + GPIO
        executeRequest("/*", "GET", false, null)
        return;        
    }
    
    setDeviceState(GPIO, state);
    
    return;
}


def setDeviceState(gpio, state) {
	log.debug "Executing 'setDeviceState'"
     
    // Determine the path to post which will set the switch to the desired state
    def Path = "/GPIO/" + gpio + "/value/";
	Path += (state == "on") ? "1" : "0";
    
    executeRequest(Path, "POST", true, gpio);
}

def executeRequest(Path, method, setGPIODirection, gpioPin) {
		   
	log.debug "The " + method + " path is: " + Path;
	    
    def headers = [:] 
    headers.put("HOST", "$settings.piIP:$settings.piPort")
    
    try {    	
        
        if(setGPIODirection) {
        	def setDirection = new physicalgraph.device.HubAction(
            	method: "POST",
            	path: "/GPIO/" + gpioPin  + "/function/OUT",
            	headers: headers)
            
        	sendHubCommand(setDirection);
        }
        
        def actualAction = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers)
        
        sendHubCommand(actualAction)        
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
