/**
 *  A generic LAN Service Manager
 *
 *  Author: Rusty Kroboth
 */

definition(
    name: "LAN Service Manager",
    namespace: "RustyKroboth",
    author: "Rusty Kroboth",
    description: "Manages a third party LAN-connected service, exposing that service's devices in the SmartThings mobile app",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=3x",
    singleInstance: true
)

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

preferences {
    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }
    section("LAN Service Manager endpoint") {
      input "LANServiceAddress", "text", title: "LAN Service Manager Address", description: "(ie. 192.168.1.17)", required: true, defaultValue: "192.168.1.17"
      input "LANServicePort", "text", title: "LAN Service Manager Port", description: "(ie. 10051)", required: true, defaultValue: "10051"
    }

    section("Enter a key string to use to authenticate communication between SmartThings hub and LAN Service Manager") {
      input "apiKey", "text", title: "API key string", description: "secretkey", required: false, defaultValue: "secretkey"
    }
}


def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(location, null, onHTTPRequest, [filterEvents:false])
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
    log.debug "Initializing"
    log.debug "SmartThings Hub API address is: " + getSmartThingsEndpoint();
    sendCommand([command:"register_endpoint", endpoint: getSmartThingsEndpoint()])
}

def uninstalled() {
    log.debug "Uninstalling"
    getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
    log.debug "Uninstall done"
}



def onHTTPRequest(evt) {
    def map = stringToMap(evt.stringValue)

    // in case we ever want to do something with the headers, here they are
//    def headers = getHttpHeaders(map.headers);
//    log.debug "API request headers: ${headers}";

    def body = getHttpBody(map.body);
    if (!body){
        return;
    }
    
    log.debug "Incoming API request body: ${body}";

    if (apiKey && body.key != apiKey){
        log.debug "Key in the incoming API request doesn't match the key set in the SmartApp";
        return;
    }
    
    if (!body.command){
        log.debug "No command provided by the incoming API request";
        return;
    }
    
    // TODO: add more commands as needed
    switch (body.command){
        case "updateDeviceList":
            updateDeviceList(body.data);
            break;
            
        case "processEvents":
            processEvents(body.data);
            break;
            
        default:
            log.debug "Unknown command ${body.command}";
        
    }
    
}

def processEvents(eventList){
    for (eventProperties in eventList){
        processEvent(eventProperties);
    }
}

def processEvent(eventProperties) {
	
    log.debug "processing event: ${eventProperties}";

    def device = getChildDevice(eventProperties.deviceId)
    if (!device){
        log.debug "device with id ${eventProperties.deviceId} does not exist (found ${device})";
        return;
    }

    eventProperties.remove('deviceId')
    device.handleEvent(eventProperties)
}

def updateDeviceList(deviceList) {

    def oldDeviceList = [];
    getAllChildDevices().each {
        oldDeviceList << it.deviceNetworkId
    }
    
    for (deviceDetails in deviceList){

        if (!deviceDetails){
            log.debug "createDevice command missing device details data";
            continue;
        }

        if (!deviceDetails.deviceId){
            log.debug "createDevice command missing deviceId";
            continue;
        }
        
        oldDeviceList = oldDeviceList - deviceDetails.deviceId;
        
        if (getChildDevice(deviceDetails.deviceId)){
            // log.debug "device with id ${deviceDetails.deviceId} already exists, skipping";
            continue;
        }

        if (!deviceDetails.typeName){
            log.debug "createDevice command missing typeName";
            continue;
        }
        if (!deviceDetails.name){
            log.debug "createDevice command missing name";
            continue;
        }
        
        log.debug "Going to create device: ${deviceDetails}";
       
        // addChildDevice(String namespace, String typeName, String deviceNetworkId, hubId, Map properties)
        def new_device = addChildDevice(
            "RustyKroboth",
            deviceDetails.typeName,
            deviceDetails.deviceId,
            hostHub.id,
            [
                "name": deviceDetails.name,
                label: deviceDetails.name,
                completedSetup: true
            ]
        )
        log.debug "addChildDevice() with id ${deviceDetails.deviceId}: ${new_device}"
    }
    
    // devices remaining that are not in the device list, thus need to be removed
    oldDeviceList.each {
        log.debug "Deleting device with id ${it}"
        deleteChildDevice(it)
    }
}


def getSmartThingsEndpoint() {
    return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

def getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

def getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

def sendCommand(cmdDetails) {

    if (
        settings.LANServiceAddress.length() == 0 ||
        settings.LANServicePort.length() == 0
    ) {
        log.error "LAN Service IP and Port endpoint not set!"
        return
    }

    def host = settings.LANServiceAddress + ":" + settings.LANServicePort;
    def headers = [:]
    headers.put("HOST", host)
    headers.put("Content-Type", "application/json")

    if (apiKey){
        cmdDetails.key = apiKey;
    }
    def json = new groovy.json.JsonBuilder(cmdDetails)
    
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/",
        headers: headers,
        body: json.toString()
    )
    sendHubCommand(hubAction)
}
