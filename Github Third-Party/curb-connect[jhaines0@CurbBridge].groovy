/**
 *  Curb (Connect)
 *
 *  Copyright 2017 Justin Haines
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
    name: "Curb (Connect)",
    namespace: "jhaines0",
    author: "Justin Haines",
    description: "App to get usage data from a Curb home energy monitor",
    category: "",
    iconUrl: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    iconX2Url: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    iconX3Url: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    singleInstance: true
){
	appSetting "clientId"
}


preferences {
    page(name: "auth", title: "Curb", nextPage:"", content:"authPage", uninstall: true)
}

mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

def authPage() {
	log.debug "authPage()"

	if(!atomicState.accessToken) {
		atomicState.accessToken = createAccessToken()
	}

	if(atomicState.authToken) {
        log.debug("Already Connected")
        
        return dynamicPage(name: "auth", title: "Connected", nextPage: "", install: true, uninstall: true) {
        	section() {
            	paragraph("You are connected to Curb")
            }
            section() {
                paragraph("If you need more frequent measurements, you may adjust the update rate below. [1-15]")
                input "samplesPerMinute", "number", required: false, title: "Sample Rate (samples per minute)", defaultValue: 1, range: "1..15"
        	}
    	}
	} else {
		log.debug("Logging In")
        
        return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall:false) {
			section() {
				paragraph("Tap below to log in to the Curb service and authorize SmartThings access")
				href url:buildRedirectUrl, style:"embedded", required:true, title:"Curb", description:"Click to enter Curb Credentials"
			}
		}
        
	}
}

def oauthInitUrl() {
	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
			response_type: "code",
			scope: "offline_access",
            audience: "app.energycurb.com/api",
			client_id: curbClientId,
            connection: "Users",
			state: atomicState.oauthInitState,
			redirect_uri: callbackUrl
	]

	redirect(location: "${curbLoginUrl}?${toQueryString(oauthParams)}")
}

def callback() {
	log.debug "callback()>> params: $params, params.code ${params.code}"

	def code = params.code
	def oauthState = params.state

	if (oauthState == atomicState.oauthInitState) {
		def tokenParams = [
			grant_type    : "authorization_code",
			code          : code,
			client_id     : curbClientId,
            client_secret : curbClientSecret,
			redirect_uri  : callbackUrl
		]

		httpPostJson([uri: curbTokenUrl, body: tokenParams]) { resp ->
            log.debug("Got POST response: ${resp.data}")
            
            atomicState.refreshToken = resp.data.refresh_token
			atomicState.authToken = resp.data.access_token
            
            getCurbLocations()
		    getUsage()
		}

		if (atomicState.authToken) {
			success()
		} else {
			fail()
		}

	} else {
		log.error "callback() failed oauthState != atomicState.oauthInitState"
	}

}

def success() {
	def message = """
        <p>Your Curb account is now connected to SmartThings!</p>
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
                <title>Curb & SmartThings connection</title>
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
                <img src="http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png" alt="Curb Icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
            </div>
        </body>
    </html>
    """

	render contentType: 'text/html', data: html
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
	log.debug "Initializing"
    
    unschedule()
    
    refreshAuthToken()
    getCurbLocations()
    getUsage()
    
    //runEvery1Minute(getUsageFromHistorical)
    runEvery5Minutes(getHistorical)
    runEvery3Hours(refreshAuthToken)
    
    def rate = settings.samplesPerMinute
    log.debug("Sampling at ${rate} samples per minute")
    
    schedule("* * * * * ?", doPoll, [data: [cycles: rate]])
}

def doPoll(data)
{
    getUsage()
    
    def period = 60.0/settings.samplesPerMinute
    //log.debug("Period: ${period}")
    
    def count = data.cycles;
    count = count - 1;
    if(count > 0)
    {
        runIn(period, doPoll, [data: [cycles: count]])
    }
}

def uninstalled() {
	log.debug "Uninstalling"
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def getCurbLocations()
{
    log.debug("Requesting Curb location info");
    
    def params = [
    	uri: "http://app.energycurb.com",
    	path: "/api/locations",
        headers: ["Authorization": "Bearer ${atomicState.authToken}"],
	]

	try {
    	httpGet(params) { resp ->
            atomicState.location = resp.data[0].id
            log.debug("Location ID: ${atomicState.location}")
		}
	} catch (e) {
    	log.error "Could not get location info: $e"
	}
}

def updateChildDevice(dni, label, values)
{
	if(dni == null)
    {
    	log.error("Tried to create a null child device! Label: $label")
        return;
    }

    try
    {
        def existingDevice = getChildDevice(dni)

        if(!existingDevice)
        {
        	if(values instanceof Collection)
            {
            	// Trying to update a non-existent device with historical data, just skip it
            	return;
            }
            else
            {
            	// Otherwise create it
                existingDevice = addChildDevice("jhaines0", "Curb Power Meter", dni, null, [name: "${dni}", label: "${label}"])
            }
        }
        
        existingDevice.handleMeasurements(values)
    }
    catch (e)
    {
        log.error "Error creating or updating device: ${e}"
    }
}

include 'asynchttp_v1'



def getUsageFromHistorical() {

    log.debug("Getting Usage (from Historical)")
    def params = [
    	uri: "https://app.energycurb.com",
    	path: "/api/historical/${atomicState.location}/5m/m",
        headers: ["Authorization": "Bearer ${atomicState.authToken}"],
        requestContentType: 'application/json'
	]

	asynchttp_v1.get(processUsageFromHistorical, params)
}

def processUsageFromHistorical(resp, data) {

    if (resp.hasError())
    {
        log.debug "Usage from Historical Response Error: ${resp.getErrorMessage()}"
        return
    }

	log.debug "  -> Got Usage (from Historical)"

    def json = resp.json

    if(json)
    {
    	//log.debug "Got Usage: ${json}"
        def mainSum = 0.0

        json.each
        {
        	//log.debug "Have sensor: ${it.label} (${it.id})"
            def latest = it.values.max {vv -> vv.t}

            if(it.main)
            {
                mainSum += latest.w
            }
            else
            {
            	updateChildDevice("${it.id}", it.label, latest.w)
            }
        }

        updateChildDevice("__MAIN__", "Main", mainSum)
    }
    else
    {
    	log.error "Malformed data in usage from historical: ${resp.data}"
    }
}


def getUsage() {

    log.debug("Getting Usage")
    
    def params = [
    	uri: "https://app.energycurb.com",
    	path: "/api/latest/${atomicState.location}",
        headers: ["Authorization": "Bearer ${atomicState.authToken}"],
        requestContentType: 'application/json'
	]

	asynchttp_v1.get(processUsage, params)
}

def processUsage(resp, data) {

    if (resp.hasError())
    {
        log.debug "Usage Response Error: ${resp.getErrorMessage()}, falling back to historical"
        getUsageFromHistorical()
        return
    }

    def json = resp.json
    
    if(json)
    {
        log.debug "  -> Got Usage Data (${json.circuits.size()} sensors)"
    	//log.debug "Got Latest: ${json}"

        json.circuits.each
        {
        	//log.debug "Have sensor: ${it.label} (${it.id})"
            updateChildDevice("${it.id}", it.label, it.w)
        }

        updateChildDevice("__MAIN__", "Main", json.net)
    }
    else
    {
    	log.error "Malformed usage data: ${resp.data}"
    }
}

def getHistorical() {
	log.debug("Getting Historical")
    
    def params = [
    	uri: "https://app.energycurb.com",
    	path: "/api/historical/${atomicState.location}/24h/5m",
        headers: ["Authorization": "Bearer ${atomicState.authToken}"],
        requestContentType: 'application/json'
	]

	asynchttp_v1.get(processHistorical, params)
}

def processHistorical(resp, data)
{
    if (resp.hasError())
    {
        log.debug "Historical Response Error: ${resp.getErrorMessage()}"
        return
    }

	def json = resp.json

    if(json)
    {
        log.debug "  -> Got Historical Data"
        def total = null

        json.each
        {
            updateChildDevice("${it.id}", it.label, it.values)

            if(it.main)
            {
                it.values.sort{a,b -> a.t <=> b.t}
                if(total == null)
                {
                    total = it
                }
                else
                {
                    if(it.values.size() != total.values.size())
                    {
                        log.debug("Size mismatch")
                    }
                    else
                    {
                        for(int i = 0; i < total.values.size(); ++i)
                        {
                            if(total.values[i].t != it.values[i].t)
                            {
                                log.debug("Time mismatch")
                            }
                            else
                            {
                                total.values[i].w = (total.values[i].w) + (it.values[i].w)
                            }
                        }
                    }
                }
            }
        }

        updateChildDevice("__MAIN__", "Main", total.values)
    }
    else
    {
    	log.error "Malformed historical data: ${resp.data}"
    }
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def refreshAuthToken() {
	log.debug "refreshing auth token"

	if(!atomicState.refreshToken) {
		log.warn "Can not refresh OAuth token since there is no refreshToken stored"
	} else {
		def tokenParams = [
			grant_type: "refresh_token",
			client_id : curbClientId,
            client_secret : curbClientSecret,
			refresh_token: atomicState.refreshToken
		]

		httpPostJson([uri: curbTokenUrl, body: tokenParams]) { resp ->
			log.debug "response contentType: ${resp.contentType}"
            log.debug("Got POST response (refresh): ${resp.data}")
            
			atomicState.authToken = resp.data.access_token
		}
	}
}


def getCurbClientId()        { return "R7LHLp5rRr6ktb9hhXfMaILsjwmIinKa" }
def getCurbClientSecret()    { return "pcxoDsqCN7o_ny5KmEKJ2ci0gL5qqOSfxnzF6JIvwsfRsUVXFdD-DUc40kkhHAZR" }
def getCurbAuthUrl()         { return "https://energycurb.auth0.com" }
def getCurbLoginUrl()        { return "${curbAuthUrl}/authorize" }
def getCurbTokenUrl()        { return "${curbAuthUrl}/oauth/token" }

def getServerUrl()           { return "https://graph.api.smartthings.com" }
def getShardUrl()            { return getApiServerUrl() }
def getCallbackUrl()         { return "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl()    { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${shardUrl}" }
def getApiEndpoint()         { return "https://api.energycurb.com" }


