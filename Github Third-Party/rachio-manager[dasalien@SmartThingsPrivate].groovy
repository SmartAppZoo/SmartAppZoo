/**
 *  Rachio Manager SmartApp V2
 *
 *  Copyright 2020 Rachio Inc.
 *  Written by Anthony Santilli (@tonesto7). Updated by DIeter Rothhardt
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
 *	Modified: 11-24-2020
 */

import groovy.json.*
import java.text.SimpleDateFormat

definition(
    name: "Rachio Manager",
    namespace: "acrosscable12814",
    author: "Dieter Rothhardt",
    description: "Community version of the SmartThings Rachio Integration.",
    category: "Green Living",
    iconUrl: "https://s3-us-west-2.amazonaws.com/rachio-media/smartthings/Rachio-logo-100px.png",
    iconX2Url: "https://s3-us-west-2.amazonaws.com/rachio-media/smartthings/Rachio-logo-200px.png",
    iconX3Url: "https://s3-us-west-2.amazonaws.com/rachio-media/smartthings/Rachio-logo-300px.png",
    singleInstance: true,
    usesThirdPartyAuthentication: true)

preferences {
    page(name: "startPage")
    page(name: "apiKeyPage")
    page(name: "authPage")
    page(name: "noOauthPage")
    page(name: "devMigrationPage")
    page(name: "devicePage")
    page(name: "supportPage")
}

mappings {
    path("/rachioReceiver") { action: [ POST: "rachioReceiveHandler" ] }
}

String appVer() { return "2.0.0" }

def appInfoSect()	{
    section() {
        def str = ""
        str += "${app?.name}"
        str += "\nCopyright\u00A9 2020 Dieter Rothhardt"
        str += "\nVersion: ${appVer()}"
        paragraph str, image: "https://s3-us-west-2.amazonaws.com/rachio-media/smartthings/Rachio-logo-100px.png"
    }
}

def startPage() {
    getAccessToken()
    if(!state?.accessToken) {
        noOauthPage()
    } else if(atomicState?.authToken) {
        if(!settings?.controllers && settings?.sprinklers) {
            devMigrationPage()
        } else {
            devicePage()
        }
    } else { authPage() }
}

def noOauthPage() {
    return dynamicPage(name: "noOauthPage", title: "Oauth Not Enabled", uninstall: true) {
        appInfoSect()
        section() {
            paragraph "Oauth is Not Enabled for this SmartApp.  Please return to the IDE and Enable OAuth under App Settings", required: true, state: null
        }
        removeSect()
    }
}

def authPage()  {
    //log.debug "authPage()"
    def description = null
    def uninstallAllowed = false
    def oauthTokenProvided = false

    if(settings?.authKey && settings?.authKey.toString()?.length() > 10 && settings?.authKey != atomicState?.authToken) {
        atomicState?.authToken = settings?.authKey
        oauthTokenProvided = true
    }
    if(atomicState?.authToken) {
        getRachioDeviceData(true)
        def usrName = atomicState?.userName ?: ""
        description = usrName ? "You are signed in as $usrName" : "You are connected."
        uninstallAllowed = true
        oauthTokenProvided = true
    } else {
        description = "Login to Rachio Web App to get your API Key"
    }
    
    if (!oauthTokenProvided) { log.info "No Rachio AuthToken Found... Please Login to Web App > Account Settings > Get API Key..." }
    def authPara = !oauthTokenProvided ? "Please Login to Web App\n??? Tap on More Tab\n??? Click on Account Settings\n??? Click Get API Key\n??? Tap the Copy Icon\n??? Press the back button\n??? Paste the key in the input below" : "Tap Next to setup your sprinklers."
    return dynamicPage(name: "authPage", title: "Auth Page", nextPage: (oauthTokenProvided ? "devicePage" : null), uninstall: uninstallAllowed) {
        appInfoSect()
        section() {
            paragraph authPara
            href url: "https://app.rach.io", style: "embedded", required: (!oauthTokenProvided), state: (oauthTokenProvided ? "complete" : ""), title: "Rachio", description: description
            href "apiKeyPage", title: "Enter your API Key", description: (authKey ? "API Key Entered" : "Tap to Enter API Key"), state: (authKey ? "complete" : null), required: true
        }
        if(uninstallAllowed) { removeSect() }
    }
}

def apiKeyPage() {
    return dynamicPage(name: "apiKeyPage", title: "API Key", install: false, uninstall: false) {
        section() {
            input "authKey", "text", title: "API Key", required: true, description: "Paste the API Key Here...", submitOnChange: true
        }
    }
}

def removeSect() {
    remove("Remove this App and Devices!", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis App and All Devices will be removed")
}

def devMigrationPage() {
    return dynamicPage(name: "devMigrationPage", title: "Migration Page", nextPage: "devicePage", install: false, uninstall: false) {
        section() {
            paragraph "This SmartApp was updated to support multiple controllers.\nYour previous controller and zone selections are being migrated to the new data structure.", required: true, state: null
        }
        section() {
            log.debug "Migrating Controller and Zone Selections to New Data Structure..."
            List devs = []
            String id = settings?.sprinklers
            devs.push(id as String)
            app?.updateSetting("controllers", [type: "enum", value: devs])
            log.debug "Controllers: ${settings?.controllers}"
            if(settings?.selectedZones) {
                List zones = settings?.selectedZones?.collect { it as String }
                if(zones) {
                    app?.updateSetting("${id}_zones", [type: "enum", value: zones])
                }
                log.debug "Controller($id) Zones: ${settings?."${id}_zones"}"
            }
            paragraph "Setting Migration Complete...\n\nTap Next to Proceed to Device Configuration", state: "complete"
        }
    }
}

// This method is called after "auth" page is done with Oauth2 authorization, then page "deviceList" with content of devicePage()
def devicePage() {
    //log.trace "devicePage()..."
    if(!atomicState?.authToken) {
        log.debug "No accesstoken"
        return
    }
    // Step 1: get (list) of available devices associated with the login account.
    def devData = getRachioDeviceData()
    def devices = getDeviceInputEnum(devData)
    log.debug "rachioDeviceList(): ${devices}"

    //step2: render the page for user to select which device
    return dynamicPage(name: "devicePage", title: "${(atomicState?.authToken && atomicState?.selectedDevices) ? "Select" : "Manage"} Your Devices", install: true, uninstall: true) {
        appInfoSect()
        section("Controller Configuration:"){
            input "controllers", "enum", title: "Select your controllers", description: "Tap to Select", required: true, multiple: true, options: devices, submitOnChange: true, image: (atomicState?.modelInfo ? atomicState?.modelInfo.img : "")
            atomicState?.controllerIds = settings?.controllers
        }
        if(settings?.controllers) {
            updateHwInfoMap(devData?.devices)
            devices?.sort { it?.value }?.each { cont->
                if(cont?.key in settings?.controllers) {
                    section("${cont?.value} Zones:"){
                        if(settings?."${cont?.key}_zones") {
                            def dData = devData?.devices?.find { it?.id == cont?.key }
                            if(dData) { devDesc(dData) }
                        }
                        def zoneData = zoneSelections(devData, cont?.key)
                        input "${cont?.key}_zones", "enum", title: "Select your zones", description: "Tap to Select", required: true, multiple: true, options: zoneData, submitOnChange: true
                    }
                }
            }
            section("Preferences:") {
                input(name: "pauseInStandby", title: "Disable Actions while in Standby?", type: "bool", defaultValue: true, multiple: false, submitOnChange: true,
                        description: "Allow your device to be disabled in SmartThings when you place your controller in Standby Mode...")
                paragraph "Select the Duration time to be used for manual Zone Runs (This can be changed under each zones device page)"
                input(name: "defaultZoneTime", title: "Default Zone Runtime (Minutes)", type: "number", description: "Tap to Modify", required: false, defaultValue: 10, submitOnChange: true)
            }
        }
        section() {
            href "supportPage", title: "Rachio Support", description: ""
            href "authPage", title: "Manage Login", description: ""
        }
        removeSect()
    }
}

void settingUpdate(name, value, type=null) {
	log.trace "settingUpdate($name, $value, $type)..."
	if(name && type) {
		app?.updateSetting("$name", [type: "$type", value: value])
	}
	else if (name && type == null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
	log.trace "settingRemove($name)..."
	if(name) { app?.deleteSetting("$name") }
}

void appCleanup() {
    log.trace "appCleanup()"
    def stateItems = ["deviceId", "selectedDevice", "selectedZones", "inStandbyMode", "webhookId", "isWateringMap", "inStandbyModeMap"]
    def setItems = ["sprinklers", "selectedZones"]
    stateItems?.each { if(state?.containsKey(it as String)) {state.remove(it)} }
    setItems?.each { if(settings?.containsKey(it as String)) {settingRemove(it)} }
}

def devDesc(dev) {
    if(dev) {
        def str = ""
        def zoneCnt = dev?.zones?.findAll { it?.id in settings?."${dev?.id}_zones" }?.size() ?: 0
        str += "${atomicState?.installed ? "Installed" : "Installing"} Device:\n${atomicState?.modelInfo[dev?.id]?.desc}"
        str += "\n($zoneCnt) Zone(s) ${atomicState?.installed ? "are selected" :  "will be installed"}"
        paragraph "${str}", state: "complete", image: (atomicState?.modelInfo[dev?.id]?.img ?: "")
    }
}

def supportPage() {
    return dynamicPage(name: "supportPage", title: "Rachio Support", install: false, uninstall: false) {
        section() {
            href url: getSupportUrl(), style:"embedded", title:"Rachio Support (Web)", description:"", state: "complete", image: "http://rachio-media.s3.amazonaws.com/images/icons/icon-support.png"
            href url: getCommunityUrl(), style:"embedded", title:"Rachio Community (Web)", description:"", state: "complete", image: "http://d33v4339jhl8k0.cloudfront.net/docs/assets/5355b85be4b0d020874de960/images/58333550903360645bfa6cf8/file-Er3y7doeam.png"
        }
    }
}

def zoneSelections(devData, devId=null) {
    //log.debug "zoneSelections: $devData"
    def res = [:]
    if(!devData) { return res }
    devData?.devices.sort {it?.name}.each { dev ->
        if(dev?.id == devId) {
            dev?.zones?.sort {it?.zoneNumber }.each { zone ->
                def str = (zone?.enabled == true) ? "" : " (Disabled)"
                //log.debug "zoneId: $zone.id"
                def adni = [zone?.id].join('.')
                res[adni] = "${zone?.name}$str"
            }
        }
    }
    return res
}

// This was added to handle missing oauth on the smartapp and notifying the user of why it failed.
def getAccessToken() {
    try {
        if(!atomicState?.accessToken) {
            atomicState?.accessToken = createAccessToken()
            //log.debug "Created ST Access Token... ${atomicState?.accessToken}" //Hiding this from Release
        }
        else { return true }
    }
    catch (ex) {
        def msg = "Error: OAuth is not Enabled for the Rachio (Connect) application!!!.  Please click remove and Enable Oauth under the SmartApp App Settings in the IDE..."
        // sendPush(msg)
        log.warn "getAccessToken Exception | ${msg}"
        return false
    }
}

def getRachioDeviceData(noData=false) {
    //log.trace "getRachioDevicesData($noData)..."

    //Step1: GET account info "userId"
    atomicState.userId = getUserId();
    if (!atomicState?.userId) {
        log.error "No user Id found exiting"
        return
    }
    def userInfo = getUserInfo(atomicState?.userId)
    //log.debug "userInfo: ${userInfo}"
    atomicState?.userName = userInfo?.username

    if(!noData) { return userInfo }
}

def getDeviceInputEnum(data) {
    //Step3: Obtain device information for a location
    def devices = [:]
    if(!data) { return devices }
    data?.devices.sort { it?.name }.each { sid ->
       //log.info "systemId: ${sid.id}"
       def dni = sid?.id
       devices[dni] = sid?.name
       //log.info "Found sprinkler with dni(locationId.gatewayId.systemId.zoneId): $dni and displayname: ${devices[dni]}"
    }
    // log.info "getRachioDevicesData() >> sprinklers: $devices"
    return devices
}

def getUserInfo(userId) {
    //log.trace "getUserInfo ${userId}"
    return _httpGet("person/${userId}");
}

def getUserId() {
    //log.trace "getUserId()"
    def res = _httpGet("person/info");
    if (res) {
        return res?.id;
    }
    return null
}

void updateHwInfoMap(devdata) {
    def result = [:]
    if(devdata && settings?.controllers) {
        def results = null
        results = devdata?.findAll { it?.id in settings?.controllers }
        results?.each { dev ->
            result[dev?.id] = getHardwareInfo(dev?.model)
        }
    }
    atomicState?.modelInfo = result
}

def getDevGeneration(devId) {
    if(devId && atomicState?.modelInfo[devId] && atomicState?.modelInfo[devId]?.gen) {
        return atomicState?.modelInfo[devId]?.gen
    } 
    return null
}

def getHardwareInfo(val) {
    switch(val) {
        case "GENERATION1_8ZONE":
            return [model: "8ZoneV1", desc: "8-Zone (Gen 1)", img: getAppImg("rachio_gen1.png"), gen: "Gen1"]
        case "GENERATION1_16ZONE":
            return [model: "16ZoneV1", desc: "16-Zone (Gen 1)", img: getAppImg("rachio_gen1.png"), gen: "Gen1"]
        case "GENERATION2_8ZONE":
            return [model: "8ZoneV2", desc: "8-Zone (Gen 2)", img: getAppImg("rachio_gen2.png"), gen: "Gen2"]
        case "GENERATION2_16ZONE":
            return [model: "16ZoneV2", desc: "16-Zone (Gen 2)", img: getAppImg("rachio_gen2.png"), gen: "Gen2"]
        case "GENERATION3_8ZONE":
            return [model: "8ZoneV3", desc: "8-Zone (Gen 3)", img: getAppImg("rachio_gen3.png"), gen: "Gen3"]
        case "GENERATION3_16ZONE":
            return [model: "16ZoneV3", desc: "16-Zone (Gen 3)", img: getAppImg("rachio_gen3.png"), gen: "Gen3"]
    }
    return [desc: null, model: null, img: "", gen: null]
}

def getAppImg(imgName)	{ return "https://raw.githubusercontent.com/tonesto7/rachio-manager/master/images/$imgName" }

def _httpGet(subUri) {
    //log.debug "_httpGet($subUri)"
    try {
        def params = [
            uri: "${apiEndpoint}/1/public/${subUri}",
            headers: ["Authorization": "Bearer ${atomicState.authToken}"]
        ]
        httpGet(params) { resp ->
            if(resp.status == 200) {
                return resp?.data
            } else {
                //refresh the auth token
                if (resp?.status == 500 && resp?.data?.status?.code == 14) {
                    log.debug "Storing the failed action to try later"
                    data.action = "getRachioDeviceData"
                    log.debug "Refreshing your authToken!"
                    // refreshAuthToken()
                } else {
                    log.error "Authentication error, invalid authentication method, lack of credentials, etc."
                }
              return null
            }
        }
    } catch (ex) {
        if(ex instanceof groovyx.net.http.HttpResponseException) {
            if(ex?.response) {
                log.error("httpGet() Response Exception | Status: ${ex?.response?.status} | Data: ${ex?.response?.data}")
            }
        } else {
            log.error "_httpGet exception: ${ex.message}"
        }
    }
}

def getDisplayName(iroName, zname) {
    if(zname) {
        return "${iroName}:${zname}"
    } else {
        return "Rachio"
    }
}

//Section3: installed, updated, initialize methods
def installed() {
    log.trace "Installed with settings: ${settings}"
    initialize()
    atomicState?.installed = true
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    log.trace "initialized..."
    unschedule()
    scheduler()
    subscribe(app, onAppTouch)
    updateDevZoneStates() //Creates the selectedDevices maps in state
    runIn(2, "initStep2", [overwrite: true])
    sendActivityFeeds("is connected to SmartThings")
    atomicState.timeSendPush = null
}

void initStep2() {
    addRemoveDevices()
    appCleanup()
    runIn(3, "initStep3", [overwrite: true])
}

void initStep3() {
    initWebhooks()
    poll()
}

def uninstalled() {
    log.trace "uninstalled() called... removing smartapp and devices"
    unschedule()
    //Remove any existing webhooks before uninstall...
    removeAllWebhooks()
    if(addRemoveDevices(true)) {
        //Revokes Smartthings endpoint token...
        revokeAccessToken()
        //Revokes Rachio Auth Token
        if(atomicState?.authToken) {
          atomicState?.authToken = null
        }
    }
}

def onAppTouch(event) {
    updated()
}

def scheduler() {
    // runEvery1Minute("heartbeat")
    runEvery15Minutes("heartbeat")
    //runEvery30Minutes("heartbeat")
}

def heartbeat() {
    log.trace "heartbeat 15 minute keep alive poll()..."
    poll()
}

void initWebhooks() {
    settings?.controllers?.each { c->
        if(c) { 
            initWebhook(c)
            // log.debug "webhooks($c): ${getWebhookIdsForDev(c)}"
        }
    }
}

//Subscribes to the selected controllers API events that will be used to trigger a poll
def initWebhook(controlId) {
    //log.trace "initWebhook..."
    def result = false
    def whId = atomicState?.webhookIds ?: [:]
    def cmdType = whId[controlId] == null ? "post" : "put"
    def apiWebhookUrl = "${rootApiUrl()}/notification/webhook"
    def endpointUrl = apiServerUrl("/api/token/${atomicState?.accessToken}/smartapps/installations/${app.id}/rachioReceiver")
    def bodyData
    if(!whId[controlId]) { 
        bodyData = new JsonOutput().toJson([device:[id: controlId], externalId: app.id, url: endpointUrl, eventTypes: webhookEvtTypes()])
    } else { 
        bodyData = new JsonOutput().toJson([id: whId[controlId], externalId: app.id, url: endpointUrl, eventTypes: webhookEvtTypes()])
    }
    try {
        if(webhookHttp(apiWebhookUrl, bodyData, cmdType, controlId)) {
            log.info "Successfully ${cmdType == "post" ? "Created" : "Updated"} API Webhook Subscription for Controller (${controlId})!!!"
            result = true
        }
    } catch(ex) {
        log.error "initWebhook Exception: ${ex.message} | Data sent: ${bodyData}"
    }
    return result
}

//This isn't used for anything other than to return the webhooks for the device
def getWebhookIdsForDev(devId) {
    if(!devId) { return null }
    def data = _httpGet("notification/${devId}/webhook")
    def res = null
    if(data) { res = data?.findAll { it?.externalId == app?.id }?.collect { it?.id } }
    return res
}

void removeWebhookByDevId(devId) {
    def webhookIds = atomicState?.webhookIds
    if(webhookIds && webhookIds[devId] != null) {
        if(webhookHttp("${rootApiUrl()}/notification/webhook/${webhookIds[devId]}", "", "delete")) {
            log.warn "Removed API Webhook Subscription for (${webhookIds[devId]})"
        }
    }
}

//Removes the webhook subscription for the device.
void removeAllWebhooks() {
    def webhookIds = atomicState?.webhookIds
    if(settings?.controllers && webhookIds) {
        settings?.controllers?.each { c->
            if(c) {
                if(webhookHttp("${rootApiUrl()}/notification/webhook/${webhookIds[c]}", "", "delete")) {
                    log.warn "Removed API Webhook Subscription for (${webhookIds[c]})"
                }
            }
        }
    }
}

//Returns the available event types to subscribe to.
def webhookEvtTypes() {
    def typeIds = []
    def okTypes = ["DEVICE_STATUS_EVENT", "ZONE_STATUS_EVENT"] //, "WEATHER_INTELLIGENCE_EVENT", "RAIN_DELAY_EVENT"]
    def data = _httpGet("notification/webhook_event_type")
    if(data) {
        typeIds = data?.findAll { it?.name in okTypes }.collect { ["id":it?.id?.toString()] }
    }
    return typeIds
}

//Handles the http requests for the webhook methods
def webhookHttp(url, jsonBody, type=null, ctrlId) {
    //log.trace "webhookHttp($url, $jsonBody, $type)"
    def returnStatus = false
    def response = null
    def cmdParams = [
        uri: url,
        headers: ["Authorization": "Bearer ${atomicState?.authToken}", "Content-Type": "application/json"],
        body: jsonBody
    ]
    try {
        if(type == "post") {
            httpPost(cmdParams) { resp ->
                response = resp
            }
        }
        else if(type == "put") {
            httpPut(cmdParams) { resp ->
                response = resp
            }
        }
        else if(type == "delete") {
            httpDelete(cmdParams) { resp ->
                response = resp
            }
        }
        if(response) {
            //log.debug "response.status: ${response?.status} | data: ${response?.data}"
            if(response?.status in [200, 201, 204]) {
                returnStatus = true
                if(type in ["put", "post"]) {
                    def whIds = atomicState?.webhookIds ?: [:]
                    whIds[ctrlId] = response?.data?.id
                    atomicState?.webhookIds = whIds
                } else if(type == "delete") {
                    def whIds = atomicState?.webhookIds ?: [:]
                    whIds?.remove(ctrlId)
                    atomicState?.webhookIds = whIds
                }
            } else {
                //refresh the auth token
                if (response?.status == 401) {
                    log.debug "Refreshing your authToken!"
                    // refreshAuthToken()
                } else {
                    log.error "Authentication error, invalid authentication method, lack of credentials, etc."
                }
            }
        } else { return returnStatus }
    } catch(Exception e) {
        log.error "webhookHttp Exception Error: ", e
    }
    return returnStatus
}

def getDeviceIds() {
    return settings?.controllers ?: null
}

def getZoneIds(devId) {
    return settings?."${devId}_zones" ?: null
}

def getZoneData(userId, zoneId) {
    return _httpGet("person/${userId}/${zoneId}")
}

void updateDevZoneStates() {
    def devMap = [:]
    def userInfo = getUserInfo(atomicState?.userId)
    userInfo?.devices?.each { dev ->
        if(dev?.id in settings?.controllers) {
            devMap[dev?.id] = [:]
            devMap[dev?.id]["name"] = dev?.name
            def zoneMap = [:]
            dev?.zones?.each { zone ->
               if(zone?.id in settings?."${dev?.id}_zones") {
                    zoneMap[zone?.id] = [:]
                    zoneMap[zone?.id] = zone?.name
                }
            }
            devMap[dev?.id]["zones"] = zoneMap
        }
    }
    atomicState?.selectedDevices = devMap
}

def getDeviceInfo(devId) {
    //log.trace "getDeviceInfo..."
    return _httpGet("device/${devId}")
}

def getCurSchedule(devId) {
    //log.trace "getCurSchedule..."
    return _httpGet("device/${devId}/current_schedule")
}

def getDeviceData(devId) {
    //log.trace "getDeviceData($devId)..."
    return _httpGet("device_with_current_schedule/${devId}")
}

def rootApiUrl() { return "https://api.rach.io/1/public" }

def cleanupObjects(id){
    if(settings?."${id}_zones") { settingRemove("${id}_zones") }
    def whIds = atomicState?.webhookIds
    if(whIds && whIds[id]) { removeWebhookByDevId(id) }
}

def isWatering() {
    def i = atomicState?.isWateringMap?.findAll { it?.value == true }
    return (i?.size() > 0)
}

def removeWateringItem(id) {
    def i = atomicState?.isWateringMap ?: [:]
    if(id && i[id] != null) { i?.remove(id) }
    atomicState?.isWateringMap = i
}

def removeStandbyItem(id) {
    def i = atomicState?.inStandbyModeMap ?: [:]
    if(id && i[id] != null) { i?.remove(id) }
    atomicState?.inStandbyModeMap = i
}

def updateWateringItem(id, val) {
    def i = atomicState?.isWateringMap ?: [:]
    if(id && i != null) { i[id] = val }
    atomicState?.isWateringMap = i
}

def updateStandbyItem(String id, Boolean val) {
    def i = atomicState?.inStandbyModeMap ?: [:]
    if(id && i != null) { i[id] = val }
    atomicState?.inStandbyModeMap = i
}

def addRemoveDevices(uninst=false) {
    //log.trace "addRemoveDevices($uninst)..."
    try {
        def delete = []
        if(uninst == false) {
            def devsInUse = []
            def selectedDevices = atomicState?.selectedDevices
            selectedDevices?.each { contDev ->
                //Check if the discovered sprinklers are already initiated with corresponding device types.
                def d = getChildDevice(contDev?.key)
                if(!d) {
                    d = addChildDevice("acrosscable12814", getChildContName(), contDev?.key, null, [label: getDeviceLabelStr(contDev?.value?.name)])
                    d.completedSetup = true
                    log.info "Controller Device Created: (${d?.displayName}) with id: [${contDev?.key}]"
                } else {
                    //log.debug "found ${d?.displayName} with dni: ${dni?.key} already exists"
                }
                devsInUse += contDev.key
                contDev?.value?.zones?.each { zoneDni ->
                    //Check if the discovered sprinklers are already initiated with corresponding device types.
                    def d2 = getChildDevice(zoneDni?.key)
                    if(!d2) {
                        d2 = addChildDevice("acrosscable12814", getChildZoneName(), zoneDni?.key, null, [label: getDeviceLabelStr(zoneDni?.value)])
                        d2.completedSetup = true
                        log.info "Zone Device Created: (${d2?.displayName}) with id: [${zoneDni?.key}]"
                    } else {
                        //log.debug "found ${d2?.displayName} with dni: ${zoneDni?.key} already exists"
                    }
                    devsInUse += zoneDni?.key
                }
            }
            //log.debug "devicesInUse: ${devsInUse}"
            delete = app.getChildDevices(true).findAll { !(it?.deviceNetworkId in devsInUse) }
        } else {
            atomicState?.selectedDevices = []
            delete = app.getChildDevices(true)
        }
        if(delete?.size() > 0) {
            log.warn "Device Delete: ${delete} | Removing (${delete?.size()}) Devices..."
            delete?.each {
                cleanupObjects(it?.deviceNetworkId)
                deleteChildDevice(it?.deviceNetworkId, true)
                log.warn "Deleted the Device: ${it?.displayName}"
            }
        }
        return true
    } catch (ex) {
        if(ex instanceof physicalgraph.exception.ConflictException) {
            def msg = "Error: Can't Delete App because Devices are still in use in other Apps, Routines, or Rules.  Please double check before trying again."
            log.warn "addRemoveDevices Exception | $msg"
        }
        else if(ex instanceof physicalgraph.app.exception.UnknownDeviceTypeException) {
            def msg = "Error: Device Handlers are likely Missing or Not Published.  Please verify all device handlers are present before continuing."
            log.warn "addRemoveDevices Exception | $msg"
        }
        else { log.error "addRemoveDevices Exception: ${ex}" }
        return false
    }
}

def getDeviceLabelStr(name) {
    return "Rachio - ${name}"
}

def getTimeSinceInSeconds(time) {
    if(!time) { return 10000 }
    return (int) (now() - time)/1000
}

// This is the endpoint the webhook sends the events to...
def rachioReceiveHandler() {
    def reqData = request.JSON
    if(reqData?.size() || reqData == [:]) {
        // log.trace "eventDatas: ${reqData?.summary}"
        log.trace "Rachio Device Event | Summary: (${reqData?.summary}) | Requesting Latest Data from API | DeviceID: ${reqData?.deviceId}"
        if(reqData?.deviceId) {
            def dev = getChildDevice(reqData?.deviceId)
            poll(dev, "api")
        } else { poll() }
    }
}

//Section4: polling device info methods
void poll(child=null, type=null) {
    def lastPollSec = getTimeSinceInSeconds(atomicState?.lastPollDt)
    if(child && !type) { type = "device" }
    log.info "${app.label} -- Polling API for Latest Data -- Last Update was ($lastPollSec seconds ago)${type ? " | Reason: [$type]" : ""}"
    if(lastPollSec < 2) {
        runIn(3, "poll", [overwrite: true])
        //log.warn "Delaying poll... It's too soon to request new data"
        return
    }
    def selectedDevices = atomicState?.selectedDevices
    def ctrlCnt = 0
    def zoneCnt = 0
    selectedDevices?.each { cont->
        def devData = getDeviceData(cont?.key)
        def cDev = getChildDevice(cont?.key)
        if(cDev) {
            ctrlCnt = ctrlCnt+1
            pollChild(cDev, devData)
            cont?.value?.zones?.each { zone->
                zoneCnt = zoneCnt+1
                def zDev = getChildDevice(zone?.key)
                if(zDev) { pollChild(zDev, devData) }
            }
        }
    }
    log.info "Updating (${ctrlCnt}) Controllers and (${zoneCnt}) Zone devices..."
    atomicState?.lastPollDt = now()
}

def pollChild(child, devData) {
    if (pollChildren(child, devData)){
        //generate event for each (child) device type identified by different dni
    }
}

def pollChildren(childDev, devData) {
    //log.trace "updating child device (${child?.label})" // | ${child?.device?.deviceNetworkId})"
    try {
        if(childDev && devData) {
            String dni = childDev?.device?.deviceNetworkId
            String devLabel = childDev?.label
            def schedData = devData.currentSchedule
            def devStatus = devData
            def rainDelay = getCurrentRainDelay(devStatus)
            def status = devStatus?.status
            def onlStatus = status?.toString()?.toLowerCase() == "online" ? "online" : "offline"
            if(!childDev?.getDataValue("HealthEnrolled")) { childDev.updated() }
            Boolean pauseInStandby = settings?.pauseInStandby == false ? false : true
            Boolean inStandby = devData?.on.toString() != "true" ? true : false
            Boolean schedRunning = (schedData?.status == "PROCESSING") ? true : false
            def data = []
            Map selectedDevices = atomicState?.selectedDevices ?: [:]
            selectedDevices?.each { contDev ->
                if(dni == contDev?.key) {
                    updateStandbyItem(dni, inStandby)
                    // log.debug "schedRunning: ${schedRunning} | isWatering: ${isWatering()}"
                    if (isWatering() && !schedRunning) {
                        handleWateringSched(dni, false)
                    }
                    def newLabel = getDeviceLabelStr(devData?.name).toString()
                    if(devLabel != newLabel) {
                        childDev?.label = newLabel
                        log.info "Controller Label has changed from (${devLabel}) to [${newLabel}]"
                    }
                    data = [data: devData, schedData: schedData, rainDelay: rainDelay, status: status, standby: inStandby, pauseInStandby: pauseInStandby]
                } else {
                    contDev?.value?.zones?.each { zone ->
                        if (dni == zone?.key) {
                            def zoneData = findZoneData(zone?.key, devData)
                            def newLabel = getDeviceLabelStr(zone?.value).toString()
                            if(devLabel != newLabel) {
                                childDev?.label = newLabel
                                log.info "Zone Label has changed from (${devLabel}) to [${newLabel}]"
                            }
                            data = [data: zoneData, schedData: schedData, devId: contDev?.key, status: status, standby: inStandby, pauseInStandby: pauseInStandby]
                        }
                    }
                }
            }
            if (childDev && data != []) {
                childDev?.generateEvent(data)
                childDev?.sendEvent(name: "DeviceWatch-DeviceStatus", value: onlStatus, displayed: false)
            }
        } else { log.warn "pollChildren cannot update children because it is missing the required parameters..." }
    } catch(Exception ex) {
        log.error "exception polling children:", ex
        //refreshAuthToken()
    }
    return result
}

void setWateringDeviceState(devId, val) {
    // log.trace "setWateringDeviceState($devId, $val)"
    updateWateringItem(devId, val)
}

void handleWateringSched(devId, val=false) {
    // log.trace "handleWateringSched($devId, $val)"
    if(val == true) {
        log.trace "Watering is Active... Scheduling poll for every 1 minute"
        runEvery1Minute("poll")
    } else {
        log.trace "Watering has finished... 1 minute Poll has been unscheduled"
        unschedule("poll")
        runIn(60, "poll") // This is here just to make sure that the schedule actually stopped and that the data is really current.
    }
    updateWateringItem(devId, val)
}

def findZoneData(devId, devData) {
    if(!devId || !devData) { return null }
    if(devData?.zones) { return devData?.zones.find { it?.id == devId } }
    return null
}

def setValue(child, deviceId, newValue) {
    def jsonRequestBody = '{"value":'+ newValue+'}'
    def result = sendJson(child, jsonRequestBody, deviceId)
    return result
}

def sendJson(subUri, jsonBody, deviceId, standbyCmd=false) {
    //log.trace "Sending: ${jsonBody}"
    def returnStatus = false
    def cmdParams = [
        uri: "${apiEndpoint}/1/public/${subUri}",
        headers: ["Authorization": "Bearer ${atomicState?.authToken}", "Content-Type": "application/json"],
        body: jsonBody
    ]

    try{
        if(!standbyCmd && settings?.pauseInStandby == true && deviceId && atomicState?.inStandbyModeMap[deviceId] == true) {
            log.debug "Skipping this command while controller is in 'Standby Mode'..."
            return true
        }

        httpPut(cmdParams) { resp ->
            returnStatus = resp
            if(resp.status == 201 || resp.status == 204) {
                returnStatus = true
                //runIn(4, "poll", [overwrite: true])
            } else {
                //refresh the auth token
                if (resp.status == 401) {
                    log.debug "Refreshing your authToken!"
                    // refreshAuthToken()
                } else {
                    log.error "Authentication error, invalid authentication method, lack of credentials, etc."
                }
            }

        }
    } catch(Exception e) {
        log.error "sendJson Exception Error: ${e}"
    }
    return returnStatus
}

//Section6: helper methods ---------------------------------------------------------------------------------------------

def toJson(Map m) {
    return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
    return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def epochToDt(val) {
    return formatDt(new Date(val))
}

def formatDt(dt) {
    def tf = new SimpleDateFormat("MMM d, yyyy - h:mm:ss a")
    if(location?.timeZone) { tf?.setTimeZone(location?.timeZone) }
    else {
        log.warn "SmartThings TimeZone is not found or is not set... Please Try to open your ST location and Press Save..."
        return null
    }
    return tf.format(dt)
}

def getDurationDesc(long secondsCnt) {
    int seconds = secondsCnt %60
    secondsCnt -= seconds
    long minutesCnt = secondsCnt / 60
    long minutes = minutesCnt % 60
    minutesCnt -= minutes
    long hoursCnt = minutesCnt / 60

    return "${minutes} min ${(seconds >= 0 && seconds < 10) ? "0${seconds}" : "${seconds}"} sec"
}

//Returns time differences is seconds
def GetTimeValDiff(timeVal) {
    try {
        def start = new Date(timeVal).getTime()
        def now = new Date().getTime()
        def diff = (int) (long) (now - start) / 1000
        //log.debug "diff: $diff"
        return diff
    } catch (ex) {
        log.error "GetTimeValDiff Exception: ${ex}"
        return 1000
    }
}

def getChildContName()	{ return "Rachio Controller V2" }
def getChildZoneName()	{ return "Rachio Zone V2" }
def getServerUrl()		{ return "https://graph.api.smartthings.com" }
def getShardUrl()		{ return getApiServerUrl() }
def getAppEndpoint()	{ return "https://app.rach.io" }
def getApiEndpoint()	{ return "https://api.rach.io" }
def getSupportUrl() 	{ return "http://support.rachio.com/" }
def getCommunityUrl() 	{ return "http://community.rachio.com/" }

def debugEventFromParent(child, message) {
    child.sendEvent("name":"debugEventFromParent", "value":message, "description":message, displayed: true, isStateChange: true)
}

//send both push notification and mobile activity feeds
def sendPushAndFeeds(notificationMessage){
    if (atomicState.timeSendPush){
        if (now() - atomicState.timeSendPush > 86400000){
            sendPush("Rachio " + notificationMessage)
            sendActivityFeeds(notificationMessage)
            atomicState.timeSendPush = now()
        }
    } else {
        sendPush("Rachio " + notificationMessage)
        sendActivityFeeds(notificationMessage)
        atomicState.timeSendPush = now()
    }
    atomicState.authToken = null
}

def sendActivityFeeds(notificationMessage) {
    def devices = app.getChildDevices(true)
    devices.each { child ->
           //update(child)
        child.generateActivityFeedsEvent(notificationMessage)
    }
}

def standbyOn(child, deviceId) {
    log.debug "standbyOn() command received from ${child?.device?.displayName}"
    if(deviceId) {
        def jsonData = new JsonBuilder("id":deviceId)
        def res = sendJson("device/off", jsonData.toString(), deviceId, true)
        // poll()
        // child?.log("${child?.device.displayName} Standby OFF (Result: $res)")
        return res
    }
}

def standbyOff(child, deviceId) {
    log.debug "standbyOff() command received from ${child?.device?.displayName}"
    if(deviceId) {
        def jsonData = new JsonBuilder("id":deviceId)
        def res = sendJson("device/on", jsonData.toString(), deviceId, true)
        // // poll()
        // child?.log("${child?.device.displayName} Standby OFF (Result: $res)")
        return res
    }
}

def on(child, deviceId) {
    log.trace "App on()..."
}

def off(child, deviceId) {
    log.trace "Received off() command from (${child?.device?.displayName})..."
    // child?.log("Stop Watering - Received from (${child?.device.displayName})")
    if(deviceId) {
        def jsonData = new JsonBuilder("id":deviceId)
        def res = sendJson("device/stop_water", jsonData.toString(), deviceId)
        // poll()
        return res
    }
    return false
}

def setRainDelay(child, deviceId, delayVal) {
    //eeddir if (delayVal) {
        def secondsPerDay = 24*60*60;
        def duration = delayVal * secondsPerDay;
        def jsonData = new JsonBuilder("id":child?.device?.deviceNetworkId, "duration":duration)
        log.debug("Rachio Manager: ${duration}")
        def res = sendJson("device/rain_delay", jsonData?.toString(), deviceId)
		log.debug("Rachio Manager res : ${res}")
        if (res) { child?.sendEvent(name: 'rainDelay', value: delayVal) }
        return res
    //}
    return false
}

def isWatering(devId) {
    //log.debug "isWatering()..."
    def res = _httpGet("device/${devId}/current_schedule");
    def result = (res && res?.status) ? true : false
    return result
}

def getDeviceStatus(devId) {
    return _httpGet("device/${devId}")
}

def getControlLblById(id) {
    def dev = getChildDevice(id)
    return dev ? dev?.displayName : null
}

def getCurrentRainDelay(res) {
    log.debug("getCurrentRainDelay($devId)...")
    
    log.debug(res.rainDelayExpirationDate)
    log.debug(res.rainDelayStartDate)
    //log.debug(res.rainDelayExpirationDate)
    // convert to configured rain delay to days.
    //def ret =  (res?.rainDelayExpirationDate || res?.rainDelayStartDate) ? (res?.rainDelayExpirationDate - res?.rainDelayStartDate)/(26*60*60*1000) : 0
    //log.debug(ret)
    def value = 0//eeddir (long) Math.floor(ret + 0.5d)
    return value
}

def startZone(child, deviceId, zoneNum, mins) {
    def res = false
    def ctrlLbl = getControlLblById(deviceId)
    log.trace "Starting to Water on ${ctrlLbl ? "$ctrlLbl: " : ""}(ZoneName: ${child?.device.displayName} | ZoneNumber: ${zoneNum} | RunDuration: ${mins})"
    //child?.log("Starting to water on (ZoneName: ${child?.device.displayName} | ZoneNumber: ${zoneNum} | RunDuration: ${mins})...")
    def zoneId = child?.device?.deviceNetworkId
    if (zoneId && zoneNum && mins) {
        def duration = mins.toInteger() * 60
        def jsonData = new JsonBuilder("id":zoneId, "duration":duration)
        //log.debug "startZone jsonData: ${jsonData}"
        res = sendJson("zone/start", jsonData?.toString(), deviceId)
    } else { log.error "startZone Missing ZoneId or duration... ${zoneId} | ${mins}" }
    return res
}

def runAllZones(child, deviceId, mins) {
    def res = false
    log.trace "runAllZones(ZoneName: ${child?.device?.displayName}, Duration: ${mins})..."
    //child?.log("runAllZones(ZoneName: ${child?.device?.displayName} | Duration: ${mins})")
    def selectedDevices = atomicState?.selectedDevices ?: [:]
    if (deviceId && selectedDevices[deviceId] && selectedDevices[deviceId]?.zones && mins) {
        def zoneData = []
        def sortNum = 1
        def duration = mins.toInteger() * 60
        selectedDevices[deviceId]?.zones?.each { z ->
            zoneData << ["id":z?.key, "duration":duration, "sortOrder": sortNum]
            sortNum = sortNum+1
        }
        def jsonData = new JsonBuilder("zones":zoneData)
        //child?.log("runAllZones  jsonData: ${jsonData}")
        res = sendJson("zone/start_multiple", jsonData?.toString(), deviceId)
    } else { log.error "runAllZones Missing ZoneIds or Duration... ${selectedDevices[deviceId]?.zones} | ${mins}" }
    return res
}

def pauseScheduleRun(child) {
    log.trace "pauseScheduleRun..."
    def schedData = getCurSchedule(atomicState?.deviceId)
    def schedRuleData = getScheduleRuleInfo(schedData?.scheduleRuleId)
    child?.log "schedRuleData: $schedRuleData"
    child?.log "Schedule Started on: ${epochToDt(schedRuleData?.startDate)} | Total Duration: ${getDurationDesc(schedRuleData?.totalDuration.toLong())}"

    if(schedRuleData) {
        def zones = schedRuleData?.zones?.sort { a , b -> a.sortOrder <=> b.sortOrder }
        zones?.each { zn ->
            child?.log "Zone#: ${zn?.zoneNumber} | Zone Duration: ${getDurationDesc(zn?.duration.toLong())} | Order#: ${zn?.sortOrder}"
            if(zn?.zoneId == schedData?.zoneId) {
                def zoneRunTime = "Elapsed Runtime: ${getDurationDesc(GetTimeValDiff(schedData?.zoneStartDate.toLong()))}"
                child?.log "Zone Started: ${epochToDt(schedData?.zoneStartDate)} | ${zoneRunTime} | Cycle Count: ${schedData?.cycleCount} | Cycling: ${schedData?.cycling}"
            }
        }
    }
}

//Required by child devices
def getZones(device) {
    log.trace "getZones(${device.label})..."
    def res = _httpGet("device/${device?.deviceNetworkId}")
    return !res ? null : res?.zones
}

def getScheduleRuleInfo(schedId) {
    def res = _httpGet("schedulerule/${schedId}")
    return res
}