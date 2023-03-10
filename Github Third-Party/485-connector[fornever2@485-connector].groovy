/**
 *  485-connector
 *
 *  Copyright 2020 fornever2@gmail.com
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
    name: "485-connector",
    namespace: "fornever2",
    author: "fornever2@gmail.com",
    description: "A Connector between RS485 Homenet and SmartThings",
    category: "My Apps",
    iconUrl: "https://www.shareicon.net/data/256x256/2016/01/19/705449_connection_512x512.png",
    iconX2Url: "https://www.shareicon.net/data/256x256/2016/01/19/705449_connection_512x512.png",
    iconX3Url: "https://www.shareicon.net/data/256x256/2016/01/19/705449_connection_512x512.png"
)


preferences {
    page(name: "settingPage")
    page(name: "connectingPage")
}

def installed() {
	log.debug "Installed with settings: ${settings}"

    // Create token
    if (!state.accessToken) {
    	log.debug "creating token..."
        createAccessToken()
    }
    
    // 485 서버에 token 정보등을 보내서 서버에 STInfo 파일 생성
    def options = [
     	"method": "POST",
        "path": "/smartthings/installed",
        "headers": [
        	"HOST": settings.serverAddress,
            "Content-Type": "application/json"
        ],
        "body":[
            "app_url":"${apiServerUrl}/api/smartapps/installations/",
            "app_id":app.id,
            "access_token":state.accessToken
        ]
    ]    
    log.debug "Sending intialize info - ${options}"    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
        
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	// Unsubscribe from all events
	unsubscribe()
    
    // Subscribe to stuff
	initialize()
}

def uninstalled() {
	log.debug "uninstalled()"
    
    // 485 서버에 uninstall 된 것을 보내서 기존 STInfo 파일 삭제
    def options = [
     	"method": "POST",
        "path": "/smartthings/uninstalled",
        "headers": [
        	"HOST": settings.serverAddress,
            "Content-Type": "application/json"
        ]
    ]    
    log.debug "Sending uninstalled info - ${options}"    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
}

def childUninstalled() {
	// TODO : check call if child device is deleted.
	log.debug "childUninstalled()"
}
//////////////////////////////////////////////////////////

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "initialize() called..."
}

//////////////////////////////////////////////////////////

def settingPage(){	
	state.addedCountNow = 0
	state.curStatus = "setting"
    state.dniHeaderStr = "485-connector-"
	dynamicPage(name:"settingPage", title:"Settings", nextPage: "connectingPage", uninstall: true) {
        section("Label") {
        	label name: "label", title:"You can change the name of this smartapp", required: false, multiple: false, description: name
        }
		section("RS485 server setting") {
        	paragraph "Please input RS485 server's local IP address including port number."
        	input "serverAddress", "text", title: "IP address (ex. 192.168.29.101:8080)", required: true, value: "192.168.29.101:8080"
        }
	}
}

def connectingPage(){
    log.debug "connectingPage() - settings.serverAddress : ${settings.serverAddress}, state.curStatus : ${state.curStatus}"
    
    if (state.curStatus == "setting") {
    	state.curStatus = "connecting"
        getConnectorStatus()
    } 
    
    if (state.curStatus == "setting" || state.curStatus == "connecting") {
        dynamicPage(name:"connectingPage", title:"Connecting", refreshInterval:1) {
			section("Connecting") {
        		paragraph "Trying to connect ${settings.serverAddress}\nPlease wait...."        	
        	}
		}        
    } else if (state.curStatus == "connected") {
        dynamicPage(name:"connectingPage", title:"Connected", install: true, uninstall: true) {
			section("Connected") {
        		paragraph "Connected to ${settings.serverAddress}"
                paragraph "Added Count : " + state.addedCountNow
        	}
		}
    }
}

def getConnectorStatus() {	
    def options = [
     	"method": "GET",
        "path": "/homenet",
        "headers": [
        	"HOST": settings.serverAddress,
            "Content-Type": "application/json"
        ]
    ]
    log.debug "getConnectorStatus() - sendHubCommand : ${options}"
    sendHubCommand(new physicalgraph.device.HubAction(options, null, [callback: connectorCallback]))
}

def connectorCallback(physicalgraph.device.HubResponse hubResponse){
	//log.debug "connectorCallback() called..."
	def msg, status, json
    try {
        msg = parseLanMessage(hubResponse.description)        
        def jsonObj = msg.json
        log.debug "connectorCallback() - response : ${jsonObj}"
        def count = 0
        jsonObj.each{ item->
            def dni = state.dniHeaderStr + item.id.toLowerCase()
            log.debug "dni : ${dni}, item : ${item}"
            if(!getChildDevice(dni)){
            	try{
                	def typeName
                	if (item.type == "light") {
						typeName = "485-light"
                    } else if (item.type == "thermostat") {
                    	typeName = "485-thermostat"
                    }
                    
                    def childDevice = addChildDevice("fornever2", typeName, dni, location.hubs[0].id, [
                    	"label": item.id,
                        "uri": item.uri
                    ])                    
                    childDevice.init()
                    childDevice.setUrl("${settings.serverAddress}")
                    childDevice.setPath("/homenet/${item.id}")
                    childDevice.refresh()
                    
                    state.addedCountNow = (state.addedCountNow.toInteger() + 1)
                    log.debug "[addChildDevice] - typeName:${typeName}, dni:${dni}, label:${label}"
                }catch(e){
                	log.error("ADD DEVICE Error!!! ${e}")
                }
            }
        }
        state.curStatus = "connected"
        log.debug "connected"
	} catch (e) {
        log.error("Exception caught while parsing data: "+e);
    }
}


///////////////////////////////////////

def updateProperty(){
    log.debug "updateProperty - id: ${params.id}, property: ${params.property}, value: ${params.value}"
    //def data = request.JSON
    def dni = state.dniHeaderStr + params.id.toLowerCase()
    def chlidDevice = getChildDevice(dni)
    if(chlidDevice){
		chlidDevice.updateProperty(params.property, params.value)
        def resultString = new groovy.json.JsonOutput().toJson("result":true)
    	render contentType: "text/plain", data: resultString   
    } else {
    	log.error "Device not found - dni : ${dni}"
        httpError(501, "Device not found - dni : ${dni}")
    }
}

def authError() {
    [error: "Permission denied"]
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/updateProperty/:id/:property/:value")    { action: [POST: "authError"]  }
    } else {
        path("/updateProperty/:id/:property/:value")    { action: [POST: "updateProperty"]  }
    }
}