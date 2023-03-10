/**
 *  Dash (Connect)
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
definition(
    name: "Dash (Connect)",
    namespace: "julienjoannic",
    author: "Julien JOANNIC",
    description: "Connect Dash to Smartthings",
    category: "Convenience",
    singleInstance: true,
    iconUrl: "https://lh6.ggpht.com/Ab32cDI1WYhyj0ac8ryTpE0YUMO56F4-KCN9sAONlrbLwU0IYxyMYU27DJKTSO14prs=w300-rw",
    iconX2Url: "https://lh6.ggpht.com/Ab32cDI1WYhyj0ac8ryTpE0YUMO56F4-KCN9sAONlrbLwU0IYxyMYU27DJKTSO14prs=w300-rw",
    iconX3Url: "https://lh6.ggpht.com/Ab32cDI1WYhyj0ac8ryTpE0YUMO56F4-KCN9sAONlrbLwU0IYxyMYU27DJKTSO14prs=w300-rw")

mappings {
    path("/oauth/initialize") { action: [GET: "oauthInitialize"]}
    path("/oauth/callback") { action: [GET: "oauthCallback"]}
    path("/event") { action: [POST: "onEvent"]}
}

preferences {
	page(name: "connect", title: "Dash (Connect)")
}

def connect() {
	// Create access token if needed
    if (!state.accessToken) {
    	log.debug "Creating access token"
    	createAccessToken()
    	log.debug "Access token created: ${state.accessToken}"
    }
    
    if (!state.authToken) {
    	// Authorization needed
        // Redirect URL for the OAuth initialization
        def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"

        // Authorization page
        return dynamicPage(name: "connect", install: false, uninstall: false) {
            section() {
                paragraph "To connect Dash to Smartthings, you first need to signup for the developper API and enter the client ID and secret assigned to you. Then, you can authorize Smartthings to access the Dash API."
                input "clientId", "text", title: "Client ID", submitOnChange: true
                input "clientSecret", "text", title: "Client Secret", submitOnChange: true
                href url: redirectUrl, style: "external", required: true, title: "Connect to Dash", description: "Tap to authorize Smartthings"
            }
        }
    }
    else {
        def options = [:]
        try {
    		def userUrl = "https://dash.by/api/chassis/v1/user"
        	log.debug "Querying ${userUrl}"
            
            httpGet(uri: userUrl, contentType: "application/json", headers: ["Authorization": "Bearer ${state.authToken}"]) { resp ->
                log.debug "Received /user response: ${resp.data}"
                state.vehicles = resp.data.vehicles
                state.vehicles.each { vehicle ->
                    options[vehicle.id] = "${vehicle.name} (${vehicle.year} ${vehicle.make} ${vehicle.model})"
                }
            }
        }
        catch (e) {
        	log.error e
        }
    
    	// Configuration
        return dynamicPage(name: "connect", install: true, uninstall: true) {
        	section() {
            	input "selectedVehicles", "enum", title: "Vehicles", options: options, multiple:true, required: false
            }
        }
    }
}

def oauthInitialize() {
	// Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    state.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
        response_type: "code",
        scope: "user",
        client_id: clientId,
        state: state.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]
	
    def url = "https://dash.by/api/auth/authorize?${toQueryString(oauthParams)}"
    log.debug "Redirecting user to ${url}"
    redirect(location: url)
}

def oauthCallback() {
	log.debug "OAuth callback: $params"
    log.debug "Expected state: ${state.oauthInitState}"
    log.debug "Received state: ${params.state}"
    
    if (params.state == state.oauthInitState) {
    	def tokenParams = [
        	"client_id": clientId,
            "client_secret": clientSecret,
            "code": params.code,
            "grant_type": "authorization_code"
        ]
        def tokenUrl = "https://dash.by/api/auth/token"
        log.debug "Querying ${tokenUrl}"
        
    	httpPostJson(uri: tokenUrl, body: tokenParams) { resp ->
        	log.debug "Received /token response: ${resp.data}"
        	state.authToken = resp.data.access_token
        }
        
        if (state.authToken) {
        	displayMessageAsHtml("<p>Your Smartthings account is now connected to Dash!</p><p>Close the browser to get back to the Smartthings app and finish the configuration.</p>")
        }
        else {
        	displayMessageAsHtml("<p>There was an error connecting your Dash account to Smartthings.</p><p>Please try again.</p>")
        }
    }
    else {
    	log.warn "Different OAuth state received, aborting authorization procedure"
    }
}

def updateVehicles() {
	selectedVehicles.each { vehicleId ->
    	def device = getChildDevices()?.find { it.deviceNetworkId == vehicleId }
        if (!device) {
        	def vehicle = state.vehicles.find { it.id == vehicleId }
            log.debug "Creating device for vehicle ${vehicleId} : ${vehicle}"
            device = addChildDevice("julienjoannic", "Car (Dash)", vehicleId, null, [label: vehicle.name])
            device.sendEvent(name: "switch", value: "unknown")
        }
    }
    
    getChildDevices()?.each { device ->
    	if (!selectedVehicles?.contains(device.deviceNetworkId)) {
        	log.debug "Deleting device for vehicle ${device}"
        	deleteChildDevice(device.deviceNetworkId)
        }
    }
}

def registerWebhook() {
    def webhookUrl = "${getApiServerUrl()}/api/token/${state.accessToken}/smartapps/installations/${app.id}/event"
    log.debug "Registering webhook with callback URL ${webhookUrl}"
    
	def body = [
        authClientId: clientId,
        secret: clientSecret,
        webHookUrl: webhookUrl.toString(),
        eventTypes: ["ignition_on", "ignition_off"]
    ]
    def url = "https://dash.by/api/authclient/webhooks/register"
    log.debug "Querying ${url} with body ${body}"

    httpPostJson(uri: url, body: body) { resp ->
        log.debug "Received /register response: ${resp.data}"
    }
}

def onEvent() {
	log.debug "Received event: ${params}"
    log.debug "Content: ${request?.JSON}"
    
    if (request.JSON.eventType.startsWith("ignition")) {
    	def device = getChildDevices()?.find { it.deviceNetworkId == request.JSON.payload.vehicleId }
        log.debug "Received event ${request.JSON.eventType} for device ${device}"
        if (device) {
        	device.sendEvent(name: "switch", value: request.JSON.eventType.split("_")[1])
        }
    }
}

def initialize() {
	updateVehicles()
   	if (selectedVehicles) {
    	registerWebhook()
    }
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