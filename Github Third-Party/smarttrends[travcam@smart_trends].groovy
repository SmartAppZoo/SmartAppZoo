/**
 *  SmartTrends
 *
 *  Copyright 2016 Tyler P
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
 *
 *  Initial code base on example project by Jody Albrittion
 *	//https://github.com/jodyalbritton/api-endpoint/blob/master/api-example.groovy
 *
 */
 
import groovy.json.JsonBuilder
 
definition(
    name: "SmartTrends",
    namespace: "tpude",
    author: "Tyler Pudenz",
    description: "SmartThings SmartApp for logging events/values from SmartThings to parse.com",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    oauth: true)

//Options for user selection in the SmartApp
preferences {
	page(name: "pageOne", title: "Log events from the following devices:", nextPage: "pageTwo", uninstall: true) {
		section("Allow Endpoint to Control These Things...") {
			input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
			input "dimmers", "capability.switchLevel", title: "Which Dimmers?", multiple: true, required: false
			input "thermostat", "capability.thermostat", title: "Which Thermostats?", multiple: true, required: false
			input "motions", "capability.motionSensor", title: "Which Motions?", multiple: true, required: false
			input "accelerations", "capability.accelerationSensor", title: "Which Accelerations?", multiple: true, required: false
			input "contacts", "capability.contactSensor", title: "Which Contacts?", multiple: true, required: false
			input "illuminants", "capability.illuminanceMeasurement", title: "Which Illuminance Sensors?", multiple: true, required: false
			input "temperatures", "capability.temperatureMeasurement", title: "Which Temperatures?", multiple: true, required: false
			input "humidities", "capability.relativeHumidityMeasurement", title: "Which Humidities?", multiple: true, required: false
			input "presence", "capability.presenceSensor", title: "Which Presence?", multiple: true, required: false
			input "lock", "capability.lock", title: "Which Locks?", multiple: true, required: false
			input "batteries", "capability.battery", title: "Which Batteries?", multiple: true, required: false
			input "powers", "capability.powerMeter", title: "Power Meters", required:false, multiple: true
		}
	}
	page(name: "pageTwo", title: "Enter parse.com API keys", install: true, uninstall: true) {
		section("Allow Endpoint to Control These Things...") {
			input "restAPIKey", "text", title: "Parse.com REST API Key"
			input "appKey", "text", title: "Parse.com API Application ID"
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	//unsubscribe from user removed items before re-init
	unsubscribe()
	initialize()
}

//subscribe to the following events
def initialize() {
	subscribe(switches, "switch", switchEventHandler)
	subscribe(dimmers, "level", switchLevelEventHandler)
	subscribe(motions, "motion", motionEventHandler)
	subscribe(accelerations, "acceleration", accelEventHandler)
	subscribe(contacts, "contact", contactEventHandler)
	subscribe(illuminants, "illuminance", illumEventHandler)
	subscribe(temperatures, "temperature", tempEventHandler)
	subscribe(humidities, "humidity", humidityEventHandler)
	subscribe(lock, "lock", lockEventHandler)
	subscribe(batteries, "battery", batteryEventHandler)
	subscribe(powers, "power", powerEventHandler)
	subscribe(energys, "energy", energyEventHandler)
	subscribe(presence, "presence", presenceEventHandler)
    
	subscribe(thermostat, "heatingSetpoint", tstatHeatSPEventHandler)
	subscribe(thermostat, "coolingSetpoint", tstatCoolSPEventHandler)
	subscribe(thermostat, "thermostatSetpoint", tstatSPEventHandler)
	subscribe(thermostat, "thermostatMode", tstatModeEventHandler)
	subscribe(thermostat, "thermostatFanMode", tstatFanModeEventHandler)
	subscribe(thermostat, "thermostatOperatingState", tstatOpStateEventHandler)
	subscribe(thermostat, "thermostatFanState", tstatFanStateEventHandler)
}

//Paths to expose endpoints for sending data from each device type
mappings {
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
	path("/switches/:id/:events") {
		action: [
			GET: "showSwitchEvents"
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
	path("/dimmers/:id/:command") {
		action: [
			GET: "updateDimmer"
		]
	}
	path("/switches/:id/:command/:level") {
		action: [
			GET: "updateSwitch"
		]
	}
	path("/motions") {
		action: [
			GET: "listMotions"
		]
	}
	path("/motions/:id") {
		action: [
			GET: "showMotion"
		]
	}
	path("/motions/:id/events") {
		action: [
			GET: "showMotionEvents"
		]
	}
	path("/illuminants") {
		action: [
			GET: "listIlluminants"
		]
	}
	path("/illuminants/:id") {
		action: [
			GET: "showIlluminant"
		]
	}
	path("/contacts") {
		action: [
			GET: "listContacts"
		]
	}
	path("/contacts/:id") {
		action: [
			GET: "showContact"
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
	path("/temperatures/:id/:command") {
		action: [
			GET: "updateTemperatures"
		]
	}
	path("/humidities") {
		action: [
			GET: "listHumidities"
		]
	}
	path("/humidities/:id") {
		action: [
			GET: "showHumidity"
		]
	}
	path("/batteries") {
		action: [
			GET: "listBatteries"
		]
	}
	path("/batteries/:id") {
		action: [
			GET: "showBattery"
		]
	}
	path("/powers") {
		action: [
			GET: "listPowers"
		]
	}
	path("/powers/:id") {
		action: [
			GET: "showPower"
		]
	}
	path("/energies") {
		action: [
			GET: "listEnergies"
		]
	}
	path("/energies/:id") {
		action: [
			GET: "showEnergy"
		]
	}
	path("/thermostats") {
		action: [
			GET: "listThermostats"
		]
	}
	path("/thermostats/:id") {
		action: [
			GET: "showThermostat"
		]
	}  
	path("/thermostats/:id/:command/:temp") {
		action: [
			GET: "updateThermostat"
		]
	}
	path("/presence") {
		action: [
			GET: "listPresence"
		]
	}  
	path("/presences/:id") {
		action: [
			GET: "showPresence"
		]
	}     
}

// Event handlers for each subscription event above.
def switchEventHandler(evt) {
    logField(evt) { it.toString() }
}
def switchLevelEventHandler(evt) {
    logField(evt) { it.toString() }
}
def motionEventHandler(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}
def accelEventHandler(evt) {
    logField(evt) { it == "active" ? "1" : "0" }
}
def contactEventHandler(evt) {
    logField(evt) { it == "open" ? "1" : "0" }
}
def illumEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tempEventHandler(evt) {
    logField(evt) { it.toString() }
}
def humidityEventHandler(evt) {
    logField(evt) { it.toString() }
}
def lockEventHandler(evt) {
    logField(evt) {it == "locked" ? "locked" : "unlocked" }
}
def batteryEventHandler(evt) {
    logField(evt) { it.toString() }
}
def powerEventHandler(evt) {
    logField(evt) { it.toString() }
}
def energyEventHandler(evt) {
    logField(evt) { it.toString() }
}
def presenceEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatHeatSPEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatCoolSPEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatSPEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatModeEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatFanModeEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatOpStateEventHandler(evt) {
    logField(evt) { it.toString() }
}
def tstatFanStateEventHandler(evt) {
    logField(evt) { it.toString() }
}

//Parse.com API data transfer
private logField(evt, Closure c) {
	def type = "application/json"
	def url = "https://api.parse.com/1/classes/"
   
	log.debug evt.name + " Event triggered"
   
	switch (evt.name) {
		case "switch":
			httpPostJson(uri: url + "switches", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "level":
			httpPostJson(uri: url + "dimmers", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "motion":
			httpPostJson(uri: url + "motion", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "acceleration":
			httpPostJson(uri: url + "acceleration", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "contact":
			httpPostJson(uri: url + "contact", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "illuminance":
			httpPostJson(uri: url + "illuminance", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "temperature":
			httpPostJson(uri: url + "temps", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "humidity":
			httpPostJson(uri: url + "humidity", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break            
		case "locks":
			httpPostJson(uri: url + "locks", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break           
		case "battery":
			httpPostJson(uri: url + "batteries", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "power":
			httpPostJson(uri: url + "power", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "energy":
			httpPostJson(uri: url + "energy", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "presence":
			httpPostJson(uri: url + "presence", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "heatingSetpoint":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "coolingSetpoint":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break            
		case "thermostatSetpoint":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break            
		case "thermostatMode":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "thermostatFanMode":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break   
		case "thermostatOperatingState":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break
		case "thermostatFanState":
			httpPostJson(uri: url + "thermostats", headers:['content-type':type, 'x-parse-rest-api-key': restAPIKey, 'x-parse-application-id': appKey],	body:[deviceID: evt.deviceId, deviceName: evt.displayName, name: evt.name, value: evt.value, date: evt.isoDate, unit: evt.unit])
			break            
	}

	log.debug evt.name+" Event data successfully posted"
}