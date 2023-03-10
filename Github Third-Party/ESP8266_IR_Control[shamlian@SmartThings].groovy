/**
 *  ESP8266 IR Control SmartApp
 *
 *  Copyright 2017 Steven Shamlian
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
    name: "ESP8266 Control App",
    namespace: "shamlian",
    author: "Steven Shamlian",
    description: "App to trigger signals from ESP8266. See https://github.com/mdhiggins/ESP8266-HTTP-IR-Blaster for hardware. Suitable for controlling with a switch.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Device that activates the TV") {
		input "theswitch", "capability.switch", required: true
	}

	section("ESP8266 Settings") {
    	input "ipAddr", "text", title: "IP Address", description: "dotted octets here", required: true
        input "password", "password", title: "Password", description: "password", required: true
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
    subscribe(theswitch, "switch.on",  powerHandler)
    subscribe(theswitch, "switch.off", powerHandler)
}

def powerHandler(evt) {
    def powerPath = "/msg?code=E0E040BF:SAMSUNG:32&pulse=3&pass=" + password + "&simple=1"
    def switchPath = "/msg?code=FF50AF:NEC:32&pulse=1&pass=" + password + "&simple=1"

    def headers = [:] 
    headers.put("HOST", ipAddr + ":80")
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    def method = "GET"

    sendHubCommand(
        new physicalgraph.device.HubAction(
            method: method,
        	path: powerPath,
        	body: command,
        	headers: headers
        )
    )

    sendHubCommand(
        new physicalgraph.device.HubAction(
            method: method,
        	path: switchPath,
        	body: command,
        	headers: headers
        )
    )
    
    log.debug "Sent power command."
}