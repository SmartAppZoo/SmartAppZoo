/**
 *  iAquaLink
 *
 *  Copyright 2018 Chris Kooken
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
    name: "iAquaLink",
    namespace: "iAqualink",
    author: "Chris Kooken",
    description: "Controls for iAquaLink pool systems",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "cloudLogin", title: "iAqualink Cloud Login", nextPage:"", content:"cloudLogin", uninstall: true);
   	page(name: "selectDevices", title: "Select iAqualink Devices", nextPage:"", content:"selectDevices", uninstall: true, install: true)

	//section("Title") {
	//	// TODO: put inputs here
	//}
}

//	----- LOGIN PAGE -----
def cloudLogin() {
	def cloudLoginText = "If possible, open the IDE and select Live Logging.  THEN, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete.  Your current token:\n\r\n\r${state.TpLinkToken}" +
		"\n\r\n\rAvailable actions:\n\r" +
		"	Initial Install: Obtains token and adds devices.\n\r" +
		"	Add Devices: Only add devices.\n\r" +
		"	Update Token:  Updates the token.\n\r"
	def errorMsg = ""
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r\n\r${state.currentError}" +
			"\n\r\n\rPlease resolve the error and try again.\n\r\n\r"
		}
	return dynamicPage(
		name: "cloudLogin", 
		title: "iAqualink Device Service Manager", 
		nextPage: "selectDevices", 
		uninstall: true) {
		section(errorMsg)
		section(cloudLoginText) {
			input( 
				"userName", "string", 
				title:"Your iAqualink Username (Email)", 
				required:true, 
				displayDuringSetup: true
			)
			input(
				"userPassword", "password", 
				title:"Your iAqualink Password", 
				required: true, 
				displayDuringSetup: true
			)
			input(
				"updateToken", "enum",
				title: "What do you want to do?",
				required: true, 
				multiple: false,
				options: ["Initial Install", "Add Devices"]
			)
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	def errorMsg = ""
	def TPLinkDevicesMsg = "test"
   
    def newDevices = getDeviceStates();
     newDevices = [:]
     
	return dynamicPage(
		name: "selectDevices", 
		title: "Select Your iAqualink Devices", 
		install: true,
		uninstall: true) {
		section(errorMsg)
		section(TPLinkDevicesMsg) {
			input "selectedDevices", "enum",
			required:false, 
			multiple:true, 
			title: "Select Devices (${newDevices.size() ?: 0} found)",
			options: newDevices
		}
	}
}

def getDeviceStates(){

 def getHomeDevicesParams = [
     uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command=get_home&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO",
     requestContentType: 'application/json',
     contentType: 'application/json',
     headers: ['Accept':'application/json; version=1, */*; q=0.01']		
 ]
 httpGet(getHomeDevicesParams){resp ->
     def devices = [:];
     devices["message"] = resp.data.message;
     devices["status"] = resp.data.home_screen.status[0]
     devices["spa_temp"] = resp.data.home_screen.spa_temp[4]
     devices["pool_temp"] = resp.data.home_screen.pool_temp[5]
     devices["air_temp"] = resp.data.home_screen.air_temp[6]
     devices["spa_set_point"] = resp.data.home_screen.spa_set_point[7]
     devices["pool_set_point"] = resp.data.home_screen.pool_set_point[8]     
     devices["freeze_protection"] =resp.data.home_screen.freeze_protection[10]
     devices["spa_pump"] = resp.data.home_screen.spa_pump[11]
     devices["pool_pump"] = resp.data.home_screen.pool_pump[12]
     devices["spa_heater"] = resp.data.home_screen.spa_heater[13]
     devices["pool_heater"] = resp.data.home_screen.pool_heater[14]
     devices["solar_heater"] = resp.data.home_screen.solar_heater[15]
     
     
      def getDevicesParams = [
		uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command=get_devices&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO",
		requestContentType: 'application/json',
		contentType: 'application/json', 
		headers: ['Accept':'application/json; version=1, */*; q=0.01']		
	]
    
    httpGet(getDevicesParams){devicesResp ->
    	//log.debug(devicesResp.data)
        devices["aux_1"] = (((devicesResp.data.devices_screen.aux_1-null)[0]).state-null)[0];
        devices["aux_2"] = (((devicesResp.data.devices_screen.aux_2-null)[0]).state-null)[0];
        devices["aux_3"] = (((devicesResp.data.devices_screen.aux_3-null)[0]).state-null)[0];
        devices["aux_4"] = (((devicesResp.data.devices_screen.aux_4-null)[0]).state-null)[0];
        devices["aux_5"] = (((devicesResp.data.devices_screen.aux_5-null)[0]).state-null)[0];
        devices["aux_6"] = (((devicesResp.data.devices_screen.aux_6-null)[0]).state-null)[0];
        devices["aux_7"] = (((devicesResp.data.devices_screen.aux_7-null)[0]).state-null)[0];
        
    }
     
     log.debug(devices);
     
     return devices;
 }

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

	unsubscribe()
	unschedule()
	runEvery1Minute(refreshDevices)
	//addChildDevice("iAqualink","Aqualink Relay", "pool_heater", null, ["label": "Pool Heater", "name": "Pool Heater", "data": ["deviceId" : "pool_heater", "type": "main"]]);	
	//addChildDevice("iAqualink","Aqualink Relay", "spa_heater", null, ["label": "Spa Heater", "name": "Spa Heater", "data": ["deviceId" : "spa_heater", "type": "main"]]);	
	//addChildDevice("iAqualink","Hot Tub Controller", "hot_tub", null,  ["label": "Hot Tub", "name": "Hot Tub", "data": ["deviceId" : "hot_tub", "type": "tub"]]);
	//addChildDevice("iAqualink","Aqualink Relay", "spa_pump", null, ["label": "Spa Pump", "name": "Spa Pump", "data": ["deviceId" : "spa_pump", "type": "main"]]);
    //addChildDevice("iAqualink","Aqualink Relay", "pool_pump", null, ["label": "Pool Pump", "name": "Pool Pump", "data": ["deviceId" : "pool_pump", "type": "main"]]);
    //addChildDevice("iAqualink","Aqualink Relay", "aux_2", null, ["label": "Pool Light", "name": "Pool Light", "data": ["deviceId" : "aux_2", "type": "aux"]]);
    //addChildDevice("iAqualink","Aqualink Relay", "aux_4", null, ["label": "Spa Jets", "name": "Spa Jets", "data": ["deviceId" : "aux_4", "type": "aux"]]);
    //addChildDevice("iAqualink","Aqualink Relay", "aux_6", null, ["label": "Deck Lights", "name": "Deck Lights", "data": ["deviceId" : "aux_6", "type": "aux"]]);
    //addChildDevice("iAqualink","Aqualink Relay", "aux_7", null, ["label": "Wall Lights", "name": "Wall Lights", "data": ["deviceId" : "aux_7", "type": "aux"]]);
     // addChildDevice("iAqualink","Aqualink Relay", "aux_5", null, ["label": "Spa Light", "name": "Spa Light", "data": ["deviceId" : "aux_5", "type": "aux"]]);	
}

def refreshDevices(){
	log.debug("refreshing devices")
    def devices = getChildDevices();
    devices.each{device ->
    	log.debug("refreshing: " + device.name)
    	device.refresh()
    }
}

def setSpaTemp(newval){
	def getHomeDevicesParams = [
	     uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command=set_temps&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO&temp1="+newval,
	     requestContentType: 'application/json',
	     contentType: 'application/json',
	     headers: ['Accept':'application/json; version=1, */*; q=0.01']		
	 ]
	 httpGet(getHomeDevicesParams){resp ->
	     def devices = [:];
	     devices["message"] = resp.data.message;
	     devices["status"] = resp.data.home_screen.status[0]
	     devices["spa_temp"] = resp.data.home_screen.spa_temp[4]
	     devices["pool_temp"] = resp.data.home_screen.pool_temp[5]
	     devices["air_temp"] = resp.data.home_screen.air_temp[6]
	     devices["spa_set_point"] = resp.data.home_screen.spa_set_point[7]
	     devices["pool_set_point"] = resp.data.home_screen.pool_set_point[8]     
	     devices["freeze_protection"] =resp.data.home_screen.freeze_protection[10]
	     devices["spa_pump"] = resp.data.home_screen.spa_pump[11]
	     devices["pool_pump"] = resp.data.home_screen.pool_pump[12]
	     devices["spa_heater"] = resp.data.home_screen.spa_heater[13]
	     devices["pool_heater"] = resp.data.home_screen.pool_heater[14]
	     devices["solar_heater"] = resp.data.home_screen.solar_heater[15]
	
	}
}



def execCommand(deviceId, newState){
	def allDevices = getDeviceStates();
    def currentDeviceState = allDevices[deviceId];
    
    log.debug("States" + newState + " " + currentDeviceState);
    if (newState.toInteger() == currentDeviceState.toInteger())
    {
    	log.debug("State of " + deviceId + " is already set to " + newState);
        return;
    }
    
	def commandParams = [
		uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command=set_"+deviceId+"&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO",
		requestContentType: 'application/json',
		contentType: 'application/json', 
		headers: ['Accept':'application/json; version=1, */*; q=0.01']		
	]
    httpGet(commandParams){resp ->
    	log.debug(resp.data) 
        return resp.data.message;
    }
}

//*******************************
//***** New Code ****************
//*******************************

def processCommandAndGetDeviceInfo(homeCommand, deviceCommand, queryData){

 def getHomeDevicesParams = [
     uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command="+homeCommand+"&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO"+ queryData,
     requestContentType: 'application/json',
     contentType: 'application/json',
     headers: ['Accept':'application/json; version=1, */*; q=0.01']		
 ]
 httpGet(getHomeDevicesParams){resp ->
     def devices = [:];
     devices["message"] = resp.data.message;
     devices["status"] = resp.data.home_screen.status[0]
     devices["spa_temp"] = resp.data.home_screen.spa_temp[4]
     devices["pool_temp"] = resp.data.home_screen.pool_temp[5]
     devices["air_temp"] = resp.data.home_screen.air_temp[6]
     devices["spa_set_point"] = resp.data.home_screen.spa_set_point[7]
     devices["pool_set_point"] = resp.data.home_screen.pool_set_point[8]     
     devices["freeze_protection"] =resp.data.home_screen.freeze_protection[10]
     devices["spa_pump"] = resp.data.home_screen.spa_pump[11]
     devices["pool_pump"] = resp.data.home_screen.pool_pump[12]
     devices["spa_heater"] = resp.data.home_screen.spa_heater[13]
     devices["pool_heater"] = resp.data.home_screen.pool_heater[14]
     devices["solar_heater"] = resp.data.home_screen.solar_heater[15]
     
     
      def getDevicesParams = [
		uri: "https://iaqualink-api.realtime.io/v1/mobile/session.json?actionID=command&command="+deviceCommand+"&serial=QA33KCEM3SZU&sessionID=JNFEG4JMKW2W61F0EHLB1ROU1S24B8OO",
		requestContentType: 'application/json',
		contentType: 'application/json', 
		headers: ['Accept':'application/json; version=1, */*; q=0.01']		
	]
    
    httpGet(getDevicesParams){devicesResp ->
    	//log.debug(devicesResp.data)
        devices["aux_1"] = (((devicesResp.data.devices_screen.aux_1-null)[0]).state-null)[0];
        devices["aux_2"] = (((devicesResp.data.devices_screen.aux_2-null)[0]).state-null)[0];
        devices["aux_3"] = (((devicesResp.data.devices_screen.aux_3-null)[0]).state-null)[0];
        devices["aux_4"] = (((devicesResp.data.devices_screen.aux_4-null)[0]).state-null)[0];
        devices["aux_5"] = (((devicesResp.data.devices_screen.aux_5-null)[0]).state-null)[0];
        devices["aux_6"] = (((devicesResp.data.devices_screen.aux_6-null)[0]).state-null)[0];
        devices["aux_7"] = (((devicesResp.data.devices_screen.aux_7-null)[0]).state-null)[0];
        
    }
     
     log.debug(devices);
     
     return devices;
 }

}
