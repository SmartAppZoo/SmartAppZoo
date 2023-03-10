/**
 *  OmniPro Controller
 *
 *  Copyright 2016 Ryan Wagoner
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
    name: "OmniPro Integration",
    namespace: "excaliburpartners",
    author: "Ryan Wagoner",
    description: "Provides integration with the Leviton/HAI OmniPro controller",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
	appSetting "server"
	appSetting "port"
        appSetting "area"
}

preferences {
	section("Settings") {
		input("server", "string", title:"Server", required: true, displayDuringSetup: true)
		input("port", "string", title:"Port", defaultValue: "8000", required: true, displayDuringSetup: true)
		input("area", "string", title:"Follow Area ID", defaultValue: "1", displayDuringSetup: true)
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
	subscribeController()
	setupDevices()
	
	runEvery5Minutes(subscribeController)
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

void subscribeController() {
	log.debug "Subscribing controller"

	def callback = getCallBackAddress()
	def host = "${settings.server}:${settings.port}"

	def body = '{ "callback": "http://' + callback + '/notify/omnilink" }'
	def length = body.getBytes().size().toString()
	
	def hubAction = new physicalgraph.device.HubAction("""POST /Subscribe HTTP/1.1\r\nHOST: $host\r\nContent-Type: application/json\r\nContent-Length: $length\r\n\r\n$body\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: subscribeControllerHandler])
		
	//log.debug hubAction
	sendHubCommand(hubAction)
}

void subscribeControllerHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'subscribeControllerHandler(${hubResponse.body})'"
	
	def dni = hubResponse.mac
	def d = getChildDevice(dni)
	
	if (!d) {
		d = addChildDevice("excaliburpartners", "OmniPro Controller", dni, location.hubs[0].id, [
			"name": "OmniPro Controller",
			"label": "OmniPro Controller",
			"completedSetup": true
		])
		log.debug "Created ${d.displayName} with id $dni"
	} else {
		log.debug "Found ${d.displayName} with id $dni"
	}
}

void setupDevices() {
	log.debug 'setupDevices'

	def host = "${settings.server}:${settings.port}"
	
	// Area
	def hubActionArea	= new physicalgraph.device.HubAction("""GET /ListAreas HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceAreaSetupHandler])
	
	//log.debug hubActionArea
	sendHubCommand(hubActionArea)
	
	// Contact
	def hubActionContact	= new physicalgraph.device.HubAction("""GET /ListZonesContact HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceContactSetupHandler])
	
	//log.debug hubActionContact
	sendHubCommand(hubActionContact)
	
	// Motion
	def hubActionMotion	= new physicalgraph.device.HubAction("""GET /ListZonesMotion HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceMotionSetupHandler])
	
	//log.debug hubActionMotion
	sendHubCommand(hubActionMotion)
	
	// Water
	def hubActionWater	= new physicalgraph.device.HubAction("""GET /ListZonesWater HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceWaterSetupHandler])
	
	//log.debug hubActionWater
	sendHubCommand(hubActionWater)
	
	// Smoke
	def hubActionSmoke	= new physicalgraph.device.HubAction("""GET /ListZonesSmoke HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceSmokeSetupHandler])
	
	//log.debug hubActionSmoke
	sendHubCommand(hubActionSmoke)
	
	// CO
	def hubActionCO	= new physicalgraph.device.HubAction("""GET /ListZonesCO HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceCarbonMonoxideSetupHandler])
	
	//log.debug hubActionCO
	sendHubCommand(hubActionCO)
	
	// Temp
	def hubActionTemp	= new physicalgraph.device.HubAction("""GET /ListZonesTemp HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceTempSetupHandler])
	
	//log.debug hubActionTemp
	sendHubCommand(hubActionTemp)
	
	// Unit
	def hubActionUnit	= new physicalgraph.device.HubAction("""GET /ListUnits HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceUnitSetupHandler])
	
	//log.debug hubActionUnit
	sendHubCommand(hubActionUnit)
	
	
	// Thermostat
	def hubActionThermostat = new physicalgraph.device.HubAction("""GET /ListThermostats HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceThermostatSetupHandler])
	
	//log.debug hubActionThermostat
	sendHubCommand(hubActionThermostat)
	
	// Buttons
	def hubActionButton = new physicalgraph.device.HubAction("""GET /ListButtons HTTP/1.1\r\nHOST: $host\r\n\r\n""", 
		physicalgraph.device.Protocol.LAN, host, [callback: deviceButtonSetupHandler])
	
	//log.debug hubActionButton
	sendHubCommand(hubActionButton)
}

void deviceAreaSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceAreaSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('AREA', 'OmniPro Area', devices)
}

void deviceContactSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceContactSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('CONTACT', 'OmniPro Contact', devices)
}

void deviceMotionSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceMotionSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('MOTION', 'OmniPro Motion', devices)
}

void deviceWaterSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceWaterSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('WATER', 'OmniPro Water', devices)
}

void deviceSmokeSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceSmokeSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('SMOKE', 'OmniPro Smoke', devices)
}

void deviceCarbonMonoxideSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceCarbonMonoxideSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('CO', 'OmniPro Carbon Monoxide', devices)
}

void deviceTempSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceTempSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('TEMP', 'OmniPro Temp', devices)
}

void deviceUnitSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceUnitSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('UNIT', 'OmniPro Unit', devices)
}

void deviceThermostatSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceThermostatSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('THERMOSTAT', 'OmniPro Thermostat', devices)
}

void deviceButtonSetupHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug "Firing 'deviceButtonSetupHandler(${hubResponse.body})'"
	def devices = hubResponse.json
	log.debug "${hubResponse.error}"
	
	createChildDevices('BUTTON', 'OmniPro Button', devices)
}

void removeDevices() {
	removeChildDevices(getChildDevices())
}

private createChildDevices(type, typeName, devices)
{
	devices.each {	
		def dni = "OMNILINK:${type}:${it.id}"
		def d = getChildDevice(dni)
		
		if (!d) {
			d = addChildDevice("excaliburpartners", typeName, dni, location.hubs[0].id, [
				"name": it.name,
				"completedSetup": true,
			])
			log.debug "Created ${d.displayName} with id $dni"
		} else {
			log.debug "Found ${d.displayName} with id $dni"
		}
		
		d.refresh()
    }
}

private removeChildDevices(delete) {
	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

private getCallBackAddress() {
	return location.hubs[0].localIP + ":" +  location.hubs[0].localSrvPortTCP
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
	return hexport
}

def passToChild(type, data) {
	def dni = "OMNILINK:${type}:${data.id}"
	def d = getChildDevice(dni)
	
	if (d) {
		d.parseFromParent(data);
	} else {
		log.warn "passToChild device $dni missing"
	}	
}

def buildAction(method, path, body) {
	def headers = [:] 
	headers.put("HOST", "${settings.server}:${settings.port}")
	
	if (method == "POST")
		headers.put("Content-Type", "application/json")

	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers,
		)

		log.debug hubAction
		return hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}