/**
 *  SimpliSafe Control
 *
 *  Copyright 2015 Felix Gorodishter
 *	Modifications by Toby Harris - 2/10/2018
 *  Modifications by Scott Silence - 2/6/2020
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License. 
 *
 *  Monitors and controls the state of a SimpliSafe alarm system, syncs with Smart Home Monitor and can turn on/off switches based on SimpliSafe state.
 *  Works in conjunction with SimpliSafe Alarm Integration device type.
 */

import groovy.transform.Field

String getVersionNum() 		{ return "0.0.1" }
String getVersionLabel() 	{ return "Simplisafe Control, version ${getVersionNum()}" }
boolean setStateOnSS() 		{ return true }

// Automatically generated. Make future change here.
definition(
    name: "SimpliSafe Control",
    namespace: "ssilence5",
    author: "Scott Silence",
    description: "Monitors and controls the state of a SimpliSafe alarm system.",
    category: "Safety & Security",
    iconUrl: "https://pbs.twimg.com/profile_images/594250179215241217/LOjVA4Yf.jpg",
    iconX2Url: "https://pbs.twimg.com/profile_images/594250179215241217/LOjVA4Yf.jpg",
    singleInstance: true)

preferences {
	page(name: "mainPage")
    page(name: "prefAdvancedOptions")
}

/**************** START OF PAGES **************************/
def mainPage() {
	state.clear()
    def showUninstall = username != null && password != null
  	return dynamicPage(name: "mainPage", title: "Setup Simplisafe Alarm", nextPage: "prefAdvancedOptions", uninstall: showUninstall, install: false) {
    	section("Login Credentials") {
      		input(name: "username", type: "email", title: "Username/Email", required: true, description: "SimpliSafe Username")
	  		input(name: "password", type: "password", title: "Password", required: true, description: "SimpliSafe Password")
			input(name: "ssVersion", type: "enum", title: "SimpliSafe Version", required: true, multiple: false, description: "Alarm system version", options: [[1: "1"], [2:"2"], [3:"3"]])
    	}
  	}
}

def prefAdvancedOptions() {
	if(checkApiAuth()) {
    	//apiLogout()
        //Clear all state variables first        
        return dynamicPage(name: "prefAdvancedOptions", title: "Setup Simplisafe Alarm", nextPage: "", uninstall: true, install: true) {            
            section("How Frequently do you want to poll Simplisafe for changes?"){
                input(name: "pollingInterval", title: "Polling Interval (in Minutes)", type: "enum", required:false, multiple:false, defaultValue:"5", description: "5", options:[[1:"1"], [2:"2"], [3:"3"], [5:"5"], [10:"10"], [15:"15"], [30:"30"]])
            }
            section("Would you like for Simplisafe State changes to update the Location Mode?") {
           		input(name: "syncWithMode", title: "Sync with Location Modes?", type: "bool", required: false, description: "", submitOnChange: true)        
                if(settings.syncWithMode == true) {
            		input(name: "locModeOff", type: "mode", title: "What Location Mode should be set when Simplisafe is Off?", required: false, multiple: false)
                    input(name: "locModeHome", type: "mode", title: "What Location Mode should be set when Simplisafe is Home?", required: false, multiple: false)
                    input(name: "locModeAway", type: "mode", title: "What Location Mode should be set when Simplisafe is Away?", required: false, multiple: false)
				}
        	}
            section("Select the debug logging level (higher levels send more information to IDE Live Logging). A setting of 2 is recommended for normal operations") {
				input(name: "debugLevel", title:"Debug Logging Level", type: "enum", required:false, multiple:false, defaultValue:"2", description: "2", options:[[5:"5"], [4:"4"], [3:"3"], [2:"2"], [1:"1"], [0:"0"]])			  
			}           
        }
    } else {
        return dynamicPage(name: "prefAdvancedOptions", title: "Error!", install: false, uninstall: true) {
          section(""){ paragraph "The username or password you entered is incorrect. Try again." }
    	}
  	}
}

/**************** END OF PAGES **************************/

/******** START OF SMARTTHINGS STATE CHANGE FUNCTIONS ******************/

def installed() {
	LOG("Installed with settings: ${settings}",2,null,'trace')	
  	init(true)
}

def updated() {
    LOG("Updated with settings: ${settings}",2,null,'trace')	
    unsubscribe()
    //unschedule()
    init(false)
}

def uninstalled() {
  	LOG("Uninstalling...",2,null,'warn')
  	unschedule()
    
    //Remove all children
    removeChildDevices(getAllChildDevices())
}

def init(installing) {
	LOG("init() Start - installing: ${installing}", 4, null, 'info')
    
    //Unsubscribe and Unschedule all
    try {
		unsubscribe()
		unschedule() // reset all the schedules
	} catch (Exception e) {
		LOG("init() - Exception encountered trying to unschedule(). Exception: ${e}", 1, null, "error")
	}	 
    
    def nowTime = now()
	def nowDate = getTimestamp()
    
    //Initialize variables/states
	state.ssVersion = getSSVersion()
    state.pollingInterval = getPollingInterval()
    state.lastScheduledPoll = nowTime
	state.lastScheduledPollDate = nowDate
    state.lastPoll = nowTime
	state.lastPollDate = nowDate
    
    //Mode information
    state.modeSync = settings.syncWithMode
    state.modeSyncOff = locModeOff
    state.modeSyncHome = locModeHome
    state.modeSyncAway = locModeAway
    
    //If we are installing (not just updating), then create a new Child
    if(installing) {
    	addChildDevice("ssilence5", "SimpliSafe Alarm", UUID.randomUUID().toString(), location.hubs[0]?.id, ["label": "Simplisafe - ${state.locationName ? state.locationName : 'Home'}", completedSetup: true])
    }
          
	//subscribe(alarmsystem, "alarm", alarmstate)
    //subscribe(alarmsystem, "status", alarmstate)
   	//subscribe(alarmsystem, "presence", alarmstate)
    //subscribe(location, "alarmSystemStatus", shmaction)
    
    subscribe(location, "alarmSystemStatus", shmHandler)

	//log("End initialization().", "DEBUG")
    
    //Poll everything
    poll()
    
	//Setup initial polling and determine polling intervals, and setup schedule
    Integer pollingInterval = getPollingInterval()
    LOG("installing() - Using runEvery to setup polling with pollingInterval: ${pollingInterval}", 4, null, 'trace')
    "runEvery${pollingInterval}Minute${pollingInterval!=1?'s':''}"("pollScheduled")   
    
    LOG("installing() - Current State: ${state}",5,null,'info')
    LOG("installing() - Current Settings: ${settings}",5,null,'info')
}
/******** END OF SMARTTHINGS STATE CHANGE FUNCTIONS ******************/

def removeChildDevices(devices) {
	LOG("removeChildDevices(${devices})",4,null,'info')
    def devName
    try {
    	devices?.each {
        	LOG("Removing Child: ${it}",4,null,'info')
            devName = it.displayName
    		deleteChildDevice(it.deviceNetworkId)
   		}
  	} catch (Exception e) {
    	LOG("Error ${e} removing device ${devName}",1,null,'warn')
    }
}

/*def updatestate() {
	LOG("updatestate()
	log.info("Checking SimpliSafe and Smart Home Monitor state")
	tempState = alarmsystem.currentState("alarm").value.toLowerCase()
	log.debug("SimpliSafe updatestate: '$state.alarmstate'")   
	
}*/

void updateLastPoll(Boolean isScheduled=false) {
	if (isScheduled) {
		state.lastScheduledPoll = now()
		state.lastScheduledPollDate = getTimestamp()
	} else {
		state.lastPoll = now()
		state.lastPollDate = getTimestamp()
	}
}

def poll() {
	//This will poll everything (all children and there settings)
	LOG("poll() - Running at ${getTimestamp()} (epic: ${now()})", 1, null, "trace")
    
    //Poll Children Devices
    def devices = getChildDevices()
	devices.each { child ->
		pollChild(child) //parse received message from parent
	}
}

// Called by scheduled() event handler
def pollScheduled() {
	updateLastPoll(true)
	LOG("pollScheduled() - Running at ${state.lastScheduledPollDate} (epic: ${state.lastScheduledPoll})", 3, null, "trace")	  
	return poll()
}

def pollChild(child) {
	LOG("pollChild() Started - child: ${child.device.label}",4,null,'info')

	//Check Auth first
	checkApiAuth()
    def tempAlarmStateTimeStamp = 0
    def result = false;
    
	httpGet ([uri: getAPIUrl("refresh"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { response ->
        //Looking good so far
      	result = true
        LOG("pollChild() - Refresh Succeed", 2, null, 'info')
        LOG("====>Response: ${response.data}", 4, null, 'trace')
        
        //Set some default values at the start
        def pollTemp = null //Initiall set to null  
        def pollMessage = "none"
        def pollCarbonMonoxide = "clear"
        def pollSmoke = "clear"
        def pollWater = "dry"
        
		//Check alarm state
        state.alarmState = response.data.subscription.location.system.alarmState.toLowerCase()
        setLocMode(state.alarmState)
        def pollStatus = state.alarmState
        def pollAlaram = state.alarmState
        
        //Get Alarm State TimeStamp
        //tempAlarmStateTimeStamp = resp.data.subscription.location.system.alarmStateTimestamp
		
		//Check temperature              
		if (response.data.subscription.location.system.temperature != null) {
        	pollTemp = response.data.subscription.location.system.temperature
			//log.info "Temperature: $response.data.subscription.location.system.temperature"
		}       
		
		//Check messages
       	if (state.ssVersion == 3) {
        	//log.info "All Messages: ${response.data.subscription.location.system.messages}"
            if (response.data.subscription.location.system.messages[0] != null) {
            	pollStatus = "alert"
                pollMessage = response.data.subscription.location.system.messages[0].text
                //log.info "Messages: ${response.data.subscription.location.system.messages[0].text}"
				
				//Check for alerts
				if (response.data.subscription.location.system.messages[0].category == "alarm") {
                	pollStatus = "alarm"
                    //log.info "Message category: ${response.data.subscription.location.system.messages[0].category}"
				
					//Carbon Monoxide sensor alerts
					if (response.data.subscription.location.system.messages[0].data.sensorType == "C0 Detector") {
                        pollCarbonMonoxide = "detected"
                        pollStatus = "carbonMonoxide"
                        //log.info "Message sensor: ${response.data.subscription.location.system.messages[0].data.sensorType}"
					}
					
					//Smoke sensor alerts
					if (response.data.subscription.location.system.messages[0].data.sensorType == "Smoke Detector") {
                    	pollSmoke = "detected"
                        pollStatus = "smoke"
                        //log.info "Message sensor: ${response.data.subscription.location.system.messages[0].data.sensorType}"
					}

					//Water sensor alerts
					if (response.data.subscription.location.system.messages[0].data.sensorType == "Water Sensor") {
                    	pollWater = "wet"
                        pollStatus = "water"
                        //log.info "Message sensor: ${response.data.subscription.location.system.messages[0].data.sensorType}"
					}
				}	
            }
      	}
        
        //Let's send the results
        def updates = [status: pollStatus,
					   alarm: pollAlaram,
					   temp: pollTemp,
                       message: pollMessage,
                       carbonMonoxide: pollCarbonMonoxide,
                       smoke: pollSmoke,
                       water: pollWater]
        child.generateEvent(updates)
    }
	
    //Check events
    httpGet ([uri: getAPIUrl("events"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { response ->
        LOG("pollChild() - Events Succeed", 2, null, 'info')
        LOG("====>Response: ${response.data}", 4, null, 'trace')
        if (response.data.events[0] != null) {
            def pollEvent = response.data.events[0].messageBody
            //log.info "Events: ${response.data.events[0].messageBody}"
            
                    //Let's send the results
        	def updates = [event: pollEvent]
            child.generateEvent(updates)
        }
    }	
    
    //Check for setting changes
    httpGet ([uri: getAPIUrl("settingsSystem"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { response ->
        LOG("pollChild() - System Settings Succeed", 2, null, 'info')
        LOG("====>Response: ${response.data}", 4, null, 'trace')
        /*if (response.data.events[0] != null) {
            def pollEvent = response.data.events[0].messageBody
            //log.info "Events: ${response.data.events[0].messageBody}"
            
                    //Let's send the results
        	def updates = [event: pollEvent]
            child.generateEvent(updates)
        }*/
    }	
    
    //Need to update SHM State
	
    //updateModeandPresence()

    //log.info "Alarm State2: $response"
    //apiLogout()
}

def setState(child, newState) {
	LOG("setState() Entered for Child: ${child.device.label} - New State: ${newState}",2,null,'info')
    
    def bResp = false
    
    if (state.alarmState == null ||
    	state.alarmState.toLowerCase() != newState.toLowerCase()) {
    	switch (newState.toLowerCase()) {
        	case "off":
            	//log.info("Settings state to OFF")
                bResp = setAlarmOff()
                state.alarmState = "off"              
                //sendLocationEvent(name: "alarmSystemStatus" , value : "off" )
            	break;
            case "home":
            	//log.info("Settings state to HOME")
                bResp = setAlarmHome()
                state.alarmState = "home"
                //sendLocationEvent(name: "alarmSystemStatus" , value : "stay" )
            	break;
            case "away":
            	//log.info("Settings state to AWAY")
                bResp = setAlarmAway()
                state.alarmState = "away"
                //sendLocationEvent(name: "alarmSystemStatus" , value : "away" )
            	break;
          	default:
            	LOG("setState() - Invalid State: ${newState}",1,null,'warn')
                break;
        }
    }    
}

def shmHandler(evt) {
	def shmState = evt.value
    
    LOG("shmHandler(evt) Entered with Evt: ${shmState}",1,null,'warn')
  	LOG("shmHandler(evt) - SHM alarm state: ${location.currentState("alarmSystemStatus")?.value}",1,null,'warn')
    
    if (shmState != null) {
        def devices = getChildDevices()
        devices.each { child ->
        	//First let's poll the child, just to verify current state
            pollChild(child)
            //child.
            switch(shmState.toLowerCase()) {
            	case "off":
                	LOG("Setting to Off",1,null,'info')
                	child.off()
                    break
                case "away":
                	LOG("Setting to Away",1,null,'info')
                	child.away()
                    break
                case "stay":
                	//if (child.
                	LOG("Setting to Home",1,null,'info')
                	child.home()
                    break
                default:
                	LOG("Setting to Invalid State - ${shmState.toLowerCase()}",1,null,'info')
                	return
            }
        }
    }
}

/*************** START OF ALARM STATE CONTROL ********************/
def setAlarmOff() {
	//log.debug("setAlarmOff()")
	
    //def message = "Setting SimpliSafe to Off"
    //log.info(message)
    //sendMessage(message)
    
    //Update the location mode
    //setLocMode("Off")
    
   	if (setStateOnSS()) {
        try {
            httpPost([ uri: getAPIUrl("alarmOff"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8" ])
            //If we succeeded we can immediately poll
			poll()
        } catch (e) {
        	LOG("Error ${e} setting Alarm to Off",1,null,'warn')
            //There was a timeout, so we can't poll right away. Wait 10 seconds and try polling.
            runIn(10, poll)
            return false;
        } 
    } else {
    	//Simulating calling AlarmAway
       	String path = getAPIUrl("alarmOff")
        log.info("Path: ${path} - SSVersion: ${state.ssVersion}")
        poll()
    }
    return true;
}
  
def setAlarmAway() {
	//log.debug("setAlarmAway()")
    
    //def message = "Setting SimpliSafe to Away"
    //log.info(message)
    //sendMessage(message)
    
    //Update the location mode
    //setLocMode("Away")
    
    //If udpating on SS, then post the request
    if (setStateOnSS()) {
        try {
            httpPost([ uri: getAPIUrl("alarmAway"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8" ])
            //If we succeeded we can immediately poll, however, since this is away, we will setup a poll after the timeout            
            poll()
            runIn(50, poll)
        } catch (e) {
            LOG("Error ${e} setting Alarm to Away",1,null,'warn')
            //There was a timeout, so we can't poll right away. Wait 10 seconds and try polling.
            runIn(10, poll)
            return false;
        }
    } else {
    	//Simulating calling AlarmAway
       	String path = getAPIUrl("alarmAway")
        log.info("Path: ${path} - SSVersion: ${state.ssVersion}")
        poll()
    }
    return true;
}
  
def setAlarmHome() {
	log.debug("setAlarmHome()")
    
    //def message = "Setting SimpliSafe to Home"
    //log.info(message)
    //sendMessage(message)
    
    //setLocMode("Home")
    
    //If udpating on SS, then post the request
    if (setStateOnSS()) {
        try {
        	httpPost([uri: getAPIUrl("alarmHome"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8" ])
            //If we succeeded we can immediately poll
            poll()
        } catch (e) {
            LOG("Error ${e} setting Alarm to Home",1,null,'warn')
            //There was a timeout, so we can't poll right away. Wait 10 seconds and try polling.
            runIn(10, poll)
            return false;
        }
    } else {
    	//Simulating calling AlarmAway
       	String path = getAPIUrl("alarmHome")
        log.info("Path: ${path} - SSVersion: ${state.ssVersion}")
        poll()
    }
    return true;
}

def setLocMode(modeToSetTo) {
	//log.info("Location: Current Mode: ${location.mode} -- New Mode: ${modeToSetTo}")
    //log.info("State - ${state}")
    
    //If not syncing with Modes, return
    if (state.modeSync == false) { return }
    
	//Get current mode as starting point
    def newMode = location.mode
    switch(modeToSetTo.toLowerCase()) {
    	case "off":
        	newMode = state.modeSyncOff
        	break;
        case "away":
        	newMode = state.modeSyncAway
        	break;
        case "home":
        	newMode = state.modeSyncHome
        	break;
        default:
        	break;
    }
    
	if (newMode != null && location.mode != newMode) {
        if (location.modes?.find {it.name == newMode}) {
            location.setMode(newMode)
            return true
        } else {
        	//Couldn't find the mode, so return false
            return false
        }
    }
    return true
}

/*************** END OF ALARM STATE CONTROL ********************/

/* TODO : Add Switch Control Capability
def alarmstateon() {
    log.debug ("Setting switches to on")
      alarmtile?.on()
  }
  
def alarmstateoff() {
    log.debug ("Setting switches to off")
      alarmtile?.off()
} */

/* TODO : Old Alarm Functionality and SHM
// TODO - centralize somehow
private getalarmOff() {
	def result = false
	if (state.alarmstate == "off") {
	result = true }
	log.trace "alarmOff = $result"
	result
}

private getalarmAway() {
	def result = false
	if (state.alarmstate == "away") {
	result = true }
	log.trace "alarmAway = $result"
	result
}

private getalarmHome() {
	def result = false
	if (state.alarmstate == "home") {
	result = true }
	log.trace "alarmHome = $result"
	result
}

private getshmOff() {
	def result = false
	if (state.shmstate == "off") {
	result = true }
	log.trace "shmOff = $result"
	result
}

private getshmAway() {
	def result = false
	if (state.shmstate == "away") {
	result = true }
	log.trace "shmAway = $result"
	result
}

private getshmStay() {
	def result = false
	if (state.shmstate == "stay") {
	result = true }
	log.trace "shmStay = $result"
	result
}*/
  
private sendMessage(msg) {
	Map options = [:]
    
    log.info "PushMessage: $sendPushMessage"
    log.info "Settings: $settings"
    
	if (location.contactBookEnabled) {
    	if (recipients) {
        	log.debug 'Contact Book Enabled, Sending to Recipients'
			sendNotificationToContacts(msg, recipients, options)
        } else {
        	log.debug 'Contact Book Enabled, No Recipients Selected, Nothing Sent'
        }
	} else {
    	if (phone) {
        	options.phone = phone
			if (sendPushMessage && sendPushMessage != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
        } else if (sendPushMessage && sendPushMessage != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)        
	}
}

// need these next 5 get routines because settings variable defaults aren't set unless the "Preferences" page is actually opened/rendered
Integer getPollingInterval() {
	return (settings?.pollingInterval?.isNumber() ? settings.pollingInterval as Integer : 5)
}

Integer getTempDecimals() {
	return ( settings?.tempDecimals?.isNumber() ? settings.tempDecimals as Integer : (wantMetric() ? 1 : 0))
}

Integer getSSVersion() {
	return ( settings?.ssVersion?.isNumber() ? settings.ssVersion as Integer : 3)
}

Integer getDebugLevel() {
	return ( settings?.debugLevel?.isNumber() ? settings.debugLevel as Integer : 2)
}

//Additional Run Every handlers
void runEvery2Minutes(handler) {
	Random rand = new Random()
	//log.debug "Random2: ${rand}"
	int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/2 * * * ?", handler)
}

void runEvery3Minutes(handler) {
	Random rand = new Random()
	//log.debug "Random3: ${rand}"
	int randomSeconds = rand.nextInt(59)
	schedule("${randomSeconds} 0/3 * * * ?", handler)
}

/************ Begin Logging Methods *******************************************************/

void LOG(message, level=3, child=null, String logType='debug', event=false, displayEvent=true) {
	// boolean dbgLevel = debugLevel(level)
	if (!debugLevel(level)) return		// let's not waste CPU cycles if we don't have to...
	
	if (logType == null) logType = 'debug'
	String prefix = ""
	// is now a Field def logTypes = ['error', 'debug', 'info', 'trace', 'warn']
	
	if(!LogTypes.contains(logType)) {
		//log.error "LOG() - Received logType (${logType}) which is not in the list of allowed types ${LogTypes}, message: ${message}, level: ${level}"
		//if (event && child) { debugEventFromParent(child, "LOG() - Invalid logType ${logType}") }
		logType = 'debug'
	}
	
	if ( logType == 'error' ) { 
		state.lastLOGerror = "${message} @ ${getTimestamp()}"
		state.LastLOGerrorDate = getTimestamp()		 
	}
    
	// if ( debugLevel(0) ) { return }
	if ( debugLevel(5) ) { prefix = 'LOG: ' }
	log."${logType}" "${prefix}${message}"		  
	if (event) { debugEvent(message, displayEvent) }
	if (child) { debugEventFromParent(child, message) }  
}

void debugEvent(message, displayEvent = false) {
	def results = [
		name: 'appdebug',
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { LOG("Generating AppDebug Event: ${results}", 3, null, 'debug') }
	//sendEvent (results)
}

void debugEventFromParent(child, message) {
	 def data = [
				debugEventFromParent: message
			]		  
	//if (child) { child.generateEvent(data) }
}

boolean debugLevel(level=3) {
	Integer dbgLevel = getDebugLevel()
	return (dbgLevel == 0) ? false : (dbgLevel >= level?.toInteger())
}

String getTimestamp() {
	// There seems to be some possibility that the timeZone will not be returned and will cause a NULL Pointer Exception
	def timeZone = location?.timeZone ? location.timeZone : ""
	// LOG("timeZone found as ${timeZone}", 5)
	if (timeZone == "") {
		return new Date().format("yyyy-MM-dd HH:mm:ss z")
	} else {
		return new Date().format("yyyy-MM-dd HH:mm:ss z", timeZone)
	}
}

/************ End Logging Methods *********************************************************/

/************ START Api Calls ************************/

def apiLogout() {
    httpDelete([ uri: getAPIUrl("initAuth"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8" ]) { resp ->
        if (resp.status == 200) {
            state.sid = null
            state.uid = null
            log.info "Logged out from API."
        }
    }
}

boolean apiLogin() {
	//Login to the system
    LOG("apiLogin() entered", 1, null, "info")
   
   	//Define the login Auth Body and Header Information
    def authBody = [ "grant_type":"password",
                    "username":settings.username,
                    "password": settings.password ]                    
    def authHeader = [ "Authorization":"Basic NGRmNTU2MjctNDZiMi00ZTJjLTg2NmItMTUyMWIzOTVkZWQyLjEtMC0wLldlYkFwcC5zaW1wbGlzYWZlLmNvbTo="	]
    
    try {
        httpPost([ uri: getAPIUrl("initAuth"), headers: authHeader, contentType: "application/json; charset=utf-8", body: authBody ]) { resp ->
            state.auth = resp.data            
            state.auth.respAuthHeader = ["Authorization":resp.data.token_type + " " + resp.data.access_token]
            state.auth.tokenExpiry = now() + (resp.data.expires_in * 1000)
            LOG("apiLogin() - initAuth success with Response1: ${resp.data}", 3, null, "info")
        }
 	} catch (e) {
    	LOG("apiLogin() - General Exception: ${e}", 1, null, "error")
        return false
    }
    
    //Check for valid UID, and if not get it
    if (!state.uid) {
    	getApiUserId()
   	}
    
    //Check for valid Subscription ID, and if not get it
    //Might be able to expand this to multiple systems
    if (!state.sid) {
    	getApiSubId()
    }
}

def getApiUserId() {
	LOG("getApiUserId() entered", 1, null, "info")
	//check auth and get uid    
    httpGet ([uri: getAPIUrl("authCheck"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { resp ->
        state.uid = resp.data.userId
        LOG("getApiUserId() - success with Response1: ${resp.data}", 4, null, "info")
    }
    LOG("getApiUserId() - User ID: ${state.uid}", 2, null, "info")
}

def getApiSubId() {
	//get subscription id
    LOG("getApiSubId() entered", 1, null, "info")
    httpGet ([uri: getAPIUrl("subId"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { resp ->
    	state.sid = resp.data.subscriptions[0].location.sid
        state.locationName = resp.data.subscriptions[0].location.locationName.toString()
        //Integer temp = resp.data.subscriptions?.size() ?: 0
		//state.sid = tsid.substring(1, tsid.length() - 1)
        //LOG("SID: ${resp.data.subscriptions.location.sid.toString()} -- TSID: ${tsid}", 1, null, "info")
        
        //LOG("${temp}", 1, null, "info")        
        LOG("getApiSubId() - success with Response1: ${resp.data}", 4, null, "info")
    }
    
    LOG("getApiSubID() - Subscription ID: ${state.sid}", 2, null, "info")
    LOG("getApiSubID() - Location Name: ${state.locationName}", 2, null, "info")
}

boolean checkApiAuth() {
	LOG("checkApiAuth() entered", 1, null, "info")
        
    //If no State Auth, or now Token Expiry, or time has expired, need to relogin
    if (!state.auth || !state.auth.tokenExpiry || now() > state.auth.tokenExpiry) {    
    	LOG("checkApiAuth() Token Time has Expired, executing login", 2, null, "warn")
        apiLogin()
    }
    
	//Check Auth
    try {
        httpGet ([uri: getAPIUrl("authCheck"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { response ->
			LOG("checkApiAuth() - authCheck Successful on first attempt", 3, null, "info")
            return true       
        }
    } catch (e) {
    	LOG("checkApiAuth() - General Exception on authCheck: ${e}", 1, null, "error")
        //state.auth.clear()
        if (apiLogin()) {
            httpGet ([uri: getAPIUrl("authCheck"), headers: state.auth.respAuthHeader, contentType: "application/json; charset=utf-8"]) { response ->
				LOG("checkApiAuth() - authCheck Successful after Login", 3, null, "info")
				return true
            }
    	} else {
        	return false
        }          
	}
}

def getAPIUrl(urlType) {
	//Login to the system
    LOG("getAPIUrl() entered with urlType: ${state.sid}", 1, null, "info")

	//def baseUrl = "scottsilence-eval-test.apigee.net"
    def baseUrl = "api.simplisafe.com"
    
	if (urlType == "initAuth") {
    	return "https://${baseUrl}/v1/api/token"
    }
    else if (urlType == "authCheck") {
    	return "https://${baseUrl}/v1/api/authCheck"
    }
    else if (urlType == "subId" ) {
    	return "https://${baseUrl}/v1/users/$state.uid/subscriptions?activeOnly=false"
    }
    else if (urlType == "sensors") {
    	if (state.ssVersion == 3) {
    		return "https://${baseUrl}/v1/ss3/subscriptions/$state.sid/sensors?forceUpdate=false"
        } else {
        	//Not sure what this is or if it is available
            return null
        } 
    }
    else if (urlType == "settingsSystem") {
    	if (state.ssVersion == 3) {
    		return "https://${baseUrl}/v1/ss3/subscriptions/$state.sid/settings/normal?forceUpdate=false"
        } else {
        	//Not sure what this is or if it is available
            return null
        } 
    }
    else if (urlType == "alarmOff" ) {
    	if (state.ssVersion == 3) {
    		return "https://${baseUrl}/v1/ss3/subscriptions/$state.sid/state/off"
        } else {
        	return "https://${baseUrl}/v1/subscriptions/$state.sid/state?state=off"
        }       
    }
    else if (urlType == "alarmHome" ) {
   		if (state.ssVersion == 3) {
    		return "https://${baseUrl}/v1/ss3/subscriptions/$state.sid/state/home"
        } else {
        	return "https://${baseUrl}/v1/subscriptions/$state.sid/state?state=home"
        }
    }
    else if (urlType == "alarmAway" ) {
   		if (state.ssVersion == 3) {
    		return "https://${baseUrl}/v1/ss3/subscriptions/$state.sid/state/away"
        } else {
        	return "https://${baseUrl}/v1/subscriptions/$state.sid/state?state=away"
        }
    }
    else if (urlType == "refresh") {
    	return "https://${baseUrl}/v1/subscriptions/$state.sid/"
    }
    else if (urlType == "events") {
    	return "https://${baseUrl}/v1/subscriptions/$state.sid/events?numEvents=1"
    }
    else {
    	log.info "Invalid URL type"
        return null
    }
}

/************ END Api Calls **************************/

@Field final List LogTypes = 			['error', 'debug', 'info', 'trace', 'warn']