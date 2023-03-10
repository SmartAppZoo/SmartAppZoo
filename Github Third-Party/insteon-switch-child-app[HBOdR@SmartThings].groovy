/**
 *  Insteon Switch Child Smart App
 *  
 *	This 
 *
 *  Original Author     : ethomasii@gmail.com
 *  Creation Date       : 2013-12-08
 *
 *  Rewritten by        : idealerror
 *  Last Modified Date  : 2016-12-13
 *
 *  Rewritten by        : kuestess
 *  Last Modified Date  : 2017-12-30
 *
 *  Rewritten by        : finik
 *  Last Modified Date  : 2018-01-27
 *
 *
 *  Copyright 2018 Hugo Bonilla
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

import groovy.json.JsonSlurper

definition(
	name: "Insteon Switch Child",
	namespace: "HBOdR",
	author: "Hugo Bonilla",
	description: "Do not install this directly, use 'Insteon Device Manager' instead. This is a child application for 'Insteon Device Manager'.",
	category: "Convenience",
	singleInstance: true,
	iconUrl: "http://www.freeiconspng.com/uploads/light-switch-png-icon-1.png",
	iconX2Url: "http://www.freeiconspng.com/uploads/light-switch-png-icon-1.png",
	iconX3Url: "http://www.freeiconspng.com/uploads/light-switch-png-icon-1.png",
)

preferences {
	page(name: "pageMain", title: "", install: true, uninstall: true) {
		section(title: "Insteon Switches") {
			input name: "deviceName", title: "Device Name", type: "text", required: true
			input name: "deviceID", title: "Device ID", type: "text", required: true
			input name: "isDimmable", title: "Dimmable", type: "bool", defaultValue: "false"
		}
	}
}

def installed() {
	log.debug "Child ${settings.deviceName} installed with settings: ${settings}"
	initialize()
	addInsteonSwitch();
}

def updated() {
	unsubscribe()
	log.debug "Child updated with settings: ${settings}"
	initialize()
	updateInsteonDevice();
}

def initialize() {
	log.debug "My ID = ${app.getId()} initialized with settings: ${settings}"
	app.updateLabel("${settings.deviceName}")
}

def uninstalled() {
	log.trace "Uninstalling Insteon Switch Child..."
}

def addInsteonSwitch() {
	if (!deviceName) return

	def theSwitchType = settings.isDimmable ? "Insteon Dimmer" : "Insteon Switch"
	def theDeviceNetworkId = getInsteonDeviceID()
	def theHostHubId = parent.settings.hostHub.id

	def theDevice = addChildDevice(
    	theSwitchType,
        theDeviceNetworkId,
        theHostHubId, [
        label: deviceName, name: deviceName,
        ]
    )
	setInsteonDevice(theDevice)

	theDevice.refresh();

	log.debug "New Device added ${deviceName}"
}

def updateInsteonDevice() {
	if (!deviceName) return

	log.debug "Updating Device ${deviceName}"

	def theDeviceNetworkId = getInsteonDeviceID();
	def theDevice = getDevicebyNetworkId(theDeviceNetworkId)

	if (theDevice) { // The switch already exists
		setInsteonDevice(theDevice)

		log.debug "The device id is ${theDevice.id}."
		theDevice.label = deviceName
		theDevice.name = deviceName
		subscribe(theDevice, "switch", switchChange)
	} 
}

def switchChange(evt) {
	log.debug "Switch event! Setting state now for ${state.InsteonDeviceID} to ${evt.value}"
}

def getInsteonDeviceID() {
	if (!state.InsteonDeviceID)
		setInsteonDevice()

	return state.InsteonDeviceID
}

def setInsteonDevice(theDevice) {
	state.InsteonDeviceID = "${settings.deviceID.toUpperCase()}"
	
    if (theDevice)
		theDevice.deviceNetworkId = state.InsteonDeviceID
}

def childOn(id) {
	def theDevice = getDevicebyNetworkId("${id}")    
	def level = (theDevice.currentValue("level") * 255 / 100) as Integer
    level = Math.max(Math.min(level, 255), 0)

	log.debug "Turning ON switch ${id} with level: ${level}"
	sendCmd(id, 0x11, level, commandCallback)
}

def childOff(id) {
	log.trace "ChildOff for ${id}"
    sendCmd(id, 0x13, 0, commandCallback)
}

def childDim(id, value) {
    log.trace "childDim(${id}, ${value})"
    
    def level = (value * 255 / 100) as Integer
    level = Math.max(Math.min(level, 255), 0)

    sendCmd(id, 0x11, level, commandCallback)
}

def childStatus(id) {
	log.trace "childStatus(${id})"
	sendCmd(id, 0x19, 0, commandCallback)

	if (!state.numberOfTries) {	//set id when enter for the first time
    	state.numberOfTries = 3
		state.pendingDeviceId = id
	}
    else
    	state.numberOfTries = state.numberOfTries - 1

	runIn(1, getBufferStatus)
}

void commandCallback(physicalgraph.device.HubResponse hubResponse) {
    log.trace "commandCallback()..."
    def body = hubResponse.body

    if (body != null)
    	log.debug "Reply from command: ${body}"
}

void statusCallback(physicalgraph.device.HubResponse hubResponse) {
	log.trace "statusCallback()"
	def body = hubResponse.xml
	if (body != null) {
		def buffer = "${body}"
		def deviceId = buffer.substring(22, 28)
		def status = buffer.substring(38, 40)
		def level = Math.round(Integer.parseInt(status, 16)*(100/255))

		log.debug "Device: ${deviceId}, level: ${level}"

		def children = getChildDevices()
	  
		// Propagate the response to the right child
		children.each { child ->
			if (deviceId == child.deviceNetworkId)
				child.updatestatus(level)
		}

		if (state.numberOfTries == 0)	// avoid infinite loop if deviceID doesn't exit/match in Insteon hub.
			state.pendingDeviceId = null
        
		if ((state.pendingDeviceId != null) && (deviceId != state.pendingDeviceId)){
			log.debug "Response is for wrong device. Trying again ${state.numberOfTries} more time."
			childStatus(state.pendingDeviceId)	// recursive call to childStatus may cause infinite loop see above (and live logs).
		} else
			state.pendingDeviceId = null
	}
}

def getBufferStatus() {
    log.debug "getBufferStatus()"

    def ip = parent.settings.InsteonIP
    def port = parent.settings.InsteonPort
	def iphex = convertIPtoHex(ip)
	def porthex = convertPortToHex(port)
    def userpassascii = "${parent.settings.InsteonHubUsername}:${parent.settings.InsteonHubPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    
    def headers = [:] //"HOST:" 
	headers.put("HOST", "$ip:$port")
	headers.put("Authorization", userpass)
    
	def hubAction = new physicalgraph.device.HubAction(
		[method: "GET",
		path: "/buffstatus.xml",
		headers: headers],
		"$iphex:$porthex",
		[callback: statusCallback]
	)

    sendHubCommand(hubAction)    
}

def sendCmd(id, cmd, param, callback = null) {
	log.debug "sendCmd(${id}, ${cmd}, ${param})"
	
    def ip = parent.settings.InsteonIP
    def port = parent.settings.InsteonPort
	def iphex = convertIPtoHex(ip)
	def porthex = convertPortToHex(port)
	def userpassascii = "${parent.settings.InsteonHubUsername}:${parent.settings.InsteonHubPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
	
	def headers = [:] //"HOST:" 
	headers.put("HOST", "$ip:$port")
	headers.put("Authorization", userpass)
	
	def uri = "/3?0262${id}0F${toHex(cmd)}${toHex(param)}=I=3"
	
	def hubAction = new physicalgraph.device.HubAction(
		[method: "GET",
		path: uri,
		headers: headers],
        "$iphex:$porthex",
		[callback: callback]
	)

	sendHubCommand(hubAction) 
}

private getDevicebyNetworkId(String theDeviceNetworkId) {
	return getChildDevices().find {
		d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
	}
}

private toHex(value) {
	return value.toString().format( '%02x', value.toInteger() )
}

private String convertIPtoHex(ipAddress) { 
    return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
}

private String convertPortToHex(port) {
	return port.toString().format( '%04x', port.toInteger() )
}
