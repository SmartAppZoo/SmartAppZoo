/**
 *  MyNextManager V2 (Service Manager)
 *  Copyright 2018-2020 Yves Racine
 *  LinkedIn profile: www.linkedin.com/in/yracine
 *  Refer to readme file for installation instructions.
 *     http://github.com/yracine/device-type.myNext/blob/master/README.md
 * 
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights,
 *  trade secret in the Background technology. May be subject to consulting fees under an Agreement 
 *  between the Developer and the Customer. Developer grants a non exclusive perpetual license to use
 *  the Background technology in the Software developed for and delivered to Customer under this
 *  Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * 
 * Software Distribution is restricted and shall be done only with Developer's written approval.
 *
**/

import java.text.SimpleDateFormat
import groovy.transform.Field

//******************    INSERT YOUR NEST ACCOUNT INFORMATION BELOW between the single quotes *******************************************************************************************************************************************
@Field String nest_user_id= ''
@Field String nest_access_token=''


//******************    INSERT YOUR GOOGLE ACCOUNT INFORMATION BELOW between the single quotes ******************************************************************************
// YOU CAN COPY THE WHOLE COOKIE in google_cookiep1. YOU DON'T NEED TO SPLIT IT ANYMORE. The code contains the other google cookie p2-p6 for backward compatibility
@Field String google_cookiep1=''
@Field String google_issue_token_url=''
@Field String google_cookiep2=''
@Field String google_cookiep3=''
@Field String google_cookiep4=''
@Field String google_cookiep5=''
@Field String google_cookiep6=''



private def get_AppSettingsValue(variable) {
    return ("${variable}" ? "${variable}".toString(): "")
}

definition(
    name: "${get_APP_NAME()}",
    namespace: "yracine",
    author: "Yves Racine",
    description: "Connect your Google/Nest Products to SmartThings.",
    category: "My Apps",
    iconUrl: "${getCustomImagePath()}WorksWithNest.jpg",
    iconX2Url: "${getCustomImagePath()}WorksWithNest.jpg",
    singleInstance: true    
)

// ** DO NOT USE THE AppSetting variables ANYMORE, LEFT FOR RETRIEVAL 
{
	appSetting "nest_user_id"
	appSetting "nest_access_token"
 
	appSetting "google_cookiep1_OCAK"
	appSetting "google_cookiep2"
	appSetting "google_cookiep3"
	appSetting "google_cookiep4"
	appSetting "google_cookiep5"
	appSetting "google_cookiep6"
	appSetting "google_issue_token_url"
 
}

private def get_APP_VERSION() {
	return "3.2.3"
}    
preferences {

	page(name: "about", title: "About", nextPage:"auth")
	page(name: "auth", title: "Next", content:"authPage", nextPage:"structureList")
	page(name: "structureList",title: "Nest Structures", content:"structureList",nextPage: "NextTstatList")   
	page(name: "NextTstatList", title: "Nest Thermostats devices", content:"NextTstatList",nextPage: "NextSensorList")
	page(name: "NextSensorList", title: "Nest Sensor devices", content:"NextSensorList",nextPage: "NextProtectList")
	page(name: "NextProtectList", title: "Nest Protect devices", content:"NextProtectList",nextPage: "otherSettings")
//	page(name: "NextProtectList", title: "Nest Protect devices", content:"NextProtectList",nextPage: "NextCamList")
	page(name: "NextCamList", title: "Nest Camera devices", content:"NextCamList",nextPage: "otherSettings")
	page(name: "otherSettings", title: "Other Settings", content:"otherSettings", install:true)
	page(name: "watchdogSettingsSetup")    
	page(name: "reportSettingsSetup")    
	page(name: "cacheSettingsSetup")    
}

mappings {
    path("/oauth/initialize") {action: [GET: "login"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

def about() {


 	dynamicPage(name: "about", install: false, uninstall: true) {
 		section("") {
			paragraph image:"${getCustomImagePath()}ecohouse.jpg", "${get_APP_NAME()}, the smartapp that connects your Nest devices to SmartThings via cloud-to-cloud integration"
			paragraph "Version ${get_APP_VERSION()}" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url:"https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright©2018-2020 Yves Racine"
				href url:"http://github.com/yracine/device-type-myNext", style:"embedded", required:false, title:"More information...", 
					description: "http://github.com/yracine"
		}
		section("Cache Settings") {
			href(name: "toCacheSettingsSetup", page: "cacheSettingsSetup",required:false,  description: "Optional",
				title: "Cache settings for structures & devices in Service Manager", image: "${getCustomImagePath()}cacheTimeout.jpg" ) 
		}        
    
	}
    
}

def otherSettings() {

	traceEvent(settings.logFilter,"NextCamList>list p: $p",detailedNotif)


	dynamicPage(name: "otherSettings", title: "Other Settings", install: true, uninstall: false) {
		section("Polling interval in minutes, range=[5,10,15,30],default=10 min.\n\nWarning: for shorter polling intervals (ex. 1 or 5 minutes) with Google Accounts, you may encounter some Google token issues when sending commands" + 
			" as Google has some stricter rules if you have many Nest devices.") {
			input "givenInterval", "enum", title:"Interval?", required: false,metadata: [values: [1,5,10,15,30]]
		} 
		section("Handle/Notify any exception proactively [default=false, you will not receive any exception notification]") {
			input "handleExceptionFlag", "bool", title: "Handle exceptions proactively?", required: false
		}
		section("Scheduler's watchdog Settings (needed if any ST scheduling issues)") {
			href(name: "toWatchdogSettingsSetup", page: "watchdogSettingsSetup",required:false,  description: "Optional",
				title: "Scheduler's watchdog Settings", image: "${getCustomImagePath()}safeguards.jpg" ) 
		}
		
		section("Notifications & Logging") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:false
			input "phoneNumber", "phone", title: "Send a text message?", required: false
			input "detailedNotif", "bool", title: "Detailed Logging & Notifications?", required:false
			input "logFilter", "enum", title: "log filtering [Level 1=ERROR only,2=<Level 1+WARNING>,3=<2+INFO>,4=<3+DEBUG>,5=<4+TRACE>]?",required:false, metadata: [values: [1,2,3,4,5]]
				          
		}
		section("Enable Amazon Echo/Ask Alexa Notifications for events logging (optional)") {
			input (name:"askAlexaFlag", title: "Ask Alexa verbal Notifications [default=false]?", type:"bool",
				description:"optional",required:false)
			input (name:"listOfMQs",  type:"enum", title: "List of the Ask Alexa Message Queues (default=Primary)", options: state?.askAlexaMQ, multiple: true, required: false,
				description:"optional")            
			input ("AskAlexaExpiresInDays", "number", title: "Ask Alexa's messages expiration in days (optional,default=2 days)?", required: false)
		}
		section("Summary Report Settings") {
			href(name: "toReportSettingsSetup", page: "reportSettingsSetup",required:false,  description: "Optional",
				title: "Summary Reports via notifications/Ask Alexa", image: "${getCustomImagePath()}reports.jpg" ) 
		}
		section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
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


def reportSettingsSetup() {
	dynamicPage(name: "reportSettingsSetup", title: "Summary Report Settings ", uninstall: false) {
		section("Report options: Daily/Weekly Summary reports are sent by notifications (right after midnight, early morning) and/or can be verbally given by Ask Alexa") {
			input (name:"tstatDaySummaryFlag", title: "include Past Day Summary Report for your Nest Tstat(s) [default=false]?", type:"bool",required:false)
			input (name:"camDaySummaryFlag", title: "include Past Day Summary Report for your Nest Cam(s) [default=false]?", type:"bool",required:false)
			input (name:"protectDaySummaryFlag", title: "include Past Day Summary Report for your Nest Protect(s) [default=false]?", type:"bool",required:false)
			input (name:"tstatWeeklySummaryFlag", title: "include Weekly Summary Report for your Nest Tstat(s) [default=false]?", type:"bool",required:false)
			input (name:"camWeeklySummaryFlag", title: "include Weekly Summary Report for your Nest Cam(s) [default=false]?", type:"bool", 	required:false)
			input (name:"protectWeeklySummaryFlag", title: "include Weekly Summary Report for your Nest Protect(s) [default=false]?", type:"bool", required:false)
		}
		section {
			href(name: "toOtherSettingsPage", title: "Back to Other Settings Page", page: "otherSettings")
		}
	}
}   

def cacheSettingsSetup() {
	dynamicPage(name: "cacheSettingsSetup", title: "Cache Settings ", uninstall: false) {
 		section("To refresh your current structures, don't use the cache [default=cache is not used, use cache for better performances") {	
			input(name: "use_cache", title:"use of cached structures including devices?", type: "bool", required:false, defaultValue: true)
			input(name: "cache_timeout", title:"Cache timeout in minutes (default=2 min)?", type: "number", required:false, description: "optional")
		}        
		section {
			href(name: "toOtherSettingsPage", title: "Back to About Page", page: "about")
		}
	}
}   



def authPage() {
	settings.detailedNotif=true
	settings.logFilter=5    
  
//	def settingKey= "detailedNotif"   
//	app.updateSetting(settingKey,true)        
//	settingKey= "logFilter"   
//	app.updateSetting(settingKey,5)  

	traceEvent(settings.logFilter,"authPage(),atomicState.oauthTokenProvided=${atomicState?.oauthTokenProvided}", detailedNotif)

	atomicState?.accessToken=get_AppSettingsValue(nest_access_token)
	if (!atomicState.accessToken) {
		traceEvent(settings.logFilter,"about to create access token", detailedNotif)
		try {        
			createAccessToken()
		} catch (e) {
			traceEvent(settings.logFilter,"authPage() exception $e, not able to create access token, probable cause: oAuth is not enabled for MyNextManager smartapp ", true, GLOBAL_LOG_ERROR, true)
			return           
		}        
		atomicState.accessToken = state.accessToken
		atomicState?.oauthTokenProvided=false 
								 
				  
			   
	} else {
		atomicState?.oauthTokenProvided=true       
		atomicState?.access_token=  atomicState?.accessToken 
		atomicState?.nest_auth_token=  atomicState?.accessToken 
		atomicState?.nest_user_id=get_AppSettingsValue(nest_user_id)        
		atomicState?.google_issue_token_url=get_AppSettingsValue(google_issue_token_url) 
		if ((!nest_user_id) && (!google_issue_token_url)) {
			traceEvent(settings.logFilter,"authPage>error: the smartapps's nest_user_id or google_issue_token_url global variables are not populated, please read the README at the github", true, GLOBAL_LOG_ERROR, true)
        
		}        
	}    
	if (google_issue_token_url && (isTokenExpired())) {        
		loginFromUI()		
	}            

	def description = "Google/Nest Connection Required> press here for login prompt."
	def uninstallAllowed = false


	if (atomicState?.oauthTokenProvided) {
		description = "Text in blue: you are already connected to Google/Nest. You just need to tap the upper right 'Next' button.\n\nIf text in red, please re-login at Nest by pressing here as there was a connection error."
		uninstallAllowed = true
	} else {
		description = "Google Connection Required, press here for login prompt." // Worth differentiating here vs. not having atomicState.authToken? 
		uninstallAllowed = true
	}
    
	def redirectUrl = "${get_ST_URI_ROOT()}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${getServerUrl()}"

	traceEvent(settings.logFilter,"authPage(),redirectUrl=$redirectUrl", detailedNotif)
  
    
	traceEvent(settings.logFilter,"authPage>atomicState.authToken=${atomicState.accessToken},atomicState.oauthTokenProvided=${atomicState?.oauthTokenProvided}, RedirectUrl = ${redirectUrl}",
		detailedNotif)

	// get rid of next button until the user is actually auth'd


	if (!atomicState?.access_token) {

		return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed, submitOnChange: true) {
			section(){
				paragraph "Be sure to have populated the Nest info (user_id & access_token) or the Google info (cookie & issue_url) in the header (code)  as indicated in the README file at my github"
				input(name: "var1", title:"", type: "text", required:false,  description: "Tap remove to exit")
			}
		}            
	} else {

		return dynamicPage(name: "auth", title: "Log In", nextPage:"structureList", uninstall:uninstallAllowed,submitOnChange: true) {
			section(){
				paragraph "To be connected with Google/Nest, you need to have copied your login info from Nest/Google and save it in the smartapp's header (code) as global variables at this point. If so, then just tap the upper right Next to continue the setup of your Nest devices."
				input(name: "var2", title:"", type: "text", required:false,  description: "Tap Next to continue")
			}
		}

	}

}

def structureList() {
//	settings.logFilter=5
//	settings.detailedNotif=true    
	def structures
    
	traceEvent(settings.logFilter,"structureList>begin", detailedNotif)
	def use_cache=settings.use_cache
	def last_execution_interval=(settings.cache_timeout)?:2    // set a min. execution interval to avoid unecessary queries to Nest
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}  
	delete_obsolete_devices()	
//	structures =getStructures(false, settings.cache_timeout,GLOBAL_BUCKETS_CLAUSE)

	traceEvent(settings.logFilter,"structureList>about to call, type=[where]", detailedNotif,GLOBAL_LOG_TRACE)
	structures=getStructures(use_cache, settings.cache_timeout,GLOBAL_WHERE_CLAUSE)
	traceEvent(settings.logFilter,"structureList>where structure=$structures", detailedNotif,GLOBAL_LOG_TRACE)
	def known_buckets= GLOBAL_BUCKETS_TYPES_CLAUSE  
	traceEvent(settings.logFilter,"structureList>about to call, type=$known_buckets", detailedNotif,GLOBAL_LOG_TRACE)
	structures=getStructures(use_cache, settings.cache_timeout,"${known_buckets}",true)
    
/*    
	if (atomicState?.access_token) {
    
		traceEvent(settings.logFilter,"otherSettings>about to call login_dropcam with ${atomicState?.access_token}", detailedNotif,GLOBAL_LOG_TRACE)
    	if (login_dropcam(atomicState?.access_token, atomicState?.cookie)) {
				getAllCameras(false)
		} else {
			traceEvent(settings.logFilter,"otherSettings>login_dropcam failed,atomicState?.lastHttpStatus=${atomicState?.lastHttpStatus}", detailedNotif,GLOBAL_LOG_TRACE, true)
        
		}        
	}    
*/	
	atomicState?.lastExecutionTimestamp=now()
/*
	if ((!structures) && (use_cache)) structures =atomicState?."structureDNIs" // restore the last saved DNIs    
	int structureCount=structures.size()    
*/    
	def p =dynamicPage(name: "structureList", title: "Nest devices integration to SmartThings", uninstall: true) {
		section(""){
			paragraph "Got all structures from Google/Nest, check Live Logging (IDE) for errors (if any, the login info has not been copied correctly) & Tap next to select your Nest devices to expose to SmartThings."
			input(name: "structure", title:"", type: "text",  required:false, description: "Tap Next to continue")
		}
	}
    
	return p   
    
}


def NextTstatList() {
	traceEvent(settings.logFilter,"NextTstatList>begin", detailedNotif)
	def structures    

	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:2   // set an execution interval  to avoid unecessary queries to Nest
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}  
    
	traceEvent(settings.logFilter,"NextTstatList> about to call getObject()", detailedNotif)
	def tstatObjects= getObject("", "thermostat", use_cache, last_execution_interval)    
	def tstatList=atomicState?.thermostatList    
	if (!tstatList) tstatList=[]    
	def tstatDNIs= [:] 
	def object =[:]   
	if (tstatObjects) {     
		tstatObjects.each {    
			traceEvent(settings.logFilter,"NextTstatList>> about to loop returned thermostats from getObject(): $tstatObjects", detailedNotif,GLOBAL_LOG_TRACE)    
			def name = (it?.name) ?: it?.long_name                    
			def dni = [ app.id, name, it?.id].join('.')
			tstatDNIs[dni] = name
		}  
		atomicState?.thermostatData=tstatObjects        
	} else if (tstatList) {
		traceEvent(settings.logFilter,"NextTstatList>> about to get protect list from previously stored thermostats", detailedNotif,GLOBAL_LOG_TRACE)    
		thermostats.each {
			def tstatInfo=it.tokenize('.')
			def name=tstatInfo[1]            
			object[it]=name
			tstatDNIs << object           
		}        
	}
	int tstatCount=tstatDNIs.size()
	traceEvent(settings.logFilter,"NextTstatList>device list: $tstatDNIs, count=$tstatCount", detailedNotif)

	def p = dynamicPage(name: "NextTstatList", title: "Select Your Nest Thermostat(s) to be exposed to SmartThings ($tstatCount found).", uninstall: true) {
		section(""){
        
			paragraph image: "${getCustomImagePath()}NestTstat.png", "Tap below to see the list of Nest Tstats available in your Nest's primary (main) account"
			input(name: "thermostats", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", metadata:[values:tstatDNIs])
		}
	}

	traceEvent(settings.logFilter,"NextTstatList>list p: $p",detailedNotif)
	return p
}
def NextSensorList() {
	traceEvent(settings.logFilter,"NextSensorList>begin", detailedNotif)

	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:2   // set an execution interval  to avoid unecessary queries to Nest
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}  
    
	traceEvent(settings.logFilter,"NextSensorList> about to call getObject()", detailedNotif, GLOBAL_LOG_TRACE)
	def sensorObjects= getObject("", "sensor", use_cache, last_execution_interval)    
	def sensorList=atomicState?.sensorList    
	if (!sensorList) sensorList=[]    
	def sensorDNIs= [:] 
	def object =[:]   
	if (sensorObjects) {     
		sensorObjects.each {    
			traceEvent(settings.logFilter,"NextSensorList>> about to loop returned thermostats from getObject(): $tstatObjects", detailedNotif,GLOBAL_LOG_TRACE)    
			def name = (it?.name) ?: it?.long_name                    
			def dni = [ app.id, name, it?.id].join('.')
			sensorDNIs[dni] = name
		}  
		atomicState?.sensorData=sensorObjects        
	} else if (sensorList) {
		traceEvent(settings.logFilter,"NextSensorList> about to get sensor list from previously stored sensors", detailedNotif,GLOBAL_LOG_TRACE)    
		sensors.each {
			def sensorInfo=it.tokenize('.')
			def name=sensorInfo[1]            
			object[it]=name
			sensorDNIs << object           
		}        
	}
	int sensorCount=sensorDNIs.size()
	traceEvent(settings.logFilter,"NextSensorList>device list: $sensorDNIs, count=$sensorCount", detailedNotif)

	def p = dynamicPage(name: "NextSensorList", title: "Select Your Nest sensor(s) to be exposed to SmartThings ($sensorCount found).", uninstall: true) {
		section(""){
        
			paragraph image: "${getCustomImagePath()}NestSensor.png", "Tap below to see the list of Nest sensors available in your Nest's primary (main) account"
			input(name: "sensors", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", metadata:[values:sensorDNIs])
		}
	}

	traceEvent(settings.logFilter,"NextSensorList>list p: $p",detailedNotif)
	return p
}


def NextProtectList() {
	traceEvent(settings.logFilter,"NextProtectList>begin", detailedNotif)

	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:2   // set an execution interval to avoid unecessary queries to Nest
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}  
	def protectObjects= getObject("", "protect", use_cache, last_execution_interval)    
	def protectList=atomicState?.protectList    
	if (!protectList) protectList=[]    
	def protectDNIs= [:] 
	def object =[:]   
	if (protectObjects) {    
		traceEvent(settings.logFilter,"NextProtectList>> about to loop returned protects from getObject(): $protectObjects", detailedNotif,GLOBAL_LOG_TRACE)    
		protectObjects.each {    
			def name = (it?.name) ?: it?.long_name                    
			def dni = [ app.id, name, it?.id].join('.')
			protectDNIs[dni] = name
		}            
		atomicState?.protectData=protectObjects        
	} else if (protectList) {
		traceEvent(settings.logFilter,"NextProtectList>> about to get protect list from previously stored protects", detailedNotif,GLOBAL_LOG_TRACE)    
		protectUnits.each {
			def protectInfo=it.tokenize('.')
			def name=protectInfo[1]            
			object[it]=name
			protectDNIs << object           
		}        
	}
	int protectCount=protectDNIs.size()
          

	traceEvent(settings.logFilter,"NextProtectList>device list: $protectDNIs, count=$protectCount", detailedNotif)

	def p = dynamicPage(name: "NextProtectList", title: "Select Your Nest Protect unit(s) to be exposed to SmartThings ($protectCount found).", uninstall: true) {
		section(""){
			paragraph image: "${getCustomImagePath()}NestProtect.png","Tap below to see the list of Nest Protects available in your Nest's primary (main) account. "
			input(name: "protectUnits", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", options: protectDNIs)
		}
	}

	traceEvent(settings.logFilter,"NextProtectList>list p: $p",detailedNotif)
	return p
}


def NextCamList() {
	traceEvent(settings.logFilter,"NextCamList>begin", detailedNotif)
	def use_cache=settings.use_cache
	def last_execution_interval= (settings.cache_timeout)?:2   // set an execution interval to avoid unecessary queries to Nest
	def time_check_for_execution = (now() - (last_execution_interval * 60 * 1000))
	if ((use_cache) && (atomicState?.lastExecutionTimestamp) && (atomicState?.lastExecutionTimestamp > time_check_for_execution)) {
		use_cache=false	    
	}  
	def cameraObjects= getObject("", "camera", use_cache, last_execution_interval)    
	def cameraList=atomicState?.cameraList    
	if (!cameraList) cameraList=[]    
	def cameraDNIs= [:]    
	def object =[:]   
	if (cameraObjects) {    
		traceEvent(settings.logFilter,"NextCamList>> about to loop returned cams from getObject(): $cameraObjects", detailedNotif,GLOBAL_LOG_TRACE)    
		cameraObjects.each {    
			def name = (it?.name) ?: it?.long_name                    
			def dni = [ app.id, name, it?.id].join('.')
			cameraDNIs[dni] = name
		}            
		atomicState?.cameraData=cameraObjects        
	} else if (cameraList) {
		traceEvent(settings.logFilter,"NextCamList>> about to get camera list from previously stored cameras", detailedNotif,GLOBAL_LOG_TRACE)    
		cameras.each {
			def cameraInfo=it.tokenize('.')
			def name=cameraInfo[1]            
			object[it]=name
			cameraDNIs << object            
		}        
	}
	int cameraCount=cameraDNIs.size()
    
	traceEvent(settings.logFilter,"NextCamList>device list: $cameraDNIs, count=$cameraCount", detailedNotif)

	def p = dynamicPage(name: "NextCamList", title: "Select Your Nest Cams -if any -to be exposed to SmartThings ($cameraCount found).", uninstall: true) {
		section(""){
			paragraph image: "${getCustomImagePath()}NestCam2.png","Tap below to see the list of Nest Cam(s) available in your Nest's primary (main) account." +
				"Please refer to the prerequisites in order to make the ST integration work."
			input(name: "cameras", title:"", type: "enum", required:false, multiple:true, description: "Tap to choose", options: cameraDNIs)
		}
	}
	return p
}


void save_auth_data(auth_data) {
//	atomicState?.refresh_token = auth_data?.refresh_token
	atomicState?.access_token = auth_data?.access_token
	atomicState?.expires_in=auth_data?.expires_in
//	atomicState?.token_type = auth_data?.token_type
	atomicState?.authexptime= auth_data?.authexptime
	traceEvent(settings.logFilter,"save_auth_data>atomicState auth=$atomicState",detailedNotif)
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
		def auth_data=[:]
		auth_data?.access_token=atomicState?.access_token            
		auth_data?.expires_in=atomicState?.expires_in            
		auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
		auth_data?.nest_user_id=atomicState?.nest_user_id            
		auth_data?.nest_auth_token=atomicState?.nest_auth_token            
		auth_data?.google_jwt=atomicState?.google_jwt            
		auth_data?.authexptime=atomicState?.authexptime            
		it.save_data_auth(auth_data) 
	}
}
def refreshThisChildAuthTokens(child) {

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"refreshThisChildAuthTokens>begin child id: ${child.device.deviceNetworkId}, updating it with ${atomicState}", detailedNotif)
*/
	def auth_data=[:]
	auth_data?.access_token=atomicState?.access_token            
	auth_data?.expires_in=atomicState?.expires_in            
	auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
	auth_data?.nest_user_id=atomicState?.nest_user_id            
	auth_data?.nest_auth_token=atomicState?.nest_auth_token            
	auth_data?.google_jwt=atomicState?.google_jwt            
	auth_data?.authexptime=atomicState?.authexptime            
	child.save_data_auth(auth_data)

/*
	For Debugging purposes, due to the fact that logging is not working when called (separate thread)
	traceEvent(settings.logFilter,"refreshThisChildAuthTokens>end",detailedNotif)
*/
}



boolean refreshParentTokens() {

	if ((google_issue_token_url) && (isTokenExpired())) {
		if (login()) {
			refreshAllChildAuthTokens()
			return true            
		}		        
	} else {
		refreshAllChildAuthTokens()
		return true            
	}    
	return false    
    
}


private boolean login_dropcam(access_token, cookie="") {
//	settings.detailedNotif=true // set to true initially
//	settings.logFilter=5   
	def REFERER= get_REFERER()
	def USER_AGENT=get_USER_AGENT()   
	boolean send_msg=false
//	send_msg=true    
    
	def msg
 	def TOKEN_EXPIRED=401
	def NEST_SUCCESS=200    
 	String args="access_token=" + access_token 
//	def args_encoded = java.net.URLEncoder.encode(args.toString(), "UTF-8")

	traceEvent(settings.logFilter,"login_dropcam>data=$args", detailedNotif,GLOBAL_LOG_TRACE, send_msg)
	def loginParams = [
		uri: "${get_NEST_URI_ROOT()}/dropcam/api/login",
		headers: [
			'Sec-Fetch-Mode': 'cors',
			'Content-Type':"application/x-www-form-urlencoded",			
			'Referer': REFERER,
			'X-Requested-With': "XmlHttpRequest",
			'User-Agent': USER_AGENT,
			'charset': "UTF-8",
			'cookie': cookie            
		],            
		body: args.toString()
	]
    
	traceEvent(settings.logFilter,"login_dropcam>About to post $loginParams", detailedNotif,GLOBAL_LOG_TRACE, send_msg)
	try {
		httpPost(loginParams) { resp ->
			atomicState?.lastHttpStatus=resp?.status
			traceEvent(settings.logFilter,"login_dropcam>resp.status=${resp?.status}", detailedNotif, GLOBAL_LOG_TRACE, send_msg)
			traceEvent(settings.logFilter,"login_dropcam>resp.status=${resp?.status}", detailedNotif, GLOBAL_LOG_TRACE, send_msg)
			if (resp?.status == NEST_SUCCESS) {
				traceEvent(settings.logFilter,"login_dropcam>success, resp.status=${resp?.status}", detailedNotif)
				traceEvent(settings.logFilter, "login_dropcam>success, resp.data=${resp?.data}",detailedNotif,GLOBAL_LOG_TRACE, send_msg)
			} else {
				traceEvent(settings.logFilter,"login_dropcam>http status: ${resp.status}",detailedNotif)

			//refresh the auth token
				if (resp.status == TOKEN_EXPIRED) {
					if (handleException) {            
						traceEvent(settings.logFilter,"login_dropcam>http status=${resp?.status}: need to re-authorize at Nest",detailedNotif)      
					}                        
                    
				} else {
					traceEvent(settings.logFilter,"login_dropcam>http status=${resp?.status}", detailedNotif)
				} 
				return false                    
			}                
		}        

	} catch (java.net.UnknownHostException e) {
		atomicState?.lastHttpStatus=e?.response?.status
		msg ="login_dropcam>Unknown host - check the URL " + loginParams.uri
		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR, send_msg)
		return false        
	} catch (java.net.NoRouteToHostException e) {
		atomicState?.lastHttpStatus=e?.response?.status
		msg= "login_dropcam>No route to host - check the URL " + loginParams.uri
		traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_ERROR,send_msg)
		return false        
	} catch ( groovyx.net.http.HttpResponseException e) {
		if (atomicState?.access_token) {  // if authenticated, then there is a problem.           
			atomicState?.lastHttpStatus=e?.response?.status
		}        
		traceEvent(settings.logFilter,"login_dropcam>exception $e", detailedNotif, GLOBAL_LOG_TRACE, send_msg)
		traceEvent(settings.logFilter,"login_dropcam>exception, resp.status=${e?.response?.status}", detailedNotif, GLOBAL_LOG_TRACE, send_msg)
		return false        
	}        
	return true    
            
}
private def getAllCameras(useCache=true, cache_timeout=2) {
	settings.detailedNotif=true // set to true initially
	settings.logFilter=5   
	def REFERER= get_REFERER()
	def USER_AGENT=get_USER_AGENT()    
	
	def type="camera"    
	boolean found_in_cache=false
	traceEvent(settings.logFilter,"getAllCameras>begin fetching cameras from dropcam apis...", detailedNotif)
	def msg
 	def TOKEN_EXPIRED=401
	def NEST_SUCCESS=200    
   
	def responseValues=[]
	if (useCache) {    
		def cache="atomicState?.${type}Data"  
		def cache_timestamp= atomicState?."${type}_timestamp"            
		def cached_interval=(cache_timeout) ?:2  // set a minimum of caching to avoid unecessary load on Nest servers
		def time_check_for_cache = (now() - (cached_interval * 60 * 1000))
		traceEvent(settings.logFilter,"getAllCameras>about to get structures from global cache $cache", detailedNotif)
		if ((atomicState?."${type}") && ((cache_timestamp) && (cache_timestamp > time_check_for_cache))) {  //cache still valid
 			traceEvent(settings.logFilter,"cache_timestamp= $cache_timestamp, cache= ${cache}",detailedNotif)
			return "atomicState?.${type}Data" 
		} else {
 			traceEvent(settings.logFilter,"no objects found in cache $cache, ${now()} vs. ${cache}_timestamp=" + cache_timestamp, detailedNotif)
		}
	}        
    
	def cameras= getObject("","camera", useCache, cache_timeout)
	def cameraData=atomicState?.deviceData // to get the cached data so far

	cameras.each {
    
		def request_url= "${get_CAMERA_WEBAPI_BASE()}/api/cameras/${it?.id}"
        
		traceEvent(settings.logFilter,"getAllCameras>request_url=${request_url}", detailedNotif,GLOBAL_LOG_TRACE)
		def deviceListParams = [
			uri: request_url,
			headers: [
				"Authorization": "Basic ${atomicState.access_token}",        
				'Referer': REFERER,
				'User-Agent': USER_AGENT
			],            
		]
        
		traceEvent(settings.logFilter,"device list params: $deviceListParams",detailedNotif)
		try {
			httpGet(deviceListParams) { resp ->
				atomicState?.lastHttpStatus=resp?.status
				traceEvent(settings.logFilter,"getAllCameras>resp.status=${resp?.status}", detailedNotif)
				if (resp?.status == NEST_SUCCESS) {
					traceEvent(settings.logFilter, "getAllCameras>resp.data=${resp?.data}",detailedNotif)
					responseValues=resp.data
				} else {
					traceEvent(settings.logFilter,"getAllCameras>http status: ${resp.status}",detailedNotif)

					//refresh the auth token
					if (resp.status == TOKEN_EXPIRED) {
						if (handleException) {            
							traceEvent(settings.logFilter,"http status=${resp?.status}: need to re-authorize at Nest",
							detailedNotif)      
						}                        
                    
					} else {
						traceEvent(settings.logFilter,"MyNextManager>error (${resp?.status}) while fetching cameras from Nest", detailedNotif)     
					}	
				}                    
			}
		} catch (java.net.UnknownHostException e) {
			atomicState?.lastHttpStatus=e?.response?.status
			msg ="getAllCameras>Unknown host - check the URL " + deviceListParams.uri
			traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
		} catch (java.net.NoRouteToHostException e) {
			atomicState?.lastHttpStatus=e?.response?.status
			msg= "getAllCameras>No route to host - check the URL " + deviceListParams.uri
			traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_ERROR)  
		} catch ( groovyx.net.http.HttpResponseException e) {
			if (atomicState?.access_token) {  // if authenticated, then there is a problem.           
				atomicState?.lastHttpStatus=e?.response?.status
				traceEvent(settings.logFilter,"MyNextManager>error (${e}) while fetching cameras from Nest,trying to re-login", detailedNotif,GLOBAL_LOG_ERROR)     
			}  
			if ((e?.response?.status == TOKEN_EXPIRED) && (google_issue_token_url)) {
				traceEvent(settings.logFilter,"getAllCameras>http status=${e?.response?.status}: about to call login() to re-login",
                detailedNotif)      
				if (login()) { 
					refreshAllChildAuthTokens()
				}               
				                    
			}               
		}
		def object=responseValues[0]    
		def uuid = object?.uuid    
		cameraData[uuid] << ['name':object?.name]	
		cameraData[uuid] << ['is_online': object?.is_online]
		cameraData[uuid] << ['is_streaming': object?.is_streaming]
		cameraData[uuid] << ['battery_voltage': object?.rq_battery_voltage]
		cameraData[uuid] << ['ac_voltage': object?.rq_battery_vbridge_volt]
		cameraData[uuid] << ['location': object?.location]
		cameraData[uuid] << ['data_tier': object?.streaming.data-usage-tier]
		cameraData[uuid] << ['locale':object?.locale]
		cameraData[uuid] << ['software_version':object?.software_version]
		cameraData[uuid] << ['where_id':object?.where_id]
		cameraData[uuid] << ['where_name':object?.where_name]
		cameraData[uuid] << ['label':object?.label]
		cameraData[uuid] << ['last_connection':object?.last_connection]
		cameraData[uuid] << ['last_api_check':object?.last_api_check]
		cameraData[uuid] << ['is_audio_input_enabled':object?.is_audio_input_enabled]
		cameraData[uuid] << ['is_video_history_enabled':object?.is_video_history_enabled]
		cameraData[uuid] << ['web_url':object?.web_url]
		cameraData[uuid] << ['app_url':object?.app_url]
		cameraData[uuid] << ['is_public_share_enabled':object?.is_public_share_enabled]
		cameraData[uuid] << ['public_share_url':object?.public_share_url]
		cameraData[uuid] << ['public_snapshot_url':object?.snapshot_url]
		cameraData[uuid] << ['last_event':object?.last_event]
		cameraData[uuid] << ['last_event_sound':object?.last_event_sound]
		cameraData[uuid] << ['last_event_motion':object?.last_event_motion]
		cameraData[uuid] << ['last_event_start_time':object?.last_event_start_time]
		cameraData[uuid] << ['last_event_end_time':object?.last_event_end_time]		
	
	} /* for each camera */  
	atomicState?.deviceData=cameraData  
   
	return cameraData
}



def getStructures(useCache=true, cache_timeout=2, type=GLOBAL_BUCKETS_CLAUSE, initialLoad=false) {
//	settings.detailedNotif=true // set to true initially
//	settings.logFilter=5   
	boolean found_in_cache=false
	def REFERER= get_REFERER()
	def USER_AGENT=get_USER_AGENT()    
    
	traceEvent(settings.logFilter,"getStructures>begin fetching ${type}...", detailedNotif,GLOBAL_LOG_TRACE)
	def msg
 	def TOKEN_EXPIRED=401
	def NEST_SUCCESS=200    
   
	def responseValues=[]
	if (useCache) {    
		def cache="atomicState?.${type}"  
		def cache_timestamp= atomicState?."${type}_timestamp"            
		def cached_interval=(cache_timeout) ?:2  // set a minimum of caching to avoid unecessary load on Nest servers
		def time_check_for_cache = (now() - (cached_interval * 60 * 1000))
		traceEvent(settings.logFilter,"getStructures>about to get structures from global cache $cache", detailedNotif)
		if ((atomicState?."${type}") && ((cache_timestamp) && (cache_timestamp > time_check_for_cache))) {  //cache still valid
 			traceEvent(settings.logFilter,"cache_timestamp= $cache_timestamp, cache= ${cache}",detailedNotif)
            // devices are already loaded in global variables       
			if ((type == GLOBAL_BUCKETS_TYPES_CLAUSE) || (type == GLOBAL_BUCKETS_CLAUSE)) {
				def cacheDeviceData=[:]
                cacheDeviceData=atomicState?.deviceData            
				return cacheDeviceData
			}     
			responseValues=atomicState?."$type"
			found_in_cache=true      
            
		} else {
 			traceEvent(settings.logFilter,"no objects found in cache $cache, ${now()} vs. ${cache}_timestamp=" + cache_timestamp, detailedNotif)
			state.remove("$type")            
		}
	}        
    
	if (!responseValues) {      
		def requestBody= '{"known_bucket_types":' + " ${type}" +',"known_bucket_versions": []}'   
		traceEvent(settings.logFilter,"getStructures>requestBody=${requestBody}", detailedNotif)
		def deviceListParams = [
			uri: "${get_NEST_URI_ROOT()}/api/0.1/user/${atomicState?.nest_user_id}/app_launch",
			headers: ["Authorization": "Basic ${atomicState.access_token}",
				'charset': "UTF-8",
				'Content-Type': "application/json",
				'Referer': REFERER,
				'User-Agent': USER_AGENT            
			],			                
			body: requestBody,
			query: [format: 'json']
            
		]

		traceEvent(settings.logFilter,"getStructures>device list params: $deviceListParams",detailedNotif,GLOBAL_LOG_TRACE)

		if ((google_issue_token_url) && (isTokenExpired())) {
			traceEvent(settings.logFilter,"getStructures>token expired, calling login()",detailedNotif,GLOBAL_LOG_TRACE)
			login()			        
		}        
		try {
			httpPost(deviceListParams) { resp ->
				atomicState?.lastHttpStatus=resp?.status
				traceEvent(settings.logFilter,"getStructures>resp.status=${resp?.status}", detailedNotif)
				traceEvent(settings.logFilter,"getStructures>resp.data=${resp?.data}", detailedNotif, GLOBAL_LOG_TRACE)
//				traceEvent(settings.logFilter, "getStructures>resp.data.buckets=${resp?.data?.updated_buckets[0]}",detailedNotif, GLOBAL_LOG_TRACE)
				if (resp?.status == NEST_SUCCESS) {
					traceEvent(settings.logFilter, "getStructures>resp.data=${resp?.data}",detailedNotif)
					traceEvent(settings.logFilter, "getStructures>resp.data.buckets=${resp?.data?.updated_buckets[0]}",detailedNotif, GLOBAL_LOG_TRACE)
					responseValues=resp.data
					atomicState?.nest_czfe_url=resp?.data?.service_urls?.urls?.czfe_url // to be used in state change post for individual object later
					traceEvent(settings.logFilter, "getStructures>nest_czfe_url=${atomicState?.nest_czfe_url}",detailedNotif, GLOBAL_LOG_TRACE)
				} else {
					traceEvent(settings.logFilter,"getStructures>http status: ${resp.status}",detailedNotif, GLOBAL_LOG_TRACE)

					if ((resp?.status == TOKEN_EXPIRED)  && (google_issue_token_url)) {
					//refresh the auth token
						traceEvent(settings.logFilter,"getStructures>http status=${resp?.status}: need to re-authorize at Nest, trying to call login()", 
								detailedNotif, GLOBAL_LOG_WARN)      
						if (login()) { 
							refreshAllChildAuthTokens()
						}               
                    
					} else {
						traceEvent(settings.logFilter,"MyNextManager>error (${resp?.status}) while fetching structures from Nest", detailedNotif,GLOBAL_LOG_WARN)     
					}
				} 
			}                
		} catch (java.net.UnknownHostException e) {
			atomicState?.lastHttpStatus=e?.response?.status
			msg ="getStructures>Unknown host - check the URL " + deviceListParams.uri
			traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR)   
		} catch (java.net.NoRouteToHostException e) {
			atomicState?.lastHttpStatus=e?.response?.status
			msg= "getStructures>No route to host - check the URL " + deviceListParams.uri
			traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_ERROR)  
		} catch (groovyx.net.http.HttpResponseException e) {
			if ((e?.response?.status == TOKEN_EXPIRED) && (google_issue_token_url)) { // if authenticated, then there is a problem. 
				traceEvent(settings.logFilter,"getStructures>http status=${e?.response?.status}: about to call login() to re-login",
					detailedNotif, GLOBAL_LOG_WARN)      
				if (login()) { 
					refreshAllChildAuthTokens()
				}               
			}               
			else if ((atomicState?.access_token) && (google_issue_token_url)) {            
				atomicState?.lastHttpStatus=e?.response?.status
				msg="getStructures>error (${e}) while fetching structures from Nest, trying to re-login"     
				traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_INFO) 
				if (login()) { 
					refreshAllChildAuthTokens()
				}               
			}        
		}        
	}        
    

	def objects_in_buckets = [:]
	if (type==GLOBAL_BUCKETS_CLAUSE) {
		objects_in_buckets=	responseValues?.updated_buckets[0]?.value?.buckets
	} else if (type==GLOBAL_WHERE_CLAUSE ) {        
		objects_in_buckets= responseValues?.updated_buckets
	} else {
		objects_in_buckets= responseValues?.updated_buckets
	}    
	def sn, object_data, object_key   
	def deviceData=[:]
	def wheresData=[]
	def cameraList=[],tstatList=[],protectList=[],sensorList=[],structureList=[]
	if (atomicState?.deviceData) {
		deviceData=atomicState?.deviceData    
	}    
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    
	if (atomicState?.cameraList && type != GLOBAL_BUCKETS_CLAUSE) {
		cameraList =  atomicState?.cameraList   
	}    
	if (atomicState?.thermostatList && type != GLOBAL_BUCKETS_CLAUSE) {
		tstatList =  atomicState?.thermostatList   
	}    
	if (atomicState?.sensorList && type != GLOBAL_BUCKETS_CLAUSE) {
		sensorList =  atomicState?.sensorList   
	}    
	if (atomicState?.protectList && type != GLOBAL_BUCKETS_CLAUSE) {
		protectList =  atomicState?.protectList   
	}    
 	if (atomicState?.structureList && type != GLOBAL_BUCKETS_CLAUSE) {
		structureList =  atomicState?.structureList   
	}    
    
	def child_devices=getChildDevices()
	objects_in_buckets.each { object ->
		def data=[:]    
		if (object?.value instanceof Map) {
//			traceEvent(settings.logFilter, "getStructures>object.inspect():${object?.value?.inspect()}", detailedNotif)
		} else {
			traceEvent(settings.logFilter, "getStructures>object=${object}", detailedNotif, GLOBAL_LOG_TRACE)
		}        
		if (type==GLOBAL_BUCKETS_CLAUSE) {    
			if (object.toString().startsWith('topaz.')) {
				sn = object.toString().tokenize('.').last()
				data << ['id': sn,'type': "protect"]
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=Protect discovered...", detailedNotif, GLOBAL_LOG_TRACE)
				def old_data=[:]
				try {
					old_data= deviceData[sn]              
				} catch (e) { }
				deviceData[sn] = (old_data)? (old_data + data): data                
				protectList << sn               
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=Protect added", detailedNotif, GLOBAL_LOG_TRACE)
			} else if (object.toString().startsWith('kryptonite.')) {
				sn = object.toString().tokenize('.').last()
				data << ['id': sn,'type': "sensor"]
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=sensor discovered...", detailedNotif, GLOBAL_LOG_TRACE)
				def old_data=[:]
				try {
					old_data= deviceData[sn]              
				} catch (e) { }
				deviceData[sn] = (old_data)? (old_data + data): data                
				sensorList << sn                
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=sensor added", detailedNotif, GLOBAL_LOG_TRACE)
			} else if (object.toString().startsWith('device.')) {
				sn = object.toString().tokenize('.').last()
				data << ['id': sn,'type': "thermostat"]
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=thermostat discovered...", detailedNotif, GLOBAL_LOG_TRACE)
				def old_data=[:]
				try {
					old_data= deviceData[sn]              
				} catch (e) { }
				deviceData[sn] = (old_data)? (old_data + data): data                
				tstatList << sn                
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=thermostat added", detailedNotif, GLOBAL_LOG_TRACE)
                
			} else if (object.toString().startsWith('quartz.')) {
				sn = object.toString().tokenize('.').last()
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=camera discovered...", detailedNotif, GLOBAL_LOG_TRACE)
				data << ['id': sn,'type': "camera"]
				def old_data=[:]
				try {
					old_data= deviceData[sn]              
				} catch (e) { }
				deviceData[sn] = (old_data)? (old_data + data): data                
				cameraList << sn                
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=camera added", detailedNotif, GLOBAL_LOG_TRACE)
			} else if (object.toString().startsWith('structure.')) {
				sn = object.toString().tokenize('.').last()
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=structure discovered", detailedNotif, GLOBAL_LOG_TRACE)
				data << ['id': sn,'type': "structure"]
				def old_data=[:]
				try {
					old_data= deviceData[sn]              
				} catch (e) { }
				deviceData[sn] = (old_data)? (old_data + data): data                
				structureList << sn                
				traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=structure added", detailedNotif, GLOBAL_LOG_TRACE)
			}
		} else if (type==GLOBAL_WHERE_CLAUSE) {
			wheresData=object?.value?.wheres           
			traceEvent(settings.logFilter, "getStructures>wheresData = ${wheresData}\n\n}", detailedNotif, GLOBAL_LOG_TRACE)
			            
		} else {
        
			object_key= object?.object_key            
			object_data  =object?.value  
			sn = object?.object_key?.toString()?.tokenize('.')?.last()  
			def foundObject=null            
			def deviceType= ""            
			try {            
				foundObject= deviceData[sn]
				deviceType=foundObject?.type
			} catch (Exception e) {
				traceEvent(settings.logFilter, "getStructures>object not found at index $sn\n", detailedNotif)
                
			}            
			            
			if (foundObject) {
				traceEvent(settings.logFilter, "getStructures>about to process found objectId = $sn,  object_Key=$object_key, objectType=$deviceType", detailedNotif)
				if (detailedNotif) {
 					if (object?.value instanceof Map) {  
						Map m = (Map) object_data
							m.each {k,v -> 
							traceEvent(settings.logFilter, "getStructures>found ${deviceType} ${sn}->${k}: ${v}\n", detailedNotif, GLOBAL_LOG_TRACE)
						}
					}                        
				}
			} else {   /* if not found */
				traceEvent(settings.logFilter, "getStructures>not found.. objectId = $sn,  object_Key=$object_key, objectType=$deviceType", detailedNotif)
				if (detailedNotif) {
 					if (object?.value instanceof Map) {            
						Map m = (Map) object_data
						m.each {k,v -> 
							traceEvent(settings.logFilter, "getStructures>not found ${object_key}->${k}: ${v}\n", detailedNotif, GLOBAL_LOG_TRACE)
						}
					}                    
				}
                
			}            
			def selectedInApp = child_devices.find {it.deviceNetworkId.contains(sn)}
			if ((initialLoad) || (selectedInApp && child_devices) || (object_key.toString().startsWith("structure."))) {
				traceEvent(settings.logFilter, "getStructures>object $sn is selected in MyNextManagerV2, about to save data", detailedNotif, GLOBAL_LOG_TRACE)
				traceEvent(settings.logFilter, "getStructures>processing objectId = $sn,  object_Key=$object_key, objectType=$deviceType, data=${object_data}", detailedNotif,GLOBAL_LOG_TRACE)
				if (object_key.toString().startsWith("shared.${sn}")) {
					def new_data =save_shared_data(sn, object_data)                
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
	
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=shared,  data=${deviceData[sn]}", detailedNotif,GLOBAL_LOG_TRACE)
				} else if (object_key.toString().startsWith("topaz.${sn}")) {
					def new_data =save_protect_data(sn, object_data)                
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=Protect,  data=${deviceData[sn]})", detailedNotif,GLOBAL_LOG_TRACE)
				} else if (object_key.toString().startsWith("kryptonite.${sn}")) {
					def new_data = save_sensor_data(sn, object_data)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=Sensor,  data=${deviceData[sn]}\n", detailedNotif,GLOBAL_LOG_TRACE)
				} else if (object_key.toString().startsWith("device.${sn}")) {
					def new_data = save_tstat_data(sn, object_data)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=tstat, data=${deviceData[sn]}", detailedNotif,GLOBAL_LOG_TRACE)
				} else if (object_key.toString().startsWith("quartz.${sn}")) {
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=cam, object_data=${object_dat}\n", detailedNotif,GLOBAL_LOG_TRACE)
					def new_data = save_camera_data(sn,object_data)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=cam, data=${deviceData[sn]}\n", detailedNotif,GLOBAL_LOG_TRACE)
				} else if ((object_key.toString().startsWith("track.${sn}")) && (object?.value instanceof Map)) {
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=track about to process\n", detailedNotif,GLOBAL_LOG_TRACE)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					def new_data = [:]
					new_data << ['is_online': object_data?.online]                    
					new_data << ['last_connection': object_data?.last_connection]                    
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=track processed\n", detailedNotif,GLOBAL_LOG_TRACE)
				} else if ((object_key.toString().startsWith("widget_track.${sn}")) && (object?.value instanceof Map)) {
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=widget_track about to process\n", detailedNotif,GLOBAL_LOG_TRACE)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					def new_data = [:]
					new_data << ['is_online': object_data?.online]                    
					new_data << ['last_connection': object_data?.last_connection]                    
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=widget_track processed\n", detailedNotif,GLOBAL_LOG_TRACE)
				} else if (object_key.toString().startsWith("structure.${sn}")) {
					def new_data = save_structure_data(sn,object_data)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					def thermostats=(String[])  new_data?.devices 
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=structure, thermostats linked to $sn = $thermostats\n", detailedNotif,GLOBAL_LOG_TRACE)
					thermostats.each { // link all tstats to structure_id
						def tstat_id = it?.tokenize('.')?.last()                        
						traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=structure, linking $tstat_id to structure $sn\n", detailedNotif,GLOBAL_LOG_TRACE)
						def tstat_data=[]
						try {
							tstat_data= deviceData[tstat_id]              
						} catch (e) { }
						deviceData[tstat_id]= tstat_data + ['structure_id': sn] 
					}                    
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=structure,processed data=${deviceData[sn]}\n", detailedNotif,GLOBAL_LOG_TRACE)
                
				} else if ((object_key.toString().startsWith("link.${sn}")) && (object?.value instanceof Map)) {
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=link discovered\n,", detailedNotif,GLOBAL_LOG_TRACE)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					def structure_id = object_data?.structure?.tokenize('.')?.last()                        
					def new_data = [:]
					new_data << ['structure_id': structure_id]                    
					traceEvent(settings.logFilter, "getStructures>id = $sn, linking tstat $sn to $structure_id\n", detailedNotif,GLOBAL_LOG_TRACE)
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=link processed\n", detailedNotif,GLOBAL_LOG_TRACE)
               
				} else if ((object_key.toString().startsWith("rcs_settings.${sn}")) && (object?.value instanceof Map)) {
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=rcs settings discovered\n,", detailedNotif,GLOBAL_LOG_TRACE)
					def old_data=[:]
					try {
						old_data= deviceData[sn]              
					} catch (e) { }
					def new_data = [:]
					new_data << ['multiroom_temperature': object_data?.multiroom_temperature]                    
					new_data << ['multiroom_active': object_data?.multiroom_active]                    
					new_data << ['active_rcs_sensors': object_data?.active_rcs_sensor]                    
					new_data << ['associated_rcs_sensors': object_data?.associated_rcs_sensors]                    
					new_data << ['rcs_control_setting': object_data?.rcs_control_setting]                    
					deviceData[sn] = (old_data)? (old_data + new_data): new_data                
					traceEvent(settings.logFilter, "getStructures>id = $sn, objectType=rcs settings processed\n", detailedNotif,GLOBAL_LOG_TRACE)
				}
			} else if (child_devices) { // if not selected
				traceEvent(settings.logFilter, "getStructures>object $sn is not selected in MyNextManagerV2, not saving data & continue processing", detailedNotif, GLOBAL_LOG_TRACE)
			}                           
		}        

	}
    
	if ((responseValues) && (!found_in_cache)  && (atomicState?.lastHttpStatus == NEST_SUCCESS)) {    
		if (type == GLOBAL_BUCKETS_CLAUSE) {
			traceEvent(settings.logFilter, "getStructures>saving lists & deviceData", detailedNotif,GLOBAL_LOG_TRACE)
			cleanupState()			            
			atomicState?.cameraList=cameraList	    
			atomicState?.thermostatList=tstatList	    
			atomicState?.sensorList=sensorList	    
			atomicState?.protectList=protectList
			atomicState?.structureList=structureList
			atomicState?.deviceData=deviceData	
			atomicState?."${type}"=responseValues // save all objects for further references
            
		} else if (type == GLOBAL_WHERE_CLAUSE) {
			traceEvent(settings.logFilter, "getStructures>saving wheresData", detailedNotif,GLOBAL_LOG_TRACE)
			atomicState?."${type}"=responseValues // save all objects for further references
			atomicState?.wheresData=wheresData    
		} else {           
			traceEvent(settings.logFilter, "getStructures>saving deviceData", detailedNotif,GLOBAL_LOG_TRACE)
			state.remove(deviceData)            
			atomicState?.deviceData=deviceData			        
		}
		atomicState?."${type}_timestamp"= now()  // save timestamp for further references
		atomicState?.lastExecutionTimestamp = now() 
	}        
 
	traceEvent(settings.logFilter, "getStructures>end...objectType=Structure, List=${atomicState?.structureList}", detailedNotif,GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter, "getStructures>end...objectType=Protects, List=${atomicState?.protectList}", detailedNotif,GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter, "getStructures>end.. objectType=Thermostats, List=${atomicState?.thermostatList}", detailedNotif,GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter, "getStructures>end.. objectType=Sensors, List=${atomicState?.sensorList}", detailedNotif,GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter, "getStructures>end...objectType=Cameras, List=${atomicState?.cameraList}\n\n", detailedNotif,GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter, "getStructures>end...deviceData=${atomicState?.deviceData}", detailedNotif,GLOBAL_LOG_TRACE)
	return deviceData
}

private def save_structure_data(sn, object_data) {
	traceEvent(settings.logFilter, "save_structure_data>begin...about to save structure data...", detailedNotif,GLOBAL_LOG_TRACE)

	def structureData=[]
    

	def data=[:]

	data << ['id': sn]
	data << ['type': 'structure']
	data << ['name': object_data?.name]
	data << ['geofence_enhanced_autoaway': object_data?.geofence_enhanced_autoaway]
	data << ['longitude': object_data?.longitude]
	data << ['latitude': object_data?.latitude]
	data << ['location': object_data?.location]
	data << ['city': object_data?.city]
	data << ['address_lines': object_data?.address_lines]
	data << ['postal_code': object_data?.postal_code]
	data << ['country_code': object_data?.country_code]
//	data << ['members': object_data?.members]
	data << ['away': object_data?.away]
	data << ['topaz_away': object_data?.topaz_away]
	data << ['user': object_data?.user]
	data << ['away_timestamp': object_data?.away_timestamp]
	data << ['manual_away_timestamp': object_data?.manual_away_timestamp]
	data << ['manual_eco_timestamp': object_data?.manual_eco_timestamp]
	data << ['time_zone': object_data?.time_zone]
	data << ['away_setter': object_data?.away_setter]
	data << ['swarm': object_data?.swarm]
//	data << ['hvac_smoke_safety_shutoff_enabled': object_data?.hvac_smoke_safety_shutoff_enabled]
	data << ['eta': object_data?.eta]
	data << ['eta_preconditioning_active': object_data?.eta_preconditioning_active]
	data << ['eta_unique_id': object_data?.eta_unique_id]
	data << ['manual_eco_all': object_data?.manual_eco_all]
	data << ['num_thermostats': object_data?.num_thermostats]
	data << ['vacation_mode': object_data?.vacation_mode]
	data << ['demand_charge_enabled': object_data?.demand_charge_enabled]
	data << ['devices': object_data?.devices]

    
	return data    
}

private def save_shared_data(sn,object_data) {
	traceEvent(settings.logFilter, "save_shared_data>begin...about to save shared data...", detailedNotif,GLOBAL_LOG_TRACE)
	def wheresData=[]
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    

	def data=[:]
	data << ['id': sn]
	data << ['name': object_data?.name]               
	data << ['current_temperature': object_data?.current_temperature]	
	data << ['target_temperature': object_data?.target_temperature]
 	data << ['target_temperature_type': object_data?.target_temperature_type]
	data << ['hvac_ac_state': object_data?.hvac_ac_state]
	data << ['hvac_heater_state': object_data?.hvac_heater_state]
	data << ['target_temperature_high': object_data?.target_temperature_high]
	data << ['target_temperature_low': object_data?.target_temperature_low]
 	data << ['can_heat': object_data?.can_heat]
	data << ['can_cool': object_data?.can_cool]
	data << ['auto_away': object_data?.auto_away]
	data << ['auto_away_learning': object_data?.auto_away_learning]
	data << ['hvac_cool_x2_state':object_data?.hvac_cool_x2_state]
	data << ['hvac_cool_x3_state':object_data?.hvac_cool_x3_state]
	data << ['hvac_heat_x2_state':object_data?.hvac_heat_x2_state]
	data << ['hvac_heat_x3_state':object_data?.hvac_heat_x3_state]
	data << ['hvac_aux_heater_state':object_data?.hvac_aux_heater_state]
	data << ['hvac_aux_heat_state':object_data?.hvac_aux_heat_state]
	data << ['hvac_emer_heat_state':object_data?.hvac_emer_heat_state]
	data << ['hvac_fan_state':object_data?.hvac_fan_state]
    
	if (object_data?.hvac_ac_state) {
		data << [operating_state: "cooling"]
	} else if (object_data?.hvac_heater_state) {
		data << [operating_state: "heating"]
	} else {				                     
		data << [operating_state: "idle"]
	}   
	def where_object = wheresData.find {it?.where_id == object_data?.where_id} 
	if (where_object) {
		data << ['where_name': where_object?.name]              
	}                    
	if (object_data?.description) {             
		data << ['long_name': object_data?.description ]               	
	} else if (where_object) {
		data << ['long_name': where_object?.name ]               	
	}                
	if (!data?.name) {                
		data << ['name': data.long_name]
	}                    
	traceEvent(settings.logFilter, "save_shared_data>end...saved shared data...", detailedNotif,GLOBAL_LOG_TRACE)
	return data
}

private def save_protect_data(sn, object_data) {
	traceEvent(settings.logFilter, "save_protect_data>begin...about to save protect data...", detailedNotif,GLOBAL_LOG_TRACE)

	def wheresData=[]
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    

	def data=[:]

	data << ['id': sn]
	data << ['type': 'protect']
	if (object_data?.co_status ==0) {
		data << ['co_state':"clear"]                    
	} else if (object_data?.co_status in [1,2]) {
		data << ['co_state':"warning"]                    
	} else if (object_data?.co_status ==3) {
		data << ['co_state':"emergency"]                    
	} else {
		data << ['co_state':"unknown"]                   
	}               
	if (object_data?.smoke_status ==0) {
		data << ['alarm_state':"clear"]                   
	} else if (object_data?.smoke_status in [1,2]) {
		data << ['alarm_state':"warning"]                    
	} else if (object_data?.smoke_status ==3) {
		data << ['alarm_state':"emergency"]                    
	} else {
		data << ['alarm_state':"unknown"]                   
	}               
	if (object_data?.battery_health_state ==0) {
		data << ['battery_health_state': "ok"]                    
	} else if (object_data?.smoke_status in [1,2]) {
		data << ['battery_health_state': "low"]                   
	} else if (object_data?.smoke_status ==3) {
		data << ['battery_health_state': "very low"]                    
	} else {
		data << ['battery_health_state': "unknown"]                    
	}               
	data << ['device_locale': object_data?.device_locale]               
	data << ['software_version': object_data?.software_version]
	data << ['component_smoke_test_passed': object_data?.component_smoke_test_passed]
	data << ['component_co_test_passed': object_data?.component_co_test_passed]
	data << ['component_pir_test_passed': object_data?.component_pir_test_passed]
	data << ['component_speaker_test_passed': object_data?.component_speaker_test_passed]
	data << ['component_heat_test_passed': object_data?.component_heat_test_passed]
	data << ['auto_away': object_data?.auto_away]
	data << ['auto_away_decision_time_secs': object_data?.auto_away_decision_time_secs]
	data << ['heat_status': object_data?.heat_status]
	data << ['night_light_enable': object_data?.night_light_enable]
	data << ['night_light_continuous': object_data?.night_light_continuous]
	data << ['night_light_enable': object_data?.night_light_enable]
	data << ['night_light_brightness': object_data?.night_light_brightness]
	data << ['home_alarm_link_capable': object_data?.home_alarm_link_capable]
	data << ['home_alarm_link_connected': object_data?.home_alarm_link_connected]
	data << ['home_alarm_link_type': object_data?.home_alarm_link_type]
	data << ['is_rcs_capable': object_data?.is_rcs_capable]
	data << ['wired_or_battery': object_data?.wired_or_battery]
	data << ['capability_level': object_data?.capability_level]
	data << ['home_away_input': object_data?.home_away_input]
	data << ['model': object_data?.model]
	data << ['hushed_state': object_data?.hushed_state]
	data << ['last_manual_test_time': object_data?.latest_manual_test_end_utc_secs]
	data << ['structure_id': object_data?.structure_id]
	data << ['battery_level': object_data?.battery_level]
	data << ['last_connection': object_data?.last_connection]
	data << ['replace_by_date_utc_secs': object_data?.replace_by_date_utc_secs]
	data << ['device_born_on_date_utc_secs': object_data?.device_born_on_date_utc_secs]
    
 	def where_object = wheresData.find {it?.where_id == object_data?.where_id} 
	if (where_object) {
		data << ['where_name': where_object?.name]              
	}                    
	if (object_data?.description) {             
		data << ['long_name': object_data?.description ]               	
	} else if (where_object) {
		data << ['long_name': where_object?.name + ' protect' ]               	
	}                
	if (!data?.name) {                
		data << ['name': data.long_name]
	}                    
	traceEvent(settings.logFilter, "save_protect_data>end...saved protect data...", detailedNotif,GLOBAL_LOG_TRACE)
	return data    
}

private def save_sensor_data(sn, object_data) {
	traceEvent(settings.logFilter, "save_sensor_data>begin...about to save sensor data...", detailedNotif,GLOBAL_LOG_TRACE)

	def wheresData=[]
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    
	def data=[:]
	data << ['id': sn]
	data << ['type': 'sensor']

	data << ['name': object_data?.name]               
	data << ['temperature': object_data?.current_temperature]	
	data << ['battery_level': object_data?.battery_level]	
	data << ['last_updated_at': object_data?.last_updated_at]	
 	data << ['structure_id': object_data?.structure_id]
          
	def where_object = wheresData.find {it?.where_id == object_data?.where_id} 
	if (where_object) {
		data << ['where_name': where_object?.name]              
	}                    
	if (object_data?.description) {             
		data << ['long_name': object_data?.description ]               	
	} else if (where_object) {
		data << ['long_name': where_object?.name + ' sensor' ]               	
	}                
	if (!data?.name) {                
		data << ['name': data.long_name]
	}
    
	traceEvent(settings.logFilter, "save_sensor_data>end...saved sensor data...", detailedNotif,GLOBAL_LOG_TRACE)
	return data    
}    

private def save_tstat_data(sn,object_data) {
	traceEvent(settings.logFilter, "save_tstat_data>begin...about to save tstat data...", detailedNotif,GLOBAL_LOG_TRACE)

	def wheresData=[]
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    
	def data=[:]
	data << ['id': sn]
	data << ['type': 'thermostat']
	data << ['temperature_scale': object_data?.temperature_scale]               
	data << ['temperature': object_data?.backplate_temperature]	
	if ( object_data?.battery_level) {                
		data << ['battery_level': object_data?.battery_level]
	}                    
	data << ['device_locale': object_data?.device_locale]               
	data << ['current_schedule_mode': object_data?.current_schedule_mode]
	data << ['has_fan': object_data?.has_fan]
	data << ['has_humidifier': object_data?.has_humidifier]
	data << ['has_dehumidifier': object_data?.has_dehumidifier]
	data << ['cooling_source': object_data?.cooling_source]
	data << ['cooling_x2_source': object_data?.cooling_x2_source]
	data << ['cooling_x3_source': object_data?.cooling_x3_source]
	data << ['heater_delivery': object_data?.heater_delivery]
	data << ['cooling_delivery': object_data?.cooling_delivery]
	data << ['alt_heat_delivery': object_data?.alt_heat_delivery]
	data << ['aux_heat_delivery': object_data?.aux_heat_delivery]
	data << ['heat_x2_delivery': object_data?.heat_x2_delivery]
	data << ['cooling_x2_delivery': object_data?.cooling_x2_delivery]
	data << ['eco_temperature_high': object_data?.leaf_away_high]
	data << ['eco_temperature_low': object_data?.leaf_away_low]
	data << ['temperature_lock_low_temp': object_data?.temperature_lock_low_temp]
	data << ['temperature_lock_high_temp': object_data?.temperature_lock_high_temp]
	data << ['temperature_lock': object_data?.temperature_lock]
	data << ['leaf': object_data?.leaf]
	data << ['leaf_threshold_cool': object_data?.leaf_threshold_cool]
	data << ['leaf_threshold_heat': object_data?.leaf_threshold_heat]
 	data << ['error_code': object_data?.error_code]
	data << ['fan_current_speed': object_data?.fan_current_speed]
	data << ['fan_schedule_speed': object_data?.fan_schedule_speed]
 	data << ['fan_mode': object_data?.fan_mode]
 	data << ['fan_timer_timeout': object_data?.fan_timer_timeout]
 	data << ['fan_timer_duration': object_data?.fan_timer_duration]
 	data << ['fan_capabilities': object_data?.fan_capabilities]
 	data << ['fan_duty_start_time': object_data?.fan_duty_start_time]
 	data << ['fan_duty_end_time': object_data?.fan_duty_end_time]
 	data << ['fan_duty_cycle': object_data?.fan_duty_cycle]
	data << ['equipment_type': object_data?.equipment_type]
 	data << ['humidity': object_data?.current_humidity]
 	data << ['target_humidity': object_data?.target_humidity]
 	data << ['target_humidity_enabled': object_data?.target_humidity_enabled]
	data << ['auto_away_enabled': object_data?.auto_away_enable]
	data << ['auto_away_reset_enabled': object_data?.auto_away_reset]
	data << ['auto_dehum_enabled': object_data?.auto_dehum_enabled]
 	data << ['auto_dehum_state': object_data?.auto_dehum_state]
	data << ['dehumidifier_fan_activation': object_data?.dehumidifier_fan_activation]
 	data << ['humidifier_state': object_data?.humidifier_state]	
	data << ['humidifier_type': object_data?.humidifier_type]
	data << ['humidifier_fan_activation': object_data?.humidifier_fan_activation]
 	data << ['eco_mode': object_data?.eco?.mode]
	data << ['has_hot_water_control':object_data?.has_hot_water_control]
	data << ['target_change_pending': object_data?.target_change_pending]
	data << ['alt_heat_source':object_data?.alt_heat_source]
	data << ['has_aux_heat':object_data?.has_aux_heat]
	data << ['aux_heat_source':object_data?.aux_heat_source]
	data << ['emer_heat_source':object_data?.emer_heat_source]
	data << ['emer_heat_delivery':object_data?.emer_heat_delivery]
	data << ['emer_heat_enabled':object_data?.emer_heat_enable]
	data << ['heat_pump_aux_threshold': object_data?.heat_pump_aux_threshold]
	data << ['has_x2_heat':object_data?.has_x2_heat]
	data << ['has_x3_heat':object_data?.has_x3_heat]
	data << ['has_x2_cool':object_data?.has_x2_cool]
	data << ['has_x3_cool':object_data?.has_x3_cool]
	data << ['has_water_temperature':object_data?.has_hot_water_temperature]
	data << ['compressor_lockout_enabled':object_data?.compressor_lockout_enabled]
	data << ['compressor_lockout_leaf':object_data?.compressor_lockout_leaf]
	data << ['has_dual_fuel': object_data?.has_dual_fuel]
	data << ['dual_fuel_breakpoint': object_data?.dual_fuel_breakpoint]
	data << ['dual_fuel_breakpoint_override': dual_fuel_breakpoint_override]
	data << ['heat_pump_comp_threshold_enabled':object_data?.heat_pump_comp_threshold_enabled]
	data << ['heat_pump_aux_threshold_enabled':object_data?.heat_pump_aux_threshold_enabled]
	data << ['heat_pump_comp_threshold':object_data?.heat_pump_comp_threshold]
	data << ['heat_pump_savings':object_data?.heat_pump_savings]
	data << ['heatpump_setback_active':object_data?.heatpump_setback_active]
	data << ['away_temperature_enabled': (object_data?.away_temperature_enabled) ?: false]
	data << ['away_temperature_high': object_data?.away_temperature_high_adjusted]
	data << ['away_temperature_low': object_data?.away_temperature_low_adjusted]
	data << ['hot_water_active': object_data?.hot_water_active]
	data << ['hot_water_away_active': object_data?.hot_water_away_active]
	data << ['hot_water_boiling_state': object_data?.hot_water_boiling_state]
	data << ['hot_water_boost_time_to_end"': object_data?.hot_water_boost_time_to_end]
	data << ['heater_delivery': object_data?.heater_delivery]
	data << ['time_to_target': object_data?.time_to_target]
	data << ['learning_state': object_data?.learning_state]
	data << ['time_to_target_training': object_data?.time_to_target_training]
	data << ['hvac_smoke_safety_shutoff_active': object_data?.hvac_smoke_safety_shutoff_active]
	data << ['rssi': object_data?.rssi]
	data << ['upper_safety_temp_enabled': object_data?.upper_safety_temp_enabled]
	data << ['upper_safety_temp': object_data?.upper_safety_temp]
	data << ['lower_safety_temp_enabled': object_data?.lower_safety_temp_enabled]
	data << ['lower_safety_temp': object_data?.lower_safety_temp]
	data << ['structure_id': object_data?.structure_id]
	data << ['current_version': object_data?.current_version]
	data << ['radiant_control_enabled': object_data?.radiant_control_enabled]
	data << ['learning_days_completed_cool': object_data?.learning_days_completed_cool]
	data << ['learning_days_completed_heat': object_data?.learning_days_completed_heat]
	data << ['learning_days_completed_range': object_data?.learning_days_completed_range]
	data << ['filter_changed_date': object_data?.filter_changed_date]
	data << ['last_connection': object_data?.last_connection]
	data << ['preconditioning_active': object_data?.preconditioning_active]
	data << ['preconditioning_enabled': object_data?.preconditioning_enabled]
	data << ['rcs_capable': object_data?.rcs_capable]
	data << ['thermostat_alert': object_data?.thermostat_alert]
	data << ['should_wake_on_approach': object_data?.should_wake_on_approach]
	data << ['user_brightness': object_data?.user_brightness]
	data << ['smoke_shutoff_supported': object_data?.smoke_shutoff_supported]
   

	if ((object_data?.online == 'true') || (object_data?.online == 'false')) {    
		data << ['is_online': object_data?.online]
	}			                    

	if (object_data?.eco?.mode in ['manual-eco','auto-eco']) {
		data << ['target_temperature_type': 'eco']
	}
   
	def where_object = wheresData.find {it?.where_id == object_data?.where_id} 
	if (where_object) {
		data << ['where_name': where_object?.name]              
	}                    
	if (object_data?.description) {             
		data << ['long_name': object_data?.description ]               	
	} else if (where_object) {
		data << ['long_name': where_object?.name + ' tstat' ]               	
	}                
	if (!data?.name) {                
		data << ['name': data.long_name]
	}                    

	traceEvent(settings.logFilter, "save_tstat_data>begin...saved tstat data...", detailedNotif,GLOBAL_LOG_TRACE)
	return data
}    

private def save_camera_data(sn, object_data) {
	traceEvent(settings.logFilter, "save_camera_data>begin...about to save camera data...", detailedNotif,GLOBAL_LOG_TRACE)

	def wheresData=[]
	if (atomicState?.wheresData) {
		wheresData=atomicState?.wheresData    
	}    
	def data=[:]
	data << ['id': sn]
	data << ['type': 'camera']
	data << ['name': object_data?.name]               
	data << ['public_token': object_data?.public_token]
 	data << ['snapshot_url': object_data?.snapshot_url]
 	data << ['audio_input_enabled': object_data?.audio_input_enabled]
 	data << ['public_share_enabled': object_data?.public_share_enabled]
	data << ['streaming_state': object_data?.streaming_state]
	data << ['activity_zones': object_data?.activity_zones]
	data << ['capabilities': object_data?.capabilities]
	data << ['last_connect_time': object_data?.last_connect_time]
             
	def where_object = wheresData.find {it?.where_id == object_data?.where_id} 
	if (where_object) {
		data << ['where_name': where_object?.name]              
	}                    
	if (object_data?.description) {             
		data << ['long_name': object_data?.description ]               	
	} else if (where_object) {
		data << ['long_name': where_object?.name]              	
	}                
	if (!data?.name) {                
		data << ['name': data?.long_name + " cam" ]
	}            
	traceEvent(settings.logFilter, "save_camera_data>end...saved camera data...", detailedNotif,GLOBAL_LOG_TRACE)
	return data
}    

def getObject(objectId="", relatedType="", useCache=true, cache_timeout=2) {
//	settings.detailedNotif=true // set to true initially
//	settings.logFilter=5    
	boolean to_be_found_in_cache=false    
	def deviceData=[:]
    
	traceEvent(settings.logFilter,"getObject>begin fetching $relatedType", detailedNotif,GLOBAL_LOG_TRACE)
	def msg
	def objects = []
	def type=GLOBAL_BUCKETS_TYPES_CLAUSE  

	def foundObject=null    
	if (useCache) {
    
		def known_buckets= GLOBAL_BUCKETS_TYPES_CLAUSE  
		traceEvent(settings.logFilter,"getObject>about to call getStructures(), type=known_buckets", detailedNotif,GLOBAL_LOG_TRACE)
		deviceData=getStructures(true, cache_timeout,"${known_buckets}")
	} else {	
		traceEvent(settings.logFilter,"getObject>calling getStructures() to get all structures, not from cache", detailedNotif,GLOBAL_LOG_TRACE)
		def structures
        
		atomicState?.thermostatList=[]        
		atomicState?.protectList=[]        
		atomicState?.sensorList=[]        
		atomicState?.cameraList=[]        
		traceEvent(settings.logFilter,"getObject>about to call getStructures(), type=buckets", detailedNotif,GLOBAL_LOG_TRACE)
		structures=getStructures(true, cache_timeout,GLOBAL_BUCKETS_CLAUSE)
        
		traceEvent(settings.logFilter,"getObject>about to call getStructures(), type=known_buckets", detailedNotif,GLOBAL_LOG_TRACE)
		def known_buckets=GLOBAL_BUCKETS_TYPES_CLAUSE       
		deviceData=getStructures(false, settings.cache_timeout,"${known_buckets}")
	}        
   
	traceEvent(settings.logFilter,"getObject>id=${objectId}, relatedType=$relatedType about to lookup in deviceData...", detailedNotif,GLOBAL_LOG_TRACE)
	if (objectId) {   
		traceEvent(settings.logFilter,"getObject>id=${objectId}, about to lookup $objectId...", detailedNotif,GLOBAL_LOG_TRACE)
		try {
			foundObject = deviceData[objectId]
			if (foundObject) {
				if ((relatedType) && (relatedType != foundObject.type)) {
					traceEvent(settings.logFilter,"getObject>id=${objectId}, type (${foundObject?.type}) is not = relatedType=${relatedType}", detailedNotif, GLOBAL_LOG_WARN)
				}
				objects << foundObject                
			}                
		} catch (Exception e) {
			traceEvent(settings.logFilter,"getObject>id=${objectId} with relatedType=${relatedType}, exception $e", detailedNotif, GLOBAL_LOG_ERROR)
		            
		} 
	} else if (relatedType) {
    
		traceEvent(settings.logFilter,"getObject>About to loop to get all ${relatedType} objects in deviceData...", detailedNotif,GLOBAL_LOG_TRACE)
        
		deviceData.each {key, value ->
			traceEvent(settings.logFilter, "getObject>key= ${key}, object=${value}\n", detailedNotif)  
			if (value instanceof Map) {            
//				traceEvent(settings.logFilter, "getObject>object map=${value.inspect()}\n", detailedNotif)              
				if (relatedType == value?.type) {
					traceEvent(settings.logFilter,"getObject>found ${value?.id},name=${value?.name} with relatedType=${relatedType}, objectType= ${value?.type}", detailedNotif,GLOBAL_LOG_TRACE)
					objects << value                    
				}
			} else {
				traceEvent(settings.logFilter, "getObject>object value=${value}\n", detailedNotif)              
			}            
 		}        
        
	} else {
		objects = deviceData    
	}    
	traceEvent(settings.logFilter,"getObject>end found ${objectId} with relatedType=${relatedType}, return= $objects", detailedNotif,GLOBAL_LOG_TRACE)
	return objects
}


private void delete_obsolete_devices() {
	settings.logFilter=5
	settings.detailedNotif=true    
	def new_tstat_list=[]
	def new_protect_list=[]
	def new_camera_list=[]
	def new_sensor_list=[]
	def old_thermostatList= atomicState?.thermostatList
	def old_cameraList= atomicState?.cameraList
	def old_sensorList= atomicState?.sensorList
	def old_protectList= atomicState?.protectList
	def structure_data =getStructures(false, settings.cache_timeout,GLOBAL_BUCKETS_CLAUSE)
	def known_bucket_types= GLOBAL_BUCKETS_TYPES_CLAUSE
	structure_data =getStructures(false, settings.cache_timeout,"${known_bucket_types}")
	def structureData=getObject("", "structure", true)    // save all structures for futur references    
	atomicState?.structureData=	structureData
	traceEvent(settings.logFilter,"delete_obsolete_devices>structure data found=${structureData.inspect()}\n", detailedNotif)   

	if (atomicState?.lastHttpStatus != 200) {
		traceEvent(settings.logFilter,"delete_obsolete_devices>not able to refresh structures, exiting...", detailedNotif)   
		return            
	}    
    
	if (structure_data) {
		new_tstat_list=atomicState?.thermostatList			    
		new_protect_list=atomicState?.protectList  		    
		new_camera_list= atomicState?.cameraList	    
		new_sensor_list= atomicState?.sensorList	    
	} else {
		traceEvent(settings.logFilter,"delete_obsolete_devices>error fetching structures", detailedNotif)   
	}    
    
	def child_devices=getChildDevices()
	if ( old_thermostatList && old_thermostatList != new_tstat_list ) {
		def IdsToBeDeleted= (new_tstat_list) ? (old_thermostatList - new_tstat_list)  : old_thermotstatList  
		        
		def tstatsSet=[]                    
		IdsToBeDeleted.each {
			def id = it                    
			def tstatToBeDeleted=child_devices.find {it.deviceNetworkId.contains(id) }
			if( tstatToBeDeleted) tstatsSet << tstatToBeDeleted                       
		} 
		if (tstatsSet) {		        
			traceEvent(settings.logFilter, "MyNextManager>about to delete obsolete thermostats under ST: $tstatsSet", true, GLOBAL_LOG_WARN, true)
			delete_child_tstats(tstatsSet)
		}            
	} else if (!new_tstat_list) {
		def deleteTstats = child_devices.findAll {  (it.getName()?.contains(getTstatChildName())) }    
		delete_child_tstats(deleteTstats)
	}    
	if (old_protectList && old_protectList != new_protect_list ) {
		def IdsToBeDeleted= (new_protect_list ) ? (old_protectList - new_protect_list) : old_protectList
		def protectsSet=[]                    
		IdsToBeDeleted.each {
			def id = it                    
			def protectToBeDeleted= child_devices.find{it.deviceNetworkId.contains(id) }
			if( protectToBeDeleted) protectsSet << protectToBeDeleted                       
		}    
		if (protectsSet) {        
			traceEvent(settings.logFilter, "MyNextManager>about to delete obsolete protect units under ST: $protectsSet", true, GLOBAL_LOG_WARN, true)
			delete_child_protects(protectsSet)
		}            
	} else if (!new_protect_list) {
		def deleteProtects=child_devices.findAll {  (it.getName()?.contains(getProtectChildName())) }    
		delete_child_protects(deleteProtects)
	}    
	if (old_cameraList && old_cameraList != new_camera_list) {
		def IdsToBeDeleted= (new_camera_list) ? (old_cameraList - new_camera_list) : old_cameraList 
		def camerasSet=[]                    
		IdsToBeDeleted.each {
			def id = it                    
			def cameraToBeDeleted= child_devices.find {it.deviceNetworkId.contains(id)}
			if(cameraToBeDeleted) camerasSet << cameraToBeDeleted                       
		}   
		if (camerasSet) {
			traceEvent(settings.logFilter, "MyNextManager>about to delete obsolete Nest Cams under ST: $camerasSet", true, GLOBAL_LOG_WARN, true)
			delete_child_cameras(camerasSet)
		}           
	}  else if (!new_camera_list) {
		def deleteCams=child_devices.findAll {  (it.getName()?.contains(getCameraChildName())) }    
		delete_child_cameras(deleteCams )
	}    
	if (old_sensorList && old_sensorList != new_camera_list) {
		def IdsToBeDeleted= (new_sensor_list) ? (old_sensorList - new_sensor_list) : old_sensorList 
		def sensorsSet=[]                    
		IdsToBeDeleted.each {
			def id = it                    
			def sensorToBeDeleted= child_devices.find {it.deviceNetworkId.contains(id)}
			if(sensorToBeDeleted) sensorsSet << sensorToBeDeleted                       
		}   
		if (sensorsSet) {
			traceEvent(settings.logFilter, "MyNextManager>about to delete obsolete Sensors under ST: $sensorsSet", true, GLOBAL_LOG_WARN, true)
			delete_child_sensors(sensorsSet)
		}           
	}  else if (!new_sensor_list) {
		def deleteSensors=child_devices.findAll {  (it.getName()?.contains(getSensorChildName())) }    
		delete_child_sensors(deleteSensors )
	}    

}





private def process_redirectURL(redirect) {

	if (!redirect) {
		return true        
	}    
	def redirectURL= redirect.minus('https://')
	redirectURL= 'https://' + redirectURL.substring(0,(redirectURL?.indexOf('/',0)))             
	traceEvent(settings.logFilter,"process_redirectURL>orignal redirection= ${redirect}, redirectURL=$redirectURL")           
	state?.redirectURLcount=(state?.redirectURLcount?:0)+1            
	save_redirectURL(redirectURL)    
	if (state?.redirectURLcount > get_MAX_REDIRECT()) {
		return false    
	}		        
	return true    
}


def updateObjects(child, objectType, objectId="") {


	traceEvent(settings.logFilter,"updateObjects>atomicState." + objectType + "Data=" + atomicState?."${objectType}Data", detailedNotif)
	traceEvent(settings.logFilter,"updateObjects>about to try to find type=$objectType && objectId=$objectId", detailedNotif)
	def objectsToBeUpdated=[]
	if (objectId) {
		def foundObject= atomicState?."${objectType}Data".find {it.id==objectId}
		if (foundObject) {
			traceEvent(settings.logFilter,"updateObjects> found $objectId data=$foundObject", detailedNotif)
			objectsToBeUpdated << foundObject			            
		}        
	} else {
		objectsToBeUpdated=atomicState?."${objectType}Data"    
	}   
	if (objectsToBeUpdated) {
		child.updateChildData(objectsToBeUpdated)
	}        
	traceEvent(settings.logFilter,"updateObjects>end with type=$objectType && objectId=$objectId, objectsToBeUpdated=$objectsToBeUpdated", detailedNotif)

}

def updateStructure(child, objectId="") {

	def structureData=getObject("", "structure", true)    // save all structures for futur references    
	atomicState?.structureData=	structureData

	traceEvent(settings.logFilter,"updateObjects>atomicState.structureData=${atomicState?.structureData}", detailedNotif)
	traceEvent(settings.logFilter,"updateObjects>about to try to find objectId=$objectId", detailedNotif)
	def objectsToBeUpdated=[]
	if (objectId) {
		def foundObject= atomicState?.structureData.find {it.id==objectId}
		if (foundObject) {
			traceEvent(settings.logFilter,"updateObjects> found $objectId data=$foundObject", detailedNotif)
			objectsToBeUpdated << foundObject			            
		}        
	} else {
		objectsToBeUpdated=atomicState?.structureData    
	}   
   	if (objectsToBeUpdated) child.updateChildStructureData(objectsToBeUpdated)
	traceEvent(settings.logFilter,"updateStructure>end with type=$objectType && objectId=$objectId, objectsToBeUpdated=$objectsToBeUpdated", detailedNotif)

}




def installed() {
	settings.logFilter=4    
	traceEvent(settings.logFilter,"Installed with settings: ${settings}", detailedNotif)
	atomicState?.deviceData=[:]
     
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
	send ("MyNextManager>Your Nest cloud-to-cloud connection will be removed as you've uninstalled the smartapp. You will need to re-login at next execution.")
	delete_child_devices()
//	removeAccessToken()
}



def offHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value", detailedNotif)
}



def rescheduleHandler(evt) {
	traceEvent(settings.logFilter,"$evt.name: $evt.value", detailedNotif)
	rescheduleIfNeeded()		
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
	} 
	def foundProtect=protectUnits.find {dni}    
	if (foundProtect) {
		protectUnits.remove(dni)
		app.updateSetting("protectUnits", protectUnits ? protectUnits : [])
	}	
    
	def foundCam=cameras.find {dni}    
	if (foundCam) {
		cameras.remove(dni)
		app.updateSetting("cameras", cameras ? cameras : [])
	}	
	if (getChildDevices().size <= 1) {
		traceEvent(settings.logFilter,"purgeChildDevice>no more devices to poll, unscheduling and terminating the app", true,GLOBAL_LOG_ERROR)
		unschedule()
		atomicState.authToken=null
		runIn(1, "terminateMe")
	}
}


private void delete_child_tstats(deleteTstats=[]) {

	deleteTstats.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"deleteTstats>exception $e while deleting Nest thermostat ${it.deviceNetworkId},needs to be removed from all automation scenes & rooms", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}


}
private void delete_child_sensors(deleteSensors=[]) {

	deleteSensors.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"deleteTstats>exception $e while deleting Nest Sensor ${it.deviceNetworkId}, needs to be removed from all automation scenes & rooms", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}


}

private void delete_child_protects(deleteProtects=[]) {
	deleteProtects.each { 
		try {    
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"delete_child_protects>exception $e while deleting Nest Protect unit ${it.deviceNetworkId},needs to be removed from all automation scenes & rooms", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}
}


    
private void delete_child_cameras(deleteCameras=[]) {
	deleteCameras.each { 
		try {   
			deleteChildDevice(it.deviceNetworkId) 
		} catch (e) {
			traceEvent(settings.logFilter,"delete_child_cameras>exception $e while deleting Nest Camera ${it.deviceNetworkId},needs to be removed from all automation scenes & rooms", detailedNotif, GLOBAL_LOG_ERROR)
		}   
	}
}

private void delete_child_devices() {
	def deleteProtects=[], deleteTstats=[], deleteCameras=[],deleteSensors=[]
    
	// Delete any that are no longer in settings

	def child_devices=getChildDevices()
	if(!thermostats) {
		deleteTstats = child_devices.findAll {  (it.getName()?.contains(getTstatChildName())) }
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete all Nest thermostats", detailedNotif)
 	} else {
		deleteTstats = child_devices.findAll { ((!thermostats?.contains(it.deviceNetworkId)) && (it.getName()?.contains(getTstatChildName()))) }
 	}
 
	traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteTstats.size()} thermostat devices", detailedNotif)

	delete_child_tstats(deleteTstats)
    
	if(!sensors) {
		deleteSensors = child_devices.findAll {  (it.getName()?.contains(getSensorChildName())) }
 		traceEvent(settings.logFilter,"delete_child_devices>about to delete all Nest Sensors", detailedNotif)
 	} else {
		deleteSensors = child_devices.findAll { ((!sensors?.contains(it.deviceNetworkId)) && (it.getName()?.contains(getSensorChildName()))) }
 	}
 
	traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteTstats.size()} thermostat devices", detailedNotif)

	delete_child_sensors(deleteSensors)


	if(!protectUnits) {
		deleteProtects = child_devices.findAll {  (it.getName()?.contains(getProtectChildName())) }
		traceEvent(settings.logFilter,"delete_child_devices>about to delete all Nest Protects", detailedNotif)
	} else {
		deleteProtects = child_devices.findAll { ((!protectUnits?.contains(it.deviceNetworkId)) && (it.getName()?.contains(getProtectChildName()))) }
	}        
	traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteProtects.size()} protect devices", detailedNotif)

	delete_child_protects(deleteProtects)
	
	if(!cameras) {
		deleteCameras = child_devices.findAll {(it.getName()?.contains(getCameraChildName()))}
		traceEvent(settings.logFilter,"delete_child_devices>about to delete all Nest Cameras", detailedNotif)
	} else {

		deleteCameras = child_devices.findAll { ((!cameras?.contains(it.deviceNetworkId)) && (it.getName()?.contains(getCameraChildName())))}
	}        
	traceEvent(settings.logFilter,"delete_child_devices>about to delete ${deleteCameras.size()} Nest cameras", detailedNotif)

	delete_child_cameras(deleteCameras)


}


private void create_child_tstats() {

   	int countNewChildDevices =0

	def allChildDevices=getChildDevices()
	def devices = thermostats.collect { dni ->

							 
		traceEvent(settings.logFilter,"create_child_tstats>looping thru thermostats, found id $dni", detailedNotif)
		def tstat_info  = dni.tokenize('.')
		def thermostatId = tstat_info.last()
 		def name = tstat_info[1]
		def d= allChildDevices.find {it.deviceNetworkId.contains(thermostatId)}               

		if(!d) {
									  
									   
							
			def labelName = 'MyTstat ' + "${name}"
			traceEvent(settings.logFilter,"create_child_tstats>about to create child device with id $dni, thermostatId = $thermostatId, name=  ${name}", detailedNotif)
			d = addChildDevice(getTstatChildNamespace(), getTstatChildName(), dni, null,
				[label: "${labelName}"]) 
			def auth_data = [:]
			auth_data?.access_token=atomicState?.access_token            
			auth_data?.expires_in=atomicState?.expires_in            
			auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
			auth_data?.nest_user_id=atomicState?.nest_user_id            
			auth_data?.nest_auth_token=atomicState?.nest_auth_token            
			auth_data?.google_jwt=atomicState?.google_jwt            
			auth_data?.authexptime=atomicState?.authexptime            
			d.initialSetup( auth_data, thermostatId) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_tstats>created ${d.displayName} with id $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_tstats>found ${d.displayName} with id $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
 					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_tstats>exception $e while trying to refresh existing tokens in child $d", detailedNotif,GLOBAL_LOG_ERROR)
            
			}            
		}

	}

	traceEvent(settings.logFilter,"create_child_devices>created $countNewChildDevices, total=${devices.size()} thermostats", detailedNotif)

}


private void create_child_protects() {

   	int countNewChildDevices =0
	def allChildDevices=getChildDevices()
	def devices = protectUnits.collect { dni ->

		def protect_info  = dni.tokenize('.')
		def protectId = protect_info.last()
 		def name = protect_info[1]
		def d= allChildDevices.find {it.deviceNetworkId.contains(protectId)}               
		traceEvent(settings.logFilter,"create_child_protects>looping thru protects, found id $dni", detailedNotif)

		if(!d) {
										
									  
							  
			def labelName = 'MyAlarm ' + "${name}"
			traceEvent(settings.logFilter,"create_child_protects>about to create child device with id $dni, protectId = $protectId, name=  ${name}", detailedNotif)
			d = addChildDevice(getProtectChildNamespace(), getProtectChildName(), dni, null,
				[label: "${labelName}"]) 
			def auth_data = [:]
			auth_data?.access_token=atomicState?.access_token            
			auth_data?.expires_in=atomicState?.expires_in            
			auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
			auth_data?.nest_auth_token=atomicState?.nest_auth_token            
			auth_data?.nest_user_id=atomicState?.nest_user_id            
			auth_data?.google_jwt=atomicState?.google_jwt            
			auth_data?.authexptime=atomicState?.authexptime            
			d.initialSetup( auth_data, protectId) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_protects>created ${d.displayName} with id $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_protects>found ${d.displayName} with id $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
 					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_protects>exception $e while trying to refresh existing tokens in child $d", detailedNotif)
            
			}            
		}

	}

	traceEvent(settings.logFilter,"create_child_devices>created $countNewChildDevices, total=${devices.size()} protects", detailedNotif)
}


private void create_child_cameras() {

   	int countNewChildDevices =0
	def allChildDevices=getChildDevices()    
	def devices = cameras.collect { dni ->

		def camera_info  = dni.tokenize('.')
		def cameraId = camera_info.last()
 		def name = camera_info[1]
		def d= allChildDevices.find {it.deviceNetworkId.contains(cameraId)}               
		traceEvent(settings.logFilter,"create_child_cameras>looping thru cameras, found id $dni", detailedNotif)

		if(!d) {
									   
									
							 
			def labelName = 'MyCam ' + "${name}"
			traceEvent(settings.logFilter,"create_child_cameras>about to create child device with id $dni, cameraId = $cameraId, name=  ${name}", detailedNotif)
			d = addChildDevice(getChildNamespace(), getCameraChildName(), dni, null,
				[label: "${labelName}"])  
			def auth_data = [:]
			auth_data?.access_token=atomicState?.access_token            
			auth_data?.expires_in=atomicState?.expires_in            
			auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
			auth_data?.nest_user_id=atomicState?.nest_user_id            
			auth_data?.nest_auth_token=atomicState?.nest_auth_token            
			auth_data?.google_jwt=atomicState?.google_jwt            
			auth_data?.authexptime=atomicState?.authexptime            
			d.initialSetup( auth_data, cameraId) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_cameras>created ${d.displayName} with id $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_cameras>found ${d.displayName} with id $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
					                
 					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_cameras>exception $e while trying to refresh existing tokens in child $d", detailedNotif)
            
			}            
		}

	}

}

private void create_child_sensors() {

   	int countNewChildDevices =0
	def allChildDevices=getChildDevices()    
	def devices = sensors.collect { dni ->

		def sensor_info  = dni.tokenize('.')
		def sensorId = sensor_info.last()
 		def name = sensor_info[1]
		def d= allChildDevices.find {it.deviceNetworkId.contains(sensorId)}               
		traceEvent(settings.logFilter,"create_child_sensors>looping thru sensors, found id $dni", detailedNotif)

		if(!d) {
									   
									
							 
			def labelName = 'MySensor ' + "${name}"
			traceEvent(settings.logFilter,"create_child_sensors>about to create child device with id $dni, sensorId = $sensorId, name=  ${name}", detailedNotif)
			d = addChildDevice(getChildNamespace(), getSensorChildName(), dni, null,
				[label: "${labelName}"])  
			def auth_data = [:]
			auth_data?.access_token=atomicState?.access_token            
			auth_data?.expires_in=atomicState?.expires_in            
			auth_data?.nest_czfe_url=atomicState?.nest_czfe_url            
			auth_data?.nest_user_id=atomicState?.nest_user_id            
			auth_data?.nest_auth_token=atomicState?.nest_auth_token            
			auth_data?.google_jwt=atomicState?.google_jwt            
			auth_data?.authexptime=atomicState?.authexptime            
			d.initialSetup( auth_data, sensorId) 	// initial setup of the Child Device
			traceEvent(settings.logFilter,"create_child_sensors>created ${d.displayName} with id $dni", detailedNotif)
			countNewChildDevices++            
		} else {
			traceEvent(settings.logFilter,"create_child_sensors>found ${d.displayName} with id $dni already exists", detailedNotif)
			try {
				if (d.isTokenExpired()) {  // called refreshAllChildAuthTokens when updated
 					refreshAllChildAuthTokens()    
				}
			} catch (e) {
				traceEvent(settings.logFilter,"create_child_sensors>exception $e while trying to refresh existing tokens in child $d", detailedNotif)
            
			}            
		}

	}

}
def initialize() {
    
	traceEvent(settings.logFilter,"initialize begin...", detailedNotif)
	atomicState?.exceptionCount=0
	atomicState?.sendExceptionCount=0
	atomicState?.lastHttpStatus=200    
	def msg
	atomicState?.poll = [ last: 0, rescheduled: now() ]
        
	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "askAlexaMQ", askAlexaMQHandler)    
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
	if (energyMeter)	{
		subscribe(energyMeter,"energy", rescheduleHandler,[filterEvents: false])
	}
	if (motionSensor)	{
		subscribe(motionSensor,"motion", rescheduleHandler,[filterEvents: false])
	}

	subscribe(app, appTouch)
	delete_child_devices()	
	create_child_tstats()
	create_child_protects()
//	create_child_cameras()
	create_child_sensors()
	rescheduleIfNeeded()  
    
}

def askAlexaMQHandler(evt) {
	if (!evt) return
	switch (evt.value) {
		case "refresh":
		state?.askAlexaMQ = evt.jsonData && evt.jsonData?.queues ? evt.jsonData.queues : []
		traceEvent(settings.logFilter,"askAlexaMQHandler>new refresh value=$evt.jsonData?.queues", detailedNotif, GLOBAL_LOG_TRACE)
		break
	}
}

def appTouch(evt) {
	rescheduleIfNeeded() 
	takeAction()    
}

private def cleanupState() {
	traceEvent(settings.logFilter,"cleanupState>About clean up some state variables used...", detailedNotif, GLOBAL_LOG_TRACE)
/*    
	state.remove("deviceData")
   
	state.remove("thermostatData")
	state.remove("protectData")
	state.remove("sensorData")    
	state.remove("thermostatList")
	state.remove("protectList")
	state.remove("sensorList")
*/    
	state.remove("${GLOBAL_BUCKETS_CLAUSE}")
	state.remove("${GLOBAL_BUCKETS_TYPES_CLAUSE}")
    
//  remove old caches from previous versions
//	state.remove('["buckets","demand_response","device","device_alert_dialog","shared","topaz","kryptonite","quartz","link","structure","structure_metadata", "metadata", "occupancy","user","user_settings","rcs_settings","track","widget_track"]')
//	state.remove('["demand_response","device","device_alert_dialog","shared","topaz","kryptonite","quartz","link","structure","structure_metadata", "metadata", "occupancy","user","user_settings","rcs_settings","track","widget_track"]') 
    
	traceEvent(settings.logFilter,"cleanupState>clean up of some state variables done", detailedNotif, GLOBAL_LOG_TRACE)
    
}



def rescheduleIfNeeded(evt) {
	if (evt) traceEvent(settings.logFilter,"rescheduleIfNeeded>$evt.name=$evt.value", detailedNotif)
	int delay = (givenInterval) ? givenInterval.toInteger() : 10 // By default, do it every 10 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (atomicState?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		traceEvent(settings.logFilter,"rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago", detailedNotif, GLOBAL_LOG_TRACE)
		takeAction()        
	}
	if (((atomicState?.poll["last"]?:0) + (delay * 60000) < currentTime)) {
		traceEvent(settings.logFilter,"rescheduleIfNeeded>scheduling takeAction in ${delay} minutes..", detailedNotif,GLOBAL_LOG_TRACE)
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
	traceEvent(settings.logFilter,"takeAction>begin", detailedNotif)
	def todayDay
	atomicState?.newDay=false      
    
	if (!location.timeZone) {    	
		traceEvent(settings.logFilter,"takeAction>Your location is not set in your ST account, you'd need to set it as indicated in the prerequisites for better exception handling..",
			true,GLOBAL_LOG_ERROR, true)
	} else {
		todayDay = new Date().format("dd",location.timeZone)
	}        
	if ((!atomicState?.today) || (todayDay != atomicState?.today)) {
		atomicState?.exceptionCount=0   
		atomicState?.sendExceptionCount=0 
		atomicState?.newDay=true        
		atomicState?.today=todayDay  
		atomicState?.pollRotation=0
	}   
    
	int delay = (givenInterval) ? givenInterval.toInteger() : 10 // By default, do it every 10 min.
	atomicState?.poll["last"] = now()
		
	//schedule the rescheduleIfNeeded() function
    
	if (((atomicState?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		traceEvent(settings.logFilter,"takeAction>scheduling rescheduleIfNeeded() in ${delay} minutes..", detailedNotif, GLOBAL_LOG_TRACE)
		unschedule()        
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		atomicState?.poll["rescheduled"] = now()
	}
    
//	delete_obsolete_devices()
	def structures
//	traceEvent(settings.logFilter,"poll>about to call, type=[buckets]", detailedNotif,GLOBAL_LOG_TRACE)
//	def structures =getStructures(false, settings.cache_timeout,GLOBAL_BUCKETS_CLAUSE)
//	structures=getStructures(false, settings.cache_timeout,GLOBAL_WHERE_CLAUSE)
	def known_buckets= GLOBAL_BUCKETS_TYPES_CLAUSE  
	traceEvent(settings.logFilter,"poll>about to call, type=$known_buckets", detailedNotif,GLOBAL_LOG_TRACE)
	structures=getStructures(true, settings.cache_timeout,"${known_buckets}")
    
	if (!atomicState?.pollRotation) {    
		atomicState?.pollRotation=1
		poll_tstats()    
		poll_protects()    
	//	poll_cameras()   
		poll_sensors()   
		        
	} else if (atomicState?.pollRotation==1) {
		atomicState?.pollRotation=2
		poll_protects()    
		poll_tstats()    
	//	poll_cameras()   
		poll_sensors()   
	} else {   
		atomicState?.pollRotation=0
		poll_sensors()   
		poll_tstats()    
		poll_protects()    
	//	poll_cameras()   
	}
    
	traceEvent(settings.logFilter,"takeAction>end", detailedNotif)

}



private void poll_protects() {
	traceEvent(settings.logFilter,"poll_protects>begin", detailedNotif)
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false
    
	def objects= getObject("", "protect",true, settings.cache_timeout)    
	if (objects) {  
		state.remove(protectData)     
		atomicState?.protectData=objects        
	}    
	def devicesProtects = protectUnits.collect { dni ->
		def d = getChildDevice(dni)
		if (d) {       
			traceEvent(settings.logFilter,"poll_protects>Looping thru protects, found id $dni, about to poll", detailedNotif, GLOBAL_LOG_TRACE)
			d.poll()
			exceptionCheck = d.currentVerboseTrace?.toString()
			if ((exceptionCheck) && (((exceptionCheck.contains("exception") || (exceptionCheck.contains("error"))) && 
				(!exceptionCheck.contains("TimeoutException")) && (!exceptionCheck.contains("No signature of method: physicalgraph.device.CommandService.executeAction")) &&
				(!exceptionCheck.contains("UndeclaredThrowableException"))))) {  
				// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
				atomicState.exceptionCount=atomicState.exceptionCount+1    
				traceEvent(settings.logFilter,"found exception/error after polling, exceptionCount= ${atomicState?.exceptionCount}: $exceptionCheck", detailedNotif, GLOBAL_LOG_ERROR) 
			} else {   // poll was successful          
				// reset exception counter            
				atomicState?.exceptionCount=0   
				if (atomicState?.newDay && askAlexaFlag) { // produce summary reports only at the beginning of a new day
					def PAST_DAY_SUMMARY=1 // day
					def PAST_WEEK_SUMMARY=7 // days
					if (settings.protectDaySummaryFlag) {
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past day", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_DAY_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}                    
					if (settings.protectWeeklySummaryFlag) { // produce summary report only at the beginning of a new day
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past week", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_WEEK_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}
				} /* end if askAlexa */                    
                
			}                
			                
		} /* end if (d) */        
	}
    
	if (handleException) {    
		if ((exceptionCheck) && (exceptionCheck.contains("Unauthorized")) && (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT)) {
			// need to authenticate again    
			atomicState?.access_token=null                    
			atomicState?.oauthTokenProvided=false
			traceEvent(settings.logFilter,"$exceptionCheck after ${atomicState?.exceptionCount} errors, press on 'Next' and re-login at Nest..." , 
				true, GLOBAL_LOG_ERROR,true)
		} else if (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT) {
			traceEvent(settings.logFilter,"too many exceptions/errors, $exceptionCheck (${atomicState?.exceptionCount} errors so far), you may need to press on 'Next' and re-login at Nest..." ,
				true, GLOBAL_LOG_ERROR, true)
		}
	} /* end if handleException */        

	traceEvent(settings.logFilter,"poll_protects>end", detailedNotif)

}

private void poll_sensors() {
	traceEvent(settings.logFilter,"poll_sensors>begin", detailedNotif)
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false
    
	def objects= getObject("", "sensor", true, settings.cache_timeout)    
	if (objects) {  
		state.remove(sensorData)     
		atomicState?.sensorData=objects        
	}    
	def devicesSensors = sensors.collect { dni ->
		def d = getChildDevice(dni)
		if (d) {       
			traceEvent(settings.logFilter,"poll_sensors>Looping thru protects, found id $dni, about to poll", detailedNotif, GLOBAL_LOG_TRACE)
			d.poll()
			exceptionCheck = d.currentVerboseTrace?.toString()
			if ((exceptionCheck) && (((exceptionCheck.contains("exception") || (exceptionCheck.contains("error"))) && 
				(!exceptionCheck.contains("TimeoutException")) && (!exceptionCheck.contains("No signature of method: physicalgraph.device.CommandService.executeAction")) &&
				(!exceptionCheck.contains("UndeclaredThrowableException"))))) {  
				// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
				atomicState.exceptionCount=atomicState.exceptionCount+1    
				traceEvent(settings.logFilter,"found exception/error after polling, exceptionCount= ${atomicState?.exceptionCount}: $exceptionCheck", detailedNotif, GLOBAL_LOG_ERROR) 
			} else {   // poll was successful          
				// reset exception counter            
				atomicState?.exceptionCount=0   
				if (atomicState?.newDay && askAlexaFlag) { // produce summary reports only at the beginning of a new day
					def PAST_DAY_SUMMARY=1 // day
					def PAST_WEEK_SUMMARY=7 // days
					if (settings.protectDaySummaryFlag) {
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past day", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_DAY_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}                    
					if (settings.protectWeeklySummaryFlag) { // produce summary report only at the beginning of a new day
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past week", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_WEEK_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}
				} /* end if askAlexa */                    
                
			}                
			                
		} /* end if (d) */        
	}
    
	if (handleException) {    
		if ((exceptionCheck) && (exceptionCheck.contains("Unauthorized")) && (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT)) {
			// need to authenticate again    
			atomicState?.access_token=null                    
			atomicState?.oauthTokenProvided=false
			traceEvent(settings.logFilter,"$exceptionCheck after ${atomicState?.exceptionCount} errors, press on 'Next' and re-login at Nest..." , 
				true, GLOBAL_LOG_ERROR,true)
		} else if (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT) {
			traceEvent(settings.logFilter,"too many exceptions/errors, $exceptionCheck (${atomicState?.exceptionCount} errors so far), you may need to press on 'Next' and re-login at Nest..." ,
				true, GLOBAL_LOG_ERROR, true)
		}
	} /* end if handleException */        

	traceEvent(settings.logFilter,"poll_sensors>end", detailedNotif)

}



private void poll_tstats() {
	traceEvent(settings.logFilter,"poll_tstats>begin", detailedNotif)
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false

	def objects= getObject("", "thermostat", true, settings.cache_timeout)    
	if (objects) {  
		state.remove(thermostatData)     
		atomicState?.thermostatData=objects        
	}    

	def devicesTstats = thermostats.collect { dni ->
		def d = getChildDevice(dni)
		if (d) {       
			traceEvent(settings.logFilter,"poll_tstats>Looping thru thermostats, found id $dni, about to poll", detailedNotif, GLOBAL_LOG_TRACE)
			d.poll()
			exceptionCheck = d.currentVerboseTrace?.toString()
			if ((exceptionCheck) && (((exceptionCheck.contains("exception") || (exceptionCheck.contains("error"))) && 
				(!exceptionCheck.contains("TimeoutException")) && (!exceptionCheck.contains("No signature of method: physicalgraph.device.CommandService.executeAction")) &&
				(!exceptionCheck.contains("UndeclaredThrowableException"))))) {  
				// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
				atomicState.exceptionCount=atomicState.exceptionCount+1    
				traceEvent(settings.logFilter,"found exception/error after polling, exceptionCount= ${atomicState?.exceptionCount}: $exceptionCheck", detailedNotif, GLOBAL_LOG_ERROR) 
			} else {             
				// reset exception counter            
				atomicState?.exceptionCount=0      
				if (atomicState?.newDay && askAlexaFlag) { // produce summary reports only at the beginning of a new day
					def PAST_DAY_SUMMARY=1 // day
					def PAST_WEEK_SUMMARY=7 // days
					if (settings.tstatDaySummaryFlag) {
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past day", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_DAY_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}                    
					if (settings.tstatWeeklySummaryFlag) { // produce summary report only at the beginning of a new day
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past week", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_WEEK_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}
				} /* end if askAlexa */                    
			}                
		} /* end if (d) */        
	} 
	
	if (handleException) {    
		if ((exceptionCheck) && (exceptionCheck.contains("Unauthorized")) && (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT)) {
			// need to authenticate again    
			atomicState?.access_token=null                    
			atomicState?.oauthTokenProvided=false
			traceEvent(settings.logFilter,"$exceptionCheck after ${atomicState?.exceptionCount} errors, press on 'Next' and re-login at Nest..." , 
				true, GLOBAL_LOG_ERROR,true)
		} else if (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT) {
			traceEvent(settings.logFilter,"too many exceptions/errors, $exceptionCheck (${atomicState?.exceptionCount} errors so far), you may need to press on 'Next' and re-login at Nest..." ,
				true, GLOBAL_LOG_ERROR, true)
		}
	} /* end if handleException */        

	traceEvent(settings.logFilter,"poll_tstats>end", detailedNotif)

}

private void poll_cameras() {
	traceEvent(settings.logFilter,"poll_cameras>begin", detailedNotif)
	String exceptionCheck    
	def MAX_EXCEPTION_COUNT=10
	boolean handleException = (handleExceptionFlag)?: false
    
	def objects= getObject("", "camera", true, settings.cache_timeout)    
	if (objects) {  
		state.remove(cameraData)     
		atomicState?.cameraData=objects        
	}    

	def deviceCameras = cameras.collect { dni ->
		def d = getChildDevice(dni)
		if (d) {       
			traceEvent(settings.logFilter,"poll_cameras>Looping thru cameras, found id $dni, about to poll", detailedNotif)
			d.poll()
			exceptionCheck = d.currentVerboseTrace?.toString()
			if ((exceptionCheck) && (((exceptionCheck.contains("exception") || (exceptionCheck.contains("error"))) && 
				(!exceptionCheck.contains("TimeoutException")) && (!exceptionCheck.contains("No signature of method: physicalgraph.device.CommandService.executeAction")) &&
				(!exceptionCheck.contains("UndeclaredThrowableException"))))) {  
				// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
				atomicState.exceptionCount=atomicState.exceptionCount+1    
				traceEvent(settings.logFilter,"found exception/error after polling, exceptionCount= ${atomicState?.exceptionCount}: $exceptionCheck", detailedNotif, GLOBAL_LOG_ERROR, true) 
			} else {             
				// reset exception counter            
				atomicState?.exceptionCount=0      
				if (atomicState?.newDay && askAlexaFlag) { // produce summary reports only at the beginning of a new day
					def PAST_DAY_SUMMARY=1 // day
					def PAST_WEEK_SUMMARY=7 // days
					if (settings.camDaySummaryFlag) {
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past day", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_DAY_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}                    
					if (settings.camWeeklySummaryFlag) { // produce summary report only at the beginning of a new day
						traceEvent(settings.logFilter,"About to call produceSummaryReport for device ${d.displayName} in the past week", detailedNotif, GLOBAL_LOG_TRACE) 
						d.produceSummaryReport(PAST_WEEK_SUMMARY)
						String summary_report =d.currentValue("summaryReport")                        
						if (summary_report) {                        
							send (summary_report, askAlexaFlag)                        
						}                            
					}
				} /* end if askAlexa */                    
			}                
		} /* end if (d) */        
	}
	
	if (handleException) {    
		if ((exceptionCheck) && (exceptionCheck.contains("Unauthorized")) && (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT)) {
			// need to authenticate again    
			atomicState?.access_token=null                    
			atomicState?.oauthTokenProvided=false
			traceEvent(settings.logFilter,"$exceptionCheck after ${atomicState?.exceptionCount} errors, press on 'Next' and re-login at Nest..." , 
				true, GLOBAL_LOG_ERROR,true)
		} else if (atomicState?.exceptionCount>=MAX_EXCEPTION_COUNT) {
			traceEvent(settings.logFilter,"too many exceptions/errors, $exceptionCheck (${atomicState?.exceptionCount} errors so far), you may need to press on 'Next' and re-login at Nest..." ,
				true, GLOBAL_LOG_ERROR, true)
		}
	} /* end if handleException */        

	traceEvent(settings.logFilter,"poll_cameras>end", detailedNotif)

}


def isTokenExpired() {
	if (!google_issue_token_url) return false
	def buffer_time_expiration=5  // set a 5 min. buffer time before token expiration to avoid auth_err 
	def time_check_for_exp = now() + (buffer_time_expiration * 60 * 1000)
	traceEvent(settings.logFilter,"isTokenExpired>expiresIn timestamp: ${atomicState?.authexptime} > timestamp check for exp: ${time_check_for_exp}?", detailedNotif)
	if ((atomicState?.authexptime) && (atomicState?.authexptime > time_check_for_exp)) {
		traceEvent(settings.logFilter,"isTokenExpired>not expired", detailedNotif)
		return false
	}
	traceEvent(settings.logFilter,"isTokenExpired>expired", detailedNotif)
	return true    
}


def loginFromUI() {
	settings.logFilter=5
	settings.detailedNotif=true

	if (login()) {
    
		success()    
	} else {
		fail()    
	}    
}

def login() {
	settings.logFilter=5
	settings.detailedNotif=true
	boolean send_msg=false
//	send_msg=true    
	def NEST_SUCCESS=200
	def msg
   
	traceEvent(settings.logFilter,"login>begin", detailedNotif, GLOBAL_LOG_TRACE)

	String cookie= get_AppSettingsValue(google_cookiep1) + get_AppSettingsValue(google_cookiep2) +get_AppSettingsValue(google_cookiep3) +get_AppSettingsValue(google_cookiep4) +
		get_AppSettingsValue(google_cookiep5) +get_AppSettingsValue(google_cookiep6) 
	def issue_token_url=get_AppSettingsValue(google_issue_token_url)   
	atomicState?.google_issue_token_url=issue_token_url 
	atomicState?.cookie=cookie        
/*
	String cookie= get_AppSettingsValue('google_cookiep1_OCAK') + ' ' + get_AppSettingsValue('google_cookiep2')  +
		' ' + get_AppSettingsValue("google_cookiep3") + ' ' +' ' +  get_AppSettingsValue("google_cookiep4") +
		' ' + get_AppSettingsValue("google_cookiep5") + ' ' + get_AppSettingsValue("google_cookiep6")
	atomicState?.cookie=cookie        
    
	def issue_token_url=get_AppSettingsValue('google_issue_token_url')   
	atomicState?.google_issue_token_url=issue_token_url 
*/    
	traceEvent(settings.logFilter,"login>google_issue_url=$issue_token_url", detailedNotif, GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter,"login>cookie=$cookie", detailedNotif)
	traceEvent(settings.logFilter,"login>google_issue_url=$issue_token_url", detailedNotif, GLOBAL_LOG_TRACE)
	traceEvent(settings.logFilter,"login>cookie=$cookie", detailedNotif)
    
	def API_URL = get_NEST_URI_ROOT()
	def USER_AGENT = get_USER_AGENT()
    
    
	def URL_JWT = "https://nestauthproxyservice-pa.googleapis.com/v1/issue_jwt"

	// Nest website's (public) API key
	def NEST_API_KEY = "AIzaSyAdkSIMNc51XGNEAYWasX9UOWkS5P6sZE4"

	def nest_access_token=null
    
	def params= [
		uri: issue_token_url,   
    	headers: [
            'User-Agent': USER_AGENT,
            'Sec-Fetch-Mode': 'cors',
            'X-Requested-With': 'XmlHttpRequest',
            'Referer': 'https://accounts.google.com/o/oauth2/iframe',
            'cookie': cookie
		]
	]        
	traceEvent(settings.logFilter,"login>params=$params", detailedNotif)
	try {
		httpGet(params) { resp ->
				atomicState?.lastHttpStatus=resp?.status
				traceEvent(settings.logFilter,"login>resp.status=${resp?.status}", detailedNotif, GLOBAL_LOG_TRACE,send_msg)
                
				if (resp?.data?.toString().contains("USER_LOGGED_OUT")) {
                
					traceEvent(settings.logFilter,"login>error=${resp?.data}, your home.nest.com session is no longer active, need to get new issue_token_url & cookie from Google account login, exiting...", detailedNotif, GLOBAL_LOG_ERROR,true)
					return false               
				}                
				if (resp?.status == NEST_SUCCESS) {
					traceEvent(settings.logFilter, "login>resp.data=${resp?.data}",detailedNotif,  GLOBAL_LOG_TRACE,send_msg)
					nest_access_token=resp?.data?.access_token
					atomicState?.expires_in= resp?.data.expires_in 
					if (atomicState?.expires_in) {                     
						def authexptime = new Date((now() + (resp?.data?.expires_in  * 1000)))
						atomicState?.authexptime = authexptime.getTime()
					}                        
					traceEvent(settings.logFilter, "login>nest_access_token=${resp?.data}",detailedNotif,  GLOBAL_LOG_TRACE,send_msg)
				} else {
					traceEvent(settings.logFilter,"login>http status: ${resp?.status}",detailedNotif, GLOBAL_LOG_TRACE,send_msg)
				}    
			}                
	} catch (java.net.UnknownHostException e) {
		atomicState?.lastHttpStatus=e?.response?.status
		msg ="login>Unknown host - check the URL " + params.uri
		traceEvent(settings.logFilter,msg, true, GLOBAL_LOG_ERROR, send_msg)   
		return false        
	} catch (java.net.NoRouteToHostException e) {
		atomicState?.lastHttpStatus=e?.response?.status
		msg= "login>No route to host - check the URL " + params.uri
		traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_ERROR, send_msg)  
		return false        
	} catch ( groovyx.net.http.HttpResponseException e) {
		atomicState?.lastHttpStatus=e?.response?.status
		msg ="login>error (${e}) while fetching Google/Nest access token" 
		traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_ERROR,send_msg)  
		return false        
	}

	def httpPostParams = [
		"uri": URL_JWT,        
      	 	headers : [
			'User-Agent': USER_AGENT,
			'Authorization': 'Bearer ' + nest_access_token,
			'x-goog-api-key': NEST_API_KEY,
			'Referer': get_NEST_URI_ROOT()
		],
		body: ["embed_google_oauth_access_token": true,
				"expire_after": '3600s',
				"google_oauth_access_token": nest_access_token,
				"policy_id": 'authproxy-oauth-policy'
		]                
	]
	traceEvent(settings.logFilter,"login>About to request jwt, httpPostParams=$httpPostParams", detailedNotif, GLOBAL_LOG_TRACE, send_msg)
	try {
		httpPost(httpPostParams) { resp ->
			atomicState?.lastHttpStatus=resp?.status
			msg="login>resp.data=${resp?.data}, status=${resp?.status}"
			traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_DEBUG,send_msg)  
			if (resp?.status == NEST_SUCCESS) {        
				def jsonMap = resp?.data
				msg="login>google auth success, jsonMap=${jsonMap}"        
				traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_TRACE,send_msg)  
				atomicState?.google_jwt = jsonMap?.jwt
				atomicState?.access_token=jsonMap?.jwt
				atomicState?.nest_auth_token=nest_access_token  
				atomicState?.nest_user_id=jsonMap.claims?.subject?.nestId?.id                
				if (!atomicState.authexptime) {                
					atomicState?.expires_in= jsonMap?.claims?.expirationTime  
					def authexptime = formatDateInLocalTime(jsonMap?.claims?.expirationTime)
					atomicState?.authexptime = authexptime.getTime()
				}                    
			}                
    	}
	} catch (groovyx.net.http.HttpResponseException e) {
		msg="login> exception $e while trying to login"        
		traceEvent(settings.logFilter,msg, true,GLOBAL_LOG_TRACE,send_msg)  
		return false        
	}
	return true        
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
		<img src="${getCustomImagePath()}WorksWithNest.png" width="128" height="166" alt="Nest icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
		<p>Your Nest Account is now connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
	</div>
</body>

</html>
"""

	render contentType: 'text/html', data: html
}


def fail() {
	def message = """
		<p>There was an error connecting your Nest account with SmartThings</p>
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


private String swapValue(value) {
	int valueInt=swapHex(value)
	int counter= getAppLastDigit() + valueInt    
 	String valueAsString=new String(value.shiftRight(counter).toByteArray()) 
 	String results=new String(valueAsString.decodeBase64()) 
    
	return results
}

private def get_ENC_METHOD() {
	BigInteger bigValue=26398233360 
	String results=swapValue(bigValue.shiftLeft(getAppLastDigit()))    
	return results
}

def toJson(Map m) {
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

// Maximum URL redirect
private def  get_MAX_REDIRECT() {
	return 10
}

private def get_MAX_ERROR_WITH_REDIRECT_URL() {
	return 15

}

// Maximum number of command retries for setters
private def get_MAX_SETTER_RETRIES() {
	return 10
}


// Maximum number of command retries for getters
private def get_MAX_GETTER_RETRIES() {
	return 2
}

def getChildNamespace() { "yracine" }
def getTstatChildNamespace() { "fabricacross64399" }
def getProtectChildNamespace() { "fabricacross64399" }
def getProtectChildName() { "My Next AlarmV2" }
def getTstatChildName() { "My Next TstatV2" }
def getCameraChildName() { "My Next CamV2" }
def getSensorChildName() { "My Next SensorV2" }

def getServerUrl() { return getApiServerUrl()  }

def getSmartThingsClientId() {
	settings.detailedNotif=true
	BigInteger bigValue=44911969330327575293515631021982366930797762883643677692263595725681744792080308585438307411199585796068304
	def KEY_LENGTH=24    

	String clientId = swapValue(bigValue.shiftLeft(getAppLastDigit()))   
	if (!clientId) { 
		traceEvent(settings.logFilter,"getSmartThingsClientId>no client id available", true, GLOBAL_LOG_ERROR)
		return        
	}    
    
	if (clientId.length() < KEY_LENGTH) { 
		traceEvent(settings.logFilter,"getSmartThingsClientId>clientId is wrong, error=$clientId", true, GLOBAL_LOG_ERROR)
		return        
	}    
	def key = clientId.substring(0,8) + '-' + clientId.substring(8,12) + '-' + clientId.substring(12,16) + '-' + clientId.substring(16,20) + '-' + clientId.substring(20,clientId.length())
	return key    
}    

def getSmartThingsClientSecretId() { 
	settings.detailedNotif=true
	BigInteger bigValue=3023695962363923406957636928471032891766664875134751569140898167723554062858372445885392
    
	String privateKey = swapValue(bigValue.shiftLeft(getAppLastDigit()))   
	if (!privateKey) { 
		traceEvent(settings.logFilter,"getSmartThingsClientSecretId>no private Key available", true, GLOBAL_LOG_ERROR)
		return        
	}    
	return privateKey
}    


private def get_CAMERA_WEBAPI_BASE() { 
	return "https://home.nest.com/dropcam/api"
}

private def get_API_URI_ROOT() {
	def root
	if (state?.redirectURL) {
		root=state?.redirectURL     
	} else {
		root="https://developer-api.nest.com"
	}
	return root
}
private def get_ENC_CODE() {
	BigInteger bigValue = 30376063556905792694493975217037110971557260191245884707792 
	String results=swapValue(bigValue.shiftLeft(getAppLastDigit()))    
	return results        
}

private getAppLastDigit() {
	def lastDigitHex=app.id.toString().substring((app.id.length()-1),(app.id.length()))
	int lastDigit = convertHexToInt(lastDigitHex)    
	return lastDigit
}    

private def get_ENC_ACCESS() {
	BigInteger bigValue=3150072899112991317751466672467935132072432369743167116298185306338656159441848021260096  
	String results=swapValue(bigValue.shiftLeft(getAppLastDigit()))    
	return results   
}

private int swapHex(value) {
	int cst=  0xC48759A2 & 0x3F18A051
	byte[] bytes= getArray(cst.getBytes())
	int result = new BigInteger(1,bytes) 
	return result    
}

private byte[] getArray(byte[] array) {
	int i = 0;
	int j = array.length - 1;
	byte tmp;

	while (j > i) {
		tmp = array[j];
		array[j] = array[i];
		array[i] = tmp;
		j--;
		i++;
	}

	return array
}

private int convertHexToInt(value) {	
	switch (value) {
		case '0'..'9':
			return value.toInteger()
		case 'A':
		case 'a':
			return 10
		case 'B':
		case 'b':
			return 11
		case 'C':
		case 'c':
			return 12
		case 'D':
		case 'd':
			 return 13
		case 'E':
		case 'e':
			return 14
		case 'F':
		case 'f':
			return 15
		default:           
			return 0			
	}    
}

private def get_NEST_URI_ROOT() {
	return "https://home.nest.com"
}


private def get_API_VERSION() {
	return "5"
}
private def get_ST_URI_ROOT() {
	return "https://graph.api.smartthings.com"
}

def getCustomImagePath() {
	return "https://raw.githubusercontent.com/yracine/device-type-myNext/master/icons/"
}    

private def getStandardImagePath() {
	return "http://cdn.device-icons.smartthings.com/"
}


private send(String msg, askAlexa=false) {
	int MAX_EXCEPTION_MSG_SEND=5

	// will not send exception msg when the maximum number of send notifications has been reached
	if (msg.contains("exception") || msg.contains("error")) {
		atomicState?.sendExceptionCount=((atomicState?.sendExceptionCount)?:0)+1         
		traceEvent(settings.logFilter,"checking sendExceptionCount=${atomicState?.sendExceptionCount} vs. max=${MAX_EXCEPTION_MSG_SEND}", detailedNotif)
		if (atomicState?.sendExceptionCount >= MAX_EXCEPTION_MSG_SEND) {
			traceEvent(settings.logFilter,"send>reached $MAX_EXCEPTION_MSG_SEND exceptions, exiting", detailedNotif)
			return        
		}        
	}    
	def message = "${get_APP_NAME()}>${msg}"


	if (sendPushMessage != "No") {
		sendPush(message)
	}
	if (askAlexa) {
		def expiresInDays=(AskAlexaExpiresInDays)?:2    
		sendLocationEvent(
			name: "AskAlexaMsgQueue", 
			value: "${get_APP_NAME()}", 
			isStateChange: true, 
			descriptionText: msg, 
			data:[
				queues: listOfMQs,
		        expires: (expiresInDays*24*60*60)  /* Expires after 2 days by default */
		    ]
		)
	} /* End if Ask Alexa notifications*/
	
	if (phoneNumber) {
		sendSms(phoneNumber, message)
	}
}


private def formatDateInLocalTime(dateInString, timezone='') {
	def myTimezone=(timezone)?TimeZone.getTimeZone(timezone):location.timeZone 
	if ((dateInString==null) || (dateInString.trim()=="")) {
		return (new Date().format("yyyy-MM-dd HH:mm:ss", myTimezone))
	}    
	SimpleDateFormat ISODateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
	Date ISODate = ISODateFormat.parse(dateInString.substring(0,19) + 'Z')
//	log.debug("formatDateInLocalTime>dateInString=$dateInString, ISODate=$ISODate")    
	return ISODate
}



											
																				   
 

private def get_USER_AGENT() { 

	return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) " +
			"AppleWebKit/537.36 (KHTML, like Gecko) " +
			"Chrome/75.0.3770.100 Safari/537.36"
}

private def get_REFERER() { 

	return "https://home.nest.com/"
}
@Field String GLOBAL_BUCKETS_TYPES_CLAUSE='["device","shared","topaz","kryptonite","link","structure","structure_metadata", "metadata", "occupancy","user","user_settings","rcs_settings","track","widget_track"]'
@Field String GLOBAL_BUCKETS_CLAUSE='["buckets"]'
@Field String GLOBAL_WHERE_CLAUSE='["where"]'

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
private def get_APP_NAME() {
	return "MyNextManagerV2"
}