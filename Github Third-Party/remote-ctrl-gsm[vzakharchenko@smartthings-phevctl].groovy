/**
 *  remote-ctrl-gsm
 *
 *  Copyright 2021 Vasyl Zakharchenko
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
        name: "remote-ctrl-gsm",
        namespace: "vzakharchenko",
        author: "Vasyl Zakharchenko",
        description: "Remote Control Outlander PHEV over Mobile Network",
        category: "My Apps",
        iconUrl: "https://github.com/vzakharchenko/remote-ctrl-gsm/blob/master/OUTLANDER_PHEV_REMOTE_APK/res/drawable-ldpi/app_launchicon.png?raw=true",
        iconX2Url: "https://github.com/vzakharchenko/remote-ctrl-gsm/blob/master/OUTLANDER_PHEV_REMOTE_APK/res/drawable-mdpi/app_launchicon.png?raw=true",
        iconX3Url: "https://github.com/vzakharchenko/remote-ctrl-gsm/blob/master/OUTLANDER_PHEV_REMOTE_APK/res/drawable-hdpi/app_launchicon.png?raw=true")


preferences {
    section("Setup my device with this IP") {
        input "IP", "text", multiple: false, required: true
    }
    section("Setup my device with this Port") {
        input "port", "number", multiple: false, required: true
    }
    section("Setup my devices with smartthings hub (optional)") {
        input "hub", "capability.hub", multiple: false, required: false
    }
    section("Setup my devices without cloud") {
        input(name: "withoutCloud", type: 'bool', required: false, defaultValue:false)
    }
}

def installed() {
    createAccessToken()
    getToken()
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def getToken() {
    if (!state.accessToken) {
        try {
            getAccessToken()
            debug("Creating new Access Token: $state.accessToken")
        } catch (ex) {
            debug(ex)
        }
    }
}

def initialize() {
    def devices = getAllDevices().each {
        subscribe(it, "switch.on", deviceHandler);
    }
    runEvery15Minutes(handlerOnline)
}

mappings {
    path("/smartapp/check") {
        action:
        [
                GET: "phevInit"
        ]
    }
    path("/smartapp/addDevice") {
        action:
        [
                POST: "phevAddDevice"
        ]
    }
    path("/smartapp/updateDevice") {
        action:
        [
                POST: "updateDevice"
        ]
    }
    path("/smartapp/deleteDevice") {
        action:
        [
                POST: "deleteAddDevice"
        ]
    }
    path("/smartapp/allDevices") {
        action:
        [
                GET: "phevDevices"
        ]
    }
    path("/smartapp/statusDevices") {
        action:
        [
                GET: "phevGetStatusDevices"
        ]
    }
    path("/smartapp/offDevice") {
        action:
        [
                POST: "phevOffDevice"
        ]
    }

    path("/smartapp/notification") {
        action:
        [
                POST: "sendNotification"
        ]
    }
}

def phevInit() {
    updateState();
    return [status: "ok", useCloud: withoutCloud == null || !withoutCloud]
}

def handlerOnline() {
    def timeout = 1000 * 60 * 20;
    def curTime = new Date().getTime();
    getAllDevices().each {
        def activeDate = state.lastcheck;
        if ((curTime - timeout) > activeDate) {
            it.markDeviceOffline();
            debug("PHEV offline ${curTime - timeout} > ${activeDate} ")
        } else {
            it.markDeviceOnline();
            debug("PHEV online  ${curTime - timeout} < ${activeDate} ")
        }
    }
}

def updateState(){
    state.lastcheck = new Date().getTime();
// state.lastcheck = 0;
}

def phevDevices() {
    updateState();
    def deviceList = [];
    def devices = getAllDevices().each {
        deviceList.push([id: it.getDeviceNetworkId(), label: it.label ])
    }
    return [devices: deviceList]
}


def phevGetStatusDevices() {
    updateState();
    handlerOnline();
    def deviceList = [];
    def devices = getAllDevices().each {
        def st = it.currentState("switch");
        debug("switch "+st)
        deviceList.push([id: it.getDeviceNetworkId(), label: it.label, status: st  ])
    }
    return [devices: deviceList]
}

def updateDevice() {
    updateState();
    handlerOnline();
    def json = request.JSON;
    debug("update device "+json)
    def presentDevice = getAllDevicesById(json.id)
    presentDevice.update(json.value)
    if (json.value2){
        presentDevice.update2(json.value2)
        presentDevice.updateall(json.value,json.value2)
    }

    return [status: "ok"]
}


def phevAddDevice() {
    updateState();
    def json = request.JSON;
    def presentDevice = getAllDevicesById(json.id)
    if (presentDevice == null) {
        if ("battery".equals(json.actionId)){
            presentDevice = addChildDevice("vzakharchenko", "Outlander PHEV Battery", json.id, null, [label: "${json.deviceLabel}", name: "${json.deviceLabel}"])
        } else if ("doors".equals(json.actionId)){
            presentDevice = addChildDevice("vzakharchenko",  "Outlander PHEV Doors", json.id, null, [label: "${json.deviceLabel}", name: "${json.deviceLabel}"])
        } else if ("hvac".equals(json.actionId)){
            presentDevice = addChildDevice("vzakharchenko",  "Outlander PHEV Thermostat", json.id, null, [label: "${json.deviceLabel}", name: "${json.deviceLabel}"])
        }  else {
            presentDevice = addChildDevice("vzakharchenko", "Outlander PHEV Action", json.id, null, [label: "${json.deviceLabel}", name: "${json.deviceLabel}"])
        }
        presentDevice.markDeviceOnline()
        presentDevice.forceOff();
        updated();
    }
    return [status: "ok"]
}


def deleteAddDevice() {
    updateState();
    def json = request.JSON;
    deleteChildDevice(json.id)
    return [status: "ok"]
}

def sendNotification(){
    updateState();
    def json = request.JSON;
    sendPush(json.message)
    return [status: "ok"]
}


def phevOffDevice() {
    updateState();
    def json = request.JSON;
    debug("json=${json}");
    def presentDevice = getAllDevicesById(json.id);
    presentDevice.forceOff();
    return [status: "ok"]
}

def getAllDevicesById(id) {
    updateState();
    def device;
    def devices = getAllDevices().each {
        if (it.getDeviceNetworkId() == id) {
            device = it;
        }
    }
    return device;
}

def getAllDevices() {
    return childDevices;
}


def deviceHandler(evt) {
    if (withoutCloud != null && withoutCloud){
        return;
    }
    if (hub){
        apiHubGet("/${app.id}/${state.accessToken}/execute?deviceId=${evt.getDevice().getDeviceNetworkId()}",null)
    } else {
        apiGet("/${app.id}/${state.accessToken}/execute?deviceId=${evt.getDevice().getDeviceNetworkId()}");
    }
}

def apiGet(path) {
    def url = "http://${IP}:${port}";
    debug("request:  ${url}${path}");
    httpGet(uri: "${url}${path}");
}

def apiHubGet(path, query) {
    def url = "${IP}:${port}";
    debug("request:  ${url}${path} query= ${query}")
    def result = new physicalgraph.device.HubAction(
            method: 'GET',
            path: path,
            headers: [
                    HOST  : url,
                    Accept: "*/*"
            ],
            query: query
    )

    return sendHubCommand(result)
}

def debug(message) {
    def debug = false;
    if (debug) {
        log.debug message
    }
}
