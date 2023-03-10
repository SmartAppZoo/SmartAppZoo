/**
 *  Lutron Caseta Manager
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

 */
definition(
    name: "Lutron Caseta Manager",
    namespace: "kecorbin",
    author: "kecorbin",
    description: "Add Lutron Caseta Devices to SmartThings.",
    category: "Safety & Security",
    iconUrl: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg",
    iconX2Url: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg",
    iconX3Url: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg")


preferences {

  section("Bridge Setup"){
  	input "IP", "text", "title": "Caseta API Bridge IP", multiple: false, required: true
    input "port", "text", "title": "Caseta API Bridge Port", multiple: false, required: true
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }

    section("Device 1") {
		input "deviceName1", "text", title: "Device Name", required:false
        input "deviceType1", "enum", title: "Device Type", required: false, options: [
                "windowShade":"Window Shade"]
               
        input "deviceConfig1", "text", title: "Device ID", required: false
    }
    
    // add additional Devices for each Lutron you will use
 
    
}

def installed() {
  log.debug "Installed with settings: ${settings}"

  initialize()
}

def initialize() {

    subscribe(location, null, response, [filterEvents:false])
	setupShade(deviceName1, deviceType1, deviceConfig1);
	// setup additional sensors here
    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    // updateDeviceState();
    //unsubscribe();
    //updateShade(deviceName1, deviceType1, deviceConfig1);
    //subscribe(location, null, response, [filterEvents:false])
}

def updateShade(deviceName, deviceType, deviceConfig) {

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

        if(deviceType == "windowShade") { // Actions specific for the shade device type
            subscribe(theDevice, "windowShade", shadeChange)
            subscribe(theDevice, "level", shadeChange)
            subscribe(theDevice, "switch", shadeChange)
            log.debug "Setting initial state of $deviceName to off"
        }
    } else { // The shade does not exist
    	if(deviceName){ // The user filled in data about this switch
    		log.debug "This device does not exist, creating a new one now"
        	setupShade(deviceName, deviceType, deviceConfig);
       	}
    }

}

def setupShade(deviceName, deviceType, deviceConfig) {

	if(deviceName){
        log.debug deviceName
	    log.debug deviceType
        log.debug deviceConfig

        switch(deviceType) {
        	case "windowShade":
            	log.trace "Found a window shade called $deviceName on device id #$deviceConfig"
				def d = addChildDevice("kecorbin", "Lutron Shade", getRelayID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
	    		subscribe(d, "windowShade", shadeChange)
                subscribe(d, "level", shadeChange)
                subscribe(d, "switch.on", shadeOn)
                subscribe(d, "switch.off", shadeOff)
                subscribe(d, "switch", shadeChange)
            	break;
        }
	}
}


def shadeOn(evt) {
	log.debug "entering shadeOn() with " + evt
    executeShadeRequest("/shades/open", "POST", null)
    }

def shadeOff(evt) {
	log.debug "entering shadeOff() with " + evt
	executeShadeRequest("/shades/close", "POST", null)

	}
    
def shadeChange(evt){

	log.debug "shadeChange($evt.value) ";
    if (evt.value == 'closed') {
	    executeShadeRequest("/shades/close", "POST", null)
    } else if (evt.value == 'open') {
    	executeShadeRequest("/shades/open", "POST", null)
    } else {
    	executeShadeRequest("/shades/" + evt.value, "POST", null)
    }
    
}
	


// BELOW HERE IS FOR REFRESH/UPDATE
def response(evt){
 
 def msg = parseLanMessage(evt.description);
    log.debug "response(evt)"
    log.debug msg
    
    if(msg && msg.body){

    	// This is the device header state message
        def children = getChildDevices(false)
    	/**
        * should have a JSON objec tthat looks like this
            {
                "devices": {
                    "2": {
                        "value": 0
                    },
                    "3": {
                        "value": 0
                    },
                    "4": {
                        "value": 0
                    }
                }
            }
            ***/
        if(msg.json) {
        
           msg.json.devices.each { item ->
                updateShadeDevice(item.key, item.value.value, children);
            }

            log.trace "Finished Getting Caseta State"
        }


    }
}

/**
def updateShadeDevice(id, state, childDevices) {

  	def theShade = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$id") }
    if(theShade) {
    	log.debug "Updating shade $theShade for device $id with value $state"
        theShade.changeShadeState(state)
        theShade.setLevel(level)
        
    }
}
**/

/**
def updateDeviceState() {
	// This is where we poll webiopi for all the GPIO values
	log.trace "Getting device values from Lutron API"
	executeShadeRequest("/shades", "GET", null);
	// and we schedule it to run ever n seconds
    runIn(15, updateDeviceState);
}
**/



def uninstalled() {
  def delete = getChildDevices()
    delete.each {
    	unsubscribe(it)
    	log.trace "about to delete device"
        deleteChildDevice(it.deviceNetworkId)
    }
}



// HELPER FUNCTIONS

// request function
def executeShadeRequest(Path, method, device_id) {
	log.debug "Executing REST request"
    def headers = [:]
    headers.put("HOST", "$settings.IP:$settings.port")
    try {
        def resp = new physicalgraph.device.HubAction(
            method: method,
            path: Path,
            headers: headers)
			log.debug resp
        sendHubCommand(resp)
    }
    
    
    
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

/* Helper functions to get the network device ID */


def String getRelayID(deviceConfig) {

	return "lutronShade." + settings.IP + "." + deviceConfig
}

private String NetworkDeviceId(){
    def iphex = convertIPtoHex(settings.IP).toUpperCase()
    def porthex = convertPortToHex(settings.Port)
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