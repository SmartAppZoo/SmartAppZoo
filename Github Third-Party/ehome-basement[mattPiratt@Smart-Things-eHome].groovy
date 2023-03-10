/**
 *  eHome pyServer Manager
 *
 *  Copyright 2018 Bartosz Kubek
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
        name: "eHome Basement",
        namespace: "mattPiratt",
        author: "Bartosz Kubek",
        description: "Smart App for eHome basement controller.",
        category: "Safety & Security",
        iconUrl: "https://www.easyicon.net/download/png/1161404/64/",
        iconX2Url: "https://www.easyicon.net/download/png/1161404/128/",
        iconX3Url: "https://www.easyicon.net/download/png/1161404/128/")


preferences {

    section("eHome pyServer Setup"){
        input "pyServerIP", "text", "title": "pyServer address", multiple: false, required: true
        input "pyServerPort", "text", "title": "pyServer Port", multiple: false, required: true
        input "pyServerPass", "text", "title": "pyServer API Key", multiple: false, required: true
        input "theHub", "hub", title: "On which hub?", multiple: false, required: true
    }
}

def getRelaysConfig() {
    //warning: those are keys in pyServer:server.py:168
    return [
            "runningWaterPump": [name: "Hot water pump", defaultState: "off"],
            "floorHeatingPump": [name: "Floor heating pump", defaultState: "on"],
            "radiatorsPump": [name: "Radiators pump", defaultState: "on"],
            "sprinklerZone1Active": [name: "Sprinkler zone 1", defaultState: "off"],
            "sprinklerZone2Active": [name: "Sprinkler zone 2", defaultState: "off"],
            "sprinklerZone3Active": [name: "Sprinkler zone 3", defaultState: "off"],
            "sprinklerZone4Active": [name: "Sprinkler zone 4", defaultState: "off"],
    ]
};
def getThermometerConfig() {
    return [
//            "intTemp1": [name: "Internal Temperature sensor"],
            "extTemp": [name: "External Temperature sensor"],
            "waterTemp": [name: "Running Water Temperature sensor"]
    ]
};
def getStoveConfig() {
    return [
            "stove": [name: "Stove"]
    ]
};

def installed() {
//    log.debug "installed(): with settings: ${settings}"

    initialize()
}

def initialize() {
//    log.debug "initialize()"

    //init timestamp of last BoulerHouse write request
    state.bhts = now()

    subscribe(location, null, responseHandler, [filterEvents:false])

    relaysConfig.each { deviceCodeName, deviceConfig ->
        setupVirtualDevice(deviceConfig['name'], "switch", deviceCodeName, deviceConfig['defaultState']);
    }
    thermometerConfig.each { deviceCodeName, deviceConfig ->
        setupVirtualDevice(deviceConfig['name'], "temperatureSensor", deviceCodeName);
    }
    stoveConfig.each { deviceCodeName, deviceConfig ->
        setupVirtualDevice(deviceConfig['name'], "boilerHouseStove", deviceCodeName);
    }

    updateDevicesStatePeriodically();
}

def updated() {
//    log.debug "Updated(): with settings: ${settings}"

    unsubscribe();

    //init timestamp of last BoulerHouse write request
    state.bhts = now()

    relaysConfig.each { deviceCodeName, deviceConfig ->
        updateVirtualDevice(deviceConfig['name'], "switch", deviceCodeName, deviceConfig['defaultState']);
    }
    thermometerConfig.each { deviceCodeName, deviceConfig ->
        updateVirtualDevice(deviceConfig['name'], "temperatureSensor", deviceCodeName);
    }
    stoveConfig.each { deviceCodeName, deviceConfig ->
        updateVirtualDevice(deviceConfig['name'], "boilerHouseStove", deviceCodeName);
    }

    updateDevicesStatePeriodically();

    subscribe(location, null, responseHandler, [filterEvents:false]);
}

def updateVirtualDevice(deviceName, deviceType, deviceCodeName, defaultState="on") {
    log.debug "updateVirtualDevice(): deviceName: ${deviceName}; deviceType: ${deviceType}; deviceCodeName: ${deviceCodeName}"

    // If user didn't fill this device out, skip it
    if(!deviceName) return;

    def theDeviceNetworkId = "";
    switch(deviceType) {
        case "switch":
            theDeviceNetworkId = getRelayID(deviceCodeName);
            break;

        case "temperatureSensor":
            theDeviceNetworkId = getTemperatureID(deviceCodeName);
            break;
        case "boilerHouseStove":
            theDeviceNetworkId = getStoveID(deviceCodeName);
            break;
    }

//    log.debug "updateVirtualDevice(): Searching for: $theDeviceNetworkId";

    def theDevice = getChildDevices().find{ d -> d.deviceNetworkId.startsWith(theDeviceNetworkId) }

    if(theDevice){ // The switch already exists
//        log.debug "updateVirtualDevice(): Found existing device which we will now update: label: ${theDevice.label}, name: ${theDevice.name}"

        switch(deviceType) {
            case "switch":
                subscribe(theDevice, "switch", switchStateChange);
//                log.debug "updateVirtualDevice(): Subscription to event 'switch' is done. Setting initial state of $deviceName to ${defaultState}";
                setDeviceStateOnPyServer(deviceCodeName, defaultState);
                if (defaultState == "on") {
                    theDevice.on();
                } else {
                    theDevice.off();
                }
                break;
            case "boilerHouseStove":
                log.debug "updateVirtualDevice(): Subscription to event 'coalLvlReset' done for ${theDevice}";
                subscribe(theDevice, "switch", coalLevelReset);
                break;
            default:
                log.debug "updateVirtualDevice(): No use case for ${theDevice}";
                break;
        }
    } else { // The switch does not exist
        log.debug "updateVirtualDevice(): This device does not exist, creating a new one now";
        setupVirtualDevice(deviceName, deviceType, deviceCodeName, defaultState);
    }

}
def setupVirtualDevice(deviceName, deviceType, deviceCodeName, defaultState="on") {

//    log.debug "setupVirtualDevice()"

    if(deviceName){
        log.debug "setupVirtualDevice(): deviceName: ${deviceName}; deviceType: ${deviceType}; deviceCodeName: ${deviceCodeName}"

        switch(deviceType) {
            case "switch":
//                log.trace "setupVirtualDevice(): Setting up a eHome Basement Relay called $deviceName with Device ID #$deviceCodeName"
                def theDevice = addChildDevice("mattPiratt", "eHome Basement Relay", getRelayID(deviceCodeName), theHub.id, [label:deviceName, name:deviceName])
                subscribe(theDevice, "switch", switchStateChange);

//                log.debug "setupVirtualDevice(): Setting initial state of ${deviceName} at pyServer into ${defaultState}"
                setDeviceStateOnPyServer(deviceCodeName, defaultState);
                if( defaultState == "on") {
                    theDevice.on();
                } else {
                    theDevice.off();
                }
                break;

            case "temperatureSensor":
//                log.trace "setupVirtualDevice(): Setting up a eHome Basement Thermometer called $deviceName on $deviceCodeName"
                def theDevice = addChildDevice("mattPiratt", "eHome Basement Thermometer", getTemperatureID(deviceCodeName), theHub.id, [label:deviceName, name:deviceName])
                //TODO: is this really required? I dont see any use of this
                state.temperatureZone = deviceCodeName
                break;
            case "boilerHouseStove":
                log.trace "setupVirtualDevice(): Setting up a eHome Basement Stove called $deviceName on $deviceCodeName"
                def theDevice = addChildDevice("mattPiratt", "eHome Basement Stove", getStoveID(deviceCodeName), theHub.id, [label:deviceName, name:deviceName])
                subscribe(theDevice, "switch", coalLevelReset);
                break;
        }
    }
}


def String getRelayID(deviceCodeName) {

    return "pyServerRelay." + deviceCodeName
}
def String getTemperatureID(deviceCodeName){

    return  "pyServerTempSensor." + deviceCodeName
}
def String getStoveID(deviceCodeName){

    return  "pyServerStoveDTH." + deviceCodeName
}

def uninstalled() {
//    log.debug "uninstalled()"
    unsubscribe()
    def delete = getChildDevices()
    delete.each {
        unsubscribe(it)
        log.trace "uninstalled(): about to delete device ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
}

def responseHandler(evt){
    try {
        def msg = parseLanMessage(evt.description);
        log.debug "responseHandler/msg:${msg}"
//        log.debug "responseHandler/data:${parseJson(evt.data)}"
//        log.debug "responseHandler/device:${evt.device}"
//        log.debug "responseHandler/displayName:${evt.displayName}"
//        log.debug "responseHandler/deviceId:${evt.deviceId}"
//        log.debug "responseHandler/name:${evt.name}"
//        log.debug "responseHandler/surce:${evt.source}"
//        log.debug "responseHandler/stringValue:${evt.stringValue}"
//        log.debug "responseHandler/isStateChange:${evt.isStateChange()}"

        if (msg.json?.radiatorsPump) {
            if (msg.json) {
                def children = getChildDevices(false)
                msg.json.each { item ->
//                    log.debug "responseHandler(): each() item.key: ${item.key}; item.value: ${item.value}; children: ${children}"

                    if (relaysConfig[item.key]) {
                        updateRelayDevice(item.key, item.value, children);
                    } else if (thermometerConfig[item.key]) {
                        updateThermometerDevice(item.key, item.value, children);
                    } else if (item.key == "stoveCoalLvl" || item.key == "stoveTemp") {
                        updateStoveDevice(item.key, item.value, children);
                    }
                }

//                log.debug "responseHandler(): Finished seting Relay virtual switches"
            }
        }
    } catch (NullPointerException e) {
        log.debug "responseHandler(): There is an ERROR in responseHandler()!: "
        log.debug msg
    }
}

def updateRelayDevice(attributeName, newState, childDevices) {
    def theSwitch = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$attributeName") }
    if(theSwitch) {
//        log.debug "updateRelayDevice(): Updating switch $theSwitch for deviceNetworkId: ${theSwitch.deviceNetworkId} with value $newState"
        theSwitch.changeSwitchState(newState)
    }
}

def updateThermometerDevice(attributeName, temperature, childDevices){
    def theThermometer = childDevices.find{ d -> d.deviceNetworkId.endsWith(".$attributeName") }
    if(theThermometer) {
//        log.debug "updateThermometerDevice(): Updating thermometer $theThermometer for deviceNetworkId: ${theThermometer.deviceNetworkId} with value $temperature"
        theThermometer.setTemperature(temperature,state.temperatureZone)
    }
}
def updateStoveDevice(attributeName, attributeValue, childDevices){
    def theDevice = childDevices.find{ d -> d.deviceNetworkId.endsWith(".stove") }
    if(theDevice) {
        switch(attributeName) {
            case "stoveCoalLvl":
//                log.debug "updateStoveDevice(): Updating coal level of $theDevice for deviceNetworkId: ${theDevice.deviceNetworkId} with value $attributeValue"
                theDevice.setLevel(attributeValue)
                break;
            case "stoveTemp":
//                log.debug "updateStoveDevice(): Updating temperature of $theDevice for deviceNetworkId: ${theDevice.deviceNetworkId} with value $attributeValue"
                theDevice.setTemperature(attributeValue)
                break;
        }
    }
}

def updateDevicesStatePeriodically() {
    log.trace "updateDevicesStatePeriodically(): Poll data from from pyServer"
    getDevicesStateFromPyServer();
    runEvery10Minutes(updateDevicesStatePeriodically);
}

def switchStateChange(evt){
    log.debug "switchStateChange(): evt: ${evt}; evt.value: ${evt.value}"
    if(evt.value == "on" || evt.value == "off") {
        def parts = evt.device.deviceNetworkId.tokenize('.');
        def deviceCodeName = parts[1];
        log.debug "switchStateChange(): state: ${evt.value}; parts: ${parts}; deviceCodeName: ${deviceCodeName}"
        setDeviceStateOnPyServer(deviceCodeName, evt.value);
    }
}

// jsWatcher needs max 1 minute for the "reset coal level" signal to be processed
// and then SmartApp is polling data once per 5 mins.
// so until this happens, I am faking here the level of coal to 100%
def coalLevelTemporaryFakeState() {
    def theDevice = childDevices.find{ d -> d.deviceNetworkId.endsWith(".stove") }
    theDevice.setLevel(100);
}

def coalLevelReset(evt){
    log.debug "coalLevelReset(): evt: ${evt}; evt.value: ${evt.value}";
    if( evt.value == "on" || evt.value == "off") {
        setDeviceStateOnPyServer("stoveCoalLevelReset", "on");
        runIn(5, coalLevelTemporaryFakeState);
    }
}



def getDevicesStateFromPyServer() {
//    log.debug "getDevicesStateFromPyServer():";

    def lastBHWriteTSPlus10s = state.bhts + 10000
    def nowTS = now()

    if( lastBHWriteTSPlus10s < nowTS ) {
        log.debug "getDevicesStateFromPyServer(): IS REQUEST! bhts: ${state.bhts}; " +
                "lastBHWriteTSPlus10s: ${lastBHWriteTSPlus10s}; nowTS: ${nowTS}"
        def Path = "/bh/getCurrent";
        executeRequestToPyServer(Path, "GET");
    } else {
        log.debug "getDevicesStateFromPyServer(): No request! bhts: ${state.bhts}; " +
                "lastBHWriteTSPlus10s: ${lastBHWriteTSPlus10s}"
    }
}

def setDeviceStateOnPyServer(deviceCodeName, evtState) {
    def Path = "/setFlag";
    def val = (evtState == "on") ? true : false;
    state.bhts = now()
    log.debug "setDeviceStateOnPyServer(): deviceCodeName: ${deviceCodeName}; " +
            "state: ${evtState}; bhts: ${state.bhts}";
    executeRequestToPyServer(Path, "POST", ["flag":deviceCodeName,"value":val]);
}

def executeRequestToPyServer(Path, method, params=[]) {

//    log.debug "executeRequestToPyServer(): Path:" + Path + "; method: "+method+"; params: " +params

    def headers = [:]
    headers.put("HOST", "$settings.pyServerIP:$settings.pyServerPort")
//    log.debug "executeRequestToPyServer(): HOST: $settings.pyServerIP:$settings.pyServerPort"

    try {
        def actualAction = new physicalgraph.device.HubAction(
                method: method,
                path: "/api/$settings.pyServerPass"+Path,
                headers: headers,
                body: params
        )

        sendHubCommand(actualAction)
//        log.debug "executeRequestToPyServer(): sendHubCommand done!"
    }
    catch (Exception e) {
        log.error "executeRequestToPyServer(): Hit Exception $e on $hubAction"
    }
}

