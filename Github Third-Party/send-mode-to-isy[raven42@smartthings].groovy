/**
 *  Send Mode to ISY
 *
 *  Copyright 2020 David Hegland
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
    name: "Send Mode to ISY",
    namespace: "raven42",
    author: "David Hegland",
    description: "Send the SmartThings mode to the ISY944i",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
	section("Title") {
		input "isyAddress", "text", title: "ISY Address", required: false		// Address of the ISY Hub
		input "isyPort", "number", title: "ISY Port", required: false			// Port to use for the ISY Hub
        input "isyUserName", "text", title: "ISY Username", required: false		// Username to use for the ISY Hub
        input "isyPassword", "text", title: "ISY Password", required: false		// Password to use for the ISY Hub
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
	subscribe(location, "mode", modeChangeHandler)
}

private getAuthorization() {
    def userpassascii = settings.isyUserName + ":" + settings.isyPassword
    "Basic " + userpassascii.encodeAsBase64().toString()
}

def getRequest(path) {
	log.debug "getRequest() path: [" + path + "]"
    new physicalgraph.device.HubAction(
        'method': 'GET',
        'path': path,
        'headers': [
            'HOST': settings.isyAddress+":"+settings.isyPort,
            'Authorization': getAuthorization()
        ], null)
}

def sendMode(mode) {
	def type_var = 1
    def type_state = 2
    def SMARTTHINGS_MODE_REQ = 79
	def command = '/rest/vars/set/' + type_state + '/' + SMARTTHINGS_MODE_REQ + '/' + mode
    
    log.debug "sendMode() command: [" + command + "]"

    sendHubCommand(getRequest(command))
}

def modeChangeHandler(evt) {
	log.debug "modeChangeHandler() mode changed '${evt.value}'"

    def mode_map = [ Home: 20, Away: 21, Sleep: 22, Off: 23 ]

    if (location.modes?.find{it.name == evt.value}) {
    	log.debug "modeChangeHandler() found mode '${evt.value}' in location"
        if (mode_map.containsKey(evt.value)) {
        	log.debug "modeChangeHandler() found mode '${evt.value}' in mode_map"
            sendMode(mode_map[evt.value])
        } else {
        	log.debug "modeChangeHandler() mode ['${evt.value}'] not found in mode_map"
        }
    } else {
    	log.debug "modeChangeHandler() mode ['${evt.value}'] not found in location"
    }
}
