/**
 *  Thermostat Mode Manager
 *
 *  Copyright 2015 Eric Roberts
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
    name: "Thermostat Mode Manager",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "Manages the thermostat mode depending on various factors.",
    category: "Green Living",
    iconUrl: "http://baldeagle072.github.io/icons/thermostat@1x.png",
    iconX2Url: "http://baldeagle072.github.io/icons/thermostat@2x.png",
    iconX3Url: "http://baldeagle072.github.io/icons/thermostat@3x.png")


preferences {
	section("Thermostat") {
		input "thermostat", "capability.thermostat", title: "Thermostat", multiple: true, required: true
	}
    
    section("Outdoor Temperature Sensor") {
    	input "outdoorTemp", "capability.temperatureMeasurement", title: "Outdoor Temperature", multiple: false, required: false
    }
    
    section("Doors and windows") {
    	input "doorsWindows", "capability.contactSensor", title: "Doors + Windows", multiple: true, required: false
    }
    
    section("Notifications") {
    	input "recieveMsg", "bool", title: "Turn on to recieve notifications"
        input "pushbullet", "device.pushbullet", title: "Pushbullet device", multiple: true, required: false
        input "pushMsg", "bool", title: "Recieve push messages?"
        input "notifyModes", "mode", title: "Modes to recieve notifications in"
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
	state.notNotifiedRecently = true
    state.tstatMode = thermostat.currentValue("thermostatMode")

	subscribe(outdoorTemp, "temperature", "outdoorTempHandler")
    subscribe(thermostat, "thermostatMode", "setTstatModeState")
}

def outdoorTempHandler(evt) {
	def outdoorTemp = evt.value.toDouble()
    log.debug(thermostat.currentValue("temperature"))
    def tstatTemp = thermostat.currentValue("temperature")[0].toDouble()
    def doorWindowOpen = checkDoorsAndWindows()
    if (state.tstatMode == "cool") {
    	log.debug("checking (outdoorTemp > tstatTemp) ${(outdoorTemp > tstatTemp)}, doorWindowOpen $doorWindowOpen")
    	if ((outdoorTemp > tstatTemp) && doorWindowOpen) {
        	notify("It's too warm outside - close everything")
        }
    }
}

def checkDoorsAndWindows() {
	def somethingOpen = false
    for (doorWindow in doorsWindows) {
    	if (doorWindow.currentValue("contact") == "open") {
        	somethingOpen = true
        }
    }
    return somethingOpen
}

def setTstatModeState(evt) {
	log.debug("isDigital: ${evt.isDigital()}")
    if(!(evt.isDigital())) {
    	state.tstatMode = evt.value
        log.debug(state.tstatMode)
    }
}

def notify(msg) {
	log.debug(msg)
    pushbullet.each() {it ->
    	log.debug("it: $it")
        it.push(msg, msg)
    }
}

