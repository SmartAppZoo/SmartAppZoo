/**
 *  Close Garage After Sunset
 *
 *  Copyright 2015 Sean Mooney
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
    name: "Kodi Notification",
    namespace: "srmooney",
    author: "Sean Mooney",
    description: "Sends status of SmartThings Devices to kodi",
    category: "",
    iconUrl: "http://s17.postimg.org/8gs31yr3z/Kodi.png",
    iconX2Url: "http://s17.postimg.org/8gs31yr3z/Kodi.png")

import groovy.json.JsonBuilder


preferences {
	page(name: "page1", title: "", nextPage: "page2", uninstall: true, install:false) {

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
    
        section("Run when SmartThings is set to") {
            input "setMode", "mode", title: "Mode?",  required: false, multiple:true
        }

    	section("Number of Kodi Servers") {
        	/* TODO: Put in validation of this number */
            input "serverCount", "number", title: "",  required: true
            //input "serverCount", "enum", title:"", required:true, options: ["1","2","3","4","5"]
        }
    }
    page(name: "page2", title: "", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
        section("Kodi Servers") {
            //def serverCount = 4
            paragraph "Enter up to $serverCount server IP Address port combinations."
            log.info(serverCount)
            for(int i = 1; i <= serverCount; i++) {
                input "server$i", "text", title: "Server $i", description: "192.168.0.XXX:8080", required: (i==0)
            }
        }

        section("Icon"){
            paragraph title:"", image: getDefaultIcon(), required: false, "Provide the url of an icon to override the default seen on the left."
            input "iconImage", "text", title: "Icon Override", required: false
        }

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
	subscribeToEvents()
}

def subscribeToEvents(){
	subscribe(contactOpen, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
    subscribe(Noacceleration, "acceleration.inactive", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
    subscribe(Nomotion, "motion.inactive", eventHandler)
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
    subscribe(location, modeChangeHandler)
	subscribe(app, touchHandler)
}

def touchHandler(evt){
    def img = getIcon()
    def json = new JsonBuilder()
    json.call("jsonrpc":"2.0","method":"GUI.ShowNotification","params":[title: "Smartthings",message: "Hello",image: "${img}"],"id":1)
    def xbmcmessage = "/jsonrpc?request="+json.toString()
    notifyKODI(xbmcmessage)
}

def eventHandler(evt) {
    def name = evt.displayName.replaceAll(' ', '%20')
    def value = evt.value
    def img = getIcon()
    def json = new JsonBuilder()
    json.call("jsonrpc":"2.0","method":"GUI.ShowNotification","params":[title: "${name}",message: "${value}",image: "${img}"],"id":1)
    def xbmcmessage = "/jsonrpc?request="+json.toString()
    notifyKODI(xbmcmessage)
}

def modeChangeHandler(evt) {
	def mode = evt.value
    def msg = "Mode%20Activated"
    def img = getIcon()
    def json = new JsonBuilder()
    json.call("jsonrpc":"2.0","method":"GUI.ShowNotification","params":[title: "${mode}",message: "${msg}",image: "${img}"],"id":1)
    def xbmcmessage = "/jsonrpc?request="+json.toString()
    notifyKODI(xbmcmessage)
}

def notifyKODI(msg){
	def servers = settings.findAll{ it.key.startsWith("server") }
    for (server in servers) {
        def serverAddress = server.value.toString()
        //log.info "serverAddress: $serverAddress"
        if (serverAddress.count(".") == 3 && serverAddress.count(":") == 1){
    		def result = new physicalgraph.device.HubAction("""GET $msg HTTP/1.1\r\nHOST: $serverAddress\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}")
    		sendHubCommand(result)
        }
        else {
        	log.info "Sever address not valid - $server"
        }
    }	
}

def getDefaultIcon(){
	return "http://static.squarespace.com/static/5129591ae4b0fd698ebf65c0/51384d05e4b079b8225cdf28/51384d06e4b000a8acbd05b5/1362644479952/smartthings.png"
}

def getIcon(){
    if (iconImage?.trim()){
    	return iconImage
    }
    return getDefaultIcon()
}
