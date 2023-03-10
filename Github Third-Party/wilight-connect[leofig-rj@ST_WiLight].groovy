/**
 *  WiLight (Connect)
 *
 *  Copyright 2017 Leonardo Figueiro
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
 
definition (
    name: "WiLight (Connect)",
    namespace: "leofig-rj",
    author: "Leonardo Figueiro",
    description: "Allows you to integrate your WiLight Devices with SmartThings.",
    category: "My Apps",
    version: "1.0.0",
    singleInstance: true,
//    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
//    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
//    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
    iconUrl: "https://github.com/leofig-rj/ST_WiLight/raw/master/icons/wilight.png",
    iconX2Url: "https://github.com/leofig-rj/ST_WiLight/raw/master/icons/wilight@2x.png",
    iconX3Url: "https://github.com/leofig-rj/ST_WiLight/raw/master/icons/wilight@2x.png"
)

preferences {
    page(name: "mainPage")
    page(name: "configurePDevice")
    page(name: "deletePDevice")
    page(name: "discoveryPage", title: "Device Discovery", content: "discoveryPage", refreshTimeout:5)
    page(name: "addDevices", title: "Add WiLight", content: "addDevices")
    page(name: "deviceDiscovery")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Manage your WiLights", nextPage: null, uninstall: true, install: true) {
        section("Configure"){
           href "deviceDiscovery", title:"Discover Devices", description:""
        }
        section("Installed Devices"){
            getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                href "configurePDevice", title:"$it.label", description:"", params: [did: it.deviceNetworkId]
            }
        }
    }
}

def configurePDevice(params) {
    def currentDevice
    getChildDevices().each {
        if(it.deviceNetworkId == params.did){
            state.currentDeviceId = it.deviceNetworkId
            state.currentDisplayName = it.displayName
        }      
    }
    if (getChildDevice(state.currentDeviceId) != null) getChildDevice(state.currentDeviceId).configure()
    dynamicPage(name: "configurePDevice", title: "Configure WiLights created with this app", nextPage: null) {
        section {
            app.updateSetting("${state.currentDeviceId}_label", getChildDevice(state.currentDeviceId).label)
            input "${state.currentDeviceId}_label", "text", title:"Device Name", description: "", required: false
        }
        section {
              href "deletePDevice", title:"Delete $state.currentDisplayName", description: "", params: [did: state.currentDeviceId]
        }
    }
}

def deletePDevice(params) {
    try {
        unsubscribe()
        deleteChildDevice(state.currentDeviceId)
        dynamicPage(name: "deletePDevice", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "The device has been deleted. Press Done to continue"
            } 
        }
    } catch (e) {
        dynamicPage(name: "deletePDevice", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "Error: ${(e as String).split(":")[1]}."
            } 
        }
    }
}

def discoveryPage() {
   return deviceDiscovery()
}

def deviceDiscovery(params=[:]) {
    def devices = devicesDiscovered()
    
    int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
    state.deviceRefreshCount = deviceRefreshCount + 1
    def refreshInterval = 3
    
    def options = devices ?: []
    def numFound = options.size() ?: 0
    
    if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
        //Cleaning old device memory
        state.devices = [:]
        state.deviceRefreshCount = 0
        app.updateSetting("selectedDevice", "")
    }
    
    ssdpSubscribe()
    
    //WiLight discovery request every 15 //25 seconds
    if((deviceRefreshCount % 5) == 0) {
        discoverDevices()
    }
    
    //wilight.xml request every 3 seconds except on discoveries
    if(((deviceRefreshCount % 3) == 0) && ((deviceRefreshCount % 5) != 0)) {
        verifyDevices()
    }
    
    return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:"addDevices", refreshInterval:refreshInterval, uninstall: true) {
        section("Please wait while we discover your WiLight devices. Discovery can take five minutes or more. Select your device below once discovered.") {
            input "selectedDevices", "enum", required:false, title:"Select WiLight (${numFound} found)", multiple:true, options:options
        }
        section("Options") {
            href "deviceDiscovery", title:"Reset list of discovered devices", description:"", params: ["reset": "true"]
        }
    }
}

Map devicesDiscovered() {
    def vdevices = getVerifiedDevices()
    def map = [:]
    vdevices.each {
        def value = "${it.value.name}"
        def key = "${it.value.mac}"
        map["${key}"] = value
    }
    map
}

def getVerifiedDevices() {
    getDevices().findAll{ it?.value?.verified == true }
}

private discoverDevices() {
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
}

def configured() {
    
}

def isConfigured() {
    if(getChildDevices().size() > 0) return true else return false
}

def isVirtualConfigured(did) { 
    def foundDevice = false
    getChildDevices().each {
       if(it.deviceNetworkId != null){
           if(it.deviceNetworkId.startsWith("${did}/")) foundDevice = true
       }
    }
    return foundDevice
}

private virtualCreated(number) {
    if (getChildDevice(getDeviceID(number))) {
        return true
    } else {
        return false
    }
}

private getDeviceID(number) {
    return "${state.currentDeviceId}/${app.id}/${number}"
}

def initialize() {
    ssdpSubscribe()
    runEvery5Minutes("ssdpDiscover")
}

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:Basic:1", ssdpHandler)
}

void ssdpDiscover() {
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseLanMessage(description)
    
    parsedEvent << ["hub":hub]
    
    def devices = getDevices()
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
    if (devices."${ssdpUSN}") {
        def d = devices."${ssdpUSN}"
        def child = getChildDevice(parsedEvent.mac)
        def childIP
        def childPort
        if (child) {
            childIP = child.getDeviceDataByName("ip")
            childPort = child.getDeviceDataByName("port").toString()
            if(childIP != convertHexToIP(parsedEvent.networkAddress) || childPort != convertHexToInt(parsedEvent.deviceAddress).toString()){
               log.debug "Device data (${child.getDeviceDataByName("ip")}) does not match what it is reporting(${convertHexToIP(parsedEvent.networkAddress)}). Attempting to update."
               child.sync(convertHexToIP(parsedEvent.networkAddress), convertHexToInt(parsedEvent.deviceAddress).toString())
            }
        }
        
        if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            d.networkAddress = parsedEvent.networkAddress
            d.deviceAddress = parsedEvent.deviceAddress
        }
    } else {
        devices << ["${ssdpUSN}": parsedEvent]
    }
}

void verifyDevices() {
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        def ip = convertHexToIP(it.value.networkAddress)
        def port = convertHexToInt(it.value.deviceAddress)
        String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
    }
}

def getDevices() {
    state.devices = state.devices ?: [:]
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
    log.trace "wilight.xml response (application/xml)"
    def body = hubResponse.xml
    def numSerie = body?.device?.serialNumber?.text()
    def numSerieCompacto = numSerie.substring(6)
    if (body?.device?.modelName?.text().startsWith("Parent_WiLight_Device")) {
        def devices = getDevices()
        def device = devices.find {it?.key?.contains(body?.device?.UDN?.text())}
        if (device) {
            device.value << [name:"WiLight " + numSerieCompacto, serialNumber:body?.device?.serialNumber?.text(), verified: true]
        } else {
            log.error "/wilight.xml returned a device that didn't exist"
        }
    }
}

def addDevices() {
    def devices = getDevices()
    def sectionText = ""

    selectedDevices.each { dni ->bridgeLinking
        def selectedDevice = devices.find { it.value.mac == dni }
        def d
        if (selectedDevice) {
            d = getChildDevices()?.find {
                it.deviceNetworkId == selectedDevice.value.mac
            }
        }
        
        if (!d) {
            addChildDevice("leofig-rj", (selectedDevice?.value?.name?.startsWith("Parent WiLight Device") ? "Parent WiLight Device" : "Parent WiLight Device"), selectedDevice.value.mac, selectedDevice?.value.hub, [
                "label": selectedDevice?.value?.name ?: "WiLight ",
                "data": [
                    "mac": selectedDevice.value.mac,
                    "ip": convertHexToIP(selectedDevice.value.networkAddress),
                    "port": "" + Integer.parseInt(selectedDevice.value.deviceAddress,16)
                ]
            ])
            sectionText = sectionText + "Successfully added WiLight with ip address ${convertHexToIP(selectedDevice.value.networkAddress)} \r\n"
        }
        
    } 
    
    return dynamicPage(name:"addDevices", title:"Devices Added", nextPage:"mainPage",  uninstall: true) {
        if(sectionText != ""){
            section("Add WiLight Results:") {
                paragraph sectionText
            }
        }else{
            section("No devices added") {
                paragraph "All selected devices have previously been added"
            }
        }
    }
}

def uninstalled() {
    unsubscribe()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
