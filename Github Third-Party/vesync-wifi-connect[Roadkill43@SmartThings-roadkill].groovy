/**
 *  Wifi outlet
 *
 *  Copyright 2017 Marty
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
#### History #####
2017-12-1	Initial formal release
*/
definition(
    name: "VeSync Wifi Connect",
    namespace: "roadkill",
    author: "Marty Hall",
    description: "(unofficial) VeSync Wifi Connect",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
	singleInstance: true

preferences {
	page(name: "cloudLogin", title: "VeSync Cloud Login", nextPage:"", content:"cloudLogin")
	page(name: "selectDevices", title: "Select VeSync Devices", nextPage:"", content:"selectDevices", install: true)
   	page(name: "removePage")
}

def setInitialStates() {
	if (!state.Token) {state.Token = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
}

//	----- LOGIN PAGE -----
def cloudLogin() {
	setInitialStates()
	def cloudLoginText = "Enter your Username and Password for VeSync and the "+
		"action you want to complete."
	def errorMsg = ""
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r\n\r${state.currentError}" +
			"\n\r\n\rPlease resolve the error and try again.\n\r\n\r"
		}
   return dynamicPage(
		name: "cloudLogin", 
		title: "VeSync Device Manager", 
		nextPage: "selectDevices") {
            if (state.currentError != null){
                section(errorMsg)
            }
            section() {
            	paragraph "${cloudLoginText}"
                input( 
                    "userName", "text", 
                    title:"Account Email Address", 
                    required:true, 
                    displayDuringSetup: true
               )
                input(
                    "userPassword", "password", 
                    title:"Account password", 
                    required: true, 
                    displayDuringSetup: true
                )
                input(
                    "updateToken", "enum",
                    title: "What do you want to do?",
                    required: true, 
                    multiple: false,
                    options: ["Initial Setup", "Add Devices", "Update Token"]
                )
                paragraph "Current token: ${state.Token}"
            }
            section ("Remove VeSync Wifi Connect"){
        	href "removePage", description: "Tap to remove VeSync Wifi Connect", title: ""
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
		errorMsg = "not devices found"
		}
        
    log.info "Device count ${devices.size()}"
    //log.info devices.value
    state.totalDevices = devices.size()
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceId)
		if (!isChild) {
			newDevices["${it.value.deviceId}"] = "${it.value.deviceName}"
		}
	}

	settings.selectedDevices = null
	def DevicesMsg =" NOTE: Devices that have not been previously installed and are not in 'Local " +
		"WiFi control only' will appear below."
    def DeviceRemMsg = "To remove devices from Smartthings, use the 'Things' menu and remove within device settings"
	return dynamicPage(
		name: "selectDevices", 
		title: "Select Your Devices", 
		install: true) {
		section("Devices") {
        	paragraph "Total devices found ${state.totalDevices}"
            if( newDevices == [:]) {
                paragraph "No additional devices available to add."
            } else {
                paragraph "${DevicesMsg}"
                input "selectedDevices", "enum",
                required:false, 
                multiple:true, 
                title: "Available Devices (${newDevices.size() ?: 0} found)",
                options: newDevices
            }
		}
        section("Notes"){
        	paragraph "${DeviceRemMsg}"
		}
	
	}
}

// Remove page
def removePage() {
	dynamicPage(name: "removePage", title: "Remove VeSync Wifi Connect And All Devices", install: false, uninstall: true) {
    	section ("WARNING!\n\nRemoving VeSync Wifi Connect also removes all devices\n") {
        }
    }
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
	runEvery5Minutes(checkError)
	schedule("0 30 2 ? * WED", getToken)
	if (selectedDevices) {
    	//log.info "selected devices line 170 ${selectedDevices}"
		addDevices()
	}
    
}

//	----- GET A NEW TOKEN FROM CLOUD -----
def getToken() {
	def hub = location.hubs[0]
    def headers = [:]
    headers.put("Content-Type", "application/json")
    def hash = generateMD5(userPassword)
	def getTokenParams = [
    	uri: "https://smartapi.vesync.com/vold/user/login",
        HOST: "smartapi.vesync.com",
    	body: '{"account": "' + userName + '","password": "' + hash + '"}',
        requestContentType: "application/json",
		contentType: "application/json"
	]
    
    log.trace "Getting device token"

    //sendEvent(name: "token", value: data["tk"])
	httpPost(getTokenParams) {resp ->
       if (resp.status == 200 && resp.data.tk != null) {
			state.Token = resp.data.tk
            state.ID = resp.data.accountID
			log.info "Token updated to ${state.Token}, account id ${state.ID}"
			sendEvent(name: "TokenUpdate", value: "tokenUpdate Successful.")
			state.currentError = null
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		}
	}
}

//	----- PERIODIC CLOUD MX TASKS -----
def checkError() {
	if (state.currentError == null) {
    	log.info "VeSync did not have any set errors."
        return
    }
	def errMsg = state.currentError.msg
    log.info "Attempting to solve error: ${errMsg}"
    state.errorCount = state.errorCount +1
	if (errMsg == "Token expired" && state.errorCount < 6) {
	    sendEvent (name: "ErrHandling", value: "Handle comms error attempt ${state.errorCount}")
    	getDevices()
        if (state.currentError == null) {
        	log.info "getDevices successful.  apiServerUrl updated and token is good."
            return
        }
        log.error "${errMsg} error while attempting getDevices.  Will attempt getToken"
		getToken()
        if (state.currentError == null) {
        	log.info "getToken successful.  Token has been updated."
        	getDevices()
            return
        }
	} else {
		log.error "checkError:  No auto-correctable errors or exceeded Token request count."
	}
	log.error "checkError residual:  ${state.currentError}"
}

// Get the devices
def getDevices() {
	def currentDevices = getDeviceData()
	state.devices = [:]
	def devices = state.devices
	currentDevices.each {
        //log.info it
		def device = [:]
		device["deviceName"] = it.deviceName
		device["deviceId"] = it.cid
        device["deviceType"] = it.deviceType
        device["deviceStatus"] = it.deviceStatus
        device["deviceRelay"] = it.relay
		devices << ["${it.cid}": device]
		def isChild = getChildDevice(it.id)
		//log.info "Device ${it.deviceName} added to devices array"
        //log.info devices
	}

}

//	----- GET DEVICE DATA FROM THE CLOUD -----
def getDeviceData() {
	def currentDevices = ""
	def getDevicesParams = [
		uri: "https://smartapi.vesync.com/vold/user/devices",
		requestContentType: 'application/x-www-form-urlencoded',
		contentType: 'application/json',
		headers: ["tk":"${state.Token}"],
	]

	httpGet(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.devices != null) {
            // log.debug resp.data
			currentDevices = resp.data
			state.currentError = null
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

//Add the devices
def addDevices() {
	def hub = location.hubs[0]
	def hubId = hub.id
    def deviceModel = "My VeSync Wifi Connect switch"
    def deviceNamespace = "roadkill"
    // log.info "line 293 : ${selectedDevices}"
	selectedDevices.each {dni ->
		def isChild = getChildDevice(dni)
        //log.info isChild
        //log.info dni
		if (!isChild) {
			def device = state.devices.find { it.value.deviceId == dni }
			addChildDevice(
				deviceNamespace, 
                deviceModel,
				device.value.deviceId, 
				hubId, [
					"label": device.value.deviceName,
			   		"name": device.value.deviceName, 
					"data": [
						"deviceId" : device.value.deviceId,
                        "deviceType" : device.value.deviceType
					]
				]
			)
			log.info "Installed Wifi outlet with name ${device.value.deviceName}"
		}
	}
}

//	----- SEND DEVICE COMMAND TO CLOUD FOR DH -----
def sendDeviceCmd(deviceId, deviceType, command) {
	def cmdResponse = ""
    def devAction = ""
    def cmdType = ""
    switch(command){
    	case "turnOn":
        	//turn on
            log.info "VeSync Wifi Connect outlet : turn on"
            devAction = "on"
            cmdType = "devRequest"
        	break
        case "turnOff":
        	//turn off
            log.info "VeSync Wifi Connect outlet : turn off"
			devAction = "off"
            cmdType = "devRequest"
        	break 
        case "refresh":
        	//Refresh Data
            log.info "VeSync Wifi Connect outlet : refresh command"
            cmdType = "refresh"
            break
        case "getStatus":
        	//Get power stats
            log.info "VeSync Wifi Connect outlet : get power stats command"
            cmdType = "getStatus"
            break

    }
    
    if (cmdType == "devRequest") {
        log.info "devRequest, ${devAction}, ${deviceId}, ${deviceType}"
    	// Device Request actions
        log.trace "Action: ${devAction}"
    	webPut("/v1/${deviceType}/${deviceId}/status/${devAction}")
        def data = webGet("/v1/device/${deviceId}/detail")
        cmdResponse = data
        if (cmdResponse == ""){
        	cmdResponse = "ERROR refreshing data"
            log.error "Error in sendDeviceCmd on data refresh"
        }
        
    } else if (cmdType == "getStatus"){
    	// Device Request actions
        def localResp = [:]
        def data = webGet("/v1/device/${deviceId}/detail")
        cmdResponse = data
        log.info "getStatus Resp ${cmdResponse}"
        if (cmdResponse == ""){
        	cmdResponse = "ERROR refreshing data"
            log.error "Error in sendDeviceCmd on data refresh"
        }
        localResp.power = parseNumeric(data["power"]);
        localResp.voltage = parseNumeric(data["voltage"]);
        localResp.current = parseNumeric(data["energy"]);
  
        //localResp = getEnergyStatus(deviceId,localResp)
        log.info "Local Resp ${localResp}"
        cmdResponse = localResp
	
      
        
    } else if ( cmdType == "refresh" ) {
        def localResp = [:]
        
        def data = webGet("/v1/device/${deviceId}/detail")
        cmdResponse = data
       // log.debug "refresh cmdResponse: ${cmdResponse}"
       
        if (cmdResponse == ""){
        	cmdResponse = "ERROR refreshing data"
            log.error "Error in sendDeviceCmd on data refresh"
        }
        localResp.power = parseNumeric(cmdResponse.power)
        localResp.voltage = parseNumeric(cmdResponse.voltage)
        localResp.current = cmdResponse.energy
        localResp.Status = cmdResponse.deviceStatus
        //log.debug "refresh localResp: ${localResp}"
        cmdResponse = localResp
    }
    
   
	return cmdResponse
}

// parse the result of the current
def parseNumeric(input) {
        def split = input.split(':')
        def localData
        def v_stat = Long.parseLong(split[0],16)
        def v_imm = Long.parseLong(split[1],16)
        localData = (v_stat >> 12) + ( (4095 & v_stat) / 1000.0 )
      //  localData.instant = (v_imm >> 12) + ( (4095 & v_imm) / 1000.0 )
        //log.info "data ${localData}"
        return localData;
    }
    
//Get the energy usage
def getEnergyStatus(deviceId,localResp) {

        def sendCmdParams = [
                        uri: "https://server1.vesync.com:4007/loadStat",
                        contentType: 'application/json',
                        headers: ["tk":"${state.Token}","id":"${state.ID}"],
                        body: "{\"cid\":\"${deviceId}\",\"type\":\"extDay\",\"date\":\"${new Date().format('yyyyMMdd',location.timeZone)}\"}"

                    ]
         
            httpPostJson(sendCmdParams) {resp ->
            log.info "response ${resp.data}"
              if (resp.status == 200 && resp.data.cuurentDay != null) {
                  if(resp.data.power != 'NaN'){
                        localResp.energyDay = _round(resp.data.cuurentDay);
                    }
                    if(resp.data.voltage != 'NaN'){
                        localResp.energy7 = _round(resp.data.sevenDay);
                    }
                    if(resp.data.current != 'NaN'){
                        localResp.energy30 = _round(resp.data.thirtyDay);
                    } 
              }
            }
		return localResp
}

def _round(value) {
        return Math.round(value / 3600 * 1000) / 1000;
    }



import java.security.MessageDigest

def generateMD5(String s){
    return MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex()
}

//  webPut command
private webPut(path) {

	def headers = [
    	"accountId": state.Id,
        "tk": state.Token
    ]

	def params = [
	    uri: "https://smartapi.vesync.com",
	    path: path,
        headers: headers
	]
    
    log.trace "Putting with ${params}"
	
	try {
	    httpPut(params) { resp ->
	        	resp.headers.each {
	    	}
	        //log.trace "put response: ${resp.data}"
	    	return resp.data
        }
	} catch (e) {
	    log.error "something went wrong: $e"
	}
}
// WebGet Command
private webGet(path) {

	def headers = [
    	"accountId": state.Id,
        "tk": state.Token
    ]

	def params = [
	    uri: "https://smartapi.vesync.com",
	    path: path,
        headers: headers
	]
    
    log.trace "Getting with ${params}"
	
	try {
	    httpGet(params) { resp ->
	        	resp.headers.each {
	        	log.debug "Get response header: ${it.name} : ${it.value}"
	    	}
	    	log.debug "Get response data: ${resp.data}"
	    
	    	return resp.data
        }
	} catch (e) {
	    log.error "something went wrong: $e"
	}
}
