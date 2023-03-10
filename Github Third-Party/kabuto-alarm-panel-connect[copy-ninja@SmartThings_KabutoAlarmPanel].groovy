/**
 *  Kabuto Alarm Panel (Connect)
 *
 *  Copyright 2017 Jason Mok
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
    name: 			"Kabuto Alarm Panel (Connect)",
    namespace: 		"copy-ninja",
    author: 		"Jason Mok",
    description: 	"Kabuto Alarm Panel",
    category: 		"Safety & Security",
	iconUrl: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel.png",
	iconX2Url: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel@2x.png",
	iconX3Url: 		"http://www.copyninja.net/smartthings/icons/KabutoAlarmPanel@3x.png",
    singleInstance: true
)
mappings {
	path("/device/:mac/:id/:deviceState")	{ action: [ PUT: "childDeviceStateUpdate"] }
}
preferences {
	page(name: "pageDiscovery",     install: false, uninstall: true, content: "pageDiscovery", 		nextPage: "pageConfiguration" )
	page(name: "pageConfiguration", install: true, 	uninstall: true, content: "pageConfiguration")
}
def pageDiscovery() {
	if(!state.accessToken) { createAccessToken() }	
	dynamicPage(name: "pageDiscovery", nextPage: "pageConfiguration", refreshInterval: 3) {
		discoverySubscribtion()
		discoverySearch()
		discoveryVerification()
		def alarmPanels = pageDiscoveryGetAlarmPanels()
		section("Please wait while we discover your Kabuto Alarm Panel") {
			input(name: "selectedAlarmPanels", type: "enum", title: "Select Alarm Panel (${alarmPanels.size() ?: 0} found)", required: true, multiple: true, options: alarmPanels)
		}
	}	
}
def pageConfiguration() {
	def selecteAlarmPanels = [] + getSelectedAlarmPanel()
	dynamicPage(name: "pageConfiguration", nextPage: "pageDiscovery" ) {
		selecteAlarmPanels.each { alarmPanel ->
			section(hideable: true, "AlarmPanel_${alarmPanel.mac[-6..-1]}") {
				input(name: "deviceType_${alarmPanel.mac}_1", type: "enum", title:"Pin 1 Device Type", required: false, multiple: false, options: pageConfigurationGetDeviceType())
				input(name: "deviceType_${alarmPanel.mac}_2", type: "enum", title:"Pin 2 Device Type", required: false, multiple: false, options: pageConfigurationGetDeviceType())
				input(name: "deviceType_${alarmPanel.mac}_5", type: "enum", title:"Pin 5 Device Type", required: false, multiple: false, options: pageConfigurationGetDeviceType())
				input(name: "deviceType_${alarmPanel.mac}_6", type: "enum", title:"Pin 6 Device Type", required: false, multiple: false, options: pageConfigurationGetDeviceType())
				input(name: "deviceType_${alarmPanel.mac}_7", type: "enum", title:"Pin 7 Device Type", required: false, multiple: false, options: pageConfigurationGetDeviceType())
			}
		}
	}
}
Map pageConfigurationGetDeviceType() { 
	return [ "contact":"Open/Close Sensor", 
			 "motion":"Motion Sensor", 
			 "smoke":"Smoke Detector" ,
			 "siren":"Siren/Strobe",
			 "switch":"Panic Button"] }
Map pageDiscoveryGetAlarmPanels() {
	def alarmPanels = [:]
	def verifiedAlarmPanels = getAlarmPanels().findAll{ it.value.verified == true }
	verifiedAlarmPanels.each { alarmPanels["${it.value.mac}"] = it.value.name ?: "AlarmPanel_${it.value.mac[-6..-1]}" }
	return alarmPanels
}
def installed() { 
	initialize() 
	runEvery3Hours(discoverySearch)
}
def updated() { initialize() }
def initialize() {
	unsubscribe()
	unschedule()
	discoverySubscribtion(true)	
	childDeviceConfiguration()
    deviceUpdateSettings()
}

def getSelectedAlarmPanel(mac) {
	if (!state.alarmPanel) { 
		state.alarmPanel = []
		def selecteAlarmPanels = [] + settings.selectedAlarmPanels
		selecteAlarmPanels.each { alarmPanel -> 
			def selectedAlarmPanel = getAlarmPanels().find { it.value.mac == alarmPanel } 
			state.alarmPanel = state.alarmPanel + [
				mac : selectedAlarmPanel.value.mac, 
				ip  : selectedAlarmPanel.value.networkAddress, 
				port: selectedAlarmPanel.value.deviceAddress,
				hub : selectedAlarmPanel.value.hub,
				host: "${convertHexToIP(selectedAlarmPanel.value.networkAddress)}:${convertHexToInt(selectedAlarmPanel.value.deviceAddress)}"
			]
		}
	} 
	if (mac) {
		return state.alarmPanel.find { it.mac == mac } 
	} else {
		return state.alarmPanel
	}
}
def getAlarmPanels() {
	if (!state.devices) { state.devices = [:] }
	return state.devices
}

//Device discovery through SSDP
def discoveryDeviceType() { return "urn:schemas-copyninja-net:device:AlarmPanel:1" }
def discoverySearch() { sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${discoveryDeviceType()}", physicalgraph.device.Protocol.LAN)) }
def discoverySubscribtion(force=false) {
	if (force) {
		unsubscribe()
		state.subscribe = false
	}
	if(!state.subscribe) {
		subscribe(location, "ssdpTerm.${discoveryDeviceType()}", discoverySearchHandler, [filterEvents:false])
		state.subscribe = true
	}
}
def discoverySearchHandler(evt) {
	def event = parseLanMessage(evt.description)
	event << ["hub":evt?.hubId]
	def devices = getAlarmPanels()
	String ssdpUSN = event.ssdpUSN.toString()
	if (!devices."${ssdpUSN}") { devices << ["${ssdpUSN}": event] }
	if (state.alarmPanel) {
		state.alarmPanel.each {
			if (it.mac == event.mac) {
				if (it.ip != event.networkAddress) or (it.port != event.deviceAddress) {
					it.ip   = event.networkAddress 
					it.port = event.deviceAddress
					it.host = "${convertHexToIP(event.networkAddress)}:${convertHexToInt(event.deviceAddress)}"
				}
			}
		}
	}
}
def discoveryVerification() {
	def alarmPanels = getAlarmPanels().findAll { it?.value?.verified != true }
	alarmPanels.each {
		String host = "${convertHexToIP(it.value.networkAddress)}:${convertHexToInt(it.value.deviceAddress)}"
		sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: ${host}\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: discoveryVerificationHandler]))
	}
}
def discoveryVerificationHandler(physicalgraph.device.HubResponse hubResponse) {
	def body = hubResponse.xml
	def devices = getAlarmPanels()
	def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
	if (device) { device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true] }
}


def childDeviceConfiguration() {
	def selecteAlarmPanels = [] + getSelectedAlarmPanel()
	settings.each { name , value ->
		def nameValue = name.split("\\_")
		if (nameValue[0] == "deviceType") {
			def selectedAlarmPanel = getSelectedAlarmPanel().find { it.mac == nameValue[1] } 
			def deviceDNI = [ selectedAlarmPanel.mac, "${nameValue[2]}"].join('|') 
			def deviceType = ""
			switch(value) {
				case "contact": 
					deviceType = "Kabuto Contact Sensor"
					break
				case "motion": 
					deviceType = "Kabuto Motion Sensor"
					break
				case "smoke":
					deviceType = "Kabuto Smoke Sensor"
					break
				case "siren":
					deviceType = "Kabuto Siren/Strobe"
					break
				case "switch":
					deviceType = "Kabuto Panic Button"
					break
			}
			if (!getChildDevice(deviceDNI)) { addChildDevice("copy-ninja", deviceType, deviceDNI, selectedAlarmPanel.hub, [ "label": deviceType ]) }
		}
	}
}
def childDeviceStateUpdate() {
	def device = getChildDevice(params.mac.toUpperCase() + "|" + params.id)
	if (device) device.setStatus(params.deviceState)
}

def deviceUpdateSettings() {
	if(!state.accessToken) { createAccessToken() }	
	def body = [
		token : state.accessToken,
		apiUrl : apiServerUrl + "/api/smartapps/installations/" + app.id,
		sensors : []
	]
	getAllChildDevices().each {  if (it.name != "Kabuto Siren/Strobe") { body.sensors = body.sensors + [ pin : it.deviceNetworkId.split("\\|")[1] ] } }
	def selectedAlarmPanel = [] + getSelectedAlarmPanel()
	selectedAlarmPanel.each { sendHubCommand(new physicalgraph.device.HubAction([method: "PUT", path: "/settings", headers: [ HOST: it.host, "Content-Type": "application/json" ], body : groovy.json.JsonOutput.toJson(body)], it.host)) }
}
def deviceUpdateDeviceState(deviceDNI, deviceState) {
	def deviceId = deviceDNI.split("\\|")[1]
	def deviceMac = deviceDNI.split("\\|")[0]
	def body = [ pin : deviceId, state : deviceState ]
	def selectedAlarmPanel = getSelectedAlarmPanel(deviceMac)
	sendHubCommand(new physicalgraph.device.HubAction([method: "PUT", path: "/device", headers: [ HOST: selectedAlarmPanel.host, "Content-Type": "application/json" ], body : groovy.json.JsonOutput.toJson(body)], selectedAlarmPanel.host))
}

private Integer convertHexToInt(hex) { Integer.parseInt(hex,16) }
private String convertHexToIP(hex) { [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".") }