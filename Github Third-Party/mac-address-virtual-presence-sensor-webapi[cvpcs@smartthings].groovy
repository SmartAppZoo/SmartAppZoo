/**
 *  MAC Address Virtual Presence Sensor WebAPI
 *
 *  Copyright 2018 AUSTEN DICKEN
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
    name: "MAC Address Virtual Presence Sensor WebAPI",
    namespace: "cvpcs",
    author: "AUSTEN DICKEN",
    description: "Manages the creation of Virtual Presence Sensor devices based on MAC addresses to be managed via a WebAPI.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi.png",
    iconX2Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi@2x.png",
    singleInstance: true)

preferences {
    page(name: "mainPage", title: "Existing sensors", install: true, uninstall: true) {
        if(state?.installed) {
            section("Add a new sensor") {
                app "MAC Address Virtual Presence Sensor", "cvpcs", "MAC Address Virtual Presence Sensor", title: "New Sensor", page: "mainPage", multiple: true, install: true
            }
        } else {
            section("Initial Install") {
                paragraph "This smartapp installs the MAC Address Virtual Presence Sensor app so you can add multiple child sensors. Click install / done then go to smartapps in the flyout menu and add new sensors or edit existing sensors."
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    createAccessToken()
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    state.installed = true
    
    if (!state.accessToken) {
        try {
            getAccessToken()
            log.debug "Creating new Access Token: ${state.accessToken}"
        } catch (ex) {
            log.debug "Did you forget to enable OAuth in SmartApp IDE settings for SmartTiles?"
            log.debug ex
        }
    }
}

mappings {
    path("/:macAddress/:status") {
        action: [
            GET: "setPresence"
        ]
    }
}

def setPresence() {
	log.debug "Attempting to set device with MAC ${params.macAddress} to status ${params.status}"

    def apps = getChildApps();
    def app = apps.find {app -> app.macAddress.equalsIgnoreCase(params.macAddress) }
    if (app == null) {
		log.debug "Could not find any devices with MAC address ${params.macAddress}";
    	return httpError(400, "No device found with MAC address ${params.macAddress}");
    }
    
    def status = ""
    switch (params.status) {
        case "present":
            status = "present"
            break
        case "absent":
        case "notPresent":
            status = "not present"
            break
        default:
        	log.debug "Status ${params.status} was not recognized as valid"
            return httpError(400, "Status ${params.status} is not a valid status");
    }
    
    log.debug "Setting device ${app.label} to status ${status}"
    app.setPresence(status)
}
