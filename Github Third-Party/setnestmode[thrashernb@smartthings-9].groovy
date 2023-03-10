/**
 *  SetNestMode
 *
 *  Copyright 2015 Kenny Keslar
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
    name: "SetNestMode",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "Sets the nest Home/Away based on the current location's mode\r\n",
    category: "Mode Magic",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png")


preferences {
    section("Title") {
        input "present_modes", "mode", title:"Select present mode(s)", multiple: true, required:true
        input "away_modes", "mode", title:"Select away mode(s)", multiple: true, required:true
        input "thermostat", "capability.thermostat", title:"Select Nest thermostat", multiple: false, required:true
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
def modeChangeHandler(event) {
	log.debug "Location: $location = ${location.currentMode}"
    def set = false
 	def away = false
    if (away_modes instanceof String) {
    	if (location.currentMode == away_modes) {
        	set = true
            away = true
        }
    }
    else if (away_modes.contains(location.currentMode)) {
    	set = true
        away = true
    }
    
    if (present_modes instanceof String) {
    	if (location.currentMode == present_modes) {
        	set = true
        }
    }
    else if (present_modes.contains(location.currentMode)) {
    	set = true
    }
    
    if (set && !away) {
    	log.debug "Set to present"
        thermostat.present()
    }
    else if (set) {
    	log.debug "Set to away"
        thermostat.away()
    }

}

def initialize() {
	subscribe(location, "mode", modeChangeHandler)
	// TODO: subscribe to attributes, devices, locations, etc.
}

