/**
 *  Homebridge API
 *  Modelled off of Paul Lovelace's JSON API
 *  Copyright 2018 Anthony Santilli
 *
 */
 
import groovy.transform.Field
@Field String appVersion = "1.1.3"
@Field String appIconUrl = "https://raw.githubusercontent.com/pdlove/homebridge-smartthings/master/smartapps/JSON%401.png"

definition(
    name: "Homebridge (SmartThings)",
    namespace: "tonesto7",
    author: "Anthony Santilli",
    description: "Provides API interface between Homebridge (HomeKit) Service and SmartThings",
    category: "My Apps",
    iconUrl:   appIconUrl,
    iconX2Url: appIconUrl,
    iconX3Url: appIconUrl,
    oauth: true)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    if (!state?.accessToken) {
        createAccessToken()
    }
    dynamicPage(name: "mainPage", title: "Homebridge Device Configuration", install: true, uninstall:true) {
        section() {
            paragraph "${app?.name}\nv${appVersion}", image: appIconUrl
        }
        section() {
            paragraph title: "NOTICE", "Any Device Changes will require a restart of the Homebridge Service", required: true, state: null
        }
        section("Define Specific Categories:") {
            paragraph "These Categories will add the necessary capabilities to make sure they are recognized by HomeKit as the specific device type", state: "complete"
            input "lightList", "capability.switch", title: "Lights: (${lightList ? lightList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
            input "fanList", "capability.switch", title: "Fans: (${fanList ? fanList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
            input "speakerList", "capability.switch", title: "Speakers: (${speakerList ? speakerList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
        }
        section("Irrigation Devices:") {
            paragraph "Notice: \nOnly Tested with Rachio Devices"
			input "irrigationList", "capability.valve", title: "Irrigation Devices (${irrigationList ? irrigationList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
		}
        section("Fan/Light Combo Devices:") {
            paragraph "This will create two devices in homekit one light and one fan"
			input "hamptonBayFanLightList", "capability.switch", title: "Hampton Bay Fan/Light Devices (${hamptonBayFanLightList ? hamptonBayFanLightList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
		}
        section("All Other Devices:") {
            input "sensorList", "capability.sensor", title: "Sensor Devices: (${sensorList ? sensorList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
            input "switchList", "capability.switch", title: "Switch Devices: (${switchList ? switchList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
            input "deviceList", "capability.refresh", title: "Other Devices: (${deviceList ? deviceList?.size() : 0} Selected)", multiple: true, submitOnChange: true, required: false
        }
        // section("Create Devices that Simulate Buttons in HomeKit?") {
        //     paragraph title: "Description:", "HomeKit will create a switch device for each item selected.  The switch will change state to off after it fires.", state: "complete"
        //     input "buttonList", "capability.button", title: "Select Buttons Devices:  (${buttonList ? buttonList?.size() : 0} Selected)", required: false, multiple: true, submitOnChange: true
        //     input "momentaryList", "capability.momentary", title: "Select Momentary Devices:  (${momentaryList ? momentaryList?.size() : 0} Selected)", required: false, multiple: true, submitOnChange: true
        // }
        section() {
            paragraph title: "Device Counts:", "Total Devices: ${getDeviceCnt()}"
        }
        
        section("Create Mode/Routine Devices in HomeKit?") {
            paragraph title: "What are these for?", "HomeKit will create a switch device for each mode.  The switch will be ON for the active mode.", state: "complete"
            def modes = location?.modes?.sort{it?.name}?.collect { [(it?.id):it?.name] }
            input "modeList", "enum", title: "Create Devices for these Modes", required: false, multiple: true, options: modes, submitOnChange: true
            // def routines = location.helloHome?.getPhrases()?.sort { it?.label }?.collect { [(it?.id):it?.label] }
            // input "routineList", "enum", title: "Create Devices for these Routines", required: false, multiple: true, options: routines, submitOnChange: true
        }
        section("Smart Home Monitor Support (SHM)") {
            input "addShmDevice", "bool", title: "Allow SHM Control in HomeKit?", required: false, defaultValue: true, submitOnChange: true
        }
        section() {
            href url: getAppEndpointUrl("config"), style: "embedded", required: false, title: "View the Configuration Data for Homebridge", description: "Tap, select, copy, then click \"Done\""
        }
        section() {
            input "noTemp", "bool", title: "Remove Temp from Contact, Water Sensor?", required: false, defaultValue: false, submitOnChange: true
        	input "showLogs", "bool", title: "Show Events in Live Logs?", required: false, defaultValue: true, submitOnChange: true
        	label title: "SmartApp Label (optional)", description: "Rename this App", defaultValue: app?.name, required: false 
        }
    }
}

def getDeviceCnt() {
    def devices = []
    def items = ["deviceList", "sensorList", "switchList", "lightList", "fanList", "speakerList", "irrigationList", "hamptonBayFanLightList"]
    items?.each { item ->   
        if(settings[item]?.size() > 0) {     
            devices = devices + settings[item]
        }
    }
    return devices?.unique()?.size() ?: 0
}

def getAppEndpointUrl(subPath)	{ return "${apiServerUrl("/api/smartapps/installations/${app.id}${subPath ? "/${subPath}" : ""}?access_token=${atomicState.accessToken}")}" }

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	if(!state?.accessToken) {
        createAccessToken()
    }
	runIn(2, "registerDevices", [overwrite: true])
   	runIn(4, "registerSensors", [overwrite: true])
    runIn(6, "registerSwitches", [overwrite: true])
	state?.subscriptionRenewed = 0
    subscribe(location, null, HubResponseEvent, [filterEvents:false])
    if(settings?.addShmDevice) { subscribe(location, "alarmSystemStatus", changeHandler) }
    if(settings?.modeList) { 
        subscribe(location, "mode", changeHandler)
        if(state.lastMode == null) { state?.lastMode = location.mode?.toString() }
    }
    if(settings?.routineList) { subscribe(location, "routineExecuted", changeHandler) }
}

def renderDevices() {
    def deviceData = []
    def items = ["deviceList", "sensorList", "switchList", "lightList", "fanList", "speakerList", "irrigationList", "hamptonBayFanLightList", "modeList"]
    items?.each { item ->   
        if(settings[item]?.size()) {
            settings[item]?.each { dev->
                try {
                    def dData = getDeviceData(item, dev)
                    if(dData && dData?.size()) { deviceData?.push(dData) }
                } catch (e) {
                    log.error("Error Occurred Parsing Device ${dev?.displayName}, Error " + e.message)
                }
            }    
        }
    }
    if(settings?.addShmDevice == true) { deviceData.push(getShmDevice()) }
    return deviceData
}

def getDeviceData(type, sItem) {
    // log.debug "getDeviceData($type, $sItem)"
    def curType = null
    def devId = sItem
    def obj = null
    def name = null
    def attrVal = null
    def isVirtual = false
    switch(type) {
        case "routineList":
            isVirtual = true
            curType = "Routine"
            obj = getRoutineById(sItem)
            if(obj) {
                name = obj?.label
                attrVal = "off"
            }
            break
        case "modeList":
            isVirtual = true
            curType = "Mode"
            obj = getModeById(sItem)
            if(obj) {
                name = obj?.name
                attrVal = modeSwitchState(obj?.name)
            }
            break
        case "momentaryList":
        case "buttonList":
            isVirtual = true
            curType = "Button"
            obj = sItem
            if(obj) {
                name = obj?.displayName
                attrVal = "off"
            }
            break
        default:
            curType = "device"
            obj = sItem
            break
    }
    if(curType && obj) {
        return [
            name: !isVirtual ? sItem?.displayName : name,
            basename:  !isVirtual ? sItem?.name : name,
            deviceid: !isVirtual ? sItem?.id : devId,
            status: !isVirtual ? sItem?.status : "Online",
            manufacturerName: !isVirtual ? sItem?.getManufacturerName() : "SmartThings",
            modelName: !isVirtual ? (sItem?.getModelName() ?: sItem?.getTypeName()) : "${curType} Device",
            serialNumber: !isVirtual ? sItem?.getDeviceNetworkId() : "${curType}${devId}",
            firmwareVersion: "1.0.0",
            lastTime: !isVirtual ? sItem?.getLastActivity() : now(),
            capabilities: !isVirtual ? deviceCapabilityList(sItem) : ["${curType}": 1], 
            commands: !isVirtual ? deviceCommandList(sItem) : [on:[]], 
            attributes: !isVirtual ? deviceAttributeList(sItem) : ["switch": attrVal]
        ]
    } else { return null }
}

def modeSwitchState(mode) {
    return location?.mode?.toString() == mode ? "on" : "off"
}

def getShmDevice() {
    return [
        name: "Security Alarm",
        basename: "Security Alarm",
        deviceid: "alarmSystemStatus", 
        status: "ACTIVE",
        manufacturerName: "SmartThings",
        modelName: "Security System",
        serialNumber: "SHM",
        firmwareVersion: "1.0.0",
        lastTime: null,
        capabilities: ["Alarm System Status":1, "Alarm":1], 
        commands: [], 
        attributes: ["alarmSystemStatus": getShmStatus()]
    ]
}

def findDevice(paramid) {
    def device = deviceList.find { it?.id == paramid }
  	if (device) return device
	device = sensorList.find { it?.id == paramid }
	if (device) return device
  	device = switchList.find { it?.id == paramid }
    if (device) return device
    device = lightList.find { it?.id == paramid }
    if (device) return device
    device = fanList.find { it?.id == paramid }
    if (device) return device
    device = speakerList.find { it?.id == paramid }
    if (device) return device
    device = irrigationList.find { it?.id == paramid }
    if (device) return device
    device = hamptonBayFanLightList.find { it?.id == paramid }
    return device
    // device = momentaryList.find { it?.id == paramid }
    // if (device) return device
    // device = buttonList.find { it?.id == paramid }
    // return device
    
}

def findDeviceNew(paramid, type=null) {
    def items = ["deviceList", "sensorList", "switchList", "lightList", "fanList", "speakerList", "irrigationList", "momentaryList", "buttonList"]
    if(type) { items = ["${type}"] }
    items?.each { item ->   
        if(settings[item]?.size()) {
            settings[item]?.each { dev->
                if(dev?.id == paramid) { 
                    log.debug "${getObjType(dev)}"
                    return dev 
                }
            }
        }
    }
    return null
}

def authError() {
    return [error: "Permission denied"]
}

def getObjType(obj) {
	if(obj instanceof String) {return "String"}
	else if(obj instanceof GString) {return "GString"}
	else if(obj instanceof Map) {return "Map"}
    else if(obj instanceof Collection) {return "Collection"}
    else if(obj instanceof Closure) {return "Closure"}
    else if(obj instanceof LinkedHashMap) {return "LinkedHashMap"}
    else if(obj instanceof HashMap) {return "HashMap"}
	else if(obj instanceof List) {return "List"}
	else if(obj instanceof ArrayList) {return "ArrayList"}
	else if(obj instanceof Integer) {return "Integer"}
	else if(obj instanceof BigInteger) {return "BigInteger"}
	else if(obj instanceof Long) {return "Long"}
	else if(obj instanceof Boolean) {return "Boolean"}
	else if(obj instanceof BigDecimal) {return "BigDecimal"}
	else if(obj instanceof Float) {return "Float"}
	else if(obj instanceof Byte) {return "Byte"}
	else { return "unknown"}
}

def getShmStatus(retInt=false) {
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

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        platforms: [
            [
                platform: "SmartThings",
                name: "SmartThings",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state?.accessToken
            ]
        ]
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def renderLocation() {
    return [
    	latitude: location?.latitude,
    	longitude: location?.longitude,
    	mode: location?.mode,
    	name: location?.name,
    	temperature_scale: location?.temperatureScale,
    	zip_code: location?.zipCode,
        hubIP: location?.hubs[0]?.localIP,
        smartapp_version: appVersion
  	]
}

def CommandReply(statusOut, messageOut) {
    def replyJson = new groovy.json.JsonOutput().toJson([status: statusOut, message: messageOut])
    render contentType: "application/json", data: replyJson
}

def deviceCommand() {
	log.info("Command Request: $params")
	def device = findDevice(params?.id)    
    def command = params?.command
    if(settings?.addShmDevice != false && params?.id == "alarmSystemStatus") {
        setShmMode(command)
        CommandReply("Success", "Security Alarm, Command $command")
    }  else if (settings?.hamptonBayFanLightList && command == "fanspeed") {
        def value1 = request.JSON?.value1
        if(value1 && device?.hasCommand(value1)) { dev?."${value1}"() }
        CommandReply("Success", "Routine Device, Command $command")
    } else if (settings?.modeList && command == "mode") {
        def value1 = request.JSON?.value1
        log.debug "value1: ${value1}"
        if(value1) { changeMode(value1) }
        CommandReply("Success", "Mode Device, Command $command")
    } else if (settings?.routineList && command == "routine") {
        def value1 = request.JSON?.value1
        if(value1) { runRoutine(value1) }
        CommandReply("Success", "Routine Device, Command $command")
    } else if ((settings?.buttonList || settings?.momentaryList) && command == "button") {
        device.on()
        CommandReply("Success", "Button Device, Command ON")
    } else {
        if (!device) {
            log.error("Device Not Found")
            CommandReply("Failure", "Device Not Found")
        } else if (!device.hasCommand(command)) {
            log.error("Device "+device.displayName+" does not have the command "+command)
            CommandReply("Failure", "Device "+device.displayName+" does not have the command "+command)
        } else {
            def value1 = request.JSON?.value1
            def value2 = request.JSON?.value2
            try {
                if (value2) {
                    device."$command"(value1,value2)
                } else if (value1) {
                    device."$command"(value1)
                } else {
                    device."$command"()
                }
                log.info("Command Successful for Device "+device.displayName+", Command "+command)
                CommandReply("Success", "Device "+device.displayName+", Command "+command)
            } catch (e) {
                log.error("Error Occurred For Device "+device.displayName+", Command "+command)
                CommandReply("Failure", "Error Occurred For Device "+device.displayName+", Command "+command)
            }
        }
    }
}

def setShmMode(mode) {
    log.info "Setting the Smart Home Monitor Mode to (${mode})..."
    sendLocationEvent(name: 'alarmSystemStatus', value: mode.toString())
}

def changeMode(mode) {
    if(mode) {
        log.info "Setting the Location Mode to (${mode})..."
        setLocationMode(mode)
        state.lastMode = mode
    }
}

def runRoutine(rt) {
    if(rt) {
        log.info "Executing the (${rt}) Routine..."
        location.helloHome?.execute(rt)
    }
}

def deviceAttribute() {
	def device = findDevice(params.id)
    def attribute = params.attribute
  	if (!device) {
    	httpError(404, "Device not found")
  	} else {
      	def currentValue = device.currentValue(attribute)
      	[currentValue: currentValue]
  	}
}

def findVirtModeDevice(id) {
    if(getModeById(id)) {
        return getModeById(id)
    } 
    return null
}

def findVirtRoutineDevice(id) {
    if (getRoutineById(id)) {
        return getRoutineById(id)
    }
    return null
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
                    commands: [on:[]], 
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
    def items = device?.capabilities?.collectEntries { capability-> [ (capability?.name):1 ] }
    if(settings?.irrigationList?.find { it?.id == device?.id }) { 
		items["Irrigation"] = 1
    }
    if(settings?.lightList.find { it?.id == device?.id }) {
        items["LightBulb"] = 1
    }
    if(settings?.fanList.find { it?.id == device?.id }) {
        items["Fan"] = 1
    }
    if(settings?.speakerList.find { it?.id == device?.id }) {
        items["Speaker"] = 1
    }
    if(settings?.hamptonBayFanLightList.find { it?.id == device?.id } && items["Switch Level"] && items["Fan Speed"]) {
        items["Fan Light"] = 1
        items["LightBulb"] = 1
        items["Fan"] = 1
    }
    if(settings?.noTemp && items["Temperature Measurement"] && (items["Contact Sensor"] != null || items["Water Sensor"] != null)) {
        items.remove("Temperature Measurement")
    }
	return items
}

def deviceCommandList(device) {
  	device.supportedCommands.collectEntries { command->
    	[ (command?.name): (command?.arguments) ]
  	}
}

def deviceAttributeList(device) {
  	device.supportedAttributes.collectEntries { attribute->
        // if(!(ignoreTheseAttributes()?.contains(attribute?.name))) {
            try {
                [(attribute?.name): device?.currentValue(attribute?.name)]
            } catch(e) {
                [(attribute?.name): null]
            }
        // }
  	}
}

def getAllData() {
    state?.subscriptionRenewed = now()
    state?.devchanges = []
	def deviceData = [location: renderLocation(), deviceList: renderDevices()]
    def deviceJson = new groovy.json.JsonOutput().toJson(deviceData)
    render contentType: "application/json", data: deviceJson
}

def registerDevices() {
    //This has to be done at startup because it takes too long for a normal command.
    log.debug "Registering (${settings?.deviceList?.size() ?: 0}) Other Devices"
	registerChangeHandler(settings?.deviceList)
    log.debug "Registering (${settings?.irrigationList?.size() ?: 0}) Sprinklers"
    registerChangeHandler(settings?.irrigationList)
}

def registerSensors() {
    //This has to be done at startup because it takes too long for a normal command.
    log.debug "Registering (${settings?.sensorList?.size() ?: 0}) Sensors"
    registerChangeHandler(settings?.sensorList)
    log.debug "Registering (${settings?.speakerList?.size() ?: 0}) Speakers"
    registerChangeHandler(settings?.speakerList)
    log.debug "Registering (${settings?.hamptonBayFanLightList?.size() ?: 0}) FanLights"
    registerChangeHandler(settings?.hamptonBayFanLightList)
}

def registerSwitches() {
    //This has to be done at startup because it takes too long for a normal command.
    log.debug "Registering (${settings?.switchList?.size() ?: 0}) Switches"
	registerChangeHandler(settings?.switchList)
    log.debug "Registering (${settings?.lightList?.size() ?: 0}) Lights"
    registerChangeHandler(settings?.lightList)
    log.debug "Registering (${settings?.fanList?.size() ?: 0}) Fans"
    registerChangeHandler(settings?.fanList)
}

def ignoreTheseAttributes() {
    return [
        'DeviceWatch-DeviceStatus', 'checkInterval', 'devTypeVer', 'dayPowerAvg', 'apiStatus', 'yearCost', 'yearUsage','monthUsage', 'monthEst', 'weekCost', 'todayUsage',
		'maxCodeLength', 'maxCodes', 'readingUpdated', 'maxEnergyReading', 'monthCost', 'maxPowerReading', 'minPowerReading', 'monthCost', 'weekUsage', 'minEnergyReading',
		'codeReport', 'scanCodes', 'verticalAccuracy', 'horizontalAccuracyMetric', 'altitudeMetric', 'latitude', 'distanceMetric', 'closestPlaceDistanceMetric',
		'closestPlaceDistance', 'leavingPlace', 'currentPlace', 'codeChanged', 'codeLength', 'lockCodes', 'healthStatus', 'horizontalAccuracy', 'bearing', 'speedMetric',
		'speed', 'verticalAccuracyMetric', 'altitude', 'indicatorStatus', 'todayCost', 'longitude', 'distance', 'previousPlace','closestPlace', 'places', 'minCodeLength',
		'arrivingAtPlace', 'lastUpdatedDt'
    ]
}

def registerChangeHandler(devices, showlog=false) {
	devices?.each { device ->
		List theAtts = device?.supportedAttributes?.collect { it?.name as String }?.unique()
        if(showlog) { log.debug "atts: ${theAtts}" }
		theAtts?.each {att ->
            if(!(ignoreTheseAttributes().contains(att))) {
                if(settings?.noTemp && att == "temperature" && (device?.hasAttribute("contact") || device?.hasAttribute("water"))) {
                    return 
                } else {
                    subscribe(device, att, "changeHandler")
                    if(showlog) { log.debug "Registering ${device?.displayName}.${att}" }
                }
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
        case "alarmSystemStatus":
            deviceid = evt?.name
            state?.alarmSystemStatus = value
            sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            break
        case "mode":
            settings?.modeList?.each { id->
                def md = getModeById(id)
                if(md && md?.id ) { sendItems?.push([evtSource: "MODE", evtDeviceName: md?.name, evtDeviceId: md?.id, evtAttr: "switch", evtValue: modeSwitchState(md?.name), evtUnit: "", evtDate: dt]) }
            }
            break
        case "routineExecuted":
            settings?.routineList?.each { id->
                def rt = getRoutineById(id)
                if(rt && rt?.id ) { sendItems?.push([evtSource: "ROUTINE", evtDeviceName: rt?.label, evtDeviceId: rt?.id, evtAttr: "switch", evtValue: "off", evtUnit: "", evtDate: dt]) } 
            }
            break
        default:
            // if(findDeviceNew(deviceid, "buttonList") || findDeviceNew(deviceid, "momentaryList")) {
            //     sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: "off", evtUnit: evt?.unit ?: "", evtDate: dt])
            // } else {
                sendItems?.push([evtSource: src, evtDeviceName: deviceName, evtDeviceId: deviceid, evtAttr: attr, evtValue: value, evtUnit: evt?.unit ?: "", evtDate: dt])
            // }
            break
    }
    if (sendEvt && state?.directIP != "" && sendItems?.size()) {
    	//Send Using the Direct Mechanism
        sendItems?.each { send->
            if(settings?.showLogs) { 
                log.debug "Sending${" ${send?.evtSource}" ?: ""} Event (${send?.evtDeviceName} | ${send?.evtAttr.toUpperCase()}: ${send?.evtValue}${send?.evtUnit}) to Homebridge at (${state?.directIP}:${state?.directPort})" 
            }
            def result = new physicalgraph.device.HubAction(
                method: "POST",
                path: "/update",
                headers: [
                    HOST: "${state?.directIP}:${state?.directPort}",
                    'Content-Type': 'application/json'
                ],
                body: [
                    change_name: send?.evtDeviceName,
                    change_device: send?.evtDeviceId,
                    change_attribute: send?.evtAttr,
                    change_value: send?.evtValue,
                    change_date: send?.evtDate
                ]
            )
            sendHubCommand(result)
        }
    }
}


def getModeById(mId) {
    return location?.getModes()?.find{it?.id == mId}
}

def getRoutineById(rId) {
    return location?.helloHome?.getPhrases()?.find{it?.id == rId}
}

def getModeByName(name) {
    return location?.getModes()?.find{it?.name == name}
}

def getRoutineByName(name) {
    return location?.helloHome?.getPhrases()?.find{it?.label == name}
}

def getShmIncidents() {
    //Thanks Adrian
    def incidentThreshold = now() - 604800000
    return location.activeIncidents.collect{[date: it?.date?.time, title: it?.getTitle(), message: it?.getMessage(), args: it?.getMessageArgs(), sourceType: it?.getSourceType()]}.findAll{ it?.date >= incidentThreshold } ?: null
}

def enableDirectUpdates() {
	log.debug("Command Request: ($params)")
	state?.directIP = params.ip
    state?.directPort = params.port
	log.debug("Trying ${state?.directIP}:${state?.directPort}")
	def result = new physicalgraph.device.HubAction(
    		method: "GET",
    		path: "/initial",
    		headers: [
        		HOST: "${state?.directIP}:${state?.directPort}"
    		],
    		query: deviceData
		)
     sendHubCommand(result)
}

def HubResponseEvent(evt) {
	// log.debug(evt.description)
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    log.debug "cp desc: " + description
    if (description?.count(",") > 4) {
        def bodyString = new String(description.split(',')[5].split(":")[1].decodeBase64())
        log.debug(bodyString)
    }
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state?.accessToken)) {
        path("/devices")                        { action: [GET: "authError"] }
        path("/config")                         { action: [GET: "authError"] }
        path("/location")                       { action: [GET: "authError"] }
        path("/:id/command/:command")     		{ action: [POST: "authError"] }
        path("/:id/query")						{ action: [GET: "authError"] }
        path("/:id/attribute/:attribute") 		{ action: [GET: "authError"] }
        path("/getUpdates")                     { action: [GET: "authError"] }
        path("/startDirect/:ip/:port")          { action: [GET: "authError"] }

    } else {
        path("/devices")                        { action: [GET: "getAllData"] }
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/location")                       { action: [GET: "renderLocation"] }
        path("/:id/command/:command")     		{ action: [POST: "deviceCommand"] }
        path("/:id/query")						{ action: [GET: "deviceQuery"] }
        path("/:id/attribute/:attribute") 		{ action: [GET: "deviceAttribute"] }
        path("/getUpdates")                     { action: [GET: "getChangeEvents"] }
        path("/startDirect/:ip/:port")          { action: [GET: "enableDirectUpdates"] }
    }
}

