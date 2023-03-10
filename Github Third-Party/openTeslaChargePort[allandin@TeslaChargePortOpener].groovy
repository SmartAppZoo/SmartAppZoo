/**
 *  Open Tesla Charge Port
 *
 *  Copyright 2019 Allan Skipper
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
    name: "Tesla Charge Port Opener",
    namespace: "allanbrauer@gmail.com",
    author: "Allan Skipper",
    description: "Opens the chargeport of your Tesla, when contact sensor registers as open.",
    category: "My Apps",
    iconUrl: "https://img.icons8.com/color/48/000000/tesla-logo.png",
    iconX2Url: "https://img.icons8.com/color/48/000000/tesla-logo.png",
    iconX3Url: "https://img.icons8.com/color/48/000000/tesla-logo.png") {
    appSetting "TESLA_CLIENT_ID"
    appSetting "TESLA_CLIENT_SECRET"    
}

preferences {
    page(name: "settings")
}

def settings(){

	dynamicPage(name: "settings", title:"", install:true, uninstall:true){
    	section("Enter your Tesla credentials") {
            input "theusername", "text", title: "Username", submitOnChange:true, required:true
       	 	input "thepassword", "password", title: "Password", submitOnChange:true, required:true   
        }    
        
        
        if(theusername!=null && thepassword!=null){
            section("Select vehicle"){
                def vehicleId = getVehicles()
                input(name: "vehicle", type: "enum", title: "Vehicle", options: vehicleId[0], required:true)
            }
         	
       	}
        section("Select contact sensor") {
            input "contactsensor", "capability.contactSensor", required:true, title: "Choose contact sensor"
           
    	}        
    	section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false 
        }
    
    }  
}    
  
def getVehicles(){
    //Authenticate first
    def vehicle_token = get_access_token()

    //Then fetch vehicles
    def vehicleId = []    
    def vehicleParams = [
    	uri: "https://owner-api.teslamotors.com/api/1/vehicles",
        headers: [
              'Authorization': "Bearer "+vehicle_token,
              ]
    ]
	try {
    	httpGet(vehicleParams) { resp ->
    		vehicleId << resp.data.response.id_s
    }
	} catch (e) {
    	log.error "Vehicles (settings > getVehicles) could not be fetched: $e"
	}
    return vehicleId
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
	subscribe(contactsensor, "contact.open", contactOpenHandler)
    
}

def get_access_token(){
    def access_token
   	def params = [
    uri: "https://owner-api.teslamotors.com/oauth/token?grant_type=password",
    body: [
        grant_type: "password",
        client_id: appSettings.TESLA_CLIENT_ID,
        client_secret: appSettings.TESLA_CLIENT_SECRET,
        email: theusername,
        password: thepassword,
    	]
	]
    log.debug "Value of params is: $params"
	try {
    	httpPostJson(params) { resp ->
        	access_token = resp.data.access_token
    	}
	} catch (e) {
		log.debug "Access_token could not be fetched. Reason: $e"
    }
    return access_token
}

def contactOpenHandler(evt){
		//Authenticate first
    	def access_token_contact_open = get_access_token()
        
        //Awake vehicle
    	def awakeVehicleParams = [
        	uri: "https://owner-api.teslamotors.com/api/1/vehicles/$vehicle/wake_up",
            headers: [
              	'Authorization': "Bearer "+access_token_contact_open,
              	]
        ]
        try {
    		httpPost(awakeVehicleParams) { resp ->
            
            
        			
            				sendPush("Car status: ${resp.data.response.state}")
            	
            
            	}
            } catch (e) {
    		log.error "Awake command failed: $e"
		}
    
    	//Then open charge port
    	def openChargePortParams = [
    		uri: "https://owner-api.teslamotors.com/api/1/vehicles/$vehicle/command/charge_port_door_open",
        	headers: [
              	'Authorization': "Bearer "+access_token_contact_open,
              	]
    	]
		try {
    		httpPost(openChargePortParams) { resp ->
        		if(resp.data.response.reason == "could_not_wake_buses"){
        			
            				sendPush("Failed: could_not_wake_buses")
            	}
                
        		if(resp.data.response.result){
        			sendPush("The charge port has been opened.")
        		} 
                      
        		else{
        			sendPush("The charge port could not be opened. Reason: ${resp.data.response.reason}.")
        		} 
    		}
		} catch (e) {
    		log.error "Chargeport open command failed: $e"
		}
    
}
