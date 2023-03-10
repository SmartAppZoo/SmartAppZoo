/*
 *  RTI Audio
 *
 *  Copyright 2020 Ralph Torchia
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

import groovy.json.*

definition(
    name: "RTI Audio",
    namespace: "rtorchia",
    author: "Ralph Torchia",
    description: "Integrate and control RTI audio distribution system",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/rtorchia/rti_audio/master/resources/images/rti_logo2.png",
    iconX2Url: "https://raw.githubusercontent.com/rtorchia/rti_audio/master/resources/images/rti_logo2.png",
    iconX3Url: "https://raw.githubusercontent.com/rtorchia/rti_audio/master/resources/images/rti_logo2.png",
    singleinstance: true
)

preferences {
    section("RTI Audio Device Setup") {
    	input(name: "deviceIP", type: "text", title: "IP Address", description: "The IP address of your RTI audio distribution system (required)", required: true)
        input(name: "maxZones", type: "number", title: "Max Zones", description: "Max number of zones system supports (required)", defaultValue: "4", required: true)
	}
    section("RTI Audio Source Names") {
    	input(name: "s1Name", type: "text", title: "Source 1", required: false)
    	input(name: "s2Name", type: "text", title: "Source 2", required: false)
    	input(name: "s3Name", type: "text", title: "Source 3", required: false)
    	input(name: "s4Name", type: "text", title: "Source 4", required: false)
    }
}

def installed() {
	//subscribeToEvents()
	atomicState.zoneList = [:]
    mainSetup()
	initialize()
}

def updated() {
	mainSetup()
    unsubscribe()
	initialize()
}

def mainSetup() {
    defineZoneNames()
    createZoneDevices()
    getCurrentConfig()
//    runEvery30Minutes(getCurrentConfig())
}

def uninstalled() {
	atomicState.zoneList = [:]
    removeZoneDevices()
}

def initialize() {
	// set default source names if left empty
}

def defineZoneNames() {
    if (s1Name == "") { s1Name = "Unknown" }
	if (s2Name == "") { s2Name = "Unknown" }
	if (s3Name == "") { s3Name = "Unknown" }
	if (s4Name == "") { s4Name = "Unknown" }
}    
    
// create zone devices
def createZoneDevices() {
   def deviceDNI = ""
   def zoneCounter = 0
   def temp = atomicState.zoneList
   if (maxZones == null) { maxZones = 4 }
   maxZones.times {
    	zoneCounter++
    	deviceDNI = "RTI Audio Zone ${zoneCounter}"
    	def device = getChildDevice(deviceDNI)
    	log.debug "deviceDNI: ${deviceDNI}"
        if (!device) {
        	device = addChildDevice("rtorchia", "RTI Audio Zone", deviceDNI, null, [label: deviceDNI])
    		temp[device.id] = zoneCounter
            log.debug "Added zone device: ${device} and map ${temp}"
        }
    }
	atomicState.zoneList = temp
    log.debug "atomicstate: ${atomicState.zoneList}"
}

// remove zone devices
def removeZoneDevices() {
	getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

// get zone configuration of audio device
def getCurrentConfig() {
    log.debug "Trying to access device config at ${deviceIP}"
    def hubAction = new physicalgraph.device.HubAction(
   		method: "GET",
       	path: "/rti_status.cgi",
   	 	headers: ["HOST": "${deviceIP}:80", "Content-Type": "text/xml; charset='utf-8'"],
   	 	null,[callback: updateZoneStatus])
   	sendHubCommand(hubAction)
}

// setup devices with config from RTI
def updateZoneStatus(physicalgraph.device.HubResponse hubResponse) {
    def config = hubResponse.body.replaceAll("<!--#...-->", "")
    def slurper = new JsonSlurper()
    def json = slurper.parseText(config)
    
    //log.info "Source: ${json.zones.zone.src}"
    //log.info "Power : ${json.zones.zone.pwr}"
    //log.info "Volume: ${json.zones.zone.vol}"
    //log.info "Mute  : ${json.zones.zone.mut}"
    //log.info "Group : ${json.zones.zone.grp}"
    // to access the key of each zone use, example, -> json.zones.zone[i].vol
	
    def deviceDNI = ""
    int zoneNumber = 0
    
    maxZones.times {
    	zoneNumber++
    	deviceDNI = "RTI Audio Zone ${zoneNumber}"
    	def device = getChildDevice(deviceDNI)
        
        if (device) {
        	device.setZoneSettings(json.zones.zone[zoneNumber-1], getSourceName(json.zones.zone[zoneNumber-1].src))
            log.debug "Sent new config to ${device} : ${json.zones.zone[zoneNumber-1]}"
        }
	}

}         

def getSourceName(source) {
	def sourceName = "None"
    if (source == "1") { sourceName = s1Name }
    else if (source == "2") { sourceName = s2Name }
    else if (source == "3") { sourceName = s3Name }
    else if (source == "4") { sourceName = s4Name }
    return sourceName
}

// send updates to RTI audio device 
def sendCommand(rtidata, dni) {
    def rti_cmd = ""
	def rtizone = atomicState.zoneList[dni]
    
    log.debug "ID: ${dni}, ZoneList: ${atomicState.zoneList}"
    
    if (rtidata.containsKey("source")) {
    	rti_cmd = "/rti_zi.cgi?z=${rtizone}&i=${rtidata.source}&s=0"
    }
    else if (rtidata.containsKey("volume")) {
        def vol = Math.round(((rtidata.volume.toInteger()/100)-1)*75)
        rti_cmd = "/rti_zvs.cgi?z=${rtizone}&v=${vol}&s=0"
    }
    else if (rtidata.containsKey("power")) {
        if (rtidata.power == "0") {
        	rti_cmd = "/rti_zp0.cgi?z=${rtizone}&s=0"
        } else {
        	rti_cmd = "/rti_zp1.cgi?z=${rtizone}&s=0"
        }
    }
    else if (rtidata.containsKey("mute")) {
        if (rtidata.mute == "0") {
        	rti_cmd = "/rti_zm0.cgi?z=${rtizone}&s=0"
        } else {
        	rti_cmd = "/rti_zm1.cgi?z=${rtizone}&s=0"
        }
    }
    else if (rtidata.containsKey("group")) {
        rti_cmd = "/rti_zg.cgi?z=${rti_zone}&g=${rtidata.group}&s=0"
    }
   
    def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
        path: "${rti_cmd}",
        headers: ["HOST": "${deviceIP}:80", "Content-Type": "text/xml; charset='utf-8'"])
    sendHubCommand(hubAction)
    
    log.debug "Sent device new status command ${rti_cmd}"
    
    // getCurrentConfig()
}
