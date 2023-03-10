/**
 *  Xee (Connect)
 *
 *  Copyright 2017 Julien JOANNIC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
include 'asynchttp_v1'
 
definition(
    name: "Xee (Connect)",
    namespace: "julienjoannic",
    author: "Julien JOANNIC",
    description: "Integration with Xee.",
    category: "Convenience",
    singleInstance: true,
    iconUrl: "https://jdmc.info/system/exhibitors/logos/000/000/006/medium/Logo_Xee.png",
    iconX2Url: "https://jdmc.info/system/exhibitors/logos/000/000/006/medium/Logo_Xee.png",
    iconX3Url: "https://jdmc.info/system/exhibitors/logos/000/000/006/medium/Logo_Xee.png") {
    
    appSetting "clientId"
    appSetting "clientSecret"
    appSetting "clientIdV3"
    appSetting "clientSecretV3"
}

mappings {
    path("/oauth/initialize") { action: [GET: "oauthInitialize"]}
    path("/oauth/callback") { action: [GET: "oauthCallback"]}
    path("/event") { action: [POST: "onEvent"]}
}

preferences {
	page(name: "connect", title: "Xee (Connect)")
}

def getServer() {
	return (useSandbox) ? "https://sandbox.xee.com/v3" : "https://api.xee.com/v4" // V4
	//return (useSandbox) ? "https://sandbox.xee.com/v3" : "https://cloud.xee.com/v3" // V3
}

def connect() {
	// Create access token if needed
    if (!state.accessToken) {
    	log.debug "Creating access token"
    	createAccessToken()
    	log.debug "Access token created: ${state.accessToken}"
    }
    
    def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    if (!state.authToken) {
    	// Authorization needed
        // Redirect URL for the OAuth initialization

        // Authorization page
        return dynamicPage(name: "connect", install: false, uninstall: false) {
            section() {
                paragraph "Tap the link below to connect Xee to Smartthings. You will be redirected to the Xee login page where you will be have to authorize Smartthings to access your data."
                input "useSandbox", "bool", title: "Use the sandbox server"
                href url: redirectUrl, style: "external", required: true, title: "Connect to Xee", description: "Tap to authorize Smartthings"
            }
        }
    }
    else {
        def options = [:]
        try {
            //def url = "${getServer()}/users/${useSandbox ? 1 : "me"}/cars" // V3
    		def url = "${getServer()}/users/${useSandbox ? 1 : "me"}/vehicles" // V4
        	log.debug "Querying ${url}"
            
            httpGet(uri: url, contentType: "application/json", headers: ["Authorization": "Bearer ${state.authToken}"]) { resp ->
                log.debug "Received ${url} response: ${resp.data}"
                state.cars = resp.data
                state.cars.each { car ->
                    options[car.id] = "${car.name} (${car.brand} ${car.model} ${car.licensePlate} )"
                }
            }
            
            if (deviceId && devicePin) {
            	log.debug "Trying to register device with ID ${deviceId} and PIN ${devicePin}"
                url = "${getServer()}/users/me/vehicles"
                def body = [deviceId: deviceId, devicePin: devicePin]
                httpPostJson(uri: url, headers: ["Authorization": "Bearer ${state.authToken}"], body: body) { resp ->
                	log.debug(resp);
                    deviceId = ""
                    devicePin = ""
                }
            }
        }
        catch (e) {
        	log.error e
        }
    
    	// Configuration
        return dynamicPage(name: "connect", install: true, uninstall: true) {
        	section("Vehicles") {
            	input "selectedCars", "enum", title: "Cars", options: options, multiple:true, required: false
            }
            section("Add device") {
            	input "deviceId", "string", title: "Device ID", required: false, submitOnChange: true
                input "devicePin", "string", title: "Device PIN", required: false, submitOnChange: true
            }
            section("Auth") {
                paragraph "Tap the link below to connect Xee to Smartthings. You will be redirected to the Xee login page where you will be have to authorize Smartthings to access your data."
                input "useSandbox", "bool", title: "Use the sandbox server"
                href url: redirectUrl, style: "external", required: true, title: "Connect to Xee", description: "Tap to authorize Smartthings"
            }
        }
    }
}

def oauthInitialize() {
	// Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    state.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
    	//client_id: appSettings.clientIdV3, // V3
        client_id: appSettings.clientId, // V4
        state: state.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]
	
    // V3
    //def scope = "users_read cars_read"
    //def url = "https://cloud.xee.com/v3/auth/auth?client_id=${appSettings.clientIdV3}&redirect_uri=${oauthParams.redirect_uri}&state=${state.oauthInitState}&response_type=code&scope=${scope}"
    // V4
    def scope = "account.read vehicles.read vehicles.signals.read vehicles.locations.read vehicles.loans.read vehicles.trips.read vehicles.accelerometers.read"
    def url = "https://api.xee.com/v4/oauth/authorize?client_id=${appSettings.clientId}&redirect_uri=${oauthParams.redirect_uri}&state=${state.oauthInitState}&response_type=code&scope=${scope}"
    log.debug "Redirecting user to ${url}"
    redirect(location: url)
}

def oauthCallback() {
	log.debug "OAuth callback: $params"
    log.debug "Expected state: ${state.oauthInitState}"
    log.debug "Received state: ${params.state}"
    
    if (params.state == state.oauthInitState) {
    	refreshToken(params.code)
        
        if (state.authToken) {
        	displayMessageAsHtml("<p>Your Smartthings account is now connected to Xee!</p><p>Close the browser to get back to the Smartthings app and finish the configuration.</p>")
        }
        else {
        	displayMessageAsHtml("<p>There was an error connecting your Xee account to Smartthings.</p><p>Please try again.</p>")
        }
    }
    else {
    	log.warn "Different OAuth state received, aborting authorization procedure"
    }
}

def refreshToken(code) {
	//def authorization = "${appSettings.clientIdV3}:${appSettings.clientSecretV3}".bytes.encodeBase64(); // V3
	def authorization = "${appSettings.clientId}:${appSettings.clientSecret}".bytes.encodeBase64(); // V4
    def body = (code) ? "grant_type=authorization_code&code=${code}" : "grant_type=refresh_token&refresh_token=${state.refreshToken}"
    def headers = ["Authorization": "Basic ${authorization}", "Content-Type": "application/x-www-form-urlencoded"]
    //def url = "https://cloud.xee.com/v3/auth/access_token" // V3
    def url = "https://api.xee.com/v4/oauth/token" // V4
    log.debug "Querying ${url} with headers ${headers} and body ${body}"

    httpPost(uri: url, headers: headers, body: body) { resp ->
    	if (resp.status == 200) {
        	log.debug "Received /auth/access_token response: ${resp.data}"
        	state.authToken = resp.data.access_token
        	state.refreshToken = resp.data.refresh_token
            
            return true
        }
        else {
        	return false
        }
    }
}

def updateCars() {
	selectedCars.each { carId ->
    	def device = getChildDevices()?.find { it.deviceNetworkId == carId }
        if (!device) {
        	log.debug "Finding car ${carId} in ${state.cars}"
        	def car = state.cars.find { car -> car.id.toString() == carId.toString() }
            log.debug "Creating device for car ${carId} : ${car}"
            device = addChildDevice("julienjoannic", "Car (Xee)", carId, null, [label: car.name])
            device.sendEvent(name: "switch", value: "unknown")
        }
    }
    
    getChildDevices()?.each { device ->
    	if (!selectedCars?.contains(device.deviceNetworkId)) {
        	log.debug "Deleting device for car ${device}"
        	deleteChildDevice(device.deviceNetworkId)
        }
    }
}

def getOnOffMap() {
	return [0: "off", 1: "on"]
}

def poll() {
	getChildDevices()?.each { device ->
    	device.update(poll(device.deviceNetworkId))
    }
}

def poll(dni) {
	//def url = "${getServer()}/cars/${dni}/status" // V3
	def url = "${getServer()}/vehicles/${dni}/status" // V4
    log.debug "Querying ${url}"
    
    try {
        httpGet(uri: url, headers: [Authorization: "Bearer ${state.authToken}", contentType: "application/json"]) { response ->

            log.debug "Received response ${response.properties}"
            return response.data
        }
    }
    catch (e) {
    	log.debug "Error: ${e}"
        if (refreshToken()) poll(dni);
    }
}

def initialize() {
	updateCars()
    poll()
    
    if (selectedCars) runEvery1Minute(poll)
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

String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def displayMessageAsHtml(message) {
    def html = """
        <!DOCTYPE html>
        <html>
            <head>
            </head>
            <body>
                <div>
                    ${message}
                </div>
            </body>
        </html>
    """
    render contentType: 'text/html', data: html
}