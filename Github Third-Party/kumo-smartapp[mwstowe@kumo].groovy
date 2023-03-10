/**
 *  Kumo SmartApp
 *
 *  Copyright 2019 Michael Stowe
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
    name: "Kumo SmartApp",
    namespace: "mwstowe",
    singleInstance: true,
    author: "Michael Stowe",
    description: "Connects to kumojs for control of Mitsubishi units\r\n\r\nhttps://github.com/sushilks/kumojs",
    category: "Convenience",
    iconUrl: "https://www.mitsubishicomfort.com/sites/default/themes/mitsubishicomfort/_assets/images/apps/kumo-icons/third-party-equipment.png",
    iconX2Url: "https://www.mitsubishicomfort.com/sites/default/themes/mitsubishicomfort/_assets/images/apps/kumo-icons/third-party-equipment.png",
    iconX3Url: "https://www.mitsubishicomfort.com/sites/default/themes/mitsubishicomfort/_assets/images/apps/kumo-icons/third-party-equipment.png")


preferences {
	page(name: "prefWelcome")
    page(name: "prefKumoValidate")
		// TODO: put inputs here
	}


/***********************************************************************/
/*                        INSTALLATION UI PAGES                        */
/***********************************************************************/
def prefWelcome() {

    dynamicPage(name: "prefWelcome", title: "Kumo Integration", uninstall: true, nextPage: "prefKumoValidate") {

        section("Local Kumojs Server IP") {
            input("kumojsServerIp", "text", title: "Enter the IP of the running kumojs instance", required: true, defaultValue: "192.168.0.")
        }

        section("Local Kumojs Server Port"){
            input("kumojsServerPort", "text", title: "Enter the Port of the running kumojs instance", required: true, defaultValue: "8084")
        }
    }
}

def prefKumoValidate() {

    atomicState.security = [:]

	if (getKumoRooms()) {
        dynamicPage(name: "prefKumoValidate", title: "Kumojs Integration Successful", install: true) {
            section(){
                paragraph "Congratulations! You have successfully connected to kumojs."
                }
        }
    } else {
	    dynamicPage(name: "prefKumoValidate",  title: "Kumojs Integration Error") {
            section(){
                paragraph "Sorry, your local server does not seem to respond at ${settings.kumojsServerIp}:${settings.kumojsServerPort}."
            }
        }
    }
}

private getKumoRooms() {

   def uri = "http://${settings.kumojsServerIp}:${settings.kumojsServerPort}/v0/rooms"

   log.debug "${uri}"

   httpGet(uri) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "Webserver failed, status = ${response.status}"
            return false
        }
        log.info response.data
        def data = response.data.toString()
	    def pData = new groovy.json.JsonSlurper().parseText(data)
                
        pData.each {
           addDevices(it)
           }
               
    }
    
    return true
}

def addDevices(def room) {
    
    def device = URLEncoder.encode(room, "UTF-8").replace("+", "%20")
       
    log.info device
    // see if the specified device exists and create it if it does not exist
    def deviceDNI = 'room/' + room;
    def devicecheck = getChildDevice(deviceDNI)
    if (!devicecheck) {

       log.info "Adding new device: " + "room ID: " + deviceDNI

       //build the device type
       def deviceHandler = "Kumojs";

       if (deviceHandler) {

        	log.info "Looking for device handler: " + deviceHandler

        	// we have a valid device type, create it
            try {
        		device = addChildDevice("mwstowe", deviceHandler, deviceDNI, null, [label: deviceName])
        		device.sendEvent(name: 'id', value: deviceId);
        		device.sendEvent(name: 'type', value: deviceType);
            } catch(e) {
            	log.info "Kumojs SmartApp cannot add Device. Please find and install the [${deviceHandler}] device handler from https://github.com/aromka/MyQController/tree/master/devicetypes/aromka"
            }
        }

    } else {
        log.info "Device already exists. ID: " + deviceDNI
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

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}



// TODO: implement event handlers
def get_status(device) {
   def deviceURI = URLEncoder.encode(device.getDeviceNetworkId(), "UTF-8").replace("+", "%20").replace("%2F", "/")

   def uri = "http://${settings.kumojsServerIp}:${settings.kumojsServerPort}/v0/" + deviceURI + "/status";

   log.debug "${uri}"

   httpGet(uri) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "Webserver failed, status = ${response.status}"
            return false
        }
        log.info response.data    
        return response.data
    }
    
}

def set_mode(device, mode) {
   def deviceURI = URLEncoder.encode(device.getDeviceNetworkId(), "UTF-8").replace("+", "%20").replace("%2F", "/")
   
   def callURI = "http://${settings.kumojsServerIp}:${settings.kumojsServerPort}/v0/" + deviceURI + "/mode/"
   def callPath = mode
   
   
    def params = [
        uri:  callURI,
        path: callPath,
        contentType: 'application/json'
    ]
   
        httpPut(params) {response ->
        if (response.status != 200 ) {
            log.debug "Webserver failed, status = ${response.status}"
            return false
        }
        log.info response.data    
        return response.data
    }
}

def setFanSpeed(device, mode) {
   def deviceURI = URLEncoder.encode(device.getDeviceNetworkId(), "UTF-8").replace("+", "%20").replace("%2F", "/")
   
   def callURI = "http://${settings.kumojsServerIp}:${settings.kumojsServerPort}/v0/" + deviceURI + "/speed/"
   def callPath = mode
   
   
    def params = [
        uri:  callURI,
        path: callPath,
        contentType: 'application/json'
    ]
   
        httpPut(params) {response ->
        if (response.status != 200 ) {
            log.debug "Webserver failed, status = ${response.status}"
            return false
        }
        log.info response.data    
        return response.data
    }
}