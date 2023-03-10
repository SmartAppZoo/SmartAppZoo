/**
 *  Harmony (Connect) - https://developer.Harmony.com/documentation
 *
 *  Author: Juan Pablo Risso (juan@smartthings.com)
 *
 *  Date: 2015-05-01
 * 
 */
 
definition(
    name: "Logitech Harmony (Connect)",
    namespace: "smartthings",
    author: "Juan Pablo Risso",
    description: "Allows you to integrate your Logitech Harmony account with SmartThings.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/harmony.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/harmony%402x.png",
){
	appSetting "clientId"
	appSetting "clientSecret"
	appSetting "callbackUrl"
}

preferences {
    page(name: "Credentials", title: "Harmony", content: "authPage", install: false)
}

mappings {
	path("/receivedToken") { action: [ POST: "receivedToken", GET: "receivedToken"] }
	path("/receiveToken") { action: [ POST: "receiveToken", GET: "receiveToken"] }
	path("/hookCallback") { action: [ POST: "hookEventHandler", GET: "hookEventHandler"] }
	path("/oauth/callback") { action: [ GET: "callback" ] }
	path("/oauth/initialize") { action: [ GET: "init"] }
}

def getServerUrl() { return "https://graph.api.smartthings.com" }

def authPage() {
    log.debug "authPage"
    def description = null          
    if (!state.HarmonyAccessToken) {
		if (!state.accessToken) {
			log.debug "About to create access token"
			createAccessToken()
		}
        description = "Click to enter Harmony Credentials"
        def redirectUrl = "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}"
        return dynamicPage(name: "Credentials", title: "Harmony", nextPage: null, uninstall: true, install:false) {
               section { href url:redirectUrl, style:"embedded", required:true, title:"Harmony", description:description }
        }
    } else {
		//device discovery request every 5 //25 seconds
		int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
		state.deviceRefreshCount = deviceRefreshCount + 1
		def refreshInterval = 3

		def huboptions = state.HarmonyHubs ?: []
		def actoptions = state.HarmonyActivities ?: []
        
		def numFoundHub = huboptions.size() ?: 0
        def numFoundAct = actoptions.size() ?: 0
		if((deviceRefreshCount % 5) == 0) {
        	log.trace "Discovering..."			
			discoverDevices()
		}
		return dynamicPage(name:"Credentials", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
			section("Please wait while we discover your Harmony Hubs and Activities. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
				input "selectedhubs", "enum", required:false, title:"Select Harmony Hubs (${numFoundHub} found)", multiple:true, options:huboptions
			}
            if (numFoundHub > 0 && numFoundAct > 0 && true)
			section("You can also add activities as virtual switches for other convenient integrations") {
				input "selectedactivities", "enum", required:false, title:"Select Harmony Activities (${numFoundAct} found)", multiple:true, options:actoptions
			}            
		}                       
    }
}

def callback() {
	def redirectUrl = null
	if (params.authQueryString) {
		redirectUrl = URLDecoder.decode(params.authQueryString.replaceAll(".+&redirect_url=", ""))
		log.debug "redirectUrl: ${redirectUrl}"
	} else {
		log.warn "No authQueryString"
	}
	
	if (state.HarmonyAccessToken) {
		log.debug "Access token already exists"
		discovery()
		success()
	} else {
		def code = params.code
		if (code) {
			if (code.size() > 6) {
				// Harmony code
				log.debug "Exchanging code for access token"
				receiveToken(redirectUrl)
			} else {
				// Initiate the Harmony OAuth flow.
				init()
			}
		} else {
			log.debug "This code should be unreachable"
			success()
		}
	}
}

def init() {
	log.debug "Requesting Code"
    def oauthParams = [client_id: "${appSettings.clientId}", scope: "remote", response_type: "code", redirect_uri: "${appSettings.callbackUrl}" ]
    redirect(location: "https://home.myharmony.com/oauth2/authorize?${toQueryString(oauthParams)}")
}

def receiveToken(redirectUrl = null) {
	log.debug "receiveToken"
    def oauthParams = [ client_id: "${appSettings.clientId}", client_secret: "${appSettings.clientSecret}", grant_type: "authorization_code", code: params.code ]
    def params = [
      uri: "https://home.myharmony.com/oauth2/token?${toQueryString(oauthParams)}",
    ]
    httpPost(params) { response -> 
    	state.HarmonyAccessToken = response.data.access_token
    }

	discovery()
	if (state.HarmonyAccessToken) {
		success()
	} else {
		fail()
	}
}

def success() {
	def message = """
		<p>Your Harmony Account is now connected to SmartThings!</p>
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
		<p>Your Harmony Account is already connected to SmartThings!</p>
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
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/harmony@2x.png" alt="Harmony icon" />
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

def buildRedirectUrl(page) {
    return "${serverUrl}/api/token/${state.accessToken}/smartapps/installations/${app.id}/${page}"
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
	unsubscribe()
    unschedule()
	enableCallback()	
	if (!state.accessToken) {
		log.debug "About to create access token"
		createAccessToken()
	} else {	
		initialize()
	}   
}

def uninstalled() {
	if (state.HarmonyAccessToken) {
		try {	
        	state.HarmonyAccessToken = ""
        	log.debug "Success disconnecting Harmony from SmartThings"
		} catch (groovyx.net.http.HttpResponseException e) {
			log.error "Error disconnecting Harmony from SmartThings: ${e.statusCode}"
		}
	}
}

def initialize() {
	if (selectedhubs || selectedactivities) {
		addDevice()
        runEvery5Minutes("discovery")
	}	
}

Map discoverDevices() {
    log.trace "Discovering devices"
    discovery()
    if (state.Harmonydevices.hubs) {
        def devices = state.Harmonydevices.hubs
        log.trace devices.toString()
        def activities = [:]
        def hubs = [:]
        devices.each {
        	def hubkey = it.key
            def hubname = getHubName(it.key)
            def hubvalue = "Harmony Hub [${hubname}]"
            hubs["harmony-${hubkey}"] = hubvalue
        	it.value.response.data.activities.each { 
                def value = "${it.value.name}"
                def key = "harmony-${hubkey}-${it.key}"
                activities["${key}"] = value
           }            
        }
        state.HarmonyHubs = hubs
        state.HarmonyActivities = activities              
    }    
}

//CHILD DEVICE METHODS
def discovery() {
    def Params = [auth: state.HarmonyAccessToken]	    
    def url = "https://home.myharmony.com/cloudapi/activity/all?${toQueryString(Params)}"
	try {
		httpGet(uri: url, headers: ["Accept": "application/json"]) {response ->
	    	if (response.status == 200) {
            	log.debug "valid Token"
                state.Harmonydevices = response.data 
	        }
	    }
	} catch (groovyx.net.http.HttpResponseException e) {
        if (e.statusCode == 401) { // token is expired
            state.remove("HarmonyAccessToken")        
            log.warn "Harmony Access token has expired"
        }
	} catch (java.net.SocketTimeoutException e) {
		log.warn "Connection timed out, not much we can do here"
	}
    getActivityList()
    poll()
    return null
}

def addDevice() {
    log.trace "Adding Hubs" 
    selectedhubs.each { dni ->
        def d = getChildDevice(dni)
        if(!d) {
            def newAction = state.HarmonyHubs.find { it.key == dni }
            d = addChildDevice("smartthings", "Logitech Harmony Hub C2C", dni, null, [label:"${newAction.value}"])
            log.trace "created ${d.displayName} with id $dni"
            poll()
        } else {
            log.trace "found ${d.displayName} with id $dni already exists"
        }
    }  
    log.trace "Adding Activities" 
    selectedactivities.each { dni ->
        def d = getChildDevice(dni)
        if(!d) {
            def newAction = state.HarmonyActivities.find { it.key == dni }
            d = addChildDevice("smartthings", "Harmony Activity", dni, null, [label:"${newAction.value} [Harmony Activity]"])
            log.trace "created ${d.displayName} with id $dni"
            poll()
        } else {
            log.trace "found ${d.displayName} with id $dni already exists"
        }
    }    
}

def activity(dni,mode) {
    def Params = [auth: state.HarmonyAccessToken]
    def url = ''
    if (dni == "all") {
        url = "https://home.myharmony.com/cloudapi/activity/off?${toQueryString(Params)}"  
    } else {    
        def aux = dni.split('-')
        def hubId = aux[1]
        if (mode == "hub" || (aux.size() <= 2) || (aux[2] == "off")){	
        	url = "https://home.myharmony.com/cloudapi/hub/${hubId}/activity/off?${toQueryString(Params)}" 
        } else {
            def activityId = aux[2]
        	url = "https://home.myharmony.com/cloudapi/hub/${hubId}/activity/${activityId}/${mode}?${toQueryString(Params)}"
        }
	}      
	try {
    	httpPostJson(uri: url) { response -> 
        	if (response.data.code == 200 || dni == "all")
            	return "Command sent succesfully"
			else
                return "Command failed"
            runIn(20, "poll", [overwrite: true])
		} 
    } catch (groovyx.net.http.HttpResponseException ex) {
        log.error ex
    } 
    if (getChildDevices())
    	poll()
}

def poll() {
	// GET THE LIST OF ACTIVITIES
    if (state.HarmonyAccessToken) {
    	getActivityList()
        def Params = [auth: state.HarmonyAccessToken]	    
        def url = "https://home.myharmony.com/cloudapi/state?${toQueryString(Params)}"
        try {
            httpGet(uri: url, headers: ["Accept": "application/json"]) {response -> 
                def map = [:]
                response.data.hubs.each {
                    map["${it.key}"] = "${it.value.response.data.currentAvActivity},${it.value.response.data.activityStatus}"
                    def hub = getChildDevice("harmony-${it.key}")
                    if (hub) {
                        if (it.value.response.data.currentAvActivity == "-1") {
                            hub.sendEvent(name: "currentActivity", value: "--", descriptionText: "There isn't any activity running", display: false)
                        } else {
                            def currentActivity = getActivityName(it.value.response.data.currentAvActivity,it.key)
                            hub.sendEvent(name: "currentActivity", value: currentActivity, descriptionText: "Current activity is ${currentActivity}", display: false)
                        }
                    }   
                }
                def activities = getChildDevices() 
                def activitynotrunning = true
                activities.each { activity ->
                    def act = activity.deviceNetworkId.split('-') 
                    if (act.size() > 2) {
                        def aux = map.find { it.key == act[1] }
                        if (aux) {
                            def aux2 = aux.value.split(',')
                            def childDevice = getChildDevice(activity.deviceNetworkId)                   
                            if ((act[2] == aux2[0]) && (aux2[1] == "1" || aux2[1] == "2")) {
                                childDevice?.sendEvent(name: "switch", value: "on")
                                if (aux2[1] == "1")
                                    runIn(5, "poll", [overwrite: true])
                            } else {
                                childDevice?.sendEvent(name: "switch", value: "off")
                                if (aux2[1] == "3")
                                    runIn(5, "poll", [overwrite: true])
                            }
                        } 
                    }    
                }
                return "Poll completed $map - $state.hubs"
            }
        } catch (groovyx.net.http.HttpResponseException e) {
            if (e.statusCode == 401) { // token is expired
                state.remove("HarmonyAccessToken")        
                return "Harmony Access token has expired"
            }    
        }
	}        
}


def getActivityList() {
	// GET ACTIVITY'S NAME
    if (state.HarmonyAccessToken) {
        def Params = [auth: state.HarmonyAccessToken]	    
        def url = "https://home.myharmony.com/cloudapi/activity/all?${toQueryString(Params)}"
        try {
            httpGet(uri: url, headers: ["Accept": "application/json"]) {response -> 
                response.data.hubs.each {
                    def hub = getChildDevice("harmony-${it.key}")
                    if (hub) {
                    	def hubname = getHubName("${it.key}")
                        def activities = []
                        def aux = it.value.response.data.activities.size()
                        if (aux >= 1) {
                            activities = it.value.response.data.activities.collect {
                                [id: it.key, name: it.value['name'], type: it.value['type']]
                            }
                            activities += [id: "off", name: "${hubname} Activity OFF", type: "0"]
                            log.trace activities
                        }    
                        hub.sendEvent(name: "activities", value: new groovy.json.JsonBuilder(activities).toString(), descriptionText: "Activities are ${activities.collect { it.name }?.join(', ')}", display: false)   		
					}
                }    
            }
        } catch (groovyx.net.http.HttpResponseException e) {
        	log.trace e
        }
    }    
	return activity
}

def getActivityName(activity,hubId) {
	// GET ACTIVITY'S NAME
    def actname = activity
    if (state.HarmonyAccessToken) {
        def Params = [auth: state.HarmonyAccessToken]	    
        def url = "https://home.myharmony.com/cloudapi/hub/${hubId}/activity/all?${toQueryString(Params)}"
        try {
            httpGet(uri: url, headers: ["Accept": "application/json"]) {response -> 
                actname = response.data.data.activities[activity].name
            }
        } catch (groovyx.net.http.HttpResponseException e) {
        	log.trace e
        }
    }    
	return actname
}

def getHubName(hubId) {
	// GET HUB'S NAME
    def hubname = hubId
    if (state.HarmonyAccessToken) {
        def Params = [auth: state.HarmonyAccessToken]	    
        def url = "https://home.myharmony.com/cloudapi/hub/${hubId}/discover?${toQueryString(Params)}"
        try {
            httpGet(uri: url, headers: ["Accept": "application/json"]) {response -> 
                hubname = response.data.data.name
            }
        } catch (groovyx.net.http.HttpResponseException e) {
        	log.trace e
        }      
    }    
	return hubname
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