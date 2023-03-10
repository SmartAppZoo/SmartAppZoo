/**
 *  NIBE Uplink
 *
 *  Copyright 2017 Petter Arnqvist Eriksson
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
    name: "Nibe Uplink",
    namespace: "arnqvist",
    author: "Petter Arnqvist Eriksson",
    description: "Nibe connection",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    oauth: true) {
    appSetting "clientId"
    appSetting "clientSecret"
}

mappings {
	path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
	path("/oauth/callback") {action: [GET: "callback"]}
}

preferences {
	page(name: "authentication", title: "Nibe Uplink", content: "mainPage", submitOnChange: true, install: true)
}

def mainPage() {
	if(!atomicState.accessToken) {
        atomicState.authToken = null
        atomicState.accessToken = createAccessToken()
    }

    return dynamicPage(name: "authentication", uninstall: true) {
        if (!atomicState.authToken) {
            def redirectUrl = "https://graph-eu01-euwest1.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${getApiServerUrl()}"

            section("Nibe authentication") {
                paragraph "Tap below to log in to Nibe Uplink and authorize SmartThings access."
                href url:redirectUrl, style:"embedded", required:true, title:"", description:"Click to enter credentials"
            }
        } else {
             section("Options") {
            	input "systemId", "number", title:"System ID:", required: true
            }
        }
    }
}

def oauthInitUrl() {
   atomicState.oauthInitState = UUID.randomUUID().toString()

   def oauthParams = [
      response_type: "code",
      scope: "READSYSTEM",
      client_id: getAppClientId(),
      state: atomicState.oauthInitState,
      access_type: "offline",
      redirect_uri: "https://graph-eu01-euwest1.api.smartthings.com/oauth/callback"
   ]

   redirect(location: "https://api.nibeuplink.com/oauth/authorize?" + toQueryString(oauthParams))
}

def callback() {
 	//log.debug "callback()>> params: $params, params.code ${params.code}"

	def postParams = [
		uri: "https://api.nibeuplink.com",
		path: "/oauth/token",
		requestContentType: "application/x-www-form-urlencoded;charset=UTF-8",
		body: [
			code: params.code,
			client_secret: getAppClientSecret(),
			client_id: getAppClientId(),
			grant_type: "authorization_code",
			redirect_uri: "https://graph-eu01-euwest1.api.smartthings.com/oauth/callback",
            scope: "READSYSTEM"
		]
	]

	def jsonMap
	try {
		httpPost(postParams) { resp ->
			log.debug "resp callback"
			log.debug resp.data
			atomicState.refreshToken = resp.data.refresh_token
            atomicState.authToken = resp.data.access_token
            atomicState.last_use = now()
			jsonMap = resp.data
		}
	} catch (e) {
		log.error "something went wrong: $e"
		return
	}

	if (atomicState.authToken) {
        def message = """
                <p>Your account is now connected to SmartThings!</p>
                <p>Click 'Done' to finish setup.</p>
        """
        displayMessageAsHtml(message)
        getChildDevice(atomicState.childDeviceID)?.poll()
	} else {
        def message = """
            <p>There was an error connecting your account with SmartThings</p>
            <p>Please try again.</p>
        """
        displayMessageAsHtml(message)
	}
}

def isTokenExpired() {
    if (atomicState.last_use == null || now() - atomicState.last_use > 1800) {
    	return refreshAuthToken()
    }
    return false
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

    if(!atomicState.refreshToken) {
        log.warn "Can not refresh OAuth token since there is no refreshToken stored"
    } else {
        def stcid = getAppClientId()

        def refreshParams = [
            method: 'POST',
            uri   : "https://api.nibeuplink.com",
            path  : "/oauth/token",
            body : [
                refresh_token: "${atomicState.refreshToken}",
                client_secret: getAppClientSecret(),
                grant_type: 'refresh_token',
                client_id: getAppClientId()
            ],
        ]

        try {
            httpPost(refreshParams) { resp ->
                if(resp.data) {
                    //log.debug resp.data
                    atomicState.authToken = resp?.data?.access_token
					atomicState.last_use = now()

                    return true
                }
            }
        }
        catch(Exception e) {
            log.debug "caught exception refreshing auth token: " + e
        }
    }
    return false
}

def toQueryString(Map m) {
   return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getAppClientId() { appSettings.clientId }
def getAppClientSecret() { appSettings.clientSecret }

def setupChildDevice() {
	if(!atomicState.childDeviceID) {
    	atomicState.childDeviceID = UUID.randomUUID().toString()
    }

    if(!getChildDevice(atomicState.childDeviceID)) {
    	if(!addChildDevice("arnqvist", "Nibe F750", atomicState.childDeviceID, null, [name: "Nibe F750 ${atomicState.childDeviceID}", label:"Nibe F750", completedSetup: true])) {
        	log.error "Failed to add child device"
        }
    }
}

/**
def isSystemIdSet() {
    if (systemId == null) {
    	return getSystemId()
    }
    return false
}
*/

/**
def getSystemId() {
	//refreshAuthToken()

    def params = [
        uri:  'https://api.nibeuplink.com',
        path: '/api/v1/systems',
        //query: [parameterIds: 'systemid'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Chilla: ${params}"
    try {
        httpGet(params) {resp ->
            //log.debug "resp data: ${resp.data}"
            //log.debug "SystemID: ${resp.data.objects.systemId}"

            def systemId = resp.data.objects.systemId
            log.debug "SystemID: ${systemId}"
        }
    } catch (e) {
        log.error "error: $e"
    }
}
*/


def getParamPath() { return "/api/v1/systems/" + systemId + "/parameters" }
//log.debug " systemid: ${systemId}"

def getIndoorTemp() {
	refreshAuthToken()

	def ParamPath = getParamPath()
    def params = [
        uri:  'https://api.nibeuplink.com',
        path: ParamPath,
        query: [parameterIds: 'indoor_temperature'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Parameters: ${ParamPath}"
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
            //log.debug "systemId: ${resp.data.rawValue}"

           return resp.data.rawValue[0].toDouble() / 10
           //return Math.round(temp)
        }
    } catch (e) {
        log.error "error: $e"
    }
}

def getOutdoorTemp() {
	//refreshAuthToken()

    def ParamPath = getParamPath()
    def params = [
        uri:  'https://api.nibeuplink.com',
        path: ParamPath,
        query: [parameterIds: 'outdoor_temperature'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Chilla: ${params}"
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
           // log.debug "systemId: ${resp.data.rawValue}"

            return resp.data.rawValue[0].toInteger() / 10
        }
    } catch (e) {
        log.error "error: $e"
    }
}

def getWaterTemp() {
	//refreshAuthToken()

    def ParamPath = getParamPath()
    def params = [
        uri:  'https://api.nibeuplink.com',
        path: ParamPath,
        query: [parameterIds: 'hot_water_temperature'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Chilla: ${params}"
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
           // log.debug "systemId: ${resp.data.rawValue}"

            return resp.data.rawValue[0].toInteger() / 10
        }
    } catch (e) {
        log.error "error: $e"
    }
}

def getFanSpeed() {
	//refreshAuthToken()

    def ParamPath = getParamPath()
    def params = [
        uri:  'https://api.nibeuplink.com',
        path: ParamPath,
        query: [parameterIds: 'fan_speed'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Chilla: ${params}"
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
            //log.debug "systemId: ${resp.data.rawValue}"

            return resp.data.rawValue[0].toInteger()
        }
    } catch (e) {
        log.error "error: $e"
    }
}

def getAddition() {
	//refreshAuthToken()

    def ParamPath = getParamPath()
    def params = [
        uri:  'https://api.nibeuplink.com',
        path: ParamPath,
        query: [parameterIds: '43084'],
        contentType: 'application/json',
        headers: ["Authorization": "Bearer ${atomicState.authToken}"]
    ]
    //log.debug " Chilla: ${params}"
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
            //log.debug "systemId: ${resp.data.rawValue}"

            def additionStr = resp.data.rawValue[0].toInteger() / 100
            return additionStr.toString()
        }
    } catch (e) {
        log.error "error: $e"
    }
}

def installed() {
	setupChildDevice()
}

def updated() {
	setupChildDevice()
}

