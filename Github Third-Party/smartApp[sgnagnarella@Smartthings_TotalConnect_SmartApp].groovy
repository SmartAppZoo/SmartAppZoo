/**
 *  TotalConnect Location and Device Details
 *
 *  Copyright 2015 Yogesh Mhatre, Brian Wilson, Sebastian Gnagnarella
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

/** 
 * Most of this is borrowed from: https://github.com/mhatrey/TotalConnect/blob/master/TotalConnectTester.groovy
 * Goal if this is to return your Location ID and Device ID to use with my Total Connect Device located here:
 *  https://github.com/bdwilson/SmartThings-TotalConnect-Device
 *
 * To install, go to the IDE: https://graph.api.smartthings.com/ide/app/create,
 * Create a new SmartApp from Code, Save, Publish, Install at your location and
 * enter your credentials for your TotalConnect account. 
 */


definition(
   	 	name: "TotalConnect Location and Device Details using Cloud Node App as proxy",
    	namespace: "sgnagnarella",
    	author: "Sebastian Gnagnarella",
    	description: "Total Connect App to show you your Location and Device ID's for use with Total Connect Device",
    	category: "My Apps",
   	 	iconUrl: "https://s3.amazonaws.com/yogi/TotalConnect/150.png",
    	iconX2Url: "https://s3.amazonaws.com/yogi/TotalConnect/300.png"
)

preferences {
        section ("Switch Function: ON = Arm"){
        	input("switch1", "capability.switch", multiple: false, require: true)
    		input("userName", "text", title: "Username", description: "Your username for TotalConnect")
    		input("password", "password", title: "Password", description: "Your Password for TotalConnect")
            input("applicationId", "applicationId", title: "ApplicationId", description: "Usually 14588")
            input("applicationVersion", "applicationVersion", title: "ApplicationVersion", description: "Usually 3.0.32")
	        input("appServerBaseUrl", "appServerBaseUrl", title: "AppServerBaseUrl", description: "https://xxxxxxxx.herokuapp.com")

    }
}

// End of Page Functions

def installed(){
	getDetails()
}
def updated(){
	unsubscribe()
    getDetails()
}

// Login Function. Returns SessionID for rest of the functions
def login(token) {
    log.debug "===== Executed login ====="

    def paramsLogin = [
        uri: settings.appServerBaseUrl + "/Login",
        body: [
            username: settings.userName,
            password: settings.password,
            ApplicationID: settings.applicationId,
            ApplicationVersion: settings.applicationVersion
        ]
    ]

    httpPostJson(paramsLogin) { resp ->
    	def data = resp.data
        log.debug(data)
        def results = resp.data
        token = results.AuthenticateLoginResults.SessionID
    }
    
    log.debug "Smart Things has logged In. SessionID: ${token}" 
    return token
}       // returns token

def logout(token) {
		log.debug "During logout - ${token}"
        def paramsLogout = [
            uri: settings.appServerBaseUrl + "/Logout",
            body: [
                SessionID: token,
            ]
        ]

    httpPostJson(paramsLogout) 
} //Takes token as arguement

// Get LocationID & DeviceID map
Map getSessionDetails(token) {

    log.debug "===== Executed getSessionDetails ====="
    
	def applicationId="14588"
	def applicationVersion="1.0.34"
	def locationId
    def deviceId
    def locationName
    Map locationMap = [:]
    Map deviceMap = [:]

    def getSessionParams = [
        uri: settings.appServerBaseUrl + "/GetSessionDetails",
        body: [
            SessionID: token,
            ApplicationID: settings.applicationId,
            ApplicationVersion: settings.applicationVersion
        ]
    ]

    httpPostJson(getSessionParams) { resp ->
        def results = resp.data
        
        results.SessionDetailResults.Locations[0].LocationInfoBasic.each
        {
            LocationInfoBasic ->
            locationName = LocationInfoBasic.LocationName
            locationId = LocationInfoBasic.LocationID
            deviceId = LocationInfoBasic.DeviceList.DeviceInfoBasic.DeviceID
            locationMap["${locationName}"] = "${locationId}"
            deviceMap["${locationName}"] = "${deviceId}"
        }   
    }

	log.debug "Location ID map is " + locationMap + " & Device ID map is " + deviceMap
  	return [locationMap: locationMap, deviceMap: deviceMap]
} // Should return Map of Locations

def getDetails() {
    def token = login(token)
    def details = getSessionDetails(token) // Get Map of Location
   logout(token)
}