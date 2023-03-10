/**
 *  SmartPing Automation
 *
 *  Copyright 2016 Jason Botello
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
	name: "SmartPing Automation",
	namespace: "jasonbio/smartping",
	author: "jasonbio",
	parent: "jasonbio/smartping:SmartPing",
	description: "Monitor your website uptime and trigger SmartThings automations if it goes down.",
	category: "SmartThings Labs",
	iconUrl: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping.png",
	iconX2Url: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping@3x.png"
)

preferences {
	page name: "mainPage", title: "", install: true, uninstall: true
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	if (validateURL()) {
		state.downHost = "false"
		state.pollVerify = "false"
		app.updateLabel("${state.website}")
		runIn(5, poll)
    	}
}

def validateURL() {
	state.website = website.toLowerCase()
    	if (state.website.contains(".com") || state.website.contains(".net") || state.website.contains(".org") || state.website.contains(".biz") || state.website.contains(".us") || state.website.contains(".info") || state.website.contains(".io") || state.website.contains(".ca") || state.website.contains(".co.uk") || state.website.contains(".tv") || state.website.contains(":")) {
    		state.website = state.website.trim()
    		if (state.website.startsWith("http://")) {
    			state.website = state.website.replace("http://", "")
        		state.website = state.website.replace("www.", "")
    		}
    		if (state.website.startsWith("https://")) {
    			state.website = state.website.replace("https://", "")
        		state.website = state.website.replace("www.", "")
    		}
    		if (state.website.startsWith("www.")) {
    			state.website = state.website.replace("www.", "")
    		}
    		state.validURL = "true"
    		return true
	} else {
    		state.validURL = "false"
        	return false
    	}
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "") {
    		section {
        		paragraph "URL you want to monitor. ex: google.com"
            		input(name: "website", title:"URL", type: "text", required: true)
            		input(name: "threshold", title:"False Alarm Threshold (minutes)", type: "number", required: true, defaultValue:2)
        	}
        	section {
        		paragraph "If the URL goes offline"
            		lightInputs()
            		lightActionInputs()
            		switchInputs()
            		switchActionInputs()
            		alarmInputs()
            		alarmActionInputs()
        	}
    	}
}

def poll() {
	def reqParams = [
            uri: "http://${state.website}"
    	]
    	if (state.validURL == "true") {
    		try {
        		httpGet(reqParams) { resp ->
            			if (resp.status == 200) {
                			if (state.downHost == "true") {
            					turnOffHandler()
                    				log.info "successful response from ${state.website}, turning off handlers"
                			} else {
                    				log.info "successful response from ${state.website}, no handlers"
                			}
            			} else {
            				if (state.downHost == "false") {
                				if (state.pollVerify == "false") {
        						runIn(60*threshold, pollVerify)
            						state.pollVerify = "true"
            					}
                				log.info "request failed to ${state.website}, calling pollVerify with a ${threshold} minute threshold"
                			} else {
                				log.info "pollVerify already called"
                			}
            			}
        		}
    		} catch (e) {
        		if (state.downHost == "false") {
        			if (state.pollVerify == "false") {
        				runIn(60*threshold, pollVerify)
            				state.pollVerify = "true"
            			}
            			log.info "request failed to ${state.website}, calling pollVerify with a ${threshold} minute threshold"
        		} else {
           			log.info "pollVerify already called"
        		}
    		}
    	}
    	schedule("0 0/5 * * * ?", poll)
}

def pollVerify() {
	def reqParams = [
		uri: "http://${state.website}"
	]
    	try {
        	httpGet(reqParams) { resp ->
            		if (resp.status == 200) {
                		state.downHost = "false"
                		state.pollVerify = "false"
                		turnOffHandler()
                		log.info "successful response from ${state.website}, false alarm avoided"
            		} else {
            			state.downHost = "true"
                		state.pollVerify = "false"
            			turnOnHandler()
                		log.info "request failed to ${state.website}, turning on handlers"
            		}
        	}
    	} catch (e) {
        	state.downHost = "true"
        	state.pollVerify = "false"
        	turnOnHandler()
        	log.info "request failed to ${state.website}, turning on handlers"
    	}
}

def turnOnHandler() {
	if (lights) {
    		lights.each {
			if (it.hasCommand('setLevel')) {
				it.setLevel(level as Integer)
			} else {
				it.on()
			}
		}
    		lights?.on()
        	setColor()
        	setColorTemperature()
    		log.info "turning on lights"
    	}
	if (switches) {
    		switches.on()
    		log.info "turning on switches"
   	}
	if (alarms) {
    		alarms.each {
			if (it.hasCommand('both')) {
				it.both()
			} else if (it.hasCommand('siren')) {
				it.siren()
            		} else if (it.hasCommand('strobe')) {
            			it.strobe()
			}
		}
        	log.info "turning on siren(s)"
    	}
}

def turnOffHandler() {
	if (lights) {
    		lights.off()
    		log.info "turning on light(s)"
    	}
    	if (switches) {
    		switches.off()
    		log.info "turning off switch(es)"
    	}
    	if (alarms) {
    		alarms.off()
		log.info "turning off siren(s)"
	}
}

private lightInputs() {
	input "lights", "capability.switch", title: "Control these lights", multiple: true, required: false, submitOnChange: true
}

private switchInputs() {
	input "switches", "capability.switch", title: "Control these switches", multiple: true, required: false, submitOnChange: true
}

private alarmInputs() {
	input "alarms", "capability.alarm", title: "Control these alarms", multiple: true, required: false, submitOnChange: true
}

private lightActionMap() {
	def map = [on: "Turn On", off: "Turn Off"]
	if (lights.find{it.hasCommand('setLevel')} != null) {
		map.level = "Turn On & Set Level"
	}
	if (lights.find{it.hasCommand('setColor')} != null) {
		map.color = "Turn On & Set Color"
	}
	map
}

private lightActionOptions() {
	lightActionMap().collect{[(it.key): it.value]}
}

private lightActionInputs() {
	if (lights) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: lightActionOptions(), required: requiredInput, submitOnChange: true
		if (action == "color") {
			input "color", "enum", title: "Color", required: false, multiple:false, options: [
				["Soft White":"Soft White - Default"],
				["White":"White - Concentrate"],
				["Daylight":"Daylight - Energize"],
				["Warm White":"Warm White - Relax"],
				"Red","Green","Blue","Yellow","Orange","Purple","Pink"]

		}
		if (action == "colorTemperature") {
			input "colorTemperature", "enum", title: "Color Temperature", options: [[2700: "Soft White (2700K)"], [3300: "White (3300K)"], [4100: "Moonlight (4100K)"], [5000: "Cool White (5000K)"], [6500: "Daylight (6500K)"]], defaultValue: "3300"
		}
		if (action == "level" || action == "color" || action == "colorTemperature") {
			input "level", "enum", title: "Dimmer Level", options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "80"
		}
	}
}

private switchActionMap() {
	def map = [on: "Turn On", off: "Turn Off"]
}

private switchActionOptions() {
	switchActionMap().collect{[(it.key): it.value]}
}

private switchActionInputs() {
	if (switches) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: switchActionOptions(), required: requiredInput, submitOnChange: true
	}
}

private alarmActionMap() {
	def map = [off: "Turn Off"]
	if (alarms.find{it.hasCommand('both')} != null) {
		map.both = "Turn On Siren & Strobe"
	}
    if (alarms.find{it.hasCommand('siren')} != null) {
		map.siren = "Turn On Siren"
	}
    if (alarms.find{it.hasCommand('strobe')} != null) {
		map.strobe = "Turn On Strobe"
	}
	map
}

private alarmActionOptions() {
	alarmActionMap().collect{[(it.key): it.value]}
}

private alarmActionInputs() {
	if (alarms) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: alarmActionOptions(), required: requiredInput, submitOnChange: true
	}
}

private setColor() {

	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

	def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]

	lights.each {
		if (it.hasCommand('setColor')) {
			it.setColor(value)
		}
		else if (it.hasCommand('setLevel')) {
			it.setLevel(level as Integer ?: 100)
		}
		else {
			it.on()
		}
	}
}

def setColorTemperature() {
	def tempValue = colorTemperature as Integer ?: 3300
	def levelValue = level as Integer ?: 100
	lights.each {
		if (it.hasCommand('setColorTemperature')) {
			it.setColorTemperature(tempValue)
			if (it.hasCommand('setLevel')) {
				it.setLevel(levelValue)
			}
			else {
				it.on()
			}
		}
		else if (it.hasCommand('setLevel')) {
			it.setLevel(levelValue)
		}
		else {
			it.on()
		}
	}
}
