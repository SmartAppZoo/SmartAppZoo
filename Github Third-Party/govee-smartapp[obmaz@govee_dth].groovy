/**
 *  Govee Smartapp
 *
 * MIT License
 *
 * Copyright (c) 2021 zambobmaz@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


import groovy.json.JsonSlurper
import groovy.transform.Field

@Field nameSpace = 'imageafter45121'
@Field name = 'Govee Smartapp'
@Field dth = "Govee DTH"

definition(
        name: name,
        namespace: nameSpace,
        author: "obmaz",
        description: name,
        category: "My Apps",
        iconUrl: "http://baldeagle072.github.io/icons/standard-tile@1x.png",
        iconX2Url: "http://baldeagle072.github.io/icons/standard-tile@2x.png",
        iconX3Url: "http://baldeagle072.github.io/icons/standard-tile@3x.png")

preferences {
    page(name: "firstPage")
    page(name: "secondPage")
}

def firstPage() {
    // If it does not use page, "location.hubs.size" will make error "java.lang.NullPointerException: Cannot get property 'physicalHubs' on null object"
    dynamicPage(name: "firstPage", title: "Setting", uninstall: true) {
        if (location.hubs.size() < 1) {
            section() {
                paragraph "[ERROR]\nSmartThings Hub not found.\nYou need a SmartThings Hub to use $name."
            }
            return
        }

//        section("Smartthings") {
//            input "devHub", "enum", title: "Hub", required: true, multiple: false, options: getHubs()
//        }

		section("Govee") {
            paragraph "You can get the API key in official Govee App"
        }

        //d10f0582-26eb-4225-915a-b6b62d35089x
        section {
            input(name: "apiKey", type: "text", title: "API Key", required: true, description: "", defaultValue: "")
        }
        
        section {
            href(name: "toSecondPage", page: "secondPage", description: "Select the device")
        }
    }
}

def secondPage() {
    def resp = sendCommand("GET", "/v1/devices", null, null)
    def status
    def message
    def deviceNameList = []
    def mac = [:]
    def model = [:]
    def retrievable = [:]
    def controllable = [:]
    def supportCmds = [:]

    try {
        status = resp.status
        message = resp.data.message

        resp.data.data.devices.each { item ->
            deviceNameList.push(item.deviceName)
            mac[item.deviceName] = item.device
            model[item.deviceName] = item.model
            retrievable[item.deviceName] = item.retrievable
            controllable[item.deviceName] = item.controllable
            supportCmds[item.deviceName] = item.supportCmds
        }
    } catch (e) {
        message = "Check Govee API Key\n" + e
    }

    dynamicPage(name: "secondPage", uninstall: true, install: true) {
        section {
            paragraph "Status : ${message}"
        }

        if (status == 200) {
            section("Device") {
                input(name: "deviceName", type: "enum", title: "Select Device", required: true, multiple: false, submitOnChange: true, options: deviceNameList)

                if (deviceName) {
                    deviceModel = model[deviceName]
                    deviceMac = mac[deviceName]
                    deviceControllable = controllable[deviceName]
                    deviceRetrievable = retrievable[deviceName]
                    deviceSupportCmds = supportCmds[deviceName]
                } else {
                    deviceModel = "Selected device not found"
                    deviceMac = "Selected device not found"
                    deviceControllable = "Selected device not found"
                    deviceRetrievable = "Selected device not found"
                    deviceSupportCmds = "Selected device not found"
                }
                paragraph "Model: ${deviceModel}"
                paragraph "Mac: ${deviceMac}"
                paragraph "Controllable: ${deviceControllable}"
                paragraph "Retrievable: ${deviceRetrievable}"
                paragraph "SupportCmds: ${deviceSupportCmds}"
            }
        }
    }
}

def sendCommand(method, path, query, payload) {
    log.debug "sendCommand : $method $path $query"

    def retVal
    def params = [
            method : method,
            uri    : "https://developer-api.govee.com",
            path   : path,
            headers: [
                    "Govee-API-Key": apiKey,
                    "Content-Type" : "application/json",
            ],
            query  : query,
            body   : payload
    ]

    try {
        if (params.method == 'GET') {
            httpGet(params) { resp ->
                retVal = resp
            }
        } else if (params.method == 'PUT') {
            httpPutJson(params) { resp ->
                retVal = resp
            }
        }
        log.debug "response : $retVal.data"
    } catch (groovyx.net.http.HttpResponseException e) {
        log.debug "something went wrong: $e"
    }

    return retVal
}

def installed() {
    log.debug "installed"
}

def updated() {
    log.debug "updated"
    initialize()
}

def initialize() {
    log.debug "initialize"

    def deviceId = app.id + "_" + modelNum
    def existing = getChildDevice(deviceId)

    if (!existing) {
        def childDevice = addChildDevice(nameSpace, dth, deviceId, getLocationID(), [label: deviceName])
    }
}

def getApiKey() {
    return settings.apiKey
}

def getDeviceName() {
    return settings.deviceName
}

def setDeviceModel(value) {
    state.deviceModel = value
}

def setDeviceMac(value) {
    state.deviceMac = value
}

def setDeviceControllable(value) {
    state.deviceControllable = value
}

def setDeviceRetrievable(value) {
    state.deviceRetrievable = value
}

def setDeviceSupportCmds(value) {
    state.deviceSupportCmds = value
}

def getDeviceModel() {
    return state.deviceModel
}

def getDeviceMac() {
    return state.deviceMac
}

def getDeviceControllable() {
    return state.deviceControllable
}

def getDeviceRetrievable() {
    return state.deviceRetrievable
}

def getDeviceSupportCmds() {
    return state.deviceSupportCmds
}

def getLocationID() {
    def locationID = null
    try {
        locationID = getHubID(devHub)
    } catch (err) {
    }
    return locationID
}

def getHubs() {
    def list = []
    location.getHubs().each {
        hub -> list.push(hub.name)
    }
    return list
}

def getHubID(name) {
    def id = null
    location.getHubs().each {
        hub ->
            if (hub.name == name) {
                id = hub.id
            }
    }
    return id
}

def uninstalled() {
    log.debug "uninstalled"

    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}