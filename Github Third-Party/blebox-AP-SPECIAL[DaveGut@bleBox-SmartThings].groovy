/*
bleBox Device Integration Application, Version 0/1
		Copyright 2018, 2019 Dave Gutheinz
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this  file 
except in compliance with the License. You may obtain a copy of the License at: 
		http://www.apache.org/licenses/LICENSE-2.0.
Unless required by applicable law or agreed to in writing,software distributed under the 
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific language governing permissions 
and limitations under the License.

Usage: Use this code freely without reference to origination.
===== History =====
07.31.2021	1.0.04	Special update for wLightBox converting it to rgbw.
============================================================================================*/
def appVersion() { return "1.0.04" }
import groovy.json.JsonSlurper
definition(
	name: "bleBox Integration",
	namespace: "davegut",
	author: "Dave Gutheinz",
	description: "Application to install bleBox devices.",
	category: "Convenience",
	iconUrl: "https://www.persianasdomesticas.com/images/blebox_nowe-logoPNG-web.png",
	iconX2Url: "https://www.persianasdomesticas.com/images/blebox_nowe-logoPNG-web.png",
	iconX3Url: "https://www.persianasdomesticas.com/images/blebox_nowe-logoPNG-web.png",
	singleInstance: true)
preferences {
	page(name: "mainPage")
	page(name: "addDevicesPage")
	page(name: "listDevicesPage")
}
def setInitialStates() {
	logDebug("setInitialStates")
	app?.deleteSetting("selectedAddDevices")
    state.listDevicesStarted = false
    state.addDevicesStarted = false
	state.missingDevice = false
}
def installed() {
	if (!state.devices) { state.devices = [:] }
	if (!state.deviceIps) { state.deviceIps = [:] }
	app?.updateSetting("automaticIpPolling", [type:"bool", value: false])
	app?.updateSetting("debugLog", [type:"bool", value: false])
	app?.updateSetting("infoLog", [type:"bool", value: true])
	initialize()
}
def updated() { initialize() }
def initialize() {
	logDebug("initialize")
	unschedule()
	if (selectedAddDevices) { addDevices() }
	if (automaticIpPolling == true) { runEvery1Hour(updateDevices) }
}


//	=====	Main Page	=====
def mainPage() {
	logDebug("mainPage")
	setInitialStates()
	return dynamicPage(name:"mainPage",
		title:"bleBox Device Manager",
		uninstall: true,
		install: true) {
		section() {
			href "addDevicesPage",
				title: "Install bleBox Devices",
				description: "Gets device information. Then offers new devices for install.\n" +
							 "(It will take a minute for the next page to load.)"
			href "listDevicesPage",
					title: "List all available bleBox devices",
					description: "Lists available devices.\n" +
								 "(It will take several minutes for the next page to load.)"
			input ("infoLog", "bool",
				   required: false,
				   submitOnChange: true,
				   title: "Enable Application Info Logging")
			input ("debugLog", "bool",
				   required: false,
				   submitOnChange: true,
				   title: "Enable Application Debug Logging")
			paragraph "Recommendation:  Set Static IP Address in your WiFi router for all bleBox Devices. " +
				"The polling option takes significant system resources while running."
			input ("automaticIpPolling", "bool",
				   required: false,
				   submitOnChange: true,
				   title: "Start (true) / Stop (false) Hourly IP Polling",
				   description: "Not Recommended.")
		}
	}
}


//	=====	Add Devices	=====
def addDevicesPage() {
    def message = "The devices in the dropdown below are available."
    if (state.addDevicesStarted == false) {
    	state.addDevicesStarted = true
    	message = "Looking for bleBox Devices.  This will take several minutes."
		state.devices = [:]
		findDevices(250, "parseDeviceData")
    }
    def devices = state.devices
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.dni)
		if (!isChild) {
			newDevices["${it.value.dni}"] = "${it.value.type} ${it.value.label}"
		}
	}
	logDebug("addDevicesPage: newDevices = ${newDevices}")
	return dynamicPage(name:"addDevicesPage",
		title:"Add bleBox Devices to Hubitat",
		refreshInterval: 15, 
		install: true) {
	 	section() {
        	paragraph message
			input ("selectedAddDevices", "enum",
				   required: false,
				   multiple: true,
				   title: "Devices to add (${newDevices.size() ?: 0} available)",
				   description: "Use the dropdown to select devices to add.  Then select 'Done'.",
				   options: newDevices)
		}
	}
}
def parseDeviceData(response) {
	def cmdResponse = parseResponse(response)
	if (cmdResponse == "error") { return }
	logDebug("parseDeviceData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	if (cmdResponse.device) { cmdResponse = cmdResponse.device }
	def label = cmdResponse.deviceName
	def dni = cmdResponse.id
	def type = cmdResponse.type
	def ip = convertHexToIP(response.ip)
	def typeData
	if (type == "gateBox") {
		if (cmdResponse.hv.length() > 7) {
			if (cmdResponse.hv.substring(0,7) == "doorBox") { type = "doorBox" }
		}
	}
	def devData = [:]
	devData["dni"] = dni.toUpperCase()
	devData["ip"] = ip
	devData["label"] = label
	devData["type"] = type
	state.devices << ["${dni.toUpperCase()}" : devData]
	def isChild = getChildDevice(dni)
	if (isChild) {
		isChild.updateDataValue("deviceIP", ip)
	}
	if (type == "switchBoxD") {
		sendGetCmd(ip, """/api/relay/state""", "parseRelayData")
//	} else if (type == "wLightBox") {
//		sendGetCmd(ip, """/api/rgbw/state""", "parseRgbwData")
	} else if (type == "shutterBox") {
    	sendGetCmd(ip, """/api/settings/state""", "parseShutterData")
    } else if (type == "dimmerBox") {
		sendGetCmd(ip, "/api/dimmer/state", "parseDimmerData")
	}
}
def parseRelayData(response) {
	def cmdResponse = parseResponse(response)
	if (cmdResponse == "error") { return }
	logDebug("parseRelayData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	def relays = cmdResponse.relays
	def devIp = convertHexToIP(response.ip)
	def device = state.devices.find { it.value.ip == devIp }
	def dni = device.value.dni
	device.value << [dni:"${dni}-0", label:relays[0].name, relayNumber:"0"]
	def relay2Data = ["dni": "${dni}-1",
					  "ip": device.value.ip,
					  "type": device.value.type,
					  "label": relays[1].name,
					  "relayNumber": "1"]
	state.devices << ["${dni}-1" : relay2Data]
}
def parseRgbwData(response) {
	def cmdResponse = parseResponse(response)
	if (cmdResponse == "error") { return }
	logDebug("parseRgbwData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
    def type = "wLightBox RGBW"
	if (cmdResponse.rgbw.colorMode == 3) { type = "wLightBox Mono" }
	def devIp = convertHexToIP(response.ip)
	def device = state.devices.find {it.value.ip == devIp }
	device.value << [type: type]
}
def parseShutterData(response) {
	def cmdResponse = parseResponse(response)
	logDebug("parseShutterData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	if (cmdResponse == "error") { return }
	def controlType = cmdResponse.settings.shutter.controlType
	if (controlType == 3) {
		def devIp = convertHexToIP(response.ip)
		def device = state.devices.find {it.value.ip == devIp }
		device.value << [type: "shutterBox Tilt"]
	}
}
def parseDimmerData(response) {
	def cmdResponse = parseResponse(response)
	logDebug("parseDimmerData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	if (cmdResponse == "error") { return }
	if (cmdResponse.dimmer.loadType == 1) {
		def devIp = convertHexToIP(response.ip)
		def device = state.devices.find {it.value.ip == devIp }
		device.value << [type: "dimmerBox NoDim"]
	}
}
def addDevices() {
	logDebug("addDevices:  Devices = ${state.devices}")
	def hub
	try { hub = location.hubs[0] }
	catch (error) { 
		logWarn("Hub not detected.  You must have a hub to install devices.")
		return
	}
	def hubId = hub.id
	selectedAddDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = state.devices.find { it.value.dni == dni }
			def deviceData = [:]
			deviceData["applicationVersion"] = appVersion()
			deviceData["deviceIP"] = device.value.ip
			if (device.value.relayNumber) {
            	deviceData["relayNumber"] = device.value.relayNumber
            }
			try {
				addChildDevice(
					"davegut",
					"bleBox ${device.value.type}",
					device.value.dni,
					hubId, [
						"label" : device.value.label,
						"name" : device.value.type,
						"data" : deviceData
					]
				)
			} catch (error) {
				logWarn("Failed to install ${device.value.label}.  Driver bleBox ${device.value.type} most likely not installed.")
			}
		}
	}
}


//	=====	Update Device IPs	=====
def listDevicesPage() {
	logDebug("updateIpsPage")
    def message = "Found the following bleBox Devices:"
    if (state.listDevicesStarted == false) {
    	state.listDevicesStarted = true
    	message = "Looking for bleBox Devices.  This will take several minutes."
		state.deviceIps = [:]
		findDevices(250, "parseIpData")
    }
	def deviceIps = state.deviceIps
	def foundDevices = "Found Devices (Installed / DNI / IP / Alias):"
	def count = 1
	deviceIps.each {
		def installed = false
		if (getChildDevice(it.value.DNI)) { installed = true }
		foundDevices += "\n${count}:\t${installed}\t${it.value.dni}\t${it.value.ip}\t${it.value.label}"
		count += 1
	}
	return dynamicPage(name:"listDevicesPage",
		title:"Available bleBox Devices on your LAN",
		refreshInterval: 30, 
		install: false) {
	 	section() {
        	paragraph message
			paragraph "${foundDevices}"
            paragraph "RECOMMENDATION: Set Static IP Address in your WiFi router for bleBox Devices."
		}
	}
}
def parseIpData(response) {
	def cmdResponse = parseResponse(response)
	if (cmdResponse == "error") { return }
	logDebug("parseIpData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	if (cmdResponse.device) { cmdResponse = cmdResponse.device }
	def label = cmdResponse.deviceName
	def ip = convertHexToIP(response.ip)
	def dni = cmdResponse.id.toUpperCase()
	if (cmdResponse.type == "switchBoxD") {
		addIpData("${dni}-0", ip, label)
		addIpData("${dni}-1", ip, label)
		return
	}
	addIpData(dni, ip, label)
}
def addIpData(dni, ip, label) {
	logDebug("addData: ${dni} / ${ip} / ${label}")
	def device = [:]
	def deviceIps = state.deviceIps
	device["dni"] = dni
	device["ip"] = ip
	device["label"] = label
	deviceIps << ["${dni}" : device]
	def isChild = getChildDevice(dni)
	if (isChild) {
		isChild.updateDataValue("deviceIP", ip)
	}
}


//	===== Recurring IP Check =====
def updateDevices() {
	logDebug("UpdateDevices: ${state.devices}")
	state.missingDevice = false
	def devices = state.deviceIps
	if (deviceIps == [:]) {
		findDevices(1000, parseIpData)
		return
	} else {
		devices.each {
			def deviceIP = it.value.ip
			runIn(2, setMissing)
			sendGetCmd(deviceIP, "/api/device/state", checkValid)
			pauseExecution(2100)
		}
	}
	if (state.missingDevice == true) {
		state.deviceIps= [:]
		findDevices(1000, parseIpData)
		state.missingDevices == false
	}
}
def checkValid() {
	def cmdResponse = parseResponse(response)
	if (cmdResponse == "error") { return }
	logDebug("parseIpData: ${convertHexToIP(response.ip)} // ${cmdResponse}")
	if (cmdResponse.device) { cmdResponse = cmdResponse.device }
    else { return }		//	Handle case where a error message is returned by the device.
	unschedule("setMissing")
}
def setMissing() { state.missingDevice = true }


//	=====	Device Communications	=====
def findDevices(pollInterval, action) {
	logDebug("findDevices: ${pollInterval} / ${action}")
	def hub
	try { hub = location.hubs[0] } catch (error) { 
		logWarn "Hub not detected.  You must have a hub to install these devices."
		return
	}
	def hubIpArray = hub.localIP.split('\\.')
	def networkPrefix = [hubIpArray[0],hubIpArray[1],hubIpArray[2]].join(".")
	logInfo("findDevices: IP Segment = ${networkPrefix}")
    List actions = []
    def deviceIP
    def parameters
	for(int i = 2; i < 254; i++) {
		deviceIP = "${networkPrefix}.${i.toString()}"
        actions.add(new physicalgraph.device.HubAction([method: "GET", path: "/api/device/state", 
        		headers: [ Host: "${deviceIP}:80"]], 
        		null, [callback: action]))
	}
    sendHubCommand(actions, pollInterval)
}
private sendGetCmd(deviceIP, command, action){
	logDebug("sendGetCmd: ${command} // ${deviceIP} // ${action}")
	def parameters = [method: "GET", path: command, headers: [ Host: "${deviceIP}:80"]]
	sendHubCommand(new physicalgraph.device.HubAction(parameters, null, [callback: action]))
}
private sendPostCmd(command, body, action){
	logDebug("sendPostCmd: ${command} // ${getDataValue("deviceIP")} // ${action}")
	def parameters = [method: "POST",
					  path: command,
					  body: body,
					  headers: [
						  Host: "${getDataValue("deviceIP")}:80"
					  ]]
	sendHubCommand(new physicalgraph.device.HubAction(parameters, null, [callback: action]))
}
def parseResponse(response) {
	def cmdResponse
	if(response.status != 200) {
		logWarn("parseInput: Error - ${convertHexToIP(response.ip)} // ${response.status}")
		cmdResponse = "error"
	} else if (response.body == null){
		logWarn("parseInput: ${convertHexToIP(response.ip)} // no data in command response.")
		cmdResponse = "error"
	} else {
//	Added try to catch potential parsing error
		def jsonSlurper = new groovy.json.JsonSlurper()
        try {
        	cmdResponse = jsonSlurper.parseText(response.body)
        } catch (error) {
        	cmdResponse = "error"
        	logWarn("parseInput: error parsing body = ${response.body}")
        }
	}
	return cmdResponse
}


//	=====	Utility methods	=====
def uninstalled() {
    getAllChildDevices().each { 
        deleteChildDevice(it.deviceNetworkId)
    }
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private Integer convertHexToInt(hex) { Integer.parseInt(hex,16) }
def logDebug(msg){
	if(debugLog == true) { log.debug "${appVersion()} ${msg}" }
}
def logInfo(msg){
	if(infoLog != false) { log.info "${appVersion()} ${msg}" }
}
def logWarn(msg) { log.warn "${appVersion()} ${msg}" }

//	end-of-file