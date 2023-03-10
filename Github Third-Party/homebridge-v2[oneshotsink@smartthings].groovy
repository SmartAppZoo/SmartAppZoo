/**
 *  Homebridge SmartThing Interface
 *  Loosely Modelled off of Paul Lovelace's JSON API
 *  Copyright 2018, 2019, 2020 Anthony Santilli
 */

String appVersion()                     { return "2.0.4" }
String appModified()                    { return "12-23-2019" }
String branch()                         { return "master" }
String platform()                       { return "SmartThings" }
String pluginName()                     { return "${platform()}-v2" }
String appIconUrl()                     { return "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/${branch()}/images/hb_tonesto7@2x.png" }
String getAppImg(imgName, ext=".png")   { return "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/${branch()}/images/${imgName}${ext}" }
Map minVersions()                       { return [plugin: 209] }

definition(
    name: "Homebridge v2",
    namespace: "tonesto7",
    author: "Anthony Santilli",
    description: "Provides the API interface between Homebridge (HomeKit) and ${platform()}",
    category: "My Apps",
    iconUrl:   "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/master/images/hb_tonesto7@1x.png",
    iconX2Url: "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/master/images/hb_tonesto7@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/master/images/hb_tonesto7@3x.png",
    oauth: true)

{
    appSetting "devMode"
}

preferences {
    page(name: "startPage")
    page(name: "mainPage")
    page(name: "defineDevicesPage")
    page(name: "deviceSelectPage")
    page(name: "changeLogPage")
    page(name: "capFilterPage")
    page(name: "virtDevicePage")
    page(name: "developmentPage")
    page(name: "donationPage")
    page(name: "historyPage")
    page(name: "deviceDebugPage")
    page(name: "settingsPage")
    page(name: "confirmPage")
}

private Map ignoreLists() {
    return [
        commands: ["indicatorWhenOn", "indicatorWhenOff", "ping", "refresh", "indicatorNever", "configure", "poll", "reset"],
        attributes: ['DeviceWatch-Enroll', 'DeviceWatch-Status', "checkInterval"],
        evt_attributes: [
            'DeviceWatch-DeviceStatus', "DeviceWatch-Enroll", 'checkInterval', 'devTypeVer', 'dayPowerAvg', 'apiStatus', 'yearCost', 'yearUsage','monthUsage', 'monthEst', 'weekCost', 'todayUsage',
            'maxCodeLength', 'maxCodes', 'readingUpdated', 'maxEnergyReading', 'monthCost', 'maxPowerReading', 'minPowerReading', 'monthCost', 'weekUsage', 'minEnergyReading',
            'codeReport', 'scanCodes', 'verticalAccuracy', 'horizontalAccuracyMetric', 'altitudeMetric', 'latitude', 'distanceMetric', 'closestPlaceDistanceMetric',
            'closestPlaceDistance', 'leavingPlace', 'currentPlace', 'codeChanged', 'codeLength', 'lockCodes', 'healthStatus', 'horizontalAccuracy', 'bearing', 'speedMetric',
            'speed', 'verticalAccuracyMetric', 'altitude', 'indicatorStatus', 'todayCost', 'longitude', 'distance', 'previousPlace','closestPlace', 'places', 'minCodeLength',
            'arrivingAtPlace', 'lastUpdatedDt', 'scheduleType', 'zoneStartDate', 'zoneElapsed', 'zoneDuration', 'watering', 'eventTime', 'eventSummary', 'endOffset', 'startOffset',
            'closeTime', 'endMsgTime', 'endMsg', 'openTime', 'startMsgTime', 'startMsg', 'calName', "deleteInfo", "eventTitle", "floor", "sleeping", "powerSource", "batteryStatus"
        ],
        capabilities: ["Health Check", "Ultraviolet Index", "Indicator"]
    ]
}

def startPage() {
    if(!state?.installData) { state?.installData = [initVer: appVersion(), dt: getDtNow().toString(), updatedDt: getDtNow().toString(), shownDonation: false] }
    checkVersionData(true)
    if(showChgLogOk()) { return changeLogPage() }
    else if(showDonationOk()) { return donationPage() }
    else { return mainPage() }
}

def mainPage() {
    if (!state?.accessToken) {
        createAccessToken()
    }
    Boolean isInst = (state?.isInstalled == true)
    return dynamicPage(name: "mainPage", title: "Homebridge Device Configuration", nextPage: (isInst ? "confirmPage" : ""), install: !isInst, uninstall:true) {
        appInfoSect()
        section("Define Specific Categories:") {
            paragraph "Each category below will adjust the device attributes to make sure they are recognized as the desired device type under HomeKit", state: "complete"
            Boolean conf = (lightList || buttonList || fanList || fan3SpdList || fan4SpdList || speakerList || shadesList)
            Integer fansize = (fanList?.size() ?: 0) + (fan3SpdList?.size() ?: 0) + (fan4SpdList?.size() ?: 0)
            String desc = "Tap to configure"
            if(conf) {
                desc = ""
                desc += lightList ? "(${lightList?.size()}) Light Devices\n" : ""
                desc += buttonList ? "(${buttonList?.size()}) Button Devices\n" : ""
                desc += (fanList || fan3SpdList || fan4SpdList) ? "(${fansize}) Fan Devices\n" : ""
                desc += speakerList ? "(${speakerList?.size()}) Speaker Devices\n" : ""
                desc += shadesList ? "(${shadesList?.size()}) Shade Devices\n" : ""
                desc += "\nTap to modify..."
            }
            href "defineDevicesPage", title: "Define Device Types", required: false, image: getAppImg("devices2"), state: (conf ? "complete" : null), description: desc
        }

        section("All Other Devices:") {
            Boolean conf = (sensorList || switchList || deviceList)
            String desc = "Tap to configure"
            if(conf) {
                desc = ""
                desc += sensorList ? "(${sensorList?.size()}) Sensor Devices\n" : ""
                desc += switchList ? "(${switchList?.size()}) Switch Devices\n" : ""
                desc += deviceList ? "(${deviceList?.size()}) Other Devices\n" : ""
                desc += "\nTap to modify..."
            }
            href "deviceSelectPage", title: "Select your Devices", required: false, image: getAppImg("devices"), state: (conf ? "complete" : null), description: desc
        }

        section("Capability Filtering:") {
            Boolean conf = (removeBattery || removeButton || removeContact || removeEnergy || removeHumidity || removeIlluminance || removeLevel || removeLock || removeMotion || removePower || removePresence ||
            removeSwitch || removeTamper || removeTemp || removeValve)
            href "capFilterPage", title: "Filter out capabilities from your devices", required: false, image: getAppImg("filter"), state: (conf ? "complete" : null), description: (conf ? "Tap to modify..." : "Tap to configure")
        }

        section("Virtual Devices:") {
            Boolean conf = (modeList || routineList)
            String desc = "Create virtual mode or routines devices\n\nTap to Configure..."
            if(conf) {
                desc = ""
                desc += modeList ? "(${modeList?.size()}) Mode Devices\n" : ""
                desc += routineList ? "(${routineList?.size()}) Routine Devices\n" : ""
                desc += "\nTap to modify..."
            }
            href "virtDevicePage", title: "Configure Virtual Devices", required: false, image: getAppImg("devices"), state: (conf ? "complete" : null), description: desc
        }

        section("Smart Home Monitor (SHM):") {
            input "addSecurityDevice", "bool", title: "Allow SHM Control in HomeKit?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("alarm_home")
        }
        section("Plugin Options:") {
            paragraph "Turn off if you are having issues sending commands"
            input "sendCmdViaHubaction", "bool", title: "Send HomeKit Commands Locally?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("command2")
            input "temp_unit", "enum", title: "Temperature Unit?", required: true, defaultValue: location?.temperatureScale, options: ["F":"Fahrenheit", "C":"Celcius"], submitOnChange: true, image: getAppImg("command2")
        }
        section("Review Configuration:") {
            Integer devCnt = getDeviceCnt()
            href url: getAppEndpointUrl("config"), style: "embedded", required: false, title: "Render the config.json data for Homebridge", description: "Tap, select, copy, then click \"Done\"", state: "complete", image: getAppImg("info")
            if(devCnt > 148) {
                paragraph "Notice:\nHomebridge Allows for 149 Devices per Bridge!!!", image: getAppImg("error"), state: null, required: true
            }
            paragraph "Devices Selected: (${devCnt})", image: getAppImg("info"), state: "complete"
        }
        section("History and Device Data:") {
            href "historyPage", title: "View Command and Event History", image: getAppImg("backup")
            href "deviceDebugPage", title: "Device Data Viewer", image: getAppImg("debug")
        }
        section("App Preferences:") {
            href "settingsPage", title: "App Settings", required: false, image: getAppImg("settings")
            label title: "App Label (optional)", description: "Rename this App", defaultValue: app?.name, required: false, image: getAppImg("name_tag")
        }
        if(devMode()) {
            section("Dev Mode Options") {
                input "sendViaNgrok", "bool", title: "Communicate with Plugin via Ngrok Http?", defaultValue: false, submitOnChange: true, image: getAppImg("command2")
                if(sendViaNgrok) { input "ngrokHttpUrl", "text", title: "Enter the ngrok code from the url", required: true, submitOnChange: true }
            }
            section("Other Settings:") {
                input "restartService", "bool", title: "Restart Homebridge plugin when you press Save?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("reset2")
            }
        }
        clearTestDeviceItems()
    }
}

def defineDevicesPage() {
    return dynamicPage(name: "defineDevicesPage", title: "", install: false, uninstall: false) {
        section("Define Specific Categories:") {
            paragraph "Each category below will adjust the device attributes to make sure they are recognized as the desired device type under HomeKit", state: "complete"
            input "lightList", "capability.switch", title: "Lights: (${lightList ? lightList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("light_on")
            input "buttonList", "capability.button", title: "Buttons: (${buttonList ? buttonList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("button")
            input "speakerList", "capability.switch", title: "Speakers: (${speakerList ? speakerList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("media_player")
            input "shadesList", "capability.windowShade", title: "Window Shades: (${shadesList ? shadesList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("window_shade")
        }
        section("Fan Categories") {
            input "fanList", "capability.switch", title: "Fans: (${fanList ? fanList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("fan_on")
            input "fan3SpdList", "capability.switch", title: "Fans (3 Speeds): (${fan3SpdList ? fan3SpdList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("fan_on")
            input "fan4SpdList", "capability.switch", title: "Fans (4 Speeds): (${fan4SpdList ? fan4SpdList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("fan_on")
        }
    }
}

def deviceSelectPage() {
    return dynamicPage(name: "deviceSelectPage", title: "", install: false, uninstall: false) {
        section("All Other Devices:") {
            input "sensorList", "capability.sensor", title: "Sensors: (${sensorList ? sensorList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("sensors")
            input "switchList", "capability.switch", title: "Switches: (${switchList ? switchList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("switch")
            input "deviceList", "capability.refresh", title: "Others: (${deviceList ? deviceList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false, image: getAppImg("devices2")
        }
    }
}

def settingsPage() {
    return dynamicPage(name: "settingsPage", title: "", install: false, uninstall: false) {
        section("Logging:") {
            input "showEventLogs", "bool", title: "Show Events in Live Logs?", required: false, defaultValue: true, submitOnChange: true, image: getAppImg("debug")
            input "showDebugLogs", "bool", title: "Debug Logging?", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("debug")
        }
    }
}

def historyPage() {
    return dynamicPage(name: "historyPage", title: "", install: false, uninstall: false) {
        List cHist = getCmdHistory()?.sort {it?.dt}?.reverse()
        List eHist = getEvtHistory()?.sort {it?.dt}?.reverse()
        section("Last (${cHist?.size()}) Commands:") {
            if(cHist?.size()) {
                cHist?.each { c-> paragraph title: c?.dt, "Device: ${c?.data?.device}\nCommand: (${c?.data?.cmd})${c?.data?.value1 ? "\nValue1: (${c?.data?.value1})" : ""}${c?.data?.value2 ? "\nValue2: (${c?.data?.value2})" : ""}", state: "complete" }
            } else {paragraph "No Command History Found..." }
        }
        section("Last (${eHist?.size()}) Events:") {
            if(eHist?.size()) {
                eHist?.each { h-> paragraph title: h?.dt, "Device: ${h?.data?.device}\nEvent: (${h?.data?.name})${h?.data?.value ? "\nValue: (${h?.data?.value})" : ""}", state: "complete" }
            } else {paragraph "No Event History Found..." }
        }
    }
}

def capFilterPage() {
    return dynamicPage(name: "capFilterPage", title: "Filter out capabilities", install: false, uninstall: false) {
        section("Restrict Temp Device Creation") {
            input "noTemp", "bool", title: "Remove Temp from All Contacts and Water Sensors?", required: false, defaultValue: false, submitOnChange: true
            if(settings?.noTemp) {
                input "sensorAllowTemp", "capability.sensor", title: "Allow Temp on these Sensors", multiple: true, submitOnChange: true, required: false, image: getAppImg("temperature")
            }
        }
        section("Remove Capabilities from Devices") {
            paragraph "This will allow you to filter out certain capabilities from creating unwanted devices under HomeKit"
            input "removeBattery", "capability.battery", title: "Remove Battery from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("battery")
            input "removeButton", "capability.button", title: "Remove Buttons from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("button")
            input "removeContact", "capability.contactSensor", title: "Remove Contact from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("contact")
            input "removeEnergy", "capability.energyMeter", title: "Remove Energy Meter from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("power")
            input "removeHumidity", "capability.relativeHumidityMeasurement", title: "Remove Humidity from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("humidity")
            input "removeIlluminance", "capability.illuminanceMeasurement", title: "Remove Illuminance from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("illuminance")
            input "removeLevel", "capability.switchLevel", title: "Remove Level from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("speed_knob")
            input "removeLock", "capability.lock", title: "Remove Lock from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("lock")
            input "removeMotion", "capability.motionSensor", title: "Remove Motion from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("motion")
            input "removePower", "capability.powerMeter", title: "Remove Power Meter from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("power")
            input "removePresence", "capability.presenceSensor", title: "Remove Presence from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("presence")
            input "removeSwitch", "capability.switch", title: "Remove Switch from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("switch")
            input "removeTamper", "capability.tamperAlert", title: "Remove Tamper from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("tamper")
            input "removeTemp", "capability.temperatureMeasurement", title: "Remove Temperature from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("temperature")
            input "removeValve", "capability.valve", title: "Remove Valve from these Devices", multiple: true, submitOnChange: true, required: false, image: getAppImg("valve")
        }
    }
}

def virtDevicePage() {
    return dynamicPage(name: "virtDevicePage", title: "", install: false, uninstall: false) {
        section("Create Devices for Modes in HomeKit?") {
            paragraph title: "What are these for?", "A virtual switch will be created for each mode in HomeKit.\nThe switch will be ON when that mode is active.", state: "complete", image: getAppImg("info")
            def modes = location?.modes?.sort{it?.name}?.collect { [(it?.id):it?.name] }
            input "modeList", "enum", title: "Create Devices for these Modes", required: false, multiple: true, options: modes, submitOnChange: true, image: getAppImg("mode")
        }
        section("Create Devices for Routines in HomeKit?") {
            paragraph title: "What are these?", "A virtual device will be created for each routine in HomeKit.\nThese are very useful for use in Home Kit scenes", state: "complete", image: getAppImg("info")
            def routines = location.helloHome?.getPhrases()?.sort { it?.label }?.collect { [(it?.id):it?.label] }
            input "routineList", "enum", title: "Create Devices for these Routines", required: false, multiple: true, options: routines, submitOnChange: true, image: getAppImg("routine")
        }
    }
}

def donationPage() {
    return dynamicPage(name: "donationPage", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        section("") {
            def str = ""
            str += "Hello User, \n\nPlease forgive the interuption but it's been 30 days since you installed/updated this SmartApp and I wanted to present you with this one time reminder that donations are accepted (We do not require them)."
            str += "\n\nIf you have been enjoying the software and devices please remember that we have spent thousand's of hours of our spare time working on features and stability for those applications and devices."
            str += "\n\nIf you have already donated, thank you very much for your support!"

            str += "\n\nIf you are just not interested or have already donated please ignore this message and toggle the setting below"
            str += "\n\nThanks again for using Homebridge SmartThings"
            paragraph str, required: true, state: null
            input "sentDonation", "bool", title: "Already Donated?", defaultValue: false, submitOnChange: true
            href url: textDonateLink(), style: "external", required: false, title: "Donations", description: "Tap to open in browser", state: "complete", image: getAppImg("donate")
        }
        updInstData("shownDonation", true)
    }
}

def confirmPage() {
    return dynamicPage(name: "confirmPage", title: "Confirm Page", install: true, uninstall:true) {
        section("") {
            paragraph "Restarting the service is no longer required to apply any device changes under homekit.\n\nThe service will refresh your devices shortly after Pressing Done/Save.", state: "complete", image: getAppImg("info")
        }
    }
}

def deviceDebugPage() {
    return dynamicPage(name: "deviceDebugPage", title: "", install: false, uninstall: false) {
        section("All Other Devices:") {
            paragraph "Have a device that's not working under homekit like you want?\nSelect a device from one of the inputs below and it will show you all data about the device.", state: "complete", image: getAppImg("info")
            if(!debug_switch && !debug_other)
                input "debug_sensor", "capability.sensor", title: "Sensors: ", multiple: false, submitOnChange: true, required: false, image: getAppImg("sensors")
            if(!debug_sensor && !debug_other)
                input "debug_switch", "capability.switch", title: "Switches: ", multiple: false, submitOnChange: true, required: false, image: getAppImg("switch")
            if(!debug_switch && !debug_sensor)
                input "debug_other", "capability.refresh", title: "Others Devices: ", multiple: false, submitOnChange: true, required: false, image: getAppImg("devices2")
            if(debug_other || debug_sensor || debug_switch) {
                href url: getAppEndpointUrl("deviceDebug"), style: "embedded", required: false, title: "Tap here to view Device Data...", description: "", state: "complete", image: getAppImg("info")
            }
        }
    }
}

public clearTestDeviceItems() {
    settingRemove("debug_sensor")
    settingRemove("debug_switch")
    settingRemove("debug_other")
}

def viewDeviceDebug() {
    def sDev = null;
    if(debug_other) sDev = debug_other
    if(debug_sensor) sDev = debug_sensor
    if(debug_switch) sDev = debug_switch
    def json = new groovy.json.JsonOutput().toJson(getDeviceDebugMap(sDev))
    def jsonStr = new groovy.json.JsonOutput().prettyPrint(json)
    render contentType: "application/json", data: jsonStr
}

def getDeviceDebugMap(dev) {
    def r = "No Data Returned"
    if(dev) {
        try {
            r = [:]
            r?.name = dev?.displayName?.toString()?.replaceAll("[#\$()!%&@^']", "");
            r?.basename = dev?.getName();
            r?.deviceid = dev?.getId();
            r?.status = dev?.getStatus();
            r?.manufacturer = dev?.getManufacturerName() ?: "Unknown";
            r?.model = dev?.getModelName() ?: dev?.getTypeName();
            r?.deviceNetworkId = dev?.getDeviceNetworkId();
            r?.lastActivity = dev?.getLastActivity() ?: null;
            r?.capabilities = dev?.capabilities?.collect { it?.name as String }?.unique()?.sort() ?: [];
            r?.commands = dev?.supportedCommands?.collect { it?.name as String }?.unique()?.sort() ?: [];
            r?.customflags = getDeviceFlags(dev) ?: [];
            r?.attributes = [:];
            r?.eventHistory = dev?.eventsSince(new Date() - 1, [max: 20])?.collect { "${it?.date} | [${it?.name}] | (${it?.value}${it?.unit ? " ${it?.unit}" : ""})" };
            dev?.supportedAttributes?.collect { it?.name as String }?.unique()?.sort()?.each { r?.attributes[it] = dev?.currentValue(it as String); };
        } catch(ex) {
            log.error("Error while generating device data: ", ex);
        }
    }
    return r
}

def getDeviceCnt() {
    def devices = []
    def items = ["deviceList", "sensorList", "switchList", "lightList", "buttonList", "fanList", "fan3SpdList", "fan4SpdList", "speakerList", "shadesList", "modeList", "routineList"]
    items?.each { item ->
        if(settings[item]?.size() > 0) {
            devices = devices + settings[item]
        }
    }
    return devices?.unique()?.size() ?: 0
}

def installed() {
    log.debug "${app.name} | installed() has been called..."
    state?.isInstalled = true
    state?.installData = [initVer: appVersion(), dt: getDtNow().toString(), updatedDt: "Not Set", showDonation: false, shownChgLog: true]
    initialize()
}

def updated() {
    log.debug "${app.name} | updated() has been called..."
    state?.isInstalled = true
    if(!state?.installData) { state?.installData = [initVer: appVersion(), dt: getDtNow().toString(), updatedDt: getDtNow().toString(), shownDonation: false] }
    unsubscribe()
    stateCleanup()
    initialize()
}

def initialize() {
    if(!state?.accessToken) { createAccessToken() }
    subscribeToEvts()
    if(settings?.restartService == true) {
        log.warn "Sent Request to Homebridge Service to Stop... Service should restart automatically"
        attemptServiceRestart()
        settingUpdate("restartService", "false", "bool")
    }
    runIn(10, "updateServicePrefs")
    runIn(15, "sendDeviceRefreshCmd")
    runEvery5Minutes("healthCheck")
}

private subscribeToEvts() {
    runIn(2, "registerDevices_1")
    runIn(4, "registerDevices_2")
    runIn(6, "registerDevices_3")
    log.info "--------------------------------------"
    log.info "Starting Device Subscription Process"
    log.info "--------------------------------------"
    if(settings?.addSecurityDevice) {
        subscribe(location, "alarmSystemStatus", changeHandler)
    }
    if(settings?.modeList) {
        if(showDebugLogs) log.debug "Registering (${settings?.modeList?.size() ?: 0}) Virtual Mode Devices"
        subscribe(location, "mode", changeHandler)
        if(state.lastMode == null) { state?.lastMode = location?.mode?.toString() }
    }
    state?.subscriptionRenewed = 0
    subscribe(app, onAppTouch)
    if(settings?.sendCmdViaHubaction != false) { subscribe(location, null, lanEventHandler, [filterEvents:false]) }
    if(settings?.routineList) {
        if(showDebugLogs) log.debug "Registering (${settings?.routineList?.size() ?: 0}) Virtual Routine Devices"
        subscribe(location, "routineExecuted", changeHandler)
    }
}

private healthCheck() {
    checkVersionData()
    if(checkIfCodeUpdated()) {
        log.warn("Code Version Change Detected... Health Check will occur on next cycle.")
        return
    }
}

private checkIfCodeUpdated() {
    if(showDebugLogs) log.debug("Code versions: ${state?.codeVersions}")
    if(state?.codeVersions) {
        if(state?.codeVersions?.mainApp != appVersion()) {
            checkVersionData(true)
            state?.pollBlocked = true
            updCodeVerMap("mainApp", appVersion())
            Map iData = atomicState?.installData ?: [:]
            iData["updatedDt"] = getDtNow().toString()
            iData["shownChgLog"] = false
            if(iData?.shownDonation == null) {
                iData["shownDonation"] = false
            }
            atomicState?.installData = iData
            log.info("Code Version Change Detected... | Re-Initializing SmartApp in 5 seconds")
            return true
        }
    }
    return false
}

private stateCleanup() {
    List removeItems = []
    if(state?.directIP && state?.directPort) {
        state?.pluginDetails = [
            directIP: state?.directIP,
            directPort: state?.directPort
        ]
        removeItems?.push("directIP")
        removeItems?.push("directPort")
    }
    removeItems?.each { if(state?.containsKey(it)) state?.remove(it) }
}

def onAppTouch(event) {
    updated()
}

def renderDevices() {
    Map devMap = [:]
    List devList = []
    List items = ["deviceList", "sensorList", "switchList", "lightList", "buttonList", "fanList", "fan3SpdList", "fan4SpdList", "speakerList", "shadesList", "modeList", "routineList"]
    items?.each { item ->
        if(settings[item]?.size()) {
            settings[item]?.each { dev->
                try {
                    Map devObj = getDeviceData(item, dev) ?: [:]
                    if(devObj?.size()) { devMap[dev] = devObj }
                } catch (e) {
                    log.error("Device (${dev?.displayName}) Render Exception: ${ex.message}")
                }
            }
        }
    }
    if(settings?.addSecurityDevice == true) { devList?.push(getSecurityDevice()) }
    if(devMap?.size()) { devMap?.sort{ it?.value?.name }?.each { k,v-> devList?.push(v) } }
    return devList
}

def getDeviceData(type, sItem) {
    // log.debug "getDeviceData($type, $sItem)"
    String curType = null
    String devId = sItem
    Boolean isVirtual = false
    String firmware = null
    String name = null
    Map optFlags = [:]
    def attrVal = null
    def obj = null
    switch(type) {
        case "routineList":
            isVirtual = true
            curType = "Routine"
            optFlags["virtual_routine"] = 1
            obj = getRoutineById(sItem)
            if(obj) {
                name = "Routine - " + obj?.label
                attrVal = "off"
            }
            break
        case "modeList":
            isVirtual = true
            curType = "Mode"
            optFlags["virtual_mode"] = 1
            obj = getModeById(sItem)
            if(obj) {
                name = "Mode - " + obj?.name
                attrVal = modeSwitchState(obj?.name)
            }
            break
        default:
            curType = "device"
            obj = sItem
            // Define firmware variable and initialize it out of device handler attribute`
            try {
                if (sItem?.hasAttribute("firmware")) { firmware = sItem?.currentValue("firmware")?.toString() }
            } catch (ex) { }
            break
    }
    if(curType && obj) {
        return [
            name: !isVirtual ? sItem?.displayName?.toString()?.replaceAll("[#\$()!%&@^']", "") : name?.toString()?.replaceAll("[#\$()!%&@^']", ""),
            basename: !isVirtual ? sItem?.name : name,
            deviceid: !isVirtual ? sItem?.id : devId,
            status: !isVirtual ? sItem?.status : "Online",
            manufacturerName: (!isVirtual ? sItem?.getManufacturerName() : pluginName()) ?: pluginName(),
            modelName: !isVirtual ? (sItem?.getModelName() ?: sItem?.getTypeName()) : "${curType} Device",
            serialNumber: !isVirtual ? sItem?.getDeviceNetworkId() : "${curType}${devId}",
            firmwareVersion: firmware ?: "1.0.0",
            lastTime: !isVirtual ? (sItem?.getLastActivity() ?: null) : now(),
            capabilities: !isVirtual ? deviceCapabilityList(sItem) : ["${curType}": 1],
            commands: !isVirtual ? deviceCommandList(sItem) : [on: 1],
            deviceflags: !isVirtual ? getDeviceFlags(sItem) : optFlags,
            attributes: !isVirtual ? deviceAttributeList(sItem) : ["switch": attrVal]
        ]
    } else { return null }
}

String modeSwitchState(String mode) {
    return location?.mode?.toString() == mode ? "on" : "off"
}

def getSecurityDevice() {
    return [
        name: "Security Alarm",
        basename: "Security Alarm",
        deviceid: "alarmSystemStatus_${location?.id}",
        status: "ACTIVE",
        manufacturerName: pluginName(),
        modelName: "Security System",
        serialNumber: "SHM",
        firmwareVersion: "1.0.0",
        lastTime: null,
        capabilities: ["Alarm System Status": 1, "Alarm": 1],
        commands: [],
        attributes: ["alarmSystemStatus": getSecurityStatus()]
    ]
}

def getDeviceFlags(device) {
    Map opts = [:]
    if(settings?.fan3SpdList?.find { it?.id == device?.id }) {
        opts["fan_3_spd"] = 1
    }
    if(settings?.fan4SpdList?.find { it?.id == device?.id }) {
        opts["fan_4_spd"] = 1
    }
    // if(opts?.size()) log.debug "opts: ${opts}"
    return opts
}

def findDevice(dev_id) {
    List allDevs = []
    ["deviceList", "sensorList", "switchList", "lightList", "buttonList", "fanList", "fan3SpdList", "fan4SpdList", "speakerList", "shadesList"]?.each { key-> allDevs = allDevs + (settings?."${key}" ?: []) }
    return allDevs?.find { it?.id == dev_id } ?: null
}

def authError() {
    return [error: "Permission denied"]
}

def getSecurityStatus(retInt=false) {
    def cur = location.currentState("alarmSystemStatus")?.value
    def inc = getShmIncidents()
    if(inc != null && inc?.size()) { cur = 'alarm_active' }
    if(retInt) {
        switch (cur) {
            case 'stay':
                return 0
            case 'away':
                return 1
            case 'night':
                return 2
            case 'off':
                return 3
            case 'alarm_active':
                return 4
        }
    } else { return cur ?: "disarmed" }
}

private setSecurityMode(mode) {
    log.info "Setting the Smart Home Monitor Mode to (${mode})..."
    sendLocationEvent(name: 'alarmSystemStatus', value: mode.toString())
}

def renderConfig() {
    Map jsonMap = [
        platforms: [
            [
                platform: pluginName(),
                name: pluginName(),
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app?.getId(),
                access_token: state?.accessToken,
                temperature_unit: settings?.temp_unit ?: location?.temperatureScale,
                validateTokenId: false,
                logConfig: [
                    debug: false,
                    showChanges: true,
                    hideTimestamp: false,
                    hideNamePrefix: false,
                    file: [
                        enabled: true
                    ]
                ]
            ]
        ]
    ]
    def configJson = new groovy.json.JsonOutput().toJson(jsonMap)
    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def renderLocation() {
    return [
        latitude: location?.latitude,
        longitude: location?.longitude,
        mode: location?.mode,
        name: location?.name,
        temperature_scale: settings?.temp_unit ?: location?.temperatureScale,
        zip_code: location?.zipCode,
        hubIP: location?.hubs[0]?.localIP,
        local_commands: (settings?.sendCmdViaHubaction != false),
        app_version: appVersion()
    ]
}

def CommandReply(statusOut, messageOut) {
    def replyJson = new groovy.json.JsonOutput().toJson([status: statusOut, message: messageOut])
    render contentType: "application/json", data: replyJson
}

def lanEventHandler(evt) {
    // log.trace "lanStreamEvtHandler..."
    def msg = parseLanMessage(evt?.description as String)
    Map headerMap = msg?.headers
    // log.trace "lanEventHandler... | headers: ${headerMap}"
    try {
        Map msgData = [:]
        if (headerMap?.size()) {
            if (headerMap?.evtSource && headerMap?.evtSource == "Homebridge_${pluginName()}") {
                if (msg?.body != null) {
                    def slurper = new groovy.json.JsonSlurper()
                    msgData = slurper?.parseText(msg?.body as String)
                    // log.debug "msgData: $msgData"
                    if(headerMap?.evtType) {
                        switch(headerMap?.evtType) {
                            case "hkCommand":
                                // log.trace "hkCommand($msgData)"
                                def val1 = msgData?.values?.value1 ?: null
                                def val2 = msgData?.values?.value2 ?: null
                                processCmd(msgData?.deviceid, msgData?.command, val1, val2, true)
                                break
                            case "enableDirect":
                                // log.trace "enableDirect($msgData)"
                                state?.pluginDetails = [
                                    directIP: msgData?.ip,
                                    directPort: msgData?.port,
                                ]
                                updCodeVerMap("plugin", msgData?.version ?: null)
                                activateDirectUpdates(true)
                                break
                        }
                    }
                }
            }
        }
    } catch (ex) {
        log.error "lanEventHandler Exception:", ex
    }
}

def deviceCommand() {
    // log.info("Command Request: $params")
    def val1 = request?.JSON?.value1 ?: null
    def val2 = request?.JSON?.value2 ?: null
    processCmd(params?.id, params?.command, val1, val2)
}

private processCmd(devId, cmd, value1, value2, local=false) {
    log.info("Process Command${local ? "(LOCAL)" : ""} | DeviceId: $devId | Command: ($cmd)${value1 ? " | Param1: ($value1)" : ""}${value2 ? " | Param2: ($value2)" : ""}")
    def device = findDevice(devId)
    def command = cmd
    if(settings?.addSecurityDevice != false && devId == "alarmSystemStatus_${location?.id}") {
        setSecurityMode(command)
        CommandReply("Success", "Security Alarm, Command $command")
    }  else if (settings?.modeList && command == "mode" && devId) {
        log.debug("Virtual Mode Received: ${devId}")
        changeMode(devId)
        CommandReply("Success", "Mode Device, Command $command")
    } else if (settings?.routineList && command == "routine" && devId) {
        log.debug("Virtual Routine Received: ${devId}")
        runRoutine(devId)
        CommandReply("Success", "Routine Device, Command $command")
    } else {
        if (!device) {
            log.error("Device Not Found")
            CommandReply("Failure", "Device Not Found")
        } else if (!device?.hasCommand(command as String)) {
            log.error("Device ${device.displayName} does not have the command $command")
            CommandReply("Failure", "Device ${device.displayName} does not have the command $command")
        } else {
            try {
                if (value2 != null) {
                    device?."$command"(value1,value2)
                    log.info("Command Successful for Device ${device.displayName} | Command ${command}($value1, $value2)")
                } else if (value1 != null) {
                    device?."$command"(value1)
                    log.info("Command Successful for Device ${device.displayName} | Command ${command}($value1)")
                } else {
                    device?."$command"()
                    log.info("Command Successful for Device ${device.displayName} | Command ${command}()")
                }
                CommandReply("Success", "Device ${device.displayName} | Command ${command}()")
                logCmd([cmd: command, device: device?.displayName, value1: value1, value2: value2])
            } catch (e) {
                log.error("Error Occurred for Device ${device.displayName} | Command ${command}()")
                CommandReply("Failure", "Error Occurred For Device ${device.displayName} | Command ${command}()")
            }
        }
    }

}

def changeMode(modeId) {
    if(modeId) {
        def mode = findVirtModeDevice(modeId)
        if(mode) {
            log.info"Setting the Location Mode to (${mode})..."
            setLocationMode(mode)
            state?.lastMode = mode
        } else { log.error("Unable to find a matching mode for the id: ${modeId}") }
    }
}

def runRoutine(rtId) {
    if(rtId) {
        def rt = findVirtRoutineDevice(rtId)
        if(rt?.label) {
            log.info "Executing the (${rt?.label}) Routine..."
            location?.helloHome?.execute(rt?.label)
        } else { log.error("Unable to find a matching routine for the id: ${rtId}") }
    }
}

def deviceAttribute() {
    def device = findDevice(params?.id)
    def attribute = params?.attribute
    if (!device) {
        httpError(404, "Device not found")
    } else {
        return [currentValue: device?.currentValue(attribute)]
    }
}

def findVirtModeDevice(id) {
    return getModeById(id) ?: null
}

def findVirtRoutineDevice(id) {
    return getRoutineById(id) ?: null
}

def deviceQuery() {
    log.trace "deviceQuery(${params?.id}"
    def device = findDevice(params?.id)
    if (!device) {
        def mode = findVirtModeDevice(params?.id)
        def routine = findVirtModeDevice(params?.id)
        def obj = mode ? mode : routine ?: null
        if(!obj) {
            device = null
            httpError(404, "Device not found")
        } else {
            def name = routine ? obj?.label : obj?.name
            def type = routine ? "Routine" : "Mode"
            def attrVal = routine ? "off" : modeSwitchState(obj?.name)
            try {
                deviceData?.push([
                    name: name,
                    deviceid: params?.id,
                    capabilities: ["${type}": 1],
                    commands: [on:1],
                    attributes: ["switch": attrVal]
                ])
            } catch (e) {
                log.error("Error Occurred Parsing ${item} ${type} ${name}, Error " + e.message)
            }
        }
    }

    if (result) {
        def jsonData = [
            name: device.displayName,
            deviceid: device.id,
            capabilities: deviceCapabilityList(device),
            commands: deviceCommandList(device),
            attributes: deviceAttributeList(device)
        ]
        def resultJson = new groovy.json.JsonOutput().toJson(jsonData)
        render contentType: "application/json", data: resultJson
    }
}

def deviceCapabilityList(device) {
    def items = device?.capabilities?.findAll{ !(it?.name in ignoreLists()?.capabilities) }?.collectEntries { capability-> [ (capability?.name):1 ] }
    // ["Health Check", "Ultraviolet Index", "Indicator"]?.each { if(it in items) { items?.remove(it as String) } }
    if(settings?.lightList?.find { it?.id == device?.id }) {
        items["LightBulb"] = 1
    }
    if(settings?.buttonList?.find { it?.id == device?.id }) {
        items["Button"] = 1
    }
    if(settings?.fanList?.find { it?.id == device?.id }) {
        items["Fan"] = 1
    }
    if(settings?.speakerList?.find { it?.id == device?.id }) {
        items["Speaker"] = 1
    }
    if(settings?.shadesList?.find { it?.id == device?.id }) {
        items["WindowShade"] = 1
    }
    if(settings?.noTemp && items["Temperature Measurement"] && (items["Contact Sensor"] || items["Water Sensor"])) {
        Boolean remTemp = true
        if(settings?.sensorAllowTemp) {
            List aItems = settings?.sensorAllowTemp?.collect { it?.getId() as String } ?: []
            if(aItems?.contains(device?.id as String)) { remTemp = false }
        }
        if(remTemp) { items?.remove("Temperature Measurement") }
    }
    if(settings?.removeBattery && items["Battery"] && isDeviceInInput('removeBattery', device?.id)) { items?.remove("Battery"); if(showDebugLogs) { log.debug "Filtering Battery"; } }
    if(settings?.removeButton && items["Button"] && isDeviceInInput('removeButton', device?.id)) { items?.remove("Button");  if(showDebugLogs) { log.debug "Filtering Button"; } }
    if(settings?.removeContact && items["Contact Sensor"] && isDeviceInInput('removeContact', device?.id)) { items?.remove("Contact Sensor");  if(showDebugLogs) { log.debug "Filtering Contact"; } }
    if(settings?.removeEnergy && items["Energy Meter"] && isDeviceInInput('removeEnergy', device?.id)) { items?.remove("Energy Meter");  if(showDebugLogs) { log.debug "Filtering Energy"; } }
    if(settings?.removeHumidity && items["Relative Humidity Measurement"] && isDeviceInInput('removeHumidity', device?.id)) { items?.remove("Relative Humidity Measurement");  if(showDebugLogs) { log.debug "Filtering Humidity"; } }
    if(settings?.removeIlluminance && items["Illuminance Measurement"] && isDeviceInInput('removeIlluminance', device?.id)) { items?.remove("Illuminance Measurement");  if(showDebugLogs) { log.debug "Filtering Illuminance"; } }
    if(settings?.removeLevel && items["Switch Level"] && isDeviceInInput('removeLevel', device?.id)) { items?.remove("Switch Level");  if(showDebugLogs) { log.debug "Filtering Level"; } }
    if(settings?.removeLock && items["Lock"] && isDeviceInInput('removeLock', device?.id)) { items?.remove("Lock");  if(showDebugLogs) { log.debug "Filtering Lock"; } }
    if(settings?.removeMotion && items["Motion Sensor"] && isDeviceInInput('removeMotion', device?.id)) { items?.remove("Motion Sensor");  if(showDebugLogs) { log.debug "Filtering Motion"; } }
    if(settings?.removePower && items["Power Meter"] && isDeviceInInput('removePower', device?.id)) { items?.remove("Power Meter");  if(showDebugLogs) { log.debug "Filtering Power Meter"; } }
    if(settings?.removePresence && items["Presence Sensor"] && isDeviceInInput('removePresence', device?.id)) { items?.remove("Presence Sensor");  if(showDebugLogs) { log.debug "Filtering Presence"; } }
    if(settings?.removeSwitch && items["Switch"] && isDeviceInInput('removeSwitch', device?.id)) { items?.remove("Switch");  if(showDebugLogs) { log.debug "Filtering Switch"; } }
    if(settings?.removeTamper && items["Tamper Alert"] && isDeviceInInput('removeTamper', device?.id)) { items?.remove("Tamper Alert");  if(showDebugLogs) { log.debug "Filtering Tamper"; } }
    if(settings?.removeTemp && items["Temperature Measurement"] && isDeviceInInput('removeTemp', device?.id)) { items?.remove("Temperature Measurement");  if(showDebugLogs) { log.debug "Filtering Temp"; } }
    if(settings?.removeValve && items["Valve"] && isDeviceInInput('removeValve', device?.id)) { items?.remove("Valve");  if(showDebugLogs) { log.debug "Filtering Valve"; } }
    return items
}

def deviceCommandList(device) {
    return device?.supportedCommands?.findAll { !(it?.name in ignoreLists()?.commands) }?.collectEntries { command-> [ (command?.name): 1 ] }
}

def deviceAttributeList(device) {
    return device?.supportedAttributes?.findAll { !(it?.name in ignoreLists()?.attributes) }?.collectEntries { attribute->
        try {
            [(attribute?.name): device?.currentValue(attribute?.name)]
        } catch(e) {
            [(attribute?.name): null]
        }
    }
}

String getAppEndpointUrl(subPath) { return "${apiServerUrl("/api/smartapps/installations/${app.id}${subPath ? "/${subPath}" : ""}?access_token=${state?.accessToken}")}" }

def getAllData() {
    state?.subscriptionRenewed = now()
    state?.devchanges = []
    def deviceJson = new groovy.json.JsonOutput().toJson([location: renderLocation(), deviceList: renderDevices()])
    updTsVal("lastDeviceDataQueryDt")
    render contentType: "application/json", data: deviceJson
}

def registerDevices_1() {
    //This has to be done at startup because it takes too long for a normal command.
    ["fanList": "Fan Devices", "fan3SpdList": "Fans (3Spd) Devices", "fan4SpdList": "Fans (4Spd) Devices", "buttonList": "Button Devices", "deviceList": "Other Devices"]?.each { k,v->
        if(showDebugLogs) log.debug "Registering (${settings?."${k}"?.size() ?: 0}) ${v}"
        registerChangeHandler(settings?."${k}")
    }
}

def registerDevices_2() {
    //This has to be done at startup because it takes too long for a normal command.
    ["sensorList": "Sensor Devices", "speakerList": "Speaker Devices"]?.each { k,v->
        if(showDebugLogs) log.debug "Registering (${settings?."${k}"?.size() ?: 0}) ${v}"
        registerChangeHandler(settings?."${k}")
    }
}

def registerDevices_3() {
    //This has to be done at startup because it takes too long for a normal command.
    ["switchList": "Switch Devices", "lightList": "Light Devices", "shadesList": "Window Shade Devices"]?.each { k,v->
        if(showDebugLogs) log.debug "Registering (${settings?."${k}"?.size() ?: 0}) ${v}"
        registerChangeHandler(settings?."${k}")
    }
    log.info "--------------------------------------"
    log.info "Registered (${getDeviceCnt()} Devices)"
    log.info "--------------------------------------"
}

Boolean isDeviceInInput(setKey, devId) {
    if(settings[setKey]) {
        List aItems = settings[setKey] ? settings[setKey]?.collect { it?.getId() as String } : []
        if(aItems?.contains(devId as String)) { return true }
    }
    return false
}

def registerChangeHandler(devices, showlog=false) {
    devices?.each { device ->
        List theAtts = device?.supportedAttributes?.collect { it?.name as String }?.unique()
        if(showlog) { log.debug "atts: ${theAtts}" }
        theAtts?.each {att ->
            if(!(ignoreLists().evt_attributes?.contains(att))) {
                if(settings?.noTemp && att == "temperature" && (device?.hasAttribute("contact") || device?.hasAttribute("water"))) {
                    Boolean skipAtt = true
                    if(settings?.sensorAllowTemp) {
                        skipAtt = isDeviceInInput('sensorAllowTemp', device?.id)
                    }
                    if(skipAtt) { return }
                }
                if(att == "battery" &&      isDeviceInInput('removeBattery', device?.id)) {return}
                if(att == "button" &&       isDeviceInInput('removeButton', device?.id)) {return}
                if(att == "switch" &&       isDeviceInInput('removeSwitch', device?.id)) {return}
                if(att == "temperature" &&  isDeviceInInput('removeTemp', device?.id)) {return}
                if(att == "contact" &&      isDeviceInInput('removeContact', device?.id)) {return}
                if(att == "energy" &&       isDeviceInInput('removeEnergy', device?.id)) {return}
                if(att == "humidity" &&     isDeviceInInput('removeHumidity', device?.id)) {return}
                if(att == "illuminance" &&  isDeviceInInput('removeIlluminance', device?.id)) {return}
                if(att == "level" &&        isDeviceInInput('removeLevel', device?.id)) { return }
                if(att == "lock" &&         isDeviceInInput('removeLock', device?.id)) { return }
                if(att == "motion" &&       isDeviceInInput('removeMotion', device?.id)) { return }
                if(att == "power" &&        isDeviceInInput('removePower', device?.id)) { return }
                if(att == "presence" &&     isDeviceInInput('removePresence', device?.id)) { return }
                if(att == "tamper" &&       isDeviceInInput('removeTamper', device?.id)) { return }
                if(att == "valve" &&        isDeviceInInput('removeValve', device?.id)) { return }

                subscribe(device, att, "changeHandler")
                if(showlog) { log.debug "Registering ${device?.displayName}.${att}" }
            }
        }
    }
}

def changeHandler(evt) {
    def sendItems = []
    def sendNum = 1
    def src = evt?.source
    def deviceid = evt?.deviceId
    def deviceName = evt?.displayName
    def attr = evt?.name
    def value = evt?.value
    def dt = evt?.date
    def sendEvt = true

    switch(evt?.name) {
        case "hsmStatus":
            deviceid = "alarmSystemStatus_${location?.id}"
            attr = "alarmSystemStatus"
            sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            break
        case "hsmAlert":
            if(evt?.value == "intrusion") {
                deviceid = "alarmSystemStatus_${location?.id}"
                attr = "alarmSystemStatus"
                value = "alarm_active"
                sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            } else { sendEvt = false }
            break
        case "hsmRules":
        case "hsmSetArm":
            sendEvt = false
            break
        case "alarmSystemStatus":
            deviceid = "alarmSystemStatus_${location?.id}"
            sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            break
        case "mode":
            settings?.modeList?.each { id->
                def md = getModeById(id)
                if(md && md?.id) { sendItems?.push([evtSource: "MODE", evtDeviceName: "Mode - ${md?.name}", evtDeviceId: md?.id, evtAttr: "switch", evtValue: modeSwitchState(md?.name), evtUnit: "", evtDate: dt]) }
            }
            break
        case "routineExecuted":
            settings?.routineList?.each { id->
                def rt = getRoutineById(id)
                if(rt && rt?.id) {
                    sendItems?.push([evtSource: "ROUTINE", evtDeviceName: "Routine - ${rt?.label}", evtDeviceId: rt?.id, evtAttr: "switch", evtValue: "off", evtUnit: "", evtDate: dt])
                }
            }
            break
        default:
            sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            break
    }

    if (sendEvt && state?.pluginDetails?.directIP != "" && sendItems?.size()) {
        //Send Using the Direct Mechanism
        sendItems?.each { send->
            if(settings?.showEventLogs) {
                String unitStr = ""
                switch(send?.evtAttr as String) {
                    case "temperature":
                        unitStr = "\u00b0${send?.evtUnit}"
                        break
                    case "humidity":
                    case "level":
                    case "battery":
                        unitStr = "%"
                        break
                    case "power":
                        unitStr = "W"
                        break
                    case "illuminance":
                        unitStr = " Lux"
                        break
                    default:
                        unitStr = "${send?.evtUnit}"
                        break
                }
                log.debug "Sending${" ${send?.evtSource}" ?: ""} Event (${send?.evtDeviceName} | ${send?.evtAttr.toUpperCase()}: ${send?.evtValue}${unitStr}) to Homebridge at (${state?.pluginDetails?.directIP}:${state?.pluginDetails?.directPort})"
            }
            sendHttpPost("update", [
                change_name: send?.evtDeviceName,
                change_device: send?.evtDeviceId,
                change_attribute: send?.evtAttr,
                change_value: send?.evtValue,
                change_date: send?.evtDate,
                app_id: app?.getId(),
                access_token: state?.accessToken
            ])
            logEvt([name: send?.evtAttr, value: send?.evtValue, device: send?.evtDeviceName])
        }
    }
}

private sendHttpGet(path, contentType) {
    if(settings?.sendViaNgrok && settings?.ngrokHttpUrl) {
        httpGet([
            uri: "https://${settings?.ngrokHttpUrl}.ngrok.io",
            path: "/${path}",
            contentType: contentType
        ])
    } else { sendHubCommand(new physicalgraph.device.HubAction(method: "GET", path: "/${path}", headers: [HOST: getServerAddress()])) }
}

private sendHttpPost(path, body, contentType = "application/json") {
    if(settings?.sendViaNgrok && settings?.ngrokHttpUrl) {
        Map params = [
            uri: "https://${settings?.ngrokHttpUrl}.ngrok.io",
            path: "/${path}",
            contentType: contentType,
            body: body
        ]
        httpPost(params)
    } else {
        Map params = [
            method: "POST",
            path: "/${path}",
            headers: [
                HOST: getServerAddress(),
                'Content-Type': contentType
            ],
            body: body
        ]
        def result = new physicalgraph.device.HubAction(params)
        sendHubCommand(result)
    }
}

private getServerAddress() { return "${state?.pluginDetails?.directIP}:${state?.pluginDetails?.directPort}" }

def getModeById(String mId) {
    return location?.modes?.find{it?.id?.toString() == mId}
}

def getRoutineById(String rId) {
    return location?.helloHome?.getPhrases()?.find{it?.id == rId}
}

def getModeByName(String name) {
    return location?.modes?.find{it?.name?.toString() == name}
}

def getRoutineByName(String name) {
    return location?.helloHome?.getPhrases()?.find{it?.label == name}
}

def getShmIncidents() {
    //Thanks Adrian
    def incidentThreshold = now() - 604800000
    return location.activeIncidents.collect{[date: it?.date?.time, title: it?.getTitle(), message: it?.getMessage(), args: it?.getMessageArgs(), sourceType: it?.getSourceType()]}.findAll{ it?.date >= incidentThreshold } ?: null
}

void settingUpdate(name, value, type=null) {
    if(name && type) {
        app?.updateSetting("$name", [type: "$type", value: value])
    }
    else if (name && type == null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(String name) {
    if(name && settings?.containsKey(name as String)) { app?.deleteSetting(name as String) }
}

Boolean devMode() {
    return (appSettings?.devMode?.toString() == "true")
}

private activateDirectUpdates(isLocal=false) {
    log.trace "activateDirectUpdates: ${getServerAddress()}${isLocal ? " | (Local)" : ""}"
    sendHttpPost("initial", [
        app_id: app?.getId(),
        access_token: state?.accessToken
    ])
}

private attemptServiceRestart(isLocal=false) {
    log.trace "attemptServiceRestart: ${getServerAddress()}${isLocal ? " | (Local)" : ""}"
    sendHttpPost("restart", [
        app_id: app?.getId(),
        access_token: state?.accessToken
    ])
}

private sendDeviceRefreshCmd(isLocal=false) {
    log.trace "sendDeviceRefreshCmd: ${getServerAddress()}${isLocal ? " | (Local)" : ""}"
    sendHttpPost("refreshDevices", [
        app_id: app?.getId(),
        access_token: state?.accessToken
    ])
}

private updateServicePrefs(isLocal=false) {
    log.trace "updateServicePrefs: ${getServerAddress()}${isLocal ? " | (Local)" : ""}"
    sendHttpPost("updateprefs", [
        app_id: app?.getId(),
        access_token: state?.accessToken,
        local_commands: (settings?.sendCmdViaHubaction != false),
        local_hub_ip: location?.hubs[0]?.localIP
    ])
}

def pluginStatus() {
    def body = request.JSON;
    state?.pluginUpdates = [hasUpdate: (body?.hasUpdate == true), newVersion: (body?.newVersion ?: null)]
    if(body?.version) { updCodeVerMap("plugin", body?.version)}
    def resultJson = new groovy.json.JsonOutput().toJson({ status: 'OK'})
    render contentType: "application/json", data: resultJson
}

def enableDirectUpdates() {
    // log.trace "enableDirectUpdates: ($params)"
    state?.pluginDetails = [
        directIP: params?.ip,
        directPort: params?.port
    ]
    updCodeVerMap("plugin", params?.version ?: null)
    activateDirectUpdates()
    updTsVal("lastDirectUpdsEnabled")
    def resultJson = new groovy.json.JsonOutput().toJson({ status: 'OK'})
    render contentType: "application/json", data: resultJson
}

mappings {
    if (!params?.access_token || (params?.access_token && params?.access_token != state?.accessToken)) {
        path("/devices")					{ action: [GET: "authError"] }
        path("/config")						{ action: [GET: "authError"] }
        path("/location")					{ action: [GET: "authError"] }
        path("/pluginStatus")			    { action: [POST: "authError"] }
        path("/:id/command/:command")		{ action: [POST: "authError"] }
        path("/:id/query")					{ action: [GET: "authError"] }
        path("/:id/attribute/:attribute") 	{ action: [GET: "authError"] }
        path("/startDirect/:ip/:port/:version")		{ action: [GET: "authError"] }
    } else {
        path("/devices")					{ action: [GET: "getAllData"] }
        path("/config")						{ action: [GET: "renderConfig"]  }
        path("/deviceDebug")			    { action: [GET: "viewDeviceDebug"]  }
        path("/location")					{ action: [GET: "renderLocation"] }
        path("/pluginStatus")			    { action: [POST: "pluginStatus"] }
        path("/:id/command/:command")		{ action: [POST: "deviceCommand"] }
        path("/:id/query")					{ action: [GET: "deviceQuery"] }
        path("/:id/attribute/:attribute")	{ action: [GET: "deviceAttribute"] }
        path("/startDirect/:ip/:port/:version")		{ action: [POST: "enableDirectUpdates"] }
    }
}

def appInfoSect() {
    Map codeVer = state?.codeVersions ?: null
    Boolean isNote = false
    section() {
        String str = "Version: v${appVersion()}"
        str += state?.pluginDetails?.version ? "\nPlugin: v${state?.pluginDetails?.version}" : ""
        str += (state?.pluginDetails?.version && state?.pluginUpdates) ? " (${(state?.pluginUpdates?.hasUpdate == true) ? "Update Available: v${state?.pluginUpdates?.newVersion}" : "Up-to-Date"})" : ""
        href "changeLogPage", title: "${app?.name}", description: str, image: appIconUrl()
        Map minUpdMap = getMinVerUpdsRequired()
        List codeUpdItems = codeUpdateItems(true)
        if(minUpdMap?.updRequired && minUpdMap?.updItems?.size()) {
            isNote=true
            String str3 = "Updates Required:"
            minUpdMap?.updItems?.each { item-> str3 += bulletItem(str3, item)  }
            paragraph str3, required: true, state: null
            paragraph "If you just updated the code please press Done/Save to let the app process the changes.", required: true, state: null
        } else if(codeUpdItems?.size()) {
            isNote=true
            String str2 = "Code Updates Available:"
            codeUpdItems?.each { item-> str2 += bulletItem(str2, item) }
            paragraph str2, required: true, state: null
        }
        if(!isNote) { paragraph "No Issues to Report" }
    }
}

/**********************************************
        APP HELPER FUNCTIONS
***********************************************/
String bulletItem(String inStr, String strVal) { return "${inStr == "" ? "" : "\n"} \u2022 ${strVal}" }
String dashItem(String inStr, String strVal, newLine=false) { return "${(inStr == "" && !newLine) ? "" : "\n"} - ${strVal}" }
String textDonateLink() { return "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=RVFJTG8H86SK8&source=url" }
Integer versionStr2Int(str) { return str ? str.toString()?.replaceAll("\\.", "")?.toInteger() : null }
Boolean codeUpdIsAvail(String newVer, String curVer, String type) {
    Boolean result = false
    def latestVer
    if(newVer && curVer) {
        List versions = [newVer, curVer]
        if(newVer != curVer) {
            latestVer = versions?.max { a, b ->
                List verA = a?.tokenize('.'); List verB = b?.tokenize('.'); Integer commonIndices = Math.min(verA?.size(), verB?.size());
                for (int i = 0; i < commonIndices; ++i) { if(verA[i]?.toInteger() != verB[i]?.toInteger()) { return verA[i]?.toInteger() <=> verB[i]?.toInteger() }; }
                verA?.size() <=> verB?.size()
            }
            result = (latestVer == newVer) ? true : false
        }
    }
    return result
}
Boolean appUpdAvail() { return (state?.appData?.versions && state?.codeVersions?.mainApp && codeUpdIsAvail(state?.appData?.versions?.mainApp, appVersion(), "main_app")) }
Boolean pluginUpdAvail() { return (state?.appData?.versions && state?.codeVersions?.plugin && codeUpdIsAvail(state?.appData?.versions?.plugin, state?.codeVersions?.plugin, "plugin")) }
private Map getMinVerUpdsRequired() {
    Boolean updRequired = false
    List updItems = []
    Map codeItems = [plugin: "Homebridge Plugin"]
    Map codeVers = state?.codeVersions ?: [:]
    codeVers?.each { k,v->
        if(codeItems?.containsKey(k as String) && v != null && (versionStr2Int(v) < minVersions()[k as String])) { updRequired = true; updItems?.push(codeItems[k]); }
    }
    return [updRequired: updRequired, updItems: updItems]
}

private List codeUpdateItems(shrt=false) {
    Boolean appUpd = appUpdAvail()
    Boolean plugUpd = pluginUpdAvail()
    List updItems = []
    if(appUpd || servUpd) {
        if(appUpd) updItems.push("${!shrt ? "\nHomebridge " : ""}App: (v${state?.appData?.versions?.mainApp?.toString()})")
        if(plugUpd) updItems.push("${!shrt ? "\n" : ""}Plugin: (v${state?.appData?.versions?.server?.toString()})")
    }
    return updItems
}

Integer getLastTsValSecs(val, nullVal=1000000) {
    def tsMap = atomicState?.tsDtMap
    return (val && tsMap && tsMap[val]) ? GetTimeDiffSeconds(tsMap[val]).toInteger() : nullVal
}

private updTsVal(key, dt=null) {
    def data = atomicState?.tsDtMap ?: [:]
    if(key) { data[key] = dt ?: getDtNow() }
    atomicState?.tsDtMap = data
}

private remTsVal(key) {
    def data = atomicState?.tsDtMap ?: [:]
    if(key) {
        if(key instanceof List) {
            key?.each { k-> if(data?.containsKey(k)) { data?.remove(k) } }
        } else { if(data?.containsKey(key)) { data?.remove(key) } }
        atomicState?.tsDtMap = data
    }
}

private getTsVal(val) {
    def tsMap = atomicState?.tsDtMap
    if(val && tsMap && tsMap[val]) { return tsMap[val] }
    return null
}

private updCodeVerMap(key, val) {
    Map cv = atomicState?.codeVersions ?: [:]
    if(val && (!cv.containsKey(key) || (cv?.containsKey(key) && cv[key] != val))) { cv[key as String] = val }
    if (cv?.containsKey(key) && val == null) { cv?.remove(key) }
    atomicState?.codeVersions = cv
}

private cleanUpdVerMap() {
    Map cv = atomicState?.codeVersions ?: [:]
    cv?.each { k, v-> if(v == null) ri?.push(k) }
    ri?.each { cv?.remove(it) }
    atomicState?.codeVersions = cv
}

private updInstData(key, val) {
    Map iData = atomicState?.installData ?: [:]
    iData[key] = val
    atomicState?.installData = iData
}

private getInstData(key) {
    def iMap = atomicState?.installData
    if(val && iMap && iMap[val]) { return iMap[val] }
    return null
}

private checkVersionData(now = false) { //This reads a JSON file from GitHub with version numbers
    def lastUpd = getLastTsValSecs("lastAppDataUpdDt")
    if (now || !state?.appData || (lastUpd > (3600*6))) {
        if(now && (lastUpd < 300)) { return }
        getConfigData()
    }
}

private getConfigData() {
    Map params = [
        uri: "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/master/appData.json",
        contentType: "application/json"
    ]
    def data = getWebData(params, "appData", false)
    if(data) {
        state?.appData = data
        updTsVal("lastAppDataUpdDt")
        if(showDebugLogs) log.debug("Successfully Retrieved (v${data?.appDataVer}) of AppData Content from GitHub Repo...")
    }
}

private getWebData(params, desc, text=true) {
    try {
        httpGet(params) { resp ->
            if(resp?.data) {
                if(text) { return resp?.data?.text.toString() }
                return resp?.data
            }
        }
    } catch (ex) {
        incrementCntByKey("appErrorCnt")
        if(ex instanceof groovyx.net.http.HttpResponseException) {
            log.warn("${desc} file not found")
        } else { log.error("getWebData(params: $params, desc: $desc, text: $text) Exception: ${ex}") }
        return "${label} info not found"
    }
}

/******************************************
|       DATE | TIME HELPERS
******************************************/
def formatDt(dt, tzChg=true) {
    def tf = new java.text.SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(tzChg) { if(location.timeZone) { tf.setTimeZone(location?.timeZone) } }
    return tf?.format(dt)
}

def getDtNow() {
    def now = new Date()
    return formatDt(now)
}

def GetTimeDiffSeconds(lastDate, sender=null) {
    try {
        if(lastDate?.contains("dtNow")) { return 10000 }
        def now = new Date()
        def lastDt = Date.parse("E MMM dd HH:mm:ss z yyyy", lastDate)
        def start = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(lastDt)).getTime()
        def stop = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(now)).getTime()
        def diff = (int) (long) (stop - start) / 1000
        return diff?.abs()
    } catch (ex) {
        log.error("GetTimeDiffSeconds Exception: (${sender ? "$sender | " : ""}lastDate: $lastDate): ${ex}")
        return 10000
    }
}

/******************************************
|       Changelog Logic
******************************************/
Boolean showDonationOk() { return (state?.isInstalled && !atomicState?.installData?.shownDonation && getDaysSinceUpdated() >= 30 && !settings?.sentDonation) ? true : false }
Integer getDaysSinceUpdated() {
    def updDt = atomicState?.installData?.updatedDt ?: null
    if(updDt == null || updDt == "Not Set") {
        updInstData("updatedDt", getDtNow().toString())
        return 0
    } else {
        def start = Date.parse("E MMM dd HH:mm:ss z yyyy", updDt)
        def stop = new Date()
        if(start && stop) {	return (stop - start) }
        return 0
    }
}

String changeLogData() { return getWebData([uri: "https://raw.githubusercontent.com/tonesto7/homebridge-smartthings-v2/master/CHANGELOG-app.md", contentType: "text/plain; charset=UTF-8"], "changelog") }
Boolean showChgLogOk() { return (state?.isInstalled && (state?.curAppVer != appVersion() || state?.installData?.shownChgLog != true)) }
def changeLogPage() {
    def execTime = now()
    return dynamicPage(name: "changeLogPage", title: "", nextPage: "mainPage", install: false) {
        section() {
            paragraph title: "Release Notes", "", state: "complete", image: getAppImg("whats_new")
            paragraph changeLogData()
        }
        state?.curAppVer = appVersion()
        updInstData("shownChgLog", true)
    }
}

Integer stateSize() { def j = new groovy.json.JsonOutput().toJson(state); return j?.toString().length(); }
Integer stateSizePerc() { return (int) ((stateSize() / 100000)*100).toDouble().round(0); }
private addToHistory(String logKey, data, Integer max=10) {
    Boolean ssOk = (stateSizePerc() > 70)
    List eData = atomicState[logKey as String] ?: []
    if(eData?.find { it?.data == data }) { return; }
    eData?.push([dt: getDtNow(), data: data])
    if(!ssOk || eData?.size() > max) { eData = eData?.drop( (eData?.size()-max) ) }
    atomicState[logKey as String] = eData
}

List getCmdHistory() { return atomicState?.cmdHistory ?: [] }
List getEvtHistory() { return atomicState?.evtHistory ?: [] }
void clearHistory() {
    atomicState?.cmdHistory = []
    atomicState?.evtHistory = []
}

private logEvt(evtData) { addToHistory("evtHistory", evtData, 15) }
private logCmd(cmdData) { addToHistory("cmdHistory", cmdData, 15) }
