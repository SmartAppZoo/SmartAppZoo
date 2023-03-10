/**
 *  Light Physical Single Light Setting
 *
 *  Copyright 2018 Eliot Stocker
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
    name: "Light Physical Single Light Setting",
    namespace: "piratemedia/smartthings",
    author: "Eliot Stocker",
    description: "child app to set a single lights settings",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
    parent: "piratemedia/smartthings:Light Physical Button Time Settings"){
    appSetting "devices"
}


preferences {
    page(name: "LightSettingsPage")
}

def SelectedDevice() {
	def light = null
	appSettings.devices.each{ l ->
    	if(l.label == selected) {
        	light = l
        }
    }
	return light
}

def LightSettingsPage() {
    dynamicPage(name: "LightSettingsPage", install: true, uninstall: true) {
    	if(getListOfDevices()) {
            section("Select Light(s) For settings") {
                input "selected", "enum", title: "Light", submitOnChange: true, options: lightChoices()
            }
        }
    
    	if(SelectedDevice() != null) {
            section("Select Light Settings") {
                input "io", "bool", title: "On/Off", required: true, defaultValue: true
                if(canControlLevel()) {
                    input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
                }
                if(canControlColorTemperature()) {
                    input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
                }
                if(canControlColor()) {
                    input "color", "enum", title: "Color", options: ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet"], required: false
                }
            }
            
            section("Single Light Setup Name") {
        		label title: "Setup Name", required: false, defaultValue: selected
        	}
        }
    }
}

def checkDeviceForCapability(capability) {
	return parent.checkDeviceForCapabilityById(SelectedDevice().id, capability)
}

def canControlLevel() {
	return checkDeviceForCapability('Switch Level')
}

def canControlColorTemperature() {
	return checkDeviceForCapability('Color Temperature')
}

def canControlColor() {
	return checkDeviceForCapability('Color Control')
}

def lightChoices() {
	def names = []
	appSettings.devices.each{ light ->
    	names.add(light.label)
    }
	return names
}

def getListOfDevices() {
	appSettings.devices = parent.getLightDevices()
    return true
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
}

def getLightSettings() {
    return [
        light: selected,
        on: io,
        level: level,
        temp: temp,
        color: color
    ]
}

// TODO: implement event handlers