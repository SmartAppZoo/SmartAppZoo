/**
 *	My Ecobee Init (Service Manager)
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
 *	Ecobee Service Manager
 *
 *	Author: scott
 *	Date: 2013-08-07
 *
 *  Last Modification:
 *      JLH - 01-23-2014 - Update for Correct SmartApp URL Format
 *      JLH - 02-15-2014 - Fuller use of ecobee API
 *		Y.Racine Nov 2014 - Simplified the Service Manager as much as possible to reduce tight coupling with 
 *							its child device types (device handlers) for better performance and reliability.
 *      linkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 **/
definition(
    name: "My Ecobee Init",
    namespace: "eco-community",
    author: "Sean Schneyer",
    description: "Connect your Ecobee thermostat and Sensors to SmartThings. Please ensure to turn on OAuth.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)
preferences {
	page(name: "about", title: "About", nextPage: "auth")
	page(name: "auth", title: "ecobee", content:"authPage", nextPage:"deviceList")
	page(name: "deviceList", title: "ecobee", content:"ecobeeDeviceList",nextPage: "otherSettings")
	page(name: "otherSettings", title: "Other Settings", content:"otherSettings", install:true)
}

mappings {
	path("/swapToken") {
		action: [
			GET: "swapToken"
		]
	}
}

def about() {
 	dynamicPage(name: "about", install: false, uninstall: true) {
 		section("About") {	
			paragraph "MyEcobeeInit, the smartapp that connects your Ecobee thermostat to SmartThings via cloud-to-cloud integration"
			paragraph "Version 2.3.2\n\n" 
			paragraph "Github Project"
				href url:"https://github.com/StrykerSKS/SmartThings/tree/master/devicetypes/eco-community/my-ecobee-device.src", style:"embedded", required:false, title:"More information...", 
					description: "https://github.com/StrykerSKS/SmartThings/tree/master/devicetypes/eco-community/my-ecobee-device.src"
		}
	}        
}

def otherSettings() {
	dynamicPage(name: "otherSettings", title: "Other Settings", install: true, uninstall: false) {
		section("Polling at which interval in minutes (range=[5..59],default=10 min.)?") {
			input "givenInterval", "number", title:"Interval", required: false
		}
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phoneNumber", "phone", title: "Send a text message?", required: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
	}
}

def authPage() {
	log.debug "authPage(),state.oauthTokenProvided=${state?.oauthTokenProvided}"

	if (!atomicState.accessToken) {
		log.debug "about to create access token"
		createAccessToken()
		atomicState.accessToken = state.accessToken
	}

	def description = "Required"
	def uninstallAllowed = false

	if (atomicState.authToken) {

		// TODO: Check if it's valid
		if (true) {
			description = "You are connected."
			uninstallAllowed = true
			state?.oauthTokenProvided=true            
		} else {
			description = "Required" // Worth differentiating here vs. not having atomicState.authToken? 
		}
	}

	def redirectUrl = oauthInitUrl()
	log.debug "authPage>atomicState.authToken=${atomicState.authToken},state.oauthTokenProvided=${state?.oauthTokenProvided}, RedirectUrl = ${redirectUrl}"


	// get rid of next button until the user is actually auth'd

	if (!state?.oauthTokenProvided) {

		return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed, submitOnChange: true) {
			section(){
				paragraph "Tap below to log in to the ecobee portal and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
				href url:redirectUrl, style:"embedded", required:true, title:"ecobee", description:description
			}
		}

	} else {

		return dynamicPage(name: "auth", title: "Log In", nextPage:"deviceList", uninstall:uninstallAllowed,submitOnChange: true) {
			section(){
				paragraph "Tap Next to continue to setup your thermostats."
				href url:redirectUrl, style:"embedded", state:"complete", title:"ecobee", description:description
			}
		}

	}


}

def ecobeeDeviceList() {
	log.debug "ecobeeDeviceList()"

	def stats = getEcobeeThermostats()

	log.debug "device list: $stats"

	def ems = getEcobeeThermostats("ems")

	log.debug "device list: $ems"

	stats = stats + ems
	def p = dynamicPage(name: "deviceList", title: "Select Your Thermostats", uninstall: true) {
		section(""){
			paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings (3 max)."
			input(name: "thermostats", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:stats])
		}
	}

	log.debug "list p: $p"
	return p
}



def getEcobeeThermostats(String type="") {
	log.debug "getting device list"
	def msg
    
	def requestBody
    
	if ((type == "") || (type == null)) {
    
	 	requestBody = '{"selection":{"selectionType":"registered","selectionMatch":""}}'
	} else {
		requestBody = '{"selection":{"selectionType":"managementSet","selectionMatch":"/"}}'
	}    
    
	def deviceListParams = [
		uri: "https://api.ecobee.com",
		path: "/1/thermostat",
		headers: ["Content-Type": "text/json", "Authorization": "Bearer ${atomicState.authToken}"],
		query: [format: 'json', body: requestBody]
	]

	log.debug "_______AUTH______ ${atomicState.authToken}"
	log.debug "device list params: $deviceListParams"

	def stats = [:]
	try {
		httpGet(deviceListParams) { resp ->

			if (resp.status == 200) {
				resp.data.thermostatList.each { stat ->
					def dni = [ app.id, stat.name, stat.identifier ].join('.')
					stats[dni] = getThermostatDisplayName(stat)
				}
			} else {
				log.debug "http status: ${resp.status}"

				//refresh the auth token
				if (resp.status == 500 && resp.data.status.code == 14) {
					log.debug "Storing the failed action to try later"
					data.action = "getEcobeeThermostats"
					log.error "getEcobeeThermostats>Need to refresh your auth_token!"
					state?.msg ="Need to refresh your authorization token!" 		                    
					log.error state.msg        
					runIn(30, "sendMsgWithDelay")
					atomicState.authToken= null                    
				} else {
					log.error "getEcobeeThermostats>Authentication error, invalid authentication method, lack of credentials, etc."
				}
			}
            
    	}        
	} catch (java.net.UnknownHostException e) {
		state?.msg ="Unknown host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
	} catch (java.net.NoRouteToHostException t) {
		state?.msg= "No route to host - check the URL " + deviceListParams.uri
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
	} catch (java.io.IOException e) {
		log.debug "getEcobeeThermostats>$e while getting list of thermostats, probable cause: not the right account for this type (${type}) of thermostat " +
			deviceListParams            
	} catch (e) {
		state?.msg= "exception $e while getting list of thermostats" 
		log.error state.msg        
		runIn(30, "sendMsgWithDelay")
    }

	log.debug "thermostats: $stats"

	return stats
}

def setParentAuthTokens(auth_data) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("setParentAuthTokens>begin auth_data: $auth_data")
*/

	atomicState.refreshToken = auth_data?.refresh_token
	atomicState.authToken = auth_data?.access_token
	atomicState.expiresIn=auth_data?.expires_in
	atomicState.tokenType = auth_data?.token_type
	atomicState.authexptime= auth_data?.authexptime
	refreshAllChildAuthTokens()
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("setParentAuthTokens>New atomicState: $atomicState")
*/
}

def refreshAllChildAuthTokens() {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("refreshAllChildAuthTokens>begin updating children with $atomicState")
*/

	def children= getChildDevices()
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("refreshAllChildAuthtokens> refreshing ${children.size()} thermostats")
*/

	children.each { 
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
		send("refreshAllChildAuthTokens>begin updating $it.deviceNetworkId with $atomicState")
*/
    	it.refreshChildTokens(atomicState) 
	}
}

def refreshThisChildAuthTokens(child) {
/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("refreshThisChildAuthTokens>begin child id: ${child.device.deviceNetworkId}, updating it with ${atomicState}")
*/
	child.refreshChildTokens(atomicState)

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	send("refreshThisChildAuthTokens>end")
*/
}


def getThermostatDisplayName(stat) {
	log.debug "getThermostatDisplayName"
	if(stat?.name) {
		return stat.name.toString()
	}

	return (getThermostatTypeName(stat) + " (${stat.identifier})").toString()
}

def getThermostatId(stat) {
	log.debug "getThermostatId"
	return (stat?.idenfifier)
}

def getThermostatTypeName(stat) {
	log.debug "getThermostatTypeName"
	return stat.modelNumber == "siSmart" ? "Smart Si" : (stat.modelNumber=="idtSmart") ? "Smart" : (stat.modelNumber=="athenaSmart") ? "Ecobee3" : "Ems"
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


private def delete_child_devices() {
	def delete
    
	// Delete any that are no longer in settings

	if(!thermostats) {
		log.debug "delete_child_devices>delete all ecobee thermostats"
		delete = getAllChildDevices()
	} else {
		delete = getChildDevices().findAll { !thermostats.contains(it.deviceNetworkId) }
	}


	delete.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			log.error "delete_child_devices>exception $e while deleting ecobee thermostat ${it.deviceNetworkId}"
			send "MyEcobeeInit>exception $e while deleting ecobee thermostat ${it.deviceNetworkId}"
		}   
	}
	log.debug "delete_child_devices>deleted ${delete.size()} ecobee thermostats"


}

private def create_child_devices() {

   	int i =0
	def devices = thermostats.collect { dni ->

		def d = getChildDevice(dni)
		log.debug "create_child_devices>looping thru thermostats, found id $dni"

		if(!d) {
			def tstat_info  = dni.tokenize('.')
			def thermostatId = tstat_info.last()
 			def name = tstat_info[1]
/*            
			i++		// Used to simulate many thermostats
			def labelName = 'My ecobee ' + "${name}_${i}"
*/                    
			def labelName = 'My ecobee ' + "${name}"
			log.debug "create_child_devices>about to create child device with id $dni, thermostatId = $thermostatId, name=  ${name}"
			d = addChildDevice(getChildNamespace(), getChildName(), dni, null,
				[label: "${labelName}"]) 
			d.initialSetup( getSmartThingsClientId(), atomicState, thermostatId ) 	// initial setup of the Child Device
			log.debug "create_child_devices>created ${d.displayName} with id $dni"
		} else {
			log.debug "create_child_devices>found ${d.displayName} with id $dni already exists"
		}

	}

	log.debug "create_child_devices>created ${devices.size()} thermostats"



}

def initialize() {
    
	log.debug "initialize"
	state?.exceptionCount=0    
	state?.msg=null
	state?.poll = [ last: 0, rescheduled: now() ]
    
	delete_child_devices()	
	create_child_devices()
    
	Integer delay = givenInterval ?: 10 // By default, do it every 10 min.
	if ((delay < 5) || (delay>59)) {
		state?.msg= "MyEcobeeInit>scheduling interval not in range (${delay} min), exiting..."
		log.debug state.msg
		runIn(30, "sendMsgWithDelay")
 		return
	}
	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)

	log.trace "initialize>polling delay= ${delay}..."
	rescheduleIfNeeded()   
}


def rescheduleIfNeeded(evt) {
	if (evt) log.debug("rescheduleIfNeeded>$evt.name=$evt.value")
	Integer delay = givenInterval ?: 10 // By default, do poll every 10 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (state?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		log.info "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago"
	}
	if (((state?.poll["last"]?:0) + (delay * 60000) < currentTime) && canSchedule()) {
		log.info "rescheduleIfNeeded>scheduling takeAction in ${delay} minutes.."
		schedule("0 0/${delay} * * * ?", takeAction)
	}
    
	takeAction()
    
	// Update rescheduled state
    
	if (!evt) state.poll["rescheduled"] = now()
}

def takeAction() {
	log.trace "takeAction>begin"
	def msg, exceptionCheck    
	def MAX_EXCEPTION_COUNT=5

	Integer delay = givenInterval ?: 10 // By default, the poll is done every 10 min.
	state?.poll["last"] = now()
		
	//schedule the rescheduleIfNeeded() function
    
	if (((state?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		log.info "takeAction>scheduling rescheduleIfNeeded() in ${delay} minutes.."
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		state?.poll["rescheduled"] = now()
	}

	def devices = thermostats.collect { dni ->
		def d = getChildDevice(dni)
		log.debug "takeAction>Looping thru thermostats, found id $dni, about to poll"
		try {        
			d.poll()
			exceptionCheck = d.currentVerboseTrace.toString()
			if ((exceptionCheck.contains("exception") || (exceptionCheck.contains("error")) && 
				(!exceptionCheck.contains("Java.util.concurrent.TimeoutException")))) {  
			// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
				state.exceptionCount=state.exceptionCount+1    
				log.error "found exception/error after polling, exceptionCount= ${state?.exceptionCount}: $exceptionCheck" 
			} else {             
				// reset exception counter            
				state?.exceptionCount=0       
			}                
		} catch (e) {
			state.exceptionCount=state.exceptionCount+1    
			log.error "MyEcobeeInit>exception $e while trying to poll the device $d, exceptionCount= ${state?.exceptionCount}" 
		}
	}
	if ((state?.exceptionCount>=MAX_EXCEPTION_COUNT) || (exceptionCheck.contains("Unauthorized"))) {
		// need to authenticate again    
		atomicState.authToken=null                    
		state?.oauthTokenProvided=false
		msg="too many exceptions/errors or unauthorized exception, $exceptionCheck (${state?.exceptionCount} errors), press on 'ecobee' and re-login..." 
		send "MyEcobeeInit> ${msg}"
		log.error msg
	}    
	log.trace "takeAction>end"

}

private void sendMsgWithDelay() {

	if (state?.msg) {
		send "MyEcobeeInit> ${state.msg}"
	}
}

private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)

	}

	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, msg)
	}

	log.debug msg
}


def oauthInitUrl() {
	log.debug "oauthInitUrl"
	def stcid = getSmartThingsClientId();

	atomicState.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [
		response_type: "code",
		scope: "ems,smartWrite",
		client_id: stcid,
		state: atomicState.oauthInitState,
		redirect_uri: buildRedirectUrl()
	]

	return "https://api.ecobee.com/authorize?" + toQueryString(oauthParams)
}

def buildRedirectUrl() {
	log.debug "buildRedirectUrl: ${serverUrl}/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/swapToken"
	return serverUrl + "/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/swapToken"
}

def swapToken() {
	log.debug "swapping token: $params"
	debugEvent ("swapping token: $params", true)

	def code = params.code
	def oauthState = params.state

	def stcid = getSmartThingsClientId()

	def tokenParams = [
		grant_type: "authorization_code",
		code: params.code,
		client_id: stcid,
		redirect_uri: buildRedirectUrl()
	]

	def tokenUrl = "https://api.ecobee.com/token?" + toQueryString(tokenParams)

	log.debug "Swapping token $params"

	def jsonMap
	httpPost(uri:tokenUrl) { resp ->
		jsonMap = resp.data
	}

	log.debug "Swapped token for $jsonMap"
	debugEvent ("swapped token for $jsonMap", true)

	atomicState.refreshToken = jsonMap.refresh_token
	atomicState.authToken = jsonMap.access_token
	atomicState.expiresIn=jsonMap.expires_in
	atomicState.tokenType = jsonMap.token_type
	def authexptime = new Date((now() + (atomicState.expiresIn * 60 * 1000))).getTime()
	atomicState.authexptime = authexptime


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

def getChildDeviceIdsString() {
	return thermostats.collect { it.split(/\./).last() }.join(',')
}

def toJson(Map m) {
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getChildNamespace() { "eco-community" }
def getChildName() { "My Ecobee Device" }

//def getServerUrl() { return "https://graph.api.smartthings.com" }

def getServerUrl() { return getApiServerUrl()  }

def getSmartThingsClientId() { 
	if(!appSettings.clientId) {
    		return "obvlTjUuuR2zKpHR6nZMxHWugoi5eVtS"
    	} else {
		return appSettings.clientId 
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