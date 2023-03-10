/**
 *  KODI Addon Activation
 *
 *  Copyright 2014 Richard
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
    name: "KODI Addon Activation",
    namespace: "",
    author: "Richard",
    description: "Activates KODI Addon such as ip camera stream",
    category: "My Apps",
    iconUrl: "http://s17.postimg.org/8gs31yr3z/Kodi.png",
    iconX2Url: "http://s17.postimg.org/8gs31yr3z/Kodi.png")


import groovy.json.JsonBuilder


preferences {
	section("Choose one or more, when..."){
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contactOpen", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "mySwitchOn", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
		input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
		input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
		input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
		input "locksLocked", "capability.lock", title: "Lock Locked?",  required: false, multiple:true
        input "locksUnlocked", "capability.lock", title: "Lock Unlock?",  required: false, multiple:true

        
	}

    section("Addon info"){
    	input "Command", "text", title: "Addon Name", required: false
	}
    
    section("KODI Notifications LivingRoom:") {
    input "xbmcserver", "text", title: "KODI IP", description: "IP Address", required: false
    input "xbmcport", "number", title: "KODI Port", description: "Port", required: false
    }
    
    section("KODI Notifications Bedroom:") {
    input "xbmcserver2", "text", title: "KODI IP", description: "IP Address", required: false
    input "xbmcport2", "number", title: "KODI Port", description: "Port", required: false
    } 
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
    initialize()

}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
    initialize()

}

def initialize() {
	subscribe(app, touch)
    subscribe(app, touch2)

}


def touch(evt) {
		def lanaddress = "${settings.xbmcserver}:${settings.xbmcport}"
        def deviceNetworkId = "1234"
        def json = new JsonBuilder()
        json.call("jsonrpc":"2.0","method":"Addons.ExecuteAddon","params":[addonid: "${settings.Command}"],"id":1)
        def xbmcmessage = "/jsonrpc?request="+json.toString()
        def result = new physicalgraph.device.HubAction("""GET $xbmcmessage HTTP/1.1\r\nHOST: $lanaddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
        sendHubCommand(result)

}

def touch2(evt) {
        def lanaddress = "${settings.xbmcserver2}:${settings.xbmcport2}"
        def deviceNetworkId = "1234"
        def json = new JsonBuilder()
        json.call("jsonrpc":"2.0","method":"Addons.ExecuteAddon","params":[addonid: "${settings.Command}"],"id":1)
        def xbmcmessage = "/jsonrpc?request="+json.toString()
        def result = new physicalgraph.device.HubAction("""GET $xbmcmessage HTTP/1.1\r\nHOST: $lanaddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
        sendHubCommand(result)

}


def subscribeToEvents() {
	subscribe(contactOpen, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitchOn, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
    subscribe(locksLocked, "lock.locked", eventHandler)
    subscribe(locksUnlocked, "lock.unlocked", eventHandler)
}

def eventHandler(evt) {
	sendmessage()
    sendmessage2()
}

def sendmessage() {
        def lanaddress = "${settings.xbmcserver}:${settings.xbmcport}"
        def deviceNetworkId = "1234"
        def json = new JsonBuilder()
        json.call("jsonrpc":"2.0","method":"Addons.ExecuteAddon","params":[addonid: "${settings.Command}"],"id":1)
        def xbmcmessage = "/jsonrpc?request="+json.toString()
        def result = new physicalgraph.device.HubAction("""GET $xbmcmessage HTTP/1.1\r\nHOST: $lanaddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
        sendHubCommand(result)
}

def sendmessage2() {
        def lanaddress = "${settings.xbmcserver2}:${settings.xbmcport2}"
        def deviceNetworkId = "1234"
        def json = new JsonBuilder()
        json.call("jsonrpc":"2.0","method":"Addons.ExecuteAddon","params":[addonid: "${settings.Command}"],"id":1)
        def xbmcmessage = "/jsonrpc?request="+json.toString()
        def result = new physicalgraph.device.HubAction("""GET $xbmcmessage HTTP/1.1\r\nHOST: $lanaddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
        sendHubCommand(result)
}

