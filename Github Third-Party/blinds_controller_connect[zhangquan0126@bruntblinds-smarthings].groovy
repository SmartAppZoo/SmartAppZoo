/**
 *  Blinds Controller
 *
 *  Copyright 2018 Quan Zhang
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
    name: "Blinds Controller Connect (Unofficial)",
    namespace: "zhangquan0126",
    author: "Quan Zhang",
    description: "A Service Manager for the Blinds Controller Device connecting through the Cloud",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)

preferences {
    page(name: "cloudLogin", title: "Cloud Login", nextPage:"", content:"cloudLogin", uninstall: true)
    page(name: "selectDevices", title: "Select Devices", nextPage:"", content:"selectDevices", uninstall: true, install: true)
}

def setInitialStates() {
	if (!state.sessionid) {state.sessionid = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
}

//	----- LOGIN PAGE -----
def cloudLogin() {
	setInitialStates()
	def cloudLoginText = "If possible, open the IDE and select Live Logging.  THEN, " +
		"enter your Username and Password for and the "+
		"action you want to complete.  Your current session:\n\r\n\r${state.SessionId}" +
		"\n\r\n\rAvailable actions:\n\r" +
		"   Initial Install: Obtains token and adds devices.\n\r" +
		"   Add Devices: Only add devices.\n\r"
	def errorMsg = ""
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r\n\r${state.currentError}" +
			"\n\r\n\rPlease resolve the error and try again.\n\r\n\r"
		}
        return dynamicPage(
		name: "cloudLogin", 
		title: "Blinds Controller Device Service Manager", 
		nextPage: "selectDevices", 
		uninstall: true) {
		section(errorMsg)
		section(cloudLoginText) {
			input( 
				"userName", "string", 
				title:"Your Email Address", 
				required:true, 
				displayDuringSetup: true
		   )
			input(
				"userPassword", "password", 
				title:"password", 
				required: true, 
				displayDuringSetup: true
			)
			input(
				"action", "enum",
				title: "What do you want to do?",
				required: true, 
				multiple: false,
				options: ["Initial Install", "Add Devices"]
			)
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	if (action != "Add Devices") {
		getSessionId()
	}
	if (state.currentError != null) {
		return cloudLogin()
	}
	getDevices()
	def devices = state.devices
	if (state.currentError != null) {
		return cloudLogin()
	}
	def errorMsg = ""
	if (devices == [:]) {
		errorMsg = "There were no devices from Cloud.  This usually means "+
			"that all devices are in 'Local Control Only'.  Correct then " +
			"rerun.\n\r\n\r"
		}
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.serial)
		if (!isChild) {
			newDevices["${it.value.serial}"] = "${it.value.name} model ${it.value.deviceModel}"
		}
	}
	if (newDevices == [:]) {
		errorMsg = "No new devices to add.  Are you sure they are in Remote " +
			"Control Mode?\n\r\n\r"
		}
	def DevicesMsg = "Session Id is ${state.sessionid}\n\r" +
		"Devices that have not been previously installed and are not in 'Local " +
		"WiFi control only' will appear below.  TAP below to see the list of " +
		"devices available select the ones you want to connect to " +
		"SmartThings.\n\r\n\rPress DONE when you have selected the devices you " +
		"wish to add, thenpress DONE again to install the devices.  Press   <   " +
		"to return to the previous page."
	return dynamicPage(
		name: "selectDevices", 
		title: "Select Your Devices", 
		install: true,
		uninstall: true) {
		section(errorMsg)
		section(DevicesMsg) {
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
        log.debug "Device ${it}"
        def map = new JsonSlurper().parseText(it.toString())
		device["timestamp"] = map["TIMESTAMP"][0]
		device["name"] = map["NAME"][0]
        device["serial"] = map["SERIAL"][0]
		device["deviceModel"] = map["MODEL"][0]
        device["thingUri"] = map["thingUri"][0]
        devices << ["${map["SERIAL"][0]}": device]
		log.info "Device ${map["NAME"][0]} added to devices array"
	}
}

def addDevices() {
    def hub = location.hubs[0]
    def hubId = hub.id
    log.debug "selectedDevices: ${selectedDevices}"
    if (selectedDevices instanceof String) {
    	def isChild = getChildDevice(selectedDevices)
        if (!isChild) {
            log.debug "devices: ${state.devices}"
            def device = state.devices.find { it.key == selectedDevices }
            log.debug "device: ${device}, value: ${device.value}"
            def deviceModel = device.value.deviceModel
            log.debug "device serial ${device.value.serial}"
            addChildDevice(
                "zhangquan0126",
                "Blinds Controller", 
                device.value.serial,
                hubId, [
                    "label": device.value.name,
                    "name": device.value.deviceModel, 
                    "data": [
                        "deviceId" : device.value.serial,
                        "appServerUrl": device.value.thingUri,
                        "sessionId": state.sessionid
                    ]
                ]
            )
            log.info "Installed ${deviceModel} with name ${device.value.name}"
        }
    }
    else {
        selectedDevices.each { dni ->
            log.debug "dni: ${dni}"
            def isChild = getChildDevice(dni)
            if (!isChild) {
                log.debug "devices: ${state.devices}"
                def device = state.devices.find { it.key == dni }
                log.debug "device: ${device}"
                log.debug "device serial ${device.value.serial}"
                def deviceModel = device.value.deviceModel
                addChildDevice(
                    "zhangquan0126",
                    "Blinds Controller", 
                    device.value.serial,
                    hubId, [
                        "label": device.value.name,
                        "name": device.value.deviceModel, 
                        "data": [
                            "deviceId" : device.value.serial,
                            "appServerUrl": device.value.thingUri,
                            "sessionId": state.sessionid
                        ]
                    ]
                )
                log.info "Installed ${deviceModel} with name ${device.value.name}"
            }
        }
    }
}

//	----- GET A NEW SESSION FROM CLOUD -----
def getSessionId() {
	def cmdBody = [
        ID: "${userName}",
        PASS: "${userPassword}",
	]
	def getTokenParams = [
		uri: "https://sky.brunt.co/session",
		headers: ["Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Origin": "https://sky.brunt.co",
            "Accept-Language": "en-gb",
            "Accept": "application/vnd.brunt.v1+json",
            "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216"],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPost(getTokenParams) {resp ->
		if (resp.status == 200) {
            log.debug "resp.cookie is ${resp.getHeaders('Set-Cookie')}"
            resp.getHeaders('Set-Cookie').each {
                state.sessionid = it.value.split(';')[0].split('=')[1]
                log.info "SessionId updated to ${state.sessionid}"
			    sendEvent(name: "SessionIdUpdate", value: "sessionIdUpdate Successful.")
			    state.currentError = null
            }
		} else {
			state.currentError = resp.statusLine
			log.error "Error in getSessionId: ${state.currentError}"
			sendEvent(name: "SessionIdUpdate", value: state.currentError)
        }
	}
}

def getDeviceData() {
	def currentDevices = ""
	def getDevicesParams = [
		uri: "https://sky.brunt.co/thing",
		headers: ["Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Origin": "https://sky.brunt.co",
            "Accept-Language": "en-gb",
            "Accept": "application/vnd.brunt.v1+json",
            "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216",
            "Cookie": "skySSEIONID=${state.sessionid}"],
		body: ""
	]
	httpGet(getDevicesParams) {resp ->
		if (resp.status == 200) {
            log.info "resp.data is ${resp.data}"
			currentDevices = resp.data
			state.currentError = null
			return currentDevices
		} else {
			state.currentError = resp.statusLine
			log.error "Error in getDeviceData: ${state.currentError}"
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    unsubscribe()
	unschedule()
	runEvery5Minutes(checkError)
	schedule("0 30 2 ? * WED", getToken)
	if (selectedDevices) {
		addDevices()
	}
}

// TODO: implement event handlers

def sendDeviceCmd(appServerUrl, deviceId, sessionId, state) {
    log.debug "uri: https://thing.brunt.co:8080/thing${appServerUrl}"
	def cmdResponse = ""
	def cmdBody =  
           [
               "requestPosition": "${state}"
           ]
    if (state == -1) {
        def sendCmdParams = [
            uri: "https://thing.brunt.co:8080/thing${appServerUrl}",
            headers: ["Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Origin": "https://sky.brunt.co",
                "Accept-Language": "en-gb",
                "Accept": "application/vnd.brunt.v1+json",
                "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216",
                "Cookie": "skySSEIONID=${sessionId}"],
            body : "" 
        ]
        httpGet(sendCmdParams) {resp ->
            if (resp.status == 200) {
                log.debug "resp.data is ${resp.data}"
                def jsonSlurper = new JsonSlurper()
                cmdResponse = jsonSlurper.parseText(new String(resp.data.buf))
            } else {
                state.currentError = resp.statusLine
                cmdResponse = "ERROR: ${resp.statusLine}"
                log.error "Error in sendDeviceCmd: ${state.currentError}"
            }
        }
    }
    else {
       def sendCmdParams = [
            uri: "https://thing.brunt.co:8080/thing${appServerUrl}",
            headers: ["Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "Origin": "https://sky.brunt.co",
                "Accept-Language": "en-gb",
                "Accept": "application/vnd.brunt.v1+json",
                "User-Agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216",
                "Cookie": "skySSEIONID=${sessionId}"],
            body : new groovy.json.JsonBuilder(cmdBody).toString() 
        ]
        httpPut(sendCmdParams) {resp ->
            if (resp.status == 200) {
                cmdResponse = resp.data
            } else {
                state.currentError = resp.statusLine
                cmdResponse = "ERROR: ${resp.statusLine}"
                log.error "Error in sendDeviceCmd: ${state.currentError}"
            }
        }
    }
	
	return cmdResponse
}

//	----- CHILD CALLED TASKS -----
def removeChildDevice(alias, deviceNetworkId) {
	try {
		deleteChildDevice(it.deviceNetworkId)
		sendEvent(name: "DeviceDelete", value: "${alias} deleted")
	} catch (Exception e) {
		sendEvent(name: "DeviceDelete", value: "Failed to delete ${alias}")
	}
}