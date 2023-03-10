/**
 *  Home Audio Controller
 *
 *  Copyright 2016 Steve Sell
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
    name: "Home Audio Controller",
    namespace: "steve28",
    author: "Steve Sell",
    description: "HTD MCA-66 Controller SmartApp",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	singleInstance: true
)

preferences {
  section("SmartThings Hub") {
    input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  }
  section("MCA-66 Controller") {
    input "ip_address", "text", title: "Controller Address", description: "(ie. 192.168.1.10)", required: true, defaultValue: "192.168.1.144"
    input "port", "text", title: "Controller Port", description: "(ie. 8080)", required: true, defaultValue: "8080"
  }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    log.debug "Retrieving zone labels and adding children."
    sendCommand("/mca66?command=getzonelabels","getZoneLabels")
}

def uninstalled() {
	log.debug "Uninstalling.  Deleting children..."
	deleteZones()
}

def updated() {
	log.debug "Running Updated"
    refreshZones()
}

def hubResponseHandler(hubResponse) {
	if (hubResponse.status != 200) {
    	log.err "Error talking to server: ${hubResponse.status}"
        return
    }
    
    // Send the zone status messages out to each zone
    for (def i=1;i<7;i++) {
    	def zonedevice = getChildDevice("mca66_zone_${i}")
        if (zonedevice) {
        	log.debug "Zone ${i}: " + hubResponse.json["${i}"]
            zonedevice.updateZone(hubResponse.json["${i}"])
        }
    }
}

def refreshZones() {
	log.debug "Refreshing Zones"
	sendCommand("/mca66?command=status&zone=1")
}

def followMe(vol, src, all) {
	def endloop=0
	if (all==true) {
    	endloop = 7
    } else {
    	endloop = 6
    }
   	log.trace "Executing Follow Me. vol:${vol} src:${src} endloop:${endloop}"

	for (def i=1;i<endloop;i++) {
    	log.trace "Looping follow me..."
    	sendCommand("/mca66?command=pwr&zone=${i}&value=1")
    	sendCommand("/mca66?command=setinput&zone=${i}&value=${src}")
        sendCommand("/mca66?command=setvol&zone=${i}&value=${vol}")
    }
}

private sendCommand(path, callbackHandler="hubResponseHandler") {
	def host = settings.ip_address + ":" + settings.port
	def myAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: path,
        headers: [
            HOST: host
        ]],
        host,
        [callback: callbackHandler]
    )
    sendHubCommand(myAction)
}

private addZones(names_json) {
    log.debug "Adding zone children..."
    log.debug names_json
    for (def i=1; i<7; i++) {
    	def zone_label = names_json["${i}"]
    	log.debug "Adding mca66_zone_${i} ${zone_label}"
        addChildDevice("steve28", "MCA-66 Zone", "mca66_zone_${i}", hostHub.id, 
                       ["name":"mca66_zone_${i}", label:zone_label])
    }
}

private deleteZones() {
	log.debug "Deleting Children..."
	getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def getZoneLabels(hubResponse) {
	log.debug "Got zone label reply"
    if (hubResponse.status != 200) {
        log.err "Error talking to server: ${hubResponse.status}"
    } else {
        addZones(hubResponse.json)
    }
}