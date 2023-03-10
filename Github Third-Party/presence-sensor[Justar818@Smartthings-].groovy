/**
 *  Improved Mobile Presence
 *
 *  Copyright 2018 John Callahan
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
    name: "Presence Sensor",
    namespace: "johndc7",
    author: "John Callahan",
    description: "Improved Presence Sensor integration for SmartThings",
    category: "Convenience",
    iconUrl: "https://st.callahtech.com/icons/icon.png",
    iconX2Url: "https://st.callahtech.com/icons/icon@2x.png",
    iconX3Url: "https://st.callahtech.com/icons/icon@2x.png",
    oauth: [displayName: "Presence Sensor", displayLink: "https://st.callahtech.com"])


preferences {
  page(name: "default", title: "Presence Sensor", uninstall: true, install: true) {
    section("Create a device") {
      href(name: "newDeviceHref",
        title: "New Device",
        page: "createPage",
        description: "Create new device")
    }
    section("Pair an existing device") {
      href(name: "repairDeviceHref",
        title: "Existing Device",
        page: "repairPage",
        description: "Pair an existing device (For Presence Sensor app reinstalls, etc.)")
    }
    section("Other settings") {
      input "notify", "bool", title: "Notifications"
    }
  }
  page(name: "createPage")
  page(name: "repairPage")
}

def createPage() {
  String deviceId = getDeviceId().toString();
  dynamicPage(name: "createPage", install: false) {
  	section("Instructions"){
  	  paragraph "To create a new device, enter a name for the device below and choose one of the options to pair the Presence Sensor app."
    }
    section("Name") {
      input "deviceName", "text", title: "Device name", required: false, submitOnChange: true
    }
    if(deviceName){
    section("Pair Presence Sensor app") {
      paragraph "New device id: ${deviceId}"
      href(name: "pairCurrent",
        title: "Pair another device",
        url: "https://st.callahtech.com/pair?stId=${location.id}&id=${deviceId}&name=${deviceName}",
        description: "Pair another device")
      href(name: "newDevice",
        title: "Pair this device",
        style: "external",
        url: "https://st.callahtech.com/pair?stId=${location.id}&id=${deviceId}&name=${deviceName}&current=true",
        description: "Pair this device")
    }
    }
  }
}

def repairPage() {
	dynamicPage(name: "repairPage", install: false) {
  	section("Instructions"){
  	  paragraph "Select an existing device, and choose one of the options to pair the Presence Sensor app."
    }
    section("Device") {
      input "repairDevice", "capability.presenceSensor", multiple: false, required: false, submitOnChange: true
    }
    if(repairDevice){
    section("Pair Presence Sensor app") {
      href(name: "repairCurrent",
        title: "Pair different device",
        url: "https://st.callahtech.com/pair?stId=${location.id}&id=${repairDevice.getDeviceNetworkId()}",
        description: "Pair another device")
      href(name: "repairOther",
        title: "Pair this device",
        style: "external",
        url: "https://st.callahtech.com/pair?stId=${location.id}&id=${repairDevice.getDeviceNetworkId()}&current=true",
        description: "Pair this device")
    }
    }
  }
}

mappings {
  path("/update") {
    action: [
      POST: "updatePresence"
    ]
  }
  path("/devices") {
  	action: [
    	GET: "listDevices"
    ]
  }
}

def setToken(){
	if(!state.accessToken) {
    	createAccessToken()
	}
	try {
    	httpPost([
        	uri: "https://st.callahtech.com",
    		path: "/updateLocation",
            body: [
            	access_token: state.accessToken,
                id: location.getId(),
                token_type: "bearer",
                scope: "app",
                uri: "${getApiServerUrl()}/api/smartapps/installations/${app.id}",
                name: location.getName()
            ]
        ]) { resp ->
        	//log.debug(resp.data);
        	return resp.data;	    	
    	}
	} catch (e) {
	    log.error "Could not set location auth: $e"
	}
}

def getDeviceId(){
	try {
    	httpGet([
        	uri: "https://st.callahtech.com",
    		path: "/newid"
        ]) { resp ->
        	log.debug(resp.data);
        	return resp.data;	    	
    	}
	} catch (e) {
	    log.error "Could not get new ID: $e"
	}
}

def listDevices() {
	def resp = []
    getChildDevices().each {
      resp << [id: it.getDeviceNetworkId(), name: it.displayName]
    }
    return resp
}

def updatePresence() {
    def body = request.JSON;
    log.debug("Received push from server");
    log.debug(body);
    if(body == null || body.toString() == "{}"){
    	getChildDevices().checkPresence();
        log.error("No JSON data received. Requesting update of ${getChildDevices().size()} device(s) at location.");
    	return [error:true,type:"No Data",message:"No JSON data received. Requesting update of ${getChildDevices().size()} device(s) at location."];
	}
    for(int i = 0; i < getChildDevices().size(); i++)
    	if(getChildDevices().get(i).getDeviceNetworkId() == body.id){
        	getChildDevices().get(i).setPresence(body.present,body.location);
            if(body.battery && body.charging != null)
            	getChildDevices().get(i).setBattery(body.battery,body.charging);
            log.debug("Updating: ${body.id}");
            return [error:false,type:"Device updated",message:"Sucessfully updated device: ${body.id}"];
        }
    log.debug("Creating new device ${body.name} with an id of: ${body.id}");
    addChildDevice("Improved Mobile Presence", body.id, null, [name: body.name ? body.name : "Presence Sensor"]);
    return [error:false, type:"Device created", message:"Created new device ${body.name} with an id of: ${body.id}"];
    //return [error:true,type:"Invalid ID",message:"No device with an id of ${body.id} could be found. Requesting update of ${getChildDevices().size()} device(s) at location."];
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
	setToken();
	log.debug("Subscribing to events");
    subscribe(getChildDevices(), "presence", presenceNotifier);
}

def presenceNotifier(evt) {
	log.debug "Event: " + evt.descriptionText;
    if(notify)
    	sendNotification(evt.descriptionText)
}
