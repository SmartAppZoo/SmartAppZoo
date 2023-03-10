/**
 *  MyEcobeeInit (Service Manager)
 *  Copyright 2016-2020 Yves Racine
 *  LinkedIn profile: www.linkedin.com/in/yracine
 *  Refer to readme file for installation instructions.
 *     https://github.com/yracine/device-type.myecobee/blob/master/README.md
 *
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights,
 *  trade secret in the Background technology. May be subject to consulting fees under an Agreement 
 *  between the Developer and the Customer. Developer grants a non exclusive perpetual license to use
 *  the Background technology in the Software developed for and delivered to Customer under this
 *  Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * 
 * Software Distribution is restricted and shall be done only with Developer's written approval.
 *
 *
**/

import groovy.transform.Field

definition(
    name: "${get_APP_NAME()}",
    namespace: "yracine",
    author: "Yves Racine",
    description: "Connect your Ecobee thermostat(s) and switch(es) to SmartThings.",
    category: "My Apps",
    oauth: true,    
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)

def get_APP_VERSION() {return "5.2"}

preferences {
	page(name: "about", title: "About", nextPage: "auth")
	page(name: "auth", title: "ecobee", content:"authPage", nextPage:"deviceList")
	page(name: "deviceList", title: "ecobee", content:"ecobeeDeviceList",nextPage: "switchList")
	page(name: "switchList", title: "Ecobee Switches", content: "selectEcobeeSwitches", nextPage: "otherSettings")
	page(name: "otherSettings", title: "Other Settings", content:"otherSettings", install:true)
	page(name: "watchdogSettingsSetup")   
	page(name: "reportSettingsSetup")	        
	page(name: "cacheSettingsSetup")    
}


mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
	path("/callback") {action: [GET: "callback"]}    
}

def about() {
 	dynamicPage(name: "about", install: false, uninstall: true) {
 		section("") {	
			paragraph image:"${getCustomImagePath()}ecohouse.jpg","${get_APP_NAME()}, the smartapp that connects your Ecobee thermostat(s) and switch(es) to SmartT via cloud-to-cloud integration"
			paragraph "Version ${get_APP_VERSION()}\n" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright©2014-2020 Yves Racine"
				href url:"https://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information...", 
					description: "https://github.com/yracine/device-type.myecobee"
		}
		section("Cache Settings") {
			href(name: "toCacheSettingsSetup", page: "cacheSettingsSetup",required:false,  description: "Optional",
				title: "Cache settings for devices in Service Manager", image: "${getCustomImagePath()}cacheTimeout.jpg" ) 
		}        
        
	}        
}
def cacheSettingsSetup() {
	dynamicPage(name: "cacheSettingsSetup", title: "Cache Settings ", uninstall: false) {
 		section("To refresh your current available devices at ecobee, don't use the cache [default=cache is not used, use cache for better performances") {	
			input(name: "use_cache", title:"use of cached devices?", type: "bool", required:false, defaultValue: true)
			input(name: "cache_timeout", title:"Cache timeout in minutes (default=3 min)?", type: "number", required:false, description: "optional")
		}        
		section {
			href(name: "toAboutPage", title: "Back to About Page", page: "about")
		}
	}
} 
def otherSettings() {
	dynamicPage(name: "otherSettings", title: "Other Settings", install: true, uninstall: false) {
		section("Polling at which interval in minutes (range=[1,5,10,15,30],default=5 min.)?") {
			input "givenInterval", "enum", title:"Interval?", required: false, options:[1,5,10,15,30]
		}
		section("Handle/Notify any exception proactively [default=false, you will not receive any exception notification]") {
			input "handleExceptionFlag", "bool", title: "Handle exceptions proactively?", required: false
		}
		section("Scheduler's watchdog Settings (needed if any ST scheduling issues)") {
			href(name: "toWatchdogSettingsSetup", page: "watchdogSettingsSetup",required:false,  description: "Optional",
				title: "Scheduler's watchdog Settings", image: "${getCustomImagePath()}safeguards.jpg" ) 
		}
		section("Notifications & Logging") {
			input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required:
				false
			input "phoneNumber", "phone", title: "Send a text message?", required: false
			input "notifyAlerts", "bool", title: "Daily Notifications for any ecobee Alerts?", required:false
			input "detailedNotif", "bool", title: "Detailed Logging & Notifications?", required:false
			input "logFilter", "enum", title: "log filtering [Level 1=ERROR only,2=<Level 1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>]?", required:false,options:[1,2,3,4,5], defaultValue:1
		}
		section("Summary Report Settings") {
			href(name: "toReportSettingsSetup", page: "reportSettingsSetup",required:false,  description: "Optional",
				title: "Summary Reports via notifications/Ask Alexa", image: "${getCustomImagePath()}reports.jpg" ) 
		}
	}
}

def reportSettingsSetup() {
	dynamicPage(name: "reportSettingsSetup", title: "Summary Report Settings ", uninstall: false) {
		section("Report options: Daily/Weekly Summary reports are sent by notifications (right after midnight, early morning) and/or can be verbally given by Ask Alexa") {
			input (name:"tstatDaySummaryFlag", title: "include Past Day Summary Report for your Ecobee Tstat(s) [default=false]?", type:"bool",required:false)
			input (name:"tstatWeeklySummaryFlag", title: "include Weekly Summary Report for your Ecobee Tstat(s) [default=false]?", type:"bool",required:false)
		}
		section {
			href(name: "toOtherSettingsPage", title: "Back to Other Settings Page", page: "otherSettings")
		}
	}
}   

def watchdogSettingsSetup() {
	dynamicPage(name: "watchdogSettingsSetup", title: "Scheduler's Watchdog Settings ", uninstall: false) {
		section("Watchdog options: the watchdog should be a single polling device amongst the choice of sensors below. The watchdog needs to be regularly polled every 5-10 minutes and will be used as a 'heartbeat' to reschedule if needed.") {
			input (name:"tempSensor", type:"capability.temperatureMeasurement", title: "What do I use as temperature sensor to restart smartapp processing?",
				required: false, description: "Optional Watchdog- just use a single polling device")
			input (name:"motionSensor", type:"capability.motionSensor", title: "What do I use as a motion sensor to restart smartapp processing?",
				required: false, description: "Optional Watchdog -just use a single polling device")
			input (name:"energyMeter", type:"capability.powerMeter", title: "What do I use as energy sensor to restart smartapp processing?",
				required: false, description: "Optional Watchdog-  just use a single polling device")
			input (name:"powerSwitch", type:"capability.switch", title: "What do I use as Master on/off switch to restart smartapp processing?",
				required: false, description: "Optional Watchdog - just use a single  polling device")
		}
		section {
			href(name: "toOtherSettingsPage", title: "Back to Other Settings Page", page: "otherSettings")
		}
        
	}
}   

def authPage() {
settings.detailedNotif=true
settings.logFilter=5

//	def settingKey= "detailedNotif"   
//  app.updateSetting(settingKey, [value: "true", type: "bool"])        
//  settingKey= "logFilter"   
//  app.updateSetting(settingKey, [value: "5", type: "int"])  
    
	traceEvent(settings.logFilter,"authPage(),atomicState.oauthTokenProvided=${atomicState?.oauthTokenProvided}", detailedNotif)

	if (!atomicState.accessToken) {
		traceEvent(settings.logFilter,"about to create access token", detailedNotif)
		try {        
			createAccessToken()
		} catch (e) {
			traceEvent(settings.logFilter,"authPage() exception $e, not able to create access token, probable cause: oAuth is not enabled for MyEcobeeInit smartapp ", true, GLOBAL_LOG_ERROR, true)
			exit            
		}        
		atomicState.accessToken = state.accessToken
	}

	def description = "Required"
	def uninstallAllowed = false

	if (atomicState?.authToken) {

		if (!atomicState?.jwt) {
			atomicState?.jwt = true
			refreshAuthToken()
		}
		// TODO: Check if it's valid
		if (true) {
			description = "You are already connected to ecobee. You just need to tap the upper right 'Next' button.\n\nIf text in red, please re-login at ecobee by pressing here as there was a connection error."
			uninstallAllowed = true
			atomicState?.oauthTokenProvided=true            
		} else {
			description = "ecobee Connection Required, press here for login prompt." // Worth differentiating here vs. not having atomicState.authToken? 
		}
	}

	def redirectUrl = "${get_ST_URI_ROOT()}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${getServerUrl()}"

	traceEvent(settings.logFilter,"authPage>atomicState.authToken=${atomicState.authToken},atomicState.oauthTokenProvided=${atomicState?.oauthTokenProvided}, RedirectUrl = ${redirectUrl}",
		detailedNotif)


	// get rid of next button until the user is actually auth'd

	if (!atomicState?.oauthTokenProvided) {

		return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed, submitOnChange: true) {
			section(){
				paragraph "Tap below to log in to the ecobee portal and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
				href url:redirectUrl, style:"external", required:true, title:"ecobee Connection>", description:description
			}
		}

	} else {

		return dynamicPage(name: "auth", title: "Log In", nextPage:"deviceList", uninstall:uninstallAllowed,submitOnChange: true) {
			section(""){
				paragraph "When connected to ecobee,just tap the upper right Next to continue to setup your ecobee devices"
				href url:redirectUrl, style:"external", state:"complete", title:"ecobee Connection Status>", description:description
			}
		}

	}


}

def ecobeeDeviceList() {
	traceEvent(settings.logFilter,"ecobeeDeviceList>begin", detailedNotif, GLOBAL_LOG_TRACE)
	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:3   // set an execution interval to avoid unecessary queries to Ecobee
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}    
	def tstatDNIs= [:]
    
	if (!use_cache) { 
		traceEvent(settings.logFilter,"ecobeeDeviceList>about to get thermostat list from ecobee", detailedNotif)    
		def stats = getEcobeeThermostats("registered",use_cache)

		traceEvent(settings.logFilter,"ecobeeDeviceList>device list: $stats", detailedNotif)
/*
        def ems = getEcobeeThermostats("ems", use_cache)
		traceEvent(settings.logFilter,"ecobeeDeviceList>ems device list: $ems", detailedNotif)
		tstatDNIs = ems 
*/
		tstatDNIs = stats 
	} 
	if (!tstatDNIs) {
		def tstat=[:]
		traceEvent(settings.logFilter,"ecobeeDeviceList>about to get thermostat list from previously stored settings.thermostats", detailedNotif)    
		thermostats.each {
			def tstatInfo=it.tokenize('.')
			def name=tstatInfo[1]            
			tstat[it]=name
			tstatDNIs << tstat            
		}        
	}
	int tstatCount=tstatDNIs.size()    
	traceEvent(settings.logFilter,"ecobeeDeviceList>${tstatCount} found, device list: $tstatDNIs", detailedNotif)

	def p = dynamicPage(name: "deviceList", title: "Select Your Thermostats to be exposed to SmartThings (${tstatCount} found)", uninstall: true) {
		section(""){
			paragraph image: "${getCustomImagePath()}ecobee4.jpg", "Tap below to see the list of ecobee thermostats available in your ecobee account.\n\nIf you have disconnect issues with your ST account, select only 1 tstat and create 1 instance of MyEcobeeInit per tstat and use a single watchdog in OtherSettings"
			input(name: "thermostats", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", options:tstatDNIs)
		}
	}

	traceEvent(settings.logFilter,"ecobeeDeviceList>list p: $p",detailedNotif)
	return p
}


def selectEcobeeSwitches() {
	traceEvent(settings.logFilter,"selectEcobeeSwitches>begin", detailedNotif, GLOBAL_LOG_TRACE)

	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:3   // set an execution interval to avoid unecessary queries to Ecobee
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}    
	atomicState?.lastExecutionTimestamp=now()

	def switchDNIs= [:]
    
	if (!use_cache) { 
		traceEvent(settings.logFilter,"selectEcobeeSwitches>about to get switch list from ecobee", detailedNotif)    
		switchDNIs= getEcobeeSwitches(false)
		
	}
	if (!switchDNIs) {
		def aSwitch=[:]
		traceEvent(settings.logFilter,"selectEcobeeSwitches>about to get switch list from previously stored settings.ecobeeSwitches", detailedNotif)    
		ecobeeSwitches.each {
			def switchInfo=it.toString().tokenize('.')
			def name=switchInfo[1]            
			aSwitch[it]=name
			switchDNIs << aSwitch            
		}        
	}
	int switchCount=switchDNIs.size()    
	traceEvent(settings.logFilter,"selectEcobeeSwitches>${switchCount} found, device list: $switchDNIs", detailedNotif)

	def p = dynamicPage(name: "switchList", title: "Select Your Switches to be exposed to SmartThings (${switchCount} found)", uninstall: true) {
		section(""){
			paragraph image: "${getCustomImagePath()}ecobeeSwitch.jpg", "Tap below to see the list of ecobee switches available in your ecobee account."
			input(name: "ecobeeSwitches", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", options:switchDNIs)
		}
	}

	traceEvent(settings.logFilter,"selectEcobeeSwitches>list p: $p",detailedNotif)
	return p
}

def setParentAuthTokens(auth_data) {
	if (auth_data.authexptime.toLong() > atomicState.authexptime.toLong()) {
		if (handleException) {
/*
			For Debugging purposes, due to the fact that logging is not working when called (separate thread)
			send("MyEcobeeInit>setParentAuthTokens>begin auth_data: $auth_data")
*/
			traceEvent(settings.logFilter,"setParentAuthTokens>begin auth_data: $auth_data",detailedNotif)
		} 
		save_auth_data(auth_data)        
		refreshAllChildAuthTokens()
		if (handleException) {
/*
			For Debugging purposes, due to the fact that logging is not working when called (separate thread)
			send("MyEcobeeInit>setParentAuthTokens>atomicState =$atomicState")
*/
			traceEvent(settings.logFilter,"setParentAuthTokens>setParentAuthTokens>atomicState =$atomicState",detailedNotif)
		}            
	}        

}

void save_auth_data(auth_data) {

	atomicState.refreshToken = auth_data?.refresh_token
	atomicState.authToken = auth_data?.access_token
	atomicState.expiresIn=auth_data?.expires_in
	atomicState.tokenType = auth_data?.token_type
	atomicState.authexptime= auth_data?.authexptime
	atomicState.jwt= auth_data?.jwt
	traceEvent(settings.logFilter,"save_auth_data>atomicState =$atomicState",detailedNotif)
}

def refreshAllChildAuthTokens() {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"refreshAllChildAuthTokens>begin updating children with ${atomicState.auth}")
*/

	def children= getChildDevices()
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"refreshAllChildAuthtokens> refreshing ${children.size()} thermostats",detailedNotif)
*/

	children.each { 
/*
		For Debugging purposes, due to the fact that logging is not working when called (separate thread)
		traceEvent(settings.logFilter,"refreshAllChildAuthTokens>begin updating $it.deviceNetworkId with ${$atomicState.auth}",detailedNotif)
*/
    	it.refreshChildTokens(atomicState) 
	}
}
def refreshThisChildAuthTokens(child) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"refreshThisChildAuthTokens>begin child id: ${child.device.deviceNetworkId}, updating it with ${atomicState}", detailedNotif)
*/
	child.refreshChildTokens(atomicState)

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("refreshThisChildAuthTokens>end")
*/
}

synchronized boolean refreshParentTokens() {

	if (isTokenExpired()) {
		if (refreshAuthToken()) {
			refreshAllChildAuthTokens()
			return true            
		}		        
	} else {
		refreshAllChildAuthTokens()
		return true            
	}    
	return false    
    
}
private def getEcobeeThermostats(String type="registered", useCache=true) {
	settings.logFilter=5
	traceEvent(settings.logFilter,"getEcobeeThermostats>begin getting ecobee Thermostats list", detailedNotif)
	def msg
	def stats = [ : ]
	boolean use_cache=useCache    
 
	refreshParentTokens()
	if (atomicState?.revisionList_timestamp==null) {
		use_cache=false    
	} else {
		def last_execution_interval= (settings.givenInterval)?:(settings.cache_timeout?:3)   // set an execution interval to avoid unecessary queries to Ecobee
		def time_check_for_execution = (now() - (last_execution_interval.toInteger() * 60 * 1000))
		if ((use_cache) && (atomicState?.revisionList_timestamp.toLong() < time_check_for_execution.toLong())) {
			use_cache=false	    
		}    
		traceEvent(settings.logFilter,"getEcobeeThermostats>use_cache=$use_cache,time_check_for_execution: $time_check_for_execution vs. timestamp: ${atomicState?.revisionList_timestamp}", detailedNotif)
    }
    if ((!use_cache) || (atomicState?.revisionList==null)) {

		def requestBody = build_body_request('thermostatSummary',type,"",null)
		def args_encoded = java.net.URLEncoder.encode(requestBody.toString(), "UTF-8")   
		traceEvent(settings.logFilter,"getEcobeeThermostats>requestBody=${requestBody}", detailedNotif)
		def deviceListParams = [
		    uri: "${get_URI_ROOT()}/${get_API_VERSION()}/thermostatSummary?format=json&body=${args_encoded}",
	    	headers: ['Content-Type': "application/json", "Authorization": "Bearer ${atomicState.authToken}", 'charset': "UTF-8",'Accept': "application/json"]
		]

    	traceEvent(settings.logFilter,"getEcobeeThermostats>device list params: $deviceListParams",detailedNotif)

	    int statusCode=1
		try {
	    	httpGet(deviceListParams) { resp ->
			statusCode = resp?.data?.status?.code
		    	if ((resp?.status == 200) && (!statusCode)) {
                    
			    	atomicState?.revisionList = resp.data?.revisionList
			    	atomicState?.statusList = resp.data?.statusList
			    	atomicState?.thermostatCount =resp.data?.thermostatCount
					atomicState?.revisionList_timestamp=now()                    
        			for (i in 0..resp.data.thermostatCount - 1) {
    	    			def thermostatDetails = resp.data.revisionList[i].split(':')
	    	    		def id = thermostatDetails[0]
    			    	def thermostatName = thermostatDetails[1]
        				def connected = thermostatDetails[2]
	        			def thermostatRevision = thermostatDetails[3]
    		    		def alertRevision = thermostatDetails[4]
    			    	def runtimeRevision = thermostatDetails[5]
    				    def intervalRevision = thermostatDetails[6]
    	    			traceEvent(settings.logFilter,"getEcobeeThermostats>got from ecobee, found thermostatId=${id},name=${thermostatName},connected =${connected}",settings.trace)
	    	    		traceEvent(settings.logFilter,"getEcobeeThermostats>intervalRevision=${intervalRevision},runtimeRevision=${runtimeRevision},thermostatRevision=${thermostatRevision}")
    	    			def dni = [ app.id, thermostatName, id].join('.')
		    			traceEvent(settings.logFilter,"getEcobeeThermostats>got from ecobee, found ${thermostatName}, identifier=${id}, dni=$dni", detailedNotif)
    					stats[dni] = thermostatName
	    			} /* end for */
		    	} else {
			    	traceEvent(settings.logFilter,"getEcobeeThermostats>http status: ${resp.status}",detailedNotif)
				}
        	}        
    	} catch (java.net.UnknownHostException e) {
	    	msg ="Unknown host - check the URL " + deviceListParams.uri
    		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
    	} catch (java.net.NoRouteToHostException t) {
	    	msg= "No route to host - check the URL " + deviceListParams.uri
    		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
	    } catch (java.io.IOException e) {
    		traceEvent(settings.logFilter,"getEcobeeThermostats>$e while getting list of thermostats, probable cause: not the right account for this type (${type}) of thermostat " +
			deviceListParams, detailedNotif, GLOBAL_LOG_INFO)            
    	} catch (e) {
	    	msg= "exception $e while getting list of thermostats, status=${e?.getStatusCode()}" 
				    //refresh the auth token
			if (e?.response?.status == 500) {
	    		if (handleException) {            
		    		traceEvent(settings.logFilter,"getEcobeeThermostats>Need to refresh your auth_token!, about to call refreshAuthToken()",detailedNotif, GLOBAL_LOG_ERROR)
			    }                        
				refreshParentTokens()

    		} else {
	    		if (handleException) {            
		    		traceEvent(settings.logFilter,"getEcobeeThermostats>http status=${resp.status}: authentication error, invalid authentication method, lack of credentials, (resp status= ${resp.data.status.code})",
			    		true, GLOBAL_LOG_ERROR, true)
				}                        
	    	}
	    }
    } else {

        for (i in 0..(atomicState?.revisionList.size() - 1)) {
			def thermostatDetails = atomicState?.revisionList[i].split(':')
			def id = thermostatDetails[0]
			def thermostatName = thermostatDetails[1]
			def connected = thermostatDetails[2]
			def thermostatRevision = thermostatDetails[3]
			def alertRevision = thermostatDetails[4]
			def runtimeRevision = thermostatDetails[5]
			def intervalRevision = thermostatDetails[6]
			traceEvent(settings.logFilter,"getEcobeeThermostats>found in cache, thermostatId=${id},name=${thermostatName},connected =${connected}",settings.trace)
			traceEvent(settings.logFilter,"getEcobeeThermostats>intervalRevision=${intervalRevision},runtimeRevision=${runtimeRevision},thermostatRevision=${thermostatRevision}")
			def dni = [ app.id, thermostatName, id].join('.')
			traceEvent(settings.logFilter,"getEcobeeThermostats>found ${thermostatName}, identifier=${id}, dni=$dni", detailedNotif)
			stats[dni] = thermostatName
		} /* end for */
                
    }        

	traceEvent(settings.logFilter,"getEcobeeThermostats>thermostats: $stats", detailedNotif)

	return stats
}

private def getEcobeeSwitches(useCache=true) {
//	settings.logFilter=5
	String ECOBEE_TYPE_SWITCH="LIGHT_SWITCH"  
	def msg    
    def switches=[ : ]   
 
	refreshParentTokens()
 
	traceEvent(settings.logFilter,"getEcobeeSwitches>about to get devices", detailedNotif)
	boolean use_cache=useCache    
 
    
	if (atomicState?.switchList_timestamp==null) {
		use_cache=false    
	} else {
		def last_execution_interval= (settings.givenInterval)?:(settings.cache_timeout?:3)   // set an execution interval to avoid unecessary queries to Ecobee
		def time_check_for_execution = (now() - (last_execution_interval.toInteger() * 60 * 1000))
		if ((use_cache) && (atomicState?.switchList_timestamp.toLong() < time_check_for_execution.toLong())) {
			use_cache=false	    
		}    
		traceEvent(settings.logFilter,"getEcobeeSwitches>use_cache=$use_cache,time_check_for_execution: $time_check_for_execution vs. timestamp: ${atomicState?.revisionList_timestamp}", detailedNotif)
    }
    
    if ((!use_cache) || (atomicState?.switchList==null)) {

    	def switchListParams = [
	    	uri: "${get_URI_ROOT()}" +"/ea/devices",
    		headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
    	]
    	try {
	    	httpGet(switchListParams) { response ->
		    	if (response?.status && response?.status == 200) {
			    	if (response?.data) {
				    	traceEvent(settings.logFilter,"getEcobeeSwitches>got from ecobee, status=${response.status}, data=${response.data}", detailedNotif)
    					def switchList =[]             
	    				switchList=  response.data?.devices                   
    					atomicatomicState?.switchList  = switchList
						atomicState?.switchList_timestamp=now()                
	    				traceEvent(settings.logFilter,"getEcobeeSwitches>atomicState.switchList= ${atomicState.switchList}", detailedNotif)
		    			switchList.each {                
			    			if (it.type == ECOBEE_TYPE_SWITCH && (it?.name) && (it?.identifier)) {
				    			def name=it.name
    							def id =it.identifier
	    						def dni = [ app.id, name, id].join('.')
			    				traceEvent(settings.logFilter,"getEcobeeSwitches>got ${name}, id=${id}, dni = $dni from ecobee", detailedNotif)
		    					switches[dni] = name 							
				    		}
    					} 
                     
			    	} /* end if data */
    			} else {
	    			traceEvent(settings.logFilter,"getEcobeeSwitches>http status: ${response.status}",detailedNotif)
		    	}
    		}           
	    } catch (java.net.UnknownHostException e) {
		    msg ="Unknown host - check the URL " + switchListParams.uri
    		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
	    } catch (java.net.NoRouteToHostException t) {
		    msg= "No route to host - check the URL " + switchListParams.uri
		    traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
    	} catch (e) {
 			traceEvent(settings.logFilter,"getEcobeeSwitches>http error: ${e?.response?.status}",detailedNotif)
	    	msg= "exception $e while getting list of switches" 
		    //refresh the auth token
			if (e?.response?.status == 500) {
				    if (handleException) {            
    					traceEvent(settings.logFilter,"getEcobeeSwitches>Need to refresh your auth_token!, about to call refreshAuthToken()",detailedNotif, GLOBAL_LOG_ERROR)
	    			}                        
					refreshParentTokens()

                    
	    	} else {
		    	if (handleException) {            
			    	traceEvent(settings.logFilter,"getEcobeeSwitches>http status=${response.status}: authentication error, invalid authentication method, lack of credentials, (resp status= ${response.data.status.code})",
				    		true, GLOBAL_LOG_ERROR, true)
    			}                        
	    	}
	    }
    } else {

    	atomicState?.switchList.each { 
            if (it.type == ECOBEE_TYPE_SWITCH && (it?.name) && (it?.identifier)) {
                def name=it.name
                def id =it.identifier
                def dni = [ app.id, name, id].join('.')
                traceEvent(settings.logFilter,"getEcobeeSwitches>found in cache ${name}, id=${id}, dni = $dni", detailedNotif)
                switches[dni] = name 							
            }
    	}
                
    }        
	traceEvent(settings.logFilter,"getEcobeeSwitches>switches = ${switches}", detailedNotif)
	return switches 
    
}

def updateObjects(child, objectType, objectId="") {

    traceEvent(settings.logFilter,"updateObjects>${objectType}List=" + atomicState?."${objectType}List", detailedNotif)
	traceEvent(settings.logFilter,"updateObjects>about to try to find type=$objectType && objectId=$objectId", detailedNotif)
	def objectsToBeUpdated=[]
	def list=atomicState?."${objectType}List"
	boolean foundObject=false
	if (objectId) {
		list.each {
			if (it.identifier==objectId) {
				objectsToBeUpdated << it
				foundObject=true            
			}	
		}            
	}    
	if (foundObject && objectId) {
		traceEvent(settings.logFilter,"updateObjects> found $objectId", detailedNotif)
	} else if (objectId) {
		traceEvent(settings.logFilter,"updateObjects> $objectId not found", detailedNotif)
    
	} else {
		objectsToBeUpdated=atomicState?."${objectType}List"    
	}   
	if (objectsToBeUpdated) {
		child.updateChildData(objectsToBeUpdated)
        if ((objectType=="thermostat") && (objectId != null)) {        
            traceEvent(settings.logFilter,"updateObjects>thermostat=$objectId, runtime=${objectsToBeUpdated.runtime}", detailedNotif)
            traceEvent(settings.logFilter,"updateObjects>thermostat=$objectId, settings=${objectsToBeUpdated.settings}", detailedNotif)
        }            
	}        
	traceEvent(settings.logFilter,"updateObjects>end with type=$objectType && objectId=$objectId", detailedNotif)
    
}  

def updateStructure(child) {

    traceEvent(settings.logFilter,"updateStructure>revisionList=" + atomicState?.revisionList, detailedNotif)
	def objectsToBeUpdated=[:]
	objectsToBeUpdated?.revisionList = atomicState?.revisionList
	objectsToBeUpdated?.statusList = atomicState?.statusList
	objectsToBeUpdated?.thermostatCount = atomicState?.thermostatCount
	if (objectsToBeUpdated) {
		child.updateChildStructure(objectsToBeUpdated)
	}        
	traceEvent(settings.logFilter,"updateStructure>end with type=$objectType && objectId=$objectId, objectsToBeUpdated=$objectsToBeUpdated", detailedNotif)
}  

// tstatType =managementSet or registered (no spaces).  
//		registered is for SMART & SMART-SI thermostats, 
//		managementSet is for EMS thermostat
//		may also be set to a specific locationSet (ex. /Toronto/Campus/BuildingA)
//		may be set to null if not relevant for the given method
// thermostatId may be a list of serial# separated by ",", no spaces (ex. '123456789012,123456789013') 
private def build_body_request(method, tstatType="registered", thermostatId, tstatParams = [],tstatSettings = [], includeSensor=false) {
	def selectionJson = null
	def selection = null  
	if (tstatType==null || tstatType=="") tstatType='registered'    
	if (method == 'thermostatSummary') {
		if (tstatType.trim().toUpperCase() == 'REGISTERED') {
			selection = [selection: [selectionType: 'registered', selectionMatch: '',
							includeEquipmentStatus: 'true']
						]
		} else {
			// If tstatType is different than managementSet, it is assumed to be locationSet specific (ex./Toronto/Campus/BuildingA)
			selection = (tstatType.trim().toUpperCase() == 'MANAGEMENTSET') ? 
				// get all EMS thermostats from the root
				[selection: [selectionType: 'managementSet', selectionMatch: '/',
					includeEquipmentStatus: 'true']
				] : // Or Specific to a location
				[selection: [selectionType: 'managementSet', selectionMatch: tstatType.trim(),
					includeEquipmentStatus: 'true']
				]
		}
		selectionJson = new groovy.json.JsonBuilder(selection)
		return selectionJson
	} else if (method == 'thermostatInfo') {
		selection = [selection: [selectionType: 'thermostats',
			selectionMatch: thermostatId,
			includeSettings: 'true',
			includeRuntime: 'true',
			includeProgram: 'true',           
			includeWeather: 'true',            
			includeAlerts: 'true',
			includeEvents: 'true',
			includeEquipmentStatus: 'true',
			includeSensors: 'true',
			includeAudio: 'true'            
			]
		]
		selectionJson = new groovy.json.JsonBuilder(selection)
		return selectionJson
	} else if (method == 'remoteSensorUpdate') {
		selection = [selection: [selectionType: 'thermostats',
			selectionMatch: thermostatId,
			includeSensors: 'true'
			]
		]
		selectionJson = new groovy.json.JsonBuilder(selection)
		return selectionJson
	} else {
		selection = [selectionType: 'thermostats', selectionMatch: thermostatId]
	}
	selectionJson = new groovy.json.JsonBuilder(selection)
	if ((method != 'setThermostatSettings') && (tstatSettings != null) && (tstatSettings != [])) {
		def function_clause = ((tstatParams != null) && (tsatParams != [])) ? 
			[type:method, params: tstatParams] : 
			[type: method]
		def bodyWithSettings = [
				functions: [function_clause], selection: selection,
				thermostat: [settings: tstatSettings]
			]
		def bodyWithSettingsJson = new groovy.json.JsonBuilder(bodyWithSettings)
		return bodyWithSettingsJson
	} else if (method == 'setThermostatSettings') {
		def bodyWithSettings = [
				selection: selection,thermostat: [settings: tstatSettings]
			]
		def bodyWithSettingsJson = new groovy.json.JsonBuilder(bodyWithSettings)
		return bodyWithSettingsJson
	} else if ((tstatParams != null) && (tsatParams != [])) {
		def function_clause = [type: method, params: tstatParams]
		def simpleBody = [functions: [function_clause], selection: selection]
		def simpleBodyJson = new groovy.json.JsonBuilder(simpleBody)
		return simpleBodyJson
	} else {
		def function_clause = [type: method]
		def simpleBody = [functions: [function_clause], selection: selection]
		def simpleBodyJson = new groovy.json.JsonBuilder(simpleBody)
		return simpleBodyJson
    }    
}


// thermostatId may be a list of serial# separated by ",", no spaces (ex. '123456789012,123456789013') 

def getThermostatInfo(thermostatId, useCache=true) {
//	settings.trace=true
//	settings.logFilter=5    
	def ECOBEE_NEED_TOKEN_REFRESH=14
	def TOKEN_EXPIRED=401   
	def interval    
    def msg
	boolean use_cache=useCache    
 
    
	refreshParentTokens()
	thermostatId=(thermostatId==null) ? "" : thermostatId

	if (atomicState?.thermostatList_timestamp==null) {
		use_cache=false    
	} else {
    
		def last_execution_interval= (settings.givenInterval)?:(settings.cache_timeout?:3)   // set an execution interval to avoid unecessary queries to Ecobee
		def time_check_for_execution = (now() - (last_execution_interval.toInteger() * 60 * 1000))
		if ((use_cache) && (atomicState?.thermostatList_timestamp.toLong() < time_check_for_execution.toLong())) {
			use_cache=false	    
		}    
		traceEvent(settings.logFilter,"getThermostatInfo>use_cache=$use_cache,time_check_for_execution: $time_check_for_execution vs. timestamp: ${atomicState?.thermostatList_timestamp}", detailedNotif)
    }
    def stats = []
    if ((!use_cache) || (atomicState?.thermostatList==null)) {    
    	traceEvent(settings.logFilter,"getThermostatInfo> about to call build_body_request for thermostatId=${thermostatId}...",settings.trace)
	    def requestBody = build_body_request('thermostatInfo',null,thermostatId,null)
		traceEvent(settings.logFilter,"getThermostatInfo> about to call api with body = ${requestBody}...",settings.trace)
	    int statusCode=1
		def args_encoded = java.net.URLEncoder.encode(requestBody.toString(), "UTF-8")   
 		def deviceListParams = [
		    uri: "${get_URI_ROOT()}/${get_API_VERSION()}/thermostat?format=json&body=${args_encoded}",
			headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}",'Accept': "application/json"]
		]

	    traceEvent(settings.logFilter,"getThermostatInfo>device list params: $deviceListParams",detailedNotif)

		try {
	    	httpGet(deviceListParams) { resp ->
//				traceEvent(settings.logFilter,"getThermostatInfo>got from ecobee: resp?.status= ${resp?.status}, resp?.data=${resp?.data}",settings.trace )
				statusCode = resp?.data?.status?.code
    		    if ((resp?.status ==200) && (!statusCode)) {
			    /* when success, reset the exception counter */
	    		    atomicState.exceptionCount=0
                    stats=resp.data.thermostatList     
                    stats.each {
            			def id = it.identifier           
            			def thermostatName = it.name           
    	        		def runtimeSettings = it.runtime
        		    	def thermostatSettings = it.settings
	        		    traceEvent(settings.logFilter,"getThermostatInfo>got from ecobee: thermostatId=${id},name=${thermostatName},hvacMode=${thermostatSettings.hvacMode}," +
		        			"fan=${runtimeSettings.desiredFanMode},fanMinOnTime=${thermostatSettings.fanMinOnTime},desiredHeat=${runtimeSettings.desiredHeat},desiredCool=${runtimeSettings.desiredCool}," +
    			    		"heatRangeHigh=${thermostatSettings.heatRangeHigh},heatRangeLow=${thermostatSettings.heatRangeLow},coolRangeHigh=${thermostatSettings.coolRangeHigh},coolRangeLow=${thermostatSettings.coolRangeLow}," +
	    			    	"current Humidity= ${runtimeSettings.actualHumidity},desiredHumidity=${runtimeSettings.desiredHumidity},humidifierMode=${thermostatSettings.humidifierMode}," +
		    			    "desiredDehumidity= ${runtimeSettings.desiredDehumidity},dehumidifierMode=${thermostatSettings.dehumidifierMode}", settings.trace)         
                        traceEvent(settings.logFilter,"getTstatInfo>done for ${thermostatId}",settings.trace)
                    } /* for each thermostat */  
					atomicState?.thermostatList = stats                
					atomicState?.thermostatList_timestamp=now()                
        		} /* end if statusCode */                
            } /* end httpGet */                
	    } catch (java.net.UnknownHostException e) {
    		msg ="Unknown host - check the URL " + switchListParams.uri
	    	traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
    	} catch (java.net.NoRouteToHostException t) {
	    	msg= "No route to host - check the URL " + switchListParams.uri
    		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
	    } catch (e) {
 			traceEvent(settings.logFilter,"getThermostatInfo>http error: ${e?.response?.status}",detailedNotif)
       
		    msg= "exception $e while getting thermstat info on $thermostatId" 
    		if (handleException) {            
	    		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
    		}  
			if (e?.response?.status == 500) {
       		    if (handleException) {            
					traceEvent(settings.logFilter,"getThermostatInfo>Need to refresh your auth_token!, about to call refreshAuthToken()",detailedNotif, GLOBAL_LOG_ERROR)
				}                        
				refreshParentTokens()
			}
	    } /* end catch exception */
    } else {
        
    	atomicState?.thermostatList.each { 
            def name=it.name
            def id =it.identifier
            traceEvent(settings.logFilter,"getThermostatInfo>found in cache: ${name}, id=${id}", detailedNotif)
    	}
        stats=atomicState?.thermostatList        
        
    }        
    
    return stats    
}



def refreshAuthToken() {
	traceEvent(settings.logFilter,"refreshAuthToken>about to refresh auth token", detailedNotif)
	boolean result=false
	def REFRESH_SUCCESS_CODE=200    
	def UNAUTHORIZED_CODE=401    
    
	def stcid = getSmartThingsClientId()

	if(!atomicState.refreshToken) {
		traceEvent(settings.logFilter, "Cannot refresh OAuth token since there is no refreshToken stored",detailedNotif)
		return false        
	}
	def refreshParams = [
			method: 'POST',
			uri   : "${get_URI_ROOT()}",
			path  : "/token",
			query: []            
		]
	if (atomicState?.jwt) {
			refreshParams?.query = [
				grant_type:    "refresh_token",
				refresh_token:	atomicState.refreshToken,
				client_id :		stcid,
				ecobee_type:   "jwt"
			]
	} else {
			refreshParams?.query = [
				query : [grant_type: 'refresh_token', code: "${atomicState.refreshToken}", client_id: stcid]
			]
	}
	

    
	try {
    
		httpPost(refreshParams) { resp ->

			if (resp?.status == REFRESH_SUCCESS_CODE) {
				traceEvent(settings.logFilter,"refreshAuthToken>Token refresh done resp = ${resp?.data}", detailedNotif)


				if (resp.data) {

					traceEvent(settings.logFilter,"refreshAuthToken>resp.data",detailedNotif)
					atomicState.refreshToken = resp?.data?.refresh_token
					atomicState.authToken = resp?.data?.access_token
					atomicState.expiresIn=resp?.data?.expires_in
					atomicState.tokenType = resp?.data?.token_type
					atomicState?.clientId=stcid                    
					def authexptime = new Date((now() + (resp?.data?.expires_in  * 1000))).getTime()
					atomicState.authexptime=authexptime 						                        
					traceEvent(settings.logFilter,"refreshAuthToken>new refreshToken = ${atomicState.refreshToken}", detailedNotif)
					traceEvent(settings.logFilter,"refreshAuthToken>new authToken = ${atomicState.authToken}", detailedNotif)
					if (handleException) {                        
						traceEvent(settings.logFilter,"MyEcobeeInit>refreshAuthToken>,new authToken = ${atomicState.authToken}", detailedNotif)
						traceEvent(settings.logFilter,"refreshAuthToken>new authexptime = ${atomicState.authexptime}", detailedNotif)
					}                            
					traceEvent(settings.logFilter,"refreshAuthToken>new authexptime = ${atomicState.authexptime}", detailedNotif)
					result=true                    

				} /* end if resp.data */
			} else { 
				result=false                    
				traceEvent(settings.logFilter,"refreshAuthToken>refreshAuthToken failed ${resp.status} : ${resp.status.code}", detailedNotif)
				if (handleException) {            
					traceEvent(settings.logFilter,"refreshAuthToken failed ${resp.status} : ${resp.status.code}", detailedNotif)
				} /* end handle expception */                        
			} /* end if resp.status==200 */
		} /* end http post */
	} catch (groovyx.net.http.HttpResponseException e) {
			atomicState.exceptionCount=atomicState.exceptionCount+1             
			if (e.statusCode == UNAUTHORIZED_CODE) { //this issue might comes from exceed 20sec app execution, connectivity issue etc
				log.error "refreshAuthToken>exception $e"
				if (handleException) {            
					traceEvent(settings.logFilter,"refreshAuthToken>exception $e", detailedNotif,GLOBAL_LOG_ERROR, true)
				}            
			}            
	}    
    
    return result
}



def installed() {
	settings.detailedNotif=true 		// initial value
	settings.logFilter=GLOBAL_LOG_DEBUG	// initial value
	traceEvent(settings.logFilter,"Installed with settings: ${settings}", detailedNotif)
	initialize()
}

def updated() {
	traceEvent(settings.logFilter,"Updated with settings: ${settings}", detailedNotif)

	unsubscribe()
	try {    
		unschedule()
	} catch (e) {
		traceEvent(settings.logFilter,"updated>exception $e, continue processing", detailedNotif)    
	}    
	initialize()
}

def uninstalled() {
	delete_child_devices()
	revokeAccessToken()     
}


def offHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value", detailedNotif)
}



def rescheduleHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value", detailedNotif)
	rescheduleIfNeeded()		
}


private def delete_child_devices() {
	def deleteTstats=[], deleteSwitches=[]
    
	// Delete any that are no longer in settings

	def child_devices=getChildDevices()
	if(!thermostats) {
		deleteTstats = child_devices.findAll {  (it.getName()?.contains(getChildName())) }
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete all ecobee thermostats", detailedNotif)
 	} else {
		deleteTstats = child_devices.findAll { ((!thermostats?.contains(it.deviceNetworkId)) && (it.getName()?.contains(getChildName()))) }
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteTstats.size()} ecobee thermostats", detailedNotif)
 	}

	deleteTstats.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"delete_child_devices>exception $e while deleting ecobee thermostat ${it.deviceNetworkId}", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}
	traceEvent(settings.logFilter,"delete_child_devices>deleted ${deleteTstats.size()} ecobee thermostats", detailedNotif)


	if(!ecobeeSwitches) {
		deleteSwitches = child_devices.findAll {  (it.getName()?.contains(getChildSwitchName())) }
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete all ecobee switches", detailedNotif)
 	} else {
		if (ecobeeSwitches) {    
			deleteSwitches = child_devices.findAll { ((!ecobeeSwitches?.containsKey(it.deviceNetworkId)) && (it.getName()?.contains(getChildSwitchName()))) }
		}            
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteSwitches.size()} ecobee switches", detailedNotif)
 	}

	deleteSwitches.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"delete_child_devices>exception $e while deleting ecobee switch ${it.deviceNetworkId}", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}
	traceEvent(settings.logFilter,"delete_child_devices>deleted ${deleteSwitches.size()} ecobee switches", detailedNotif)
}


def create_child_thermostats() {

   	int countNewChildDevices =0
	traceEvent(settings.logFilter,"create_child_thermostats>About to loop thru thermostats $thermostats", detailedNotif)
	def allChildDevices=getChildDevices()    
	def devices = thermostats.collect { dni ->

		traceEvent(settings.logFilter,"create_child_thermostats>looping thru thermostats, found dni $dni", detailedNotif)
		def tstat_info  = dni.tokenize('.')
		def thermostatId = tstat_info.last()
 		def name = tstat_info[1]
 		def d= allChildDevices.find {it.deviceNetworkId.contains(thermostatId)}               
		traceEvent(settings.logFilter,"create_child_thermostats>looping thru thermostats, found device $d", detailedNotif)

		if(!d) {
			def labelName = 'My ecobee ' + "${name}"
			traceEvent(settings.logFilter,"create_child_thermostats>about to create child device with id $dni, thermostatId = $thermostatId, name=  ${name}", detailedNotif)
			d = addChildDevice(getTstatChildNamespace(), getChildName(), dni, null,[label: "${labelName}"]) 
			d.initialSetup( getSmartThingsClientId(), atomicState, thermostatId ) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_thermostats>created ${labelName} with dni $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_thermostats>found ${d.displayName} with dni $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
 					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_thermostats>exception $e while trying to refresh existing tokens in child $d", detailedNotif, GLOBAL_LOG_ERROR)
            
			}            
		}

	}

	traceEvent(settings.logFilter,"create_child_thermostats>created $countNewChildDevices, total=${devices.size()} thermostats", detailedNotif)
	    

}


def create_child_switches() {

   	int countNewChildDevices =0

	traceEvent(settings.logFilter,"create_child_switches>About to loop thru switches $switches", detailedNotif)
	def allChildDevices=getChildDevices()
	def devices = ecobeeSwitches.collect { object->
		def dni=object?.key   		
		traceEvent(settings.logFilter,"create_child_switches>looping thru switches, found dni $dni", detailedNotif)
        
		def switch_info  = dni.tokenize('.')
		def switchId = switch_info.last()
 		def name = switch_info[1]
		def d= allChildDevices.find {it.deviceNetworkId.contains(switchId)}               
		traceEvent(settings.logFilter,"create_child_switches>looping thru switches, found device $d", detailedNotif)


		if(!d) {
			def labelName = 'My switch ' + "${name}"
			traceEvent(settings.logFilter,"create_child_switches>about to create child device with dni $dni, switchId = $switchId, name=  ${name}", detailedNotif)
			d = addChildDevice(getChildNamespace(), getChildSwitchName(), dni,null,[label: "${labelName}"]) 
			d.initialSetup( getSmartThingsClientId(), atomicState, switchId ) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_switches>created ${labelName} with dni $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_switches>found ${d.displayName} with dni $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_switches>exception $e while trying to refresh existing tokens in child $d", detailedNotif, GLOBAL_LOG_ERROR)
            
			}            
		} /* end if existing device */                

	}

	traceEvent(settings.logFilter,"create_child_switches>created $countNewChildDevices, total=${devices.size()} switches", detailedNotif)
	    

}



def initialize() {
    
	traceEvent(settings.logFilter,"initialize begin...", detailedNotif)
	atomicState?.exceptionCount=0    
	atomicState?.poll = [ last: 0, rescheduled: now() ]
	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)
	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", rescheduleHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", rescheduleHandler, [filterEvents: false])
	}
	if (tempSensor)	{
		subscribe(tempSensor,"temperature", rescheduleHandler,[filterEvents: false])
	}
	if (motionSensor)	{
		subscribe(motionSensor,"motion", rescheduleHandler,[filterEvents: false])
	}
	if (energyMeter)	{
		subscribe(energyMeter,"energy", rescheduleHandler,[filterEvents: false])
	}

	subscribe(app, appTouch)
	subscribe(location, "askAlexaMQ", askAlexaMQHandler)

	int delay = (givenInterval) ? givenInterval.toInteger() : 5 // By default, do it every 5 min.
	traceEvent(settings.logFilter,"initialize>polling delay= ${delay}...", detailedNotif)
    
	delete_child_devices()	
	create_child_thermostats()
	create_child_switches()
    
	atomicState?.alerts=[:]   
	rescheduleIfNeeded()   

}

def askAlexaMQHandler(evt) {
	if (!evt) return
	switch (evt.value) {
		case "refresh":
		state?.askAlexaMQ = evt.jsonData && evt.jsonData?.queues ? evt.jsonData.queues : []
		traceEvent(settings.logFilter,"askAlexaMQHandler>new refresh value=$evt.jsonData?.queues", detailedNotif, GLOBAL_LOG_INFO)
  		break
	}
}

def appTouch(evt) {
	rescheduleIfNeeded()
}

def rescheduleIfNeeded(evt) {
	if (evt) traceEvent(settings.logFilter,"rescheduleIfNeeded>$evt.name=$evt.value", detailedNotif)
	int delay = (givenInterval) ? givenInterval.toInteger() : 5 // By default, do it every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (atomicState?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		traceEvent(settings.logFilter,"rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago", detailedNotif)
		takeAction()
	}
	if (((atomicState?.poll["last"]?:0) + (delay * 60000) < currentTime)) {
		traceEvent(settings.logFilter,"rescheduleIfNeeded>scheduling takeAction in ${delay} minutes..", detailedNotif)
		if (delay <5) {      
			runEvery1Minute(takeAction)
		} else if ((delay >=5) && (delay <10)) {      
			runEvery5Minutes(takeAction)
		} else if ((delay >=10) && (delay <15)) {  
			runEvery10Minutes(takeAction)
		} else if ((delay >=15) && (delay <30)) {  
			runEvery15Minutes(takeAction)
		} else {  
			runEvery30Minutes(takeAction)
		}
 	}
    
    
	// Update rescheduled state
    
	if (!evt) atomicState.poll["rescheduled"] = now()
}



def takeAction() {
	traceEvent(settings.logFilter,"takeAction>begin", detailedNotif,GLOBAL_LOG_TRACE)
	int delay = (givenInterval) ? givenInterval.toInteger() : 5 // By default, do it every 5 min.
	atomicState?.poll["last"] = now()
		
	//schedule the rescheduleIfNeeded() function
    
	if (((atomicState?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		traceEvent(settings.logFilter,"takeAction>scheduling rescheduleIfNeeded() in ${delay} minutes..",true,GLOBAL_LOG_INFO)
		unschedule()        
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		atomicState?.poll["rescheduled"] = now()
	}


	poll_thermostats()
	poll_switches() 
    
	traceEvent(settings.logFilter,"takeAction>end", detailedNotif, GLOBAL_LOG_TRACE)

}

def terminateMe() {
	try {
		app.delete()
	} catch (Exception e) {
		traceEvent(settings.logFilter, "terminateMe>failure, exception $e", GLOBAL_LOG_ERROR, true)
	}
}


def purgeChildDevice(childDevice) {
	def dni = childDevice.device.deviceNetworkId
	def foundThermostat=thermostats.find {dni}    
	if (foundThermostat) {
		thermostats.remove(dni)
		app.updateSetting("thermostats", thermostats ? thermostats : [])
	} else {
		def foundSwitch=ecobeeSwitches.find {dni}    
		if (foundSwitch) {
			ecobeeSwitches.remove(dni)
			app.updateSetting("ecobeeSwitches", ecobeeSwitches ? ecobeeSwitches : [])
		}
	}	        
	if (getChildDevices().size <= 1) {
		traceEvent(settings.logFilter,"purgeChildDevice>no more devices to poll, unscheduling and terminating the app", GLOBAL_LOG_ERROR)
		unschedule()
		atomicState.authToken=null
		runIn(1, "terminateMe")
	}
}

private void poll_thermostats() {
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false
	def alertsInfo    
	def todayDay
	atomicState?.newDay=false        
    
	if (!location.timeZone) {    
		traceEvent(settings.logFilter,"takeAction>Your location is not set in your ST account, you'd need to set it as indicated in the prerequisites for alerting purposes..",true,
			GLOBAL_LOG_ERROR,true)
	} else {
    
		todayDay = new Date().format("dd",location.timeZone)
	}        
	if ((!atomicState?.today) || (todayDay != atomicState?.today)) {
		atomicState?.alerts=[:] // reinitialize the alerts & exceptionCount every day
		atomicState?.exceptionCount=0   
		atomicState?.sendExceptionCount=0        
		atomicState?.today=todayDay        
		atomicState?.newDay=true        
	}
	def devices = thermostats.collect { dni ->
		def d = getChildDevice(dni)
		traceEvent(settings.logFilter,"poll_thermostats>looping thru thermostats, found thermostat $d", detailedNotif)
		if (d) { 
			traceEvent(settings.logFilter,"poll_thermostats>Looping thru thermostats, found id $dni, about to poll ${d.displayName}",true, GLOBAL_LOG_INFO)
			d.poll()
			exceptionCheck = d.currentVerboseTrace.toString()
			if (handleException) {            
				if ((exceptionCheck) && ((exceptionCheck.contains("exception") || (exceptionCheck.contains("error")) && 
					(!exceptionCheck.contains("TimeoutException")) && (!exceptionCheck.contains("No signature of method: physicalgraph.device.CommandService.executeAction")) &&
					(!exceptionCheck.contains("UndeclaredThrowableException"))))) {  
				// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
					atomicState.exceptionCount=atomicState.exceptionCount+1    
					traceEvent(settings.logFilter,"poll_thermostats>found exception/error after polling, exceptionCount= ${atomicState?.exceptionCount}: $exceptionCheck", true, 
						GLOBAL_LOG_ERROR) 
				} else {             
					// reset exception counter            
					atomicState?.exceptionCount=0      
				}                
			}   /* end if handleException */
			if (atomicState?.newDay && askAlexaFlag) { // produce summary reports only at the beginning of a new day
				def PAST_DAY_SUMMARY=1 // day
				def PAST_WEEK_SUMMARY=7 // days
				if (settings.tstatDaySummaryFlag) {
					traceEvent(settings.logFilter,"poll_thermostats>about to call produceSummaryReport for device ${d.displayName} in the past day", detailedNotif, GLOBAL_LOG_TRACE) 
					d.produceSummaryReport(PAST_DAY_SUMMARY)
					String summary_report =d.currentValue("summaryReport")                        
					if (summary_report) {                        
						send (summary_report, askAlexaFlag)                        
					}                            
				}                    
				if (settings.tstatWeeklySummaryFlag) { // produce summary report only at the beginning of a new day
					traceEvent(settings.logFilter,"poll_thermostats>about to call produceSummaryReport for device ${d.displayName} in the past week", detailedNotif, GLOBAL_LOG_TRACE) 
					d.produceSummaryReport(PAST_WEEK_SUMMARY)
					String summary_report =d.currentValue("summaryReport")                        
					if (summary_report) {                        
						send (summary_report, askAlexaFlag)                        
					}                            
				}
			} /* end if askAlexa */                    
			// check for ecobee alerts        
			def alerts = d.currentValue("alerts")
			if (alerts) {
				alertsInfo= alerts.split(',')
			}
			def alertsSoFar=atomicState?.alerts        
			if ((alerts && alertsInfo) && (alerts.toUpperCase() != 'NONE')) {
				traceEvent(settings.logFilter,"poll_thermostats>found some ecobee alerts: ${alertsInfo} at ${d.displayName}", detailedNotif, GLOBAL_LOG_INFO)
				alertsInfo.each {         
					traceEvent(settings.logFilter,"new alert ${it}, Alerts stored so far for today: ${alertsSoFar}", detailedNotif)
					String query = "name==${d.displayName} && type==${it}"                
					if ((!alertsSoFar) || (!(alertsSoFar.findAll{query}))) {
						// If the atomicState variable has not reported this alert for the given thermostat yet             
						d.getAlertText(it) // get the alert text
						def alertText= d.currentValue("alertText")
						if (alertText) {
							traceEvent(settings.logFilter,"${alertText} at ${d.displayName}", true, GLOBAL_LOG_INFO, notifyAlerts)
						}     
						alertsSoFar = alertsSoFar + [name: d.displayName, type:it]
						atomicState?.alerts=alertsSoFar                    
						traceEvent(settings.logFilter,"poll_thermostats>atomicState.alerts for today (${atomicState?.today}) after alert processing of ${it}: ${atomicState?.alerts}", detailedNotif)
					} // if !alertsSoFar                       
				} // for each alert            
			} // if alerts            
			if (handleException) {    
				if ((exceptionCheck) && (exceptionCheck.contains("Unauthorized")) && (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT)) {
					// need to authenticate again    
					atomicState.authToken=null                    
					atomicState?.oauthTokenProvided=false
					traceEvent(settings.logFilter,"$exceptionCheck after ${atomicState?.exceptionCount} errors, press on 'ecobee' and re-login..." , true, 
						GLOBAL_LOG_ERROR,true)
				} else if (atomicState?.exceptionCount >= MAX_EXCEPTION_COUNT) {
					traceEvent(settings.logFilter,"poll_thermostats>too many exceptions/errors, $exceptionCheck (${atomicState?.exceptionCount} errors so far), you may need to press on 'ecobee' and re-login..." ,
						true, GLOBAL_LOG_ERROR, true)
				}
			} /* end if handleException */        

		} // if (d)         
	}
}

def updateChildSwitches(child) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"updateStructures>begin child id: ${child.device.deviceNetworkId}, updating it with ${atomicState?.structures}", detailedNotif)
*/
	def switches=getEcobeeSwitches()
	child.update_switches(atomicState?.switchList)

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"updateStructures>end child id: ${child.device.deviceNetworkId}, updating it with ${atomicState?.structures}", detailedNotif)
*/
}


private void update_switch(device, dni) {

	// look for the device in the switchList

	def switch_info  = dni.tokenize('.')
	def switchId = switch_info.last()
 	def name = switch_info[1]
	def found_switch_state = atomicState?.switchList.find {it.identifier == switchId}
	if (found_switch_state) {
		if (found_switch_state.connected != true) {
			traceEvent(settings.logFilter,"update_switch>updating switch $name, dni $dni but not connected", detailedNotif, GLOBAL_LOG_WARN)
		}        
		traceEvent(settings.logFilter,"update_switch>about to update switch $name, dni $dni", detailedNotif)
		device.refresh_switch(switchId,atomicState?.switchList )
		traceEvent(settings.logFilter,"update_switch>updated switch $name, dni $dni, to $found_switch_state", detailedNotif)
        
	}    
}

private void poll_switches() {
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false
	def switches= getEcobeeSwitches()

	traceEvent(settings.logFilter,"poll_switches>About to loop thru switches $ecobeeSwitches", detailedNotif)
	def devices = ecobeeSwitches.collect { object->

		def dni=object?.key   		
		traceEvent(settings.logFilter,"poll_switches>looping thru switches, found dni $dni", detailedNotif)
		def d = getChildDevice(dni)

		traceEvent(settings.logFilter,"poll_switches>looping thru switches, found switch $d", detailedNotif)
		if (d) { 
			traceEvent(settings.logFilter,"poll_switches>Looping thru switches, found dni $dni, about to update ${d.displayName}",detailedNotif, GLOBAL_LOG_INFO)
			update_switch(d, dni)
		} else {
			traceEvent(settings.logFilter,"poll_switches>Looping thru switches, found dni $dni but switch is not instantiated under ST, ",detailedNotif, GLOBAL_LOG_INFO)
        
		} /* end if device found */            
	}           
}

def isTokenExpired() {
	def buffer_time_expiration=5  // set a 5 min. buffer time before token expiration to avoid auth_err 
	def time_check_for_exp = now() + (buffer_time_expiration * 60 * 1000);
	traceEvent(settings.logFilter,"isTokenExpired>expiresIn timestamp: ${atomicState?.authexptime} > timestamp check for exp: ${time_check_for_exp}?", detailedNotif)
	if (atomicState?.authexptime > time_check_for_exp) {
		traceEvent(settings.logFilter,"isTokenExpired>not expired", detailedNotif)
//		send "isTokenExpired>not expired in MyEcobeeInit"
		return false
	}
	traceEvent(settings.logFilter,"isTokenExpired>expired", detailedNotif)
//	send "isTokenExpired>expired in MyEcobeeInit"
	return true    
}



def oauthInitUrl() {
	traceEvent(settings.logFilter,"oauthInitUrl>begin", detailedNotif)
	def stcid = getSmartThingsClientId();

	atomicState.oauthInitState = UUID.randomUUID().toString()
	atomicState?.clientId=stcid                    

	def oauthParams = [
		response_type: "code",
		scope: "smartRead,smartWrite,ems",
		client_id: stcid,
		state: atomicState.oauthInitState,
		redirect_uri: "${get_ST_URI_ROOT()}/oauth/callback"
	]

	redirect(location: "${get_URI_ROOT()}/authorize?${toQueryString(oauthParams)}")
}


def callback() {
	traceEvent(settings.logFilter,"callback>swapping token: $params", detailedNotif)

	def code = params.code
	def oauthState = params.state
	// Validate the response from the 3rd party by making sure oauthState == atomicState.oauthInitState as expected
	if (oauthState == atomicState.oauthInitState){

		def stcid = getSmartThingsClientId()

		def tokenParams = [
			grant_type: "authorization_code",
			code: params.code,
			client_id: stcid,
			redirect_uri: "${get_ST_URI_ROOT()}/oauth/callback"            
		]
		def tokenUrl = "${get_URI_ROOT()}/token?" + toQueryString(tokenParams)

		traceEvent(settings.logFilter,"callback>Swapping token $params", detailedNotif)

		def jsonMap
		httpPost(uri:tokenUrl) { resp ->
			jsonMap = resp.data
			atomicState.refreshToken = jsonMap.refresh_token
			atomicState.authToken = jsonMap.access_token
			atomicState.expiresIn=jsonMap.expires_in
			atomicState.tokenType = jsonMap.token_type
			def authexptime = new Date((now() + (jsonMap.expires_in * 1000))).getTime()
			atomicState.authexptime = authexptime
		}
		if (atomicState.authToken ) {
			// get jwt for switch+ devices
			atomicState.jwt = true
			refreshAuthToken()
		}     
		if (atomicState.authToken) {
			success()
		} else {
			fail()
		}
        
		success()

	} else {
		fail()    
		traceEvent(settings.logFilter,"callback() failed. Validation of state did not match. oauthState != state.oauthInitState", true, GLOBAL_LOG_ERROR)
	}

}

def success() {

	def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Withings Connection</title>
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
</head>
<body>
	<div class="container">
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/ecobee%402x.png" width="216" height="216" alt="ecobee icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
		<p>Your ecobee Account is now connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
	</div>
</body>
</html>
"""

	render contentType: 'text/html', data: html
}


def fail() {
	def message = """
		<p>There was an error connecting your ecobee account with SmartThings</p>
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

def getChildDeviceIdsString() {
	return thermostats.collect { it.split(/\./).last() }.join(',')
}

def toJson(Map m) {
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getTstatChildNamespace() { "fabricacross64399" }
def getChildNamespace() { "yracine" }
def getChildName() { "My Ecobee Device" }
def getChildSwitchName() { "My Ecobee Switch" }

def getServerUrl() { return getApiServerUrl()  }

def getSmartThingsClientId() { "qqwy6qo0c2lhTZGytelkQ5o8vlHgRsrO" }

private def get_API_VERSION() {
	return "1"
}

private def get_URI_ROOT() {
	return "https://api.ecobee.com"
}
private def get_ST_URI_ROOT() {
	return "https://graph.api.smartthings.com"
}


@Field int GLOBAL_LOG_ERROR=1
@Field int GLOBAL_LOG_WARN= 2
@Field int GLOBAL_LOG_INFO=3
@Field int GLOBAL_LOG_DEBUG=4
@Field int GLOBAL_LOG_TRACE=5


def traceEvent(filterLog, message, displayEvent=false, traceLevel=GLOBAL_LOG_DEBUG, sendMessage=false) {
	int filterLevel=(filterLog)?filterLog.toInteger():GLOBAL_LOG_WARN

	if (filterLevel >= traceLevel) {
		if (displayEvent) {    
			switch (traceLevel) {
				case GLOBAL_LOG_ERROR:
					log.error "${message}"
				break
				case GLOBAL_LOG_WARN:
					log.warn "${message}"
				break
				case GLOBAL_LOG_INFO:
					log.info "${message}"
				break
				case GLOBAL_LOG_TRACE:
					log.trace "${message}"
				break
				case GLOBAL_LOG_DEBUG:
				default:            
					log.debug "${message}"
				break
			}                
		}			                
		if (sendMessage) send (message,settings.askAlexaFlag) //send message only when true
	}        
}


private send(msg, askAlexa=false) {
	int MAX_EXCEPTION_MSG_SEND=5

	// will not send exception msg when the maximum number of send notifications has been reached
	if (msg.contains("exception")) {
		atomicState?.sendExceptionCount=atomicState?.sendExceptionCount+1         
		traceEvent(settings.logFilter,"checking sendExceptionCount=${atomicState?.sendExceptionCount} vs. max=${MAX_EXCEPTION_MSG_SEND}", detailedNotif)
		if (atomicState?.sendExceptionCount >= MAX_EXCEPTION_MSG_SEND) {
			traceEvent(settings.logFilter,"send>reached $MAX_EXCEPTION_MSG_SEND exceptions, exiting", detailedNotif)
			return        
		}        
	}    
	def message = "${get_APP_NAME()}>${msg}"


	if (sendPushMessage == "Yes") {
		traceEvent(settings.logFilter,"about to send notifications", false, GLOBAL_LOG_INFO)
		sendPush(message)
	}
	if (askAlexa) {
		def expiresInDays=(AskAlexaExpiresInDays)?:5    
		sendLocationEvent(
			name: "AskAlexaMsgQueue", 
			value: "${get_APP_NAME()}", 
			isStateChange: true, 
			descriptionText: msg, 
			data:[
				queues: listOfMQs,
				expires: (expiresInDays*24*60*60)  /* Expires after 5 days by default */
			]
		)
	} /* End if Ask Alexa notifications*/
	
	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, message)
	}
}


def getCustomImagePath() {
	return "https://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/"
}    


private def get_APP_NAME() {
	return "MyEcobeeInit"
}