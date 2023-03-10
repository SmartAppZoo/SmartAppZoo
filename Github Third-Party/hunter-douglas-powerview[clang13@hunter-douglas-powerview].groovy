/**
 *  Hunter Douglas PowerView
 *
 *  Copyright 2017 Chris Lang
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
    name: "Hunter Douglas PowerView",
    namespace: "clang13",
    author: "Chris Lang",
    description: "Provides control of Hunter Douglas shades and scenes via the PowerView hub.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    
    appSetting "devOpt"
}


preferences {
	section("Title") {
		page(name: "mainPage")
        page(name: "devicesPage")
        page(name: "roomsPage")
	}
}

/*
 * Pages
 */
def mainPage() {
    def setupComplete = !!atomicState?.shades
	def pageProperties = [
    	name: "mainPage",
        title: "",
        install: setupComplete,
        uninstall: atomicState?.installed
    ]
    
	return dynamicPage(pageProperties) {
        section("PowerView Hub") {
            input("powerviewIPAddress", "text", title: "IP Address", defaultValue: "", description: "(ie. 192.168.1.10)", required: true, submitOnChange: true)
        }
        if (settings?.powerviewIPAddress) {
        	section("Devices & Scenes") {
                def description = (description != "") ? (description + "\nTap to modify") : "Tap to configure";
                href "devicesPage", title: "Manage Devices", description: description, state: "complete"
                atomicState?.loadingDevices = false
			}
        }
    }
}

def devicesPage() {
	def pageProperties = [
    	name: "devicesPage",
        title: "Manage Devices"
    ]

	// log.debug "atomicState?.loadingDevices = ${atomicState?.loadingDevices}"
	if (!atomicState?.loadingDevices) {
    	atomicState?.loadingDevices = true
    	getDevices()
	}
    
    // log.debug "atomicState?.deviceData = ${atomicState?.deviceData}"
    if (!atomicState?.deviceData?.shades || !atomicState?.deviceData?.scenes || !atomicState?.deviceData?.rooms) {
        pageProperties["refreshInterval"] = 1
        return dynamicPage(pageProperties) {
        	section("Discovering Devices...") {
	            paragraph "Please wait..."
            }
        }
	}
    
	return dynamicPage(pageProperties) {
    	section("Rooms") {
        	href "roomsPage", title: "Manage Rooms", description: "Tap to configure open/close scenes for each room", state: "complete"
		}
        section("Shades") {
            input("syncShades", "bool", title: "Automatically sync all shades", required: false, defaultValue: true, submitOnChange: true)        	
            if (settings?.syncShades == true || settings?.syncShades == null) {
                def shadesDesc = atomicState?.deviceData?.shades.values().join(", ")
                paragraph "The following shades will be added as devices: ${shadesDesc}"
                atomicState?.shades = atomicState?.deviceData?.shades
            } else {
                input(name: "shades", title:"Shades", type: "enum", required: false, multiple: true, submitOnChange: true, metadata: [values:atomicState?.deviceData?.shades])
                atomicState?.shades = getSelectedShades(settings?.shades)
                log.debug "shades: ${settings?.shades}"
            }
        }
        section("Scenes") {
            input("syncScenes", "bool", title: "Automatically sync all scenes", required: false, defaultValue: true, submitOnChange: true)        	
            if (settings?.syncScenes == true || settings?.syncScenes == null) {
                def scenesDesc = atomicState?.deviceData?.scenes.values().join(", ")
                paragraph "The following shades will be added as devices: ${scenesDesc}"
                atomicState?.scenes = atomicState?.deviceData?.scenes
            } else {
                input(name: "scenes", title:"Scenes", type: "enum", required: false, multiple: true, submitOnChange: true, metadata: [values:atomicState?.deviceData?.scenes])
                atomicState?.scenes = getSelectedScenes(settings?.scenes)
            }
        }
    }
}

def roomsPage() {
	def pageProperties = [
    	name: "roomsPage",
        title: "Manage Rooms"
    ]

	dynamicPage(pageProperties) {
        section {
        	paragraph("Configure scenes to open or close the blinds in each room. A virtual device will be created for each room so configured.")
        }
        def rooms = [:]
        atomicState?.deviceData.rooms.collect{ id, name ->
            section(name) {
            	def openSetting = "room" + id + "Open";
                def closeSetting = "room" + id + "Close"
            	def description
                if (settings[openSetting] && settings[closeSetting]) {
                	description = "Blinds in this room will open and close via the configured scenes."
				} else if (settings[openSetting]) {
                	description = "Blinds in this room will open via the configured scene, but not close."
				} else if (settings[closeSetting]) {
                	description = "Blinds in this room will close via the configured scene, but not open."
				} else {
                	description = "No virtual device will be created for this room because neither open nor close scenes are configured."
                }
                paragraph(description)
                
                // TODO limit to scenes for this room or multi-room scenes
                input(name: openSetting, title:"Open", type: "enum", required: false, multiple: false, submitOnChange: true, metadata: [values:atomicState?.deviceData?.scenes])
                input(name: closeSetting, title:"Close", type: "enum", required: false, multiple: false, submitOnChange: true, metadata: [values:atomicState?.deviceData?.scenes])
                
                rooms[id] = [
                	name: name,
                	openScene: settings[openSetting],
                    closeScene: settings[closeSetting],
                ]
			}
        }
        atomicState?.rooms = rooms        
//        log.debug "atomicState?.rooms = ${atomicState?.rooms}"
    }
}

/*
 * Service Manager lifecycle
 */
def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
}

def initialize() {
	atomicState?.installed = true
    unsubscribe()
    addRemoveDevices()
    
    unschedule()
    pollShades()
    runEvery5Minutes("pollShades")
}

def addRemoveDevices() {
	log.debug "addRemoveDevices"
	def devicesInUse = []
    if (atomicState?.rooms) {
    	atomicState?.rooms?.collect{ id, room ->
        	log.debug "checking room ${id}"
        	if (room.openScene || room.closeScene) {
                def dni = roomIdToDni(id)
                def child = getChildDevice(dni)
                if (!child) {
                    child = addChildDevice("clang13", "Hunter Douglas PowerView Room", dni, null, [label: getRoomLabel(room.name)])
                    log.debug "Created child '${child}' with dni ${dni}"
                }
                devicesInUse += dni
            }
        }
    }
    if (atomicState?.shades) {
    	atomicState?.shades?.collect{ id, name ->
        	def dni = shadeIdToDni(id)
            def child = getChildDevice(dni)
            if (!child) {
            	child = addChildDevice("clang13", "Hunter Douglas PowerView Shade", dni, null, [label: name])
                log.debug "Created child '${child}' with dni ${dni}"
			}
            devicesInUse += dni
        }
    }
    if (atomicState?.scenes) {
    	atomicState?.scenes?.collect{ id, name ->
        	def dni = sceneIdToDni(id)
            def child = getChildDevice(dni)
            if (!child) {
            	child = addChildDevice("clang13", "Hunter Douglas PowerView Scene", dni, null, [label: name])
                log.debug "Created child '${child}' with dni ${dni}"
			}
            devicesInUse += dni
        }
	}
    
    log.debug "devicesInUse = ${devicesInUse}"
    def devicesToDelete = getChildDevices().findAll { !devicesInUse?.toString()?.contains(it?.deviceNetworkId) }
    if (devicesToDelete?.size() > 0) {
        devicesToDelete.each { 
	    	log.debug "Deleting device ${it.deviceNetworkId}"
        	deleteChildDevice(it.deviceNetworkId) 
        }
    }    
}

def pollShades() {
    def now = now()
    def updateBattery = false

	// Update battery status no more than once an hour
    if (!atomicState?.lastBatteryUpdate || (atomicState?.lastBatteryUpdate - now) > (60 * 60 * 1000)) {
    	updateBattery = true
        atomicState?.lastBatteryUpdate = now
	}
    
    log.debug "pollShades: updateBattery = ${updateBattery}"
    
    getShadeDevices().eachWithIndex{ device,index -> 
    	if (device != null) {
	        def shadeId = dniToShadeId(device.deviceNetworkId)
	    	runIn(index * 5, "pollShadeDelayed", [overwrite: false, data: [shadeId: shadeId, updateBattery: updateBattery]])
        } else {
        	log.debug "Got null shade device, index ${index}"
        }
    }
}

def pollShadeDelayed(data) {
//	log.debug "pollShadeDelayed: data: ${data}"
	pollShadeId(data.shadeId, data.updateBattery);
}

/*
 * Device management
 */
def getDevices() {
	getRooms()
	getShades()
    getScenes()
}

def getRoomLabel(roomName) {
	return "${roomName} Blinds"
}

def getRoomDniPrefix() {
	return "PowerView-Room-";
}

def getSceneDniPrefix() {
	return "PowerView-Scene-";
}

def getShadeDniPrefix() {
	return "PowerView-Shade-";
}

def roomIdToDni(id) {
	return "${getRoomDniPrefix()}${id}";
}

def dniToRoomId(dni) {
	def prefix = getRoomDniPrefix()
	return dni.startsWith(prefix) ? dni.replace(prefix, "") : null
}

def sceneIdToDni(id) {
	return "${getSceneDniPrefix()}${id}";
}

def dniToSceneId(dni) {
	def prefix = getSceneDniPrefix()
	return dni.startsWith(prefix) ? dni.replace(prefix, "") : null
}

def shadeIdToDni(id) {
	return "${getShadeDniPrefix()}${id}";
}

def dniToShadeId(dni) {
	def prefix = getShadeDniPrefix()
	return dni.startsWith(prefix) ? dni.replace(prefix, "") : null
}

def getSceneDevices() {
	return atomicState?.scenes?.keySet().collect{ 
    	getChildDevice(sceneIdToDni(it)) 
    }
}

def getShadeDevice(shadeId) {
	return getChildDevice(shadeIdToDni(shadeId))
}

def getShadeDevices() {
	return atomicState?.shades?.keySet().collect{ 
    	getChildDevice(shadeIdToDni(it)) 
    }
}

// data can contain 'shades', 'scenes', and/or 'rooms' -- only deviceData for specified device types is updated
def updateDeviceDataState(data) {
    def deviceData = atomicState?.deviceData ?: [:]

	if (data?.rooms) {
    	deviceData["rooms"] = data?.rooms
    }
    if (data?.scenes) {
	    deviceData["scenes"] = data?.scenes
    }
    if (data?.shades) {
	    deviceData["shades"] = data?.shades
    }
    
    atomicState?.deviceData = deviceData
//    log.debug "updateDeviceData: atomicState.deviceData: ${atomicState?.deviceData}"
}

def getSelectedShades(Collection selectedShadeIDs) {
	return getSelectedDevices(atomicState?.deviceData?.shades, selectedShadeIDs)
}

def getSelectedScenes(Collection selectedSceneIDs) {
	return getSelectedDevices(atomicState?.deviceData?.scenes, selectedSceneIDs)
}

def getSelectedDevices(Map devices, Collection selectedDeviceIDs) {
	if (!selectedDeviceIDs) {
    	return [:]
    }
	return devices?.findAll{ selectedDeviceIDs.contains(it.key) }
}

/*
 * PowerView API
 */

// ROOMS

def getRooms() {
	callPowerView("rooms", roomsCallback)
}

def openRoom(roomDevice) {
	log.debug "openRoom: roomDevice = ${roomDevice}"
    
	def roomId = dniToRoomId(roomDevice.deviceNetworkId)
    def sceneId = atomicState?.rooms[roomId]?.openScene
    if (sceneId) {
    	triggerScene(sceneId)
	} else {
    	log.debug "no open scene configured for room ${roomId}"
	}
}

def closeRoom(roomDevice) {
	log.debug "closeRoom: roomDevice = ${roomDevice}"
    
	def roomId = dniToRoomId(roomDevice.deviceNetworkId)
    def sceneId = atomicState?.rooms[roomId]?.closeScene
    if (sceneId) {
    	triggerScene(sceneId)
	} else {
    	log.debug "no close scene configured for room ${roomId}"
	}
}

void roomsCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Entered roomsCallback()... hubResponse: ${hubResponse}"
    log.debug "json: ${hubResponse.json}"
    
    def rooms = [:]
	hubResponse.json.roomData.each{ room ->
    	def name = new String(room.name.decodeBase64())
        rooms[room.id] = name
    	log.debug "room: ID = ${room.id}, name = ${name}"
    }
    
    updateDeviceDataState([rooms: rooms])
}

// SCENES

def getScenes() {
	callPowerView("scenes", scenesCallback)
}

def triggerSceneFromDevice(sceneDevice) {
	def sceneId = dniToSceneId(sceneDevice.deviceNetworkId)
    triggerScene(sceneId)
}

def triggerScene(sceneId) {
	callPowerView("scenes", null, [sceneid: sceneId])
}

void scenesCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Entered scenesCallback()... hubResponse: ${hubResponse}"
    log.debug "json: ${hubResponse.json}"
    
    def scenes = [:]
	hubResponse.json.sceneData.each{ scene ->
    	def name = new String(scene.name.decodeBase64())
        scenes[scene.id] = name
    	log.debug "scene: ID = ${scene.id}, name = ${name}"
    }
    
    updateDeviceDataState([scenes: scenes])
}

// SHADES 

def getShades() {
	callPowerView("shades", shadesCallback)
}

def pollShade(shadeDevice, updateBatteryStatus = false) {
	log.debug "pollShade: shadeDevice = ${shadeDevice}"
	def shadeId = dniToShadeId(shadeDevice.deviceNetworkId)
    pollShadeId(shadeId)
}

def pollShadeId(shadeId, updateBatteryStatus = false) {
//	log.debug "pollShadeId: shadeId = ${shadeId}"
    def query = [refresh: "true"]
    if (updateBatteryStatus) {
    	query["s"] = "true"
    }
    callPowerView("shades/${shadeId}", shadePollCallback, query)
}

def calibrateShade(shadeDevice) {
	log.debug "calibrateShade: shadeDevice = ${shadeDevice}"
    moveShade(shadeDevice, [motion:"calibrate"])
}

def jogShade(shadeDevice) {
	log.debug "jogShade: shadeDevice = ${shadeDevice}"
    moveShade(shadeDevice, [motion:"jog"])
}

def setPosition(shadeDevice, positions) {
	log.debug "setPosition: shadeDevice = ${shadeDevice}"
    
    def shadePositions = [:]
    def positionNumber = 1
    if (positions?.containsKey("bottomPosition")) {
    	shadePositions["position${positionNumber}"] = (int) (positions.bottomPosition * 65535 / 100)
        shadePositions["posKind${positionNumber}"] = 1
        positionNumber += 1
	}
    if (positions?.containsKey("topPosition")) {
    	shadePositions["position${positionNumber}"] = (int) (positions.bottomPosition * 65535 / 100)
        shadePositions["posKind${positionNumber}"] = 2
	}
    
    moveShade(shadeDevice, [positions:shadePositions])
}

def moveShade(shadeDevice, movementInfo) {
	def shadeId = dniToShadeId(shadeDevice.deviceNetworkId)
    
    def body = [:]
    body["shade"] = movementInfo
    body.shade["id"] = shadeId

	def json = new groovy.json.JsonBuilder(body)
    callPowerView("shades/${shadeId}", setPositionCallback, null, "PUT", json.toString())
}

void shadePollCallback(physicalgraph.device.HubResponse hubResponse) {
	def shade = hubResponse.json.shade
    def childDevice = getShadeDevice(shade.id)
    
    log.debug "poll callback for shade id ${shade.id}, calling device ${childDevice}"
    childDevice.handleEvent(shade)
}

void setPositionCallback(physicalgraph.device.HubResponse hubResponse) {
//    log.debug "Entered setPositionCallback()... hubResponse: ${hubResponse}"
//    log.debug "json: ${hubResponse.json}"

	def shade = hubResponse.json.shade
    def childDevice = getShadeDevice(shade.id)
    
    log.debug "setPositionCallback for shadeId ${shade.id}, calling device ${childDevice}"
    childDevice.handleEvent(shade)
}

void shadesCallback(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Entered shadesCallback()... hubResponse: ${hubResponse}"
    log.debug "json: ${hubResponse.json}"
    
    def shades = [:]
	hubResponse.json.shadeData.each{ shade ->
    	def name = shade.name ? new String(shade.name.decodeBase64()) : "Shade ID ${shade.id}"
        shades[shade.id] = name
    	log.debug "shade: ID = ${shade.id}, name = ${name}"
    }
    
    updateDeviceDataState([shades: shades])
}

// CORE API

def callPowerView(String path, callback, Map query = null, String method = "GET", String body = null) {    
	def host = "${settings?.powerviewIPAddress}:80"
    def fullPath = "/api/${path}"
    
    log.debug "callPowerView: url = 'http://${host}${fullPath}', method = '${method}', body = '${body}', query = ${query}"
    
    def headers = [
    	"HOST" : host,
    ]
    
    def hubAction = new physicalgraph.device.HubAction(
        method: method,
        path: fullPath,
        headers: headers,
        query: query,
        body: body,
        null,
        [callback: callback]
    )
    
//    log.debug "Sending HubAction: ${hubAction}"
    
    sendHubCommand(hubAction)
}

