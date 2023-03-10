/**
 *  WebIOPi Garage Door Manager
 *
 *  Copyright 2018 Matt Dunning
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
    name: "WebIOPi Garage Door Manager",
    namespace: "mcdunning",
    author: "Matt Dunning",
    description: "Controls the garage door through WebIOPi Device provides a garage door switch and open/close sensor",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Raspberry Pi Setup") {
		input "piIP", "text", title: "Raspberry Pi IP", multiple: false, required: true
        input "piPort", "text", title: "Raspberry Pi Port", multiple: false, required: true
	}
    
    section("Garage Door Controller Setup") {
    	input "doorOpenCloseTriggerPin", "text", title: "GPIO Pin # for open/close button", required: true
        input "doorClosedSensorPin", "text", title: "GPIO Pin # for door closed sensor", required: true
        input "doorOpenSensorPin", "text", title: "GPIO Pin # for door open sensor", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def initialize() {
   log.debug "initializing"
    
   subscribe(location, null, response, [filterEvents:false])
   
   def device = addChildDevice("mdunning", "Virtual Pi Garage Door Controller", getGarageDoorControllerId(), null, [label:"Garage Door Opener", name:"Garage Door Opener"])
   subscribe(device, "button.pushed", switchPushed)
   
   // Set the state of the door to "unknown" for initialization the state will be updated once the 
   // RPi returns an open or closed state
   device.changeSwitchState(-1)  
   
   state.cycleCount = 0
   updateGPIOState()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	
    // Set state variable
    state.cycleCount = 0
	updateGPIOState()
    
    unsubscribe()
    
    log.debug "Searching for device"
  	def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(getGarageDoorControllerId()) }  
  
    if (theDevice) { // The switch already exists
    	log.debug "Found existing device which we will now update"   
        theDevice.deviceNetworkId = getGarageDoorControllerId()
        
        subscribe(theDevice, "button.pushed", switchPushed)    
    	subscribe(location, null, response, [filterEvents:false])
        
        // Set the state of the door to "unknown" for initialization the state will be updated once the 
    	// RPi returns an open or closed state
    	theDevice.changeSwitchState(-1)  
    } else {
        log.debug "New switch encountered creating child device"
    	initialize()
    }
}

def uninstalled() {
  def delete = getChildDevices()
    delete.each {
    	log.trace "about to delete device"
        deleteChildDevice(it.deviceNetworkId)
    }   
}

def String getGarageDoorControllerId() {
	return "garageDoorController." + settings.piIP
}

def response(evt){
	log.debug "Handling Response"
    
 	def msg = parseLanMessage(evt.description);
 	if(msg && msg.json && msg.json.GPIO){
    	def sensor = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(getGarageDoorControllerId()) }
        log.debug "Cycle Count:  ${state.cycleCount}"
        if(sensor && state.cycleCount != -1) {
        	def gpioData = msg.json.GPIO
          	def doorOpenSensorPinValue = gpioData.get(doorOpenSensorPin).value
            def doorClosedSensorPinValue = gpioData.get(doorClosedSensorPin).value
             
            log.debug "Door Open Sensor Pin Value (${doorOpenSensorPin}) = ${doorOpenSensorPinValue}"
            log.debug "Door Close Sensor Pin Value (${doorClosedSensorPin}) = ${doorClosedSensorPinValue}"
                
            if (doorOpenSensorPinValue == 1 || doorClosedSensorPinValue == 1) {
            	state.cycleCount = 10
                if (doorOpenSensorPinValue == 1) {
                	sensor.changeSwitchState(0)
                } else if (doorClosedSensorPinValue == 1) {
                	sensor.changeSwitchState(1)
                }
            } else {
                // Door sensor pins are both reporting 0
                // Both pins should not be able to report 1 at the same time
                if (state.cycleCount < 10) {
                    state.cycleCount = state.cycleCount + 1
                } else {
                    state.cycleCount = 0
                    sensor.changeSwitchState(-1)
                    updateGPIOState()
                }
            }
		}
  	}
    
    log.debug "Finished Getting GPIO State"
}

def switchPushed(evt){
	log.debug "Handling Switch Pushed"
    def data = parseJson(evt.data)
    
    if (data."buttonPushed".equals("refresh")) {
        log.debug "Refreshing the state of the door opener"
        executeRequest("/*", "GET")
        updateGPIOState()
    } else if (data."buttonPushed".equals("doorOpener")){
        log.debug "Door Opener Button Pressed"
        if (state.cycleCount == 10 || state.cycleCount == -1) {
        	log.debug "Door Moving"
        	// Door opener pressed while the door was not moving
        	state.cycleCount = 0
        } else {
        	log.debug "Door Stopped"
        	// Door opener was pressed while the door was moving
        	state.cycleCount = -1
        }
        
        def path = "/GPIO/" + doorOpenCloseTriggerPin + "/value/1"
		executeRequest(path, "POST")
        // deley for a second to give the door time to move
        runIn(2, updateGPIOState)
    } else {
    	log.debug "unrecognized button has been pushed: ${data.buttonPushed}"
    }
}

def executeRequest(Path, method) {
	log.debug "Executing Request"
    
	log.debug "The " + method + " path is: " + Path
	    
    def headers = [:] 
    headers.put("HOST", "$settings.piIP:$settings.piPort")
    
    try {    	
        def actualAction = new physicalgraph.device.HubAction(method: method, path: Path, headers: headers)
        sendHubCommand(actualAction)        
    } catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def updateGPIOState() { 
	log.debug "Updating GPIO map"
	
    executeRequest("/*", "GET")
    if (state.cycleCount != 10 && state.cycleCount != -1) {
    	log.debug "Door in Open/Close cycle.  Cycle count is ${state.cycleCount}"
    	runIn(5, updateGPIOState)
    }
    
    // Always reschedule the update check
    runEvery5Minutes(updateGPIOState)
}

/* Helper functions to get the network device ID */
private String NetworkDeviceId(){
	log.debug "Getting Network Device ID"
    def iphex = convertIPtoHex(settings.piIP).toUpperCase()
    def porthex = convertPortToHex(settings.piPort)
    return "$iphex:$porthex" 
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}