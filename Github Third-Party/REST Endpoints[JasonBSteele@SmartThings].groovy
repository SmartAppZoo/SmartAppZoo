/**
 *  REST Endpoints
 *
 *  Copyright 2015 Jason Steele
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "REST Endpoints",
	namespace: "JasonBSteele",
	author: "Jason Steele",
	description: "Provides REST Endpoints ",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	oauth: true)


preferences {
	section("Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", required:false, multiple: true
		input "dimmers", "capability.switchLevel", title: "Which Dimmers?", required:false, multiple: true
		input "locks", "capability.lock", title: "Which Locks?", required:false, multiple: true
	}
	section("Sense These Things...") {
		input "temperatures", "capability.temperatureMeasurement", title: "Which Temperatures?", required:false, multiple: true
	}
}

mappings {

	path("/phrases") {
		action: [
			GET: "listPhrases"
		]
	}
	path("/phrase/:helloHomePhrase") {
		action: [
			GET: "executePhrase"
		]
	}
	path("/mode/:newMode") {
		action: [
			GET: "updateMode"
		]
	}
	
	path("/mode") {
		action: [
			GET: "showMode"
		]
	}
	path("/mode/:newMode") {
		action: [
			GET: "updateMode"
		]
	}
	
	path("/switches") {
		action: [
			GET: "listSwitches"
		]
	}
	path("/switches/:id") {
		action: [
			GET: "showSwitch"
		]
	}
	path("/switches/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}
	
	path("/dimmers") {
		action: [
			GET: "listDimmers"
		]
	}
	path("/dimmers/:id") {
		action: [
			GET: "showDimmer"
		]
	}
	path("/dimmers/:id/:level") {
		action: [
			GET: "updateDimmer"
		]
	}
	
	path("/locks") {
		action: [
			GET: "listLocks"
		]
	}
	path("/locks/:id") {
		action: [
			GET: "showLock"
		]
	}
	path("/locks/:id/:command") {
		action: [
			GET: "updateLock"
		]
	}
	
	path("/temperatures") {
		action: [
			GET: "listTemperatures"
		]
	}
	path("/temperatures/:id") {
		action: [
			GET: "showTemperature"
		]
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

//phrase
def listPhrases () {
	def phrases = location.helloHome?.getPhrases()
	phrases?.collect{'"phrase":"' + it.label +'"'}
}

void executePhrase() {
	log.debug "executePhrase"
	def phrase = params.helloHomePhrase
	log.debug "Executing phrase ${phrase}"
	location.helloHome.execute(phrase)
}


//mode
def showMode() {
	[mode: location.mode]
}

void updateMode() {
	def newMode = params.newMode
	setLocationMode(newMode)
}

//switches
def listSwitches() {
	switches.collect{deviceToJson(it,"switch")}
}

def showSwitch() {
	queryDeviceToJson(switches, "switch")
}
void updateSwitch() {
	updateToggle(switches)
}

//dimmers
def listDimmers() {
	dimmers.collect{deviceToJson(it, "level")}
}

def showDimmer() {
	queryDeviceToJson(dimmers, "level")
}
void updateDimmer() {
	updateLevel(dimmers)
}

//locks
def listLocks() {
	locks.collect{deviceToJson(it,"lock")}
}

def showLock() {
	queryDeviceToJson(locks, "lock")
}

void updateLock() {
	updateToggle(locks)
}

//temperatures
def listTemperatures() {
	temperatures.collect{deviceToJson(it,"temperature")}
}

def showTemperature() {
	queryDeviceToJson(temperatures, "temperature")
}

def deviceHandler(evt) {}

private void updateToggle(devices) {
	log.debug "updateToggle request: params: ${params}, devices: $devices.id"
	
	
	//def command = request.JSON?.command
	def command = params.command
	//let's create a toggle option here
	if (command) 
	{
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} 
        else {
			if(command == "toggle") {
				if(device.currentValue('switch') == "on")
				  device.off();
				else
				  device.on();
	   		}
	   		else {
				device."$command"()
			}
		}
	}
}

private void updateLevel(devices) {
	log.debug "updateLevel request: params: ${params}, devices: $devices.id"
	
	//def level = params.level
	Integer level = params.int('level')

	if (level) {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} 
        else {
			device.setLevel(level)
		}
	}
}


private deviceToJson(device, attributeName) {
	if (!device) 
		null
		
	def s = device.currentState(attributeName)
	def deviceMap = [id: device.id, name: device.displayName, unitTime: s?.date?.time]
	deviceMap[attributeName] = s?.value
	deviceMap
}

private queryDeviceToJson(devices, attributeName) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		deviceToJSON(device)
	}
}
