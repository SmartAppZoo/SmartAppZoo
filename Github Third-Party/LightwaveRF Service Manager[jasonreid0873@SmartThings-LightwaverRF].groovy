/**
 *  LightwaveRF API Service Manager
 *
 *  Copyright 2019 Jason Reid
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
		name: "LightwaveRF Service Manager",
		namespace: "jay0873",
		author: "jay0873",
		description: "This is a Service Manager SmartApp for use with the LightwaveRF RESTful API",
		category: "My Apps",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage")
    page(name: "checkApiConnectionPage")
    page(name: "discoveryPage")
    page(name: "addDevicesPage")
    //page(name: "configureDevicePage")
    //page(name: "saveDeviceConfigurationPage")
    //page(name: "updateServiceManagerPage")
}


//
//SmartApp Pages
//
def mainPage() {
    if(state.latestHttpResponse){state.latestHttpResponse = null;}
    dynamicPage(name: "mainPage", title: "Manage your LightwaveRF API devices", nextPage: null, uninstall: true, install: true) {
        section("Configure LightwaveRF API"){
            input "apiHostAddress", "string", title: "API host address", required: true
            href "checkApiConnectionPage", title: "Test API connection", description:""
        }
        section("Configure Devices"){
            input(name: "settingsLogLevel", type: "enum", title: "Service manager log level", options: [0, 1, 2, 3, 4])
            href "discoveryPage", title:"Discover Devices", description:""//, params: [pbutton: i]
        }
        section("Installed Devices"){
            def dMap = [:]
            getChildDevices().sort({ a, b -> a["label"] <=> b["label"] }).each {
                it.getChildDevices().sort({ a, b -> a["label"] <=> b["label"] }).each {
                    logger('debug', "mainPage(), it.label: "+it.label+", it.deviceNetworkId: "+it.deviceNetworkId)
                    href "configureDevicePage", title:"$it.label", description:"", params: [dni: it.deviceNetworkId]
                }
            }
        }
    }
}

def checkApiConnectionPage() {
    dynamicPage(name:"checkApiConnectionPage", title:"Test API connection", nextPage: "mainPage", refreshInterval:10) {
        getDevices() //TODO: get root and only check status
        logger('debug', "checkApiConnectionPage(), refresh")
        
        section("Please wait for the API to answer, this might take a couple of seconds.") {
            if(state.latestHttpResponse) {
                if(state.latestHttpResponse==200) {
                    paragraph "Connected \nOK: 200"
                } else {
                    paragraph "Connection error \nHTTP response code: " + state.latestHttpResponse
                }
            }
        }
    }
}

def discoveryPage() {
    dynamicPage(name:"discoveryPage", title:"Discovery Started!", nextPage: "addDevicesPage", refreshInterval:10) {
        getDevices()
        logger('debug', "discoveryPage(), refresh")
        
        section("Please wait while we discover your LightwaveRF devices. Select your devices below once discovered.") {
            if(state.devicesMap!=null && state.devicesMap.size()>0) {
                input "selectedDevices", "enum", required:false, title:"Select LightwaveRF Device ("+ state.devicesMap.size() +" found)", multiple:true, options: state.devicesMap
                //state.selectedDevicesMap = state.devicesMap
            } else {
                input "selectedDevices", "enum", required:false, title:"Select LightwaveRF Device (0 found)", multiple:true, options: [:]
                //state.selectedDevicesMap = null
            }
        }
        
        //state.latestDeviceMap = null
    }
}

def addDevicesPage() {
    def addedDevices = addDevices(selectedDevices)
    
    dynamicPage(name:"addDevicesPage", title:"LightwaveRF Devices Added", nextPage: null, uninstall: false, install: true) {
        section("Devices added") {
            if( !addedDevices.equals("0") ) {
                addedDevices.each{ key, value ->
                    paragraph title: "Slot: " + key, "" + "Device Type: " + value.toString()
                }
            } else {
                paragraph "No devices added."
            }
        }
    }
}


//
// System Functions
//
def installed() {
    logger('debug', "Installed with settings: ${settings}")

    initialize()
}

def updated() {
    logger('debug', "Updated with settings: ${settings}")

    unsubscribe()
    initialize()
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
}


//
// lwrfAPicore Device Discovery
//
def addDevices(selectedDevices) {
    def addedDevices = [:]
    logger('debug', "selectedDevices: "+selectedDevices+" childDevices: " + getChildDevices().size() )
        
    if(selectedDevices && selectedDevices!=null) {
        
        if(getChildDevices().size()<1) {
            logger('debug', "No LightwaveRF devices installed" )
            
            if(state.latestHttpMac) {
                logger('debug', "addChildDevice HttpMac: " + state.latestHttpMac + " apiHostAddress: " + apiHostAddress + ":8100")
                addChildDevice("jreid0873", "lwrfAPIcore", ""+state.latestHttpMac, location.hubs[0].id, [
                    "label": "LightwaveRF API",
                    "data": [
                        "apiHost": apiHostAddress + ":8100",
                        "devices": "[]"
                    ]
                ])
            } else {
                addedDevices.put('Error', "The LightwaveRF API doesn't return it's MAC address. No devices were added.")
            }
        }
        
        selectedDevices.each { key ->
            logger('info', "Selected device: " + state.devicesMap[key] )
            addedDevices.put(key, state.devicesMap[key])
        }
        
        getChildDevices().each {
            logger('info', "addedDevices: " + addedDevices)
            it.updateDataValue("devices", ""+addedDevices)
            it.updated()
        }
    }
    
    if(addedDevices==[:]) {
        return "0"
    } else {
        return addedDevices
    }
}

def getDevices() {
    logger('debug', "Executing 'getDevices'")
    sendHttpRequest(apiHostAddress + ":8100", '/api/Device')
}

def sendHttpRequest(String host, String path) {
    logger('debug', "Executing 'sendHttpRequest' host: "+host+" path: "+path)
    sendHubCommand(new physicalgraph.device.HubAction("""GET ${path} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: hubResponseReceived]))
}

void hubResponseReceived(physicalgraph.device.HubResponse hubResponse) {
    parse(hubResponse.description)
}

def parse(description) {
    logger('debug', "Parsing '${description}'")
    
    def msg, json, status, mac
    try {
        msg = parseLanMessage(description)
        status = msg.status
        json = msg.json
        mac = msg.mac
    } catch (e) {
        logger("error", "Exception caught while parsing data: "+e)
        return null;
    }
  
    state.latestHttpResponse = status
    state.latestHttpMac = mac
    if(status==200){
        //def length = 0
        logger('debug', "JSON rcvd: "+json+", JSON.size: "+json.size)
        
        def devices = [:]
        for(int i=0; i<json.size; i++) {
            devices.put(json[i]['slot'] as int, "serial=" + json[i]['serial'] + "&prod=" + json[i]['prod'])
        }
       
        logger('debug', "Discovered Devices: " + devices)
        state.devicesMap = devices
    } else {
        state.devicesMap = [:]
    }
}


//
//Logging Levels
//
def logger(level, message) {
    def smLogLevel = 0
    if(settingsLogLevel) {
        smLogLevel = settingsLogLevel.toInteger()
    }
    if(level=="error"&&smLogLevel>0) {
        log.error message
    }
    if(level=="warn"&&smLogLevel>1) {
        log.warn message
    }
    if(level=="info"&&smLogLevel>2) {
        log.info message
    }
    if(level=="debug"&&smLogLevel>3) {
        log.debug message
    }
}