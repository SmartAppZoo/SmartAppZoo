/**
 * Netatmo Connect Date: 01.12.2017
 */

import java.text.DecimalFormat
import groovy.json.JsonSlurper

private getApiUrl()			{ "https://api.netatmo.com" }
private getVendorName()		{ "netatmo" }
private getVendorAuthPath()	{ "${apiUrl}/oauth2/authorize?" }
private getVendorTokenPath(){ "${apiUrl}/oauth2/token" }
private getVendorIcon()		{ "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1%402x.png" }
private getClientId()		{ appSettings.clientId }
private getClientSecret()	{ appSettings.clientSecret }
private getServerUrl() 		{ appSettings.serverUrl }
private getShardUrl()		{ return getApiServerUrl() }
private getCallbackUrl()	{ "${serverUrl}/oauth/callback" }
private getBuildRedirectUrl() { "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${shardUrl}" }

// Automatically generated. Make future change here.
definition(
	name: "Netatmo (Connect) Thermostat",
	namespace: "danyulsan91",
	author: "Brian Steere, Daniel Jackson",
	description: "Netatmo Integration for the Netatmo Thermostat",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/netamo-icon-1%402x.png",
	oauth: true,
	singleInstance: true
){
	appSetting "clientId"
	appSetting "clientSecret"
	appSetting "serverUrl"
}

preferences {
	page(name: "Credentials", title: "Fetch OAuth2 Credentials", content: "authPage", install: false)
	page(name: "listDevices", title: "Netatmo Devices", content: "listDevices", install: false)
}

mappings {
	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
	path("/oauth/callback") {action: [GET: "callback"]}
}

def authPage() {
	log.debug "In authPage"

	def description
	def uninstallAllowed = false
	def oauthTokenProvided = false

	if (!state.accessToken) {
		log.debug "About to create access token."
		state.accessToken = createAccessToken()
	}

	if (canInstallLabs()) {

		def redirectUrl = getBuildRedirectUrl()
		// log.debug "Redirect url = ${redirectUrl}"

		if (state.authToken) {
			description = "Tap 'Next' to proceed"
			uninstallAllowed = true
			oauthTokenProvided = true
		} else {
			description = "Click to enter Credentials."
		}

		if (!oauthTokenProvided) {
			log.debug "Showing the login page"
			return dynamicPage(name: "Credentials", title: "Authorize Connection", nextPage:"listDevices", uninstall: uninstallAllowed, install:false) {
				section() {
					paragraph "Tap below to log in to Netatmo and authorize SmartThings access."
					href url:redirectUrl, style:"embedded", required:false, title:"Connect to ${getVendorName()}:", description:description
				}
			}
		} else {
			log.debug "Showing the devices page"
			return dynamicPage(name: "Credentials", title: "Connected", nextPage:"listDevices", uninstall: uninstallAllowed, install:false) {
				section() {
					input(name:"Devices", style:"embedded", required:false, title:"Netatmo is now connected to SmartThings!", description:description) 
				}
			}
		}
	} else {
		def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""


		return dynamicPage(name:"Credentials", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section {
				paragraph "$upgradeNeeded"
			}
		}

	}
}

def oauthInitUrl() {
	log.debug "In oauthInitUrl"

	state.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
		response_type: "code",
		client_id: getClientId(),
		client_secret: getClientSecret(),
		state: state.oauthInitState,
		redirect_uri: getCallbackUrl(),
		scope: "read_thermostat"
	]

	// log.debug "REDIRECT URL: ${getVendorAuthPath() + toQueryString(oauthParams)}"

	redirect (location: getVendorAuthPath() + toQueryString(oauthParams))
}

def callback() {
	// log.debug "callback()>> params: $params, params.code ${params.code}"

	def code = params.code
	def oauthState = params.state

	if (oauthState == state.oauthInitState) {

		def tokenParams = [
			client_secret: getClientSecret(),
			client_id : getClientId(),
			grant_type: "authorization_code",
			redirect_uri: getCallbackUrl(),
			code: code,
			scope: "read_thermostat"
		]

		// log.debug "TOKEN URL: ${getVendorTokenPath() + toQueryString(tokenParams)}"

		def tokenUrl = getVendorTokenPath()
		def params = [
			uri: tokenUrl,
			contentType: 'application/x-www-form-urlencoded',
			body: tokenParams
		]

		// log.debug "PARAMS: ${params}"

		httpPost(params) { resp ->

			def slurper = new JsonSlurper()

			resp.data.each { key, value ->
				def data = slurper.parseText(key)

				state.refreshToken = data.refresh_token
				state.authToken = data.access_token
				state.tokenExpires = now() + (data.expires_in * 1000)
				// log.debug "swapped token: $resp.data"
			}
		}

		// Handle success and failure here, and render stuff accordingly
		if (state.authToken) {
			success()
		} else {
			fail()
		}

	} else {
		log.error "callback() failed oauthState != state.oauthInitState"
	}
}

def success() {
	log.debug "OAuth flow succeeded"
	def message = """
	<p>We have located your """ + getVendorName() + """ account.</p>
	<p>Tap 'Done' to continue to Devices.</p>
	"""
	connectionStatus(message)
}

def fail() {
	log.debug "OAuth flow failed"
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
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<title>${getVendorName()} Connection</title>
		<style type="text/css">
			* { box-sizing: border-box; }
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
				width: 100%;
				padding: 40px;
				/*background: #eee;*/
				text-align: center;
			}
			img {
				vertical-align: middle;
			}
			img:nth-child(2) {
				margin: 0 30px;
			}
			p {
				font-size: 2.2em;
				font-family: 'Swiss 721 W01 Thin';
				text-align: center;
				color: #666666;
				margin-bottom: 0;
			}
			/*
			p:last-child {
				margin-top: 0px;
			}
			*/
			span {
				font-family: 'Swiss 721 W01 Light';
				}
		</style>
		</head>
		<body>
			<div class="container">
				<img src=""" + getVendorIcon() + """ alt="Vendor icon" />
				<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
				<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
				${message}
			</div>
        </body>
        </html>
	"""
	render contentType: 'text/html', data: html
}

def refreshToken() {
	log.debug "In refreshToken"

	def oauthParams = [
		client_secret: getClientSecret(),
		client_id: getClientId(),
		grant_type: "refresh_token",
		refresh_token: state.refreshToken
	]

	def tokenUrl = getVendorTokenPath()
	def params = [
		uri: tokenUrl,
		contentType: 'application/x-www-form-urlencoded',
		body: oauthParams,
	]

	// OAuth Step 2: Request access token with our client Secret and OAuth "Code"
	try {
		httpPost(params) { response ->
			def slurper = new JsonSlurper();

			response.data.each {key, value ->
				def data = slurper.parseText(key);
				// log.debug "Data: $data"

				state.refreshToken = data.refresh_token
				state.accessToken = data.access_token
				state.tokenExpires = now() + (data.expires_in * 1000)
				return true
			}

		}
	} catch (Exception e) {
		log.debug "Error: $e"
	}

	// We didn't get an access token
	if ( !state.accessToken ) {
		return false
	}
}

String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
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
		def detail = state?.deviceDetail[deviceId]

		try {
			switch(detail?.type) {
				case 'NAPlug':
					log.debug "Creating Relay"
					createChildDevice("Netatmo Thermostat Relay", deviceId, "${detail.type}.${deviceId}", detail.module_name)
					break
				case 'NATherm1':
					log.debug "Creating Thermostat"
					createChildDevice("Netatmo Thermostat", deviceId, "${detail.type}.${deviceId}", detail.module_name)
					break
			}
		} catch (Exception e) {
			log.error "Error creating device: ${e}"
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
	log.debug "Refreshing thermostat data"
def deviceList = [:]
def moduleName = null
state.deviceDetail = [:]
state.deviceState = [:]

apiGet("/api/getthermostatsdata",["get_favorites":true]) { resp ->
    	state.response = resp.data.body
        resp.data.body.devices.each { value ->
            def key = value._id
            if (value.module_name != null) {
                deviceList[key] = "${value.station_name}: ${value.module_name}"
                state.deviceDetail[key] = value
                state.deviceState[key] = value.dashboard_data
                }

            value.modules.each { value2 ->            
                def key2 = value2._id

				if (value2.module_name != null) {
                    deviceList[key2] = "${value.station_name}: ${value2.module_name}"
                    state.deviceDetail[key2] = value2
                    state.deviceState[key2] = value2.dashboard_data
                    }
				else {
                    switch(value2.type) {
                    case "NAPlug":
                    	moduleName = "Relay ${value.station_name}"
                        break
                    case "NATherm1":
                    	moduleName = "Thermostat ${value.station_name}" 
                        break
                        }
              
                    deviceList[key2] = "${value.station_name}: ${moduleName}"
                    state.deviceDetail[key2] = value2 << ["module_name" : moduleName]
                    state.deviceState[key2] = value2.dashboard_data						
                	}
            }
        }
    }

return deviceList.sort() { it.value.toLowerCase() }

}


private removeChildDevices(delete) {
	log.debug "In removeChildDevices"

	log.debug "deleting ${delete.size()} devices"

	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def createChildDevice(deviceFile, dni, name, label) {
	log.debug "In createChildDevice"

	try {
		def existingDevice = getChildDevice(dni)
		if(!existingDevice) {
			log.debug "Creating child"
			def childDevice = addChildDevice("danyulsan91", deviceFile, dni, null, [name: name, label: label, completedSetup: true])
		} else {
			log.debug "Device $dni already exists"
		}
	} catch (e) {
		log.error "Error creating device: ${e}"
	}
}

def listDevices() {
	log.debug "Listing devices"

	def devices = getDeviceList()

	dynamicPage(name: "listDevices", title: "Choose devices", install: true) {
		section("Devices") {
			input "devices", "enum", title: "Select Device(s)", required: false, multiple: true, options: devices
		}

        section("Preferences") {
        	input "time", "enum", title: "Time Format", description: "Please select time format", required: true, options: [12:'12 Hour', 24:'24 Hour']
        }
	}
}

def apiGet(String path, Map query, Closure callback) {
	if(now() >= state.tokenExpires) {
		refreshToken();
	}

	query['access_token'] = state.accessToken
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
		// This is most likely due to an invalid token. Try to refresh it and try again.
		log.debug "apiGet: Call failed $e"
		if(refreshToken()) {
			log.debug "apiGet: Trying again after refreshing token"
			httpGet(params)	{ response ->
				callback.call(response)
			}
		}
	}
}

def apiGet(String path, Closure callback) {
	apiGet(path, [:], callback);
}

def poll() {
	log.debug "Polling"
	getDeviceList();
	def children = getChildDevices()
    //log.debug "State: ${state.deviceState}"
    //log.debug "Time Zone: ${location.timeZone}"
    
    if(location.timeZone == null)
       log.warn "Location is not set! Go to your ST app and set your location"

	settings.devices.each { deviceId ->
		def detail = state?.deviceDetail[deviceId]
		def data = state?.deviceState[deviceId]
		def child = children?.find { it.deviceNetworkId == deviceId }

		switch(detail?.type) {
			case 'NATherm1':
				log.debug "Updating Thermostat $data"
                // log.debug "$detail"
                
                // Declare Setpoint Mode and select the correct temp.
                def setpoint_mode = detail['setpoint'].find { it.key == "setpoint_mode" }.value.capitalize()
            	if(setpoint_mode == "Manual") {
                	child?.sendEvent(name: 'setpoint_temp', value: detail['setpoint'].find { it.key == "setpoint_temp" }.value, unit: "C")
                    child?.sendEvent(name: 'setpoint_endtime', value: formatTime(detail['setpoint'].find { it.key == "setpoint_endtime" }.value), unit: "") 
                }
                else {
                	child?.sendEvent(name: 'setpoint_temp', value: detail['measured'].find { it.key == "setpoint_temp" }.value, unit: "C")
                    child?.sendEvent(name: 'setpoint_endtime', value: "--", unit: "") 
                } 
				
                // Declare RF Status and send signal status to DH.
            	def rf_status = detail['rf_status']
                if(rf_status == 60) {
                    child?.sendEvent(name: 'rf_status', value: "Full", unit: "")
                }
                else if(rf_status > 60) {
                    child?.sendEvent(name: 'rf_status', value: "High", unit: "")
                }
                else if(rf_status >= 70) {
                    child?.sendEvent(name: 'rf_status', value: "Medium", unit: "")
                }
                else if(rf_status >= 80) {
                    child?.sendEvent(name: 'rf_status', value: "Low", unit: "")
                }                    
                
                // Send other details to Thermostat Device.
				child?.sendEvent(name: 'temperature', value: detail['measured'].find { it.key == "temperature" }.value, unit: "C")               
                child?.sendEvent(name: 'setpoint_mode', value: setpoint_mode)                 
                child?.sendEvent(name: 'last_updated', value: formatTime(detail['measured'].find { it.key == "time" }.value), unit: "")
                child?.sendEvent(name: 'battery', value: detail['battery_percent'], unit: "%")
				break;
		}
	}
}

def formatTime(time) {
    if(settings.time == '24') {
    def updtTime = new Date(time*1000L).format("HH:mm", location.timeZone)
    state.lastUpdated = updtTime
    return updtTime
    } else {
    def updtTime = new Date(time*1000L).format("h:mm aa", location.timeZone)
    state.lastUpdated = updtTime
    return updtTime
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