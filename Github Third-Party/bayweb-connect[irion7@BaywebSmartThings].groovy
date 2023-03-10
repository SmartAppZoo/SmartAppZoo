/**
 */

definition(
		name: "Bayweb (Connect)",
		namespace: "irion7",
		author: "Josh Leavitt",
		description: "Connect your Bayweb thermostat to SmartThings.",
		category: "Convenience",
		iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
		iconX2Url:  "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
		singleInstance: true
)

{
	appSetting "interfaceId"
    appSetting "keyCode"
}

preferences {
    page(name: "addBayWeb", title: "bayweb", nextPage:"", content:"", uninstall: true, install:true)
}

def addBayWeb() {
	log.debug "addBayWeb()"
    
	def showUninstall = username != null && password != null
	return dynamicPage(name: "addBayWeb", title: "Add your Bayweb device", nextPage:"confirmBayweb", uninstall:showUninstall) {
		section("Connect to your Bayweb device:") {
			input "deviceId", "text", title: "Device ID", required: true, autoCorrect:false
			input "keyCode", "text", title: "Key Code", required: true, autoCorrect:false
		}
		section("To use Bayweb please enter your Bayweb device ID and key Code.") {}
	}
}

def confirmBayweb() {
	log.debug "confirmBayweb()"
	if(deviceId && keyCode) {
    	def stats = getBaywebThermostats()
		log.debug "thermostat list: $stats"
    }
}

def success() {
	def message = """
        <p>Your bayweb Account is now connected to SmartThings!</p>
        <p>Click 'Done' to finish setup.</p>
    """
	connectionStatus(message)
}

def fail() {
	def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
	connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
	def redirectHtml = ""
	if (redirectUrl) {
		redirectHtml = """
			<meta http-equiv="refresh" content="3; url=${redirectUrl}" />
		"""
	}

	def html = """
        <!DOCTYPE html>
        <html>
            <head>
                <meta name="viewport" content="width=640">
                <title>Bayweb & SmartThings connection</title>
                <style type="text/css">
                    @font-face {
                        font-family: 'Swiss 721 W01 Thin';
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                        font-weight: normal;
                        font-style: normal;
                    }
                    @font-face {
                        font-family: 'Swiss 721 W01 Light';
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                        font-weight: normal;
                        font-style: normal;
                    }
                    .container {
                        width: 90%;
                        padding: 4%;
                        text-align: center;
                    }
                    img {
                        vertical-align: middle;
                    }
                    p {
                        font-size: 2.2em;
                        font-family: 'Swiss 721 W01 Thin';
                        text-align: center;
                        color: #666666;
                        padding: 0 40px;
                        margin-bottom: 0;
                    }
                    span {
                        font-family: 'Swiss 721 W01 Light';
                    }
                </style>
            </head>
        <body>
            <div class="container">
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/bayweb%402x.png" alt="bayweb icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
            </div>
        </body>
    </html>
    """

	render contentType: 'text/html', data: html
}

def getBaywebThermostats() {
	log.debug "getting device list"

	def deviceListParams = [
		uri: "https://api.bayweb.com/v2/?id=${deviceId}&key=${keyCode}&action=settings"
	]

	def stats = [:]
	try {
		httpGet(deviceListParams) { resp ->
			if (resp.status == 200) {
				resp.data.thermostatList.each { stat ->
					def dni = [app.id, stat.identifier].join('.')
					stats[dni] = "bayweb thermostat"
				}
			} else {
				log.debug "http status: ${resp.status}"
			}
		}
	} catch (groovyx.net.http.HttpResponseException e) {
        log.trace "Exception polling children: " + e.response.data.status
        if (e.response.data.status.code == 14) {
            atomicState.action = "getBaywebThermostats"
            log.debug "Refreshing your auth_token!"
            refreshAuthToken()
        }
    }
	atomicState.thermostats = stats
	return stats
}

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
    log.debug "initialize $deviceId $keyCode"
	if (deviceId && keyCode) {
		addDevice()
	}

	// Delete any that are no longer in settings
	def delete = getChildDevices().findAll { !baywebDevices }
	log.info "delete $delete"
    //removeChildDevices(delete)
}

//CHILD DEVICE METHODS
def addDevice() {
    def devices = getBaywebDevices()
    log.debug "AD - Devices $devices"
    log.debug "AD - Adding childs $devices - $deviceId"
	devices.each { dni, name ->
        log.debug "device info: $dni name: $name"
		def d = getChildDevice(dni)
		if(!d) {
			d = addChildDevice(app.namespace, "Bayweb Thermostat", dni, null, [name:"Bayweb", label:"Bayweb."+deviceId, completedSetup: true])
			log.trace "created ${d.name} with id $dni"
		} else {
			log.trace "found ${d.name} with id $dni already exists"
		}
	}
    
    //send activity feeds to tell that device is connected
	//def notificationMessage = "is connected to SmartThings"
	//sendActivityFeeds(notificationMessage)

	pollHandler() //first time polling data data from thermostat

	//automatically update devices status every 5 mins
	runEvery10Minutes("poll")
}

def pollHandler() {
	log.debug "pollHandler()"
	pollChildren(null) // Hit the bayweb API for update on all thermostats

	atomicState.thermostats.each {stat ->
		def dni = stat.key
		log.debug ("DNI = ${dni}")
		def d = getChildDevice(dni)
		if(d) {
			log.debug ("Found Child Device.")
			d.generateEvent(atomicState.thermostats[dni].data)
		}
	}
}

def pollChildren(child = null) {
    def thermostatIdsString = getChildDeviceIdsString()
    log.debug "polling children: $thermostatIdsString"

	def result = false

	def pollParams = [
        uri: "https://api.bayweb.com/v2/?id=${deviceId}&key=${keyCode}&action=data"
    ]

	try{
		httpGet(pollParams) { resp ->
			if(resp.status == 200) {
                log.debug "poll results returned resp.data ${resp.data}"
                storeThermostatData(resp.data)
                result = true
                log.debug "updated ${atomicState.thermostats?.size()} stats: ${atomicState.thermostats}"
            }
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		log.trace "Exception polling children: " + e.response.data.status
        if (e.response.data.status.code == 14) {
            atomicState.action = "pollChildren"
        }
	}
	return result
}

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
def pollChild() {
	def devices = getChildDevices()

	if (pollChildren()) {
		devices.each { child ->
			if(atomicState.thermostats[child.device.deviceNetworkId] != null) {
				def tData = atomicState.thermostats[child.device.deviceNetworkId]
				log.info "pollChild(child)>> data for ${child.device.deviceNetworkId} : ${tData.data}"
				child.generateEvent(tData.data) //parse received message from parent
			} else if(atomicState.thermostats[child.device.deviceNetworkId] == null) {
				log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId}"
				return null
			}
		}
	} else {
		log.info "ERROR: pollChildren()"
		return null
	}

}

void poll() {
	pollChild()
}

def getBaywebDevices() {
    log.trace "getBaywebDevices"
	def devices = []

	def apiParams = [
		uri: "https://api.bayweb.com/v2/?id=${deviceId}&key=${keyCode}&action=settings"
	]
    
    def stats = [:]
	httpGet(apiParams) { resp ->
		log.debug "Connecting to bayweb for device"
		if(resp.status == 200) {
        	log.debug "response $resp.data"
        	if(resp.data.deviceid == 1) {
        
        		state.deviceid == 1
                state.access == resp.data.access  //1 is readonly 2 is readwrite
                state.location == resp.data.location
                state.timezone == resp.data.timezone
                
                log.debug "state $state"
                
        		def dni = [app.id, deviceId].join('-')
            	//def dni = "bayweb" + "." + deviceId
 				def name = "Bayweb [${deviceId}]"
            	devices += ["name" : "${name}", "dni" : "${dni}"]
                
				stats[dni] = "bayweb thermostat"
            }
		} else if(resp.status == 302) {
			singleUrl = resp.headers.Location.value
		} else {
			log.error "bayweb connect: unknown response"
		}        
	}
    
    atomicState.thermostats = stats
    
    return stats
}

def availableModes(child) {
	debugEvent ("atomicState.thermostats = ${atomicState.thermostats}")
	debugEvent ("Child DNI = ${child.device.deviceNetworkId}")

	def tData = atomicState.thermostats[child.device.deviceNetworkId]

	debugEvent("Data = ${tData}")

	if(!tData) {
		log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling"
		return null
	}

	def modes = ["off"]

    if (tData.data.heatMode) {
        modes.add("heat")
    }
    if (tData.data.coolMode) {
        modes.add("cool")
    }
    if (tData.data.autoMode) {
        modes.add("auto")
    }

    return modes
}

def currentMode(child) {
	debugEvent ("atomicState.Thermos = ${atomicState.thermostats}")
	debugEvent ("Child DNI = ${child.device.deviceNetworkId}")

	def tData = atomicState.thermostats[child.device.deviceNetworkId]

	debugEvent("Data = ${tData}")

	if(!tData) {
		log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling"
		return null
	}

	def mode = tData.data.thermostatMode
	return mode
}

def getChildDeviceIdsString() {
	return thermostats.collect { it.split(/\./).last() }.join('-')
}

def toJson(Map m) {
    return groovy.json.JsonOutput.toJson(m)
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

boolean releaseHold(deviceId) {
	log.debug "releaseHold device $deviceId"
    def payload

   	payload="&hold=0"

    return sendCommandToBayweb(payload)
}

/**
 * Executes the set hold command on the Bayweb thermostat
 * @param heating - The heating temperature to set in fahrenheit
 * @param cooling - the cooling temperature to set in fahrenheit
 * @param deviceId - the ID of the device
 * @param sendHoldType - the hold type to execute
 *
 * @return true if the command was successful, false otherwise
 */
boolean setHold(heating, cooling, deviceId, sendHoldType) {
    log.debug "setHold heating $heating cooling $cooling $deviceId holdtype $sendHoldType"
	def payload
    
    payload="&heat_sp=$heating&cool_sp=$cooling&hold=1"
    
    return sendCommandToBayweb(payload)
}

/**
 * Executes the set fan mode command on the Bayweb thermostat
 * @param heating - The heating temperature to set in fahrenheit
 * @param cooling - the cooling temperature to set in fahrenheit
 * @param deviceId - the ID of the device
 * @param sendHoldType - the hold type to execute
 * @param fanMode - the fan mode to set to
 *
 * @return true if the command was successful, false otherwise
 */
boolean setFanMode(heating, cooling, deviceId, sendHoldType, fanMode) {
    log.debug "setFanMode heating $heating cooling $cooling $deviceId holdtype $sendHoldType - only setting fan mode"
    
	def payload
    
    if (fanMode == "auto")
    	payload = "&fan=0"
    else if (mode == "on")
    	payload = "&fan=1"   
    else {
    	log.debug "requested unsupported bayweb fan mode $fanMode - ignoring"
    	return false
    }
    
	return sendCommandToBayweb(payload)
}

/**
 * Sets the mode of the Bayweb thermostat
 * @param mode - the mode to set to - "auto" "emergency heat" "heat" "off" "cool"
 * @param deviceId - the ID of the device
 *
 * @return true if the command was successful, false otherwise
 */
boolean setMode(mode, deviceId) {
    def payload
    
    if (mode == "heat")
    	payload = "&mode=1"
    else if (mode == "cool")
    	payload = "&mode=2"  
    else if (mode == "off")
    	payload = "&mode=0"  
    else {
    	log.debug "requested unsupported bayweb mode $mode - ignoring"
    	return false
    }
	return sendCommandToBayweb(payload)
}

/**
 * Makes a request to the Bayweb API to actuate the thermostat.
 * Used by command methods to send commands to Bayweb.
 *
 * @param bodyParams - a map of request parameters to send to Bayweb.
 *
 * @return true if the command was accepted by Bayweb without error, false otherwise.
 */
private boolean sendCommandToBayweb(payload) {
	log.debug "sendCommandToBayweb $payload"
	def isSuccess = false
	def cmdParams = [
		uri: "https://api.bayweb.com/v2/?id=${deviceId}&key=${keyCode}&action=set${payload}"
	]

	try{
        httpPost(cmdParams) { resp ->
            if(resp.status == 200) {
                log.debug "updated ${resp.data}"
                def returnStatus = resp.data.status.code
                if (returnStatus == 0) {
                    log.debug "Successful call to bayweb API."
                    isSuccess = true
                } else {
                    log.debug "Error return code = ${returnStatus}"
                    debugEvent("Error return code = ${returnStatus}")
                }
            }
        }
	} catch (groovyx.net.http.HttpResponseException e) {
        log.trace "Exception Sending Json: " + e.response.data.status
        debugEvent ("sent Json & got http status ${e.statusCode} - ${e.response.data.status.code}")
        if (e.response.data.status.code == 14) {
            // TODO - figure out why we're setting the next action to be pollChildren
            // after refreshing auth token. Is it to keep UI in sync, or just copy/paste error?
            atomicState.action = "pollChildren"
            log.debug "Refreshing your auth_token!"
            refreshAuthToken()
        } else {
            debugEvent("Authentication error, invalid authentication method, lack of credentials, etc.")
            log.error "Authentication error, invalid authentication method, lack of credentials, etc."
        }
    }

    return isSuccess
}

def getChildName()           { return "Bayweb Thermostat" }
def getApiEndpoint()         { return "https://api.bayweb.com" }
def getSmartThingsClientId() { return appSettings.clientId }

def debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	log.debug "Generating AppDebug Event: ${results}"
	sendEvent (results)
}

//send both push notification and mobile activity feeds
def sendPushAndFeeds(notificationMessage) {
	log.warn "sendPushAndFeeds >> notificationMessage: ${notificationMessage}"
	log.warn "sendPushAndFeeds >> atomicState.timeSendPush: ${atomicState.timeSendPush}"
	if (atomicState.timeSendPush) {
		if (now() - atomicState.timeSendPush > 86400000) { // notification is sent to remind user once a day
			sendPush("Your Bayweb thermostat " + notificationMessage)
			sendActivityFeeds(notificationMessage)
			atomicState.timeSendPush = now()
		}
	} else {
		sendPush("Your Bayweb thermostat " + notificationMessage)
		sendActivityFeeds(notificationMessage)
		atomicState.timeSendPush = now()
	}
	atomicState.authToken = null
}

/**
 * Stores data about the thermostats in atomicState.
 * @param thermostats - a list of thermostats as returned from the Bayweb API
 */
private void storeThermostatData(thermostats) {
    log.trace "Storing thermostat data: $thermostats"
    def data
    def dni = [app.id, deviceId].join('-')
    def collector = [:]
    log.debug "updating dni $dni"
    
    //atomicState.thermostats = thermostats.inject([:]) { collector, stat ->
    //    log.debug "collector $collector"
    //    log.debug "stat $stat"
        
        data = [ /*
            timeStamp: stat.timestamp,
            coolMode: (stat.settings.coolStages > 0),
            heatMode: (stat.settings.heatStages > 0),
            deviceTemperatureUnit: stat.settings.useCelsius,
            minHeatingSetpoint: (stat.settings.heatRangeLow / 10),
            maxHeatingSetpoint: (stat.settings.heatRangeHigh / 10),
            minCoolingSetpoint: (stat.settings.coolRangeLow / 10),
            maxCoolingSetpoint: (stat.settings.coolRangeHigh / 10),
            autoMode: stat.settings.autoHeatCoolFeatureEnabled,
            deviceAlive: stat.runtime.connected == true ? "true" : "false",
            auxHeatMode: (stat.settings.hasHeatPump) && (stat.settings.hasForcedAir || stat.settings.hasElectric || stat.settings.hasBoiler),
            temperature: (stat.runtime.actualTemperature / 10),
            heatingSetpoint: stat.runtime.desiredHeat / 10,
            coolingSetpoint: stat.runtime.desiredCool / 10,
            thermostatMode: stat.settings.hvacMode,
            humidity: stat.runtime.actualHumidity,
            thermostatFanMode: stat.runtime.desiredFanMode */
            
            thermostatMode: thermostats.mode,
            auxHeatMode: "off",
            deviceAlive: "true",
            autoMode: "off",
            minHeatingSetpoint: 50,
            maxHeatingSetpoint: 80,
            minCoolingSetpoint: 68,
            maxCoolingSetpoint: 89,
        	timeStamp: thermostats.timestamp,
            coolMode: (thermostats.mode == 2),
            heatMode: (thermostats.mode == 1),
            temperature: thermostats.iat,
            heatingSetpoint: thermostats.sp,
            coolingSetpoint: thermostats.sp,
            thermostatSetpoint: thermostats.sp,
            thermostatMode: baywebModeCodeToModeName(thermostats.mode),
            humidity: thermostats.iah,
            thermostatFanMode: baywebFanModeCodeToModeName(thermostats.fan),
            outsideAirTemp: thermostats.oat,
            outsideHumidity: thermostats.oah,
            wind: thermostats.wind,
            solar: thermostats.solar,
            door: thermostats.door,
            relay_w2: thermostats.relay_w2,
            relay_y2: thermostats.relay_y2,
            in1: thermostats.in1,
            in2: thermostats.in2,
            in3: thermostats.in3,
            activity: thermostats.act,
            activitySetpoint: thermostats.act_sp,
            mode: thermostats.mode,
            hold: baywebHoldCodeToModeName(thermostats.hold),
            thermostatOperatingState: getThermostatOperatingState(thermostats)
        ]

        data["deviceTemperatureUnit"] = "F"
        collector[dni] = [data:data]
        //log.debug "returning collector $collector"
        //return collector
    //}
    
    
    atomicState.thermostats = collector
    log.debug "updated ${atomicState.thermostats?.size()} thermostats: ${atomicState.thermostats}"
}

//smartthings api states are: "auto" "emergency heat" "heat" "off" "cool"
def baywebHoldCodeToModeName(holdcode) {
	if(holdcode == 1)
    	return "on"
    else 
    	return "off"
}

//smartthings api states are: "auto" "emergency heat" "heat" "off" "cool"
def baywebModeCodeToModeName(modecode) {
	if(modecode == 0)
    	return "off"
    else if (modecode == 1)
    	return "heat"
    else if (modecode == 2)
    	return "cool"
    else 
    	return "off"    //Shouldn't do this
}

//smartthings api states are: "auto" "on" "circulate"
def baywebFanModeCodeToModeName(modecode) {
	if(modecode == 0)
    	return "auto"
    else if (modecode == 1)
    	return "on"
    else 
    	return "auto"    //Shouldn't do this
}

//smartthings api states are: "heating" "idle" "pending cool" "vent economizer" "cooling" "pending heat" "fan only"
def getThermostatOperatingState(statdata) {
	if( (statdata.mode == 1) && (statdata.fan == 1) )
    	return "heating"
    else if( (statdata.mode == 2) && (statdata.fan == 1) )
    	return "cooling"
    else if( (statdata.mode == 0) && (statdata.fan == 1) )
    	return "fan only"
    else
    	return "idle"
}

def sendActivityFeeds(notificationMessage) {
	def devices = getChildDevices()
	devices.each { child ->
		child.generateActivityFeedsEvent(notificationMessage) //parse received message from parent
	}
}

def convertFtoC (tempF) {
	return String.format("%.1f", (Math.round(((tempF - 32)*(5/9)) * 2))/2)
}