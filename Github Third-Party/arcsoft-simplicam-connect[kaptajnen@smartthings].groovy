/**
 *  Arcsoft Simplicam
 *
 *  Copyright 2016 Jonas Laursen
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
    name: "Arcsoft Simplicam (Connect)",
    namespace: "dk.decko",
    author: "Jonas Laursen",
    description: "Turn Arcsoft Simplicams on/off",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@3x.png")


preferences {
	section("Closeli Credentials") {
		input(name: "email", type: "email", title: "Email address", description: "Enter Email Address", required: true)
        input(name: "password", type: "password", title: "Password", description: "Enter Password", required: true)
	}
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
	// TODO: subscribe to attributes, devices, locations, etc.
    if (closeliGetSession()) {
    	closeliGetDevices()
    }
    
    runEvery5Minutes(syncState)
}

def closeliGetSession() {
	try {
    	httpPost("https://client.closeli.com/login", "email=${settings.email}&password=${settings.password}") { resp ->
        	log.debug("Got a response for get session")
			if (resp.contentType == "application/json") {
            	if (resp.data["success"] == true) {
                	state.cookie = ""
                	resp.getHeaders('set-cookie').each {
                    	state.cookie = state.cookie + it.value.split(";")[0] + ";"
                    }
                    return true
                }
                else {
                	log.debug("Unable to log in: ${resp.data}")
                }
            }
            else {
            	log.debug("Reply was not json: ${resp.data}")
            }
        }
    } catch (e) {
    	log.debug("Exception raised during login: $e")
    }
}

def closeliGetCSRF() {
	try {
        httpGet(["uri": "https://client.closeli.com", "headers": ["Cookie": state.cookie]]) { resp ->
        	log.debug("Got a response for get csrf")
            def csrf = ""
            resp.data[0].children[0].children.each { node ->
                if (node.attributes()["name"] == "_csrf") {
                    csrf = node.attributes()["content"]
                }
            }

            if (csrf != "") {
                return csrf
            } else {
    			log.debug("Could not find CSRF in response")
            }
        }
    } catch (e) {
    	log.debug("Exception raised while trying to get CSRF token: $e")
    }
}

def closeliGetDevices() {
	closeliGetSession()
	try {
        httpGet(["uri": "https://client.closeli.com/device/list", "headers": ["Cookie": state.cookie]]) { resp ->
        	log.debug("Got a response for get devices: ${resp.data}")
            return resp.data.list.devicelist
        }
    } catch (e) {
    	log.debug("Exception raised while getting devices: $e")
    }
}

def addDevices() {
	def deviceData = closeliGetDevices()
    
    deviceData.each {
        log.debug(it)
        def dni = [app.id, it.deviceid].join(".")

        if (! getChildDevice(dni)) {
            def d = addChildDevice(app.namespace, "Arcsoft Simplicam", dni, null, ["label": it.devicename])
            d.setDeviceId(it.deviceid)
        } else {
            log.debug("Device with id ${dni} already exists")
        }
    }
}

def closeliSaveSetting(did, path, element) {
	closeliGetSession()
	def csrf = closeliGetCSRF()
    if (csrf != "") {
    	try {
            httpPost(["uri": "https://client.closeli.com/device/saveSetting", "headers": ["Cookie": state.cookie, "x-requested-with": "XMLHttpRequest"], "body": "deviceId=${did}&path=${path}&element=${element}&_csrf_header=${csrf}"]) { resp ->
            	log.debug("Got a response for save setting: ${resp.data}")
                if (resp.data.success != true) {
                	log.debug("Failed to save setting")
                }
            }
        } catch (e) {
        	log.debug("Exception raised while trying to save setting: $e")
        }
    }
}

def closeliCameraEnabled(did, enabled) {
	log.debug("Set status of ${did} to ${enabled}")
    def element = enabled ? "<status>On</status>" : "<status>OffByManual</status>"
    closeliSaveSetting(did, "profile/general/status", element)
}

def syncState() {
	def deviceData = closeliGetDevices()
    deviceData.each {
    	def dni = [app.id, it.deviceid].join(".")
        def device = getChildDevice(dni)
        
        if (device) {
        	def cloudStatus = (it.deviceStatus == "On" ? "on" : "off")
            def devStatus = device.currentSwitch
        	if (devStatus != cloudStatus) {
            	log.debug("Camera is not in sync with cloud. Cloud is ${cloudStatus}, device is ${devStatus}")
                device.sendEvent(name: "switch", value: cloudStatus, isStateChange: true)
            }
        	//sendEvent(device, [])
        }
    }
}


// TODO: implement event handlers