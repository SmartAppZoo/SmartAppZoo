/**
 *  GE Wireless Sensors
 *
 *  Copyright 2018 Carolyn Wales 
 *
 *  With thanks to Victor Santana, for figuring out how to get events from a raspberry pi to a smartThings hub.
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
 * General Idea:
 * We create a SmartApp that works in the background to mediate between the SmartThings framework and the RPi. Said 
 * SmartApp (this code) is responsible for creating (and deleting) sensor objects in the SmartThings Framework, for
 * listening for events from the RPi, and for forwarding those events to the appropriate sensor object.
 *
 * Events are carried as HTTP NOTIFY messages per SSDP. Note that these messages are sent over UDP and are not 
 * guaranteed to be delivered - but that's the only external listening infrastructure that the SmartThings framework provides.
 *
 */
 
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
 
definition(
    name: "GE Wireless Sensors",
    namespace: "medeasoftware",
    author: "Carolyn Wales",
    description: "Connect SmartThings to GE Wireless sensors",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

// Settings display page
preferences {
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
    }
    section("SmartThings RPi") {
      input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.4.10)", required: true, defaultValue: "192.168.4.15"
      input "proxyPort",    "text", title: "Proxy Port",    description: "(ie. 3001)",         required: true, defaultValue: "3001"
    }
    
/*    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }

*/
}

// Called by framework when the app is first installed
def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

// Called by framework when the app preferences change
def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    // TODO - use SSDP to find our little raspberry pi thing.
    // *then* send subscription request and request for device list
    // *then* subscribe to 
    // TODO: subscribe to attributes, devices, locations, etc.
    // Apparently we're supposed to use mDNS or SSDP to find our little raspberry pi thingy
    // Not sure how we send that information to/from the device handler(s)
    // This is supposed to discover devices as well (inclusion)
    // Appears that there may be two smartapps? A service manager one, and a standard one? Or maybe they're one in the same?
    
    // It appears that hubactions may be synchronous. Or not.?
    log.debug "Initialize called"
    
    sendSubscriptionRequest()
    sendCommandToRPi("GET", "/devices", null, "deviceCallback")
    subscribe(location, null, eventHandler, [filterEvents:false])
    //subscribe(theswitch, "switch.on", dummyDevicesCallback)
    //subscribe(theswitch, "switch.off", dummyEventCallback)
    //subscribe(location, null, lanResponseHandler, [filterEvents:false]) XXX - how to filter events?
 }
 
 def dummyDevicesCallback(evt){
    log.debug("Dummy Devices Callback")
    def deviceStr = '{ "sensors": [{"id":"ABCD", "name":"Door Sensor",   "type":"dw",     "triggered":true, "tampered":false, "low_battery":true, "online":true, "snr":14.4},\
                                   {"id":"DEFG", "name":"Motion Sensor", "type":"motion", "triggered":true, "tampered":false, "low_battery":true, "online":true, "snr":18.4}] }'
 
    parseDevicesList(new JsonSlurper().parseText(deviceStr))
 }
 
 def dummyEventCallback(evt) {
    log.debug("Dummy Event Callback")
    def eventStr = '{"id":"ABCD", "triggered":false, "tampered":false, "low_battery":false, "online":true, "snr":15.7}'
    sendEventToSensors(eventStr)
 }
 
 
 def sendSubscriptionRequest() {
    if (location.hubs == null ||
        location.hubs[0] == null) {
        log.warn("Hub not available")
    }
 
    def hub = location.hubs[0]
    def subscriptionBody = [:]
    subscriptionBody["ipaddr"] = hub.getLocalIP()
    subscriptionBody["port"]   = hub.getLocalSrvPortTCP()
    subscriptionBody["id"]     = hub.getId()
    log.debug("${hub.getLocalIP()}, ${hub.getLocalSrvPortTCP()}")
    sendCommandToRPi("POST", "/subscribe", subscriptionBody, "subscriptionCallback")
 }
 
 def sendCommandToRPi(String method, String path, Map body, String callbackName)
 {
    log.debug("Sending command ${path} toRPI")
    if (settings.proxyAddress.length() == 0 || settings.proxyPort.length() == 0) {
        log.debug("RPI address and port unknown, cannot send command ${method} ${path}")
        return
    }
    def options = [:]
    def params = [:]
    def headers = [:]
    headers["HOST"] = settings.proxyAddress + ":" + settings.proxyPort
    options["callback"] = callbackName
    params["path"] = path
    params["method"] = method
    params["body"] = body
    params["headers"] = headers
    
    def hubAction = new physicalgraph.device.HubAction(
        params,
        null,
        options
    )
     
    sendHubCommand(hubAction)
}

// nb - subscription response doesn't actually do much. Check response code.
def subscriptionCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug("Entered subscriptionCallback...")
    def body = hubResponse.json
    def status = hubResponse.status
    log.debug("  Callback response is ${status}") 
}

// Device description:
// { "sensors": [{"id":<id>, "name":<name>, "type":<dw/motion/etc>, "triggered":<true/false>, "tampered":<true/false>, "low_battery":<true/false>, "online":<true/false>, "snr":<number>}] }

// Event description. Not all of 'triggered/tampered/low_battery/online need be present.
// {"id":<id>, "triggered":<t/f>, "tampered":<t/f>, "low_battery":<t/f>, "online":<t/f>, "snr":<number>}

// Could also add a sensor add, but let's hold off for the moment.

def deviceCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug("Entered deviceCallback...")
    def body   = hubResponse.json
    def status = hubResponse.status
    log.debug("  Callback response is ${status}")
    log.debug("  body in calledBack is: ${body}")
    
    if (status != 200) {
        return
    }
    
    if (!body) {
        return
    }
      
    parseDevicesList(body)
}
      
def parseDevicesList(body) {

    log.debug("Attempting to parse devices list ${body}")
    log.debug("Location is ${location}")
    log.debug("hubs are ${location.hubs}")
    def hostHub = location.hubs[0]
    

    // parse through devices, adding any that we don't already have. (And subtracting those that we no longer have)
    def sensorMap = [:]
    body.sensors.each { sensor ->
        def globalId = "GE_Sensor_" + sensor.id
        if (!getChildDevice(globalId)) {
            if (sensor.type == "dw") {
                log.debug ("Adding child device doorwindow sensor, id ${globalId}")
                addChildDevice("medeasoftware", "GE Wireless Door/Window Sensor", globalId, hostHub.id,
                    [name:sensor.name, contact:sensor.triggered?"closed":"open", tamper:sensor.tampered?"detected":"clear", low_battery:sensor.low_battery, supervisory:sensor.supervisory] ) 
            } else if (sensor.type == "motion") {
                log.debug ("Adding child device motion sensor, id ${globalId}")
                addChildDevice("medeasoftware", "GE Wireless Motion Detector", globalId, hostHub.id,
                    [name:sensor.name, motion:sensor.triggered?"active":"inactive", tamper:sensor.tampered?"detected":"clear", low_battery:sensor.low_battery, supervisory:sensor.supervisory] )
            }
        }
        sensorMap[globalId] = sensor
    }
    
    // and delete any that don't appear in the list. 
    getChildDevices()?.each { device ->
        def id = device.getDeviceNetworkId()
        if (sensorMap[id] == null) {
            log.debug("Sensor ${id} not found in current sensor list, deleting device")
            deleteChildDevice(id)
        }
    }
    /*
    def deviceId = 'Fireplace'
    if (!getChildDevice(deviceId)) {
        addChildDevice("Fireplace", "Fireplace Switch", deviceId, hostHub.id, [name: "Fireplace", label: "Fireplace", completedSetup: true])
      }
*/
    
}


private def getHttpHeaders(headers) {
    if (!headers) {
        return null
    }
    def obj = [:]
    new String(headers.decodeBase64()).split("\r\n").each {param ->
        def nameAndValue = param.split(":")
        obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
    }
    return obj
}

/* 
 * Handle event coming over SSDP. Note that we're not filtering - this code gets called
 * for any events, even those that are not associated with our sensors
 */
def eventHandler(evt) {
    log.debug("Received event")
    log.debug("Event is " + evt.stringValue)
    def map = stringToMap(evt.stringValue)
    def headers = getHttpHeaders(map.headers);
    
    if (headers?.device == "RPI Wireless Sensors") {
        log.debug("Received event for RPI Wireless Sensors")
        def eventBytes = map.body.decodeBase64();
        def eventText = new String(eventBytes, "UTF-8")
        sendEventToSensors(eventText)
    }
}
        
        
def sendEventToSensors(eventText) {
    def obj = new JsonSlurper().parseText(eventText);
    def deviceId = "GE_Sensor_" + obj.id
    def sensors = getAllChildDevices()
    def sensor = null
    sensors.find {
        if (it.getDeviceNetworkId() == deviceId) {
            sensor = it;
            return true
        }
        return false
    }

    if (sensor) {
        log.debug(" Event is from known sensor ${deviceId}")
        log.debug(" Event data is ${eventText}")
        sensor.parse(eventText) // send event to appropriate device
    } else {
        log.debug(" Event is from unknown sensor ${deviceId}, should add?")
    }
}

