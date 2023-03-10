/**
 *  VeSync Smartapp
 *
 *  Copyright 2020 Chris Weber
 *
 *  Inspired by:
 *    https://github.com/hongtat/tasmota-connect
 *    Copyright 2020 AwfullySmart.com - HongTat Tan
 *    (GPL 3.0 License)
 *
 *    https://github.com/nicolaskline/smartthings
 *    (Specifically etekcity-plug.src)
 *    Author/Copyright: nkline
 *    (Unknown License)
 *
 *    https://github.com/projectskydroid/etekcity_inwallswitch_api
 *    (Apache License)
 *
 *    https://github.com/webdjoe/pyvesync_v2
 *    (MIT License)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
String appVersion() { return "0.0.1" }

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Field
definition(
    name: "VeSync SmartThings",
    namespace: "oschrich",
    author: "oschrich",
    description: "Allows you to integrate your VeSync devices with SmartThings.",
    iconUrl: "https://oschrich.github.io/pub/smartthings/vesync-180.png",
    iconX2Url: "https://oschrich.github.io/pub/smartthings/vesync-180.png",
    iconX3Url: "https://oschrich.github.io/pub/smartthings/vesync-180.png",
    singleInstance: true,
    pausable: false
)

preferences {
    page(name: "mainPage", nextPage: "", uninstall: true, install: true)
    page(name: "configuredDevice")
    page(name: "unconfiguredDevice")
    page(name: "orphanDevice")
    page(name: "deleteDeviceConfirm")
    page(name: "addDeviceConfirm")
    page(name: "popDevices")
//    page(name: "addDevice")
}

def mainPage() {
    if (state?.install) {
        dynamicPage(name: "mainPage", title: "VeSync Connector - v${appVersion()}") {
            section(){
                href "popDevices", title:"Refresh Devices", description:"Refresh device list from VeSync"
            }

            section("Configured Devices"){
          
                // name, cid, type, model, dni
                getConfiguredDevices().each {
                    String titleText = it["name"]
                    String descText = "More..."
                    href "configuredDevice", title:it["name"], description: "More...", params: [dev: it]
                }
            }
            section("Unconfigured Devices"){
          
                // name, cid, type, model, dni
                getUnconfiguredDevices().each {
                    String titleText = it["name"]
                    String descText = "Add..."
                    href "unconfiguredDevice", title:it["name"], description: "Add...", params: [dev: it]
                }
            }

            section("Orphan Devices"){
                getOrphanDevices().each {
                    String titleText = it["name"]
                    String descText = "Remove..."
                    href "orphanDevice", title:it["name"], description: "Remove...", params: [dev: it]
                }
            }
            section(title: "Settings") {
                input("username", "text",
                        title: "VeSync Email Address",
                        description: "Email Address associated with your VeSync Account",
                        required: false, submitOnChange: false)
                input("password", "password",
                        title: "VeSync Password",
                        description: "Password for your VeSync Account",
                        required: false, submitOnChange: false)
                input("dateformat", "enum",
                        title: "Date Format",
                        description: "Set preferred date format",
                        options: ["MM/dd/yyyy h:mm", "MM-dd-yyyy h:mm", "dd/MM/yyyy h:mm", "dd-MM-yyyy h:mm"],
                        defaultValue: "MM/dd/yyyy h:mm",
                        required: false, submitOnChange: false)
                input("frequency", "enum",
                        title: "Device Health Check",
                        description: "Check in on device health every so often",
                        options: ["Every 1 minute", "Every 5 minutes", "Every 10 minutes", "Every 15 minutes", "Every 30 minutes", "Every 1 hour"],
                        defaultValue: "Every 5 minutes",
                        required: false, submitOnChange: false)
            }
            remove("Remove (Includes Devices)", "This will remove all devices.")
        }
    } else {
        dynamicPage(name: "mainPage", title: "VeSync+SmartThings Connector") {
            section {
                paragraph "Success!"
            }
        }
    }
}


def popDevices(){
    log.trace "popDevices"

    setAccountToken()

    getDevices()

    dynamicPage(name: "popDevices", title: "Populate Devices", nextPage: "mainPage") {
        section {
            paragraph "The token has been added."
        }
    }

}

//def addDevice(params){
//    def t = params?.name
//    if (params?.name) {
//        atomicState?.curPageParams = params
//    } else {
//        t = atomicState?.curPageParams?.name
//    }
//
//    log.trace "addDevice ${t}"
//
//    //def newDev = addChildDevice("oschrich", "Vesync Plug", "vesync-connect-${t}")
//
//    return dynamicPage(name: "addDevice", title: "Added ${t}", nextPage: "mainPage") {
//        section(t) {
//            paragraph "The token has been added."
//        }
//    }
//}


def configuredDevice(params){
    def name = params.dev["name"]
    def type = params.dev["type"]
    def model = params.dev["model"]
    def dni = params.dev["dni"]
    def cid = params.dev["cid"]

    log.trace "configuredDevice: ${name} / ${type} / ${model} / ${dni} / ${cid}"

    dynamicPage(name: "configuredDevice", install: false, uninstall: false, nextPage: "mainPage") {
        section(name) {
            paragraph "Name: ${name}"
            paragraph "Type: ${type}"
            paragraph "Model: ${model}"
            paragraph "Device Network ID: ${dni}"
            paragraph "CID: ${cid}"
    }

        section("Remove Device from SmartThings", hideable: true, hidden: true) {
            href "deleteDeviceConfirm", title:"Remove Device", description: "Tap here to delete this device.", params: [dni: dni]
        }
    }
}

def unconfiguredDevice(params){
    def name = params.dev["name"]
    def type = params.dev["type"]
    def model = params.dev["model"]
    def dni = params.dev["dni"]
    def cid = params.dev["cid"]

    log.trace "unconfiguredDevice: ${name} / ${type} / ${model} / ${dni} / ${cid}"

    dynamicPage(name: "unconfiguredDevice", install: false, uninstall: false, nextPage: "mainPage") {
        section(name) {
            paragraph "Name: ${name}"
            paragraph "Type: ${type}"
            paragraph "Model: ${model}"
            paragraph "Device Network ID: ${dni}"
            paragraph "CID: ${cid}"
    }

        section("Add Device to SmartThings") {
            href "addDeviceConfirm", title:"Add Device", description: "Tap here to add this device.", params: [dni: dni]
        }
    }
}

def orphanDevice(params) {
    def d = params.dev

    def name = params.d["name"]
    def dni = d.getDeviceNetworkId()

    log.trace "orphanDevice: ${name} / ${dni}"

    dynamicPage(name: "orphanDevice", install: false, uninstall: false, nextPage: "mainPage") {
        section(name) {
            paragraph "Name: ${name}"
            paragraph "Device Network ID: ${dni}"
    }
        section("Remove Device from SmartThings", hideable: true, hidden: true) {
            href "deleteDeviceConfirm", title:"Remove Device", description: "Tap here to delete this device.", params: [dni: dni]
        }
    }
}

def addDeviceConfirm(params){
    def dni = params.dni
    try {

        def newDev = addChildDevice("oschrich", "Etekcity Plug", "${dni}")

        //newDev.sendEvent(name: "device_id", value: null)

        dynamicPage(name: "addDeviceConfirm", title: "", nextPage: "mainPage") {
            section {
                paragraph "The device has been added."
            }
        }
    } catch (e) {
        dynamicPage(name: "addDeviceConfirm", title: "Addition Summary", nextPage: "mainPage") {
            section {
                paragraph "Error: ${(e as String).split(":")[1]}."
            }
        }
    }
}


def deleteDeviceConfirm(params){
    def dni = params.dni

    log.trace "deleteDeviceConfirm: ${dni}"
    try {
        def d = getChildDevice(dni)
        unsubscribe(d)
        deleteChildDevice(dni, true)
        //deleteChildSetting(d.id)
        dynamicPage(name: "deleteDeviceConfirm", title: "", nextPage: "mainPage") {
            section {
                paragraph "The device (${dni}) has been deleted."
            }
        }
    } catch (e) {
        dynamicPage(name: "deleteDeviceConfirm", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "Error: ${(e as String).split(":")[1]}."
            }
        }
    }
}



def installed() {
    log.trace "installed"
    state?.install = true
}

def uninstalled() {
    log.trace "uninstalled"
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Set new Tasmota devices values to default
    settingUpdate("deviceName", "Some Device", "text")
    settingUpdate("virtualDeviceType", "", "enum")
}

def initialize() {
    log.trace "initialize"
}



/**
 * Get SmartApp's general setting value
 * @param name
 * @return String | null
 */
def generalSetting(String name) {
    return (settings?."${name}") ?: null
}

/**
 * Health Check - online/offline
 * @return Integer
 */
Integer checkInterval() {
    Integer interval = ((generalSetting("frequency") ?: 'Every 1 minute').replace('Every ', '').replace(' minutes', '').replace(' minute', '').replace('1 hour', '60')) as Integer
    if (interval < 15) {
        return (interval * 2 * 60 + 1 * 60)
    } else {
        return (30 * 60 + 2 * 60)
    }
}


def settingUpdate(name, value, type=null) {
    if(name && type) { app?.updateSetting("$name", [type: "$type", value: value]) }
    else if (name && type == null) { app?.updateSetting(name.toString(), value) }
}




private loginPost(def bodyMap) {

    def headers = [:]
    headers.put("Content-Type", "application/json")

    def params = [
        uri: "https://smartapi.vesync.com/vold/user/login",
        HOST: "smartapi.vesync.com",
        body: bodyMap,
        requestContentType: "application/json",
        contentType: "application/json"
    ]

    log.trace("Posting with ${params}")

    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                log.debug "response header: ${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response status: ${resp.getStatus()}"
            log.debug "response success: ${resp.isSuccess()}"
            log.debug "response rdata: ${resp.responseData}"
            log.debug "response data: ${resp.data}"
            log.debug "response data2: ${resp.getData()}"
                        
            return resp.responseData
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

void setAccountToken() {
    log.trace "Getting account token"

    if (!generalSetting("username") || !generalSetting("password")) {
        log.trace "No account info"
    } else {
    def hash = generateMD5(generalSetting("password"))
    def data = loginPost('{"account": "' + generalSetting("username") + '","password": "' + hash + '"}')    
    //sendEvent(name: "token", value: data["tk"])
    //sendEvent(name: "account_id", value: data["accountID"])

    if (!state?.token) { state?.token = "" }
    if (!state?.accountId) { state?.accountId = "" }

    state.token = data["tk"]
    state.accountId = data["accountID"]
    }

}

void getDevices() {
    log.trace "getDevices"

    def deviceList = []

    //def data = webGet("/vold/user/devices")
    def pass = generalSetting("password")

    if (!pass || !state?.accountId) {
        log.trace "No account info"
    }
    else
    {
        def hash = generateMD5(generalSetting("password"))
        def body = '{"email": "' + generalSetting("username") + '","password": "' + hash + '","userType": "1","method": "devices","appVersion": "2.5.1","phoneBrand": "SM N9005","phoneOS": "Android","traceId": "1576162707","timeZone": "America/New_York","acceptLanguage": "en","accountID": "' + state.accountId + '","token": "' + state.token + '"}'
        def data = jsonPost("/cloud/v1/deviceManaged/devices", body)

        data.result.list.each {
            log.trace it["deviceName"]

            def name = it["deviceName"]
            def cid = it["cid"]
            def type = it["deviceType"]
            def model = it["model"]
            def dni = "vesync-connect-${name}"

            def dev = [name: name, cid: cid, type: type, model: model, dni: dni]
            deviceList.push(dev)
        }

        state.deviceList = deviceList
    }

}

def getVesyncDevices() {
    if (!state?.deviceList) {
        getDevices()
    }
    return state.deviceList
}

def getConfiguredDevices() {
    if (!state?.deviceList) {
        getDevices()
    }

    def configList = []

    getVesyncDevices().each {
        if (getChildDevice(it["dni"])) {
            configList.push(it)
        }
    }

    return configList
}

def getUnconfiguredDevices() {
    if (!state?.deviceList) {
        getDevices()
    }
    def unconfigList = []

    getVesyncDevices().each {
        if (!getChildDevice(it["dni"])) {
            unconfigList.push(it)
        }
    }

    return unconfigList
}

def getOrphanDevices() {
    def orphanList = []

    log.trace "getOrphanDevices"

    getChildDevices().each {
        def dni = it.getDeviceNetworkId()
        def orphan = getVesyncDevices().find { it["dni"] == dni }?: null
        if (!orphan) {
            log.trace "getOrphanDevices: ${it["dni"]}"
            orphanList.push(it)
        }
    }

    return orphanList
}

def getDeviceCID(dni) {
    log.trace "getDeviceCID for ${dni}"

    def d = getVesyncDevices().find { it["dni"] == dni }?: null

    return d["cid"]
}

def getDeviceType(dni) {
    log.trace "getDeviceType for ${dni}"

    def d = getVesyncDevices().find { it["dni"] == dni }?: null

    return d["type"]
}

void setDeviceId(plug) {
    log.trace "Getting device ID for ${plug}"
    def data = webGet("/vold/user/devices")
    
    data.each {
        if (it["deviceName"] == plug) {
            sendEvent(name: "device_id", value: it["cid"])
            sendEvent(name: "device_type", value: it["deviceType"])
            sendEvent(name: "stored_plug_name", value: plug)
        }
    }
}

private webGet(path) {

    def headers = [
        "accountId": state.accountId,
        "tk": state.token
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
                log.debug "webGet response header: ${it.name} : ${it.value}"
            }
            log.debug "webGet response contentType: ${resp.contentType}"
            log.debug "webGet response status: ${resp.getStatus()}"
            log.debug "webGet response success: ${resp.isSuccess()}"
            log.debug "webGet response rdata: ${resp.responseData}"
            log.debug "webGet response data: ${resp.data}"
            log.debug "webGet response data2: ${resp.getData()}"

            log.debug "webGet response data: ${resp.data}"
        
            return resp.data
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private jsonPost(def path, def bodyMap) {
		
	def headers = [:]
    headers.put("Content-Type", "application/json")

	def params = [
    	uri: "https://smartapi.vesync.com" + path,
        HOST: "smartapi.vesync.com",
    	body: bodyMap,
        requestContentType: "application/json",
		contentType: "application/json",
        acceptLanguage: "en-US"
	]
    
    log.trace("Posting with ${params}")

    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                log.debug "response header: ${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response status: ${resp.getStatus()}"
            log.debug "response success: ${resp.isSuccess()}"
            log.debug "response rdata: ${resp.responseData}"
            log.debug "response data: ${resp.data}"
			log.debug "response data2: ${resp.getData()}"
                        
            return resp.responseData
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

private webPut(path) {

    def headers = [
        "accountId": state.accountId,
        "tk": state.token
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
        
            return resp.data
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private String convertHex(str) {
    String[] pieces;
    int i = 0;

    // magic: take string of "hex1:hex2", split into "hex1" and "hex2", i = (int)hex1 + (int)hex2
    pieces = str.split(':');
    for( String piece : pieces )
    i = i + Integer.parseInt(piece, 16);

    // more magic: (float)(i/8192) -> rounded to 2 digits -> back to a string
    return (((float)i / 8192).round(2)).toString();
}

import java.security.MessageDigest

def generateMD5(String s){
    return MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex()
}
