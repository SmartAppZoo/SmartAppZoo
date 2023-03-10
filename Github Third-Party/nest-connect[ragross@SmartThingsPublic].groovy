/**
 *  Copyright 2015 SmartThings
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
 *  Nest (Connect) - https://developer.nest.com/documentation
 *
 *  Author: Juan Pablo Risso (juan@smartthings.com)
 *
 *  Date: 2015-02-05
 *
 *  To-Do:
 *		- Concent Messege (platform) 
 *		- Change away (once message is done)
 *		- Error notification 
 * 
 */
 
definition(
    name: "Nest (Connect)",
    namespace: "smartthings",
    author: "Juan Pablo Risso",
    description: "Allows you to integrate your Nest Thermostats with SmartThings.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/nest-home-app.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/nest-home-app@2x.png"
){
	appSetting "clientId"
	appSetting "clientSecret"
	appSetting "serverUrl"
}

preferences {
    page(name: "Credentials", title: "Nest", content: "authPage", install: false)
}

mappings {
	path("/receivedToken") { action: [ POST: "receivedToken", GET: "receivedToken"] }
	path("/receiveToken") { action: [ POST: "receiveToken", GET: "receiveToken"] }
	path("/hookCallback") { action: [ POST: "hookEventHandler", GET: "hookEventHandler"] }
	path("/oauth/callback") { action: [ GET: "callback" ] }
	path("/oauth/initialize") { action: [ GET: "init"] }
}

def getSmartThingsClientId() {
   return appSettings.clientId
}

def getSmartThingsClientSecret() {
   return appSettings.clientSecret
}

def callback() {
	def redirectUrl = null
	if (params.authQueryString) {
		redirectUrl = URLDecoder.decode(params.authQueryString.replaceAll(".+&redirect_url=", ""))
		log.debug "redirectUrl: ${redirectUrl}"
	} else {
		log.warn "No authQueryString"
	}
	
	if (state.NestAccessToken) {
		log.debug "Access token already exists"
		setup()
		success()
	} else {
		def code = params.code
		if (code) {
			if (code.size() > 6) {
				// Nest code
				log.debug "Exchanging code for access token"
				receiveToken(redirectUrl)
			} else {
				// SmartThings code, which we ignore, as we don't need to exchange for an access token.
				// Instead, go initiate the Nest OAuth flow.
				log.debug "Executing callback redirect to auth page"
			    def stcid = getSmartThingsClientId()
    			def oauthParams = [client_id: stcid, state: "${app.id}"]
    			redirect(location: "https://home.nest.com/login/oauth2?${toQueryString(oauthParams)}")
			}
		} else {
			log.debug "This code should be unreachable"
			success()
		}
	}
}

def authPage() {
    log.debug "authPage"
    def description = null          
    if (!state.NestAccessToken) {
		if (!state.accessToken) {
			log.debug "About to create access token"
			createAccessToken()
		}
        description = "Click to enter Nest Credentials"
        def redirectUrl = "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}"
        return dynamicPage(name: "Credentials", title: "Nest", nextPage: null, uninstall: true, install:false) {
               section { href url:redirectUrl, style:"embedded", required:true, title:"Nest", description:description }
        }
    } else {
		//device discovery request every 5 //25 seconds
		int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
		state.deviceRefreshCount = deviceRefreshCount + 1
		def refreshInterval = 3

		def options = discoverDevices() ?: []

		def numFound = options.size() ?: 0
		if((deviceRefreshCount % 5) == 0) {
        	log.trace "Discovering..."			
			discoverDevices()
		}
		return dynamicPage(name:"Credentials", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your Nest devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selecteddevice", "enum", required:false, title:"Select Nest (${numFound} found)", multiple:true, options:options
			}
		}                       
    }
}

def init() {
	log.debug "In init"
	def stcid = getSmartThingsClientId()    
    def oauthParams = [client_id: stcid, state: "${app.id}"]
    def authorizationUrl = "https://home.nest.com/login/oauth2?${toQueryString(oauthParams)}"
    redirect(location: authorizationUrl)
}

def receiveToken(redirectUrl = null) {
	log.debug "receiveToken"
    def stcid = getSmartThingsClientId()
    def oauthClientSecret = getSmartThingsClientSecret()
    def oauthParams = [ client_id: stcid, client_secret: oauthClientSecret, grant_type: "authorization_code", code: params.code ]
    def params = [
      uri: "https://api.home.nest.com/oauth2/access_token?${toQueryString(oauthParams)}",
    ]
    httpPost(params) { response -> 
    	state.NestAccessToken = response.data.access_token
    }

	setup()
	if (state.NestAccessToken) {
		success()
	} else {
		fail()
	}
}

def success() {
	def message = """
		<p>Your Nest Account is now connected to SmartThings!</p>
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

def receivedToken() {
	def message = """
		<p>Your Nest Account is already connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
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
        <title>SmartThings Connection</title>
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
                width: 560px;
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
                padding: 0 40px;
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
		${redirectHtml}
        </head>
        <body>
            <div class="container">
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/nest@2x.png" alt="Nest icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
            </div>
        </body>
        </html>
	"""
	render contentType: 'text/html', data: html
}

String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getServerUrl() { return appSettings.serverUrl ?: "https://graph.api.smartthings.com" }

def buildRedirectUrl(page) {
    return "${serverUrl}/api/token/${state.accessToken}/smartapps/installations/${app.id}/${page}"
}

Map discoverDevices() {
    log.trace "Discovering devices"
    setup()
    if (state.nestdevices.devices.thermostats) {
        def devices = state.nestdevices.devices.thermostats
        def map = [:]
        devices.each {
            def value = "${it.value.name_long} [${state.nestdevices.structures[it.value.structure_id].name}]"
            def key = "${it.value.device_id}"
            map["${key}"] = value
        }
        map
    }    
}

def installed() {
	enableCallback()	
	if (!state.accessToken) {
		log.debug "About to create access token"
		createAccessToken()
	} else {	
		initialize()
	}
}

def updated() {
	enableCallback()	
	if (!state.accessToken) {
		log.debug "About to create access token"
		createAccessToken()
	} else {	
		initialize()
	}   
}

def uninstalled() {
	def devices = getChildDevices()
	log.trace "deleting ${devices.size()} device"
	devices.each {
		deleteChildDevice(it.deviceNetworkId)
	}
	if (state.NestAccessToken) {
		try {				
        	log.debug "Success disconnecting Nest from SmartThings"
		} catch (groovyx.net.http.HttpResponseException e) {
			log.error "Error disconnecting Nest from SmartThings: ${e.statusCode}"
		}
	}
}

def initialize() {
	if (selecteddevice) {
		addDevice()
        runEvery5Minutes("poll")
	}	
}

//CHILD DEVICE METHODS
def setup() {
    def Params = [auth: state.NestAccessToken]	    
    def url = "https://developer-api.nest.com/?${toQueryString(Params)}"
	try {
		httpGet(uri: url, headers: ["Accept": "application/json"]) {response ->
	    	if (response.status == 200) {
            	log.debug "valid Token"
                state.nestdevices = response.data 
	        }
	    }
	} catch (groovyx.net.http.HttpResponseException e) {
        if (e.statusCode == 401) { // token is expired
            state.remove("NestAccessToken")        
            sendNotification("Nest Access token has expired")
        }
	} catch (java.net.SocketTimeoutException e) {
		log.warn "Connection timed out, not much we can do here"
	}
    return null
}

def addDevice() {
	def nestresponse = state.nestdevices
    if (nestresponse) {
        def devices = nestresponse.devices.thermostats
        log.trace "Adding childs" 
        selecteddevice.each { dni ->
            def d = getChildDevice(dni)
            if(!d) {
                def newDevice = devices.find { it.value.device_id == dni }
                log.trace "dni = $dni"
                d = addChildDevice("smartthings", "Nest Thermostat", dni, null, [label:"${newDevice?.value.name_long} [${nestresponse.structures[newDevice?.value.structure_id].name}]"])
                log.trace "created ${d.displayName} with id $dni"
                poll()
            } else {
                log.trace "found ${d.displayName} with id $dni already exists"
            }
        }
    }
}

def mode(device_id,mode) {
    def Params = [auth: state.NestAccessToken]	
    def url = "https://developer-api.nest.com/devices/thermostats/${device_id}/?${toQueryString(Params)}"
    Map body = [hvac_mode:mode]
	try {
    	httpPutJson(uri: url) { resp -> 
	    	def url2 = resp.getHeaders("Location").value[0].toString()
            def finalresponse = "Command failed"
            try {
                httpPutJson(uri: url2, headers: ["Content-Type": "application/json"], body: body) { resp2 -> 
                    finalresponse = resp2.data.toString()
            	}        
            } catch (groovyx.net.http.HttpResponseException ex) {
                def aux = ex.response.data.toString().split(':').last()
                def error = aux.substring(0,aux.length() - 1)
                sendNotification(error)
                return error
            }
            runIn(5, "poll", [overwrite: false])
            return finalresponse            
		} 
    } catch (groovyx.net.http.HttpResponseException ex) {
        if (ex.statusCode == 429) { // exceeded the allowed limit
            sendNotification("You exceeded the allowed limit of requests. Please try again later.")
        }    
    }    
}

def temp(device_id,target,value) {	
	def temp = value as BigDecimal
    def Params = [auth: state.NestAccessToken]
    def url = "https://developer-api.nest.com/devices/thermostats/${device_id}/?${toQueryString(Params)}"    
    def max
    def min
    def scale= getTemperatureScale()  
    if (scale == "F") {
    	max = 90
        min = 50 
    } else {
    	max = 32
        min = 9 
    }    
    Map body = ["${target}":temp]
	if((temp >= min) && (temp <= max)) {
    	def finalresponse = "Command failed"
        try {
            httpPutJson(uri: url) { resp -> 
                def url2 = resp.getHeaders("Location").value[0].toString()
                try {
                	httpPutJson(uri: url2, headers: ["Content-Type": "application/json"], body: body) { response -> 
                    	finalresponse = response.data.toString()
                	}
                } catch (groovyx.net.http.HttpResponseException ex) {
                	def aux = ex.response.data.toString().split(':').last()
                    def error = aux.substring(0,aux.length() - 1)
                    sendNotification("Nest: $error")
                    poll()
                	return error
                }
            	runIn(3, "poll", [overwrite: false])
                return finalresponse
            } 
        } catch (groovyx.net.http.HttpResponseException ex) {
             def msg = ex.toString().split(':').last()
             if (msg.contains("Too Many Requests"))
             	sendNotification("Nest: Too Many Requests, please try again later.")
             return getResponse(ex)              
        }  
    } else {
    	return "Out of range"
    }
}

def poll() {
	// check if there are devices installed :)
	setup()
	def nestresponse = state.nestdevices
    //sendNotification(nestresponse.toString())
    if (nestresponse) {
        def devices = nestresponse.devices.thermostats
        log.trace "Polling childs" 
        devices.each { 
            log.trace "Thermostat $it.value.device_id has been updated"        
            def childDevice = getChildDevice(it.value.device_id)
            if(childDevice) {    
                if (it.value.is_online) {
                	def scale= getTemperatureScale()
                    def away = nestresponse.structures[it.value.structure_id].away.toString()
                    def rushhour = false
                    def rushhourstart = nestresponse.structures[it.value.structure_id].peak_period_start_time
                    def rushhourends = nestresponse.structures[it.value.structure_id].peak_period_end_time
	                if (rushhourstart) {
    					def currenttime = now()
                        rushhourstart = rushhourstart.fromSystemFormat().getTime()
                        rushhourends = rushhourends.fromSystemFormat().getTime()
                        if (rushhourstart <= currenttime && rushhourends >= currenttime)
                    		rushhour = true   
					}
 					if (scale == "F")
  						childDevice?.sendEvent(name:"temperature", value: it.value.ambient_temperature_f)
                    else
                    	childDevice?.sendEvent(name:"temperature", value: it.value.ambient_temperature_c)
                    childDevice?.sendEvent(name:"humidity", value: it.value.humidity) 
                    if (it.value.can_heat)
                        childDevice?.sendEvent(name:"canheat", value: "yes")
                    else
                        childDevice?.sendEvent(name:"canheat", value: "no")
                    if (it.value.can_cool)
                        childDevice?.sendEvent(name:"cancool", value: "yes")
                    else
                        childDevice?.sendEvent(name:"cancool", value: "no")                    
                    if (it.value.has_leaf)
                        childDevice?.sendEvent(name:"leafinfo", value: "yes")
                    else
                        childDevice?.sendEvent(name:"leafinfo", value: "no")
                    if (it.value.is_using_emergency_heat)
                        childDevice?.sendEvent(name:"emergencyheat", value: "yes")
                    else
                        childDevice?.sendEvent(name:"emergencyheat", value: "no")                    
                    childDevice?.sendEvent(name:"presence", value: away)    
                    if (away == "home") {   
                        switch (it.value.hvac_mode) {
                            case "heat":
                            	if (scale == "F")
                                	childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.target_temperature_f)
                                else
                                	childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.target_temperature_c)
                                childDevice?.sendEvent(name:"lowarrowup", value: "default")
                                childDevice?.sendEvent(name:"lowarrowdown", value: "default")
                                childDevice?.sendEvent(name:"higharrowup", value: "disable")
                                childDevice?.sendEvent(name:"higharrowdown", value: "disable")  
                                
                                break;
                            case "cool":
                                childDevice?.sendEvent(name:"heatingSetpoint", value: "")
                                if (scale == "F")
                                	childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.target_temperature_f)
                                else
                                	childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.target_temperature_c)
                                childDevice?.sendEvent(name:"lowarrowup", value: "disable")
                                childDevice?.sendEvent(name:"lowarrowdown", value: "disable")
                                childDevice?.sendEvent(name:"higharrowup", value: "default")
                                childDevice?.sendEvent(name:"higharrowdown", value: "default")                                     
                                break;                        
                            case "heat-cool":
                                if (scale == "F") {                            
                                    childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.target_temperature_low_f)
                                    childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.target_temperature_high_f) 
                                } else {
                                    childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.target_temperature_low_c)
                                    childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.target_temperature_high_c)
                                } 
                                childDevice?.sendEvent(name:"lowarrowup", value: "default")
                                childDevice?.sendEvent(name:"lowarrowdown", value: "default")
                                childDevice?.sendEvent(name:"higharrowup", value: "default")
                                childDevice?.sendEvent(name:"higharrowdown", value: "default")
                                break;
                            case "off":
                                childDevice?.sendEvent(name:"heatingSetpoint", value: "")
                                childDevice?.sendEvent(name:"coolingSetpoint", value: "")
                                childDevice?.sendEvent(name:"lowarrowup", value: "disable")
                                childDevice?.sendEvent(name:"lowarrowdown", value: "disable")
                                childDevice?.sendEvent(name:"higharrowup", value: "disable")
                                childDevice?.sendEvent(name:"higharrowdown", value: "disable")
                                break; 
                        } 
                        if (rushhour) {
                  		    childDevice?.sendEvent(name:"thermostatMode", value: "rushhour")   
                            childDevice?.sendEvent(name:"lowarrowup", value: "disable")
                            childDevice?.sendEvent(name:"lowarrowdown", value: "disable")
                            childDevice?.sendEvent(name:"higharrowup", value: "disable")
                            childDevice?.sendEvent(name:"higharrowdown", value: "disable")                            
                        } else {
                            childDevice?.sendEvent(name:"thermostatMode", value: it.value.hvac_mode)  
                        }    
                    } else {
                        childDevice?.sendEvent(name:"thermostatMode", value: away) 
                        childDevice?.sendEvent(name:"lowarrowup", value: "disable")
                        childDevice?.sendEvent(name:"lowarrowdown", value: "disable")
                        childDevice?.sendEvent(name:"higharrowup", value: "disable")
                        childDevice?.sendEvent(name:"higharrowdown", value: "disable")                      
                        if (scale == "F") {     
                            childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.away_temperature_low_f)
                            childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.away_temperature_high_f)
                        } else {
                            childDevice?.sendEvent(name:"heatingSetpoint", value: it.value.away_temperature_low_c)
                            childDevice?.sendEvent(name:"coolingSetpoint", value: it.value.away_temperature_high_c)
                        }    
                    }
                } else {
                    childDevice?.sendEvent(name:"thermostatMode", value: "offline")
                    childDevice?.sendEvent(name:"emergencyheat", value: "no")
                    childDevice?.sendEvent(name:"temperature", value: "")
                    childDevice?.sendEvent(name:"humidity", value: "")
                    childDevice?.sendEvent(name:"heatingSetpoint", value: "")
                    childDevice?.sendEvent(name:"coolingSetpoint", value: "")
                    childDevice?.sendEvent(name:"leafinfo", value: "no")  
                    childDevice?.sendEvent(name:"lowarrowup", value: "disable")
                    childDevice?.sendEvent(name:"lowarrowdown", value: "disable")
                    childDevice?.sendEvent(name:"higharrowup", value: "disable")
                    childDevice?.sendEvent(name:"higharrowdown", value: "disable")                 
                }
            } else {
                log.trace "This device doesn't exists"
            }  
        }  
	} else {
    	//sendNotification(" not Here")
    }
}

def sendNotification(msg) {
	sendNotification(msg)
}

def hookEventHandler() {
    // log.debug "In hookEventHandler method."
    log.debug "request = ${request}"
    
    def json = request.JSON 

	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}