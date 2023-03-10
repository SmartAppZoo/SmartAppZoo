/**
 *  RpiSvcMgr
 *
 *  Copyright 2018 abchez
 *
 */
definition(
    name: "RpiSvcMgr",
    namespace: "abchez",
    author: "abchez",
    description: "HTTP sensor server svc mgr",
    category: "My Apps",
    iconUrl: "https://drive.google.com/uc?id=1CX4QK9ydTek2BHUEVnoxN9E69rbhwDgs&export=download",
    iconX2Url: "https://drive.google.com/uc?id=1CX4QK9ydTek2BHUEVnoxN9E69rbhwDgs&export=download",
    iconX3Url: "https://drive.google.com/uc?id=1CX4QK9ydTek2BHUEVnoxN9E69rbhwDgs&export=download")



preferences {
    page(name:"page1")
}

def page1() {
    dynamicPage(name:"page1", title:"Device Setup", install: true, uninstall: true) {
    	section {
        	input(name: "rpiIP", type: "text", title: "IP address", required: true)
        	input(name: "rpiPort", type: "number", title: "Port", defaultValue: 8800, required: true)
        	input(name: "rpiLabel", type: "text", title: "Name", required: false)
            if (app.getInstallationState() == "COMPLETE") {
	            def c = getChildDevices().find { d -> true }
                if (!c) {
                    paragraph "HTTP sensor server device not installed or deleted."
                } else {
                    paragraph "HTTP sensor server device ${c.getDeviceNetworkId()} is ${c.currentValue("onlineState")}"
                }
                if (state.setupPending) {
                    paragraph "Setup pending..."
                }
                def errorState = app.currentState("error")
                if (errorState && errorState.value != "") {
                	paragraph "${app.currentState("error").value}"
                }
            }
        }
    }
}

def installed() {
    Log("Installed with settings: ${settings}")
	initialize()
}

def uninstalled() {
}

def childUninstalled() {
}

def updated() {
	Log("Updated with settings: ${settings}")
    unschedule()
    unsubscribe()

    initialize()
}

def initialize() {

    def appLabel = settings.rpiLabel;
    if (appLabel == null || appLabel == "" || appLabel == "RpiSvcMgr") {
        appLabel = "HTTP sensors ${rpiIP}";
    }
    
    app.updateLabel(appLabel)
    
    startSetup()
}

def startSetup () {
logEx {
    Log("startSetup")
    state.setupPending = true
    state.setupUUID = Long.toHexString((Math.random() * 0x100000).round())
    clearError()
    runIn(60, setupTimeout, [data: [setupUUID: state.setupUUID]])

    subscribeRPI()
}
}

def setupTimeout(data) {
logEx {
	if (state.setupUUID != data.setupUUID)
    	return;
    
    state.setupPending = false

    Log("TIMEOUT setting up HTTP sensor server device")
    def errorState = app.currentState("error")
    if (!errorState || errorState.value == "") {
        setError("TIMEOUT setting up HTTP sensor server device")
    }

    getChildDevices().each {
        d -> d.setOffline()
        sendPushMessage("${d.getLabel()} is offline.");
    }
}
}

def subscribeRPI () {
    Log("subscribeRPI")
    def hub = location.getHubs()[0]
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "PUT",
        path: "/subscribe",
        body: [ uuid: state.setupUUID, hubIP: hub.getLocalIP(), hubPort: hub.getLocalSrvPortTCP() ]
    ], "${rpiIP}:${rpiPort}", [callback: subscribeRPIcallback]))
}

def cmdPause() {
    Log("pause")
    
    sendHubCommand(new physicalgraph.device.HubAction([
    	method: "POST",
        path: "/pauseOutput"
    ], "${rpiIP}:${rpiPort}"))
}

def subscribeRPIcallback(physicalgraph.device.HubResponse hubResponse) {
logEx {
    if (!state.setupPending)
    	return;

    if (hubResponse?.json?.uuid == "${state.setupUUID}") {
        Log("subscribeRPIcallback ${state.setupUUID}")
        def piDevices = hubResponse?.json?.devices;
        if (piDevices?.size() > 0) {
            return createOrUpdateComponentdevice(hubResponse.hubId, hubResponse.mac, piDevices)
        }
    }
}
}

def createOrUpdateComponentdevice(hubId, mac, piDevices) {
    Log("createOrUpdateComponentdevice ${rpiLabel}")
    
    def appLabel = settings.rpiLabel;
    if (appLabel == null || appLabel == "" || appLabel == "RpiSvcMgr") {
        appLabel = "HTTP sensors ${rpiIP} ${mac}";
    }
    app.updateLabel(appLabel)
    
    def delete = getChildDevices().findAll { d -> d.getDeviceNetworkId() != mac }
    delete.each { d -> deleteChildDevice(d.getDeviceNetworkId()) }

    def piComponentDevice = getChildDevices()?.find { d -> d.getDeviceNetworkId() == mac }
    if (!piComponentDevice) {
        Log("Creating HTTP sensor server device with dni: ${mac}")
        def deviceLabel = "HTTP sensors ${rpiIP}"
        piComponentDevice = addChildDevice("abchez", "RPI Component Device", mac, hubId, [label: deviceLabel])
        piComponentDevice.setId()
    }
    
    if (piComponentDevice.refreshChildren(piDevices)) {
        setSetupCompleted()
    }
}

def setSetupCompleted() {

    getChildDevices().each { 
    	d -> d.setOnline()
        sendPushMessage("${d.getLabel()} is online.");
    }

    state.setupUUID = null
    state.setupPending = false
    clearError()
    unschedule("setupTimeout")
    
    runEvery15Minutes(checkOnlineState)

    state.lastSetupCompleted = new Date ()
    Log("SUCCESS setting up HTTP sensor server device")
}

def checkOnlineState() {
logEx {
    getChildDevices().each { d -> 
        def currentState = d.currentValue("onlineState")
        def minsSinceLastNotification = d.minsSinceLastNotification()
        Log("checkOnlineState ${minsSinceLastNotification}")
        if (currentState == "online") {
            if (minsSinceLastNotification > 15) {
                d.setOffline()
                //sendPushMessage("${d.getLabel()} is offline.");
                Log("${d.getLabel()} is offline.");
            }
        } else {
            if (minsSinceLastNotification <= 15) {
                d.setOnline()
                sendPushMessage("${d.getLabel()} is back online.");
                
                // force notification of current device states
                def hub = location.getHubs()[0]
                sendHubCommand(new physicalgraph.device.HubAction([
                    method: "PUT",
                    path: "/subscribe",
                    body: [ uuid: "keepalive", hubIP: hub.getLocalIP(), hubPort: hub.getLocalSrvPortTCP() ]
                ], "${rpiIP}:${rpiPort}", [callback: subscribeForceNotificationCallback]))
            }
        }
    }
}
}

def subscribeForceNotificationCallback(physicalgraph.device.HubResponse hubResponse) {
logEx {
    Log("subscribeForceNotificationCallback ${hubResponse?.json?.uuid}")
}
}

def Log(String text) {
    sendEvent(name: "log", value: text)
    //log.debug text
}

def setError(String text) {
    state.setErrorTime = new Date ()
    sendEvent(name: "error", value: text)
}

def clearError() {
    def errorState = app.currentState("error")
    if (errorState && errorState.value != "") {
        sendEvent(name: "error", value: "")
        state.setErrorTime = null
    }
}

def logEx(closure) {
    try {
        closure()
    }
    catch (e) {
        setError("${e}")
        //Log("EXCEPTION error: ${e}")
        throw e
    }
}