/**
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
	name: "Notify via lights",
	namespace: "erik4281",
	author: "Erik Vennink",
	description: "Changes the color and brightness of Philips Hue bulbs when any of a variety of SmartThings is activated.  Supports motion, contact, acceleration, moisture and presence sensors as well as switches.",
	category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png"
)

preferences {
	section("Control these bulbs...") {
		input "lights", "capability.switchLevel", multiple: true, required: true, title: "Lights, switches & dimmers"
	}
	section("Choose one or more, when...") {
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
		input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
	}
	section("Choose light effects...") {
		input "color", "enum", title: "Color?", required: false, multiple:false, options: [
		["Soft White":"Soft White - Default"],
		["White":"White - Concentrate"],
		["Daylight":"Daylight - Energize"],
		["Warm White":"Warm White - Relax"],
		"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
		input "level", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
		input "duration", "number", title: "Duration Seconds?", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(contact, "contact.closed", eventOffHandler)
	subscribe(contactClosed, "contact.open", eventOffHandler)
	subscribe(acceleration, "acceleration.inactive", eventOffHandler)
	subscribe(motion, "motion.inactive", eventOffHandler)
	subscribe(mySwitch, "switch.off", eventOffHandler)
	subscribe(mySwitchOff, "switch.on", eventOffHandler)
	subscribe(arrivalPresence, "presence.not present", eventOffHandler)
	subscribe(departurePresence, "presence.present", eventOffHandler)
}

def eventHandler(evt) {
	log.debug "eventHandler"
	takeAction(evt)
}

def eventOffHandler(evt) {
	log.debug "eventOffHandler"
	resetHue()
}

def appTouchHandler(evt) {
	log.debug "appTouchHandler"
	takeAction(evt)
}

private takeAction(evt) {
	state.previous = [:]
	lights.each {light ->
		state.previous[light.id] = [
		"switch": light.currentValue("switch"),
		"level" : light.currentValue("level"),
		"hue": light.currentValue("hue"),
		"saturation": light.currentValue("saturation"),
		"color": light.currentValue("color")]
	}
	log.debug "current values = $state.previous"
	def hue = 23
	def saturation = 56
	switch(settings."color") {
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
	def newColorValue = [hue: hue, saturation: 100, level: (level as Integer) ?: 100]
	def newValue = [hue: hue, saturation: 100]
	log.debug "new color value = $newColorValue"
	log.debug "new value = $newValue"
	getDeviceCapabilities()
	lights.each {light ->              
		def type = state.lightCapabilities[light.id]
		if (type == "level") {
			if (level != null) {
				light.setLevel(newColorValue.level)
				light.setLevel(newColorValue.level)
			}
		}
		else if (type == "color") {
			if (level != null) {
				light.setColor(newColorValue)
				light.setColor(newColorValue)
			}
			else {
				light.setColor(newValue)
				light.setColor(newValue)
			}
		}
		else {
			if (level != null) {
				light.setLevel(newColorValue.level)
				light.setLevel(newColorValue.level)
			}
		}
	}
	setTimer()
}

def setTimer() {
	if (!duration) {
		log.debug "pause 10"
		pause(10 * 1000)
		log.debug "reset hue"
		resetHue()
	}
	else if (duration < 10) {
		log.debug "pause $duration"
		pause(duration * 1000)
		log.debug "resetHue"
		resetHue()
	}
	else {
		log.debug "runIn $duration, resetHue"
		runIn(duration,"resetHue", [overwrite: false])
	}
}

def resetHue() {
	lights.each {light ->
		def type = state.lightCapabilities[light.id]
		log.info state.previous[light.id].switch
		if (type == "level") {
			if (state.previous[light.id].switch == "off") {
				light.off()
				light.off()
			}
			else {
				light.setLevel(state.previous[light.id].level)
				light.setLevel(state.previous[light.id].level)
			}
		}
		else if (type == "color") {
			light.setColor(state.previous[light.id])
			light.setColor(state.previous[light.id])
		}
		else {
			if (state.previous[light.id].switch == "off") {
				light.off()
				light.off()
			}
			else {
				light.setLevel(state.previous[light.id].level)
				light.setLevel(state.previous[light.id].level)
			}
		}
	}
}

private getDeviceCapabilities() {
	def caps = [:]
	lights.each {light ->
		if (light.hasCapability("Color Control")) {
			caps[light.id] = "color"
		}
		else if (light.hasCapability("Switch Level")) {
			caps[light.id] = "level"
		}
		else {
			caps[light.id] = "switch"
		}
	}
	state.lightCapabilities = caps
}
