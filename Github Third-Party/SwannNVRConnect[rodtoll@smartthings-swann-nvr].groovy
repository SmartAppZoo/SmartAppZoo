/**
 *  Copyright 2015 Rod Toll
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
 *  Swann NVR SmartApp
 *
 *  Author: rodtoll
 */

definition(
    name: "Swann NVR (Connect)",
    namespace: "rodtoll",
    author: "SmartThings",
    description: "Connect and take pictures using your Swann NVR from inside the application.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true
)

preferences {
	section("SWANN Configuration") {
            input "swannAddress", "text", title: "ISY Address", required: false, defaultValue: "10.0.1.80" 			// Address of the swann Nvr
            input "swannPort", "number", title: "Swann NVR Port", required: false, defaultValue: 85 				// Port to use for the HTTP interface
            input "cameraCount", "number", title: "Camera Count", required: false, defaultValue: 5  					// # of cameras
            input "swannUserName", "text", title: "User Name", required: false, defaultValue: "admin"				// Username to use with the NVR
			input "swannPassword", "text", title: "Password", required: false, defaultValue: "Alpha1Romero"			// Password to use with the NVR
    }
}


////////////////////////////
// CORE SETUP AND STATE
//

def installed() {
	setCurrentLoadState("Startup")
	initialize()
}

def updated() {
	log.debug "SWANNNVRAPP: Updated with settings: ${settings}"
}

def setCurrentLoadState(state) {
    atomicState.loadingState = state
    log.debug "SWANNNVRAPP: #### Transitioning to state: "+state
}

def initialize() {
	atomicState.activeSnapshotCameraId = -1
    atomicState.cameraMap = [:]
	setCurrentLoadState("Setup")
	findPhysicalHub()
	subscribe(location, null, locationHandler, [filterEvents:false])
	setCurrentLoadState("InitialStatusRequest")
    sendHubCommand(getRequest('/PSIA/System/deviceInfo'))
}


def locationHandler(evt) {
    //try {
        def msg = parseLanMessage(evt.description)
        //log.debug "SWANNNVRAPP: Message: "+msg
        if(!msg.xml) {
        	if(msg.body && msg.body.length() > 0) {
	            msg.xml = new XmlSlurper().parseText(msg.body)
            }
        } 
        if(msg.xml) {
        	//log.debug 'SWANNNVRAPP: Received command... ' + msg.xml.name()
        	handleXmlMessage(msg.xml)
        } else {
            //log.debug 'SWANNNVRAPP: Active..'+atomicState.activeSnapshotCameraId
        	if(atomicState.activeSnapshotCameraId != -1) {
            	//log.debug 'SWANNNVRAPP: Active snapshot: '+atomicState.activeSnapshotCameraId.toString()
                def cameraDni = atomicState.cameraMap[atomicState.activeSnapshotCameraId.toString()]
            	//log.debug 'SWANNNVRAPP: Active dni: #'+cameraDni+'#'
                def cameraDevice = getChildDevice(cameraDni)
                //log.debug 'SWANNNVRAPP: Got a camera to check against'+cameraDevice
                if(cameraDevice != null) {
                	cameraDevice.parse(evt.description)
                }
            }
        }
    //} catch(e) {
    //    log.debug 'ERROR -- Details: '+ e
   // }
}

def resetActiveSnapshot() {
	log.debug 'SWANNNVRAPP: Resetting active snapshot state'
	atomicState.activeSnapshotCameraId = -1
}

//////////////////////
// MESSAGE HANDLERS
//

def getDniForCameraIndex(index) {
	//return makeNetworkId(settings.swannAddress, settings.swannPort)
    return settings.swannAddress + ":" + settings.swannPort + ":" + index.toString()
}

def createCamera(index) {
	def dni = getDniForCameraIndex(index)
	log.debug "SWANNNVRAPP: Adding camera "+index.toString()+" as dni=#"+dni+"#"
    def newDevice = addChildDevice("rodtoll", "SwannNVRCamera", dni, 
                                   null, 
                                   ["label":"Camera "+index.toString(), completedSetup:true, "preferences": 
                                    ["swannAddress": settings.swannAddress, "swannPort": settings.swannPort, "swannUserName": settings.swannUserName, "swannPassword": settings.swannPassword]
                                   ])
    newDevice.setCameraIndex(index)
    def newCameraMap = atomicState.cameraMap
    newCameraMap.put(index.toString(), dni)
    atomicState.cameraMap = newCameraMap
}

def handleXmlMessage(xml) {
	log.debug 'SWANNNVRAPP: Processing xml message. Type='+xml.name()
    if(xml.name() == 'DeviceInfo') {
    	log.debug 'SWANNNVRAPP: Model #: '+xml.model.toString()
        log.debug 'SWANNNVRAPP: Mac Address: '+xml.macAddress.toString()
        log.debug 'SWANNNVRAPP: Firmware Version: '+xml.firmwareVersion.toString()
        log.debug 'SWANNNVRAPP: Firmware Release Date: '+xml.firmwareReleasedDate.toString()
        log.debug 'SWANNNVRAPP: Logic Version: '+xml.logicVersion.toString()
        log.debug 'SWANNNVRAPP: Logic Release Date: '+xml.logicReleasedDate.toString()
		setCurrentLoadState("CreatingDevices")
        for(def index = 1; index <= settings.cameraCount; index++) {
	    	createCamera(index)
        }
		setCurrentLoadState("DoneInitialization")
        
    }
}

//////////////////////
// WORKING METHODS
//


def take(cameraIndex) {
	atomicState.activeSnapshotCameraId = cameraIndex
	def requestPath = '/PSIA/Streaming/channels/'+cameraIndex.toString()+'01/picture'
	log.debug "SWANNNVRCAMERA: Taking picture using..."+requestPath
    def request = getRequestWithS3(requestPath)
//    log.debug "SWANNNVRCAMERA: Request..."+request
    sendHubCommand(request)
}

////////////////////
// HELPER METHODS
//

private getAuthorization() {
    def userpassascii = settings.swannUserName + ":" + settings.swannPassword
    "Basic " + userpassascii.encodeAsBase64().toString()
}

private String makeNetworkId(ipaddr, port) { 
     String hexIp = ipaddr.tokenize('.').collect { 
     String.format('%02X', it.toInteger()) 
     }.join() 
     String hexPort = String.format('%04X', port) 
     return "${hexIp}:${hexPort}" 
}

def findPhysicalHub() {
    def savedIndex = 0
    
	for (def i = 0; i < location.hubs.size(); i++) {
        def hub = location.hubs[i]
        if(hub.type.toString() == "PHYSICAL") {
        	savedIndex = i
        }
    }
    
    log.debug "SWANNNVRAPP: Picking hub: "+savedIndex
    
    atomicState.hubIndex = savedIndex
}

def getPhysicalHubId() {
	return location.hubs[atomicState.hubIndex].id
}

def getRequest(path) {
    new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': settings.swannAddress+":"+settings.swannPort,
            'Authorization': getAuthorization()
        ], null)
}

def getRequestWithS3(path) {
	//log.debug 'SWANNNVRCAMERA: Address='+settings.swannAddress+":"+settings.swannPort+" UserPass="+settings.swannUserName + ":"+settings.swannPassword
    def hubAction = new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': settings.swannAddress+":"+settings.swannPort,
            'Authorization': getAuthorization()
        ], null)
	hubAction.options = [outputMsgToS3:true]
	hubAction        
}
