/**
 *  Amazing Lights
 *
 *  Copyright 2015 Erik Vennink
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

/************
 * Metadata *
 ************/

definition(
	name: "Amazing Lights with scenes",
	namespace: "erik4281",
	author: "Erik Vennink",
	description: "Lights are completely automated with this SmartApp. Activated by motion, open-close switch or a normal (virtual) switch. Different scenes can be programmed, depending on day of week, time or mode.",
	category: "SmartThings Labs",
	iconUrl: "http://icons.iconarchive.com/icons/hopstarter/sleek-xp-software/128/Nero-Smart-Start-icon.png",
	iconX2Url: "http://icons.iconarchive.com/icons/hopstarter/sleek-xp-software/128/Nero-Smart-Start-icon.png",
	iconX3Url: "http://icons.iconarchive.com/icons/hopstarter/sleek-xp-software/128/Nero-Smart-Start-icon.png")

/**********
 * Setup  *
 **********/

preferences {
	page(name: "switchPage", title: "Switch on lights when this happens:", nextPage: "scenesPage", uninstall: true) 
	page(name: "scenesPage", title: "Switch these lights when this is true:", install: true, uninstall: true) 
	page(name: "optionsPage", title: "Use these options:", install: false, uninstall: false, previousPage: "scenesPage") 
	page(name: "devicePage", install: false, uninstall: false, previousPage: "optionsPage")
}

def switchPage() {
	dynamicPage(name: "switchPage") {
		section("Control these switches") {
			input "lights", "capability.switchLevel", multiple: true, required: false, title: "Lights, switches & dimmers"
		}
		section("Switch on when..."){
			input "motionSensor", "capability.motionSensor", title: "Motion here", required: false, multiple: true
			input "contactSensor", "capability.contactSensor", title: "Contact opens", required: false, multiple: true
			input "inputSwitch", "capability.switch", title: "Switch turns on", required: false, multiple: true
		}
		section("Switch off when..."){
			input "delayMinutes", "number", title: "Off after x minutes of motion/contact", required: false
		}
		section("Or switch off faster") {
			input "shortDelayMinutes", "number", title: "Off after x minutes", required: false
			input "shortModes", "mode", title: "In mode(s)", required: false, multiple: true
			input "shortStarting", "time", title: "And starting from", required: false
			input "shortEnding", "time", title: "And ending at", required: false
		}
		section("Monitor illuminance sensor") {
			input "lightSensor", "capability.illuminanceMeasurement", title: "Sensor(s)?", required: false
			input "lightOnValue", "number", title: "On at < (Lux, empty = 100)?", required: false
			input "lightOffValue", "number", title: "Off at > (Lux, empty = 150)?", required: false
		}
		section("Use MoodCube switch (disable auto-switching light)") {
			input "moodSwitch", "capability.switch", title: "Switch", required: false
		}
		section([mobileOnly:true]) {
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			label title: "Assign a name", required: false
		}
	}
}

def scenesPage() {
	def sceneId = getTiming()
	state.sceneId = sceneId
	log.debug "Current SceneId = ${sceneId}"
	dynamicPage(name: "scenesPage") {
		section {
			for (num in 1..8) {
				href "optionsPage", title: "${num}. ${sceneName(num)}${sceneId==num ? ' (current)' : ''}", params: [sceneId:num], description: "", state: sceneIsDefined(num) ? "complete" : "incomplete"
			}
		}
	}
}

def optionsPage(params=[:]) {
	log.debug "optionsPage($params)"
	def sceneId = params.sceneId as Integer ?: state.sceneId
	state.sceneId = sceneId
	dynamicPage(name:"optionsPage", title: "${sceneId}. ${sceneName(sceneId)}") {
		section {
			input "sceneName${sceneId}", "text", title: "Scene Name", required: false
		}
		section {
			href "devicePage", title: "Show Device States", params: [sceneId:sceneId], description: "", state: sceneIsDefined(sceneId) ? "complete" : "incomplete"
		}
		section("Timing options") {
			input "starting_${sceneId}", "time", title: "Only starting from", required: false
			input "ending_${sceneId}", "time", title: "Only ending at", required: false
			input "days_${sceneId}", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes_${sceneId}", "mode", title: "Only when mode is", multiple: true, required: false
			input "switchOn_${sceneId}", "capability.switch", title: "Only when switch is ON", required: false, multiple: true
			input "switchOff_${sceneId}", "capability.switch", title: "Only when switch is OFF", required: false, multiple: true
		}
	}
}

def devicePage(params) {
	getDeviceCapabilities()
	def sceneId = params.sceneId as Integer ?: state.sceneId
	dynamicPage(name: "devicePage") {
		section {
			input "sceneName${sceneId}", "text", title: "Scene Name", required: false
		}
		section("Lights") {
			lights.each {light ->
				input "onoff_${sceneId}_${light.id}", "boolean", title: light.displayName
			}
		}
		section("Dimmers") {
			lights.each {light ->
				input "level_${sceneId}_${light.id}", "enum", title: light.displayName, options: levels, description: "", required: false
			}
		}
		section("Colors") {
			lights.each {light ->
				input "color_${sceneId}_${light.id}", "enum", title: light.displayName, required: false, multiple:false, options: [
				["Soft White":"Soft White - Default"],
				["White":"White - Concentrate"],
				["Daylight":"Daylight - Energize"],
				["Warm White":"Warm White - Relax"],
				"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
			}
		}
	}
}

/*************************
 * Installation & update *
 *************************/

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	subscribe(app, appTouchHandler)
	subscribe(motionSensor, "motion.active", eventHandler)
	subscribe(contactSensor, "contact.open", eventHandler)
	subscribe(inputSwitch, "switch.on", eventHandler)
	subscribe(motionSensor, "motion.inactive", eventOffHandler)
	subscribe(contactSensor, "contact.closed", eventOffHandler)
	subscribe(inputSwitch, "switch.off", eventOffHandler)
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
}

/******************
 * Event handlers *
 ******************/

def appTouchHandler(evt) {
	activateHue()
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	if (motionValue) {
		state.eventStopTime = null
	}
	else {
		state.eventStopTime = now()
	}
	if ((shortModeOk || shortTimeOk) && shortDelayMinutes) {
		runIn(shortDelayMinutes*60, turnOffAfterDelayShort, [overwrite: false])
	}
	else if (delayMinutes) {
		runIn(delayMinutes*60, turnOffAfterDelay, [overwrite: false])
	}
	else  {
		turnOffAfterDelay()
	}
}

def eventHandler(evt) {
	log.trace "eventHandler: $evt.name: $evt.value"
	state.eventStopTime = null
	if (modeOk && daysOk && timeOk && darkOk && moodOk) {
		log.info "All checks OK, switching on now"
		activateHue()
	}
}

def eventOffHandler(evt) {
	log.trace "eventHandler: $evt.name: $evt.value"
	state.eventStopTime = now()
	if (evt.name == "switch" && evt.value == "off" && moodOk) {
		log.info "Switch was set to off. Starting timer to switch off."
		runIn(shortDelayMinutes*60, turnOffAfterDelayShort, [overwrite: false])
	}
	else if (switchOff && modeOk && daysOk && timeOk && moodOk) {
		log.info "Switches are off and all checks passed"
		if ((shortModeOk || shortTimeOk) && shortDelayMinutes) {
			log.info "Now starting short timer to switch off"
			runIn(shortDelayMinutes*60, turnOffAfterDelayShort, [overwrite: false])
		}
		else if (delayMinutes) {
			log.info "Now starting normal timer to switch off"
			runIn(delayMinutes*60, turnOffAfterDelay, [overwrite: false])
		}
		else  {
			log.info "Now starting to switch off"
			turnOffAfterDelay()
		}
	}
	else if (switchOff && moodOk) {
		log.info "Now starting 30 minute timer for backup off switching"
		runIn(30*60, turnOffAfterDelay, [overwrite: false])
	}
}

def illuminanceHandler(evt) {
	if (modeOk && daysOk && timeOk && moodOk) {
		if (state.lastStatus != "off" && evt.integerValue > (lightOffValue ?: 150)) {
			log.info "Light was not off and brightness was too high"
			deactivateHue()
		}
		else if (state.eventStopTime) {
			if (state.lastStatus != "off" && switchOff) {
				log.info "Light was not off and not currently activated"
				def elapsed = now() - state.eventStopTime                
				if((shortModeOk || shortTimeOk) && shortDelayMinutes) {
					if (elapsed >= ((shortDelayMinutes ?: 0) * 60000L) - 2000) {
						deactivateHue()
					}
				}
				else if(delayMinutes) {
					if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
						deactivateHue()
					}
				}
			}
			else if (state.lastStatus != "on" && evt.integerValue < (lightOnValue ?: 100) && switchOff != true) {
				log.info "Light was not on and brightness was too low"
				activateHue()
			}
		}
		else if (state.lastStatus != "on" && evt.integerValue < (lightOnValue ?: 100)){
			log.info "Light was not on and brightness was too low"
			activateHue()
		}
	}
}

/********************
 * Actuator methods *
 ********************/

def turnOffAfterDelay() {
	if (state.eventStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.eventStopTime
		if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
			log.info "Deactivating started"
			deactivateHue()
		}
	}
}

def turnOffAfterDelayShort() {
	if (state.eventStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.eventStopTime
		if (elapsed >= ((shortDelayMinutes ?: 0) * 60000L) - 2000) {
			log.info "Deactivating started"
			deactivateHue()
		}
	}
}

private activateHue() {
	state.lastStatus = "on"
	getDeviceCapabilities()
	getTiming()
	def sceneId = state.sceneId
	log.info "FINALLY SELECTED SCENE = $sceneId"
	lights.each {light ->
		def type = state.lightCapabilities[light.id]
		def isOn = settings."onoff_${sceneId}_${light.id}" == "true" ? true : false
		log.info "Light on: ${settings."onoff_${sceneId}_${light.id}"}"
		if (isOn) {
			light.on()
		}
		else {
			light.off()
		}
		if (type != "switch" && moodOk) {
			def level = switchLevel(sceneId, light)
			if (type == "level") {
				if (level != null) {
					light.setLevel(level)
				}
			}
			else if (type == "color") {
				def hue = 23
				def saturation = 56
				switch(settings."color_${sceneId}_${light.id}") {
					case "White":
					hue = 52
					saturation = 19
					break;
					case "Daylight":
					hue = 53
					saturation = 91
					break;
					case "Soft White":
					hue = 23
					saturation = 56
					break;
					case "Warm White":
					hue = 20
					saturation = 80
					break;
					case "Blue":
					hue = 70
					saturation = 100
					break;
					case "Green":
					hue = 39
					saturation = 100
					break;
					case "Yellow":
					hue = 25
					saturation = 100
					break;
					case "Orange":
					hue = 10
					saturation = 100
					break;
					case "Purple":
					hue = 75
					saturation = 100
					break;
					case "Pink":
					hue = 83
					saturation = 100
					break;
					case "Red":
					hue = 100
					saturation = 100
					break;
				}
				if (level != null) {
					light.setColor(level: level, hue: hue, saturation: saturation)
				}
				else {
					light.setColor(hue: hue, saturation: saturation)
				}
			}
			else {
				if (level != null) {
					light.setLevel(level)
				}
			}
		}	
		else {
		}
	}
}

private deactivateHue() {
	state.lastStatus = "off"
	log.info "Deactivating Hue now (3x)"
	lights.each {light ->
		light.off()
		pause(250)
		light.off()
		pause(250)
		light.off()
	}
}

/******************
 * Helper methods *
 ******************/

private getTiming() {
	def sceneId = params.sceneId as Integer ?: state.sceneId
	log.info "Before sceneId = ${sceneId}"
	if (sceneName1 && modeOkScene_1 && daysOkScene_1 && timeOkScene_1 && switchOnScene_1 && switchOffScene_1) {
		state.selectedSceneId = 1
		log.info sceneName1
	}
	else if (sceneName2 && modeOkScene_2 && daysOkScene_2 && timeOkScene_2 && switchOnScene_2 && switchOffScene_2) {
		state.selectedSceneId = 2
		log.info sceneName1
	}
	else if (sceneName3 && modeOkScene_3 && daysOkScene_3 && timeOkScene_3 && switchOnScene_3 && switchOffScene_3) {
		state.selectedSceneId = 3
		log.info sceneName1
	}
	else if (sceneName4 && modeOkScene_4 && daysOkScene_4 && timeOkScene_4 && switchOnScene_4 && switchOffScene_4) {
		state.selectedSceneId = 4
		log.info sceneName1
	}
	else if (sceneName5 && modeOkScene_5 && daysOkScene_5 && timeOkScene_5 && switchOnScene_5 && switchOffScene_5) {
		state.selectedSceneId = 5
		log.info sceneName1
	}
	else if (sceneName6 && modeOkScene_6 && daysOkScene_6 && timeOkScene_6 && switchOnScene_6 && switchOffScene_6) {
		state.selectedSceneId = 6
		log.info sceneName1
	}
	else if (sceneName7 && modeOkScene_7 && daysOkScene_7 && timeOkScene_7 && switchOnScene_7 && switchOffScene_7) {
		state.selectedSceneId = 7
		log.info sceneName1
	}
	else if (sceneName8 && modeOkScene_8 && daysOkScene_8 && timeOkScene_8 && switchOnScene_8 && switchOffScene_8) {
		state.selectedSceneId = 8
		log.info sceneName1
	}
	sceneId = state.selectedSceneId
	state.sceneId = sceneId
	log.info "After sceneId = ${sceneId}"
}

private closestLevel(level) {
	level ? "${Math.round(level/5) * 5}%" : "0%"
}

private switchLevel(sceneId, light) {
	def percent = settings."level_${sceneId}_${light.id}"
	if (percent) {
		percent[0..-2].toInteger()
	}
	else {
		null
	}
}

private getDeviceCapabilities() {
	def caps = [:]
	lights.each {
		if (it.hasCapability("Color Control")) {
			caps[it.id] = "color"
		}
		else if (it.hasCapability("Switch Level")) {
			caps[it.id] = "level"
		}
		else {
			caps[it.id] = "switch"
		}
	}
	state.lightCapabilities = caps
}

private getLevels() {
	def levels = []
	for (int i = 0; i <= 100; i += 10) {
		levels << "$i%"
	}
	levels
}

private Boolean sceneIsDefined(sceneId) {
	def tgt = "onoff_${sceneId}".toString()
	settings.find{it.key.startsWith(tgt)} != null
}

private sceneName(num) {
	final names = ["UNDEFINED","One","Two","Three","Four","Five","Six","Seven","Eight"]
	settings."sceneName${num}" ?: "Scene ${names[num]}"
}

/***********
 * Checks  *
 ***********/

private getSwitchOff() {
	def result = true
	if (inputSwitch) {
		def current = inputSwitch.currentValue('switch')
		def switchValue = inputSwitch.find{it.currentSwitch == "on"}
		if (switchValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOff = $result"
	result
}

private getMoodOk() {
	def result = true
	if (moodSwitch) {
		if (moodSwitch.currentSwitch == "off") {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "moodOk = $result"
	result
}

private getDarkOk() {
	def result = true
	if (lightSensor) {
		result = lightSensor.currentIlluminance < (lightOnValue ?: 100)
	}
	else {
		result = true
	}
	log.trace "darkOk = $result"
	result
}

private getShortModeOk() {
	def result = !shortModes || shortModes.contains(location.mode)
	log.trace "shortModeOk = $result"
	result
}

private getShortTimeOk() {
	def result = false
	if (shortStarting && shortEnding) {
		def currTime = now()
		def shortStart = timeToday(shortStarting).time
		def shortStop = timeToday(shortEnding).time
		result = shortStart < shortStop ? currTime >= shortStart && currTime <= shortStop : currTime <= shortStop || currTime >= shortStart
	}
	log.trace "shortTimeOk = $result"
	result
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

////////////////////////////////////
////////////////////////////////////

private getModeOkScene_1() {
	def result = !modes_1 || modes_1.contains(location.mode)
	log.trace "modeOkScene_1 = $result"
	result
}

private getModeOkScene_2() {
	def result = !modes_2 || modes_2.contains(location.mode)
	log.trace "modeOkScene_2 = $result"
	result
}

private getModeOkScene_3() {
	def result = !modes_3 || modes_3.contains(location.mode)
	log.trace "modeOkScene_3 = $result"
	result
}

private getModeOkScene_4() {
	def result = !modes_4 || modes_4.contains(location.mode)
	log.trace "modeOkScene_4 = $result"
	result
}

private getModeOkScene_5() {
	def result = !modes_5 || modes_5.contains(location.mode)
	log.trace "modeOkScene_5 = $result"
	result
}

private getModeOkScene_6() {
	def result = !modes_6 || modes_6.contains(location.mode)
	log.trace "modeOkScene_6 = $result"
	result
}

private getModeOkScene_7() {
	def result = !modes_7 || modes_7.contains(location.mode)
	log.trace "modeOkScene_7 = $result"
	result
}

private getModeOkScene_8() {
	def result = !modes_8 || modes_8.contains(location.mode)
	log.trace "modeOkScene_8 = $result"
	result
}

////////////////////////////////////
////////////////////////////////////

private getDaysOkScene_1() {
	def result = true
	if (days_1) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_1.contains(day)
	}
	log.trace "daysOkScene_1 = $result"
	result
}

private getDaysOkScene_2() {
	def result = true
	if (days_2) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_2.contains(day)
	}
	log.trace "daysOkScene_2 = $result"
	result
}

private getDaysOkScene_3() {
	def result = true
	if (days_3) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_3.contains(day)
	}
	log.trace "daysOkScene_3 = $result"
	result
}

private getDaysOkScene_4() {
	def result = true
	if (days_4) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_4.contains(day)
	}
	log.trace "daysOkScene_4 = $result"
	result
}

private getDaysOkScene_5() {
	def result = true
	if (days_5) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_5.contains(day)
	}
	log.trace "daysOkScene_5 = $result"
	result
}

private getDaysOkScene_6() {
	def result = true
	if (days_6) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_6.contains(day)
	}
	log.trace "daysOkScene_6 = $result"
	result
}

private getDaysOkScene_7() {
	def result = true
	if (days_7) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_7.contains(day)
	}
	log.trace "daysOkScene_7 = $result"
	result
}

private getDaysOkScene_8() {
	def result = true
	if (days_8) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days_8.contains(day)
	}
	log.trace "daysOkScene_8 = $result"
	result
}

////////////////////////////////////
////////////////////////////////////

private getTimeOkScene_1() {
	def result = true
	if (starting_1 && ending_1) {
		def currTime = now()
		def start = timeToday(starting_1, location?.timeZone).time
		def stop = timeToday(ending_1, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_1 = $result"
	result
}

private getTimeOkScene_2() {
	def result = true
	if (starting_2 && ending_2) {
		def currTime = now()
		def start = timeToday(starting_2, location?.timeZone).time
		def stop = timeToday(ending_2, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_2 = $result"
	result
}

private getTimeOkScene_3() {
	def result = true
	if (starting_3 && ending_3) {
		def currTime = now()
		def start = timeToday(starting_3, location?.timeZone).time
		def stop = timeToday(ending_3, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_3 = $result"
	result
}

private getTimeOkScene_4() {
	def result = true
	if (starting_4 && ending_4) {
		def currTime = now()
		def start = timeToday(starting_4, location?.timeZone).time
		def stop = timeToday(ending_4, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_4 = $result"
	result
}

private getTimeOkScene_5() {
	def result = true
	if (starting_5 && ending_5) {
		def currTime = now()
		def start = timeToday(starting_5, location?.timeZone).time
		def stop = timeToday(ending_5, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_5 = $result"
	result
}

private getTimeOkScene_6() {
	def result = true
	if (starting_6 && ending_6) {
		def currTime = now()
		def start = timeToday(starting_6, location?.timeZone).time
		def stop = timeToday(ending_6, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_6 = $result"
	result
}

private getTimeOkScene_7() {
	def result = true
	if (starting_7 && ending_7) {
		def currTime = now()
		def start = timeToday(starting_7, location?.timeZone).time
		def stop = timeToday(ending_7, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_7 = $result"
	result
}

private getTimeOkScene_8() {
	def result = true
	if (starting_8 && ending_8) {
		def currTime = now()
		def start = timeToday(starting_8, location?.timeZone).time
		def stop = timeToday(ending_8, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkScene_8 = $result"
	result
}

////////////////////////////////////
////////////////////////////////////

private getSwitchOnScene_1() {
	def result = true
	if (switchOn_1) {
		def current = switchOn_1.currentValue('switch')
		def switchOnValue = switchOn_1.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_1 = $result"
	result
}

private getSwitchOnScene_2() {
	def result = true
	if (switchOn_2) {
		def current = switchOn_2.currentValue('switch')
		def switchOnValue = switchOn_2.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_2 = $result"
	result
}

private getSwitchOnScene_3() {
	def result = true
	if (switchOn_3) {
		def current = switchOn_3.currentValue('switch')
		def switchOnValue = switchOn_3.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_3 = $result"
	result
}

private getSwitchOnScene_4() {
	def result = true
	if (switchOn_4) {
		def current = switchOn_4.currentValue('switch')
		def switchOnValue = switchOn_4.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_4 = $result"
	result
}

private getSwitchOnScene_5() {
	def result = true
	if (switchOn_5) {
		def current = switchOn_5.currentValue('switch')
		def switchOnValue = switchOn_5.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_5 = $result"
	result
}

private getSwitchOnScene_6() {
	def result = true
	if (switchOn_6) {
		def current = switchOn_6.currentValue('switch')
		def switchOnValue = switchOn_6.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_6 = $result"
	result
}

private getSwitchOnScene_7() {
	def result = true
	if (switchOn_7) {
		def current = switchOn_7.currentValue('switch')
		def switchOnValue = switchOn_7.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_7 = $result"
	result
}

private getSwitchOnScene_8() {
	def result = true
	if (switchOn_8) {
		def current = switchOn_8.currentValue('switch')
		def switchOnValue = switchOn_8.find{it.currentSwitch == "on"}
		if (switchOnValue) {
			result = true
		}
		else {
			result = false
		}
	}
	else {
		result = true
	}
	log.trace "switchOnScene_8 = $result"
	result
}

////////////////////////////////////
////////////////////////////////////

private getSwitchOffScene_1() {
	def result = true
	if (switchOff_1) {
		def current = switchOff_1.currentValue('switch')
		def switchOffValue = switchOff_1.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_1 = $result"
	result
}

private getSwitchOffScene_2() {
	def result = true
	if (switchOff_2) {
		def current = switchOff_2.currentValue('switch')
		def switchOffValue = switchOff_2.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_2 = $result"
	result
}

private getSwitchOffScene_3() {
	def result = true
	if (switchOff_3) {
		def current = switchOff_3.currentValue('switch')
		def switchOffValue = switchOff_3.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_3 = $result"
	result
}

private getSwitchOffScene_4() {
	def result = true
	if (switchOff_4) {
		def current = switchOff_4.currentValue('switch')
		def switchOffValue = switchOff_4.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_4 = $result"
	result
}

private getSwitchOffScene_5() {
	def result = true
	if (switchOff_5) {
		def current = switchOff_5.currentValue('switch')
		def switchOffValue = switchOff_5.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_5 = $result"
	result
}

private getSwitchOffScene_6() {
	def result = true
	if (switchOff_6) {
		def current = switchOff_6.currentValue('switch')
		def switchOffValue = switchOff_6.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_6 = $result"
	result
}

private getSwitchOffScene_7() {
	def result = true
	if (switchOff_7) {
		def current = switchOff_7.currentValue('switch')
		def switchOffValue = switchOff_7.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_7 = $result"
	result
}

private getSwitchOffScene_8() {
	def result = true
	if (switchOff_8) {
		def current = switchOff_8.currentValue('switch')
		def switchOffValue = switchOff_8.find{it.currentSwitch == "on"}
		if (switchOffValue) {
			result = false
		}
		else {
			result = true
		}
	}
	else {
		result = true
	}
	log.trace "switchOffScene_8 = $result"
	result
}
