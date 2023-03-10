/**
 *  Simple Manual Scenes v1.1.0
 *
 *  Copyright 2015 Jim Worley
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
 *  Description:
 *  This app is designed to allow you to attach a list of devices to a button.  
 *  You can manually set the properties for each switch (level, hue, etc.) depending on which capabilities it supports.
 *
 *  This app does not allow you to "record" or "save" the state of the devices that are currently on.  
 *  It does not save what the device's current properties are before changing them.  
 *  You press the button, the devices are set to what you set. 
 *
 *  Author: Jim Worley
 *  Date: 08Aug2015
 *
 * Thanks to Mike Maxwell for the superState app which this GUI was based off of!  You can see
 * his work at https://github.com/mikemaxwell.
 */
 
definition(
    name: "Simple Manual Scenes",
    namespace: "noname4444",
    author: "Jim Worley",
    description: "This app allows you to manually set multiple device states and attach them all to a single button press.",
    category: "My Apps",
    iconUrl: "http://b.dryicons.com/images/icon_sets/coquette_part_7_icons_set/png/64x64/toolbox.png",
    iconX2Url: "http://b.dryicons.com/images/icon_sets/coquette_part_7_icons_set/png/128x128/toolbox.png",
    iconX3Url: "http://b.dryicons.com/images/icon_sets/coquette_part_7_icons_set/png/128x128/toolbox.png")


preferences {
	section("Title") {
		page(name: "main")
		page(name: "addScene")
		page(name: "addDevice")
	}
}

def main(){

	log.debug "settings:${settings}"
	def nextSceneIDX = getnextSceneIDX()
	def nextSceneID = "s${nextSceneIDX}"
	dynamicPage(name: "main", title: "Simple Manual Scenes", uninstall: true,install: true) {
		section("Create Scenes"){
			def prefScenes = getSceneMaps()
			prefScenes.each(){ prefScene ->
				if (settings["${prefScene.key}Status"] != "delete") {
					 href(
						name		: prefScene.key
						,title		: prefScene.value 
						,required	: false
						,params		: [sceneID:prefScene.key]
						,page		: "addScene"
						,description: getSceneDescription(prefScene.key)
						,state		: getSceneStatus(prefScene.key)
					)
				}
			}
		}
		section {
			//always have a link for adding a new scene
			href(
				name		: nextSceneID
				,title		: "Add a new scene..." 
				,required	: false
				,params		: [sceneID:nextSceneID]
				,page		: "addScene"
				,description: null
			)
		}
	}
}


def addScene(params){
	//log.debug "ScenePage- params:${params}"

	//account for different paths between android and ios
    def sceneID
	if (params.sceneID) {
		sceneID = params.sceneID
	} else if (params.params) {
	sceneID = params.params.sceneID
	} else {
		sceneID = atomicState.sceneID
	}
	atomicState.sceneID = sceneID

	def nextDeviceIDX = getNextDeviceIDX(sceneID)
	def nextDeviceID = "${sceneID}d${nextDeviceIDX}"

	dynamicPage(name: "addScene", title: getScenePageTitle(sceneID), uninstall: false,install: false) {
		section {
			input(
				name			: sceneID
				,title			: "Scene Name"
				,multiple		: false
				,required		: true
				,type			: "text"
            )
        }
        section("Devices"){
			def prefDevices = getDeviceMaps(sceneID)
			prefDevices.each(){ prefDevice ->
				if (settings["${prefDevice.key}Status"] != "delete") {
					href(
						name		: prefDevice.key
						,title		: "Device Group ${getDeviceNumberFromID(sceneID, prefDevice.key)}"
						,required	: false
						,params		: [deviceID:prefDevice.key,sceneID:sceneID]
						,page		: "addDevice"
						,state		: getDeviceGroupStatus(prefDevice.key)
						,description: getDeviceDescription(prefDevice.key)
					)
				}
			}
		}
		section {
			//always have a link for adding a new scene
			href(
				name		: nextDeviceID
					,title		: "Add a Device..." 
					,required	: false
					,params		: [deviceID:nextDeviceID,sceneID:sceneID]
					,page		: "addDevice"
					,description: null
				)  
		}
		section {
			input(
				name		: "${sceneID}Status"
				,type		: "enum"
				,title		: "Allows you to inactivate or delete the scene."
				,defaultValue: settings["${sceneID}Status"] ?: "active"
				,required	: true
				,options    : ["active":"Active","inactive":"Inactive","delete":"Remove the scene completely."]
			)
			if (sceneErrors(sceneID)){
				input(name:"${sceneID}Errors",type:"enum",title:"Errors Found",required:true,options: [],description:sceneErrors(sceneID))
			}
			else {
				input(name:"${sceneID}Errors",type:"enum",title:"No Errors Found",required:false,options: [],description:"No errors were found, this scene will be installed.")	
			}
		}	
	}
}

def addDevice(params){
	//log.debug "devicePage- params:${params}" 
	//account for different paths between android and ios
	def deviceID
	if (params.deviceID) {
		deviceID = params.deviceID
	} else if (params.params) {
	    deviceID = params.params.deviceID
	} else {
		deviceID = atomicState.deviceID
	}
	//def sceneID = params.sceneID ?: params.params.sceneID
	atomicState.deviceID = deviceID

	dynamicPage(name: "addDevice", title: fancyDeviceString(settings[deviceID]), install: false) {
		log.debug "Settings are dumb: ${settings}"
		section("Device") {
			input(
				name		: "${deviceID}Type"
				,type		: "enum"
				,title		: "Device Type..."
				,defaultValue: settings["${deviceID}Type"] ?: null
				,required	: true
				,options    : ["switch":"A Switch"]
				,submitOnChange: true
			)
			if (settings["${deviceID}Type"] == "switch") {
			    input(
					name			: deviceID
					,title			: "Switches"
					,multiple		: true
					,required		: true
					,type			: "capability.switch"
					,submitOnChange : true
			    )
			    if (settings["$deviceID"]){
					input(
						name		: "${deviceID}State"
						,type		: "enum"
						,title		: "Turn these switches on or off?  Defaults to On."
						,defaultValue: settings["${deviceID}State"] ?: "on"
						,required	: true
						,options    : ["on":"On","off":"Off"]
						,submitOnChange: true
					)
					if (settings["${deviceID}State"] != "off"){
						if (hasCommand(deviceID,"setLevel")){
						    input(
								name		: "${deviceID}Level"
								,type		: "number"
								,title		: "Brightness level, between 1 and 100.  Defaults to keeping the current level."
								,range: "1..100"
								,required	: false
								,defaultValue: rangeFix(settings["${deviceID}Level"]) ?: null
								,submitOnChange: true
							)
						}  //Switches have level setting
						if (hasCommand(deviceID,"setHue")){
						    input(
								name		: "${deviceID}Hue"
								,type		: "number"
								,title		: "Hue level, between 1 and 100.  Defaults to keeping the current hue."
								,range: "1..100"
								,required	: false
								,defaultValue: rangeFix(settings["${deviceID}Hue"]) ?: null
								,submitOnChange: true
							)
                        }  //Switches have hue setting     
                        if (hasCommand(deviceID,"setSaturation")){
						    input(
								name		: "${deviceID}Saturation"
								,type		: "number"
								,title		: "Saturation level, between 1 and 100.  Defaults to keeping the current saturation."
								,range: "1..100"
								,required	: false
								,defaultValue: rangeFix(settings["${deviceID}Saturation"]) ?: null
								,submitOnChange: true
							)
						}  //Switches have saturation setting
                        if (hasCommand(deviceID,"setColorTemperature")){
						    input(
								name		: "${deviceID}ColorTemperature"
								,type		: "number"
								,title		: "Color Temp level, between 2700 and 9000.  Defaults to keeping the current Color Temp."
								,range: "2700..9000"
								,required	: false
								,defaultValue: settings["${deviceID}ColorTemperature"] ?: null
								,submitOnChange: true
							)
						 }  //Switches have saturation setting
					} //If the switch(es) will be turned on
					input(
						name		: "${deviceID}Status"
						,type		: "enum"
						,title		: "Allows you to inactivate or delete the device from the scene."
						,defaultValue: settings["${deviceID}Status"] ?: "active"
						,required	: true
						,options    : ["active":"Active","inactive":"Inactive","delete":"Remove the device(s) from the scene."]
					)
					if (deviceErrors(deviceID)){
						input(name:"${deviceID}Errors",type:"enum",title:"Errors Found",required:true,options: [],description:deviceErrors(deviceID))
					}
					else {
						input(name:"${deviceID}Errors",type:"enum",title:"No Errors Found",required:false,options: [],description:"No errors were found, you may add this device.")	
					}
				}  //Switch(es) have been selected
			} //The user would like to add switches
		} //Device section
	} //AddDevice dynamic page
} //AddDevice method

def installed() {
	log.debug "Installed with settings: ${settings}"

	//initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	//unsubscribe()
	initialize()

	//def sceneSwitches = getChildDevices()
	//subscribe(sceneSwitches,"switch",processSwitches)
}

def initialize() {
	def numScenes = getMaxSceneIDX()
	def sceneList = []
	def childList = []
	def newSwitch
	if (numScenes) {
		(1..numScenes).each{
			if (settings["s${it}Status"] == "active") {
				sceneList << "s${it.toString()}".toString()
			}
		}
		childList = getAllChildDevices()*.name
		log.debug "Scene list: $sceneList"
		log.debug "Child List: $childList"
		//Look for new scenes that need to have a switch created
		sceneList.each{ sceneID ->
			log.debug "!childList.contains(sceneID): ${!childList.contains(sceneID)}"
			if (!childList.contains(sceneID)) {
				log.debug("I am adding a device for scene $sceneID")
				newSwitch = addChildDevice("noname4444", "Simple Manual Scene Switch", "${app.id}/${sceneID}", null, [name: sceneID, label: settings[sceneID], completedSetup: true])
				subscribe(newSwitch,"switch.on",processSwitches)
			}
		}
		//atomicState.removeList = []
		//Look for swtiches that no longer have an active scene
		childList.each{deviceID ->
			log.debug "!sceneList.contains(deviceID): ${!sceneList.contains(deviceID)}"
			if (!sceneList.contains(deviceID)) {
				log.debug("I am removing a device for scene ${deviceID}, deviceNetworkID: ${app.id}/${deviceID}")
				try {
					unsubscribe(getAllChildDevices().find(){it.name == deviceID})
					if (atomicState.removeList) {
						atomicState.removeList = atomicState.removeList + ["${app.id}/${deviceID}"]
					}
					else {
						atomicState.removeList = ["${app.id}/${deviceID}"]
					}
					log.debug "Remove list added: ${atomicState.removeList}"
					runIn(5,removeKid)
					//deleteChildDevice("${app.id}/${deviceID}")
				} catch (any) {
					log.error "Attempt to delete the switch for scene $deviceID failed."
					log.error any
					//log.debug getAllChildDevices().findAll(){it.name == deviceID}*.getProperties()
				}
			}
		}
	}
	else
	{
    	log.debug "No scenes defined, nothing to do."
	}
}

def removeKid(){
	log.debug "Remove list: ${atomicState.removeList}"
	//deleteChildDevice(atomicState.removeList[0])
	atomicState.removeList.each{
		try{
			deleteChildDevice(it)
		} catch (any){
			log.error any
		}
	}
	atomicState.removeList = []
	log.debug "Remove list post: ${atomicState.removeList}"
}


// TODO: implement event handlers

def processSwitches(evt){
	//log.debug "Event Device ID: ${evt.deviceId}, Device Name: ${evt.device.name}"
	//log.debug evt.device.getProperties()
    evt.device.off()
	def sceneID = evt.device.name
	log.debug "Simple Manual Scene: Switch pressed!  $sceneID"
	def numDevices = getMaxDeviceIDX(sceneID)
	if (!numDevices) {return}
	def deviceID
	def deviceOptions
    def huenum
	//loop through the device groups for the scene
	(1..numDevices).each{ 
		deviceID = "${sceneID}d${it}"
		//Process each device group if it is defined and is active
		if (settings[deviceID] && settings["${deviceID}Status"] == 'active'){
			//process each device in the device group
			settings[deviceID].each{ dev ->
				if (dev){
					log.debug "Processing device ${dev.name}"
					deviceOptions = [:]
					if (settings["${deviceID}State"] == "off"){
						dev.off()
					} //turning device off
					//Turn the device on and process level/color settings if they are specified
					else {
                        //log.debug dev.getProperties()
						dev.on()
						if (settings["${deviceID}Level"]){
							dev.setLevel(rangeFix(settings["${deviceID}Level"]))
						}
						if (settings["${deviceID}Hue"]){
                            huenum = rangeFix(settings["${deviceID}Hue"])
                            dev.setHue(huenum)
						}
						if (settings["${deviceID}Saturation"]){
                            dev.setSaturation(rangeFix(settings["${deviceID}Saturation"]))
						}
                        if (settings["${deviceID}ColorTemperature"]){
                            dev.setColorTemperature(settings["${deviceID}ColorTemperature"])
						}
					} //turning device on
				} //Make sure the device hasn't been deleted
			} //devices within a device group loop
		} //device exists and is active check
	} //device group loop
} //function definition


/**********************************************
UI preference settings methods
***********************************************/

def getScenePageTitle(sceneID){
	return settings[sceneID] ?: "Scene ${sceneID.replace("s","")}"
}

def getDeviceNumberFromID(sceneID, deviceID){
	return deviceID.replace("${sceneID}d","")
}

def getSceneMaps(){
	return settings.findAll(){it.key ==~ /s[0-9]+/}.sort{it.key}
}

def getDeviceMaps(sceneID){
	//scene key format : s1d1
	return settings.findAll(){it.key ==~ /${sceneID}d[0-9]+/}.sort{it.key}
}

def getnextSceneIDX(){
	def found = settings.findAll(){it.key ==~ /s[0-9]+/}
	def next = 0
	def crnt
	found.each(){ it.key
		crnt = it.key.replace("s","").toInteger()
		if (crnt > next ) next = crnt
	}
	next ++
	//log.debug "getNext:${next}"
	return next
}

def getNextDeviceIDX(sceneID){
	def found = settings.findAll(){it.key ==~ /${sceneID}d[0-9]+/}
	def next = 0
	def crnt
	found.each(){ it.key
		crnt = it.key.replace("${sceneID}d","").toInteger()
		if (crnt > next ) next = crnt
	}
	next ++
	return next
}

def getMaxDeviceIDX(sceneID){
	def found = settings.findAll(){it.key ==~ /${sceneID}d[0-9]+/}
	if (found.size() == 0) return null
	def next = 0
	def crnt
	found.each(){ it.key
		crnt = it.key.replace("${sceneID}d","").toInteger()
		if (crnt > next ) next = crnt
	}
	return next
}

def getMaxSceneIDX(){
	def found = settings.findAll(){it.key ==~ /s[0-9]+/}
	if (found.size() == 0) return null
	def next = 0
	def crnt
	found.each(){ it.key
		crnt = it.key.replace("s","").toInteger()
		if (crnt > next ) next = crnt
	}
	return next
}

def getDeviceDescription(deviceID){
   def descriptionParts = []

   if (deviceErrors(deviceID)){
   	  return "ERRORS: Please fix the following error(s) to enable this group.  ${deviceErrors(deviceID)}"
   }

   if (settings["${deviceID}Status"] == "inactive"){
   	  descriptionParts << "INACTIVE:"
   }

   descriptionParts << "${fancyDeviceString(settings[deviceID])} will turn ${settings["${deviceID}State"]}."

   if (settings["${deviceID}State"] == "on") {
	   if (settings["${deviceID}Level"]) {
	     descriptionParts << "The light brightness will turn to ${rangeFix(settings["${deviceID}Level"])}."
	   }
	   if (settings["${deviceID}Hue"]) {
	     descriptionParts << "The hue will be set to ${rangeFix(settings["${deviceID}Hue"])}."
	   }
	   if (settings["${deviceID}Saturation"]) {
	     descriptionParts << "The saturation will be set to ${rangeFix(settings["${deviceID}Saturation"])}."
	   }
       if (settings["${deviceID}ColorTemperature"]) {
	     descriptionParts << "The color temp will be set to ${settings["${deviceID}ColorTemperature"]}."
	   }
       
   }


   return descriptionParts.join(" ")
}

def getDeviceGroupStatus(deviceID){
	if (deviceErrors(deviceID)) {
		return "incomplete"
	}
	else if (settings["${deviceID}Status"] == "active") {
		return "complete"
	}
	else return null
}

//Scans through all the active devices for a scene and compiles them into a list
def getAllDevices(sceneID){
	def allDevices = []
	def numDevices = getMaxDeviceIDX(sceneID)

	if (numDevices){
		(1..numDevices).each{
	   	 	if (settings["${sceneID}d${it}Status"] == "active") {
	   	 		allDevices.addAll(settings["${sceneID}d${it}"])
   	 		}
		}
   	}
   	 return allDevices
}

def getSceneDescription(sceneID){
	def allDevs = getAllDevices(sceneID)
	def descriptionParts = []

	if (allDevs) {
		if (settings["${sceneID}Status"] == "inactive") {
			descriptionParts << "INACTIVE:"
		}
		descriptionParts << "The following devices will be set by this scene: ${fancyDeviceString(allDevs)}"
	}
	else {
		descriptionParts << "ERROR: No devices set for this scene."
	}

	return descriptionParts.join(" ")
}

def getSceneStatus(sceneID){
	if (!getAllDevices(sceneID)) {
		return "incomplete"
	}
	else if (settings["${sceneID}Status"] == "active") {
		return "complete"
	}
	else return null
}

/****************************
 * Utility Methods
 ****************************/

def fancyString(listOfStrings) {

	def fancify = { list ->
		return list.collect {
			def label = it
			if (list.size() > 1 && it == list[-1]) {
				label = "and ${label}"
			}
			label
		}.join(", ")
	}

	return fancify(listOfStrings)
}

def fancyDeviceString(devices = []) {
	fancyString(devices.collect { deviceLabel(it) })
}

def deviceLabel(device) {
	return device.label ?: device.name
}

def rangeFix(range){
	//log.debug "The range says: $range"
	if (!range) return null
	def rangeInt = range.toInteger()
	if (rangeInt >= 1 && rangeInt <= 100) return range
	else if (rangeInt < 1)  return 1
	else if (rangeInt > 100)  return 100
	else return null
}

/***********************************
 * Capability and Error Checking
 ***********************************/
private hasCommand(deviceList,commandName) {
	if (!settings["$deviceList"]) return false
	def isDimmer = false
	def allDimmer = true
	settings["$deviceList"].each(){ device ->
		isDimmer = false
		device.supportedCommands.each {
			if (it.name.contains(commandName)) {
				isDimmer = true
			}
		}
		if (!isDimmer) allDimmer = false
	}
	//log.debug("I'm guessing the dimmer allowed is: ${allDimmer}")
	return allDimmer
}

private deviceErrors(deviceID){
//   def descriptionParts = []
   return null

//   if (settings["${deviceID}Level"] && (settings["${deviceID}Level"] < 1 || settings["${deviceID}Level"] > 100)) {
//     descriptionParts << "The brightness level must be between 1 and 100 or missing."
//   }
//   if (settings["${deviceID}Hue"] && (settings["${deviceID}Hue"]  < 1 || settings["${deviceID}Hue"] > 100)) {
//     descriptionParts << "The hue must be between 0 and 100 or missing."
//   }
//   if (settings["${deviceID}Saturation"] && (settings["${deviceID}Saturation"] < 1 || settings["${deviceID}Saturation"] > 100)) {
//     descriptionParts << "The saturation must be between 0 and 100 or missing."
//   }
//   return descriptionParts.join(" ")
}

private sceneErrors(sceneID){
	def descriptionParts = []
	def repeats = []

	getAllDevices(sceneID).groupBy{it.id}.findAll{it.value.size() >1}.each() {repeats << it.value[0]}
	if (repeats){
		descriptionParts << "The following devices are used in more than one device list: ${fancyDeviceString(repeats)}"
	}
	return descriptionParts.join(" ")
}

