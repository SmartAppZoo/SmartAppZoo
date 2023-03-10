/**
 *  Temperature Zone Controller
 * 
 *  Author: Troy Erickson
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
 
/*
 * Imports
 */
 
import groovy.transform.Field
 
/* 
 * Global Variable Definitions
 */


 
/*
 * Smart App Definition
 */
 
definition(
    name: "Temperature Zone Controller",
    namespace: "t-royx",
    author: "Troy Erickson",
    description: "Controls a damper using a temperature sensor",
    category: "Safety & Security",
    iconUrl: "http://download.easyicon.net/png/1179528/64/",
    iconX2Url: "http://download.easyicon.net/png/1179528/128/",
    iconX3Url: "http://download.easyicon.net/png/1179528/128/")


preferences {

  section("Temperature Setpoints")
  {
  	input "upperSetpoint", "int", "title": "Upper Temperature Limit C", multiple: false, required: true
    input "lowerSetpoint", "int", "title": "Lower Temperature Limit C", multiple: false, required: true
    input "piPort", "text", "title": "Raspberry Pi Port", multiple: false, required: true
    input "defaultDamperState", "enum", title: "Default Damper State", required: true, options: 
    			[
    				"open":"Open",
                	"closed":"Closed"
                ]
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }
  
  section("Temperature Sensor Configuration") 
  {    
	input "deviceName1", "text", title: "Device Name", required:false
    input "piTempIP", "text", "title": "RPi Temp IP", multiple: false, required: true
    input "deviceType1", "enum", title: "Device Type", required: false, options: 
    			[                
                	"switch":"Relay Switch",
                	"temperatureSensor":"Temperature Sensor"
                ]
   	input "deviceConfig1", "text", title: "GPIO# or Device Name", required: false
  }
  
  section("Relay Configuration") 
  {    
		input "deviceName2", "text", title: "Device Name", required:false
        input "piRelayIP", "text", "title": "RPi Relay IP", multiple: false, required: true
        input "deviceType2", "enum", title: "Device Type", required: false, options: [                
                "switch":"Relay Switch",
                "temperatureSensor":"Temperature Sensor"]
        input "deviceConfig2", "text", title: "GPIO# or Device Name", required: false
    }               
}

// Global Variables
@Field boolean AllowAirFlow = false;
//@Field boolean EnableRelay = false;

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])    
   
	setupVirtualRelay(deviceName1, deviceType1, deviceConfig1);
    setupVirtualRelay(deviceName2, deviceType2, deviceConfig2);
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    updateGPIOState();
    unsubscribe();
    
    updateVirtualRelay(deviceName1, deviceType1, deviceConfig1);
    updateVirtualRelay(deviceName2, deviceType2, deviceConfig2);    
    
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

	return "piRelay." + settings.piRelayIP + "." + deviceConfig
}
def String getTemperatureID(deviceConfig){
    
    return  "piTemp." + settings.piTempIP + "." + deviceConfig
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
        
        // Check if a temperature response was received
        def tempContent = msg.body.tokenize('.')
        if(tempContent.size() == 2 && tempContent[0].isNumber() && tempContent[1].isNumber() ) {
            
            log.trace "Temperature Received"
            
            float temp = (float)tempContent[0].toInteger() + ((float)tempContent[1].toInteger()/(float)100)
            int max = upperSetpoint.toInteger()
            int min = lowerSetpoint.toInteger()
            
            if((temp > (float)max) || (temp < (float)min))
            {
            	log.trace "Allow Air Flow"
                controlDampers(true);
            }
            else
            {
            	log.trace "Don't Allow AirFlow"
            	controlDampers(false);
            }
            
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

def controlDampers(AllowAirFlow){

	boolean EnableRelay;
	log.trace "Controlling Dampers"
    
	/* Determine the default damper state */
	if("closed" == defaultDamperState)
   	{
    	log.trace "Damper Normally Closed"
    
    	/* The default state is closed, check if the damper should be open or closed */
        if(true == AllowAirFlow)
        {
        	log.trace "Default damper closed, enabling relay"
        	EnableRelay = true;
        }
        else
        {
        	log.trace "Default damper closed, disabling relay"
        	EnableRelay = false;
        }
    }
    else
    {
    	log.trace "Damper Normally Open"
    
    	/* The default state is open, check if the damper should be open or closed */
        if(true == AllowAirFlow)
        {
        	log.trace "Default damper opened, disabling relay"
        	EnableRelay = false;
        }
        else
        {
        	log.trace "Default damper opened, enabling relay"
        	EnableRelay = true;
        }
    }
    
    if(true == EnableRelay){
        log.trace "Setting Device State On"
        setDeviceState(21, "on")
    }
    else
    {
    	log.trace "Setting Device State Off"
    	setDeviceState(21, "off")
    }
}

def updateRelayDevice(GPIO, state, childDevices) {

  	def theSwitch = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$GPIO") }  
    if(theSwitch) { 
    	log.debug "Updating switch $theSwitch for GPIO $GPIO with value $state" 
        theSwitch.changeSwitchState(state)
    }
}

def updateTempratureSensor() 
{
	log.trace "Updating temperature for $state.temperatureZone"

	/* Put in the request to read the temperature sensor from the raspberrypi */
	executeRequest("Temp", "/devices/" + state.temperatureZone  + "/sensor/temperature/c", "GET", false, null);
    
    /* Re-run this function to get the temperature again in the defined amount of time */
    runIn(15, updateTempratureSensor);
}

def updateGPIOState() { 

	log.trace "Updating GPIO map"
	
	executeRequest("Relay", "/*", "GET", false, null);
    
    runIn(20, updateGPIOState);
}

def switchChange(evt){

	log.debug "Switch event!";
    log.debug evt.value;
    if(evt.value == "on" || evt.value == "off") return;    
	
    
    def parts = evt.value.tokenize('.');
    def deviceId = parts[1];
    def GPIO = parts[6];
    def state = parts[7];
    
    log.debug state;
    
    switch(state){
    	case "refresh":
        // Refresh this switches button
        log.debug "Refreshing the state of GPIO " + GPIO
        executeRequest("Relay", "/*", "GET", false, null)
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
    
    executeRequest("Relay", Path, "POST", true, gpio);
}

def executeRequest(Type, Path, method, setGPIODirection, gpioPin) {
		   
	log.debug "The " + method + " path is: " + Path;
	    
    def headers = [:] 
    
    if("Temp" == Type)
    {
    	headers.put("HOST", "$settings.piTempIP:$settings.piPort")
    }
    else
    {
    	// Otherwise Relay
        headers.put("HOST", "$settings.piRelayIP:$settings.piPort")
    }
    
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
