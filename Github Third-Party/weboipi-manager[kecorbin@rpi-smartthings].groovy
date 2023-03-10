/**
 *  WebOIPi Manager
 *
 *  Copyright 2016 Kevin Corbin
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
    name: "WebOIPi Manager",
    namespace: "kecorbin",
    author: "kecorbin",
    description: "Add each Pi GPIO as an individual thing.",
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
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig1", "text", title: "GPIO# or Device Name", required: false
    }
    
    // add additional Devices for each GPIO you will use
    
    section("Device 2") {
		input "deviceName2", "text", title: "Device Name", required:false
        input "deviceType2", "enum", title: "Device Type", required: false, options: [
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig2", "text", title: "GPIO# or Device Name", required: false
    }

    section("Device 3") {
		input "deviceName3", "text", title: "Device Name", required:false
        input "deviceType3", "enum", title: "Device Type", required: false, options: [
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig3", "text", title: "GPIO# or Device Name", required: false
    }
    
    section("Device 4") {
		input "deviceName4", "text", title: "Device Name", required:false
        input "deviceType4", "enum", title: "Device Type", required: false, options: [
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig4", "text", title: "GPIO# or Device Name", required: false
    }

    section("Device 5") {
		input "deviceName5", "text", title: "Device Name", required:false
        input "deviceType5", "enum", title: "Device Type", required: false, options: [
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig5", "text", title: "GPIO# or Device Name", required: false
    }

    section("Device 6") {
		input "deviceName6", "text", title: "Device Name", required:false
        input "deviceType6", "enum", title: "Device Type", required: false, options: [
                "motion":"Motion Sensor",
                "contact":"Contact Sensor"]
        input "deviceConfig6", "text", title: "GPIO# or Device Name", required: false
    }

    
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])

	setupSensor(deviceName1, deviceType1, deviceConfig1);
	// setup additional sensors here
    setupSensor(deviceName2, deviceType2, deviceConfig2);
    setupSensor(deviceName3, deviceType3, deviceConfig3);
    setupSensor(deviceName4, deviceType4, deviceConfig4);
    setupSensor(deviceName5, deviceType5, deviceConfig5);
    setupSensor(deviceName6, deviceType6, deviceConfig6);
    
}

def updated() {
	log.debug "Updated with settings: ${settings}"

    updateGPIOState();
    unsubscribe();

    updateSensor(deviceName1, deviceType1, deviceConfig1);
    updateSensor(deviceName2, deviceType2, deviceConfig2);
    updateSensor(deviceName3, deviceType3, deviceConfig3);
    updateSensor(deviceName4, deviceType4, deviceConfig4);
    updateSensor(deviceName5, deviceType5, deviceConfig5);
    updateSensor(deviceName6, deviceType6, deviceConfig6);


    subscribe(location, null, response, [filterEvents:false])
}

def updateSensor(deviceName, deviceType, deviceConfig) {

    // If user didn't fill this device out, skip it
    if(!deviceName) return;

    def theDeviceNetworkId = "";
    theDeviceNetworkId = getRelayID(deviceConfig);           
    log.trace "Searching for: $theDeviceNetworkId";

  	def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }

    if(theDevice){ // The switch already exists
    	log.debug "Found existing device which we will now update"
        theDevice.deviceNetworkId = theDeviceNetworkId + "." + deviceConfig
        theDevice.label = deviceName
        theDevice.name = deviceName

        if(deviceType == "motion") { // Actions specific for the motion device type
            subscribe(theDevice, "motion", motionSensorChange)
            log.debug "Setting initial state of $deviceName to off"
            
        } else if(deviceType == "contact") { // Actions specific to contact device type
        	subscribe(theDevice, "contact", contactSensorChange)
            log.debug "setting initial state of $deviceName to open"
        }

    } else { // The switch does not exist
    	if(deviceName){ // The user filled in data about this switch
    		log.debug "This device does not exist, creating a new one now"
        	setupSensor(deviceName, deviceType, deviceConfig);
       	}
    }

}
def setupSensor(deviceName, deviceType, deviceConfig) {

	if(deviceName){
        log.debug deviceName
	    log.debug deviceType
        log.debug deviceConfig

        switch(deviceType) {
        	case "motion":
            	log.trace "Found a motion sensor called $deviceName on GPIO #$deviceConfig"
				def d = addChildDevice("smartthings", "Pi Motion Sensor", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
	    		subscribe(d, "motion", motionSensorChange)
            	break;

			case "contact":
            	log.trace "Found a contact sensor called $deviceName on GPIO #$deviceConfig"
				def d = addChildDevice("smartthings", "Pi Contact Sensor", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
	    		subscribe(d, "contact", contactSensorChange)
            	break;

	
        }
	}
}

def String getRelayID(deviceConfig) {

	return "piMotion." + settings.piIP + "." + deviceConfig
}

def uninstalled() {
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

    	// This is the GPIO header state message
        def children = getChildDevices(false)
    	if(msg.json) {
           msg.json.GPIO.each { item ->
                updateSensorDevice(item.key, item.value.value, children);
            }

            log.trace "Finished Getting GPIO State"
        }


    }
}

def updateSensorDevice(GPIO, state, childDevices) {

  	def theSensor = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$GPIO") }
    if(theSensor) {
    	log.debug "Updating sensor $theSensor for GPIO $GPIO with value $state"
        theSensor.changeSensorState(state)
    }
}


def updateGPIOState() {
	// This is where we poll webiopi for all the GPIO values
	log.trace "Getting GPIO values from RPI"
	executeRequest("/*", "GET", false, null);
	// and we schedule it to run ever n seconds
    runIn(2, updateGPIOState);
}

def motionSensorChange(evt){

	log.debug "Motion Sensor event - now " + evt.value;
    if(evt.value == "active" || evt.value == "inactive") return;
}
	
def contactSensorChange(evt){

	log.debug "Contact Sensor event - now " + evt.value;
    if(evt.value == "open" || evt.value == "closed") return;

}


// HELPER FUNCTIONS

// request function
def executeRequest(Path, method, setGPIODirection, gpioPin) {

    def headers = [:]
    headers.put("HOST", "$settings.piIP:$settings.piPort")

    try {

        if(setGPIODirection) {
   			log.debug "Setting GPIO pin mode to input"
        	def setDirection = new physicalgraph.device.HubAction(
            	method: "POST",
            	path: "/GPIO/" + gpioPin  + "/function/IN",
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