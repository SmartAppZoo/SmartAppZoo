/**
 *  Legrand Connect (Unofficial)
 *
 *  Copyright 2019 Matt Krapivner
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
        name: "Legrand (Connect)",
        namespace: "mkrapivner",
        author: "Matt Krapivner",
        description: "The app to connect to the Node.JS server used for exchanging messages with the Legrand RFLC hub.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        singleInstance: true
)


preferences {
    page(name:"hubInfo", title:"Legrand Hub Info", content:"hubInfo", install:false, uninstall:true)
    page(name:"hubDiscovery", title:"Connect with your Legrand Hub", content:"hubDiscovery", install: false, uninstall:true)
    page(name:"lightsDiscovery", title:"Add These Lights", content:"lightsDiscovery", refreshInterval:5, install:true)
}

mappings {
    path("/HubNotify") {
        action: [
                POST: "postNotifyCallback",
                GET: "getNotifyCallback"
        ]
    }
}

def hubInfo() {
    if (!state.hubConnected && !state.lightsList)
        hubDiscovery()
    else {
        return dynamicPage(name:"hubInfo", title:"Legrand Hub Info", nextPage:"hubDiscovery", uninstall:true, install:false) {
            section{
                paragraph title: "Connected Hub Info",
                        "Hub Model: " + state.hubModel + "\nFirmware Version: " + state.hubFirmwareVersion + "\nFirmware Date: " + state.hubFirmwareDate + "\nFirmware Branch: " + state.hubFirmwareBranch
                paragraph title: "Update State", new groovy.json.JsonBuilder(state.hubUpdateState).toPrettyString()
                paragraph title: "Debug Data", "Hub IP: " + state.legrand_ip + "\nHub Mac Address: " + state.hubMacAddress + "\nHub House ID: " + state.hubHouseID
            }
        }
    }
}

def hubDiscovery() {
    // clear the refresh count for the next page
    state.refreshCount = 0
    if(!state.subscribe) {
        log.trace "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
    state.error = ""

    return dynamicPage(name:"hubDiscovery", title:"Connect with your Legrand Hub", nextPage: "lightsDiscovery", uninstall:true, install:false) {
        section ("Address of the Node.JS server:") {
            input "node_ip", "text", title: "IP address"
            input "node_port", "number", title: "Port"
        }

        section ("Address of the Legrand Hub:") {
            input "legrand_ip", "text", title: "IP address"
        }
    }
}

def lightsDiscovery() {
    if(!state.subscribe) {
        log.trace "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
    def doRescan = false
    if (state.node_ip != node_ip || state.legrand_ip != legrand_ip) {
        doRescan = true
        state.lightsList = [:]
    }

    state.node_ip = node_ip
    state.node_port = node_port
    state.legrand_ip = legrand_ip
    state.legrand_port = 2112

    if (!state.accessToken) {
        createAccessToken()
    }

    def apiServerUrl = apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}/HubNotify")
    // check for timeout error
    state.refreshCount = state.refreshCount+1
    if (state.refreshCount > 20) {
        state.error = "Network Timeout. Check your IP address and port. You must access a local IP address and a non-https port."
    }

    if (!state.initSent || doRescan) {
        state.initSent = true
        state.hubConnected = false
        prepareNodeMessage("/init", ["hubIP":state.legrand_ip, "apiServerUrl":apiServerUrl])
    }

    def options = lightsDiscovered() ?: []
    def numFound = options.size() ?: 0

    log.debug "In lightsDiscovery, found ${numFound} lights"
    if (state.error == "")
    {
        if (!options) {
            // we're waiting for the list to be created
            return dynamicPage(name:"lightsDiscovery", title:"Connecting", nextPage:"", refreshInterval:4, uninstall: true) {
                section("Connecting to ${state.node_ip}:${state.node_port}") {
                    paragraph "This can take a minute. Please wait..."
                }
            }
        } else {
            // we have the list now
            return dynamicPage(name:"lightsDiscovery", title:"Add These Lights", install: true) {
                section("See the available lights:") {
                    input "selectedLights", "enum", required:false, title:"Select Lights (${numFound} found)", multiple:true, options:options
                }
            }
        }
    }
    else
    {
        def error = state.error

        // clear the error
        state.error = ""

        // show the message
        return dynamicPage(name:"lightsDiscovery", title:"Connection Error", nextPage:"", uninstall: true) {
            section() {
                paragraph error
            }
        }
    }
}

void locationHandler(evt) {
    // log.debug "Entered locationHandler"
    def parsedEvent = parseEventMessage(evt.description)
    // log.debug "evt.description: ${evt.description}"

    /*
    log.debug "installedSmartAppId: ${evt.installedSmartAppId}"
    log.debug "name: ${evt.name}"
    log.debug "source: ${evt.source}"
    log.debug "Is this event a state change? ${evt.isStateChange()}"
    */
    if (parsedEvent.headers && parsedEvent.body) {
        // Node server responded
        def sHeader = new String(parsedEvent.headers.decodeBase64())
        def sBody = new String(parsedEvent.body.decodeBase64())
        //log.debug "headers: ${sHeader}"
        //log.debug "body: ${sBody}"
        // Node server will always respond with application/json
        def body = new groovy.json.JsonSlurper().parseText(sBody)
        log.debug "parsed body: ${body}"
        if (body.containsKey("initReceived")) {
            // wait for "hub connected"
            prepareNodeMessage("/status")
        } else if (body.containsKey("hubConnected")) {
            // TODO This is a response to "/status", but we are not getting it for some reason?
            state.hubConnected = body.hubConnected
            // get the list of lights
            if (state.hubConnected) {
                sendLegrandHubMessage(getSystemInfoCmd())
                sendLegrandHubMessage(getReportSystemPropertiesCmd())
                sendLegrandHubMessage(getZonesCmd())
            }
        }
    }
}

def prepareNodeMessage(String nodePath, Map params=null) {
    try {
        def httpMethod = "GET"
        if (nodePath in ["/command", "/init"])
            httpMethod = "POST"

        def reqHeader = [HOST: "${state.node_ip}:${state.node_port}"]
        def reqQuery = params
        def reqBody = null
        if (httpMethod == "POST")
            reqHeader << ["Content-Type": "application/json"]
        reqQuery = null
        reqBody = new groovy.json.JsonBuilder(params).toPrettyString()
        def hubAction = new physicalgraph.device.HubAction(
                method: httpMethod,
                path: nodePath,
                headers: reqHeader,
                query: reqQuery,
                body: reqBody
        )
        def requestId = hubAction.requestId
        log.debug "Sending ${httpMethod} ${nodePath} command, requestId: ${requestId}, params: ${params}"
        sendHubCommand(hubAction)
    } catch (e) {
        log.error "Caught exception sending ${httpMethod} status command: ${e}"
    }
}

def sendLegrandHubMessage(Map cmd) {
    prepareNodeMessage("/command", cmd)
}

private def parseEventMessage(String description) {
    def event = [:]
    def parts = description.split(',')
    parts.each { part ->
        part = part.trim()
        if (part.startsWith('headers')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                event.headers = valueString
            }
        }
        else if (part.startsWith('body')) {
            part -= "body:"
            def valueString = part.trim()
            if (valueString) {
                event.body = valueString
            }
        }
        else if (part.startsWith('requestId')) {
            part -= "requestId:"
            def valueString = part.trim()
            if (valueString) {
                event.requestId = valueString
            }
        }
    }

    return event
}

private def parseEventMessage(Map event) {
    //handles attribute events
    log.trace "map event recd = " + event
    return event
}

def postNotifyCallback() {
    // log.debug ("In postNotifyCallback")
    // JSON can be sent either in request.JSON or in params.
    // Handle both (Node.JS sends in request.JSON, python flask sends in params.
    // Should probably figure out why this is, but whatever
    def reqJSON = request.JSON?:null
    if (!reqJSON)
        reqJSON = params

    log.debug ("In postNotifyCallback, reqJSON: ${reqJSON}")
    switch (reqJSON?.Service) {
        case "WebServerUpdate":
            if (reqJSON.containsKey("hubConnected")) {
                def hubConnected = reqJSON.hubConnected
                if (state.hubConnected && !hubConnected) {
                    state.hubConnected = false
                    def error = reqJSON.error?:"Unknown"
                    sendPush("Legrand hub disconnected. Error: ${error}")
                } else if (!state.hubConnected && hubConnected) {
                    state.hubConnected = true
                    // get the list of lights
                    sendLegrandHubMessage(getSystemInfoCmd())
                    sendLegrandHubMessage(getReportSystemPropertiesCmd())
                    sendLegrandHubMessage(getZonesCmd())
                    sendPush("Legrand hub connected")
                }
            }
            break
        case "ZonePropertiesChanged":
            if (!reqJSON.containsKey("ZID"))
                log.error ("ZID not found in POST request. Request JSON: ${reqJSON}")
            else {
                def d = getChildDevice(createDNI(reqJSON.ZID))
                d.propertiesChanged(reqJSON.PropertyList?:null)
            }
            break
        case "ListZones":
            if (!state.lightsList || state.lightsList.size() == 0)
                state.lightsList = [:]
            reqJSON.ZoneList.each { zone ->
                state.lightsList[zone.ZID.toString()] = [:]
                sendLegrandHubMessage(getZonePropertiesCmd(zone.ZID))
            }
            break
        case "ReportZoneProperties":
            def zone = reqJSON.ZID
            def d = getChildDevice(createDNI(zone))
            if (d)
            // device already added, just update it's properties
                d.propertiesChanged(reqJSON.PropertyList?:null)
            state.lightsList[Integer.toString(zone)] = reqJSON.PropertyList
            break
        case "ZoneAdded":
            // TODO
            break
        case "ZoneDeleted":
            // TODO
            break
        case "SystemInfo":
            state.hubModel = reqJSON.Model
            state.hubFirmwareVersion = reqJSON.FirmwareVersion
            state.hubFirmwareDate = reqJSON.FirmwareDate
            state.hubFirmwareBranch = reqJSON.FirmwareBranch
            state.hubMacAddress = reqJSON.MACAddress
            state.hubHouseID = reqJSON.HouseID
            state.hubUpdateState = reqJSON.UpdateState
            break
        default:
            break
    }
    return [foo: reqJSON?.foo, bar: reqJSON?.bar]
}

def getNotifyCallback() {
    log.debug ("query params: ${params}")
    // We need to reference specific parameters we want, because params map contains other useless junk
    return [data: "test"]
}

def lightsDiscovered() {
    //log.debug ("In lightsDiscovered")
    def map = [:]
    def allFound = true
    state.lightsList.each { key, val ->
        map[key] = val.Name?: null
        if (!map[key]) {
            allFound = false
            log.warn "Not found zone ${key}"
            sendLegrandHubMessage(getZonePropertiesCmd(key))
        }
        else
            log.trace "found zone ${key}: ${val.Name}"
    }
    if (allFound)
        return map
    else
        return [:]
}

// Definitions of Legrand commands
def getSystemInfoCmd() {
    def cmd = [:]
    cmd.Service = "SystemInfo"
    return cmd
}

def getReportSystemPropertiesCmd() {
    def cmd = [:]
    cmd.Service = "ReportSystemProperties"
    return cmd
}

def getZonesCmd() {
    def cmd = [:]
    cmd.Service = "ListZones"
    return cmd
}

def getZonePropertiesCmd(zone) {
    def zid = zone as Integer
    def cmd = [:]
    cmd.Service = "ReportZoneProperties"
    cmd.ZID = zid
    return cmd
}

def setZonePropertiesCmd(zone, props) {
    def zid = zone as Integer
    def cmd = [:]
    cmd.Service = "SetZoneProperties"
    cmd.ZID = zid
    cmd.PropertyList = props
    return cmd
}

def installed() {
    //log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    //log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    unsubscribe()
    if(!state.subscribe) {
        log.trace "subscribe to location"
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
    if (selectedLights)
        addLights()
}

def addLights() {
    selectedLights.each { zid ->
        def newLight = state.lightsList[zid.toString()]
        //log.trace "newLight = " + newLight
        if (newLight != null) {
            def newLightDNI = createDNI (zid)
            // log.trace "newLightDNI = " + newLightDNI
            def d = getChildDevice(newLightDNI)
            if(!d) {
                d = addChildDevice("mkrapivner", "Dimmer Switch", newLightDNI, state.hub, [label: newLight.Name]) //, completedSetup: true
                log.trace "created ${d.displayName} with id ${newLightDNI}"

                // set up device capabilities here ??? TODO ???
            } else {
                log.debug "Found existing light ${d.displayName} with DNI ${newLightDNI}, not adding another."
            }
        }
    }
}

def createDNI(zid) {
    return "Legrand " + zid.toString()
}

/////////CHILD DEVICE METHODS

def getLightZIDbyChild(childDevice) {
    return getLightZIDbyName(childDevice.device?.deviceNetworkId)
}

def getLightZIDbyName(String name) {
    if (name) {
        def foundLight = state.lightsList.find { createDNI(it.key).toString() == name.toString()}?.key
        return foundLight
    } else {
        return null
    }
}