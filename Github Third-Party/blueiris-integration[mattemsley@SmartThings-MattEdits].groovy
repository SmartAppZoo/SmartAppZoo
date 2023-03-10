/**
 *  BlueIris Integration
 *
 *  Copyright 2016 Matthew Emsley
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
    name: "BlueIris Integration",
    namespace: "mattemsley",
    author: "Matthew Emsley",
    description: "BlueIris Endpoint mapping",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

import groovy.json.JsonBuilder

preferences {
    page(name: "copyConfig", install: true, uninstall: true)
    page(name: "setupDevices")
}

def copyConfig() {
    if (!state.accessToken) {
        createAccessToken()
    }
    dynamicPage(name: "copyConfig", title: "Config", install:true, uninstall: true) {
        section() {
            href page:"setupDevices", required:false, title:"Devices", description:"Select Devices for OAuth Control"
        }

        section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
            href url:"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/devices?access_token=${state.accessToken}", style:"embedded", required:false, title:"Debug", description:"View accessories JSON"
        }
    }
}

def setupDevices() {
    if (!state.accessToken) {
        createAccessToken()
    }
    dynamicPage(name: "setupDevices", title: "Setup Devices") {
        section("Select devices to include in the /devices API call") {
            input "motions", "capability.motionSensor", title: "Motion Sensor", multiple: false, required: false
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
        }
    }
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "BlueIris Integration",
        platforms: [
            [
                platform: "SmartThings",
                name: "SmartThings",
                app_id:        app.id,
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

def deviceCommandMap(device, type) {
  device.supportedCommands.collectEntries { command->
      def commandUrl = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/${command.name}?access_token=${state.accessToken}"
      [
        (command.name): commandUrl
      ]
  }
}

def authorizedDevices() {
    [
        motions: motions
    ]
}

def renderDevices() {
    def deviceData = authorizedDevices().collectEntries { devices->
        [
            (devices.key): devices.value.collect { device->
                [
                    name: device.displayName,
                    commands: deviceCommandMap(device, devices.key)
                ]
            }
        ]
    }
    def deviceJson    = new groovy.json.JsonOutput().toJson(deviceData)
    def deviceString  = new groovy.json.JsonOutput().prettyPrint(deviceJson)
    render contentType: "application/json", data: deviceString
}

def deviceCommand() {
  def device  = authorizedDevices()[params.type].find { it.id == params.id }
  def command = params.command
  if (!device) {
      httpError(404, "Device not found")
  } else {
      if (params.value) {
        device."$command"(params.value)
      } else {
        device."$command"()
      }
  }
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/devices")                      { action: [GET: "authError"] }
        path("/config")                       { action: [GET: "authError"] }
        path("/active") 					  { action: [GET: "authError"] }
    	path("/inactive") 					  { action: [GET: "authError"] }
    } else {
        path("/devices")                      { action: [GET: "renderDevices"]  }
        path("/config")                       { action: [GET: "renderConfig"]  }
        path("/active") 					  { action: [GET: "activeMotion"] }
    	path("/inactive") 					  { action: [GET: "inactiveMotion"] }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	installed()
    getURL(null)
}

def getURL(e) {
    if (resetOauth) {
        log.debug "Reseting Access Token"
        state.accessToken = null
    }

    if (!state.accessToken) {
        createAccessToken()
        log.debug "Creating new Access Token: $state.accessToken"
    }

    def url1 = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/data"
    def url2 = "?access_token=${state.accessToken}"
    log.debug "${title ?: location.name} Data URL: $url1$url2"
}

void activeMotion() {
	log.debug "Updated with settings: ${settings}"
    motions?.active()
    runIn(15,"inactiveMotion")
}

void inactiveMotion() {
	log.debug "Updated with settings: ${settings}"
    motions?.inactive()
}