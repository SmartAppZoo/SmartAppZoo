/**
 *  AirVisual
 *
 *  Copyright 2017 E.O. Stinson
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
    name: "AirVisual Node",
    namespace: "yostinso",
    author: "E.O. Stinson",
    description: "Child app for main AirVisual app. Do not add directly.",
    category: "Health & Wellness",
    iconUrl: "https://d25jl8yaav4s0u.cloudfront.net/images/app/health-recommendations-icon.png",
    parent: "yostinso:AirVisual"
)


preferences {
	page(name: "newNode")
    page(name: "addNode")
}

def newNode() {
    dynamicPage(name: "newNode", title: "Setup", install: false, uninstall: true, nextPage: "addNode") {
    	if (state.nodeName) {
        	section("Current node") {
            	paragraph "${state.nodeName}"
            }
        }
    	section("Configure Node") {
            input(
                capitalization: "none",
                name: "shareId",
                title: "API path",
                description: "airvisual.com/api/v2/node/<API path>",
                type: "text"
            )
    	}
	}
}

def addNode() {
	def name = getNodeName()
    if (name) {
        dynamicPage(name: "addNode", title: "Node", install: true, uninstall: false) {
            section("Status") {
                if (name) {
                    paragraph "Found node \"${name}\""
                } else {
                    paragraph "Node not found. Please check your API path.", required: true
                }
            }
        }
    } else {
        dynamicPage(name: "addNode", title: "Node", install: false, uninstall: false) {
            section("Status") {
                if (name) {
                    paragraph "Found node \"${name}\""
                } else {
                    paragraph "Node not found. Please check your API path.", required: true
                }
            }
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def uninstalled() {
  	removeDevice()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def removeDevice() {
    getChildDevices().each { device ->
      deleteChildDevice(device.deviceNetworkId)
    }
}

def initialize() {
    try {
        def existingDevice = getChildDevice(settings.shareId)
        if (!existingDevice || existingDevice == null) {
            removeDevice()
            def nodeName = getNodeName()
            if (!nodeName) {
            	state.validNode = false
            } else {
                def childDevice = addChildDevice("yostinso", "AirVisual Node Pro", settings.shareId, null, [
                    completedSetup: true,
                    name: "AirVisual.${settings.shareId}",
                    label: "AirVisual ${nodeName}"
                ])
                log.debug "Added AirVisual device ${nodeName}"
                state.validNode = true
			}
        } else {
        	log.debug "Using existing device ${existingDevice.getLabel()}"
        }
    } catch (e) {
        log.error "Error creating device ${settings.shareId}: ${e}"
    }
}

def baseUri() { return "https://airvisual.com/" }
def nodePath() { return "/api/v2/node/${settings.shareId}" }

def getNodeName() {
	try {
        httpGet(
            uri: baseUri(),
            path: nodePath(),
            contentType: "application/json"
        ) { resp ->
        	state.nodeName = resp.data.settings.node_name
            return resp.data.settings.node_name
        }
	} catch (e) {
    	log.error "Failed to fetch node name: ${e}"
        state.nodeName = null
        return null
    }
}

def getData() {
	log.debug "Starting data fetch for ${state.nodeName}"
	try {
        httpGet(
            uri: baseUri(),
            path: nodePath(),
            contentType: "application/json"
        ) { resp ->
        	return resp.data
        }
    } catch (e) {
    	log.error "Failed to get data for node ${state.nodeName} ${e}"
        return [:]
    }
}