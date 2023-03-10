/**
 *  Fritz.Box (Connect)
 *
 *  Copyright 2018 Marian Mitschke
 *
 *  Changelog:
 *  12 / 14 / 2018
 *	Fixed adding new devices
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
import java.security.MessageDigest
 
definition(
		name: "Fritz.Box (Connect)",
		namespace: "smartthings",
		author: "MarianMitschke",
		description: "Fritz.Box Connect",
		category: "SmartThings Labs",
		iconUrl: "https://en.avm.de/fileadmin/user_upload/EN/Press_photos/Logos/preview/AVM_Logo_FRITZ_RGB.png",
		iconX2Url: "https://en.avm.de/fileadmin/user_upload/EN/Press_photos/Logos/preview/AVM_Logo_FRITZ_RGB.png",
		iconX3Url: "https://en.avm.de/fileadmin/user_upload/EN/Press_photos/Logos/preview/AVM_Logo_FRITZ_RGB.png",
        singleInstance: true)


preferences {
	page(name: "searchTargetSelection", title: "Fritz.Box (Connect)", nextPage: "deviceDiscovery") {
		section("Connection Data") {
			input "fritzIp", "string", title: "Fritz.Box IP", defaultValue: "192.168.178.1", required: true
            input "fritzUser", "string", title: "Fritz.Box User", defaultValue: "Administrator", required: true
            input "fritzPassword", "password", title: "Fritz.Box PW", defaultValue: "", required: true
		}
	}
	page(name: "deviceDiscovery", title: "Fritz.Box Device Discovery", content: "deviceDiscovery")
}

def deviceDiscovery() {
    
	getDeviceList()
	def options = [:]
	def devices = getDevices()

	log.debug("deviceDiscovery called: devices = ${devices}")
    
    devices.each {
        def key = it.key
        def value = it.value
        
        options["${key}"] = value
	}

	return dynamicPage(name: "deviceDiscovery", title: "Discovery Started!", nextPage: "", refreshInterval: 5, install: true, uninstall: true) {
		section("Please wait while we discover your Fritz.Box devices. Discovery can take up to five minutes, so sit back and relax! Select your device below once discovered.") {
			input "selectedDevices", "enum", required: false, title: "Select Devices (${options.size() ?: 0} found)", multiple: true, options: options
		}
	}
}

def installed() {
	log.debug "Installed"
    initialize()
}

def updated() {
	log.debug "Updated"
	
    def childDevices = getChildDevices()
    
    if (childDevices)
    {
    	childDevices.each { dev -> 
        	if (!selectedDevices?.any { it.toString() == dev.deviceNetworkId.toString() }){
        		deleteChildDevice(dev.deviceNetworkId)
            }
        }
    }
    
    initialize()
}

def initialize() {
	unschedule()
	log.debug("init called")
    authenticate()

	if (selectedDevices) {
		addDevices()
	}

	runEvery5Minutes("getDeviceList")
}

def addDevices() {
	log.debug("addDevices() called")
	def devices = getDevices()

	selectedDevices.each { ain ->
    
		def selectedDevice = devices.find { it.key == ain }
		def d
        
		if (selectedDevice) {
        	d = getChildDevice(selectedDevice.key)
        }
        
        if (!d) {
			log.info "Adding Fritz.Box Device with AIN : ${selectedDevice.key}"
             
            addChildDevice("marianmitschke", "fritz-dect-200-connect", selectedDevice.key, null, [
				"label": "${selectedDevice.value}",
				"preferences": [
					"outletAIN": selectedDevice.key
				]
			])
		}
	}
}

// Callbacks

def deviceListCallbackHandler(response){
    if (checkResponse(response)){
    	def data = response.body
    	
    	if (data == null)
    		return
            
        def devices = getDevices()
    	    
        def devicelist = new XmlSlurper().parseText(data)
        
        for (int i = 0; i < devicelist.device.size(); i++)
        {
        	def ain = devicelist.device[i].@identifier.toString().replace(" ", "")
            def name = devicelist.device[i].name.toString()        
            def isPresent = devicelist.device[i].present.toString() == "1"
            def switchstate = devicelist.device[i].switch.state.toString() == "1" ? "on" : "off"
            def energy = devicelist.device[i].powermeter.energy.toString()
            def power = devicelist.device[i].powermeter.power.toString()
            def temp = devicelist.device[i].temperature.celsius.toString()
                        
            if (!devices.containsKey(ain)){
    			devices.put(ain, name)
    	    }
            
            def d = getChildDevice(ain)
            
            if (d)
            {
            	if (d.hasAttribute("switch"))
            	{
            		def actualswitchstate = d.currentState("switch")?.stringValue
            	    if (switchstate != actualswitchstate)
            	    {
            	    	d.generateStateSwitch(switchstate == "on")
            	    }
            	}
            	
            	d.generateRefreshEvent([energy: energy, power: power, temperature: temp])
            }
        }
	}
}

def switchCmdCallbackHandler(response){
	if (checkResponse(response)){
    	def body = response.body
        def switchstate = body.substring(0,1)
        
        log.debug("Callback of SwitchCommand - state of switch is: ${switchstate} (0=off,1=on)")
        
        def d = getChildDevices()?.find { it.deviceNetworkId == state.child }
        
        if (switchstate == "0")
    		d.generateStateSwitch(false)
        else
    		d.generateStateSwitch(true)
    }
}

def refreshDeviceCallbackHandler(response){
    if (checkResponse(response)){
        def d = getChildDevice(state.child)
    
    	def data = response.body
            
        def devicelist = new XmlSlurper().parseText(data)
        
        def foundDevice = devicelist.device.find { device -> device.@identifier.toString().replace(" ", "") == state.child }
        
        if (foundDevice)
        {
            log.debug("matching ain found")
            def switchstate = foundDevice.switch.state.toString() == "1" ? "on" : "off"
            def energy = foundDevice.powermeter.energy.toString()
            def power = foundDevice.powermeter.power.toString()
            def temp = foundDevice.temperature.celsius.toString()
            
            if (d.hasAttribute("switch"))
            {
            	def actualswitchstate = d.currentState("switch")?.stringValue
                if (switchstate != actualswitchstate)
                {
                	log.debug("triggering switch state - state was manually changed")
                	d.generateStateSwitch(switchstate == "on")
                }
            }
            
            d.generateRefreshEvent([energy: energy, power: power, temperature: temp])
        }
	}
}

// Switch Commands

def on(child){
    log.debug("child called main app (on)")
    
    sendSwitchCommand("setswitchon", child.ain(), switchCmdCallbackHandler)
    state.child = child.ain()
}

def off(child){
	log.debug("child called main app (off)")
    
    sendSwitchCommand("setswitchoff", child.ain(), switchCmdCallbackHandler)
    state.child = child.ain()
}

def refresh(child){
	log.debug("child called main app (refresh)")
    sendFritzCommand("getdevicelistinfos", refreshDeviceCallbackHandler)
    state.child = child.ain()
}

def poll(child){
	log.debug("child called main app (poll)")
    sendFritzCommand("getdevicelistinfos", refreshDeviceCallbackHandler)
    state.child = child.ain()
}

// Authentication

private authenticate(){
    log.debug("Current SID is: ${state.sid}")
    if (state.sid == null || state.sid == "0000000000000000")
    {
    	log.debug("authenticating now")
    	executeCommand("/login_sid.lua", authenticationRequestCallbackHandler)
    }
}

def authenticationRequestCallbackHandler(response){ 
    if(checkResponse(response)){
		def body = response.xml
    	def sid = body.toString().substring(0,16)
        def challenge = body.toString().substring(16,24)
        
               
        def user = fritzUser
        def password = fritzPassword
        
        def hash = generateMD5("${challenge}-${password}")
                
        executeCommand("/login_sid.lua?username=${user}&response=${challenge}-${hash}", authenticationChallengeCallbackHandler)
    }
}

def authenticationChallengeCallbackHandler(response){ 
    if(checkResponse(response)){
		def body = response.xml
    	def sid = body.toString().substring(0,16)
        
        log.debug("sid received: ${sid}")
        state.sid = sid
    }
}

// HTTP Commands

private executeCommand(command, callback){  
    def hostip = fritzIp
    def port = "80"
    
    def host = "$hostip:$port"
    
    
    def hubAction = new physicalgraph.device.HubAction("GET ${command} HTTP/1.1\r\nHOST: ${host}\r\nConnection: keep-alive\r\n\r\n",
            											physicalgraph.device.Protocol.LAN, 
                                                        host, 
                                                        [callback: callback])
                                                        
	log.debug(hubAction)
    
    sendHubCommand(hubAction)
}

private sendFritzCommand(command, callback){
    authenticate()
	
	executeCommand("/webservices/homeautoswitch.lua?switchcmd=${command}&sid=${state.sid}",callback)
}

private sendSwitchCommand(command, ain, callback){
    authenticate()
        
	executeCommand("/webservices/homeautoswitch.lua?ain=${ain}&switchcmd=${command}&sid=${state.sid}",callback)
}

// Helper

private boolean checkResponse(response){
	def header = response.header
    def statusCode = header.substring(9,12)
    
    if (statusCode == "200")
    	return true
        
    log.debug("Error ${statuscode} returned. Resetting SID")
		            
	state.sid = "0000000000000000"
    
    return false
}

def getDevices() {
	if (!state.devices) {
		state.devices = [:]
	}
    
	state.devices
}

void getDeviceList() {
	log.debug("getDeviceList()")
	sendFritzCommand("getdevicelistinfos", deviceListCallbackHandler)
}

def generateMD5(String s){
    MessageDigest digest = MessageDigest.getInstance("MD5")
    digest.update(s.getBytes("UTF-16LE"));
    byte[] hash = digest.digest();
    
    String output = ""
    
    for(byte b: hash){
        output += Integer.toString( ( b & 0xff ) + 0x100, 16).substring( 1 )
    }

    return output
}
