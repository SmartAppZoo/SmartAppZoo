/**
 *  LircWeb Control SmartApp
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
    name: "LircWeb Control App",
    namespace: "shamlian",
    author: "Steven Shamlian",
    description: "App to trigger a macro from LircWeb.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Device that activates the TV") {
		input "theswitch", "capability.switch", required: true
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
    subscribe(theswitch, "switch.on",  powerHandler)
    subscribe(theswitch, "switch.off", powerHandler)
}

def powerHandler(evt) {
    def path = "/macros/Cable"
    def headers = [:] 
    headers.put("HOST", "PUT.YOUR.HOST.HERE:AND_PORT")  // Edit this line!
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    def method = "POST"

    sendHubCommand(
        new physicalgraph.device.HubAction(
            method: method,
        	path: path,
        	body: command,
        	headers: headers
        )
    )
}