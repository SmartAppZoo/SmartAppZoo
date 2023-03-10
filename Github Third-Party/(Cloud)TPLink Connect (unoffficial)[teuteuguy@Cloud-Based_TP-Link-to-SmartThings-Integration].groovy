/*
TP-Link (unofficial) Connect Service ManagerHandler (BETA)

Copyright 2017 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
file except in compliance with the License. You may obtain a copy of the License at:
		http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under 
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
ANY KIND, either express or implied. See the License for the specific language governing 
permissions and limitations under the License.

##### Discalimer:  This Service Manager and the associated Device Handlers are in no
way sanctioned or supported by TP-Link.  All  development is based upon open-source data
on the TP-Link devices; primarily various users on GitHub.com.

##### Notes #####
1.  This Service Manager is designed to install and manage TP-Link bulbs, plugs, and
	switches using their respective device handlers.
3.  Please direct comments to the SmartThings community thread
	'Cloud TP-Link Device SmartThings Integration'.

##### History #####
07-28 - Beta Release
08-01 - Modified and tested error condition logic.  Updated on-screen messages.
*/

definition(
	name: "TP-Link (unofficial) Connect",
	namespace: "beta",
	author: "Dave Gutheinz",
	description: "A Service Manager for the TP-Link devices connecting through the TP-Link Cloud",
	category: "SmartThings Labs",
	iconUrl: "http://ecx.images-amazon.com/images/I/51S8gO0bvZL._SL210_QL95_.png",
	iconX2Url: "http://ecx.images-amazon.com/images/I/51S8gO0bvZL._SL210_QL95_.png",
	iconX3Url: "http://ecx.images-amazon.com/images/I/51S8gO0bvZL._SL210_QL95_.png",
    singleInstance: true
)

preferences {
	page(name: "cloudLogin", title: "TP-Link Cloud Login", nextPage:"", content:"cloudLogin", uninstall: true)
	page(name: "selectDevices", title: "Select TP-Link Devices", nextPage:"", content:"selectDevices", uninstall: true, install: true)
}

def setInitialStates() {
	if (!state.TpLinkToken) {state.TpLinkToken = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
}

//	----- LOGIN PAGE -----
def cloudLogin() {
	setInitialStates()
	def cloudLoginText = "If possible, open the IDE and select Live Logging.  THEN, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
        "action you want to complete.  Your current token:\n\r\n\r${state.TpLinkToken}" +
        "\n\r\n\rAvailable actions:\n\r" +
        "   Initial Install: Obtains token and adds devices.\n\r" +
        "   Add Devices: Only add devices.\n\r" +
        "   Update Token:  Updates the token.\n\r"
	def errorMsg = ""
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r\n\r${state.currentError}" +
        	"\n\r\n\rPlease resolve the error and try again.\n\r\n\r"
		}
   return dynamicPage(
		name: "cloudLogin", 
		title: "TP-Link Device Service Manager", 
		nextPage: "selectDevices", 
		uninstall: true) {
		section(errorMsg)
		section(cloudLoginText) {
			input( 
				"userName", "string", 
				title:"Your TP-Link Email Address", 
				required:true, 
				displayDuringSetup: true
		   )
			input(
				"userPassword", "password", 
				title:"TP-Link account password", 
				required: true, 
				displayDuringSetup: true
			)
			input(
				"updateToken", "enum",
				title: "What do you want to do?",
				required: true, 
				multiple: false,
				options: ["Initial Install", "Add Devices", "Update Token"]
			)
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	if (updateToken != "Add Devices") {
    	getToken()
    }
	if (state.currentError != null || updateToken == "Update Token") {
    	return cloudLogin()
	}
	getDevices()
	def devices = state.devices
	if (state.currentError != null) {
    	return cloudLogin()
	}
    def errorMsg = ""
    if (devices == [:]) {
    	errorMsg = "There were no devices from TP-Link.  This usually means "+
        	"that all devices are in 'Local Control Only'.  Correct then " +
            "rerun.\n\r\n\r"
        }
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (!isChild) {
			newDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
        }
	}
    if (newDevices == [:]) {
    	errorMsg = "No new devices to add.  Are you sure they are in Remote " +
        	"Control Mode?\n\r\n\r"
        }
    settings.selectedDevices = null
	def TPLinkDevicesMsg = "TP-Link Token is ${state.TpLinkToken}\n\r" +
		"Devices that have not been previously installed and are not in 'Local " +
        "WiFi control only' will appear below.  TAP below to see the list of " +
        "TP-Link devices available select the ones you want to connect to " +
        "SmartThings.\n\r\n\rPress DONE when you have selected the devices you " +
        "wish to add, thenpress DONE again to install the devices.  Press   <   " +
        "to return to the previous page."
	return dynamicPage(
		name: "selectDevices", 
		title: "Select Your TP-Link Devices", 
		install: true,
		uninstall: true) {
		section(errorMsg)
		section(TPLinkDevicesMsg) {
			input "selectedDevices", "enum",
			required:false, 
			multiple:true, 
			title: "Select Devices (${newDevices.size() ?: 0} found)",
			options: newDevices
		}
	}
}

def getDevices() {
	def currentDevices = getDeviceData()
    state.devices = [:]
	def devices = state.devices
	currentDevices.each {
		def device = [:]
		device["deviceMac"] = it.deviceMac
		device["alias"] = it.alias
		device["deviceModel"] = it.deviceModel
		device["deviceId"] = it.deviceId
		device["appServerUrl"] = it.appServerUrl
		devices << ["${it.deviceMac}": device]
		log.info "Device ${it.alias} added to devices array"
	}
}

def addDevices() {
	def tpLinkModel = [:]
	tpLinkModel << ["HS100" : "TP-LinkHS-Series"]
	tpLinkModel << ["HS105" : "TP-LinkHS-Series"]
	tpLinkModel << ["HS110" : "TP-LinkHS-Series"]
	tpLinkModel << ["HS200" : "TP-LinkHS-Series"]
	tpLinkModel << ["LB100" : "TP-LinkLB100-110"]
	tpLinkModel << ["LB110" : "TP-LinkLB110-110"]
	tpLinkModel << ["LB120" : "TP-LinkLB120"]
	tpLinkModel << ["LB130" : "TP-LinkLB130"]
	def hub = location.hubs[0]
	def hubId = hub.id
	selectedDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = state.devices.find { it.value.deviceMac == dni }
			def deviceModel = device.value.deviceModel.substring(0,5)
			addChildDevice(
				"beta",
				tpLinkModel["${deviceModel}"], 
				device.value.deviceMac, 
				hubId, [
					"label": device.value.alias,
			   		"name": device.value.deviceModel, 
					"data": [
						"deviceId" : device.value.deviceId,
						"appServerUrl": device.value.appServerUrl,
					]
				]
			)
			log.info "Installed TP-Link $deviceModel with alias ${device.value.deviceAlias}"
		}
	}
}

//	----- GET A NEW TOKEN FROM CLOUD -----
def getToken() {
	state.currentError = null

	def hub = location.hubs[0]
	def cmdBody = [
		method: "login",
		params: [
			appType: "Kasa_Android",
			cloudUserName: "${userName}",
			cloudPassword: "${userPassword}",
			terminalUUID: "${hub.id}"
		]
	]
    log.info "Sending token request with userName: ${userName} and userPassword:  ${userPassword}"
	def getTokenParams = [
		uri: "https://wap.tplinkcloud.com",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getTokenParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			state.TpLinkToken = resp.data.result.token
			log.info "TpLinkToken updated to ${state.TpLinkToken}"
	        sendEvent(name: "TokenUpdate", value: "getToken Successful")
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			log.error "Error in getToken: ${state.currentError}"
	        sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			log.error "Error in getToken: ${state.currentError}"
	        sendEvent(name: "TokenUpdate", value: "getToken Failure")
		}
	}
}

//	----- GET DEVICE DATA FROM THE CLOUD -----
def getDeviceData() {
	state.currentError = null
	def currentDevices = ""
	def cmdBody = [method: "getDeviceList"]
	def getDevicesParams = [
		uri: "https://wap.tplinkcloud.com?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			currentDevices = resp.data.result.deviceList
			return currentDevices
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			log.error "Error in getDeviceData: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			log.error "Error in getDeviceData: ${state.currentError}"
		}
	}
}

//	----- SEND DEVICE COMMAND TO CLOUD FOR DH -----
def sendDeviceCmd(appServerUrl, deviceId, command) {
	state.currentError = null
	def cmdResponse = ""
	def cmdBody = [
		method: "passthrough",
		params: [
			deviceId: deviceId, 
			requestData: "${command}"
		]
	]
	def sendCmdParams = [
		uri: "${appServerUrl}/?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(sendCmdParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			def jsonSlurper = new groovy.json.JsonSlurper()
			cmdResponse = jsonSlurper.parseText(resp.data.result.responseData)
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			cmdResponse = "ERROR: ${resp.statusLine}"
			log.error "Error in sendCmdParams: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			cmdResponse = "ERROR: ${resp.data.msg}"
			log.error "Error in sendCmdParams: ${state.currentError}"
		}
	}
	return cmdResponse
}

//	----- INSTALL, UPDATE, INITIALIZE -----
def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()
    runEvery5Minutes(checkToken)
    schedule("0 30 1 3/5 * ?", getToken)
	if (selectedDevices) {
		addDevices()
	}
}

//	----- PERIODIC CLOUD MX TASKS -----
def checkToken() {
	if (state.currentError != null) {
    	sendEvent(name: "TokenUpdate", value: "Updating from checkToken")
        log.error "checkToken attempting to update token due to error"
        getToken()
    }
}

//	----- CHILD CALLED TASKS -----
def removeChildDevice(alias, deviceNetworkId) {
	try {
		deleteChildDevice(it.deviceNetworkId)
	} catch (Exception e) {
		sendEvent(name: "DeviceDelete", value: "Failed to delete ${alias}")
	}
    sendEvent(name: "DeviceDelete", value: "${alias} deleted")
}