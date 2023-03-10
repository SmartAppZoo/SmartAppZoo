/**
 *  Lyric
 *
 *  Copyright 2017 Joshua Spain
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
    name: "Tank Utility (Connect)",
    namespace: "joshs85",
    author: "Josh S",
    description: "Virtual device handler for Tank Utility Propane tank monitor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true
    )

preferences {
    page(name: "settings", title: "Settings", content: "settingsPage", install:true)
    }

private static String TankUtilAPIEndPoint() { return "https://data.tankutility.com" }
private static String TankUtilityDataEndPoint() { return "https://data.tankutility.com" }
private static getChildName() { return "Tank Utility" }

def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def uninstalled() {
    def delete = getAllChildDevices()
    delete.each { deleteChildDevice(it.deviceNetworkId) }
}

def initialize() {
	log.debug "Entering the initialize method"
    def devs = getDevices()
    def devicestatus = RefreshDeviceStatus()
	devs.each { dev ->
    	def ChildName = getChildName()
        def TUDeviceID = dev
    	def dni = getDeviceDNI(TUDeviceID)
        def devinfo = devicestatus[TUDeviceID]
		def d = getChildDevice(dni)
		if(!d) {
			d = addChildDevice(app.namespace, ChildName, dni, null, ["label": devinfo.name ?: ChildName])
			log.info "created ${d.displayName} with dni: ${dni}"
		} else {
			log.info "device for ${d.displayName} with dni ${dni} already exists"
		}
		return d
	}
    try{
    	pollChildren()
    }
    catch (e)
    {
    	log.debug "Error with initial polling: $e"
    }
    
	runEvery1Hour("pollChildren")
}

private settingsPage(){
    return dynamicPage(name: "settings", title: "Settings", nextPage: "", uninstall:true, install:true) {
               	def message = getAPIToken()
                if(!(message == true)){
                    section("Authentication") {
                    	paragraph "${message}. Enter your TankUtility Username and Password."
                        input "UserName", "string", title: "Tank Utility Username", required: true
                        input "Password", "string", title: "Tank Utility Password", required: true, submitOnChange: true
                    }
                }
                else
                {
                    section("Authentication") {
                        paragraph "Authentication Succeeded!"
                    }
                }
            }
}

private getDevices() {
		log.debug "Enter getDevices"
        def Params = [
        	uri: TankUtilityDataEndPoint(),
            path: "/api/devices",
            query: [
                token: atomicState.APIToken,
            ],
        ]
        
        def devices = []
        try {
            httpGet(Params) { resp ->
                if(resp.status == 200)
                {	
                    resp.data.devices.each { dev ->
                    try{
                        def dni = [app.id, dev].join('.')
                        log.debug "Found device ID: ${dni}"
                        devices += dev
                        }
                     catch (e) {
                        log.error "Error in getDevices: $e"
                     }
				}
                }
                else
                {
                	log.error "Error from Tank Utility in getDevices - Return Code: ${resp.status} | Response: ${resp.data}"
                }
            }
            atomicState.devices = devices
          }
        catch (e) {
            log.error "Error in getDevices: $e"
        }
        return devices
}

private RefreshDeviceStatus() {
		log.info "starting RefreshDeviceStatus()"
        def devices = atomicState.devices
        def deviceData = [:]
		devices.each {dev ->
            def dni = getDeviceDNI(dev)
            def Params = [
                uri: TankUtilityDataEndPoint(),
                path: "/api/devices/${dev}",
                query: [
                    token: atomicState.APIToken,
                ],
            ]
            httpGet(Params) { resp ->
            try{
                deviceData[dev] = resp.data.device
                log.debug "device data for ${dev} = ${deviceData[dev]}"
                }
                catch (e)
                {
                	log.error "Error while processing events for pollChildren: ${e}"
				}
            }
    }
    atomicState.deviceData = deviceData
    return deviceData
}

private String getBase64AuthString() {
    String authorize = "${settings.UserName}:${settings.Password}"
    String authorize_encoded = authorize.bytes.encodeBase64()
    return authorize_encoded
}

private getAPIToken() {
	log.debug "Entering getAPIToken()"
    log.info "Requesting an API Token!"
    def Params = [
        uri: TankUtilAPIEndPoint(),
        path: "/api/getToken",
        headers: ['Authorization': "Basic ${getBase64AuthString()}"],
    ]

    try {
        httpGet(Params) { resp ->
            log.debug "getToken Return Code: ${resp.status} Response: ${resp.data}"
            if(resp.status == 200)
            {
                if (resp.data.token) {
                    atomicState.APIToken = resp?.data?.token
                    atomicState.APITokenExpirationTime = now() + (24 * 60 * 60 * 1000)
                    log.info "Token refresh Success.  Token expires at ${atomicState.APITokenExpirationTime}"
                    return true
                }
                else
                {
                    return resp.data.error
                }
            }
            else
            {
                return resp.data.error
            }
        }
    }
    catch (e) {
        log.error "Error in the getAPIToken method: $e"
    }
}

private isTokenExpired() 
{
	def currentDate = now()
    if (atomicState.APITokenExpirationTime == null)
    {
    	return true
    }
    else
    {
    	def ExpirationDate = atomicState.APITokenExpirationTime
        if (currentDate >= ExpirationDate) {return true} else {return false}
    }
}

def pollChildren(){
		log.info "starting pollChildren"
        if (isTokenExpired()) {
        	log.info "API token expired at ${atomicState.APITokenExpirationTime}.  Refreshing API Token"
        	getAPIToken()
        }
        def devices = atomicState.devices
        def deviceData = RefreshDeviceStatus()
		devices.each {dev ->
        try{
            def deviceid = dev
            def devData = deviceData[deviceid]
            def LastReading = devData.lastReading
            def dni = getDeviceDNI(deviceid)
            def d = getChildDevice(dni)
            def temperature = LastReading.temperature.toInteger()
            def level = (LastReading.tank).toFloat()
            def lastReadTime = LastReading.time_iso
            def capacity = devData.capacity
            def events = [
                ['temperature': temperature],
                ['level': level],
                ['energy': level],
                ['capacity': capacity],
            	['lastreading': lastReadTime],
            ]
            log.info "Sending events: ${events}"
            events.each {event -> d.generateEvent(event)}
            log.debug "device data for ${deviceid} = ${devData}"
        }
        catch (e)
        {
            log.error "Error while processing events for pollChildren: ${e}"
        }
	}
}

def getDeviceDNI(DeviceID) {
	return [app.id, DeviceID].join('.')
}