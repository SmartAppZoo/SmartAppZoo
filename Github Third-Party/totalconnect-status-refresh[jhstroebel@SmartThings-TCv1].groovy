/**
 *  TotalConnect Status Refresh
 *
 *  Copyright 2017 Jeremy Stroebel
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
    name: "TotalConnect Status Refresh",
    namespace: "jhstroebel",
    author: "Jeremy Stroebel",
    description: "Updates TotalConnect Devices all at once",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/yogi/TotalConnect/150.png",
    iconX2Url: "https://s3.amazonaws.com/yogi/TotalConnect/300.png")

preferences {
    page(name: "copyConfig")
}

//When adding device groups, need to add here
//Stolen code from pdlove's JSON Complete API SmartApp & bdwilson's TotalConnectDevice Device Handler
def copyConfig() {
    dynamicPage(name: "copyConfig", title: "Configure Devices", install:true, uninstall:true) {
        section("Select devices to include in the /devices API call") {
            input "alarmList", "device.totalconnectDevice", title: "Alarm Panels", multiple: false, required: false
            input "deviceList", "device.totalconnectAutomationDevice", title: "Automation Devices", multiple: true, required: false
            input "sensorList", "device.totalconnectOpenCloseSensor", title: "Open/Close Sensors", multiple: true, required: false
            //paragraph "Alarms Selected: ${alarmList ? alarmList?.size() : 0}\nDevices Selected: ${deviceList ? deviceList?.size() : 0}\nSensors Selected: ${sensorList ? sensorList?.size() : 0}"
        }
        section("Polling Settings") {
        	input("pollingInterval", "number", title: "Polling Interval (in secs)", description: "How often the SmartApp will poll TC2.0")
            input("pollOn", "bool", title: "Polling On?", description: "Pause or Resume Polling")
        }
        section("TotalConnect 2.0 Settings") {
        	// See above ST thread above on how to configure the user/password.	 Make sure the usercode is configured
			// for whatever account you setup. That way, arming/disarming/etc can be done without passing a user code.
			input("userName", "text", title: "Username", description: "Your username for TotalConnect")
			input("password", "password", title: "Password", description: "Your Password for TotalConnect")
			// get this info by using https://github.com/mhatrey/TotalConnect/blob/master/TotalConnectTester.groovy 
			input("automationDeviceId", "text", title: "Automation Device ID - You'll have to look up", description: "Device ID")
            // get this info by using https://github.com/mhatrey/TotalConnect/blob/master/TotalConnectTester.groovy 
			input("securityDeviceId", "text", title: "Security Device ID - You'll have to look up", description: "Device ID")
			// get this info by using https://github.com/mhatrey/TotalConnect/blob/master/TotalConnectTester.groovy 
			input("locationId", "text", title: "Location ID - You'll have to look up", description: "Location ID")
			input("applicationId", "text", title: "Application ID - It is '14588' currently", description: "Application ID", defaultValue: "14588")
			input("applicationVersion", "text", title: "Application Version - use '3.0.32'", description: "Application Version", defaultValue: "3.0.32")
        }
        section() {
        	paragraph "Enter the name you would like shown in the smart app list"
        	label title:"SmartApp Label (optional)", required: false 
        }
    }
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

def initialize() {
    subscribe(app, appHandler)
  	autoUpdater()
    //runEvery1Minute(autoUpdater)
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers
def appHandler(evt) {
    log.debug "app event ${evt.name}:${evt.value} received"
    log.debug "Updating Statuses"
    updateStatuses()
}

def autoUpdater() {
	if(settings.pollOn) {
		log.debug "Auto Updating Statuses at ${new Date()}"
		updateStatuses()
    	runIn(settings.pollingInterval, autoUpdater)
    } else {
    	log.debug "Polling is turned off.  AutoUpdate canceled" }
}

// Login Function. Returns SessionID for rest of the functions
def login(token) {
	log.debug "Executed login"
	def paramsLogin = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/AuthenticateUserLogin",
		body: [userName: settings.userName, password: settings.password, ApplicationID: settings.applicationId, ApplicationVersion: settings.applicationVersion]
	]	
//Test Code
//	def responseLogin = post(paramsLogin)
    def responseLogin = tcCommand("AuthenticateUserLogin", [userName: settings.userName, password: settings.password, ApplicationID: settings.applicationId, ApplicationVersion: settings.applicationVersion])
    token = responseLogin?.SessionID 

/* Original Code
	httpPost(paramsLogin) { responseLogin ->
		token = responseLogin.data.SessionID 
	}
*/
	log.debug "Smart Things has logged In. SessionID: ${token}" 
	return token
} // Returns token		

// Logout Function. Called after every mutational command. Ensures the current user is always logged Out.
def logout(token) {
	log.debug "During logout - ${token}"
	def paramsLogout = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/Logout",
		body: [SessionID: token]
	]
	httpPost(paramsLogout) { responseLogout ->
		log.debug "Smart Things has successfully logged out"
	}  
}

// Gets Panel Metadata. Pulls Zone Data from same call (does not work in testing).  Takes token & location ID as an argument.
Map securityDeviceStatus(token, locationId) {
	String alarmCode
    String zoneID
    String zoneStatus
    def zoneMap = [:]

	def getPanelMetaDataAndFullStatusEx = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/GetPanelMetaDataAndFullStatusEx",
		body: [SessionID: token, LocationID: settings.locationId, LastSequenceNumber: 0, LastUpdatedTimestampTicks: 0, PartitionID: 1]
	]
	httpPost(getPanelMetaDataAndFullStatusEx) { responseSession -> 
                                 def data = responseSession.data.children()

                                 alarmCode = data.Partitions.PartitionInfo.ArmingState
                                 zoneMap.put("0", alarmCode) //Put alarm code in as zone 0
                                 
                                 data.Zones.ZoneInfo.each
        						 {
        						 	ZoneInfo ->
                                        zoneID = ZoneInfo.'@ZoneID'
                                        zoneStatus = ZoneInfo.'@ZoneStatus'
                                    	zoneMap.put(zoneID, zoneStatus)
        						 } 
    				}

	log.debug "ZoneNumber: ZoneStatus " + zoneMap
    return zoneMap
} //Should return zone information

Map zoneStatus(token, locationId) {
    String zoneID
    String zoneStatus
    def zoneMap = [:]
	
    //use Ex version to get if zone is bypassable
	def getZonesListInStateEx = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/GetZonesListInStateEx",
		body: [SessionID: token, LocationID: settings.locationId, PartitionID: 0, ListIdentifierID: 0]
	]
	httpPost(getZonesListInStateEx) { responseSession -> 
                                 def data = responseSession.data.children()
                                 
                                 data.Zones.ZoneStatusInfoEx.each
        						 {
        						 	ZoneStatusInfoEx ->
                                        zoneID = ZoneStatusInfoEx.'@ZoneID'
                                        zoneStatus = ZoneStatusInfoEx.'@ZoneStatus'
                                        //bypassable = ZoneStatusInfoEx.'@CanBeBypassed' //0 means no, 1 means yes
                                    	zoneMap.put(zoneID, zoneStatus)
        						 } 
    				}

	log.debug "ZoneNumber: ZoneStatus " + zoneMap
    return zoneMap
} //Should return zone information

// Gets Automation Device Status. Takes token & Automation Device ID as an argument
Map automationDeviceStatus(token, deviceId) {
	String switchID
	String switchState
    Map automationMap = [:]
   
	def responseSession = tcCommand("GetAllAutomationDeviceStatusEx", [SessionID: token, DeviceID: automationDeviceId, AdditionalInput: ''])
    
    responseSession.AutomationData.AutomationSwitch.SwitchInfo.each {
    	SwitchInfo ->
			switchID = SwitchInfo.SwitchID
			switchState = SwitchInfo.SwitchState
			automationMap.put(switchID,switchState)
	}//each SwitchInfo

/* Old Code
    def getAllAutomationDeviceStatusEx = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/GetAllAutomationDeviceStatusEx",
		body: [SessionID: token, DeviceID: automationDeviceId, AdditionalInput: '']
	]

	httpPost(getAllAutomationDeviceStatusEx) { responseSession ->
        responseSession.data.AutomationData.AutomationSwitch.SwitchInfo.each
        {
            SwitchInfo ->
        		switchID = SwitchInfo.SwitchID
                switchState = SwitchInfo.SwitchState
                automationMap.put(switchID,switchState)
        }
    }
*/ 

	log.debug "SwitchID: SwitchState " + automationMap

	return automationMap
} //Should return switch state information for all SwitchIDs

def updateStatuses() {
	def token = login(token)
	def locationId = settings.locationId
	def automationDeviceId = settings.automationDeviceId
    def securityDeviceId = settings.securityDeviceId
    
    def securityStatus
    def automationStatus
    
    if(settings.alarmList!=null || settings.sensorList!=null) {
    	securityStatus = securityDeviceStatus(token, locationId)
    }//Check for alarm device and/or sensor devices before making unnecessary call

	if(settings.deviceList!=null) {
    	automationStatus = automationDeviceStatus(token, automationDeviceID)
    }//Check for automation devices before making unnecessary call

    def deviceData = []
        alarmList.each { 
        	try {
                if(securityStatus.containsKey("0")) {
					def currentStatus = securityStatus.get("0")
                    log.debug "SmartThings Status is: " + it.currentStatus
                    
                    switch(currentStatus) {
                    	case "10200":
                        	log.debug "Polled Status is: Disarmed"
                            if(it.currentStatus != "Disarmed") {
                            	sendEvent(it, [name: "status", value: "Disarmed", displayed: "true", description: "Refresh: Alarm is Disarmed"]) }
                            break
                    	case "10203":
							log.debug "Polled Status is: Armed Stay"
                            if(it.currentStatus != "Armed Stay") {
								sendEvent(it, [name: "status", value: "Armed Stay", displayed: "true", description: "Refresh: Alarm is Armed Stay"]) }
                            break
                    	case "10201":
							log.debug "Polled Status is: Armed Away"
                            if(it.currentStatus != "Armed Away") {
                        		sendEvent(it, [name: "status", value: "Armed Away", displayed: "true", description: "Refresh: Alarm is Armed Away"]) }
                            break
                        default:
                        	log.debug "Alarm Status returned an irregular value " + currentStatus
                            break
                        }
						sendEvent(name: "refresh", value: "true", displayed: "true", description: "Alarm Refresh Successful") 
				}
				else {
					log.debug "Alarm Code does not exist"
				}
      		} catch (e) {
      			log.error("Error Occurred Updating Alarm "+it.displayName+", Error " + e)
      		}
        } 
        deviceList.each { 
        	try {
            	log.debug "(Switch) SmartThings State is: " + it.currentStatus
                String switchId = it.getDeviceNetworkId().substring(3)
                
                log.debug "SwitchId is " + switchId
                
                if(automationStatus.containsKey(switchId)) {
                	def switchState = automationStatus.get(switchId)
                    log.debug "(Switch) Polled State is: ${switchState}"
                    
                    if (switchState == "0") {
						log.debug "Status is: Closed"
						if(it.currentStatus != "Closed") {
	                    	sendEvent(it, [name: "status", value: "Closed", displayed: "true", description: "Refresh: Garage Door is Closed", isStateChange: "true"]) }
    				} else if (switchState == "1") {
						log.debug "Status is: Open"
						if(it.currentStatus != "Open") {
                           	sendEvent(it, [name: "status", value: "Open", displayed: "true", description: "Refresh: Garage Door is Open", isStateChange: "true"]) }
					} else {
    					log.error "Attempted to update switchState to ${switchState}. Only valid states are 0 or 1."
    				}
				}
				else {
					log.debug "SwitchId ${switchId} does not exist"
				}
      		} catch (e) {
      			log.error("Error Occurred Updating Device " + it.displayName + ", Error " + e)
      		}
        }    
        sensorList.each { 
        	try {
                String zoneId = it.getDeviceNetworkId().substring(3)
                String zoneName = it.getDisplayName()
                log.debug "Zone ${zoneId} - ${zoneName}"
                log.debug "(Sensor) SmartThings State is: " + it.currentContact
                
                if(securityStatus.containsKey(zoneId)) {
                   	String currentStatus = securityStatus.get(zoneId)
                	log.debug "(Sensor) Polled State is: " + currentStatus
                    
                    String statusString
                    
                	if (currentStatus == "0") {
    					log.debug "Zone ${zoneId} is OK"
						statusString = "closed"
                        //sendEvent(it, [name: "status", value: "closed", displayed: "true", description: "Refresh: Zone is closed", isStateChange: "true"])
    				} else if (currentStatus == "1") {
    					log.debug "Zone ${zoneId} is Bypassed"
                        statusString = "bypassed"
       					//sendEvent(it, [name: "contact", value: "bypassed", displayed: "true", description: "Refresh: Zone is bypassed", isStateChange: "true"])
 					} else if (currentStatus == "2") {
    					log.debug "Zone ${zoneId} is Faulted"
        				statusString = "open"
                        //sendEvent(it, [name: "contact", value: "open", displayed: "true", description: "Refresh: Zone is Faulted", isStateChange: "true"])
     				} else if (currentStatus == "8") {
    					log.debug "Zone ${zoneId} is Troubled"
        				statusString = "trouble"
                        //sendEvent(it, [name: "contact", value: "trouble", displayed: "true", description: "Refresh: Zone is Troubled", isStateChange: "true"])
     				} else if (currentStatus == "16") {
    					log.debug "Zone ${zoneId} is Tampered"
        				statusString = "tampered"
                        //sendEvent(it, [name: "contact", value: "tampered", displayed: "true", description: "Refresh: Zone is Tampered", isStateChange: "true"])
     				} else if (currentStatus == "32") {
    					log.debug "Zone ${zoneId} is Failed"
        				statusString = "failed"
                        //sendEvent(it, [name: "contact", value: "failed", displayed: "true", description: "Refresh: Zone is Failed", linkText: "Zone ${zoneId} - ${zoneName}", isStateChange: "true"])
	 				}
                	else {
                		log.error "Zone ${zoneId} returned an unexpected value.  ZoneStatus: ${currentStatus}"
                	}
                    
                    if(it.currentContact != statusString) {
                    	it.updateSensor(statusString)
                    }
                    else {
                    	log.debug "Sensor Status for Zone ${zoneId} has not changed from ${it.currentContact}"
                    }
/*              
                    if(it.status != currentStatus){
                       	it.updateStatus(currentStatus)
                    }//if status differs, update.  Else leave alone
*/
				}
				else {
					log.debug "ZoneId ${zoneId} does not exist"
				}
      		} catch (e) {
      			log.error("Error Occurred Updating Sensor "+it.displayName+", Error " + e)
      		}
        }
        log.debug "Finished Updating"
    return deviceData
}

def post(Map params) {
	def response
    try {
    	httpPost(params) { resp ->
        	response = resp.data
        }//Post Command
        
        state.tokenRefresh = now() //we ran a successful command, that will keep the token alive
    } catch (SocketTimeoutException e) {
        //identify a timeout and retry?
		log.error "Timeout Error: $e. Retrying."
        sendNotification("Timeout Error!", [method: "phone", phone: "3174027537"])
        response = post(params)
    } catch (e) {
    	log.error "Something went wrong: $e"
	}//try / catch for httpPost

    return response
}//post command to catch any issues and retry command?

def tcCommand(String path, Map body) {
	def response
	def params = [
		uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/",	
		path: path,
    	body: body
    ]

    try {
    	httpPost(params) { resp ->
        	response = resp.data
        }//Post Command
        
        state.tokenRefresh = now() //we ran a successful command, that will keep the token alive
    } catch (SocketTimeoutException e) {
        //identify a timeout and retry?
		log.error "Timeout Error in tcCommand: $e"
        sendNotification("Timeout Error in tcCommand", [method: "phone", phone: "3174027537"])
		response = post(params)
    } catch (e) {
    	log.error "Something went wrong in tcCommand: $e"
	}//try / catch for httpPost

    return response
}//post command to catch any issues and retry command?

