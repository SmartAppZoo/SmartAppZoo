/*
 *  Domoticz (server)
 *
 *  Copyright 2019 Martin Verbeek
 *
    V7.00	Restructured Sensor Component, Thermostat, Motion, OnOff, now very dynamic with adding new capabilities
    V7.01	Push notification when customURL is invalid in Domoticz (access token was changed)
    V7.02	heartbeat to 5 minutes
    V7.03	customUrl is now copyable
    V7.04	check return in callbackforsettings
    V7.05	possibility in composite device to select 2 custom (type=general) devices
    V7.06	Some maint on the custom sensor, add subtype energy/gas to report select
    V7.10	Restructure of the way status is updated from the sensors
    V7.11	Bug fixes
    V7.12	Decode needed for LevelNames with last stable of DZ
    V7.13	open closed in lowercase
    V7.14	selector switch problem with Off
    V7.15	temperature for thermostat in composite creation, add callable function for selector device to reset notifications
    V7.16	Temp not send to domoticzSensor native, changed doUtilityEvent to check if native sensor exists 
    V7.17	Option to assign DZ idx to Power/Gas reporting device, it will skip total computation of utility devices and take total values direct from a DZ metering device
    V7.18	Changed setup of composite devices page so you can select utility report even no other sensors exists
    V7.19	Bug fixing
    V7.20	real time update of scene/group status, look if an event has to do with a device that is part of a group/scene and requestr a refresh of the related group/scene
    		also fixed a potential problem with scenes and group unique dni issue. THIS WILL ADD NEW GROUP/SCENE DEVICES...PLEASE DELETE OLD ONES MANUALLY.
	V7.21	Some enhancements to the setup screens
    V7.22	SetPower for power/energy virtual devices defined in Domoticz
    V7.23	Testing with getUrl for websettings/storesettings form, looks good
    V7.24	Power Report changes
    V7.25	energy used not energyMeter
    V7.26	Live Checker rework (gives not responding in smaller environments) and bugs wrt power/energy etc...
    V7.27	kill push notifications
    V7.28	bug in refreshdevices when settings.roomplans = null
    V7.29	consumptionlow to consumptionLow
    V7.30	bug fix power
 */

import groovy.json.*
import groovy.time.*
import java.Math.*
import java.net.URLEncoder
import java.util.regex.Pattern

definition(
    name: "Domoticz Server",
    namespace: "verbem",
    author: "Martin Verbeek",
    description: "Connects to local Domoticz server and define Domoticz devices in ST",
    category: "My Apps",
    singleInstance: false,
    oauth: true,
    iconUrl: "http://www.thermosmart.nl/wp-content/uploads/2015/09/domoticz-450x450.png",
    iconX2Url: "http://www.thermosmart.nl/wp-content/uploads/2015/09/domoticz-450x450.png",
    iconX3Url: "http://www.thermosmart.nl/wp-content/uploads/2015/09/domoticz-450x450.png"
)

private def cleanUpNeeded() {return true}
private def runningVersion() {"7.30"}
private def textVersion() { return "Version ${runningVersion()}"}

/*-----------------------------------------------------------------------------------------*/
/*		Mappings for REST ENDPOINT to communicate events from Domoticz      
/*-----------------------------------------------------------------------------------------*/
mappings {
    path("/EventDomoticz") {
        action: [ GET: "eventDomoticz" ]
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		PREFERENCES      
/*-----------------------------------------------------------------------------------------*/
preferences {
    page name:"setupInit"
    page name:"setupMenu"
    page name:"setupDomoticz"
    page name:"setupListDevices"
    page name:"setupDeviceRequest"
    page name:"setupRefreshToken"
    page name:"setupCompositeSensors"
    page name:"setupCompositeSensorsAssignment"
    page name:"setupSmartThingsToDomoticz"
}

/*-----------------------------------------------------------------------------------------*/
/*		SET Up INIT
/*-----------------------------------------------------------------------------------------*/
private def setupInit() {
    TRACE("[setupInit]")
    unsubscribe()
    subscribe(location, null, onLocation, [filterEvents:true])

    if (!state.accessToken) {
        initRestApi()
    }
    if (state.setup) {
        // already initialized, go to setup menu
        return setupMenu()
    }
    /* 		Initialize app state and show welcome page */
    state.setup = [:]
    state.setup.installed = false
    state.devices = [:]
    
    return setupWelcome()
}
/*-----------------------------------------------------------------------------------------*/
/*		SET Up Welcome PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupWelcome() {
    TRACE("[setupWelcome]")

    def textPara1 =
        "Domoticz Server allows you to integrate Domoticz defined devices into " +
        "SmartThings. If you miss support for devices open an issue on github " +
        "Please note that it requires a server running " +
        "Domoticz. This must be installed on the local network and accessible from " +
        "the SmartThings hub. The local network must be WHITELISTED in the Domoticz settings!\n\n"
     
    def pageProperties = [
        name        : "setupInit",
        title       : "Welcome!",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : state.setup.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textPara1
            paragraph "${app.name}. ${textVersion()}\n${textCopyright()}"
        }
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		SET Up Menu PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupMenu() {
    TRACE("[setupMenu]")
    def urlCAH
    if (state.accessToken) {
		state.urlCustomActionHttp = getApiServerUrl() - ":443" + "/api/smartapps/installations/${app.id}/" + "EventDomoticz?access_token=" + state.accessToken + "&message=#MESSAGE"
		urlCAH = getApiServerUrl() - ":443" + "/api/smartapps/installations/${app.id}/" + "EventDomoticz?access_token=" + state.accessToken + "&message=#MESSAGE"
        if (!state.validUrl) state.validUrl = false
	// Broken Domoticz settings
        //socketSend([request: "Settings"])
        pause 5
    }

    if (!settings.containsKey('domoticzIpAddress')) {
        return setupDomoticz()
    }

    def pageProperties = [
        name        : "setupMenu",
        title       : "Setup Menu",
        nextPage    : null,
        install     : true,
        uninstall   : state.setup.installed
    ]
    
	return dynamicPage(pageProperties) {
        section {
            href "setupDomoticz", title:"Configure Domoticz Server", description:"Tap to open", image: "http://www.thermosmart.nl/wp-content/uploads/2015/09/domoticz-450x450.png"
            href "setupDeviceRequest", title:"Overview of device selection criteria", description:"Tap to open", image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/Settings.jpg"
        	if (state.devices.size() > 0) {
                href "setupListDevices", title:"List Installed Devices", description:"Tap to open", image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/Overview.png"
            }
            href "setupCompositeSensors", title:"Create Composite Devices", description:"Tap to open", image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/Composite.png"	
            href "setupRefreshToken", title:"Revoke/Recreate Access Token", description:"Tap to open", image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/OAuth2.png"
            href "setupSmartThingsToDomoticz", title:"Define SmartThing Devices in Domoticz", description:"Tap to open", image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/SmartThings.jpg"
        }		
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }      
        section("About") {
            paragraph "${app.name}. ${textVersion()}\n${textCopyright()}"
        }
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		SET Up Configure Composite Sensors PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupCompositeSensors() {
    TRACE("[setupCompositeSensors]")
    
    def pageProperties = [
        name        : "setupCompositeSensors",
        title       : "Configure Composite Sensors",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : false
    ]
      
    return dynamicPage(pageProperties) {
		section {
        	input "domoticzReportPower", "bool", title: "Create Utility Report device(s)", submitOnChange: true, defaultValue: false 
        }
        if (state?.devices.size() > 0) {
            section {
            	paragraph "Add Components to devices"
                state.devices.findAll{it.value.deviceType.matches("domoticzSensor|domoticzMotion|domoticzThermostat|domoticzPowerReport")}.sort{it.value.name}.sort{it.value.deviceType}.each { key, item ->
                    def iMap = item as Map
                    if (item.deviceType.matches("domoticzSensor|domoticzMotion|domoticzThermostat") || (item.deviceType == "domoticzPowerReport" && domoticzReportPower)) {
                        href "setupCompositeSensorsAssignment",  params: iMap , description:" ", title:"${iMap.deviceType - "domoticz"}\n${iMap.name}", image:"https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/${item.deviceType}.png"
                    }
                }
        	}
      	}
	}
	addReportDevices()    
    sendThermostatModes()
}

private def setupCompositeSensorsAssignment(iMap) {
    TRACE("[setupCompositeSensorsAssignment]")
    
    def pageProperties = [
        name        : "setupCompositeSensorsAssignment",
        title       : "Add Components(Capabilities) to ${iMap.name}",
        nextPage    : "setupCompositeSensors",
        install     : false,
        uninstall   : false
    ]
    
    return dynamicPage(pageProperties) {
    	if (iMap.deviceType == "domoticzSensor") {
            section {           
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather12-icn@2x.png", "Relative Humidity Measurement"
                input "idxHumidity[${iMap.idx}]", "enum", options: state.optionsHumidity, required: false   

                paragraph image:"http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png", "Illuminance Measurement"
                input "idxIlluminance[${iMap.idx}]", "enum", options: state.optionsLux, required: false

                paragraph image:"https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/devicetypes/verbem/domoticzsensor.src/barometer-icon-png-5.png", "Barometric Pressure"
                input "idxPressure[${iMap.idx}]", "enum", options: state.optionsPressure, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png", "Power Meter"
                input "idxPower[${iMap.idx}]", "enum", options: state.optionsPower, required: false

                paragraph image:"https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/devicetypes/verbem/domoticzsensor-air-quality-sensor.src/airQuality.png", "Air Quality Sensor"
                input "idxAirQuality[${iMap.idx}]", "enum", options: state.optionsAirQuality, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Entertainment/entertainment3-icn@2x.png", "Sound Sensor"
                input "idxSound[${iMap.idx}]", "enum", options: state.optionsSound, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#1"
                input "idxCustom1[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#2"
                input "idxCustom2[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 

            }
    	}
    	if (iMap.deviceType == "domoticzThermostat") {
            section {           
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png", "Temperature Measurement"
                input "idxTemperature[${iMap.idx}]", "enum", options: state.optionsTemperature, required: false 
                
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather12-icn@2x.png", "Relative Humidity Measurement"
                input "idxHumidity[${iMap.idx}]", "enum", options: state.optionsHumidity, required: false   

                paragraph image:"http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png", "Power Meter"
                input "idxPower[${iMap.idx}]", "enum", options: state.optionsPower, required: false
             
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather1-icn@2x.png", "Thermostat FanMode"
                input "idxFanMode[${iMap.idx}]", "enum", options: state.optionsModes, required: false
                
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png", "Thermostat Mode"
                input "idxMode[${iMap.idx}]", "enum", options: state.optionsModes, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Home/home29-icn@2x.png", "Gas Meter"
                input "idxGas[${iMap.idx}]", "enum", options: state.optionsGas, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#1"
                input "idxCustom1[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#2"
                input "idxCustom2[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 
            }
    	}
    	if (iMap.deviceType == "domoticzMotion") {
            section {           
                paragraph image:"http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png", "Temperature Measurement"
                input "idxTemperature[${iMap.idx}]", "enum", options: state.optionsTemperature, required: false 
                
                paragraph image:"http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png", "Illuminance Measurement"
                input "idxIlluminance[${iMap.idx}]", "enum", options: state.optionsLux, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png", "Power Meter"
                input "idxPower[${iMap.idx}]", "enum", options: state.optionsPower, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Entertainment/entertainment3-icn@2x.png", "Sound Sensor"
                input "idxSound[${iMap.idx}]", "enum", options: state.optionsSound, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#1"
                input "idxCustom1[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 

                paragraph image:"http://cdn.device-icons.smartthings.com/Office/office11-icn@2x.png", "Custom Sensor#2"
                input "idxCustom2[${iMap.idx}]", "enum", options: state.optionsCustom, required: false 
            }
    	}
    	if (domoticzReportPower == true && iMap.deviceType == "domoticzPowerReport") {
            section {           
                paragraph image:"http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png", "Power Meter"
                input "idxPower[${iMap.idx}]", "enum", options: state.optionsPower, required: false

                paragraph image:"http://cdn.device-icons.smartthings.com/Home/home29-icn@2x.png", "Gas Meter"
                input "idxGas[${iMap.idx}]", "enum", options: state.optionsGas, required: false
            }
    	}
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		SET Up Configure Domoticz PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupDomoticz() {
    TRACE("[setupDomoticz]")

	if (settings.containsKey('domoticzIpAddress')) {
    	state.networkId = settings.domoticzIpAddress + ":" + settings.domoticzTcpPort
    	socketSend([request: "roomplans"])
		pause 5
    	}
    
    def textPara1 =
        "Enter IP address and TCP port of your Domoticz Server, then tap " +
        "Next to continue."
    
    def pageProperties = [
        name        : "setupDomoticz",
        title       : "Configure Domoticz Server",
        nextPage    : null,
        install     : false,
        uninstall   : false
    ]

    return dynamicPage(pageProperties) {
      section {
            input "domoticzIpAddress", "text", title: "Local Domoticz IP Address", submitOnChange: true, defaultValue: "0.0.0.0"
            input "domoticzTcpPort", "number", title: "Local Domoticz TCP Port", defaultValue: "8080"
            input "domoticzTypes","enum", title: "Devicetypes you want to add", options: ["Contact Sensors", "Dusk Sensors", "Motion Sensors", "On/Off/Dimmers/RGB", "Smoke Detectors", "Thermostats", "(Virtual) Sensors", "Window Coverings"], multiple: true
            if (settings.containsKey('domoticzIpAddress') && settings?.domoticzIpAddress != "0.0.0.0") input "domoticzRoomPlans", "bool", title: "Support Room Plans from Domoticz?", submitOnChange: true, defaultValue: false
            if (domoticzRoomPlans && settings.containsKey('domoticzIpAddress')) input "domoticzPlans","enum", title: "Select the rooms", options: state.listPlans, submitOnChange : true, multiple: true
            if (domoticzPlans) input "domoticzDDIP", "bool", title: "Only keep devices related to a Roomplan?", defaultValue: false
            input "domoticzGroup","bool", title: "Add Groups from Domoticz?", defaultValue: false
            input "domoticzScene", "bool", title: "Add Scenes from Domoticz?", defaultValue: false
            input "domoticzTrace", "bool", title: "Debug trace output in IDE log", defaultValue: true
        	}
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		SET Up Configure Domoticz PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupSmartThingsToDomoticz() {
    TRACE("[setupSmartThingsToDomoticz]")
     
    def pageProperties = [
        name        : "setupSmartThingsToDomoticz",
        title       : "Configure SmartThings to Domoticz Server",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : false
    ]
    
    return dynamicPage(pageProperties) {
      section {
            input "domoticzVirtualDevices", "bool", title: "Define native Smartthings devices as Domoticz Virtuals", defaultValue: false, submitOnChange: true
        	}
      if (domoticzVirtualDevices) {
      	section {
      		paragraph "Select native SmartThing devices \n\n Childdevices created by this App will be IGNORED when you select them"
      		input "dzDevicesSwitches", "capability.switch", title:"Select switch devices", multiple:true, required:false
      		input "dzDevicesLocks", "capability.lock", title:"Select Locks", multiple:true, required:false
      		input "dzSensorsContact", "capability.contactSensor", title:"Select contact sensors", multiple:true, required:false
      		input "dzSensorsMotion", "capability.motionSensor", title:"Select motion sensors", multiple:true, required:false
      		input "dzSensorsTemp", "capability.temperatureMeasurement", title:"Select Temperature sensors", multiple:true, required:false
      		input "dzSensorsHum", "capability.relativeHumidityMeasurement", title:"Select Humidity sensors", multiple:true, required:false
      		input "dzSensorsIll", "capability.illuminanceMeasurement", title:"Select Illuminance sensors", multiple:true, required:false
            input "dzPowerMeters", "capability.powerMeter", title:"Select power meters", multiple:true, required:false
        }
      }
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		SET Up Add Domoticz Devices PAGE
/*-----------------------------------------------------------------------------------------*/
private def setupDeviceRequest() {
    TRACE("[setupDeviceRequest]")

    def textHelp =
        "Domoticz devices that have the following characteristics will be added when you SAVE: \n\n" +
        "Types ${domoticzTypes}.\n\n" 
        
    if (domoticzRoomPlans) 	textHelp = textHelp + "Devices in Rooms ${domoticzPlans} with the above types \n\n"
    if (domoticzScene) 		textHelp = textHelp + "Domoticz Scenes will be added. \n\n"
    if (domoticzGroup) 		textHelp = textHelp + "Domoticz Groups will be added."
          
    def pageProperties = [
        name        : "setupDeviceRequest",
        title       : "Add Domoticz Devices",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textHelp
        }
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		When having problems accessing DZ then execute refresh Token
/*-----------------------------------------------------------------------------------------*/
private def setupRefreshToken() {
    TRACE("[setupRefreshToken]")
	
    revokeAccessToken()
    def token = createAccessToken()
    
    state.urlCustomActionHttp = getApiServerUrl() - ":443" + "/api/smartapps/installations/${app.id}/" + "EventDomoticz?access_token=" + state.accessToken + "&message=#MESSAGE"
    //socketSend([request: "Settings"])
    def pageProperties = [
        name        : "setupRefreshToken",
        title       : "Refresh the access Token",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : false
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph "The Access Token has been refreshed"
            //paragraph "${state.urlCustomActionHttp}"
            paragraph "Tap Next to continue."
        }
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		List the child devices in the SMARTAPP
/*-----------------------------------------------------------------------------------------*/
private def setupListDevices() {
    TRACE("[setupListDevices]")
	refreshDevicesFromDomoticz()
    def textNoDevices =
        "You have not configured any Domoticz devices yet. Tap Next to continue."

    def pageProperties = [
        name        : "setupListDevices",
        title       : "Connected Devices idx - name",
        nextPage    : "setupMenu",
        install     : false,
        uninstall   : false
    ]

    if (state.devices.size() == 0) {
        return dynamicPage(pageProperties) {
            section {
                paragraph textNoDevices
            }
        }
    }
	def uniqueList = state.devices.findAll{key, item -> item.containsKey("deviceType") == true}.collect{it.value.deviceType}.unique().sort()
    
    return dynamicPage(pageProperties) {
    	uniqueList.each { deviceType ->
        	section(deviceType) {paragraph getDeviceListAsText(deviceType), image: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/domoticz-server.src/${deviceType}.png"}
        }
    }
}

def installed() {
    TRACE("[installed]")
    initialize()
}

def updated() {
    TRACE("[updated]")
    initialize()
}

def uninstalled() {
    TRACE("[uninstalled]")
    unschedule()
    unsubscribe()
	// delete ST HARDWARE in DZ
    if (settings?.domoticzVirtualDevices == true) socketSend([request:"DeleteHardware"])
	// clear custom notifications in DZ
    clearAllNotifications()
    
    // delete all child devices
    def devices = getChildDevices()
    devices?.each {
        try {
            deleteChildDevice(it.deviceNetworkId)
        } catch (e) {
            log.error "[uninstalled] Cannot delete device ${it.deviceNetworkId}. Error: ${e}"
        }
    }
}

private def initialize() {
    TRACE("[Initialize] ${app.name} ${app.label}. ${textVersion()}. ${textCopyright()}")
    //socketSend([request: "Settings"])    
    notifyNewVersion()
    
    unschedule()    
    unsubscribe()    
    
    if (state.accessToken) state.urlCustomActionHttp = getApiServerUrl() - ":443" + "/api/smartapps/installations/${app.id}/" + "EventDomoticz?access_token=" + state.accessToken + "&message=#MESSAGE"
    
    state.setup.installed 		= true
    state.networkId 			= settings.domoticzIpAddress + ":" + settings.domoticzTcpPort        
    state.alive 				= true
    state.aliveAgain 			= true
    state.devicesOffline 		= false
    state.scheduleCycle 		= 11i  // next cycle is 12, which triggers refresh of devices
	state.optionsLux 			= [:]
    state.optionsMotion 		= [:]
    state.optionsTemperature 	= [:]
    state.optionsCarbon1 		= [:]
    state.optionsCarbon2 		= [:]
    state.optionsPower 			= [:]
    state.optionsModes 			= [:]
    state.optionsGas 			= [:]
    state.optionsHumidity		= [:]
    state.optionsPressure		= [:]
    state.optionsAirQuality		= [:]
	state.optionsSound			= [:]
	state.optionsCustom			= [:]

	addReportDevices()
    assignSensorToDevice()
    
    //cleaning
    if (state.containsKey("listSensors")) state.remove("listSensors")
    if (state.containsKey("reportGasDay")) state.remove("reportGasDay")
    if (state.containsKey("reportGasMonth")) state.remove("reportGasMonth")
    if (state.containsKey("reportGasYear")) state.remove("reportGasYear")
    if (state.containsKey("reportPowerDay")) state.remove("reportPowerDay")
    if (state.containsKey("reportPowerMonth")) state.remove("reportPowerMonth")
    if (state.containsKey("reportPowerYear")) state.remove("reportPowerYear")
    if (state.containsKey("requestedReport")) state.remove("requestedReport")
    
    
    if (settings?.domoticzVirtualDevices == true) {
    	if (state.dzHardwareIdx == null) {
        	socketSend([request:"CreateHardware"])
       	}

        if (dzDevicesSwitches) 	subscribe(dzDevicesSwitches, "switch", handlerEvents)
        if (dzDevicesLocks) 	subscribe(dzDevicesLocks, "lock", handlerEvents)
        if (dzSensorsContact) 	subscribe(dzSensorsContact, "contact", handlerEvents)
        if (dzSensorsMotion) 	subscribe(dzSensorsMotion, "motion", handlerEvents)
        if (dzSensorsTemp) 		subscribe(dzSensorsTemp, "temperature", handlerEvents)
        if (dzSensorsHum) 		subscribe(dzSensorsHum, "humidity", handlerEvents)
        if (dzSensorsIll) 		subscribe(dzSensorsIll, "illuminance", handlerEvents)
        if (dzPowerMeters) 		subscribe(dzPowerMeters, "power", handlerEvents)
        if (dzPowerMeters) 		subscribe(dzPowerMeters, "energy", handlerEvents)
        if (dzPowerMeters) 		subscribe(dzPowerMeters, "energyMeter", handlerEvents)
        subscribe(app, eventTest)
        
        runIn(10, defineSmartThingsInDomoticz) 
    }
    else {
    	state.remove("virtualDevices")
        socketSend([request:"DeleteHardware"])
        state.remove("dzHardwareIdx")
    }
        
    if 	(cleanUpNeeded() == true) {
        if (state?.runUpdateRoutine != runningVersion()) runUpdateRoutine()
        state.runUpdateRoutine = runningVersion()
    }
    
	aliveChecker()
	runEvery5Minutes(aliveChecker)        
    schedule("2015-01-09T12:00:00.000-0600", notifyNewVersion)   
}

private def runUpdateRoutine() {
// check for old style SCENE / GROUP dni
	return
	TRACE("[runUpdateRoutine]")
}

private def clearAllNotifications() {
	state.devices.each {key, item ->
        TRACE("[clearAllNotifications] Clear Notifications for Devices ${item.type} ${item.dni} idx ${item.idx}")
        socketSend([request : "ClearNotification", idx : item.idx])
    }

	def options = state.findAll { key, value -> key.startsWith("options") }
    options.each { key, sensor ->
    	sensor.each { idx, content ->
        	TRACE("[clearAllNotifications] Clear Notifications for Sensor ${content} idx ${idx}")
        	socketSend([request : "ClearNotification", idx : idx])       
        }
    }    
}

private def assignSensorToDevice() {
    def idx 
    def capability 
    def component = null
    def dni
    def copyDevs = [:] << state.devices

	//Remove all idxCapability settings from state.devices that do not have a idx defined to it anymore
    copyDevs.each { key, dev ->
        dev.each { k, item ->
            if (k.length() > 3 && k.startsWith("idx")) {
            	capability = settings?."${k}[${key}]"
                if (!capability && k != "idxPower" || key == item) { // leave idxPower as it might be set automically, also leave capabilities that have same idx as device
                    state.devices[key]."${k}" = null
                }
         	}
    	}
    }

	//set all assigned capabilities in state.devices

	idxSettings().each { k, v ->    
        idx = k.tokenize('[')[1]
        idx = idx.tokenize(']')[0].toString()       
        capability = k.tokenize('[')[0]
       
        if (state.devices[idx]) {
            state.devices[idx]."${capability}" = v
            dni = state.devices[idx].dni
            if (!getChildDevice(dni)) log.warn "${state.devices[idx].dni}"
            if (getChildDevice(dni).hasCommand("configure")) {
            	def componentList = [idxHumidity	: "Relative Humidity Measurement",
                					idxIlluminance	: "Illuminance Measurement",
                                    idxPressure		: "Barometric Pressure",
                                    idxPower		: "Power Meter",
                                    idxGas			: "Gas Meter",
                                    idxAirQuality	: "Air Quality Sensor",
                                    idxSound		: "Sound Sensor",
                                    idxTemperature	: "Temperature Measurement",
                                    idxCustom1		: "Custom Sensor 1",
                                    idxCustom2		: "Custom Sensor 2",
                					]
                component = componentList.find{it.key == capability}?.value
                if (component) getChildDevice(dni).configure(component)
            }
            else if (idx.toInteger() < 10000) TRACE("no configure on ${dni}")
        }
        else TRACE("no state.devices for ${idx}")
    }
}

void scheduledListSensorOptions() {
	TRACE("[scheduledListSensorOptions]")
	socketSend([request : "OptionUtility"])
    socketSend([request : "OptionTemperature"])
    socketSend([request : "OptionDevices"])
}

void defineSmartThingsInDomoticz() {
	if (!state?.unitcode) state.unitcode = 0
    
    def unitcode = state.unitcode
    def type
    def exists
    
    //DEVICES
    dzDevicesSwitches.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            type = "switch"
            if (getVirtualIdx([name: dev.displayName, type: type]) == null) {
                socketSend([request:"CreateVirtualDevice", deviceName:dev.displayName.replaceAll(" ", "%20"), switchType:0, unitcode: unitcode])
                unitcode = unitcode + 1
                // type 0 = on Off, 7 = dimmer, 8 = motion, contact = 2, lock = 19
            }
        }
    }
    dzDevicesLocks.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            type = "lock"
            if (getVirtualIdx([name: dev.displayName, type: type]) == null) {
                socketSend([request:"CreateVirtualDevice", deviceName:dev.displayName.replaceAll(" ", "%20"), switchType:19, unitcode: unitcode])
                unitcode = unitcode + 1
            }
        }
    }
    dzSensorsContact.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            type = "contact"
            if (getVirtualIdx([name: dev.displayName, type: type]) == null) {
                socketSend([request:"CreateVirtualDevice", deviceName:dev.displayName.replaceAll(" ", "%20"), switchType:2, unitcode: unitcode])
                unitcode = unitcode + 1
            }
        }
    }
    dzSensorsMotion.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            type = "motion"
            if (getVirtualIdx([name: dev.displayName, type: type]) == null) {
                socketSend([request:"CreateVirtualDevice", deviceName:dev.displayName.replaceAll(" ", "%20"), switchType:8, unitcode: unitcode])
                unitcode = unitcode + 1
            }
        }
    }
    state.unitcode = unitcode
    // SENSORS
    dzSensorsTemp.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            if (getVirtualIdx([name: dev.displayName, type: "temperature"]) == null) {
            	TRACE("Temperature creation for (${dev.displayName})")
                socketSend([request:"CreateVirtualSensor", deviceName:dev.displayName.replaceAll(" ", "%20"), sensorType:80])
            }
        }
    }
    dzSensorsIll.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            if (getVirtualIdx([name: dev.displayName, type: "illuminance"]) == null) {
            	TRACE("illuminance creation for (${dev.displayName})")
                socketSend([request:"CreateVirtualSensor", deviceName:dev.displayName.replaceAll(" ", "%20"), sensorType:246])
            }
        }
    }
    dzSensorsHum.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            if (getVirtualIdx([name: dev.displayName, type: "humidity"]) == null) {
            	TRACE("humidity creation for (${dev.displayName})")
				socketSend([request:"CreateVirtualSensor", deviceName:dev.displayName.replaceAll(" ", "%20"), sensorType:81])
            }
        }
    }
    dzPowerMeters.each { dev ->
        if (dev.deviceNetworkId.contains("IDX") == false) {
            if (getVirtualIdx([name: dev.displayName, type: "power"]) == null) {
            	TRACE("Power meter creation for (${dev.displayName})")
                socketSend([request:"CreateVirtualCustomDevice", deviceName:dev.displayName.replaceAll(" ", "%20"), deviceType:243, deviceSubType:29])
            }
        }
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		Update the usage info in virtual domoticz devices that have been selected by user to sync to DZ
/*-----------------------------------------------------------------------------------------*/
void handlerEvents(evt) {

	if (!evt?.isStateChange) {
        if (evt?.isStateChange() == false) {
            return
        }
    }

	def dev = evt?.device
    
    if (!dev) return
    if (dev?.typeName.contains("domoticz")) return
    
    def idx
    if (evt.name.contains("energy")) idx = getVirtualIdx([name:dev.displayName, type: "power"])
    else idx = getVirtualIdx([name:dev.displayName, type: evt.name])
	
    if (idx) {   
        switch (evt.name) {
        case "switch":
            socketSend([request: evt.stringValue, idx: idx])
            break
        case "lock":
        	if (evt.stringValue == "locked") socketSend([request: "on", idx: idx]) else socketSend([request: "off", idx: idx])
            break
        case "motion":
        	if (evt.stringValue.matches("inactive|off")) socketSend([request: "off", idx: idx]) else socketSend([request: "on", idx: idx])
            break
        case "contact":
        	if (evt.stringValue == "closed") socketSend([request: "SetContact", idx: idx, nvalue:0]) else socketSend([request: "SetContact", idx: idx, nvalue:1])
            break
        case "temperature":
        	socketSend([request: "SetTemp", idx: idx, temp:evt.stringValue])
            break
        case "humidity":
        	socketSend([request: "SetHumidity", idx: idx, humidity:evt.stringValue])
            break
        case "illuminance":
        	socketSend([request: "SetLux", idx: idx, lux:evt.stringValue])
            break
        case ["energy", "energyMeter"]:
        	def power = 0
            def energy = evt.stringValue.toDouble()*1000
            if (dev.hasAttribute("power") && dev.currentValue("power").toDouble() > 0) power = dev.currentValue("power").toDouble().round(1)
            socketSend([request: "SetPower", idx: idx, power:power.toString(), energy:energy.toInteger().toString()])
            break
        case "power":
        	def energy = 0
            if (dev.hasAttribute("energy") && dev.currentValue("energy").toDouble() > 0) energy = dev.currentValue("energy").toDouble().round(0)*1000	//Wh
            if (dev.hasAttribute("energyMeter") && dev.currentValue("energyMeter").toDouble() > 0) energy = dev.currentValue("energyMeter").toDouble()*1000	//Wh and preferred if exists
            socketSend([request: "SetPower", idx: idx, power:evt.stringValue, energy:energy.toInteger().toString()])
        	break
        default:
            break
        }
	}
}

private def getVirtualIdx(passed) {
	if (!settings?.domoticzVirtualDevices) return

    def virtual = state?.virtualDevices.find {key, item -> item.name.toUpperCase() == passed.name.toUpperCase() && item.type.toUpperCase() == passed.type.toUpperCase() }
  
    if (virtual) return virtual.key   
}
/*-----------------------------------------------------------------------------------------*/
/*		Update the usage info on composite DZ sensors that report on a utility
/*		kWh, Lux etc...
/*-----------------------------------------------------------------------------------------*/
void callbackForUCount(evt) {
    def response = getResponse(evt)   
    if (response?.result == null)  return

	response.result.each { utility ->
		// Power usage    
    	if (utility?.SubType == "kWh") {
            doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"power", value: utility.Usage.split()[0].toDouble().round(1), unit:utility.Usage.split()[1].toString()])
            doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"powerConsumption", value: JsonOutput.toJson("Total:${utility.Data} Today:${utility.CounterToday}")])          
            doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"energyMeter", value: utility.Data.split()[0].toDouble().round(1), unit:utility.Data.split()[1].toString()])          
        }
    	if (utility?.SubType == "Energy") {
            doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"power", value:utility.Usage.split()[0].toDouble().round(0), unit:utility.Usage.split()[1].toString()])
            doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"powerConsumption", value: JsonOutput.toJson("Total:${utility.Data} Today:${utility.CounterToday}")])          
            if (utility?.CounterToday) doUtilityEvent([idx: utility.idx, idxName: "idxPower", name:"energyMeter", value: utility.CounterToday.split()[0].toDouble().round(0), unit:utility.Data.split()[1].toString()])          
        }
        // Motion        
    	if (utility?.SwitchTypeVal == 8) {
            def motion = "inactive"
            if (utility.Status.toUpperCase() == "ON") motion = "active"
            
            doUtilityEvent([idx: utility.idx, idxName: "idxMotion", name:"motion", value:"${motion}"])
        }
        // Gas
    	if (utility?.SubType == "Gas") {
        	doUtilityEvent([idx: utility.idx, idxName: "idxGas", name:"gas", value:utility.CounterToday.split()[0]])
        }
        // Lux
    	if (utility?.SubType == "Lux") {
        	doUtilityEvent([idx: utility.idx, idxName: "idxIlluminance", name:"illuminance", value:utility.Data.split()[0].toInteger()])
        }
        // Sound Level
    	if (utility?.SubType == "Sound Level") {
        	doUtilityEvent([idx: utility.idx, idxName: "idxSound", name:"soundPressureLevel", value:utility.Data.split()[0].toInteger()])
        }	        
        // Custom / general
    	if (utility?.Type == "General") {
        	def dataDisplayed = "${utility.Name} :\n${utility.Data}"
        	doUtilityEvent([idx: utility.idx, idxName: "idxCustom1", name:"customStatus1", value:dataDisplayed])
        	doUtilityEvent([idx: utility.idx, idxName: "idxCustom2", name:"customStatus2", value:dataDisplayed])
        }	        
        // Air Quality
    	if (utility?.Type == "Air Quality") {
        	doUtilityEvent([idx: utility.idx, idxName: "idxAirQuality", name:"airQuality", value:utility.Data.split()[0].toInteger()])
        }	        
        // Pressure
    	if (utility?.Barometer) {
        	doUtilityEvent([idx: utility.idx, idxName: "idxPressure", name:"barometricPressure", value: utility.Barometer.toInteger()])
        }	        
        // Humidity
    	if (utility?.Humidity) {
        	doUtilityEvent([idx: utility.idx, idxName: "idxHumidity", name:"humidity", value: utility.Humidity.toInteger()])
        }		
 		// Temperature       
    	if (utility?.Temp) {
            float t = utility.Temp
            t = t.round(1)
            doUtilityEvent([idx: utility.idx, idxName: "idxTemperature", name:"temperature", value: t])
        }
 		// Battery       
    	if (utility?.BatteryLevel > 0 && utility?.BatteryLevel <= 100 ) {
        	doUtilityEvent([idx: utility.idx, idxName: "idxBattery", name:"battery", value:utility.BatteryLevel])
        }
 		// SignalStrength       
    	if (utility?.SignalLevel > 0 && utility?.SignalLevel <= 10) {
        	doUtilityEvent([idx: utility.idx, idxName: "idxSignalStrength", name:"rssi", value:utility.SignalLevel])
        }
	}    
}

private def doUtilityEvent(evt) {

	switch (evt.idxName) {
    case "KWH":
        evt.idxName = "idxPower"
        evt.name = "power"
        break
    case "Lux":
        evt.idxName = "idxIlluminance"
        evt.name = "illuminance"
        break
    case "Gas":
        evt.idxName = "idxGas"
        evt.name = "gas"
        break
    case "AirQuality":
        evt.idxName = "idxAirQuality"
        evt.name = "airQuality"
        break
    case "Sound":
        evt.idxName = "idxSound"
        evt.name = "soundPressureLevel"
        break
    case "Pressure":
        evt.idxName = "idxPressure"
        evt.name = "barometricPressure"
        break
    case "Temp":
        evt.idxName = "idxTemperature"
        evt.name = "temperature"
        break    }
    
	if (state.devices[evt.idx]?.dni) {
        def nativeDni = state.devices[evt.idx].dni
    	def nativeDev = getChildDevice(nativeDni)
    	if (nativeDev && nativeDev.hasAttribute(evt.name) == true) {
			TRACE("[doUtilityEvent] native idx found ${evt.idx} ${evt.name} ${evt.value}")
        	nativeDev.sendEvent(name: evt.name, value: evt.value)
        }
    }

    def stateDevice = state.devices.find {key, item -> 
        item."${evt.idxName}" == evt.idx
    }

    if (stateDevice) {
        stateDevice = stateDevice.toString().split("=")[0]
		def dni = state.devices[stateDevice].dni
        def dev = getChildDevice(dni)
        if (dev.hasAttribute(evt.name) == true) {        	
            dev.sendEvent(name: evt.name, value: evt.value)
        	TRACE("[doUtilityEvent] ${dev} gets ${evt.value} for ${evt.name} from idx ${evt.idx} device has attribute")
        }
        else { // parse needed in dth
            sendEvent(dev, [name: evt.name, value: evt.value])
        	TRACE("[doUtilityEvent] ${dev} gets ${evt.value} for ${evt.name} from idx ${evt.idx} device parse() will be used")
        }
    }    
}
/*-----------------------------------------------------------------------------------------*/
/*		Build the idx list for Devices that are part of the selected room plans and appState event handler test
/*-----------------------------------------------------------------------------------------*/
void eventTest(evt) {
	TRACE("EVENTTEST ${evt.name} ${evt.value}")
}

void callbackForRoom(evt) {
	def response = getResponse(evt)
	if (response?.result == null) return

    TRACE("[callbackForRoom] Domoticz response with Title : ${response.title} number of items returned ${response.result.size()}") 

    response.result.findAll{it?.SubType != "kWh"}.each { state.listOfRoomPlanDevices.add(it.devidx) }
}
/*-----------------------------------------------------------------------------------------*/
/*		Get Room Plans defined into Selectables for setupDomoticz
/*-----------------------------------------------------------------------------------------*/
void callbackForPlans(evt) {
	def response = getResponse(evt)   
    if (response?.result == null) return

    TRACE("[callbackForPlans] Domoticz response with Title : ${response.title} number of items returned ${response.result.size()}") 
    
    state.statusPlansRsp = response.result
    state.listPlans = response.result.collect{it.Name}.sort()
}
/*-----------------------------------------------------------------------------------------*/
/*		proces for adding and updating status for Scenes and Groups
/*-----------------------------------------------------------------------------------------*/
void callbackForScenesUpdate(evt) {
    def response = getResponse(evt)
	if (response?.result == null) return

    TRACE("[callbackForScenesUpdate] Domoticz response with Title : ${response.title} number of items returned ${response.result.size()}") 
    
	response?.result.findAll{(it.Type == "Scene" && domoticzScene) || (it.Type == "Group" && domoticzGroup)}.each { 
    	defineDomoticzInSmartThings([idx:"S${it.idx}", deviceType: "domoticzScene", subType: it.Type, name: it.Name, dzStatus: it, updateEvents:true])
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		proces for adding and updating status for Scenes and Groups
/*-----------------------------------------------------------------------------------------*/
void callbackForScenes(evt) {
    def response = getResponse(evt)
    def groupIdx = response?.result.collect {it.idx}.sort()
    state.statusScenes = groupIdx

	if (response?.result == null) return

    TRACE("[callbackForScenes] Domoticz response with Title : ${response.title} number of items returned ${response.result.size()}") 

	response?.result.findAll{(it.Type == "Scene" && domoticzScene) || (it.Type == "Group" && domoticzGroup)}.each {
        defineDomoticzInSmartThings([idx:"S${it.idx}", deviceType: "domoticzScene", subType: it.Type, name: it.Name, dzStatus: it, updateEvents:true])
    }
	state.sceneGroupDevices = [:]   
    //kick off the first of the callback sequence to fill a correct map with info in the right order.
    if (groupIdx.size() > 0) {
    	socketSend([request : "sceneListDevices", idx : groupIdx.get(0)])
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		Get devices that are part of a scene and store the in the listGroups
/*		so we can check later if a device notification could also trigger a scene/group switch
/*-----------------------------------------------------------------------------------------*/
void callbackSceneListDevices(evt) {
	def response = getResponse(evt)
	if (response?.result == null) return

	TRACE("[callbackSceneListDevices] Domoticz response with Title : ${response.title} number of items returned ${response.result.size()}")
    def idxGroup = (state.sceneGroupDevices.size()).toString()
	if (idxGroup.toInteger() < state.statusScenes.size()) state.sceneGroupDevices[idxGroup] = [sceneIdx: state.statusScenes.get(idxGroup.toInteger()), result: response.result]

    if (state.statusScenes.size() > state.sceneGroupDevices.size()) {
        socketSend([request : "sceneListDevices", idx : state.statusScenes.get(idxGroup.toInteger()+1)])
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		callback for adding and updating status of devices in SmartThings
/*-----------------------------------------------------------------------------------------*/
private def callbackForDevices(statusrsp) {
	if (statusrsp?.result == null) {
    	TRACE("[callbackForDevices] result == null ${statusrsp}")
    	return	
    }
    
	def compareTypeVal
    def SubType
    def dev
    def idxST = 9999999
    def updateEvents = false
      
    if (statusrsp.result.size() == 1) updateEvents = true
    
    if (state?.dzHardwareIdx) idxST = state.dzHardwareIdx.toInteger() 
    
    if (!state.virtualDevices) state.virtualDevices = [:]
    
	statusrsp.result.each { device ->
    	if (device?.Used == 1) {						// Only devices that are defined as being USED in DZ will make it as real devices
            compareTypeVal = device?.SwitchTypeVal

            // handle SwitchTypeVal Exceptions
            if (compareTypeVal == null) compareTypeVal = 100
            if (device?.Type.contains("Temp")) compareTypeVal = 99
            if (device?.Type == "Humidity") compareTypeVal = 97
            if (device?.Type == "Air Quality") compareTypeVal = 96
            if (device?.SubType == "Sound Level") compareTypeVal = 95
            if (device?.SubType == "kWh") compareTypeVal = 29
            if (device?.Type == "Lux") compareTypeVal = 94 
            if (device?.SetPoint) compareTypeVal = 98
            
            // REAL SmartThings devices that where defined in Domoticz as virtual devices
            if (settings?.domoticzVirtualDevices == true && device?.HardwareID == idxST) {
  
                switch (compareTypeVal) {
                    case 0:
                        SubType = "switch"
                        dev = settings.dzDevicesSwitches.find{it.displayName == device.Name}
                        if (device.Notifications == "false") {
                            socketSend([request : "Notification", idx : device.idx, type : 7, action : "on"])
                            socketSend([request : "Notification", idx : device.idx, type : 16, action : "off"])
                        }
                        break
                    case 19:
                        SubType = "lock"
                        dev = settings.dzDevicesLocks.find{it.displayName == device.Name}
                        if (device.Notifications == "false") {
                            socketSend([request : "Notification", idx : device.idx, type : 7, action : "on"])
                            socketSend([request : "Notification", idx : device.idx, type : 16, action : "off"])
                        }
                        break
                    case 2:
                        SubType = "contact"
                        dev = settings.dzSensorsContact.find{it.displayName == device.Name}
                        break
                    case 8:
                        SubType = "motion"
                        dev = settings.dzSensorsMotion.find{it.displayName == device.Name}
                        break
					case 29:
                        SubType = "power"
                        dev = settings.dzPowerMeters.find{it.displayName == device.Name}
                        break
                    case 97:
                        SubType = "humidity"
                        dev = settings.dzSensorsHum.find{it.displayName == device.Name}
                        break
                    case 99:
                        SubType = "temperature"
                        dev = settings.dzSensorsTemp.find{it.displayName == device.Name}
                        break
                    case 94:
                        SubType = "illuminance"
                        dev = settings.dzSensorsIll.find{it.displayName == device.Name}
                        break
                }
                
                state.virtualDevices[device.idx] = [idx: device.idx, name: device.Name, type: SubType, dni: dev?.deviceNetworkId ]
            	compareTypeVal = 100
                // seed the initial
                if (dev) handlerEvents([isStateChange: true, device: dev, name: SubType, stringValue:dev.currentValue(SubType)])
            }
            else
            {
                switch (compareTypeVal) 
                {
                    case [3, 13, 6, 16]:		//	Window Coverings, 6 & 16 are inverted
                        if (domoticzTypes.contains('Window Coverings')) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzBlinds", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                    	break
                    case [0, 7]:		// 	Lamps OnOff, Dimmers and RGB
                        SubType = device?.SubType
                        if (domoticzTypes.contains('On/Off/Dimmers/RGB') && SubType.matches("kWh|Energy") == false) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzOnOff", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case [2, 11]:				//	Contact 
                        if (domoticzTypes.contains('Contact Sensors')) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzContact", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case 5:				//	Smoke Detector
                        if (domoticzTypes.contains('Smoke Detectors')) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzSmokeDetector", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case 8:				//	Motion Sensors
                        if (domoticzTypes.contains('Motion Sensors')) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzMotion", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case 12:			//	Dusk Sensors/Switch
                        if (domoticzTypes.contains('Dusk Sensors')) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzDuskSensor", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case 18:			//	Selector Switch
                        if (domoticzTypes.contains("On/Off/Dimmers/RGB")) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzSelector", name:device.Name, subType: device.SwitchType, dzStatus: device, updateEvents: updateEvents)
                        break
                    case 98:			//	Thermostats
                        if (domoticzTypes.contains("Thermostats")) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzThermostat", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                       break
                    case 99:			//	Sensors
                        if (domoticzTypes.contains("(Virtual) Sensors")) defineDomoticzInSmartThings(idx: device.idx, deviceType:"domoticzSensor", name:device.Name, subType: device.Type, dzStatus: device, updateEvents: updateEvents)
                        break
                    case [29, 100, 94]:
                        break
                    default:
                        TRACE("[callbackForDevices] non handled SwitchTypeVal ${compareTypeVal} ${device}")
                    break
                }
			}
        } 
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		callback for updating usage counters and reporting in SmartThings
/*		it will also do linking between individual domoticz devices into a single SmartThings device
/*-----------------------------------------------------------------------------------------*/
def callbackForEveryThing(evt) {
    def response = getResponse(evt)
	if (response?.result == null) return
    
    def kwh = 0f
    def watt = 0f
    def powerUnit = "kWh"
    def powerUsage = "Watt"    
    def gasUsage = 0f
    def gasTotal = 0f
    def gasUnit = "m3"
    def stateDevice
    def ID
    def IDX
    def statePower = state?.devices["10000"]
    def stateGas = state?.devices["10001"]
    def devReportPower
    def devReportGas

    response.result.each {
		//TEMP		    	        
        if (it?.Type.contains("Temp")) {
        	state.optionsTemperature[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorTempNotification", idx : it.idx])
        }
        //MOTION
        if (it?.SwitchTypeVal == 8) {
        	state.optionsMotion[it.idx] = "${it.idx} : ${it.Name}"
        }	
		//MODES for thermostatFanModes and thermostatModes
		if (it?.SwitchTypeVal == 18) {
        	state.optionsModes[it.idx] = "${it.idx} : ${it.Name}"     
            }	
        //SMOKE       
        if (it?.SwitchTypeVal == 5) {
        	state.optionsCarbon1[it.idx] = "${it.idx} : ${it.Name}"
        	state.optionsCarbon2[it.idx] = "${it.idx} : ${it.Name}"
        }
        //LUX
        if (it?.Type == "Lux") {
        	state.optionsLux[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorLuxNotification", idx : it.idx])
        }
        //THERMOSTAT
        if (it?.Type == "Thermostat") {
            if (it.Notifications == "false") socketSend([request : "Notification", idx : it.idx, type:0, action:"%24value"])
        }
        //HUMIDITY
        if (it?.Humidity) {
        	state.optionsHumidity[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorHumidityNotification", idx : it.idx])
        }
        //SOUND
        if (it?.SubType == "Sound Level") {
        	state.optionsSound[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorSoundNotification", idx : it.idx])
        }
        //CUSTOM
        if (it?.Type == "General") {
        	state.optionsCustom[it.idx] = "${it.idx} : ${it.Name}"
            def dataDisplayed = "${it.Name} :\n${it.Data}"
        	doUtilityEvent([idx: it.idx, idxName: "idxCustom1", name:"customStatus1", value:dataDisplayed])
        	doUtilityEvent([idx: it.idx, idxName: "idxCustom2", name:"customStatus2", value:dataDisplayed])
        }
        //AIR QUAILTY
        if (it?.Type == "Air Quality") {
        	state.optionsAirQuality[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorAirQualityNotification", idx : it.idx])
        }
        //PRESSURE
        if (it?.Barometer) {
        	state.optionsPressure[it.idx] = "${it.idx} : ${it.Name}"
            if (it.Notifications == "false") socketSend([request : "SensorPressureNotification", idx : it.idx])
        } 
        //USAGE ENERGY (P1 meters)
        if (it?.Type == "P1 Smart Meter" && it?.SubType == "Energy") {
        	state.optionsPower[it.idx] = "${it.idx} : ${it.Name}"
        	if (it.Notifications == "false") socketSend([request : "SensorKWHNotification", idx : it.idx])	
    		if (settings.domoticzReportPower && statePower?.idxPower == it.idx) {
                devReportPower = getChildDevice(state.devReportPower)
                if (devReportPower) {
                	powerUnit = it.CounterToday.split()[1]
                	powerUsage = it.Usage.split()[1]
                    def consumptionLow = it.Data.split(";")[0].toInteger()/1000
                    devReportPower.sendEvent(name:"consumptionLow", value: "${consumptionLow.toInteger()}", unit:"kWh")
                    def consumptionHigh = it.Data.split(";")[1].toInteger()/1000
                    devReportPower.sendEvent(name:"consumptionHigh", value: "${consumptionHigh.toInteger()}", unit:"kWh")
                    def productionLow = it.Data.split(";")[2].toInteger()/1000
                    devReportPower.sendEvent(name:"productionLow", value: "${productionLow.toInteger()}", unit:"kWh")
                    def productionHigh = it.Data.split(";")[3].toInteger()/1000
                    devReportPower.sendEvent(name:"productionHigh", value: "${productionHigh.toInteger()}", unit:"kWh")
                    def momentaryUsage = it.Data.split(";")[4]
                    devReportPower.sendEvent(name:"momentaryUsage", value: "${momentaryUsage.toInteger()}", unit:"W")
                    def momentaryProduction = it.Data.split(";")[5]                    
                    devReportPower.sendEvent(name:"momentaryProduction", value: "${momentaryProduction.toInteger()}", unit:"W")

					devReportPower.sendEvent(name:"energyMeter", value: "${it.Counter.toDouble().round(0)}", unit:powerUnit)
                    devReportPower.sendEvent(name:"power", value: it.Usage.split()[0].toInteger(), unit:powerUsage)
				}            	
            }
        }
        //USAGE POWER
        if (it?.SubType == "kWh") {	
        	state.optionsPower[it.idx] = "${it.idx} : ${it.Name}"             
            if (it.Notifications == "false") socketSend([request : "SensorKWHNotification", idx : it.idx])   
            kwh = kwh + Float.parseFloat(it.Data.split()[0])
            watt = watt + Float.parseFloat(it.Usage.split()[0])
            
    		if (settings.domoticzReportPower && statePower?.idxPower == it.idx) {
                devReportPower = getChildDevice(state.devReportPower)
                if (devReportPower) {
                    powerUnit = it.Data.split()[1]
            		powerUsage = it.Usage.split()[1]
                	def k = it.Data.split()[0].toDouble().round(0)
                    def w = it.Usage.split()[0].toDouble().round(0)
                    devReportPower.sendEvent(name:"energyMeter", value: "${k}", unit:powerUnit)
                    devReportPower.sendEvent(name:"power", value: w, unit:powerUsage)
                    devReportPower.sendEvent(name:"powerConsumption", value: JsonOutput.toJson("Total:${it.Data} Today:${it.CounterToday}"))
                }            	
            }
			//add idxPower to real device by matching the ID
			ID = it?.ID
			stateDevice = state.devices.find {key, item -> 
		    	item.deviceId == ID
    		}
            
            if (!stateDevice) { // XIAOMI try
            	ID = "${it?.ID.substring(6)}${it?.ID.substring(0,6)}"                
                stateDevice = state.devices.find {key, item -> 
                    item.deviceId == ID
                }
            }
            
            IDX = it.idx
            if (stateDevice) {
            	if (state.devices[stateDevice.key]?.idxPower != IDX) {
            		state.devices[stateDevice.key].idxPower = IDX
                }
            }
		}
		//USAGE GAS
        if (it?.SubType == "Gas") {	
        	state.optionsGas[it.idx] = "${it.idx} : ${it.Name}"       
			if (it.Notifications == "false") socketSend([request : "SensorGasNotification", idx : it.idx])
            gasTotal = gasTotal + Float.parseFloat(it.Counter)
            gasUsage = gasUsage + Float.parseFloat(it.CounterToday.split()[0])

			if (settings.domoticzReportPower && stateGas?.idxGas == it.idx) {
                gasUnit = it.CounterToday.split()[1]
            	devReportGas = getChildDevice(state.devReportGas)
                if (devReportGas) {
                    devReportGas.sendEvent(name:"energyMeter", value: "${it.Counter.toDouble().round(3)}", unit:gasUnit)
                    def m = devReportGas?.currentValue("power")
                    if (!m) m = 0
                    m = m.toDouble()
                    m = it.CounterToday.split()[0].toDouble() - m
                    devReportGas.sendEvent(name:"power", value: m.round(3), unit:gasUnit)
                    devReportGas.sendEvent(name:"powerConsumption", value: JsonOutput.toJson("Total:${it.Data} Today:${it.CounterToday}"))

                }            	
            }
		}
	}

	if (settings.domoticzReportPower) {
        // pass to Devices that report Usage totals but only if report device does not contain idxPower or idxGas key
        if (statePower?.containsKey("idxPower") == false) { 
            if (kwh > 0 && state.devReportPower != null) {
                devReportPower = getChildDevice(state.devReportPower)
                if (devReportPower) {
                    devReportPower.sendEvent(name:"energyMeter", value: "${kwh.round(0)}", unit:powerUnit)
                    devReportPower.sendEvent(name:"power", value: watt.round(0), unit:powerUsage)
                    devReportPower.sendEvent(name:"powerConsumption", value: JsonOutput.toJson("no data"))
                }
            }
        }

		if (stateGas?.containsKey("idxGas") == false) { 
            if (gasUsage > 0 && state.devReportGas != null) {
                devReportGas = getChildDevice(state.devReportGas)
                if (devReportGas) {
                    devReportGas.sendEvent(name:"energyMeter", value: "${gasTotal.round(3)}", unit:gasUnit)
                    def m = devReportGas?.currentValue("power").toDouble()
                    m = gasUsage.toDouble() - m
                    devReportGas.sendEvent(name:"power", value: m.round(3), unit:gasUnit)
                    devReportGas.sendEvent(name:"powerConsumption", value: JsonOutput.toJson("no data"))
                }
            }
        }
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		callback for getting all devices 
/*-----------------------------------------------------------------------------------------*/
def handleList(evt) {
    def response = getResponse(evt) 
    if (response?.result == null) return
    
	def nextIdxList = response.result.findAll{it.Type.matches("Scene|Group") == false}.collect {it.idx}.sort()
	def nextSceneList = response.result.findAll{it.Type.matches("Scene|Group") == true}.collect {"S${it.idx}"}.sort()
    def currentIdxList
    def currentSceneList
    
    if (state.statusrsp) currentIdxList = state.statusrsp
    else currentIdxList = nextIdxList

	if (state.statusGrpRsp) {
    	currentSceneList = state.statusGrpRsp
        handleSceneTransformation(currentSceneList)
    }
    else currentSceneList = nextSceneList
    
        
    def dzAddedList = nextIdxList.minus(currentIdxList)  
    def dzDeletedList = currentIdxList.minus(nextIdxList)
    TRACE("[handleList] Deleted devices, remove child devices ${dzDeletedList}")
    
    def sceneAddedList = nextSceneList.minus(currentSceneList)
    def sceneDeletedList = currentSceneList.minus(nextSceneList)
    TRACE("[handleList] Deleted scenes, remove child devices ${sceneDeletedList}")
    
	if (state.devices) {
		def stateIdxList = state.devices.collect {it.key}.sort()
        def stateToDeleteList = stateIdxList.minus(nextIdxList)
        stateToDeleteList = stateToDeleteList.minus(nextSceneList)
        stateToDeleteList = stateToDeleteList.minus(["10000", "10001"])

        dzDeletedList.plus(sceneDeletedList).each { idx ->
            try {
                deleteChildDevice(app.id + ":IDX:" + idx)
                TRACE("[handleList] deleted child ${app.id + ":IDX:" + idx}")
            }
            catch (e) {
                log.warn "[handleList] error deleting child ${app.id + ":IDX:" + idx}"
            }
        }
             
    	TRACE("[handleList] Delete from State devices ${stateToDeleteList}")  
        stateToDeleteList.each {state.devices.remove(it)}
		stateToDeleteList = []
        
        def childIdxList = getChildDevices().collect { it.deviceNetworkId.tokenize(":")[2]}.findAll{it != null}.sort()
        //no child exists for dni in state...remove
        state.devices.each { key, item ->
        	if (item.containsKey("dni") && !getChildDevice(item.dni)) stateToDeleteList.add(key)
        }

        stateToDeleteList.each {state.devices.remove(it)}
	}
    
	state.listInprogress = true
    pause 2
    callbackForDevices(response)    
    state.statusrsp = nextIdxList
    state.statusGrpRsp = nextSceneList
    state.listInprogress = false
}
/*-----------------------------------------------------------------------------------------*/
/*		transform old style scene IDX to NEW Style (add and S in front of the IDX)
/*-----------------------------------------------------------------------------------------*/
private def handleSceneTransformation(sceneList) {   
	def scene
    def newIdx
    
	sceneList.each{ sceneIdx ->
		
    	if (sceneIdx.contains("S") == false) {   
        	
        	newIdx = "S" + sceneIdx
        	scene = getChildDevice("${app.id}:IDX:${sceneIdx}")
            
            if (scene) {
            	scene.deviceNetworkId = "${app.id}:IDX:${newIdx}"
                TRACE("[handleSceneTransformation] existing device for old scene device ${sceneIdx} transformed!")
                
                if (state?.devices[sceneIdx].deviceType == "domoticzScene") {
					state.devices[newIdx] = state.devices[sceneIdx]
                    state.devices[newIdx].dni = scene.deviceNetworkId
                    state.devices[newIdx].idx = newIdx
                    state.devices.remove(sceneIdx)
					TRACE("[handleSceneTransformation] state for old type scene device ${sceneIdx} transformed!")                	
                }
                
                else if (state?.devices[newIdx].deviceType == "domoticzScene") {
                    state.devices[newIdx].dni = scene.deviceNetworkId
                    state.devices[newIdx].idx = newIdx
                	log.warn "[handleSceneTransformation] existing state for new scene device ${newIdx}"
                }
            }
            else log.warn "[handleSceneTransformation] non existing scene device ${sceneIdx}"
        }
    }
}
/*-----------------------------------------------------------------------------------------*/
/*		callback for getting single device status
/*-----------------------------------------------------------------------------------------*/
def callbackStatus(evt) {
	if (state.listInprogress) return
    def response = getResponse(evt)
    callbackForDevices(response)    
}

/*-----------------------------------------------------------------------------------------*/
/*		dummy callback handlers for speed, when an error occurs it will show handler name
/*-----------------------------------------------------------------------------------------*/
def callbackLog(evt) {return getResponse(evt)}
def callbackKWHNotification(evt) {return getResponse(evt)}
def callbackLuxNotification(evt) {return getResponse(evt)}
def callbackHumidityNotification(evt) {return getResponse(evt)}
def callbackAirQualityNotification(evt) {return getResponse(evt)}
def callbackPressureNotification(evt) {return getResponse(evt)}
def callbackSoundNotification(evt) {return getResponse(evt)}
def callbackNotification(evt) {return getResponse(evt)}
def callbackSensorGasNotification(evt) {return getResponse(evt)}
def callbackSensorTempNotification(evt) {return getResponse(evt)}
def callbackClearNotification(evt) {return getResponse(evt)}

/*-----------------------------------------------------------------------------------------*/
/*		Capture the created hardware IDX for SmartThings
/*-----------------------------------------------------------------------------------------*/
def callbackCreateHardware(evt) {
	socketSend([request:"UpdateHardware"])	
}
def callbackUpdateHardware(evt) {
	socketSend([request:"ListHardware"])
}
def callbackListHardware(evt) {
    def response = getResponse(evt)    
    if (response?.result == null) return
    
    state.dzHardwareIdx			= null
	response.result.findAll{it.Name == "SmartThings"}.each { hardware ->
        state.dzHardwareIdx = hardware.idx
        TRACE("[callbackListHardware] SmartThings Hardware id in Domoticz is ${state.dzHardwareIdx}")
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		callback for getting the Domoticz Settings
/*-----------------------------------------------------------------------------------------*/
def callbackForSettings(evt) {
    def response = getResponse(evt)
    
	if (response?.HTTPURL == null) return
    
    def decoded = response.HTTPURL.decodeBase64()
    def httpURL = new String(decoded)
	state.validUrl = false
	if (httpURL != state.urlCustomActionHttp) {
        def postBody = getSettingsForm(response)      	
        def hubAction = new physicalgraph.device.HubAction(
                            method: "POST",
                            path: "/storesettings",
                            requestContentType: "application/x-www-form-urlencoded",
                            headers: [HOST: "${state.networkId}"],
                            null,
                            body: postBody,
                            [callback: callbackPostSettings] )
        sendHubCommand(hubAction) 
    }
    else state.validUrl = true
}

def callbackPostSettings(evt) {
    if (evt?.status != 200) log.error "[callbackPostSettings]" + evt?.status.toString()
    else {
    	TRACE("[callbackPostSettings] Post for storesettings succeeded")
        state.validUrl = true
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		callback after creation of the virtual sensor/device it will provide the new IDX
/*		call for the provided device, this will create the link between IDX and NAME
/*		in state.virtualDevices
/*-----------------------------------------------------------------------------------------*/
def callbackVirtualDevices(evt) {
    def response = getResponse(evt)

	response?.result.each { hardware ->
    	socketSend([request:"status", idx:hardware.idx])
    }  
}

/*-----------------------------------------------------------------------------------------*/
/*		Execute the real add or status update of the child device
/*-----------------------------------------------------------------------------------------*/
private def defineDomoticzInSmartThings(request) {
	//TRACE("[defineDomoticzInSmartThings] ${request}")
    def dni = app.id + ":IDX:" + request.idx
	def switchTypeVal = ""
    def mainType = ""
    def deviceId = ""
    def dev = getChildDevice(dni)
    def vid = null
    def ocfdevicetype = null

 	if (request.dzStatus instanceof java.util.Map) {
    	 
    	if (request.dzStatus?.ID != null) {
        	deviceId = request.dzStatus.ID
        }
        
    	if (request.dzStatus?.SwitchTypeVal != null) {
        	switchTypeVal = request.dzStatus.SwitchTypeVal
            mainType = "switch"
        }
        else mainType = "sensor"
        
		// offline/not accessible in DZ???
        if (request.dzStatus?.HaveTimeout != true) {
            devOnline(dev)
        }
        else {
            TRACE("[defineDomoticzInSmartThings] Device ${request.name} offline")
            devOffline(dev)
        }       
    }
    
    if (dev) {      
        //TRACE("[addSwitch] Updating child device ${request.idx}, ${request.deviceType}, ${request.name}, ${request.dzStatus}")        
 		if (!state.devices[request.idx]) {       
            state.devices[request.idx] = [
                    'dni' : dni,
                    'ip' : settings.domoticzIpAddress,
                    'port' : settings.domoticzTcpPort,
                    'idx' : request.idx,
                    'type'  : mainType,
                    'deviceType' : request.deviceType,
                    'subType' : request.subType,
                    'deviceId' : deviceId,
                    'switchTypeVal' : switchTypeVal,
                    'name' : request.name
                    ]
		}
        else if (state.devices[request.idx].containsKey("name") == false) {
        	state.devices[request.idx].name = request.name
        }
                        
        if (request.name != dev.name) {
        	dev.label = request.name
            dev.name = request.name
            state.devices[request.idx].name = request.name
        }
       
        // add base device Signal Strength and Battery components if applicable
        if (request.dzStatus instanceof java.util.Map && dev.hasCommand("configure")) {
        	if (request.dzStatus?.Name.toUpperCase().contains("MOOD") && request.dzStatus?.SubType == "LightwaveRF") {
                dev.configure("Group Off")
                dev.configure("Group Mood 1")
                dev.configure("Group Mood 2")
                dev.configure("Group Mood 3")
                dev.configure("Group Mood 4")
                dev.configure("Group Mood 5")
            }
        	if (request.dzStatus?.BatteryLevel > 0 && request.dzStatus?.BatteryLevel <= 100) {
            	if (!state.devices[request.idx].idxBattery) {
                    dev.configure("Battery")
                    state.devices[request.idx].idxBattery = request.idx
                }
            }
        	if (request.dzStatus?.SignalLevel > 0 && request.dzStatus?.SignalLevel <= 10) {
            	if (!state.devices[request.idx].idxSignalStrength) {
                    dev.configure("Signal Strength")
                    state.devices[request.idx].idxSignalStrength = request.idx
                }
            }
        }
    }
    else if ((state.listOfRoomPlanDevices?.contains(request.idx) && settings.domoticzRoomPlans == true) || settings.domoticzRoomPlans == false) {
        
        try {
            TRACE("[defineDomoticzInSmartThings] Creating child device ${request.idx}, ${request.deviceType}, ${request.name}, ${request.dzStatus}")
            if (!vid) dev = addChildDevice("verbem", request.deviceType, dni, getHubID(), [name:request.name, label:request.name, completedSetup: true])
            else dev = addChildDevice("verbem", request.deviceType, dni, getHubID(), [name:request.name, label:request.name, completedSetup: true, vid: vid, ocfdevicetype: ocfdevicetype ])
            
            state.devices[request.idx] = [
                'dni'   : dni,
                'ip' : settings.domoticzIpAddress,
                'port' : settings.domoticzTcpPort,
                'idx' : request.idx,
                'type'  : mainType,
                'deviceType' : request.deviceType,
                'subType' : request.subType,
                'deviceId' : deviceId,
                'switchTypeVal' : switchTypeVal,
                'name' : request.name
            	]
			//pause 5
        } 
        catch (e) { 
            log.error "[defineDomoticzInSmartThings] Cannot create child device. ${devParam} Error: ${e}" 
        }
    }  	else return
	
    if (request.dzStatus instanceof java.util.Map) { 
    	if (request.updateEvents == true) { 
        	def attributeList = createAttributes(dev, request.dzStatus, request.idx)
        	generateEvent(dev, attributeList)
        }
        
		if (request.dzStatus?.Notifications == "false") {
            if (mainType == "switch" && request.deviceType != "domoticzSelector") {
                socketSend([request : "Notification", idx : request.idx, type : 7, action : "on"])
                socketSend([request : "Notification", idx : request.idx, type : 16, action : "off"])
            }
            if (request.deviceType == "domoticzThermostat") {
                socketSend([request : "Notification", idx : request.idx, type : 0, action : "%24value"])
            }
            if (request.deviceType == "domoticzSelector") {
                socketSend([request : "ClearNotification", idx : request.idx])

                socketSend([request : "Notification", idx : request.idx, type : 16, action : "off"])
                def decoded = request.dzStatus?.LevelNames.decodeBase64()
                def decodedString = new String(decoded)
                def levelNames = decodedString.tokenize('|')
                def ix = 10
                def maxIx = levelNames.size() * 10
                for (ix=10; ix < maxIx; ix = ix+10) {
                    socketSend([request : "Notification", idx : request.idx, type : 7, action : "on", value: ix])
                }
            }
        }
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		generate the sendEvent that will be send to devices
/*-----------------------------------------------------------------------------------------*/
private def generateEvent (dev, Map attributeList) {

	attributeList.each { name, value ->
    	def v = value
    	if (name.toUpperCase() == "SWITCH") {
        	if (v instanceof String) {
                if (v.toUpperCase() == "OFF" ) v = "off"
                if (v.toUpperCase() == "ON") v = "on"
            }
        }

		if (name.toUpperCase() == "MOTION") { if (value.toUpperCase() == "ON") v = "active" else v = "inactive"}

    	if (name.toUpperCase() == "SMOKE") { 
        	if (value.toUpperCase() == "ON") v = "smoke"
        	if (value.toUpperCase() == "OFF") v = "clear"
        }
		try {
            if (dev.hasAttribute(name)) dev.sendEvent(name:"${name}", value:"${v}")
            else sendEvent(dev,[name:"${name}", value:"${v}"])  // parse will be triggered in DTH
        }
        catch (MissingMethodException e) {
        	log.error "Catch in GenerateEvent $name $v ${e}"
        }
    }        
}

/*-----------------------------------------------------------------------------------------*/
/*		Create a status-attribute list that will be passed to sendEvent handler method
/*-----------------------------------------------------------------------------------------*/
private def createAttributes(domoticzDevice, domoticzStatus, idx) {

	if (domoticzStatus instanceof java.util.Map == false) {
       	TRACE("[createAttributes] ${domoticzDevice} ${domoticzDevice.getSupportedAttributes()} NOT PASSED A MAP : RETURNING")
        return [:]
        }
              
    def attributeList = [:]
    domoticzStatus.each { k, v ->
    	switch (k)
        {
        	case "BatteryLevel":
            	if (domoticzDevice.hasAttribute("battery")) if (v > 0 && v <= 100) attributeList.put('battery',v)
            	break;
            case "Level":
            	if (domoticzDevice.hasAttribute("level")) attributeList.put('level', v)
                
                if (domoticzStatus?.LevelInt != v && state.devices[idx]?.MaxDimLevel == null) {
					state.devices[idx].MaxDimLevel = domoticzStatus.MaxDimLevel                   
                }    
                	
                if (domoticzStatus?.LevelNames) {
                	def ix = v / 10
                    def decoded = domoticzStatus?.LevelNames.decodeBase64()
                    def decodedString = new String(decoded)
                    def status = decodedString.tokenize('|')
                	attributeList.put('selectorState', status[ix.toInteger()])
                    attributeList.put('selector', decodedString)
                    //check for associated thermostats
                    domoticz_modeChange(idx, "Mode", status[ix.toInteger()])
                    domoticz_modeChange(idx, "FanMode", status[ix.toInteger()])
                }
            	break;
            case "Temp":
            	double vd = v               
				if (domoticzDevice.hasAttribute("temperature")) attributeList.put('temperature', vd.round(1))
            	break;
            case "SetPoint":
            	if (domoticzDevice.hasAttribute("thermostatSetpoint")) 	attributeList.put("thermostatSetpoint", v)
				if (domoticzDevice.hasAttribute("coolingSetpoint"))		attributeList.put("coolingSetpoint", v)
                if (domoticzDevice.hasAttribute("heatingSetpoint"))    	attributeList.put("heatingSetpoint", v)
                break
            case "Barometer":
				if (domoticzDevice.hasAttribute("barometricPressure")) attributeList.put('barometricPressure', v)
            	break;
            case "Humidity":
				if (domoticzDevice.hasAttribute("humidity")) attributeList.put('humidity', v)
            	break;
            case "SignalLevel":
				if (domoticzDevice.hasAttribute("rssi")) if (v > 0 && v <= 10) attributeList.put('rssi', v)
            	break;
            case "Status":
            	if (domoticzDevice.hasAttribute("motion")) attributeList.put('motion', v)
            	if (domoticzDevice.hasAttribute("contact")) attributeList.put('contact', v)
            	if (domoticzDevice.hasAttribute("smoke")) attributeList.put('smoke', v)
            	if (domoticzDevice.hasAttribute("windowShade")) { 
                	if (v == "Stopped") v = "partially open"
                	attributeList.put('windowShade', v.toLowerCase())
                }
            	if (domoticzDevice.hasAttribute("switch")) {
                	if (v.contains("Level")) attributeList.put('switch', 'On') 
                    else if (v.startsWith("Group")) attributeList.put('button', v)  
                    else attributeList.put('switch', v)
                }
            	break;
            case "Type":
				if (v == "RFY") domoticzDevice.configure(setState : [name : "somfySupported", value : true])
            	break;
       }    
    }
	return attributeList
}

/*-----------------------------------------------------------------------------------------*/
/*		send ThermostatModes that are supported to the thermostat device
/*		the modes are being retrieved from a multiselector switch that is related to
/*		the thermostat in the smartapp (this is done with composite devices)
/*-----------------------------------------------------------------------------------------*/
def sendThermostatModes() {
	def thermoDev
    def selectorDev
    def idxMode
    def tModes

	idxComponentDevices([type : "Mode"]).each { key, device ->
    	thermoDev = getChildDevice(device.dni)
        idxMode = device.idxMode
        
        if (idxMode) {
        	selectorDev = getChildDevice("${app.id}:IDX:${idxMode}")
            if (selectorDev) {
            	tModes = selectorDev.currentValue("selector").tokenize("|")
                thermoDev.sendEvent(name : "supportedThermostatModes", value : JsonOutput.toJson(tModes)) 
            }
            else {
            	log.warn "mode device not found ${app.id}:IDX:${idxMode}"
            }
        }
	}
}

/*-----------------------------------------------------------------------------------------*/
/*		adding reporting devices for usage
/*-----------------------------------------------------------------------------------------*/
private def addReportDevices() {
	def passedName
    def newdni
    def dev
  
	if (settings.domoticzReportPower) {

            passedName = "Power Reporting Device"
            newdni = app.id + ":Power Reporting Device:" + "10000"
            dev = getChildDevice(newdni)
            
            if (!dev) {
                dev = addChildDevice("verbem", "domoticzPowerReport", newdni, getHubID(), [name:passedName, label:passedName, completedSetup:true])
            }
            state.devReportPower = newdni
            
            state.devices["10000"] = [
                'dni'   : newdni,
                'idx' : "10000",
                'type'  : "Sensor",
                'deviceType' : "domoticzPowerReport",
                'subType' : "Energy",
				'switchTypeVal' : "0",
                'name' : passedName
            	]   
			passedName = "Gas Reporting Device"
            newdni = app.id + ":Gas Reporting Device:" + "10001"
            dev = getChildDevice(newdni)
            
            if (!dev) {      
                dev = addChildDevice("verbem", "domoticzPowerReport", newdni, getHubID(), [name:passedName, label:passedName, completedSetup:true])
            } 
            state.devReportGas = newdni
            state.devices["10001"] = [
                'dni'   : newdni,
                'idx' : "10001",
                'type'  : "Sensor",
                'deviceType' : "domoticzPowerReport",
                'subType' : "Gas",
                'switchTypeVal' : "0",
                'name' : passedName
            	]
    }
    else {
        if (state?.devReportPower) {
        	dev = getChildDevice(state.devReportPower)
        	if (dev) {
            	deleteChildDevice(state.devReportPower)
            	state.remove("devReportPower")
                state.devices.remove("10000")
            }
        }
		if (state?.devReportGas) {
        	dev = getChildDevice(state.devReportGas)
            if (dev) {
        		deleteChildDevice(state.devReportGas)
            	state.remove("devReportGas")
                state.devices.remove("10001")
            }
        }
    }
}
private def getDeviceListAsText(deviceType) {
    String s = ""
    
    state.devices.findAll{it.value.deviceType == deviceType}.sort().each { k,v ->
        def dev = getChildDevice(v.dni)           
        if (!dev) TRACE("[getDeviceListAsText] ${v.dni} NOT FOUND")
        s += "${k.padLeft(4)} - ${dev?.displayName} \n"       	
    }    
    return s
} 

private def TRACE(message) {
    if(domoticzTrace) {log.trace message}
}

/*-----------------------------------------------------------------------------------------*/
/*		REGULAR DOMOTICZ COMMAND HANDLERS FOR THE DEVICES
/*-----------------------------------------------------------------------------------------*/
def domoticz_selector_reset_notification(nid, levels) {
    socketSend([request : "ClearNotification", idx : nid])
    socketSend([request : "Notification", idx : nid, type : 16, action : "off"])
    def levelNames = levels.tokenize('|')
    def ix = 10
    def maxIx = levelNames.size() * 10
    for (ix=10; ix < maxIx; ix = ix+10) {
        socketSend([request : "Notification", idx : nid, type : 7, action : "on", value: ix])
    }
}

def domoticz_mood(nid, mood) {
    socketSend([request : mood, idx : nid])
}

def domoticz_poll(nid) {
	if (state.devices[nid] != "sensor") 
        socketSend([request : "status", idx : nid])
    else
        socketSend([request : "utilityCount", idx : nid])
        
    // also put out poll requests for composite parts of a device e.g. idxPower=idx, idxIlluminance
    state.devices[nid].each { name, value ->
    	if (name.startsWith("idx") && name.length() > 3 && value != nid) {
        	socketSend([request : "utilityCount", idx : value])
        }
    }
}

def domoticz_scenepoll(nid) {
	socketSend([request : "scenes", idx : nid - "S"])
}

def domoticz_off(nid) {
	socketSend([request : "off", idx : nid])
}

def domoticz_on(nid) {
    socketSend([request : "on", idx : nid])
}

def domoticz_sceneoff(nid) {
    // find out if it is a scene or a group, scenes do only ON commands
    if (state.devices[nid].subType == "Scene") {
		socketSend([request : "sceneon", idx : nid - "S"])
    }
    else {
		socketSend([request : "sceneoff", idx : nid - "S"])
    }
}

def domoticz_sceneon(nid) { 
	socketSend([request : "sceneon", idx : nid - "S"])
}

def domoticz_stop(nid) {
    socketSend([request : "stop", idx : nid])
}

def domoticz_setlevel(nid, xLevel) {
    if (xLevel.toInteger() == 0) {
        socketSend([request : "setlevel", idx : nid, level : xLevel])
    }    
    else {
        if (state.devices[nid].subType == "RFY") {
            socketSend([request : "stop", idx : nid])
        } 
        else {
            if (state.devices[nid]?.MaxDimLevel != null) {
            	xLevel = xLevel/100*state.devices[nid].MaxDimLevel
                xLevel = xLevel.toInteger() + 1i
            }
            socketSend([request : "setlevel", idx : nid, level : xLevel])
        }
	}
}

def domoticz_setcolor(nid, xHex, xSat, xBri) {
    socketSend([request : "setcolor", idx : nid, hex : xHex, saturation : xSat, brightness : xBri])
    socketSend([request : "on", idx : nid])
}

def domoticz_setcolorHue(nid, xHex, xSat, xBri) {
    socketSend([request : "setcolorhue", idx : nid, hue : xHex, saturation : xSat, brightness : xBri])
    socketSend([request : "on", idx : nid])
}

def domoticz_setcolorWhite(nid, xHex, xSat, xBri) {
    socketSend([request : "setcolorwhite", idx : nid, hex : xHex, saturation : xSat, brightness : xBri])
    socketSend([request : "on", idx : nid])
}

def domoticz_setpoint(nid, setpoint) {
	socketSend([request : "SetPoint", idx : nid, setpoint : setpoint])
}

def domoticz_modeChange(nid, modeType, nameLevel) {

    idxComponentDevices([type: modeType, idx: nid]).each { key, device ->
        def thermostatDev = getChildDevice(device.dni)

		if (thermostatDev != null) {
            if (modeType == "Mode") thermostatDev.setThermostatMode(nameLevel.toLowerCase())
            if (modeType == "FanMode") thermostatDev.setThermostatFanMode(nameLevel.toLowerCase())
        }
        else TRACE("[domoticz_modeChange] Thermostat association not found $nid $modeType $nameLevel")
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		Excecute the real request via the local HUB
/*-----------------------------------------------------------------------------------------*/
private def socketSend(passed) {
	//TRACE("[socketSend] entered with ${passed}")
	def hubPath = null
	def hubAction = null
    def hubCallback = [callback: callbackLog]
   
    switch (passed.request) {
		case "status":
			hubPath = "/json.htm?type=devices&rid=${passed.idx}"
            hubCallback = [callback: callbackStatus]
			break;
		case "utilityCount":
        	hubPath = "/json.htm?type=devices&rid=${passed.idx}"
            hubCallback = [callback: callbackForUCount] 
 			break;
		case "OptionTemperature":
        	hubPath = "/json.htm?type=devices&filter=temp"
            hubCallback = [callback: callbackForEveryThing]
            break;
		case "OptionUtility":
        	hubPath = "/json.htm?type=devices&filter=utility"
            hubCallback = [callback: callbackForEveryThing]
            break;
		case "OptionDevices":
        	hubPath = "/json.htm?type=devices&filter=all&used=true&order=Name"
            hubCallback = [callback: callbackForEveryThing]
            break;
        case "List":
        	hubPath = "/json.htm?type=devices&filter=all&used=true&order=Name"   // ALL USED Devices
            hubCallback = [callback: handleList]
			break;
		case "scenes":
        	hubPath = "/json.htm?type=scenes"
            hubCallback = [callback: callbackForScenes]
 			break;
        case "scenesUpdate":
        	hubPath = "/json.htm?type=scenes"
            hubCallback = [callback: callbackForScenesUpdate]
 			break;
		case "roomplans":
        	hubPath = "/json.htm?type=plans&order=name&used=true"
            hubCallback = [callback: callbackForPlans]
 			break;
		case "roomplan":
        	hubPath = "/json.htm?type=command&param=getplandevices&idx=${passed.idx}"
            hubCallback = [callback: callbackForRoom]
 			break;
        case "sceneon":
        	hubPath = "/json.htm?type=command&param=switchscene&idx=${passed.idx}&switchcmd=On"
            break;
        case "sceneoff":
        	hubPath = "/json.htm?type=command&param=switchscene&idx=${passed.idx}&switchcmd=Off"
            break;            
        case "sceneListDevices":
        	hubPath = "/json.htm?type=command&param=getscenedevices&idx=${passed.idx}&isscene=true"
            hubCallback = [callback: callbackSceneListDevices]
            break;            
		case "alive":
			hubPath = "/json.htm?type=devices&rid=0"
            hubCallback = [callback: aliveResponse]
			break;
        case ["off","on","stop", "toggle", "Group Off", "Group Mood 1", "Group Mood 2", "Group Mood 3", "Group Mood 4", "Group Mood 5"]:
        	passed.request = passed.request.capitalize().replaceAll(" ","%20")
        	hubPath = "/json.htm?type=command&param=switchlight&idx=${passed.idx}&switchcmd=${passed.request}"
            break;
        case "setlevel":
        	hubPath = "/json.htm?type=command&param=switchlight&idx=${passed.idx}&switchcmd=Set%20Level&level=${passed.level}"
            break;
        case "setcolor":
        	hubPath = "/json.htm?type=command&param=setcolbrightnessvalue&idx=${passed.idx}&hex=${passed.hex}&iswhite=false&brightness=${passed.brightness}&saturation=${passed.saturation}"
            break;
        case "setcolorhue":
        	hubPath = "/json.htm?type=command&param=setcolbrightnessvalue&idx=${passed.idx}&hue=${passed.hue}&iswhite=false&brightness=${passed.brightness}&saturation=${passed.saturation}"
            break;
         case "setcolorwhite":
        	hubPath = "/json.htm?type=command&param=setcolbrightnessvalue&idx=${passed.idx}&hex=${passed.hex}&iswhite=true&brightness=${passed.brightness}&saturation=${passed.saturation}"
            break;
         case "SetPoint":  
         	hubPath = "/json.htm?type=setused&idx=${passed.idx}&setpoint=${passed.setpoint}&mode=ManualOverride&until=&used=true"
            break;
         case "SetContact":
         	def OnOff = "On"
         	if (passed.nvalue == 0) OnOff = "Off" 

         	hubPath = "/json.htm?type=command&param=switchlight&idx=${passed.idx}&switchcmd=${OnOff}"
         	//hubPath = "/json.htm?type=command&param=udevice&idx=${passed.idx}&nvalue=${passed.nvalue}"
            break;
         case "SetLux":  
         	hubPath = "/json.htm?type=command&param=udevice&idx=${passed.idx}&svalue=${passed.lux}"
            break;
        case "SetTemp":  
         	hubPath = "/json.htm?type=command&param=udevice&idx=${passed.idx}&nvalue=0&svalue=${passed.temp}"
            break;
         case "SetHumidity":  
         	hubPath = "/json.htm?type=command&param=udevice&idx=${passed.idx}&svalue=0&nvalue=${passed.humidity}"
            break;
         case "SetPower":  
         	hubPath = "/json.htm?type=command&param=udevice&idx=${passed.idx}&nvalue=0&svalue=${passed.power};${passed.energy}"
            break;
         case "Notification": 
         	def tWhen = 0
            def tValue = 0
            
            if (passed?.value > 0) {
            	tWhen = 2
                tValue = passed.value
                passed.action = "${passed.action}%20${tValue}"
            }
        	hubPath = "/json.htm?type=command&param=addnotification&idx=${passed.idx}&ttype=${passed.type}&twhen=${tWhen}&tvalue=${tValue}&tmsg=IDX%20${passed.idx}%20${passed.action}&tsystems=http&tpriority=0&tsendalways=false&trecovery=false"
            hubCallback = [callback: callbackNotification]
            break;
        case ["SensorKWHNotification", "SensorLuxNotification", "SensorHumidityNotification", "SensorAirQualityNotification","SensorPressureNotification", "SensorSoundNotification"]: 
        	def typeSensor = passed.request - "Sensor" - "Notification"
            def callBack = "callback${typeSensor}Notification"
            hubPath = "/json.htm?type=command&param=addnotification&idx=${passed.idx}&ttype=5&twhen=1&tvalue=0&tmsg=SENSOR%20${passed.idx}%20${typeSensor}%20%24value&tsystems=http&tpriority=0&tsendalways=false&trecovery=false"
            hubCallback = [callback: callBack]
            break;         
        case ["SensorGasNotification"]:       
            hubPath = "/json.htm?type=command&param=addnotification&idx=${passed.idx}&ttype=14&twhen=1&tvalue=0&tmsg=SENSOR%20${passed.idx}%20Gas%20%24value&tsystems=http&tpriority=0&tsendalways=false&trecovery=false"
            hubCallback = [callback: callbackSensorGasNotification]
            break;         
        case "SensorTempNotification":         
            hubPath = "/json.htm?type=command&param=addnotification&idx=${passed.idx}&ttype=0&twhen=3&tvalue=-99&tmsg=SENSOR%20${passed.idx}%20Temp%20%24value&tsystems=http&tpriority=0&tsendalways=false&trecovery=false"
            hubCallback = [callback: callbackSensorTempNotification]
            break;                
        case "ClearNotification":  
            hubPath = "/json.htm?type=command&param=clearnotifications&idx=${passed.idx}"
            hubCallback = [callback: callbackClearNotification]
            break;
		case "Settings":  
            hubPath = "/json.htm?type=settings"
            hubCallback = [callback: callbackForSettings]
            break;
		case "CreateHardware":  
            hubPath = "/json.htm?type=command&param=addhardware&htype=15&port=1&name=SmartThings&enabled=true"
            hubCallback = [callback: callbackCreateHardware]
            break;
		case "UpdateHardware":  
            hubPath = "/json.htm?type=command&param=updatehardware&htype=15&name=SmartThings&enabled=true&idx=${state.dzHardwareIdx}&datatimeout=0&Mode1=0&Mode2=0&Mode3=0&Mode4=0&Mode5=0&Mode6=0"
            hubCallback = [callback: callbackUpdateHardware]
            break;
		case "DeleteHardware":  
            hubPath = "/json.htm?type=command&param=deletehardware&idx=${state.dzHardwareIdx}"
            break;
		case "ListHardware":  
            hubPath = "/json.htm?type=hardware"
            hubCallback = [callback: callbackListHardware]
            break;        
		case "CreateVirtualDevice":  
            hubPath = "/json.htm?type=command&param=addswitch&hwdid=${state.dzHardwareIdx}&name=${passed.deviceName}&description=undefined&switchtype=${passed.switchType}&lighttype=0&housecode=80&unitcode=${passed.unitcode}"
            hubCallback = [callback: callbackVirtualDevices]
			break;        
		case "CreateVirtualSensor":  
            hubPath = "/json.htm?type=createvirtualsensor&idx=${state.dzHardwareIdx}&sensorname=${passed.deviceName}&sensortype=${passed.sensorType}"
            hubCallback = [callback: callbackVirtualDevices]
            break;
        case "CreateVirtualCustomDevice":  
        	hubPath = "/json.htm?type=createdevice&idx=${state.dzHardwareIdx}&sensorname=${passed.deviceName}&devicetype=${passed.deviceType}&devicesubtype=${passed.deviceSubType}"
        	hubCallback = [callback: callbackVirtualDevices]
        	break; 
        default:
        	return
            break;           
	}
	
	if (hubPath == null) {
    	log.error "[socketSend] Error in calling socketsend, check hubPath/Request assignment"
        return
    }
	TRACE("[socketSend] Request: ${passed.request} callbackhandler: ${hubCallback.callback}")
    sendHubCommand(new physicalgraph.device.HubAction(method: "GET", path: hubPath, headers: [HOST: "${state.networkId}"], null, hubCallback))
    //pause 5
}

void aliveResponse(evt) {
	state.alive = true
    state.aliveCount = 0

    if (state.aliveAgain == false) {
    	state.aliveAgain = true
        //sendNotification("${app.name} ${app.label} is responding again", [method: "push"])
        
        if (state.devicesOffline) devicesOnline()
        socketSend([request : "List"])
    }
}

void aliveChecker(evt) {

    runIn(5, sendThermostatModes)
    runIn(10, refreshDevicesFromDomoticz)
    runIn(30, scheduledListSensorOptions)
    
	def cycles 
	if (state?.scheduleCycle == null)  state.scheduleCycle = 11i
    
    cycles = state.scheduleCycle + 1
    
	if (state.alive == false && state.aliveCount > 1) {
    	state.aliveAgain = false
        if (!state.devicesOffline) {
        	//sendNotification("${app.name} ${app.label} is not responding", [method: "push"])
        	devicesOffline()
        }
    }
    
    if (state.aliveCount) state.aliveCount = state.aliveCount + 1
    else state.aliveCount = 1
    
    state.alive = false  
    socketSend([request : "alive"])
    state.scheduleCycle = cycles
}

private def devOnline(dev) {
    if (dev?.currentValue("DeviceWatch-DeviceStatus") == "offline") dev.sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
}

private def devOffline(dev) {
	if (dev?.currentValue("DeviceWatch-DeviceStatus") == "online") dev.sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
}

void devicesOnline() {
    log.warn "[devicesOnline] turn devices ONLINE"

	getChildDevices().each { dev ->
    	
		if (!dev?.currentValue("DeviceWatch-Enroll")) {
            dev.sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson([protocol: "LAN", scheme:"untracked"]), displayed: false)
        } 
        else {
	        dev.sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
        }        
    }
    state.devicesOffline = false
}

void devicesOffline() {
    log.warn "[devicesOffline] turn devices OFFLINE"

	getChildDevices().each { dev ->
    
		if (!dev?.currentValue("DeviceWatch-Enroll")) {
            dev.sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson([protocol: "LAN", scheme:"untracked"]), displayed: false)
        } 
        dev.sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
    }
    state.devicesOffline = true
}

/*-----------------------------------------------------------------------------------------*/
/*		
/*-----------------------------------------------------------------------------------------*/
void refreshDevicesFromDomoticz() {
	TRACE("[refreshDevicesFromDomoticz]")
    
    socketSend([request : "roomplans"])
	state.listOfRoomPlanDevices = []
    //pause 5
    
    if (settings.domoticzPlans) {
        state.statusPlansRsp.findAll{it.Name in settings.domoticzPlans.toList()}.each {
            if (it.Devices.toInteger() > 0) {
                socketSend([request : "roomplan", idx : it.idx])
            }
        }
	}
    
    socketSend([request : "scenes"])
    socketSend([request : "List"])
}

/*-----------------------------------------------------------------------------------------*/
/*		Domoticz will send an notification message to ST for all devices THAT HAVE BEEN SELECTED to do that
/*-----------------------------------------------------------------------------------------*/
def eventDomoticz() {
	
    aliveResponse()
    
	if (settings?.domoticzVirtualDevices == true) {
        if (params.message.contains("IDX ") && params.message.split().size() == 3) {
            def idx = params.message.split()[1]
            def status = params.message.split()[2]
            //SWITCHES
            def item = state.virtualDevices.find {key, item -> item.idx == idx && item.type == "switch" }
            if (item != null) {
                settings.dzDevicesSwitches.each { device ->
                    if (device.deviceNetworkId == item.value.dni) {
                        if (device.currentValue("switch").toUpperCase() != status.toUpperCase()) {		// status was changed in DZ for a virtual device
                            if (status == "on") device.on() else device.off()
                        }
                    }
                }
                TRACE("[eventDomoticz] IDX switch with STATE ${params.message}")
                return
            }
            //LOCKS
            def lock = state.virtualDevices.find {key, lock -> lock.idx == idx && lock.type == "lock" }
            if (lock != null) {
                TRACE("Found virtual device ${lock.value.dni} of lock type")
                settings.dzDevicesLocks.each { device ->
                    if (device.deviceNetworkId == lock.value.dni) {
                        if (device.currentValue("lock").toUpperCase() != status.toUpperCase()) {		// status was changed in DZ for a virtual device
                            if (status == "on") device.lock() else device.unlock()
                        }
                    }
                }
                TRACE("[eventDomoticz] IDX Locks with STATE ${params.message}")
                return
            }
        }
   	}
    
	if (params.message.contains("IDX ") && params.message.split().size() >= 3) {
    	def idx = params.message.split()[1]
    	def status = params.message.split()[2]
        def dni = state.devices[idx]?.dni
        def deviceType = state.devices[idx]?.deviceType
        def switchTypeVal = state.devices[idx]?.switchTypeVal
		def attr = null
        def level = ""

        if (params.message.split().size() == 4) level = params.message.split()[3]
        
       	switch (deviceType) {
        	case "domoticzOnOff":
            	if (switchTypeVal != 7) attr = "switch"   // 7 is a dimmer , just request complete status to catch all states
            	break
            case "domoticzMotion":
            	attr = "motion"
                if (status == "on") status = "active" else status = "inactive"
            	break
            case "domoticzSelector":
            	attr = "switch"
				if (status == "off") {
                	level = 0
                    }
                break
            case "domoticzDuskSensor":
            	attr = "switch"
                break
            case "domoticzContact":
            	attr = "contact"
                if (status == "on") status = "open" else status = "closed"
               	break
            case "domoticzThermostat":
            	attr = "thermostatSetpoint"
               	break
            case "domoticzSmokeDetector":
            	attr = "smoke"
                if (status == "on") status = "smoke" else status = "clear"
            	break                
        }
        
		if (!getChildDevice(dni)) { // device has been deleted and notifications still present.
        	log.warn "[eventDomoticz] IDX with no defined Device ${params.message}"
            socketSend([request : "ClearNotification", idx : idx])	// let the app redefine notification
        }
        else {        
            if (getChildDevice(dni)?.displayName.toUpperCase().contains("MOOD")) {
                attr = null
            }

            if (attr) {
        		TRACE("[eventDomoticz] IDX(${deviceType}) with STATE and Attr ${params.message}")
                getChildDevice(dni).sendEvent(name: attr, value: status)
                if (level != "") {
                    // multiselector switches will have a level in their custom notification
                    getChildDevice(dni).sendEvent(name: "level", value: level)
                    domoticz_poll(idx)
                    }
            }
            else {
            	TRACE("[eventDomoticz] IDX(${deviceType}) with STATE but no Attr ${params.message}")
                socketSend([request : "status", idx : idx])
                socketSend([request : "utilityCount", idx : idx])
            }
            if (state.sceneGroupDevices) {
            	state.sceneGroupDevices.each { index, scene ->
                    if (scene.result.find {it.DevRealIdx == idx}) {
                    	TRACE("[eventDomoticz] found group device ${idx} in scene-group ${scene.sceneIdx} requesting status from Domoticz")
                        socketSend([request : "scenesUpdate"])
                    }
                }
            }
    	}
    }
    else if (params.message.contains("SENSOR ") && params.message.split().size() != 4) {
    		log.warn "[eventDomoticz] Partial ${params.message} Auto define Notifications"
            def idx = params.message.split()[1]
            socketSend([request : "ClearNotification", idx : idx])	// let the app redefine notification
    	}  
    else if (params.message.contains("SENSOR ") && params.message.split().size() == 4) {
            def idx = params.message.split()[1]
            def typeSensor = params.message.split()[2]
            def typeValue = params.message.split()[3]
            
            if (typeSensor == "Temp") typeValue = typeValue.toFloat().round(1)
            else typeValue = typeValue.toFloat().round(0).toInteger()
            
            if (typeSensor.matches("KWH|Lux|Gas|AirQuality|Sound|Pressure|Temp")) {
            	//TRACE("[eventDomoticz] ${params.message}")
                doUtilityEvent([idx: idx, idxName: typeSensor, name:"", value:typeValue])
            }
            else socketSend([request : "utilityCount", idx : idx])
    	}  
    else {
		TRACE("[eventDomoticz] no custom message in Notification (unknown device in ST) perform List ${params.message}")
		// get the unknown device defined in ST (if part of selected types)  
        socketSend([request : "List"])
    }
}

/*-----------------------------------------------------------------------------------------*/
/*		get the access token. It will be displayed in the log/IDE. Plug this in the Domoticz Notification Settings access_token
/*-----------------------------------------------------------------------------------------*/
private def initRestApi() {
    TRACE("[initRestApi]")
    if (!state.accessToken) {
        try {
        	def token = createAccessToken()
        	TRACE("[initRestApi] Created new access token: ${state.accessToken}")
        }
        catch (e) {
			log.warn "[initRestApi] did you enable OAuth in the IDE for this APP?"
        }
    }
    state.urlCustomActionHttp = getApiServerUrl() - ":443" + "/api/smartapps/installations/${app.id}/" + "EventDomoticz?access_token=" + state.accessToken + "&message=#MESSAGE"

}
//-----------------------------------------------------------
private def getResponse(evt) {    
	
    
    if (evt instanceof physicalgraph.device.HubResponse) {
        if (evt.json?.status != "OK") {
    		log.error "[getResponse] ${evt}"
    		log.error "[getResponse] ${evt.json}"
            return null
    	}
		else {
        	return evt.json
        }
    }
}

private def getHubID(){
    TRACE("[getHubID]")
    def hubID
    def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL } 
    if (hubs.size() == 1) hubID = hubs[0].id 
    return hubID
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private def textCopyright() {
    return "Copyright (c) 2019 Martin Verbeek"
}

private def idxSettings(type) {
	if (!type) type = "" else type = type + "[" 
	return settings.findAll { key, value -> key.contains("idx${type}") }
}

private def idxComponentDevices(passed) {
	if (!passed.type) return
   
	if (!passed.idx) return state.devices.findAll { key, value -> value?."idx${passed.type}" != null}
    else return state.devices.findAll { key, value -> value?."idx${passed.type}" == passed.idx.toString()}
}

def notifyNewVersion() {

	TRACE("[notifyNewVersion] on GitHub ${appVerInfo().split()[1]} running ${runningVersion()} ")
	if (appVerInfo().split()[1] != runningVersion()) {
    	sendNotificationEvent("Domoticz Server App has a newer version, ${appVerInfo().split()[1]}, please visit IDE to update app/devices")
    }
}

def getWebData(params, desc, text=true) {
	try {
		httpGet(params) { resp ->
			if(resp.data) {
				if(text) { return resp?.data?.text.toString() } 
                else { return resp?.data }
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {log.error "${desc} file not found"} 
        else { log.error "[getWebData] (params: $params, desc: $desc, text: $text) Exception:", ex}
		
        return "[getWebData] ${label} info not found"
	}
}

private def appVerInfo() { return getWebData([uri: "https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/DomoticzData", contentType: "text/plain; charset=UTF-8"], "changelog") }

private def appDZInfo()	{ return getWebData([uri: "https://raw.githubusercontent.com/verbem/Domoticz-Server/master/smartapps/verbem/domoticz-server.src/V4Patch", contentType: "text/plain; charset=UTF-8"], "DZSettings") }

private def getSettingsForm(dzSettings) {

    def V4Keys = appDZInfo().split()
    def V4 = [:]
    V4Keys.each { key ->
    	if (key.contains(":")) V4[key.split(":")[0]] = key.split(":")[1]
        else V4[key] = ""
    }
    dzSettings.each { key, value ->
    	if (key.toUpperCase().contains("ENABLE")) {if (value == 1) value = "on" else value = "off" }
    	if (V4.containsKey(key)) V4[key] = value
    }
    
    if (dzSettings.HTTPURL != state.urlCustomActionHttp) {
    	TRACE("[getSettingsForm] urlCustomActionHttp has been set")
    	V4.HTTPURL = state.urlCustomActionHttp
    	if (dzSettings.HTTPEnabled != 1) V4.HTTPEnabled = "on"
    }
    V4.remove("Patch")
    // Exception where settings keyword != form keyword
    V4.Themes = dzSettings.WebTheme
    V4.Latitude = dzSettings.Location.Latitude
    V4.Longitude = dzSettings.Location.Longitude
    
    def postData = V4.collect { "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }.join("&")
    return postData
}
