/**
*  WeMo AirPurifier (Connect)
*
*  v:1.0d - 01/18/2017 - Updated Scheduled Refresh and Device Sync
*  v:1.0c - 01/10/2017 - Initial Version
*
*  Pending: Name, Refresh/Pool time
*  Copyright 2016 Rudimar Prunzel
*  Based on the Holmes Humidifier code by Brian Keifer
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
**/

definition(
    name: "WeMo AirPurifier (Connect)",
    namespace: "RudiP",
    author: "RudiP",
    description: "Allows you to integrate your Holmes Smart AirPurifier WeMo with SmartThings.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/wemo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/wemo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name:"firstPage", title:"WeMo AirPurifier Device Setup", content:"firstPage", uninstall: true)
}

def getVersion() {
	return "1.0c" 
}

private discoverAllWemoTypes()
{
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:Belkin:device:AirPurifier:1", physicalgraph.device.Protocol.LAN))
}

private getFriendlyName(String deviceNetworkId) {
    sendHubCommand(new physicalgraph.device.HubAction("""GET /setup.xml HTTP/1.1
HOST: ${deviceNetworkId}

""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}

private verifyDevices() {
    def AirPurifiers = getWemoAirPurifiers().findAll { it?.value?.verified != true }

    AirPurifiers.each {
        getFriendlyName((it.value.ip + ":" + it.value.port))
    }
}

def firstPage()
{
    if(canInstallLabs())
    {
        int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
            state.refreshCount = refreshCount + 1
        def refreshInterval = 5

        //log.debug "REFRESH COUNT :: ${refreshCount}"

        if(!state.subscribe) {
            subscribe(location, null, locationHandler, [filterEvents:false])
            state.subscribe = true
        }

        //ssdp request every 25 seconds
        if((refreshCount % 5) == 0) {
            discoverAllWemoTypes()
        }

        //setup.xml request every 5 seconds except on discoveries
        if(((refreshCount % 1) == 0) && ((refreshCount % 5) != 0)) {
            verifyDevices()
        }


        def AirPurifiersDiscovered = AirPurifiersDiscovered()

        return dynamicPage(name:"firstPage", title:"Discovery Started!", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: selectedAirPurifiers != null) {
            section("Select a device...") {
                input "selectedAirPurifiers", "enum", required:false, title:"Select Wemo AirPurifier\n(${AirPurifiersDiscovered.size() ?: 0} found)", multiple:true, options:AirPurifiersDiscovered
            }
            section("Options:") {
                input(name: "refreshTime", title: "Refresh Time (Minutes: 1 - 60)", type: "number", range: "01..60", required: true)
            	input "detailDebug", "bool", title: "Enable Debug logs", defaultValue: false, submitOnChange: true
                paragraph "Give a name to this SmartApp (Optional)"
                input
                label(title: "Assign a name", required: false)
            }
            section ("Version " + "${getVersion()}") { }
        }
    }
    else
    {
        def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

        return dynamicPage(name:"firstPage", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
            section("Upgrade") {
                paragraph "$upgradeNeeded"
            }
        }
    }
}

def devicesDiscovered() {
    def AirPurifiers = getWemoAirPurifiers()
    def list = []
    list = AirPurifierst{ [app.id, it.ssdpUSN].join('.') }
}


def AirPurifiersDiscovered() {
    def AirPurifiers = getWemoAirPurifiers().findAll { it?.value?.verified == true }
    def map = [:]
    AirPurifiers.each {
        //def value = it.value.name ?: "Wemo AirPurifier ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
        def value = "Wemo AirPurifier ${it.value.mac}"
        def key = it.value.mac
        map["${key}"] = value
    }
    map
}

def getWemoAirPurifiers()
{
    if (!state.AirPurifiers) { state.AirPurifiers = [:] }
    state.AirPurifiers
}

def installed() {
    log.info "--- Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "--- Updated with settings: ${settings}"
    initialize()
}

def initialize() {
    log.info "--- WeMo AirPurifier (Connect) - Version: ${getVersion()}"

    // remove location subscription afterwards
    unsubscribe()
    state.subscribe = false

    // Verify and Add Selected devices
    if (selectedAirPurifiers)
    {
        addAirPurifiers()
        runIn(5, "subscribeToDevices") //initial subscriptions delayed by 5 seconds
    }
}

def resubscribe() {
    log.info "--- Resubscribe called, delegating to refresh()"
    refresh()
}

def refresh() {
    log.info "--- Refresh Devices"
    refreshDevices()
}

def refreshDevices() {
    if (settings.detailDebug) log.debug "refreshDevices() called"
    def devices = getAllChildDevices()
    devices.each { d ->
        log.info "Calling refresh() on device: ${d.id}"
        d.refresh()
    }
}

def subscribeToDevices() {
    if (settings.detailDebug) log.debug "subscribeToDevices() called"
    def devices = getAllChildDevices()
    devices.each { d ->
        d.subscribe()
    }

    // Schedule Refresh & Device Sync
    int refreshMin = settings.refreshTime ? settings.refreshTime : 5
    String refreshSchedule = "0 0/" + refreshMin.toString() + " * 1/1 * ? *"
    schedule(refreshSchedule, "refresh")
    //int refreshSec = refreshMin * 60
    //runIn(refreshSec, "refresh")

    schedule("0 0/30 * 1/1 * ? *", "doDeviceSync")  // setup ip:port syncing every 30 minutes
    // runIn(900, "doDeviceSync") //setup ip:port syncing every 15 minutes

}
def addAirPurifiers() {
    def AirPurifiers = getWemoAirPurifiers()

    selectedAirPurifiers.each { dni ->
        def selectedAirPurifier = AirPurifiers.find { it.value.mac == dni } ?: switches.find { "${it.value.ip}:${it.value.port}" == dni }
        def d
        if (selectedAirPurifier) {
            d = getChildDevices()?.find {
                it.dni == selectedAirPurifier.value.mac || it.device.getDataValue("mac") == selectedAirPurifier.value.mac
            }
        }

        if (!d) {
            log.info "Creating WeMo AirPurifier with DNI: ${selectedAirPurifier.value.mac}"
            log.info "IP: ${selectedAirPurifier.value.ip} - PORT: ${selectedAirPurifier.value.port}"
            d = addChildDevice("RudiP", "WeMo AirPurifier", selectedAirPurifier.value.mac, selectedAirPurifier?.value.hub, [
                "label": "WeMo AirPurifier ${selectedAirPurifier.value.mac}",
                "data": [
                    "mac": selectedAirPurifier.value.mac,
                    "ip": selectedAirPurifier.value.ip,
                    "port": selectedAirPurifier.value.port
                ]
            ])

            log.info "Created ${d.displayName} with id: ${d.id}, dni: ${d.deviceNetworkId}"
        } else {
            if (settings.detailDebug) log.debug "found ${d.displayName} with id $dni already exists"
        }
    }
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId
    def parsedEvent = parseDiscoveryMessage(description)
    parsedEvent << ["hub":hub]
    if (settings.detailDebug) log.debug("PARSED: ${parsedEvent}")
    if (parsedEvent?.ssdpTerm?.contains("Belkin:device:AirPurifier")) {
        if (settings.detailDebug) log.debug("FOUND WeMo AirPurifier")
        def AirPurifiers = getWemoAirPurifiers()

        if (!(AirPurifiers."${parsedEvent.ssdpUSN.toString()}"))
        { //if it doesn't already exist
            if (settings.detailDebug) log.debug ("Creating AirPurifiers")
            AirPurifiers << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
        }
        else
        { // just update the values

            if (settings.detailDebug) log.debug "Device was already found in state..."

            def d = AirPurifiers."${parsedEvent.ssdpUSN.toString()}"
            boolean deviceChangedValues = false

            if(d.ip != parsedEvent.ip || d.port != parsedEvent.port) {
                log.info "Device's port or ip changed..."
                log.info("old ip: ${d.ip}")
                log.info("new ip: ${parsedEvent.ip}")
                log.info("old port: ${d.port}")
                log.info("new port: ${parsedEvent.port}")
                d.ip = parsedEvent.ip
                d.port = parsedEvent.port
                deviceChangedValues = true
            }

            if (deviceChangedValues) {
                def children = getChildDevices()
                if (settings.detailDebug) log.debug "Found children ${children}"
                children.each {
                    if (it.getDeviceDataByName("mac") == parsedEvent.mac) {
                        log.info "updating ip and port, and resubscribing, for device ${it} with mac ${parsedEvent.mac}"
                        it.subscribe(parsedEvent.ip, parsedEvent.port)
                    }
                }
            }

        }

    }


    else if (parsedEvent.headers && parsedEvent.body) {

        def headerString = new String(parsedEvent.headers.decodeBase64())
        def bodyString = new String(parsedEvent?.body?.decodeBase64())
        def body = new XmlSlurper().parseText(bodyString)

        if (body?.device?.deviceType?.text().startsWith("urn:Belkin:device:AirPurifier:1"))
        {
            def AirPurifiers = getWemoAirPurifiers()
            def wemoAirPurifier = AirPurifiers.find {it?.key?.contains(body?.device?.UDN?.text())}
            if (wemoAirPurifier)
            {
                wemoAirPurifier.value << [name:body?.device?.friendlyName?.text(), verified: true]
            }
            else
            {
                log.error "/setup.xml returned a wemo device that didn't exist"
            }
        }


    }
}

private def parseDiscoveryMessage(String description) {
    def device = [:]
    def parts = description?.split(',')
    parts.each { part ->
        part = part.trim()
        if (part.startsWith('devicetype:')) {
            def valueString = part.split(":")[1].trim()
            device.devicetype = valueString
        }
        else if (part.startsWith('mac:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.mac = valueString
            }
        }
        else if (part.startsWith('networkAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.ip = valueString
            }
        }
        else if (part.startsWith('deviceAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.port = valueString
            }
        }
        else if (part.startsWith('ssdpPath:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                device.ssdpPath = valueString
            }
        }
        else if (part.startsWith('ssdpUSN:')) {
            part -= "ssdpUSN:"
            def valueString = part.trim()
            if (valueString) {
                device.ssdpUSN = valueString
            }
        }
        else if (part.startsWith('ssdpTerm:')) {
            part -= "ssdpTerm:"
            def valueString = part.trim()
            if (valueString) {
                device.ssdpTerm = valueString
            }
        }
        else if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                device.headers = valueString
            }
        }
        else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                device.body = valueString
            }
        }
    }

    device
}

def doDeviceSync(){
    log.info "--- Verifying Devices"
    if(!state.subscribe) {
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }

    discoverAllWemoTypes()
}

def pollChildren() {
    def devices = getAllChildDevices()
    devices.each { d ->
        //only poll switches?
        d.poll()
    }
}

def delayPoll() {
    if (settings.detailDebug) log.debug "Executing 'delayPoll'"

    runIn(5, "pollChildren")
}



private Boolean canInstallLabs()
{
    return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware)
{
    return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions()
{
    return location.hubs*.firmwareVersionString.findAll { it }
}