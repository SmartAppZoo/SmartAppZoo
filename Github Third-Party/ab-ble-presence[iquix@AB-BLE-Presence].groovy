/**
 *  AB BLE Presence (Version 0.1.1)
 *
 *  Copyright 2021-2022 iquix (Jaewon Park)
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
    name: "AB BLE Presence",
    namespace: "iquix",
    author: "iquix",
    description: "AB BLE Presence",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true
)


preferences {
    page(name: "mainPage")
    page(name: "addDevicePage")   
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "AB BLE Presence", nextPage: null, uninstall: true, install: true) {
        if (state.accessToken) {
            if(state.devList != null) {
                section("Add a new BLE Device"){
                    href "addDevicePage", title: "Add a new BLE Device", description:""
                }
                section("Settings") {
                    input "consider_present", "decimal", title: "Set timeout in considering not present (seconds)", description: "Seconds to wait till marking away", range: "60..*", defaultValue: 180, required: false
                }
            } else {
                section("Notice"){
                    paragraph "Warning: No data received from the AB BLE Gateway. Make sure that you setup the AB BLE Gateway with the setting values from the menu below."
                }
            }
            section("View Settings for AB BLE") {
                href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style :"embedded", required :false, title :"View Settings for AB BLE", description :"Tap, select, copy, then click \"Done\""
            }
        } else {
            section("Installing AB BLE Presence"){
                paragraph "Make sure you have checked OAuth while installing SmartApp in the SmartThings IDE.\nPress 'Done' to complete installation."
            }
        }
    }
}

def addDevicePage(){
    def addedDNIList = []
    def childDevices = getAllChildDevices()
    if (childDevices.size() > 0) {
        childDevices.each {childDevice->
            addedDNIList.push(childDevice.deviceNetworkId)
        }
    } else {
        log.debug "No child devices are registered yet"
    }
    def newList = ["None"]
    state.devList.each {
        if(!addedDNIList.contains("ble-" + it)){
            newList.push(it)
        }
    }
    dynamicPage(name: "addDevicePage", nextPage: "mainPage") {
        section ("Add a BLE Device") {
            input(name: "selectedDevice", title:"Select" , type: "enum", required: true, options: newList, defaultValue: "None")
        }
    }
}

def addSelectedDevice() {
    log.debug "addSelectedDevice() called"
    if(settings?.selectedDevice != "None" && settings?.selectedDevice != null){
        def dev = settings.selectedDevice
        log.debug "adding " + dev
        if (getChildDevice("ble-"+dev)) {
            //log.warn "device ${dev} already exists."
            return
        }
        try{
            addChildDevice("iquix", "AB BLE Presence Sensor", "ble-"+dev, null, ["name": dev])
        }catch(err){
            log.error "Error while adding device : ${err}"
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
    createAccessToken()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    addSelectedDevice()
    initialize()
}

def initialize() {
    runEvery1Minute(childCheckPresence)
}

def parseBLE() {
    def devs = request.JSON?.devices
    if (devs == null) return

    def devList = []
    devs.each { 
        def payload = it[3].toLowerCase()
        def eds_index = payload.indexOf("aafe1516aafe")+16
        def ibc_index = payload.indexOf("1aff4c000215")+12
        if(eds_index>=16) devList << "eds_"+payload.substring(eds_index, eds_index+32)
        else if(ibc_index>=12) devList << "ibc_"+payload.substring(ibc_index, ibc_index+40)
        //else devList << payload
    }
    devList.unique()
    log.debug "devList = ${devList}"
    
    devList.each { 
        def device = getChildDevice("ble-"+it)?.see()
    }
    state.devList = devList.collect()
}

def childCheckPresence() {
    log.debug "childCheckPresence()"
    if (childDevices.size() > 0) {
        childDevices.each { it?.checkPresence(considerPresent) }
    }
}

def renderConfig() {
    def url = apiServerUrl("").replace("https://", "").split(":")
    def configJson = new groovy.json.JsonOutput().toJson([
            Connection_Type: "HTTP Client",
            Host: url[0],
            Port: url[1].replace("/", ""),
            URI: "/api/smartapps/installations/${app.id}/parseBLE?access_token=${state.accessToken}",
            HTTPS: "Enable"
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    log.debug configString
    render contentType: "text/plain", data: configString
}

def getConsiderPresent() {
    return (settings.consider_present ?: 180)
}

def authError() {
    [error: "Permission denied"]
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/parseBLE") { action: [POST: "authError"] }
        path("/config") { action: [GET: "authError"] }
    } else {
        path("/parseBLE") { action: [POST: "parseBLE"] }
        path("/config") { action: [GET: "renderConfig"] }
    }
}