/**
 *  Light Physical Button Time Settings
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
    name: "Light Physical Button Time Settings",
    namespace: "piratemedia/smartthings",
    author: "Eliot Stocker",
    description: "Time specific setting for light setup",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-LightPhysicalControl/master/logo.png",
    parent: "piratemedia/smartthings:Light Physical Button Setup"
)


preferences {
	section("Time Range") {
    	input "start", "time", title:"Start Time", required: true
        input "end", "time", title:"End Time", required: true
    }
	section("Select Lights Settings when turned on within selected time range") {
        input "level", "number", title: "Light Brightness", range: "(1..100)", required: false
        input "temp", "number", title: "Light Color Temperature", range: "(2200..6500)", required: false
        input "color", "enum", title: "Color", options: ["Red", "Orange", "Yellow", "Green", "Blue", "Indigo", "Violet"], required: false
    }
    section("Advanced: Per Light Settings") {
    }
    section() {
    	app(name: "singleLight", appName: "Light Physical Single Light Setting", namespace: "piratemedia/smartthings", title: "New Single Light Setting", multiple: true)
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
}

def isActive() {
	if(timeOfDayIsBetween(start, end, new Date(), location.timeZone)) {
    	return true;
    }
    return false;
}

def getSettings() {
	return [
    	level: level,
        temp: temp,
        color: color
    ]
}

def getSpecificLightSetting(label) {
	def children = getChildApps()
    def data = null;
    children.each { child ->
    	def settings = child.getLightSettings();
        if(settings.light == label) {
        	data = settings
        }
    }
    return data
}

def checkDeviceForCapabilityById(id, capability) {
	return parent.checkDeviceForCapabilityById(id, capability)
}

def hasSpecificSettings() {
	def children = getChildApps()
    return children.size > 0
}

def getLightDevices() {
	return parent.getLightDevices()
}