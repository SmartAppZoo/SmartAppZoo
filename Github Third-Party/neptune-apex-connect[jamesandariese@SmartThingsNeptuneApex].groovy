/**
 *  Neptune Apex (Connect)
 *
 *  Copyright 2017 James Andariese
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
    name: "Neptune Apex (Connect)",
    namespace: "jamesandariese",
    author: "James Andariese",
    description: "Neptune Apex (Connect)",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "username"
    appSetting "password"
}


preferences {
	page(name: "page1", "title": "Connect", nextPage: "page2", uninstall: true, install: false)
    page(name: "page2", "title": "Select", install: true, uninstall: true)
}

def page1() {
	atomicState.done = null
    dynamicPage(name: "page1", "title": "Connect", nextPage: "page2", uninstall: true, install: false) {
		section("Connection") {
			input("name": "ip", type: "text", title: "IP", description: "A static IP is recommended")
        	input("name": "username", type: "text", title: "Username", description: "(not your Apex Fusion username)")
        	input("name": "password", type: "password", "title": "Password", description: "(not your Apex Fusion password)")

        }
        section("Setup Requirements") {
            paragraph("You will need to have enabled LAN access on your controller for this to work.\nLAN access or Classic Dashboard is not the same as Apex Fusion and must be setup separately.")
            href(name: "classicDashboard",
            	title: "Classic Dashboard Setup",
            	required: false,
            	style: "external",
            	url: "http://support.neptunesystems.com/support/solutions/articles/3000017576-how-to-access-your-classic-dashboard-pc",
            	description: "Tap to view setup instructions for Apex Classic Dashboard (LAN access)")
		}
	}
}

def page2() {
	if (atomicState.done == null) {
    	log.debug "first run.  sending off hub action"
	    atomicState.done = false
        
        def auth = "$username:$password".bytes.encodeBase64()
        
	    def params = [
    		"headers": [
       		 	"HOST": "$ip:80",
        	    "Authorization": "Basic $auth",
        	],
        	"method": "GET",
        	"path": "/cgi-bin/status.json",
    	]
        log.debug params
    	sendHubCommand(new physicalgraph.device.HubAction(params, "$ip:80", [callback: handleHubStatusResponseDuringSetup]))
        atomicState.done = false
    }
    if (!atomicState.done) {
    	log.debug "still waiting"
		dynamicPage(name: "page2", title: "Connecting", install:false, uninstall: false, refreshInterval: 1) {
    		section("Connecting") {
        		paragraph("Connecting to your Apex controller.  Please be patient")
    	    }
	    }
    } else {
        log.debug "all done waiting"
        if (!atomicState.success) {
            dynamicPage(name: "page2", title: "Connecting", install:false, uninstall: false) {
            	section("Error") {
       	    		paragraph("There was an error connecting to your Apex controller.  Your IP or credentials may have been wrong.  Here's what the controller responded with:")
                }
                section("Raw Output") {
                	paragraph(atomicState.message)
                }
            }
        } else {
        	def objects = [
            	"feed_0": [type: "feedMode", name: "Feed Mode A", did: "feed_0"],
            	"feed_1": [type: "feedMode", name: "Feed Mode B", did: "feed_1"],
            	"feed_2": [type: "feedMode", name: "Feed Mode C", did: "feed_2"],
            	"feed_3": [type: "feedMode", name: "Feed Mode D", did: "feed_3"],
            ]
            dynamicPage(name: "page2", title: "Connecting", install:true, uninstall: false) {
                log.debug "adding feed mode switches and buttons"
                section("Feed Mode") {
                    paragraph("Switches which turn feed mode off when switched off")
                    input(name: "did_feed_0", title: "Feed Mode A Switch", type: "bool")
                    input(name: "did_feed_1", title: "Feed Mode B Switch", type: "bool")
                    input(name: "did_feed_2", title: "Feed Mode C Switch", type: "bool")
                    input(name: "did_feed_3", title: "Feed Mode D Switch", type: "bool")
                }
                def switchesForTypes = { sectionName, typefilter, objlist ->
                	log.debug "adding switches for $sectionName, $typefilter"
                    //section(sectionName) {
                        objlist.each {
                        	def name = "did_${it['did']}"
                            if (it['type'] == typefilter) {
                                objects[it['did']] = it
                                def dfv = settings[name]
                                if (!dfv) {
                                	dfv = false
                                }
                                log.debug "Default value for $name is $dfv"
                                input(name: name, title: it['name'], type: "bool", defaultValue: dfv)
                            }
                        }
                    //}
                }
                section("Temp Probes") {
                	switchesForTypes("Temp Probes", "Temp", atomicState.status["istat"]["inputs"])
               	}
                section("Outlets") {
                	switchesForTypes("Outlets", "outlet", atomicState.status["istat"]["outputs"])
                }
                section("Cool") {
                	paragraph("That's all!")
                }
                atomicState.objects = objects
            }
        }
	}
}

def handleHubStatusResponseDuringSetup(resp) {
	log.debug resp
    if (resp.json == null || !resp.json.containsKey("istat")) {
    	atomicState.success = false
        atomicState.message = resp.body
    } else {
    	atomicState.status = resp.json
        atomicState.success = true
    }
    atomicState.done = true
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
	subscribe(location, null, locationHandler, [filterEvents:false])

	def objects = atomicState.objects
    log.debug "objects: $objects"
    if (objects == null) {
    	log.debug "there are no objects to setup.  skipping child device setup step"
    	return
    }
    def existingDevices = [:]
	getChildDevices().each {
    	if (settings["did_" + it.device.deviceNetworkId] == false) {
        	log.debug "removing no longer used ${it.device.deviceNetworkId}"
            try {
        		deleteChildDevice(it.device.deviceNetworkId)
            } catch(e) {
            	log.debug "Error deleting unwanted child ${it.device.deviceNetworkId}: $e"
            }
		} else {
        	existingDevices[it.device.deviceNetworkId] = true
        }
	}

	def typeMap = [
    	"Temp": "Neptune Apex Temp Probe",
        "feedMode": "Neptune Apex Feed Mode",
        "outlet": "Neptune Apex Outlet",
    ]
    
    objects.each { dni, obj ->
    	if (settings["did_" + dni] && !existingDevices[dni]) {
        	log.debug "addChildDevice(${typeMap[obj['type']]}, $dni, null, [label: ${obj['name']}])"
          	addChildDevice(typeMap[obj['type']], dni, null, [label: obj['name']])
        }
    }
    
    runEvery1Minute(poll)
}

def poll() {
	log.debug "Polling"
	def auth = "$username:$password".bytes.encodeBase64()

    def params = [
        "headers": [
            "HOST": "$ip:80",
            "Authorization": "Basic $auth",
        ],
        "method": "GET",
        "path": "/cgi-bin/status.json",
    ]
    log.debug params
    sendHubCommand(new physicalgraph.device.HubAction(params, "$ip:80", [callback: handleHubStatusResponse]))
}

def handleHubStatusResponse(resp) {
	log.debug "handleHubStatusResponse(resp.body)"
    if (resp.json == null || !resp.json.containsKey("istat")) {
    	log.debug "Invalid hub status response: $resp.body"
        return
    }
    log.debug "yo dawg"
    resp.json['istat']['inputs'].each {
    	def child = getChildDevice(it['did'])
        if (child) {
        	child.updateFromApex(it)
        }
    }
    resp.json['istat']['outputs'].each {
    	def child = getChildDevice(it['did'])
        if (child) {
        	child.updateFromApex(it)
        }
    }
    getChildDevices().each {
    	log.debug "looking for feed_ in ${it.device.deviceNetworkId}"
        log.debug "it.device.deviceNetworkId.startsWith('feed_') = ${it.device.deviceNetworkId.startsWith('feed_')}"
    	if (it.device.deviceNetworkId.startsWith("feed_")) {
    		it.updateFromApex(resp.json['istat']['feed'])
        }
    }
    log.debug "all done in handleHubStatusResponse"

}

def feedMode(i) {
	def auth = "$username:$password".bytes.encodeBase64()
    
    def feedCycle = "Feed"
    if (i == null) {
        i = 0
        feedCycle="Feed Cancel"
    }
    def params = [
        "headers": [
            "HOST": "$ip:80",
            "Authorization": "Basic $auth",
            "Content-Type": "application/x-www-form-urlencoded"
        ],
        "method": "POST",
        "path": "/cgi-bin/status.cgi",
        "body": "FeedCycle=$feedCycle&FeedSel=$i&noResponse=1"
    ]
    log.debug params
    sendHubCommand(new physicalgraph.device.HubAction(params, "$ip:80", [callback: handleCommandResponse]))
}

def handleCommandResponse(resp) {
	poll()
}

def setApexOutput(did, value) {
    // 0 = auto
    // 1 = off
    // 2 = on

    log.debug "setApexOutput($did, $value)"
    def auth = "$username:$password".bytes.encodeBase64()
    
    def child = getChildDevice(did)
    def apexOutputName = child.currentApexOutputName
    if (apexOutputName == null) {
        log.debug "Couldn't find the current apexOutputName for $did"
    }
    def params = [
        "headers": [
            "HOST": "$ip:80",
            "Authorization": "Basic $auth",
            "Content-Type": "application/x-www-form-urlencoded"
        ],
        "method": "POST",
        "path": "/cgi-bin/status.cgi",
        "body": "${apexOutputName}_state=${value}&noResponse=1"
    ]
    log.debug params
    sendHubCommand(new physicalgraph.device.HubAction(params, "$ip:80", [callback: handleCommandResponse]))
}

def outletOn(did) {
	setApexOutput(did, 2)
}

def outletOff(did) {
	setApexOutput(did, 1)
}

def outletAuto(did) {
	setApexOutput(did, 0)
}