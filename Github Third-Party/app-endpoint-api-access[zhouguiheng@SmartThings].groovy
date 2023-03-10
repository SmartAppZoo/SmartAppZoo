/**
 *  App Endpoint API Access
 *
 *  Copyright 2017 Vincent
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
    name: "App Endpoint API Access",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Endpoint API",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Allow Endpoint to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "presences", "capability.presenceSensor", title: "Which Presence sensors?", multiple: true, required: false
	}
}

mappings {
	path("/list") {
    	action: [
        	GET: "listDevices"
        ]
    }
	path("/switch/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}
	path("/presence/:id/:command") {
		action: [
			GET: "updatePresence"
		]
	}
}

def installed() {
	createAccessToken()
	if (!state.accessToken) {
		try {
			getAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.debug "Did you forget to enable OAuth in SmartApp IDE settings?"
            log.debug ex
		}
	}
}

def updated() {}

def listDevices() {
	def html = "<html><body>"
    html += getDeviceHtml(switches, "switch", ["on", "off"])
    html += getDeviceHtml(presences, "presence", ["arrived", "departed"])
    html += "</body></html>"
    render contentType: "text/html", data: html, status: 200
}

def getDeviceHtml(devices, endpoint, commands) {
	def html = ""
    def prefix = "https://graph-na02-useast1.api.smartthings.com/api/smartapps/installations/$app.id"
	for (device in devices) {
    	def label = getLabel(device)
    	html += "<h2>${label}</h2><table border=1 cellpadding=5>"
        for (cmd in commands) {
            html += "<tr><td>${cmd}</td><td>${prefix}/${endpoint}/${device.id}/${cmd}?access_token=${state.accessToken}</td></tr>"
        }
	    html += "</table>"
    }
    return html
}

void updateSwitch() {
	update(switches, ["on", "off", "stateOn", "stateOff"])
}

void updatePresence() {
	update(presences, ["arrived", "departed"])
}

private void update(devices, commands) {
	log.debug "update, request: params: ${params}, devices: $devices.id"

    def command = params.command
	if (command && command in commands) {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
			device."$command"()
		}
	} else {
    	httpError(403, "Command is not allowed")
    }
}

private getLabel(device) {
	device.label ? device.label : device.name
}