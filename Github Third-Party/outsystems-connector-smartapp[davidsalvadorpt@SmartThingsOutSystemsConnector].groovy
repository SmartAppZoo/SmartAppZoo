/**
 *  OutSystems Connector SmartApp
 *
 *  Copyright 2017 David Salvador
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
    name: "OutSystems Connector SmartApp",
    namespace: "davidsalvadorpt",
    author: "David Salvador",
    description: "SmartApp to bridge your smartdevices to your OutSystems Enviroment",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
	section("Devices") {
            input "switches", "capability.switch", title: "Switches:", multiple: true, required: false
            input "dimmers", "capability.switchLevel", title: "Dimmers:", multiple: true, required: false
    }
}

mappings {
/*	path("/sendPush/:message") {
		action: [
			GET: "sendPush",
            PUT: "sendPush"
		]
	} */
   	path("/switches/:command") {
        action: [PUT: "updateSwitches"]
    }
    
    path("/dimmers/:command") {
        action: [PUT: "updateDimmers"]
    }
}

def updateSwitches() {
    def cmd = params.command
    log.debug "updateSwitches command: $cmd"
    switch(cmd) {
        case "on":
            switches.on()
            break
        case "off":
            switches.off()
            break
        default:
            httpError(501, "$command is not a valid command for all switches specified")
    }
}

def updateDimmers() {
    def cmd = params.command
    log.debug "updateDimmers command: $cmd"
   	dimmers.setLevel(cmd.toInteger())
    log.debug "updateDimmers end"
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
    log.trace "https://graph.api.smartthings.com/api/token/${state.accessToken}/smartapps/installations/${app.id}/Notify"
}

def sendPush() {
	if (params.message && params.message != "") {
    	sendPush(params.message)
    }
}