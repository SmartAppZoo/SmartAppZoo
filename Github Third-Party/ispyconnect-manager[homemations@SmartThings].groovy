/** 
*  iSpyConnect Manager by Stephen Harris (stephen@homemations.com) 
*   
*  Copyright 2016 Homemations, Inc 
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
	name: "iSpyConnect Manager",
	namespace: "Homemations",
	author: "Stephen Harris",
	description: "Control security cameras via the iSpyConnect Server Platform",
    category: "Safety & Security", 
    iconUrl: "https://s3-us-west-1.amazonaws.com/iriesolutions/SmartThings/icons/iSpy/iSpy-icn.png", 
    iconX2Url: "https://s3-us-west-1.amazonaws.com/iriesolutions/SmartThings/icons/iSpy/iSpy-icn@2x.png", 
    iconX3Url: "https://s3-us-west-1.amazonaws.com/iriesolutions/SmartThings/icons/iSpy/iSpy-icn@3x.png", 
    singleInstance: true
) 

preferences {	
    page(name: "preferenceAccessCredentials", title: "iSpyConnect Configuration")    
    page(name: "preferenceCredentialsValidation", title: "iSpyConnect Configuration")    
    page(name: "preferenceCamerasConfiguration", title: "iSpyConnect Configuration")  
}

// Dynamic Preferences to support configuration validation section
def preferenceAccessCredentials() {
    log.debug "Entered Method: preferenceAccessCredentials()"
    
    def showUninstall = (apiKey != null && accessToken != null)
	return dynamicPage(name: "preferenceAccessCredentials", title: "Connect to the iSpyConnect Platform", nextPage:"preferenceCredentialsValidation", uninstall: showUninstall) {
		section("Platform Credentials") {
        	input("apiKey", "string", title:"iSpyConnect API Key", description: "Please enter the API Key provided", required: true, displayDuringSetup: true) 
    		input("accessToken", "string", title:"iSpyConnect Access Token", description: "Please enter the Access Token provided", required: true, displayDuringSetup: true) 
		}
        
        section("Platform Polling"){
			input("polling", "int", title: "Polling Interval (Minutes)", description: "The interval to poll the platform for changes", defaultValue: 5)
		}

		section("Push Notifications") {
        	input "prefrencePushAlerts", "bool", required: false, title: "Push notifications when cameras alert?"
    	}
	}
}

def preferenceCredentialsValidation() {
	log.debug "Entered Method: preferenceCredentialsValidation()"
    
    // Initialize to capture any connection errors to display to end user
    def pageNext = null
    def pageTitle = null
    def pageSection = null
    def pageParagraph = null

	try {
		httpGet(
        	[	uri:			getiSpyUri(),
				query: 			[token: accessToken, apiKey: apiKey],
				path:			getiSpyPath()			
			], {
                response ->
                if (response.status == 200) {
                    log.debug "Response: ${response.data}, Response Array Size: ${response.data.size()}"

                    if (response.data.configuration?.connectedServers?.name != null && response.data.size() >= 1) {
                        // Build map for Server configuration data
                        def server = [
                            name:			response.data.configuration.connectedServers.name.get(0),
                            wan:	 		response.data.configuration.connectedServers.serverIP.get(0) + ":" + response.data.configuration.connectedServers.wanPort.get(0),
                            lan:			response.data.configuration.connectedServers.localIP.get(0) + ":" + response.data.configuration.connectedServers.lanPort.get(0),
                            overrideURL:	response.data.configuration.connectedServers.overrideURL.get(0),
                            connected:		response.data.configuration.connectedServers.connected.get(0),
                            loopback:		response.data.configuration.connectedServers.loopback.get(0),
                            auth:			response.data.configuration.connectedServers.auth.get(0),
                            ssl:			response.data.configuration.connectedServers.ssl.get(0),
                            useLANIP:		response.data.configuration.connectedServers.useLANIP.get(0),
                            version:		response.data.configuration.connectedServers.version.get(0),
                            platform:		response.data.configuration.connectedServers.platform.get(0)
                        ]

                        // Save the server configuration data state to persistent atomic 
                        setAtomicStateServer(server)

                        pageNext = "preferenceCamerasConfiguration"
                        pageTitle = "Connection Successful"
                        pageSection = "Successful connection to ${server.name}"
                        pageParagraph = "Connection successful to iSpyConnect WAN End Point:  " + server.wan + ", & LAN End Point:  " + server.lan + ", using Auth Code:  " + server.auth
                    } else {
                        pageTitle = "Connection Unsuccessful"
                        pageSection = "iSpyConnect platform unexpected response"
                        if (response?.data?.error != null) {
                            pageParagraph = "Error: ${response.data?.error}, tap Back/Done to return to the previous page to fix the credentials then try again."
                        } else {
                            pageParagraph = "Missing expected JSON data from iSpyConnect Platform!  Make sure the local iSpyConnect server is running and connectivitiy to the iSpyConnect Platform (INTERNET) is operational, then retry."
                        }
                    }
                } else {
                    log.debug "http status: ${response.status}"

                    pageTitle = "Connection Unsuccessful"
                    pageSection = "iSpyConnect platform unexpected responsee"
                    
                    if (response.status != null) {
                    	pageParagraph = "HTTP status response was ${response.status}"
                	} else {
                		pageParagraph = "No response status from the iSpyConnect platform APIs.  Check to make sure the network/INTERNET connectivity is up and your local iSpyConnect Server is operational"
                	}
                }
            }	
        )
	} catch (groovyx.net.http.HttpResponseException e) {
        log.debug "Exception: ${e}"
        
        pageTitle = "General Exception"
        pageSection = "Unexpected application exception"
    	pageParagraph = "Exception Details:  ${e}"
    }    
    
    // If error condition occured then display thru UI
    return dynamicPage(name: "preferenceCredentialsValidation", title: pageTitle, nextPage:pageNext) {
         section(pageSection) {
        	paragraph pageParagraph
        }
    }
}

def preferenceCamerasConfiguration() {
	log.debug "Entered Method: preferenceCamerasConfiguration()"
    
    def cams = getiSpyCameras()
    log.debug "Cameras list: $cams"
    
    return dynamicPage(name: "preferenceCamerasConfiguration", title: "Cameras Configuration", install: true, uninstall: true) {
        section("Select Your Cameras") {
            paragraph "Tap below to see the list of iSpyConnect Cameras available from your iSpyConnect platform account and select the ones you want to connect to SmartThings."
            input(name: "cameras", title:"Cameras selected", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:cams])
        }
    }
}

// Smart Things standard SmartApp handler functions section
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
	log.debug "initialize()"
    log.debug "iSpyCameras: ${cameras}"
    
	def devices = cameras.collect { dni ->
    	def d = getChildDevice(dni)
        def deviceName = atomicState.cameras[dni].toString()
        log.debug "d: ${d}"
        log.debug "Camera is PTZ ${deviceName.contains("PTZ")} for ${atomicState.cameras[dni]}"
        
        if(!d) {
            if (deviceName.contains("PTZ")) {
            	d = addChildDevice(app.namespace, getChildNamePTZ(), dni, null, ["label":"${atomicState.cameras[dni]}" ?: "iSpy PTZ Camera"])
                log.debug "Created ${d.displayName} with id $dni as a iSpy PTZ Camera"
            } else {
            	d = addChildDevice(app.namespace, getChildName(), dni, null, ["label":"${atomicState.cameras[dni]}" ?: "iSpy Camera"])
                log.debug "Created ${d.displayName} with id $dni as a iSpy Camera"
            }
        } else {
            log.debug "found ${d.displayName} with id $dni already exists"
        }
        return d
    }
    log.debug "Created ${devices.size()} Cameras."
    
    // Delete any cameras that are no longer in settings
    def delete
    if(!cameras) {
		log.debug "Delete cameras"
		
        //inherits from SmartApp (data-management)
        delete = getAllChildDevices() 
	} else {
    	delete = getChildDevices().findAll { !cameras.contains(it.deviceNetworkId)}
    }
	log.warn "delete: ${delete}, deleting ${delete.size()} cameras"
	
    //inherits from SmartApp (data-management)
    delete.each { deleteChildDevice(it.deviceNetworkId) } 

	//send activity feeds to tell that device is connected
	def notificationMessage = "is connected to SmartThings"
	// h(notificationMessage) causing error
	atomicState.timeSendPush = null
	atomicState.reAttempt = 0

	//first time polling data from cameras
    pollHandler()

	//automatically update devices status every 5 mins
	runEvery5Minutes("poll")
}

// Smart App with Child devices functions section
def pollHandler() {
	log.debug "pollHandler()"
	
    // Hit the iSpyConnect API for update on all cameras
    pollChildren(null) 

	atomicState.cameras.each {stat ->
		def dni = stat.key
		log.debug ("DNI = ${dni}")
		def d = getChildDevice(dni)
		if(d) {
			log.debug ("Found Child Camera Device.")
			d.generateEvent(atomicState.cameras[dni].data)
		}
	}
}

def pollChildren(child = null) {
    def cameraIdsString = getChildDeviceIdsString()
    log.debug "polling children: $cameraIdsString"

	// Retrieve server configuration data
    def server = getAtomicStateServer()
    
    // Build server URI based on if SSL is/not in use
    def serverURI = null
    if (!server.ssl) {
    	serverURI = "http://" + server.wan
    } else {
    	serverURI = "https://" + server.wan
    }  

	// Initialize result 
    def result = false
   	try{
		httpGet(
        	[	uri: 	serverURI,
        		path: 	getCameraPath(),
        		query:	[cmd: getCamerasCmd(), auth: server.auth]
    		], { response ->
				if(response.status == 200) {
                	storeCameraData(response.data.objectList)
                	result = true
            	}
            }
    	)
	} catch (groovyx.net.http.HttpResponseException e) {
		log.trace "Exception polling children: " + e.response.data.status
        log.debug "Exception Response:  ${e.response.data.status.code}"
        if (e.response.data.status.code == 14) {
            log.debug "TODO!"
        }
	} catch (NoRouteToHostException e) {
        log.debug "Exception polling children: " + e    
    }
    
	return result
}

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
def pollChild() {
	def devices = getChildDevices()

	if (pollChildren()) {
		devices.each { child ->
            if(atomicState.cameras[child.device.deviceNetworkId] != null) {
                def tData = atomicState.cameras[child.device.deviceNetworkId]
                log.debug "pollChild(child)>> data for ${child.device.deviceNetworkId} : ${tData.data}"
                
                //parse received message from parent
                child.generateEvent(tData.data)
            } else if(atomicState.cameras[child.device.deviceNetworkId] == null) {
                log.debug "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId}"
                return null
            }
		}
	} else {
		log.debug "ERROR: pollChildren()"
		return null
	}

}

void poll() {
	pollChild()
}

// Utility functions section
def getAtomicStateCameras() {
	log.debug "Entering getAtomicStateCameras()"
    
	return atomicState.cameras
}

private void storeCameraData(cameras) {
	// Stores data about the Cameras in atomicState.  @param Cameras - a list of cameras as returned from the iSpyConnect API
    log.debug "Storing cameras data: $cameras"
    
    def data
    atomicState.cameras = cameras.inject([:]) { collector, cam ->
        def dni = [ app.id, cam.id ].join('.')
        log.debug "updating dni $dni"

        data = [
            name: cam.name,
            groups: cam.groups,
            typeID: cam.typeID,
            id: cam.id,
            active: cam.active,
            ptzID: cam.ptzID,
            talk: cam.talk,
            micID: cam.micID,
            camID: cam.camID
        ]

        collector[dni] = [data:data]
        return collector
    }
    
    log.debug "updated ${atomicState.cameras?.size()} Cameras: ${atomicState.cameras}"
}

def getAtomicStateServer() {
	return atomicState.server
}

private setAtomicStateServer(stateValue) {
	atomicState.server = stateValue
}

def getChildDeviceIdsString() {
	return cameras.collect { it.split(/\./).last() }.join(',')
}

def getiSpyCameras() {
	log.debug "getiSpyCameras() getting device list"
	
    // Retrieve server configuration data
    def server = getAtomicStateServer()
    
    // Build server URI based on if SSL is/not in use
    def serverURI = null
    if (!server.ssl) {
    	serverURI = "http://" + server.wan
    } else {
    	serverURI = "https://" + server.wan
    }

	// Initialize the cams object that will contain all cameras properties
	def cams = [:]
	try {
		httpGet (
        	[	uri: 	serverURI,
        		path: 	getCameraPath(),
        		query:	[cmd: getCamerasCmd(), auth: server.auth]
    		], {
            	response ->
				if (response.status == 200) {
					response.data.objectList.each { cam ->
						def dni = [app.id, cam.id].join('.')
						if (cam.ptzID <= 0) {
                        	cams[dni] = "Camera " + cam.name
                        } else {
                        	cams[dni] = "Camera PTZ " + cam.name	
                        }
					}
				} else {
					log.debug "http status: ${response.status}"
				}
			}
    	)
	} catch (groovyx.net.http.HttpResponseException e) {
        log.trace "Exception polling children: " + e.response.data.status
        if (e.response.data.status.code == 14) {
			log.debug "TODO!"
        }
        log.debug "No Response Error!!!"
    }
    
	atomicState.cameras = cams
    return cams
}

// iSpyConnect platform API End Point 
def getChildName()  	{ return "iSpy Camera" }
def getChildNamePTZ()	{ return "iSpy PTZ Camera" }
def getiSpyUri()  		{ return "https://www.ispyconnect.com" }
def getiSpyPath()   	{ return "/webservices/API.aspx"}
def getCameraPath()		{ return "/loadobject.json"}
def getCamerasCmd()		{ return "loadobjects"}
def getStreamPath()		{ return "/video.mjpg"}
def getSnapShot()		{ return "/livefeed"}