/**
 *  LaMetric (Connect) v2
 *
 *  Copyright 2020 Brent Maxwell
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
    name: "LaMetric (Connect) v2",
    namespace: "thebrent",
    author: "Brent Maxwell",
    description: "Connect to LaMetric Time",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "clientId",
    appSetting "clientSecret"
}

mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

preferences {
	section("Title") {
		// TODO: put inputs here
	}
    preferences {
 	   page(name: "Credentials", title: "Sample Authentication", content: "authPage", nextPage: "sampleLoggedInPage", install: false)
	}
}

def apiEndpoint() { return "https://developer.lametric.com/api/v2/" }

def authPage() {
    // Check to see if SmartApp has its own access token and create one if not.
    if(!state.accessToken) {
        // the createAccessToken() method will store the access token in state.accessToken
        createAccessToken()
    }

    def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    // Check to see if SmartThings already has an access token from the third-party service.
    if(!state.authToken) {
        return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall: false) {
            section() {
                paragraph "tap below to log in to the third-party service and authorize SmartThings access"
                href url: redirectUrl, style: "embedded", required: true, title: "3rd Party product", description: "Click to enter credentials"
            }
        }
    } else {
        // SmartThings has the token, so we can just call the third-party service to list our devices and select one to install.
    }
}

def oauthInitUrl() {

    // Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    state.oauthInitState = UUID.randomUUID().toString()
    
    def oauthParams = [
        response_type: "code",
        scope: "basic+devices_read+devices_write",
        client_id: appSettings.clientId,
        client_secret: appSettings.clientSecret,
        state: state.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]

    redirect(location: "${apiEndpoint()}/oauth2/authorize?${toQueryString(oauthParams)}")
}

def callback() {
    log.debug "callback()>> params: $params, params.code ${params.code}"

    def code = params.code
    def oauthState = params.state

    // Validate the response from the third party by making sure oauthState == state.oauthInitState as expected
    if (oauthState == state.oauthInitState){
        def tokenParams = [
            grant_type: "authorization_code",
            code      : code,
            client_id : appSettings.clientId,
            client_secret: appSettings.clientSecret,
            redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
        ]

        // This URL will be defined by the third party in their API documentation
        def tokenUrl = "${apiEndpoint()}oauth2/token?${toQueryString(tokenParams)}"

        httpPost(uri: tokenUrl) { resp ->
            state.refreshToken = resp.data.refresh_token
            state.authToken = resp.data.access_token
        }

        if (state.authToken) {
            // call some method that will render the successfully connected message
            success()
        } else {
            // gracefully handle failures
            fail()
        }

    } else {
        log.error "callback() failed. Validation of state did not match. oauthState != state.oauthInitState"
    }
}

// Example success method
def success() {
        def message = """
                <p>Your account is now connected to SmartThings!</p>
                <p>Click 'Done' to finish setup.</p>
        """
        displayMessageAsHtml(message)
}

// Example fail method
def fail() {
    def message = """
        <p>There was an error connecting your account with SmartThings</p>
        <p>Please try again.</p>
    """
    displayMessageAsHtml(message)
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

private refreshAuthToken() {
    def refreshParams = [
        method: 'POST',
        uri: "${apiEndpoint()}oauth2/token",
        path: "/token",
        query: [grant_type:'refresh_token', code:"${state.sampleRefreshToken}", client_id:XXXXXXX],
    ]
    try{
        def jsonMap
        httpPost(refreshParams) { resp ->
            if(resp.status == 200)
            {
                jsonMap = resp.data
                if (resp.data) {
                    state.sampleRefreshToken = resp?.data?.refresh_token
                    state.sampleAccessToken = resp?.data?.access_token
            	}
        	}
    	}
	}
    catch (e) {
    }
}

// The toQueryString implementation simply gathers everything in the passed in map and converts them to a string joined with the "&" character.
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
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers