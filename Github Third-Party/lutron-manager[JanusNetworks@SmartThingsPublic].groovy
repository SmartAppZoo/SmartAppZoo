/**
 *  Lutron Manager
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
    name: "Lutron Manager",
    namespace: "JanusNetworks",
    author: "Kevin Corbin",
    description: "Add Lutron devices to Smartthings",
    category: "SmartThings Labs",
    iconUrl: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg",
    iconX2Url: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg",
    iconX3Url: "http://a2.mzstatic.com/us/r30/Purple5/v4/1c/30/fc/1c30fc5d-2952-da5d-157d-8e954ebf9feb/icon175x175.jpeg")


preferences {

  section("Bridge Setup"){
  	input "IP", "text", "title": "JanusNetworks API Bridge IP", multiple: false, required: true
    input "port", "text", "title": "Caseta API Bridge Port", multiple: false, required: true
    input "theHub", "hub", title: "On which hub?", multiple: false, required: true
  }

	def deviceTypeOptions = ["rollershade": "Roller Shade",
    						 "dimmer": "Dimmer Module",
                             "pico": "A Pico Remote",
                             ]
                             
    section("Device 1") {
		input "deviceName1", "text", title: "Device Name", required:false
                input "deviceType1", "enum", title: "Device Type", required: false, options: deviceTypeOptions
        input "deviceConfig1", "text", title: "Device ID", required: false
    }
    section("Device 2") {
		input "deviceName2", "text", title: "Device Name", required:false
                input "deviceType2", "enum", title: "Device Type", required: false, options:deviceTypeOptions
        input "deviceConfig2", "text", title: "Device ID", required: false
    }
        section("Device 3") {
		input "deviceName3", "text", title: "Device Name", required:false
                input "deviceType3", "enum", title: "Device Type", required: false, options:deviceTypeOptions
        input "deviceConfig3", "text", title: "Device ID", required: false
    }
    section("Device 4") {
		input "deviceName4", "text", title: "Device Name", required:false
                input "deviceType4", "enum", title: "Device Type", required: false, options:deviceTypeOptions
        input "deviceConfig4", "text", title: "Device ID", required: false
    }
 	section("Device 5") {
		input "deviceName5", "text", title: "Device Name", required:false
                input "deviceType5", "enum", title: "Device Type", required: false, options:deviceTypeOptions
        input "deviceConfig5", "text", title: "Device ID", required: false
    }


    
    // add additional Devices for each Lutron you will use
 
    
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def initialize() {
    subscribe(location, null, response, [filterEvents:false])
	setupDevice(deviceName1, deviceType1, deviceConfig1);
    setupDevice(deviceName2, deviceType2, deviceConfig2);
    setupDevice(deviceName3, deviceType3, deviceConfig3);
	setupDevice(deviceName4, deviceType4, deviceConfig4);
    setupDevice(deviceName5, deviceType5, deviceConfig5);

	// setup additional devices here
}

def updated() {
	// called when changes are made to the smartapp configuration
    log.debug "Updated with settings: ${settings}"
	setupDevice(deviceName1, deviceType1, deviceConfig1);
    setupDevice(deviceName2, deviceType2, deviceConfig2);
    setupDevice(deviceName3, deviceType3, deviceConfig3);
	setupDevice(deviceName4, deviceType4, deviceConfig4);
    setupDevice(deviceName5, deviceType5, deviceConfig5);    
}

def setupDevice(deviceName, deviceType, deviceConfig) {
	log.debug "Lutron Manager setupDevice()"
	if(deviceName){
        log.debug deviceName
	    log.debug deviceType
        log.debug deviceConfig
		
        def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(GetDeviceNetworkID(deviceConfig)) }
        if(theDevice){ 
        // can't really update device if it's type changes, as this could break smartapps, etc - best to just delete/recreate it
        	log.debug "device already exists - updating" // The device already exists
            deleteChildDevice(GetDeviceNetworkID(deviceConfig))
        }
        
        switch(deviceType) {
        	case "pico":
            	log.trace "Adding a pico remote called $deviceName on device id #$deviceConfig"
				def d = addChildDevice("JanusNetworks", "Lutron Pico", GetDeviceNetworkID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
                // subscribe to events if necessary
         		// subscribe(d, <capability>, callbackmethod)
                // subscribe(d, "switch", switchChange)
				break;
            case "rollershade":
            	log.trace "Adding a rollershade called $deviceName on device id #$deviceConfig"
				def d = addChildDevice("JanusNetworks", "Lutron RollerShade", GetDeviceNetworkID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
                
            	break;

			case "dimmer":
            	log.trace "Adding a dimmer called $deviceName on device id #$deviceConfig"
				def d = addChildDevice("JanusNetworks", "Lutron Dimmer", GetDeviceNetworkID(deviceConfig), theHub.id, [label:deviceName, name:deviceName])
                
            	break;
        }
	}
}
    

def uninstalled() {
	// used when the service manager smartapp is uninstalled, deletes all children
    def delete = getChildDevices()
    delete.each {
    	log.trace "about to delete device"
        deleteChildDevice(it.deviceNetworkId)
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


// HELPER FUNCTIONS

// request function
def executeRequest(Path, method, device_id) {
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
def executeShadeRequest(Path, method, device_id) {
	log.debug "Executing REST request"
    def headers = [:]
    headers.put("HOST", getHostAddress())
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


def String GetDeviceNetworkID(deviceConfig) {
	// used to set the IP address on child devices so they know where to point REST calls to
	return settings.IP + ":" + settings.port + ":" + deviceConfig
}