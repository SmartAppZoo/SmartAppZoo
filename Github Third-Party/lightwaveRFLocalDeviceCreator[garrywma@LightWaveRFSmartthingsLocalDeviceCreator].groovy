/**
 *  lightwaveRFLocalDeviceCreator for Adam Clark's LightWaveRF Local API Node JS server and devices
 *
 *  Copyright 2021 Garry Whittaker
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
    name: "lightwaveRFLocalDeviceCreator",
    namespace: "smartthings-users",
    author: "Garry Whittaker",
    description: "Creates lightwaverf devices for local use based on data from lightwaverf web portal - requires https://github.com/adamclark-dev/smartthings-lightwave-node-server to be installed",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("LightWaveRF Login Settings") {
	    input "username", "text", title: "LightWaveRF Connect Username", required: true
            input "password", "password", title: "LightWaveRF Connect Password", required: true
            paragraph "Notes: This does not support smart series devices as they do not use the local api. It also does not support legacy logins with a pin number unless that pin has been used to log in to manager.lightwaverf.com."
	    paragraph "Important: all three of the device handlers from Adam Clarke's LightwaverRF NodeJS project need to be installed prior to installing this Smart App. Also please note that uninstalling this smart app will delete all created devices. Reinstalling will re-create the devices."
    }
    section("Server Address Settings") {
            input("serverIP", "text", title: "Server IP Address", description: "IP Address of the Node JS Server")
            input("lightwaveIP", "text", title: "Lightwave IP Address", description: "IP Address of the Lightwave Hub")
 	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def uninstalled() {
    removeChildDevices(getChildDevices(true))  
    unsubscribe()
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    removeChildDevices(getChildDevices(true))  
    try {
	getUser()
        getAuth()
        getProfile()
    	} catch (e) {
            log.error "error: $e"
        }    
}
def getUser()
{
   def userParams = [
            uri:  'https://control-api.lightwaverf.com/v1/',
            path: 'user',
            contentType: 'application/json',
            query: [username:settings.username, password: settings.password]
        ]
        log.debug "user ${settings.username}"
        try {
            	httpGet(userParams){ resp ->
                state.key = resp.data.application_key
            }     
        } catch (e) {
            log.error "user error: ${e}"
        }        
 }

def getAuth()
{
  
   def authParams = [
            uri:  'https://control-api.lightwaverf.com/v1/',
            path: 'auth',
            contentType: 'application/json',
            query: [application_key: state.key, do_command: false]
        ]
        try {
            httpGet(authParams){ resp ->
                state.token = resp.data.token
                }
        } catch (e) {
            log.error "auth error: ${e}"
        }        
 }

def getProfile()
{
    def deviceHandlers=['Lightwave On Off Device',  'Lightwave Dimmer Switch', 'Lightwave Inline Relay Device']
    def profileParams = [
   	    uri:  'https://control-api.lightwaverf.com/v1/',
            path: 'user_profile',
            contentType: 'application/json',
            query: [nested: '1', exclude_heating_plans: '1'],
            headers: ['x-lwrf-token': state.token, 'x-lwrf-platform': 'android']
            ]
        try {
             httpGet(profileParams){ resp ->
                resp.data.content.estates.each { estate ->
                    estate.locations.each { lwLocation ->
                    lwLocation.zones.each {zone ->
                      	def dev=""
         		zone.rooms.each {room ->
                		room.devices.each { device ->
					dev  += "\nRoom ${room.room_number}: ${room.name} Device ${device.device_number}: ${device.name} ${device.device_type_id} ${device.device_type_name}"
					if (device.device_type_id >0 && device.device_type_id <4)
					{
						dev += " adding ${deviceHandlers[device.device_type_id - 1]}"
						def networkID="LWLOCAL${room.room_number}-${device.device_number}"

						def preferenceParams = [
							serverIP: settings.serverIP,
							lightwaveIP: settings.lightwaveIP,
							roomID: room.room_number,
							deviceID: device.device_number
						]


						def deviceParams = [
							label: "${room.name} ${device.name}",
							name: "${room.name} ${device.name}",
							preferences: preferenceParams,
							completedSetup: true
						]


						addChildDevice(deviceHandlers[device.device_type_id - 1], networkID , location.hubs[0].id, deviceParams)
					}
                        	}
                       }
                       log.info "${dev}"
                     }	
                  }
                }
	     }   
            
        } catch (e) {
            log.error "profile error: ${e}"
        }  
}
