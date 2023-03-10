 /**
 *  Rainforest Eagle Manager - A service manager for the rainforest handler
 *
 *  Copyright 2017 Justin Walker
 *
 *  heavily adapted from wattvision manager
 *  https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/smartapps/smartthings/wattvision-manager.src/wattvision-manager.groovy
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
	name: "Rainforest Manager",
	namespace: "augoisms",
	author: "Justin Walker",
	description: "Monitor your whole-house energy use by connecting to your Rainforest Eagle",
	iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn@2x.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn@2x.png",
    singleInstance: true
)

preferences {
    section("Use the values printed on the bottom of your Eagle"){
        input name: "macId", type: "text", title: "MAC Address", required: true, displayDuringSetup: true, defaultValue: "0xd8d5b900000065b0"
    	input name: "cloudId", type: "text", title: "Cloud ID", required: true, displayDuringSetup: true
    }
    section("Eagle must have a static IP, default port is 80"){
        input name: "theAddr", type: "string", title: "ip:port", multiple: false, required: true, displayDuringSetup: true
    }
    section() {
    	input name: "theHub", type: "hub", title: "Hub", multiple: false, required: true
    }
}

def installed() {
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}


def initialize() {
	createChild()
}

// ========================================================
// SmartThings initiated setup
// ========================================================

def createChild() {
	log.trace "creating RainforestEagle child device"

    
    def parts = theAddr.split(":")
    def iphex = convertIPtoHex(parts[0])
    def porthex = convertPortToHex(parts[1])
    def dni = "$iphex:$porthex"
    log.debug "dni: $dni"
    // save dni for later use
    state.dni = dni
    
    def child = getChildDevice(dni)
    
    if (child) {
		log.warn "Device already exists"
        schedule(child)
	} else {
        try {
        	log.debug "attempting to create child device"
        	def newChild = addChildDevice("augoisms", "RainforestEagle", dni, theHub.id, [name:"RainforestEagle", label: "Power Meter"])
            log.trace "created ${newChild.displayName} with id $dni"
            schedule(newChild)
        }
        catch (Exception e) {
        	log.debug "error creating child device"
        	log.trace e
        }
    	
	}
}

def schedule(child) {
	// get data right away
	child.refresh()
    // schedule to refresh
    schedule("* /1 * * * ?", "refreshCallback") // every 1 minute
}

def refreshCallback() {
	log.debug "refreshCallback()"
	def child = getChildDevice(state.dni)
    child.refresh()
    
    // run in 30 seconds
    def now = new Date()
	def runTime = new Date(now.getTime() + (30 * 1000))
	runOnce(runTime, refreshCallbackNoReschedule)
}

def refreshCallbackNoReschedule() {
	log.debug "refreshCallbackNoReschedule()"
	def child = getChildDevice(state.dni)
    child.refresh()
}

// ========================================================
// public methods for child device to call
// ========================================================

public getSettings(child) {
	//log.debug "getSettings()"
    return settings
}

// ========================================================
// private helper methods
// ========================================================

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize(".").collect {  String.format("%02X", it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format("%04X", port.toInteger() )
    log.debug hexport
    return hexport
}
