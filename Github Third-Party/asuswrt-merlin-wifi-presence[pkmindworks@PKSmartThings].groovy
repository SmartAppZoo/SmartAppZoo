/**
 *  Copyright 2015 Stuart Buchanan
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
 *	AsusWRT Wifi Presence
 *
 *	Author: Stuart Buchanan, Based on original work by midyear66 with thanks
 *
 *	Date: 2016-02-01 v1.0 Initial Release
 */
definition(
    name: "AsusWRT-Merlin Wifi Presence",
    namespace: "pkmindworks",
    author: "Stuart Buchanan",
    description: "Triggers Wifi Presence Status when HTTP GET Request is recieved",
    category: "My Apps",
    iconUrl: "http://icons.iconarchive.com/icons/icons8/windows-8/256/Network-Wifi-icon.png",
    iconX2Url: "https://www.asus.com/media/img/2017/images/n-logo-asus.svg")


preferences {
    section(title: "Select Devices") {
        input "virtualPresenceDevices", "capability.presenceSensor", title: "Select Virtual Presence Sensors", required: true, multiple:true
    }
    
}

def installed() {
	createAccessToken()
	getToken()
	DEBUG("Installed App with rest api: $app.id")
    DEBUG("Installed App with token: $state.accessToken")
    DEBUG("Installed with settings: $virtualPresenceDevices")
}
def updated() {
	DEBUG("Updated App with rest api: $app.id")
    DEBUG("Updated App with token: $state.accessToken")
}


mappings {
    path("/phone/:device/:command/") {
        action: [GET: "updateSensors"]
						  
	 
    }
    path("/phone/") {
    	action: [GET: "getDevices"]
							
	 
    }
}


// Callback functions
def getDevices() {
    // This returns the details of the Presence sensors in JSON
    return virtualPresenceDevices
}

					 
						 
									  
 

def updateSensors() {

	def deviceId = params.device
    def cmd = params.command
    log.debug "device: $deviceId, command: $cmd"
    switch(cmd) {
        case "home":
        	virtualPresenceDevices.each{device ->
            	if (device.name == deviceId) {
                	device.arrived()
                }
            }
            // handle on command
            break
        case "away":
            // handle off command
        	virtualPresenceDevices.each{device ->
            	if (device.name == deviceId) {
                	device.departed()
                }
            }
            break
        default:
            httpError(501, "$command is not a valid command for switches specified")
    }
}

def getToken(){
if (!state.accessToken) {
		try {
			getAccessToken()
			DEBUG("Creating new Access Token: $state.accessToken")
		} catch (ex) {
			DEBUG("Did you forget to enable OAuth in SmartApp IDE settings")
            DEBUG(ex)
		}
	}
}

private def DEBUG(message){
	log.debug message
}
