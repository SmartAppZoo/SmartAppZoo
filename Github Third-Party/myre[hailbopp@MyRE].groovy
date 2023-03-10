import groovy.json.JsonOutput

include 'asynchttp_v1'

String getNAMESPACE() { return "zabracks" }
String getAPP_NAME() { return "MyRE" }
String getVERSION() { return "0.0.1" }

String getWORKER_APP_NAME() { return APP_NAME + "-worker" }

definition(
        name: APP_NAME,
        namespace: NAMESPACE,
        author: "Drew Worthey",
        description: "Automate all the things!",
        category: "Convenience",
        singleInstance: false,
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "pageInit")
    page(name: "pageFinishInstall")
    page(name: "pageSettings")
}

// Info
private generateNewExecutionToken() {
    state.executionToken = UUID.randomUUID().toString();
}

private revokeExecutionToken() {
    state.executionToken = null;
}

private populateConnectionInfo() {
    def accessToken = createAccessToken()
    if (state.accessToken) {
        generateNewExecutionToken()
        state.accountId = app.getAccountId();
        state.accessToken = accessToken
        state.apiServerBaseUrl = apiServerUrl("")
        state.appId = app.id
    }
}

private isOAuthEnabled() {
    return createAccessToken();
}

private isInstalled() {
    return state.installed
}

// Preference pages
private sectionDomainEntry() {
    section() {
        input "instanceName", "text", title: "Enter a name for this instance so that you can identify it later.", defaultValue: app.name, required: true
    }
    section() {
        input "apiDomain", "text", title: "Enter the domain of the ${APP_NAME} server that you wish to use.", defaultValue: "http://freecoreweb.azurewebsites.net", required: true
    }
}

private sectionSelectDevices() {
    section() {
        paragraph "Most devices should fall into one of these two categories"
        input "dev:actuator", "capability.actuator", multiple: true, title: "Which actuators?", required: false
        input "dev:sensor", "capability.sensor", multiple: true, title: "Which sensors?", required: false
    }
    section() {
        paragraph "If you cannot find a device by type, you may try looking for it by category below"
        def d
        for (capability in capabilities().findAll{ (!(it.value.d in [null, 'actuators', 'sensors'])) }.sort{ it.value.d }) {
            if (capability.value.d != d) input "dev:${capability.key}", "capability.${capability.key}", multiple: true, title: "Which ${capability.value.d}?", required: false
            d = capability.value.d
        }
    }
}

def pageInit() {
    populateConnectionInfo()
    if (!isInstalled()) {
        return dynamicPage(name: "pageInit", title: "", install: false, uninstall: false, nextPage: "pageFinishInstall") {
            section() {
                paragraph "Welcome to ${APP_NAME}! We have a little bit of preparation and configuration to do before you can log in to the web console and start automating all the things."
            }

            if(!isOAuthEnabled()) {
                section() {
                    paragraph "OAuth is not enabled. You must enable it in the SmartThings developer console."
                }
            }

            if(!location.getTimeZone()) {
                section() {
                    paragraph "Your location must be configured."
                }
            }

            Boolean isOkay = isOAuthEnabled() && location.getTimeZone()

            if (isOkay) {
                sectionDomainEntry()
                sectionSelectDevices()
            }
        }
    }
    dynamicPage(name: "pageInit", title: "", install: true, uninstall: true) {
        section("Web Console") {
            href "", title: "Web Console", style: "external", url: getConsoleInitUrl(), description: "Tap to reauthenticate.", image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png", required: false
            //href "", title: "Access Web Console", style: "external", url: getConsoleBaseUrl(), description: "Tap to open", image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png", required: false
        }

        section(title:"Settings") {
            href "pageSettings", title: "Settings", image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png", required: false
        }

    }
}

private pageSettings() {
    dynamicPage(name: 'pageSettings', title: "", install: false, uninstall: false, nextPage: (isInstalled() ? 'pageInit' : 'pageFinishInstall')) {
        sectionDomainEntry()
        sectionSelectDevices()
    }
}

private pageFinishInstall() {
    state.installed = true;
    dynamicPage(name: "pageFinishInstall", title: "", install: true, uninstall: false) {
        section() {
            paragraph "Installation is complete."
        }
        section("Note") {
            paragraph "After you tap Done, go to the Automation tab, select the SmartApps section, and open the SmartApp to access the management console.", required: true
            paragraph "You'll be asked to log in or create an account on the ${APP_NAME} server.", required: true
            paragraph "You can also access the console on any another device by entering ${settings.domain} in the address bar of your browser.", required: true
        }
        section() {
            paragraph "Now tap Done and enjoy ${APP_NAME}!"
        }
    }
}

/// Handlers
def primaryHandler(event) {
    if (!event || (!event.name.endsWith(APP_NAME))) return;
    def data = event.jsonData ?: null
}

/// Subscription management
private subscribeToEvents() {
    subscribe(location, "${APP_NAME}.poll", primaryHandler)
    subscribe(location, "${'@@' + APP_NAME}", primaryHandler)
    //subscribe(location, "HubUpdated", hubUpdatedHandler, [filterEvents: false])
    //subscribe(location, "summary", summaryHandler, [filterEvents: false])
    //setPowerSource(getHub()?.isBatteryInUse() ? 'battery' : 'mains')
}

/// Authentication with app server
private String getConsoleInitUrl(register = false) {
    def url = getConsoleBaseUrl()
    if (!url) return null

    def body = [
            AccessToken: state.accessToken,
            ApiServerBaseUrl: state.apiServerBaseUrl,
            AppId: state.appId,
            ExecutionToken: state.executionToken,
            AccountId: state.accountId,
            InstanceName: settings.instanceName
    ]

    def json = new groovy.json.JsonBuilder(body)
    return url + "api/Auth/Initialize/" + (json.toString()).bytes.encodeBase64()
}

public String getConsoleBaseUrl() {
    return "${settings.apiDomain}/"
}

def registerInstance() {
    //TODO
}

/// Initialization
def installed() {
    state.installed = true
    initialize()
    return true
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
    return true
}

private initialize() {
    subscribeToEvents()
    state.version = VERSION
    if (state.installed) {
        //registerInstance()
    }
}

/// Core functionality
def getChildren() {
    return getAllChildApps()
}

/// API Logic

def getLocationInfo() {
	def loc = getLocation()
	[
		id: loc.id,
		name: loc.name,
		contactBookEnabled: loc.getContactBookEnabled(),
		availableModes: loc.getModes().collect { it.name },
		activeMode: loc.getMode(),
		position: [
			latitude: loc.getLatitude(),
			longitude: loc.getLongitude()
		],
		tempScale: loc.getTemperatureScale(),
		timeZoneStandardOffsetMs: loc.getTimeZone().getRawOffset(),
		postalCode: loc.getZipCode()
	]
}

def mapAttributeInfo(attribute) {
    return [
            name: attribute.getName(),
            type: attribute.getDataType(),
            values: attribute.getValues()
    ]
}

def mapState(state) {
    return [
            name: state.getName(),
            timestamp: state.getDate(),
            value: state.getValue()
    ]
}

def mapCommandInfo(cmd) {
    return [
            name: cmd.getName(),
            arguments: cmd.getArguments()
    ]
}

def mapCapabilityInfo(cap) {
    return [
            name: cap.getName(),
            attributes: cap.getAttributes().collect{mapAttributeInfo(it)},
            commands: cap.getCommands().collect{mapCommandInfo(it)}
    ]
}

def getDeviceInfo(device) {
    return [
            deviceId: device.getId(),
            label: device.getLabel(),
            displayName: device.getDisplayName(),
            modelName: device.getModelName(),
            manufacturer: device.getManufacturerName(),
            attributes: device.getSupportedAttributes().collect{mapAttributeInfo(it)},
            commands: device.getSupportedCommands().collect{mapCommandInfo(it)},
            capabilities: device.getCapabilities().collect{mapCapabilityInfo(it)}
    ]
}

def getDeviceState(device) {
    return [
            deviceId: device.getId(),
            label: device.getLabel(),
            displayName: device.getDisplayName(),
            attributeStates: device.getSupportedAttributes().collect{ device.currentState(it.name) }.findAll{ it != null }.collect{ mapState(it) }
    ]
}

def mapChildApp(ca) {
    ca.getSummary()
}

def getDeviceById(deviceId) {
    getManagedDevices().find { it.id == deviceId }
}

def getManagedDevices() {
    return settings.findAll{ it.key.startsWith("dev:") }.collect{ it.value }.flatten()
}

def getDeviceStatusById(deviceId) {
    def device = getDeviceById(deviceId)
    if(device) {
        return getDeviceState(device)
    }
    return null
}

def isProjectInputValid(proj) {
    return proj && proj.projectId && proj.name && proj.description && proj.expressionTree
}

def isProjectListValid(projList) {
    def isValid = true

    projList.each {
        if(!isProjectInputValid(it)) {
            isValid = false
        }
    }

    return isValid
}

def getChildProjects() {
    getChildren().collect { mapChildApp(it) }
}

def getChildBy(Closure cl) {
    getChildren().find(cl)
}

def deleteChildProjects() {
    getChildren().each { ca ->
        app.deleteChildApp(ca)
    }
}

def generateUniqueChildName(String baseName) {
    List<String> childNames = getChildren().collect { it.getName() }

    def name = baseName
    int iteration = 0
    while(childNames.contains(name)) {
        iteration++
        name = "${baseName} ${iteration}"
    }
}

def createProjectChildApp(proj) {
    //log.info("Attempting to add child app ${NAMESPACE}:${WORKER_APP_NAME} with properties ${proj}")
    def newSmartapp = addChildApp(NAMESPACE, WORKER_APP_NAME, proj['name'])
    def success = newSmartapp.setup(proj['projectId'], proj['name'], proj['description'], proj['source'])
    //if(hubUID) newSmartapp.installed()
    newSmartapp
}

def getChildAppByProjectId(projectId) {
    getChildren().find { it.getSummary().projectId == projectId }
}

def updateProjectChildApp(projectId, name, desc, source) {
    def childApp = getChildAppByProjectId(projectId)
    childApp.updateProject(name, desc, source)
}

// Endpoints
mappings {
    path("/status") {
        action: [
                GET: 'getInstanceStatus'
        ]
    }
    path("/devices") {
        action: [
                GET: 'listDevices'
        ]
    }
    path("/devices/:deviceId") {
        action: [
                GET: 'getDeviceById'
        ]
    }
    path("/devices/:deviceId/commands/:commandName") {
        action: [
                POST: 'executeDeviceCommand'
        ]
    }
    path("/projects") {
        action: [
                GET: 'listChildApps',
                POST: 'createProject',
                DELETE: 'deleteChildApps'
        ]
    }
    path("/projects/:projectId") {
        action: [
                GET: 'retrieveProject',
                PUT: 'updateProject'
        ]
    }
    path("/projects/:projectId/run") {
        action: [
                POST: 'executeProject'
        ]
    }
    path("/projects/:projectId/resume") {
        action: [
                POST: 'resumeProject'
        ]
    }
    path("/projects/:projectId/stop") {
        action: [
                POST: 'stopProject'
        ]
    }
}

def renderJson(data) {
    def content = (new JsonOutput()).toJson(data)
    return render(contentType: "application/json", data: content)
}

// GET /status
def getInstanceStatus() {
    def response = [
        instanceId: app.id,
        accountId: app.getAccountId(),
        instanceName: settings.instanceName,
		location: getLocationInfo(),
    ]

    response.timestamp = now()
    renderJson response
}

// GET /devices
def listDevices() {
    try {
        return getManagedDevices().collect { getDeviceInfo it }
    } catch(e) {
        return render(status: 500, data: e)
    }
}

// GET /devices/:deviceId
def getDeviceById() {
    def deviceId = params?.deviceId
    def result = getDeviceStatusById(deviceId)
    if(result) {
        return result
    } else {
        return render(status: 404, data: "Device with that ID does not exist.")
    }
}

// POST /devices/:deviceId/commands/:commandName
def executeDeviceCommand() {
    def deviceId = params?.deviceId
    def command = params?.commandName
    def d = getDeviceById(deviceId)

    def params = request.JSON.params

    try {
        if (params.size()) {
            d."$command"(params as Object[])
        } else {
            d."$command"()
        }
        return render(status: 200, data: '{result: "OK"}')
    } catch (e) {
        return render(status: 500, data: e)
    }
}

// GET /projects
def listChildApps() {
    return getChildProjects()
}

// DELETE /projects
def deleteChildApps() {
    try {
        deleteChildProjects()
        return render(status: 200, data: "OK")
    } catch(e) {
        return render(status: 500, data: e)
    }
}

// POST /projects
/**
 * Add a new child app.
 * The assumed input format for this endpoint is a JSON object of type:
 * { projects: Array<{
 *          projectId: string;
 *          name: string;
 *          description: string;
 *          source: string;
 *  }> }
 */
def createProject() {
    if(request.JSON) {
        def newApp = createProjectChildApp(request.JSON)
        renderJson newApp.getSummary()
    } else {
        return render(status: 400, data: "Did not provide proper inputs.")
    }
}

// POST /projects/:projectId/run
def executeProject() {
    def projectId = params?.projectId
    def projectApp = getChildBy { it.getProjectId() == projectId }
    if(projectApp) {
        try{
            def executeResult = projectApp.execute()
            return executeResult
        } catch(ex) {
            return ex
        }

    } else {
        return render(status: 404, data: "Not Found")
    }
}

// POST /projects/:projectId/resume
def resumeProject() {
    def projectId = params?.projectId
    def projectApp = getChildBy { it.getProjectId() == projectId }
    if(projectApp) {
        def resumeResult = projectApp.resume()
        return render(status: 200, data: '{result: "OK"}')
    } else {
        return render(status: 404, data: "Not Found")
    }
}

// POST /projects/:projectId/stop
def stopProject() {
    def projectId = params?.projectId
    def projectApp = getChildBy { it.getProjectId() == projectId }
    if(projectApp) {
        def haltResult = projectApp.halt()
        return render(status: 200, data: '{result: "OK"}')
    } else {
        return render(status: 404, data: "Not Found")
    }
}

// GET /projects/:projectId
def retrieveProject() {
    def projectId = params?.projectId
    def projectApp = getChildBy { it.getProjectId() == projectId }
    if(projectApp) {
        return projectApp.getSummary()
    } else {
        return render(status: 404, data: "Not Found")
    }
}

// PUT /projects/:projectId
def updateProject() {
    def projectId = params?.projectId
    def projectApp = getChildBy { it.getProjectId() == projectId }
    def proj = request.JSON
    if(projectApp) {
        projectApp.updateProject(proj['name'], proj['description'], proj['source'])
        return projectApp.getSummary()
    } else {
        return render(status: 404, data: "Not Found")
    }
}


/// Utilities

private info(message, shift = null, err = null) { log.info message, err, 'info' }
private trace(message, shift = null, err = null) { debug message, shift, err, 'trace' }
private warn(message, shift = null, err = null) { debug message, shift, err, 'warn' }
private error(message, shift = null, err = null) { debug message, shift, err, 'error' }
private timer(message, shift = null, err = null) { debug message, shift, err, 'timer' }

/// Data

private static Map capabilities() {
    //n = name
    //d = friendly devices name
    //a = default attribute
    //c = accepted commands
    //m = momentary
    //s = number of subdevices
    //i = subdevice index in event data
    return [
            accelerationSensor			: [ n: "Acceleration Sensor",			d: "acceleration sensors",			a: "acceleration",																																																							],
            actuator					: [ n: "Actuator", 						d: "actuators",																																																																	],
            alarm						: [ n: "Alarm",							d: "alarms and sirens",				a: "alarm",								c: ["off", "strobe", "siren", "both"],																																								],
            audioNotification			: [ n: "Audio Notification",			d: "audio notification devices",											c: ["playText", "playTextAndResume", "playTextAndRestore", "playTrack", "playTrackAndResume", "playTrackAndRestore"],				 																],
            battery						: [ n: "Battery",						d: "battery powered devices",		a: "battery",																																																								],
            beacon						: [ n: "Beacon",						d: "beacons",						a: "presence",																																																								],
            bulb						: [ n: "Bulb",							d: "bulbs",							a: "switch",							c: ["off", "on"],																																													],
            button						: [ n: "Button",						d: "buttons",						a: "button",				m: true,	s: "numberOfButtons,numButtons", i: "buttonNumber",																																					],
            carbonDioxideMeasurement	: [ n: "Carbon Dioxide Measurement",	d: "carbon dioxide sensors",		a: "carbonDioxide",																																																							],
            carbonMonoxideDetector		: [ n: "Carbon Monoxide Detector",		d: "carbon monoxide detectors",		a: "carbonMonoxide",																																																						],
            colorControl				: [ n: "Color Control",					d: "adjustable color lights",		a: "color",								c: ["setColor", "setHue", "setSaturation"],																																							],
            colorTemperature			: [ n: "Color Temperature",				d: "adjustable white lights",		a: "colorTemperature",					c: ["setColorTemperature"],																																											],
            configuration				: [ n: "Configuration",					d: "configurable devices",													c: ["configure"],																																													],
            consumable					: [ n: "Consumable",					d: "consumables",					a: "consumableStatus",					c: ["setConsumableStatus"],																																											],
            contactSensor				: [ n: "Contact Sensor",				d: "contact sensors",				a: "contact",																																																								],
            doorControl					: [ n: "Door Control",					d: "automatic doors",				a: "door",								c: ["close", "open"],																																												],
            energyMeter					: [ n: "Energy Meter",					d: "energy meters",					a: "energy",																																																								],
            estimatedTimeOfArrival		: [ n: "Estimated Time of Arrival", 	d: "moving devices (ETA)",			a: "eta",																																																									],
            garageDoorControl			: [ n: "Garage Door Control",			d: "automatic garage doors",		a: "door",								c: ["close", "open"],																																												],
            holdableButton				: [ n: "Holdable Button",				d: "holdable buttons",				a: "button",				m: true,	s: "numberOfButtons,numButtons", i: "buttonNumber",																																					],
            illuminanceMeasurement		: [ n: "Illuminance Measurement",		d: "illuminance sensors",			a: "illuminance",																																																							],
            imageCapture				: [ n: "Image Capture",					d: "cameras, imaging devices",		a: "image",								c: ["take"],																																														],
            indicator					: [ n: "Indicator",						d: "indicator devices",				a: "indicatorStatus",					c: ["indicatorNever", "indicatorWhenOn", "indicatorWhenOff"],																																		],
            infraredLevel				: [ n: "Infrared Level",				d: "adjustable infrared lights",	a: "infraredLevel",						c: ["setInfraredLevel"],																																											],
            light						: [ n: "Light",							d: "lights",						a: "switch",							c: ["off", "on"],																		 																											],
            lock						: [ n: "Lock",							d: "electronic locks",				a: "lock",								c: ["lock", "unlock"],	s:"numberOfCodes,numCodes", i: "usedCode", 																									 								],
            lockOnly					: [ n: "Lock Only",						d: "electronic locks (lock only)",	a: "lock",								c: ["lock"],																																														],
            mediaController				: [ n: "Media Controller",				d: "media controllers",				a: "currentActivity",					c: ["startActivity", "getAllActivities", "getCurrentActivity"],																																		],
            momentary					: [ n: "Momentary",						d: "momentary switches",													c: ["push"],																																														],
            motionSensor				: [ n: "Motion Sensor",					d: "motion sensors",				a: "motion",																																																								],
            musicPlayer					: [ n: "Music Player",					d: "music players",					a: "status",							c: ["mute", "nextTrack", "pause", "play", "playTrack", "previousTrack", "restoreTrack", "resumeTrack", "setLevel", "setTrack", "stop", "unmute"],													],
            notification				: [ n: "Notification",					d: "notification devices",													c: ["deviceNotification"],																																											],
            outlet						: [ n: "Outlet",						d: "lights",						a: "switch",							c: ["off", "on"],																																										 			],
            pHMeasurement				: [ n: "pH Measurement",				d: "pH sensors",					a: "pH",																																																									],
            polling						: [ n: "Polling",						d: "pollable devices",														c: ["poll"],																																														],
            powerMeter					: [ n: "Power Meter",					d: "power meters",					a: "power",																																																									],
            powerSource					: [ n: "Power Source",					d: "multisource powered devices",	a: "powerSource",																																																							],
            presenceSensor				: [ n: "Presence Sensor",				d: "presence sensors",				a: "presence",																																																								],
            refresh						: [ n: "Refresh",						d: "refreshable devices",													c: ["refresh"],																																														],
            relativeHumidityMeasurement	: [ n: "Relative Humidity Measurement",	d: "humidity sensors",				a: "humidity",																																																								],
            relaySwitch					: [ n: "Relay Switch",					d: "relay switches",				a: "switch",							c: ["off", "on"],																																													],
            sensor						: [ n: "Sensor",						d: "sensors",						a: "sensor",																																																								],
            shockSensor					: [ n: "Shock Sensor",					d: "shock sensors",					a: "shock",																																																									],
            signalStrength				: [ n: "Signal Strength",				d: "wireless devices",				a: "rssi",																																																									],
            sleepSensor					: [ n: "Sleep Sensor",					d: "sleep sensors",					a: "sleeping",																																																								],
            smokeDetector				: [ n: "Smoke Detector",				d: "smoke detectors",				a: "smoke",																																																									],
            soundPressureLevel			: [ n: "Sound Pressure Level",			d: "sound pressure sensors",		a: "soundPressureLevel",																																																					],
            soundSensor					: [ n: "Sound Sensor",					d: "sound sensors",					a: "sound",																																																									],
            speechRecognition			: [ n: "Speech Recognition",			d: "speech recognition devices",	a: "phraseSpoken",			m: true,																																																		],
            speechSynthesis				: [ n: "Speech Synthesis",				d: "speech synthesizers",													c: ["speak"],																																														],
            stepSensor					: [ n: "Step Sensor",					d: "step counters",					a: "steps",																																																									],
            switch						: [ n: "Switch",						d: "switches",						a: "switch",							c: ["off", "on"],																																										 			],
            switchLevel					: [ n: "Switch Level",					d: "dimmers and dimmable lights",	a: "level",								c: ["setLevel"],																																													],
            tamperAlert					: [ n: "Tamper Alert",					d: "tamper sensors",				a: "tamper",																																																								],
            temperatureMeasurement		: [ n: "Temperature Measurement",		d: "temperature sensors",			a: "temperature",																																																							],
            thermostat					: [ n: "Thermostat",					d: "thermostats",					a: "thermostatMode",					c: ["auto", "cool", "emergencyHeat", "fanAuto", "fanCirculate", "fanOn", "heat", "off", "setCoolingSetpoint", "setHeatingSetpoint", "setSchedule", "setThermostatFanMode", "setThermostatMode"],	],
            thermostatCoolingSetpoint	: [ n: "Thermostat Cooling Setpoint",	d: "thermostats (cooling)",			a: "coolingSetpoint",					c: ["setCoolingSetpoint"],																																											],
            thermostatFanMode			: [ n: "Thermostat Fan Mode",			d: "fans",							a: "thermostatFanMode",					c: ["fanAuto", "fanCirculate", "fanOn", "setThermostatFanMode"],																																	],
            thermostatHeatingSetpoint	: [ n: "Thermostat Heating Setpoint",	d: "thermostats (heating)",			a: "heatingSetpoint",					c: ["setHeatingSetpoint"],																																											],
            thermostatMode				: [ n: "Thermostat Mode",													a: "thermostatMode",					c: ["auto", "cool", "emergencyHeat", "heat", "off", "setThermostatMode"],																															],
            thermostatOperatingState	: [ n: "Thermostat Operating State",										a: "thermostatOperatingState",																																																				],
            thermostatSetpoint			: [ n: "Thermostat Setpoint",												a: "thermostatSetpoint",																																																					],
            threeAxis					: [ n: "Three Axis Sensor",				d: "three axis sensors",			a: "orientation",																																																							],
            timedSession				: [ n: "Timed Session",					d: "timers",						a: "sessionStatus",						c: ["cancel", "pause", "setTimeRemaining", "start", "stop", ],																																		],
            tone						: [ n: "Tone",							d: "tone generators",														c: ["beep"],																																														],
            touchSensor					: [ n: "Touch Sensor",					d: "touch sensors",					a: "touch",																																																									],
            ultravioletIndex			: [ n: "Ultraviolet Index",				d: "ultraviolet sensors",			a: "ultravioletIndex",																																																						],
            valve						: [ n: "Valve",							d: "valves",						a: "valve",								c: ["close", "open"],																																												],
            voltageMeasurement			: [ n: "Voltage Measurement",			d: "voltmeters",					a: "voltage",																																																								],
            waterSensor					: [ n: "Water Sensor",					d: "water and leak sensors",		a: "water",																																																									],
            windowShade					: [ n: "Window Shade",					d: "automatic window shades",		a: "windowShade",						c: ["close", "open", "presetPosition"],																																								],
    ]
}
