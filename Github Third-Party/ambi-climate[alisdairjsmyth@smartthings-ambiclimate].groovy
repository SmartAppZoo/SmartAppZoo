/**
 *  Ambi Climate
 *
 *  Copyright 2017 Alisdair Smyth
 *
 */
definition(
    name: "Ambi Climate",
    namespace: "alisdairjsmyth",
    author: "Alisdair Smyth",
    description: "SmartApp supporting control of Ambi Climate devices within SmartThings",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: false)
{
	appSetting "email"
    appSetting "password"
}

preferences {
    page(name:"deviceDiscovery", title:"Device Selection", content:"deviceDiscovery")
}

private deviceDiscovery() {
	def options = deviceOptions() ?: []
    def count   = options.size()

    return dynamicPage(name:"deviceDiscovery", title:"Device Selection", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: true) {
        section("Select Ambi Climate devices to add...") {
            input "selectedDevices", "enum", required:true, title:"Devices (${count} found)", multiple:true, options:options
        }
    }
}

Map deviceOptions() {
	def options = [:]
    def devices = getDevices()
    devices.each { device ->
    	options[device.mac] = device.room_name
    }
    log.debug("Options: ${options}")
    return options
}

def getDevices() {
    def response
    Map params

	// Authenticate
    params = [
   		uri: "https://rest.ambiclimate.com",
       	path: "/UserCredential",
        query: [
   	    	email: appSettings.email,
       	    pwd: appSettings.password
        ]
   	]
    
   	try {
   		httpGet(params) { resp ->
       		log.debug("Authentication Response: ${resp.data}")

            response = resp
   	    }
    } catch (groovyx.net.http.HttpResponseException e) {
		throw e
    }
    
    // Retrieve List of Ambi Climate Devices
    params = [
    	uri: "https://rest.ambiclimate.com",
        path: "/User",
        query: [
        	expand: "appliance%2Cdevice",
            user_id: response.data.user_id
        ],
        headers: [
        	Authorization: "Bearer ${response.data.token_id}"
        ]
    ]
    
    try {
    	httpGet(params) { resp ->
        	log.debug("Retrieve List Response: ${resp.data}")
            state.devices = resp.data.devices
            return resp.data.devices
        }
    } catch (groovyx.net.http.HttpResponseException e) {
		throw e
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    updateDevices()
    runEvery5Minutes('refresh')
}

def uninstalled() {
	// Delete Existing Child Device
    unschedule('refresh')
    getChildDevices().each {
    	log.info("Deleting ${it.deviceNetworkId}")
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updateDevices() {
    def selectors = []

	// For each selected device, see if it exists as a child device and create if necessary
	settings.selectedDevices.each { device ->
    	selectors.add("${device}")
        def stDevice = getChildDevice(device)
        def ambi = state.devices.find { it.mac == device };
        log.debug(ambi)
        if (!stDevice) {
        	log.debug("New device to be created: ${device}")
            stDevice = addChildDevice(app.namespace, "Ambi Climate", device, null, ["label": ambi.room_name])
        } else {
        	log.debug("Device already exists: ${device}")
        }
        stDevice.sendEvent(name: "temperature", value: Math.round(ambi.sensors["temperature"].data[0].value*100)/100)
        stDevice.sendEvent(name: "humidity", value: Math.round(ambi.sensors["humidity"].data[0].value*100)/100)
        log.debug(ambi.appliances[0].appliance_state.data[0].power)
        stDevice.sendEvent(name: "switch", value: (ambi.appliances[0].appliance_state.data[0].power == "Off") ? "off" : "on")
    }
    
    // Determine if there child device which don't reflect selections and delete
    getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
    	log.info("Deleting ${it.deviceNetworkId}")
        deleteChildDevice(it.deviceNetworkId)
    }
}

def refresh() {
	def devices = getDevices()
    updateDevices()
}