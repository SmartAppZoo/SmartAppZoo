/**
 *  Photon RF Gateway
 *
 *  Copyright 2015 Greg Peterson
 *
 *
 */

definition(
    name: "Photon RF Gateway",
    namespace: "gpete",
    author: "Greg Peterson",
    description: "Adds 433MHz RF capability to SmartThings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {    
	page(name: "mainPage")
    page(name: "manageDevicesPage")
    page(name: "loginPage")
}


mappings {
    path("/rf433") {
        action: [
            POST: "parseIncomingData"
        ]
    }
}

def mainPage() {
	state.particleAPIUri = "https://api.particle.io/v1"
    state.particleEventName = "rf433"
    state.particleSendCommandFunction = "rf433"
    state.maxNumDevices = 10
    populateSmartThingsAccessToken()
    
	dynamicPage(name: "mainPage", nextPage: null, uninstall: true, install: true) {      
        section("Photon") {
        	href(name: "particleLogin", page: "loginPage", title: "Photon", description: "")
        }
        
    	section("Devices") {
        	href(name: "manageDevices", page: "manageDevicesPage", title: "Manage Devices", description: "Add or edit devices...")
        }
        
        section("Device Settings") {
        	input(name: "inactiveCheckTime", title: "Device Activity Timeout (seconds)", type: "number", required: true)
        }
    }
}

def manageDevicesPage() {
	dynamicPage(name: "manageDevicesPage", nextPage: null, uninstall: false, install: false) {
    	def supportedCapabilities = ["Motion Sensor", "Outlet"]
        
        // These input name arrays are needed for dynamically creating input fields in the mobile app
        if (state.deviceNames?.size() != state.maxNumDevices) {        	
            state.deviceNames = []
            state.deviceTypes = []
            state.deviceCodes = []
            state.deviceCodes2 = []
            for (int i = 0; i < state.maxNumDevices; i++) {
                state.deviceNames[i] = "device${i}Name"
                state.deviceTypes[i] = "device${i}Type"
                state.deviceCodes[i] = "device${i}Code"
                state.deviceCodes2[i] = "device${i}Code2"
            }
        }
        
        for (int i = 0; i < state.maxNumDevices; i++) {
            section(hideable: true, hidden: !state.deviceNames[i], "Device ${i + 1}") {
                input(name: state.deviceNames[i], title: "Name", type: "text", required: false)
                input(name: state.deviceTypes[i], title: "Type", type: "enum", required: false, options: supportedCapabilities, submitOnChange: true)
                
                def code1Title = "Code"
                def code2Title = "Code2"
                def code2Visible = false
                switch (settings."${state.deviceTypes[i]}") {
                	case "Outlet":
                    	code1Title = "On Code"
                        code2Title = "Off Code"
                        code2Visible = true
                        break
                }
                input(name: state.deviceCodes[i], title: code1Title, type: "text", required: false)
                if (code2Visible) {
                    input(name: state.deviceCodes2[i], title: code2Title, type: "text", required: false)
                }
            }
        }
    }
}

def loginPage() {
	dynamicPage(name: "loginPage") {
    	if (settings.particleUsername && settings.particlePassword && !state.particleToken) {
        	populateParticleToken()
        }
        
    	if (!state.particleToken) {
            section("Please sign into your Particle account") {
                input(name: "particleUsername", type: "text", title: "Particle Username", required: true)
                input(name: "particlePassword", type: "password", title: "Particle Password", required: true, submitOnChange: true)
            }
        }
        else {
        	section("Select Device") {
            	def particleDevices = getParticleDevices()
                input(name: "particleDevice", type: "enum", title: "Select a device", required: true, multiple: false, options: particleDevices)
            }
        }
    }
}

def getParticleDevices() {
	def particleDevices = [:]
    def readingClosure = { response -> response.data.each { device ->
    		particleDevices.put(device.id, device.name)  
        }
	}
    httpGet("${state.particleAPIUri}/devices?access_token=${state.particleToken}", readingClosure)
 	return particleDevices
}

def populateParticleToken() {
    def authEncoded = "particle:particle".bytes.encodeBase64()
    httpPost(uri: "https://api.particle.io/oauth/token",
             headers: [ 'Authorization': "Basic ${authEncoded}" ],
             body: [grant_type: "password",
                    username: settings.particleUsername,
                    password: settings.particlePassword,
                    expires_in: 2592000]
            ) { response -> state.particleToken = response.data.access_token }
    log.debug("New Particle.io auth token obtained for $particleUsername")
}

def checkAndPopulateParticleToken() {
	log.debug("Checking particle token...")
	try {
        httpGet("${state.particleAPIUri}/devices?access_token=${state.particleToken}")
    }
    catch (groovyx.net.http.HttpResponseException ex) {
    	if (ex.response.status == 200) {
        	log.debug("Particle token ok")
        }
    	else if (ex.response.status == 401) {
        	log.debug("Particle returned 401 Unauthorized. Removing old token...")
            deleteAccessToken()
            log.debug("Attempting to populate new particleToken...")
            try {
            	populateParticleToken()
                log.debug("Success!")
            }
            catch (all) {
            	log.debug("Failed!")
            }
        }
    }
}

def populateSmartThingsAccessToken() {
	if (!state.accessToken) {
    	createAccessToken()
        log.debug("Created access token")
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"    
	state.appURL = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/${state.particleEventName}?access_token=${state.accessToken}"
	initialize()
    checkWebhook()
}

def updated() {
    unsubscribe()
    initialize()
    checkWebhook()
}

def uninstalled() {
    log.debug "Uninstalling Photon RF Gateway"
    deleteWebhook()
    deleteAccessToken()
    deleteChildDevices()
    log.debug "Photon RF Gateway Uninstalled"
}

def initialize() {
	for (int i = 0; i < state.maxNumDevices; i++) {
    	def name = "device${i}"
        def label = settings."device${i}Name"
        def type = settings."device${i}Type"
    	def attributes = [:]
        attributes.code = settings."device${i}Code"
        attributes.code2 = settings."device${i}Code2"

        if (label != null && type != null && attributes.code != null) {
            def deviceType
        	switch(type) {
                case "Motion Sensor":
                    deviceType = "433 Motion"
                    break
                case "Outlet":
                	deviceType = "433 Outlet"
                    break
            }
            
            if (!deviceType) {
            	log.debug "No device type ${type}"
            	continue
            }

            def d = getChildDevice(name)
            if (d && !label.equals(d.label)) {
            	log.debug "Device name changed from ${d.label} to ${label}, renaming"
            	d.rename(label)
            }
            if (!d) {
            	log.debug "Creating device ${label}"
            	d = addChildDevice("gpete", deviceType, name, null, [label: label, name: name, completedSetup: true])
                try {
                	d.init(attributes)
                }
                catch (all) {
                	log.warn "Failed to initialize $name ($label)"
                }
            }
        }
        else if (getChildDevice(name)) {
        	log.debug "Removing cleared device ${name} (${label})"
        	deleteChildDevice(name)
        }
    }

    def devices = getChildDevices()
    devices.each { log.debug "Initialized device " + it.deviceNetworkId }
    runEvery30Minutes(checkAndPopulateParticleToken)
}

def parseIncomingData() {
	def data = new groovy.json.JsonSlurper().parseText(params.data)
    if (!data?.data) {
    	return
    }

    log.trace "Data packet: " + data
    def incomingCode = data.data
    for (int i = 0; i < state.maxNumDevices; i++) {
        def deviceName = settings."device${i}Name"
        def deviceType = settings."device${i}Type"
        def isCode1Matched = codesMatch(incomingCode, settings."device${i}Code")
        def isCode2Matched = codesMatch(incomingCode, settings."device${i}Code2")
        
    	if(isCode1Matched || isCode2Matched) {
        	log.trace "Matched ${incomingCode} to $deviceName"
            def d = getChildDevice("device${i}")
            
            switch (deviceType) {
            	case "Motion Sensor":
                	d?.activate()
            		scheduleEndMotion(i)
                    break
                case "Outlet":
                	if (isCode1Matched) {
                    	log.debug deviceName + " on button pressed"
                    	d?.sendOnEvent()
                    }
                    else if (isCode2Matched) {
                    	log.debug deviceName + " off button pressed"
                    	d?.sendOffEvent()
                    }
                    break
            }
        }
    }
}

def codesMatch(codeA, codeB) {
	def shortA = codeA?.size() > 8 && codeA[2..3].equals("00") ? codeA[8..codeA.size()-1] : codeA
    def shortB = codeB?.size() > 8 && codeB[2..3].equals("00") ? codeB[8..codeB.size()-1] : codeB
	return shortA?.equalsIgnoreCase(shortB)
}

def scheduleEndMotion(i) {
	if (!state.lastActiveTimes) {
    	log.debug "Initializing last active times array"
    	state.lastActiveTimes = []
    }
    
    def needsToBeScheduled = true
    for (time in state.lastActiveTimes) {
    	// Don't schedule if there is a last active time in the array that is less than twice the timeout
    	if (time && time + (settings.inactiveCheckTime * 1000) > now()) {
        	needsToBeScheduled = false
            break
        }
    }
    
    state.lastActiveTimes[i] = now()
    
    if (needsToBeScheduled) {
    	runIn(settings.inactiveCheckTime, handleInactive)
    }
}

def handleInactive() {
    log.debug "Running handleEndMotion"
    def reschedule = false
    for (int i = 0; i < state.lastActiveTimes?.size(); i++) {
        def lastActiveTime = state.lastActiveTimes[i]
        if (lastActiveTime && now() > lastActiveTime + (settings.inactiveCheckTime * 1000)) {
            state.lastActiveTimes[i] = null
            def d = getChildDevice("device${i}")
            d?.inactivate()
        }
        else if (lastActiveTime && !reschedule) {
            reschedule = true
        }
    }

    if (reschedule) {
        runIn(settings.inactiveCheckTime, handleInactive)
    }
}

void createWebhook() {
    log.debug("Creating a Particle webhook")

    httpPost(uri: "${state.particleAPIUri}/webhooks",
             body: [access_token: state.particleToken,
                    event: state.particleEventName,
                    url: state.appURL,
                    requestType: "POST",
                    mydevices: true]
            )
    {response ->
    	state.particleWebhookId = response?.data?.id
        log.debug "Created Particle Webhook, id: ${state.particleWebhookId}"
    }
}

void checkWebhook() {
	log.debug "Checking webhook..."
	if(!state.particleToken) {
    	log.warn "Trying to check webhook without a token"
        return
    }
    log.trace "Previous webhook id: ${state.particleWebhookId}"
    def foundHook = false
    httpGet(uri:"${state.particleAPIUri}/webhooks?access_token=${state.particleToken}") { response -> response.data.each
        { hook ->
            if (hook.id == state.particleWebhookId) {
                foundHook = true
                log.debug "Found existing webhook id: ${hook.id}"
            }
        }
    }
    if (!foundHook) {
        createWebhook()
    }
}

void deleteWebhook() {
    try {
        httpGet(uri:"${state.particleAPIUri}/webhooks?access_token=${state.particleToken}") { response -> response.data.each
            { hook ->
                if (hook.id == state.particleWebhookId) {
                    httpDelete(uri:"${state.particleAPIUri}/webhooks/${hook.id}?access_token=${state.particleToken}")
                    log.debug "Deleted the existing webhook with the id: ${hook.id} and the event name: ${state.particleEventName}"
                }
            }
        }
    }
    catch (all) {
        log.debug "Couldn't delete webhook: " + all
    }
}

void deleteAccessToken() {
    try {
        def authEncoded = "${settings.particleUsername.toLowerCase()}:${settings.particlePassword}".bytes.encodeBase64()
        def params = [
            uri: "${state.particleAPIUri}/access_tokens/${state.particleToken}",
            headers: [ 'Authorization': "Basic ${authEncoded}" ]
        ]

        httpDelete(params)
        log.debug "Deleted the existing Particle Access Token"
    }
    catch (all) {
        log.debug "Couldn't delete Particle Token: " + all
    }
}

def deleteChildDevices() {
	def delete = getChildDevices()
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def sendRFCommand(command) {
    try {
        httpPost(uri: "${state.particleAPIUri}/devices/${settings.particleDevice}/${state.particleSendCommandFunction}",
                 body: [access_token: state.particleToken,
                        args: command]
                ) { response -> log.debug "sendRFCommand response: " + response.data }
    }
    catch (all) {
        return "Failed to send command $command to Photon: " + all
    }
    return "Sent command " + command + " to the Photon"
}