/**
 * Telldus Connect
 */
import java.text.DecimalFormat
import groovy.json.JsonSlurper

private getApiUrl()			{ appSettings.ApiUrl }
private GetVendorName()     { "Telldus" }
private getVendorIcon()		{ "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1%402x.png" }

// Automatically generated. Make future change here.
definition(
	name: "Telldus (Proxy API Connect)",
	namespace: "mariussm",
	author: "Marius Solbakken",
	description: "Telldus Integration",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1%402x.png",
	oauth: false,
	singleInstance: true
){
	appSetting "ApiUrl"
	appSetting "ConsumerKey"
	appSetting "ConsumerSecret"
    appSetting "Token"
	appSetting "TokenSecret"
}

preferences {
	page(name: "listDevices", title: "Telldus Devices", content: "listDevices", install: false)
}

mappings {
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "Initialized with settings: ${settings}"

	// Pull the latest device info into state
	getDeviceList();

	settings.devices.each {
		def deviceId = it
        //log.debug "it: ${it}"
        log.debug "foreach: ${deviceId}"
		def detail = state.deviceDetail[deviceId] // TODO
		log.debug "detail: ${detail}"
        
        if(detail != null) {
            try {
                switch(detail.type) {
                    case 'sensor':
                        log.debug "Calling createChildDevice: ${detail}"
                        createChildDevice("Telldus Sensor", deviceId, detail.name, detail.id)
                        break
                    case 'switch':
                        log.debug "Calling createChildDevice: ${detail}"
                        createChildDevice("Telldus Switch", deviceId, detail.name, detail.id)
                        break
                    default:
                        log.debug "Unknown type: ${detail.type}"
                }
            } catch (Exception e) {
                log.error "Error creating device1: ${detail}: ${e}"
            }
        }
	}

	// Cleanup any other devices that need to go away
	def delete = getChildDevices().findAll { !settings.devices.contains(it.deviceNetworkId) }
	log.debug "Delete: $delete"
	delete.each { deleteChildDevice(it.deviceNetworkId) }

	// Do the initial poll
	poll()
	// Schedule it to run every 5 minutes
	runEvery5Minutes("poll")
}

def uninstalled() {
	log.debug "In uninstalled"
	removeChildDevices(getChildDevices())
}

def getDeviceList() {
	log.debug "In getDeviceList"

	def deviceList = [:]
	state.deviceDetail = [:]
	state.deviceState = [:]

	apiGet("api/sensor") { response -> 
        log.debug "api/sensor response: ${response.data}"
        response.data.sensor.each { value ->
            def key = "telldus:${value.id}"
            log.debug "key: ${key}"
            state.deviceDetail[key] = [:]
            state.deviceDetail[key]["name"] = value.name
            state.deviceDetail[key]["id"] = value.id
            state.deviceDetail[key]["type"] = "sensor"
            state.deviceState[key] = null
            deviceList[key] = "${value.name}"
        }
	}
    
    apiGet("api/device") { response -> 
        log.debug "api/device response: ${response.data}"
        response.data.device.each { value ->
            def key = "telldus:${value.id}"
            log.debug "key: ${key}"
            state.deviceDetail[key] = [:]
            state.deviceDetail[key]["name"] = value.name
            state.deviceDetail[key]["id"] = value.id
            
            
            state.deviceState[key] = null
            // TODO 19 (dimmer)
            log.debug "value.methods ${value.methods}"
            if(value.methods == 3) {
            	log.debug "adding switch"
            	state.deviceDetail[key]["type"] = "switch"
                deviceList[key] = "${value.name}"
            }
            
        }
	}

	return deviceList.sort() { it.value.toLowerCase() }
}

private removeChildDevices(delete) {
	log.debug "In removeChildDevices - deleting ${delete.size()} devices"
	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def createChildDevice(deviceFile, dni, name, telldusId) {
	log.debug "In createChildDevice"
	
    try {
		def existingDevice = getChildDevice(dni)
		if(!existingDevice) {
			log.debug "Creating child"
			def childDevice = addChildDevice("mariussm", deviceFile, dni, null, [name: name, telldusId: telldusId])
            childDevice.setTelldusID(telldusId)
		} else {
			log.debug "Device $dni already exists"
            existingDevice.setTelldusID(telldusId)
		}
	} catch (e) {
		log.error "Error creating device2: ${e}"
	}
}

def listDevices() {
	log.debug "In listDevices"

	def devices = getDeviceList()

	dynamicPage(name: "listDevices", title: "Choose devices", install: true) {
		section("Devices") {
			input "devices", "enum", title: "Select Device(s)", required: false, multiple: true, options: devices
		}

        /*section("Preferences") {
        	input "rainUnits", "enum", title: "Rain Units", description: "Millimeters (mm) or Inches (in)", required: true, options: [mm:'Millimeters', in:'Inches']
        }*/
	}
}

def apiGet(String path, Map query, Closure callback) {
	query['ConsumerKey'] = appSettings.ConsumerKey
    query['ConsumerSecret'] = appSettings.ConsumerSecret
    query['Token'] = appSettings.Token
    query['TokenSecret'] = appSettings.TokenSecret
    
	def params = [
		uri: getApiUrl(),
		path: path,
		'query': query
	]
	// log.debug "API Get: $params"

	try {
		httpGet(params)	{ response ->
			callback.call(response)
		}
	} catch (Exception e) {
		log.debug "apiGet: Call failed $e"
	}
}

def apiGet(String path, Closure callback) {
	apiGet(path, [:], callback);
}


def apiPost(String path, Map query, Closure callback) {
	query['ConsumerKey'] = appSettings.ConsumerKey
    query['ConsumerSecret'] = appSettings.ConsumerSecret
    query['Token'] = appSettings.Token
    query['TokenSecret'] = appSettings.TokenSecret
    
	def params = [
		uri: getApiUrl(),
		path: path,
		'query': query
	]
	// log.debug "API Get: $params"

	try {
		httpPost(params)	{ response ->
			callback.call(response)
		}
	} catch (Exception e) {
		log.debug "apiPost: Call failed $e"
	}
}

def apiPost(String path, Closure callback) {
	apiPost(path, [:], callback);
}

def poll() {
	log.debug "In Poll"
    
    def children = getChildDevices()
    log.debug "State: ${state.deviceState}"
    
    // Get state of switches and dimmers
    def deviceState = [:]
    apiGet("api/device") { response -> 
        log.debug "api/device response2: ${response.data}"
        response.data.device.each { value ->
            def key = "${value.id}"
            log.debug "key: ${key}"
            deviceState[key] = [:]
            deviceState[key]["state"] = value.state
            deviceState[key]["id"] = value.id
            deviceState[key]["statevalue"] = value.statevalue
        }
	}
    log.debug "poll deviceState: ${deviceState}"

	settings.devices.each { deviceId ->
		def detail = state?.deviceDetail[deviceId]
        if(detail != null) {
            log.debug "poll detail: ${detail}"

			if(detail.type == "sensor") {
                def data = [:]
                data['Temperature'] = null
                data['Humidity'] = null

                def httpparams = [:]
                httpparams['Id'] = detail.id
                apiGet("api/sensordata", httpparams) { response -> 
                    log.debug "api/sensordata response data: ${response.data}"
                    response.data.data.each { value ->
                        log.debug "value: ${value}"
                        if(value.name == "humidity") {
                            data['Humidity'] = value.value
                        }
                        if(value.name == "temp") {
                            data['Temperature'] = value.value
                        }
                    }
                }

				log.debug "sensor data: ${data}"
                def child = children?.find { it.deviceNetworkId == deviceId }
				child?.sendEvent(name: 'temperature', value: cToPref(data['Temperature']) as float, unit: getTemperatureScale())
                //child?.sendEvent(name: 'carbonDioxide', value: data['CO2'])
                child?.sendEvent(name: 'humidity', value: data['Humidity'])
                //child?.sendEvent(name: 'pressure', value: data['Pressure'])
                //child?.sendEvent(name: 'noise', value: data['Noise'])
            } else if(detail.type == "switch") {
            	log.debug "processing switch ${detail.id}"
                def child = children?.find { it.deviceNetworkId == deviceId }
            	if(deviceState[detail.id].state == 1) {
                	log.debug "switch ${detail.id} is on"
                    child?.sendEvent(name: 'switch', value: "on")
                } else {
                	log.debug "switch ${detail.id} is off"
                    child?.sendEvent(name: 'switch', value: "off")
                }
                
                //TODO
            } else {
            	log.debug "Unknown type: ${detail.type}"
            }
        }
	}
}

def setSwitchState(child, telldusId, state) {
    def httpparams = [:]
	httpparams['id'] = telldusId
    httpparams['state'] = state
    try {
        apiPost("api/device", httpparams) { response -> 
            log.debug "setSwitchState result: temp"
        }
    }  catch (Exception e) {
    }
    
    child.sendEvent(name: 'switch', value: state)
    
    //poll()
}

def cToPref(temp) {
	if(getTemperatureScale() == 'C') {
    	return temp
    } else {
		return temp * 1.8 + 32
    }
}

def rainToPref(rain) {
	if(settings.rainUnits == 'mm') {
    	return rain
    } else {
    	return rain * 0.039370
    }
}

def debugEvent(message, displayEvent) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	log.debug "Generating AppDebug Event: ${results}"
	sendEvent (results)

}

private Boolean canInstallLabs() {
	return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware) {
	return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions() {
	return location.hubs*.firmwareVersionString.findAll { it }
}