/**
 *  Hue SceneMaker
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
    name: "Hue SceneMaker",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Switch on and set Hue lights based on an (virtual) switch.",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/**********
 * Setup  *
 **********/

preferences {
    page(name: "lightSelectPage", title: "Turn on these lights:", nextPage: "optionsPage", params: [sceneId:sceneId], uninstall: true) 
    page(name: "optionsPage", title: "Use these options:", install: true, uninstall: true) 
}

def lightSelectPage() {
	dynamicPage(name: "lightSelectPage") {
        section("Use this (virtual) switch"){
            input "inputSwitch", "capability.switch", title: "Switches", required: true, multiple: true
        }

		section("To control these lights") {
			//input "lights", "capability.colorControl", multiple: true, required: false, title: "Lights, switches & dimmers"
			input "lights", "capability.switchLevel", multiple: true, required: false, title: "Lights, switches & dimmers"
		}
		section("Timing options") {
			input "starting", "time", title: "Starting from", required: false
			input "ending", "time", title: "Ending at", required: false
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
        section("Lights will also change to these when the mode changes to the selected modes. This only happens when the input switch is enabled!")
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
    }
}

def optionsPage(params) {
	dynamicPage(name: "optionsPage") {
		section("Lights") {
			lights.each {light ->
				input "onoff_${light.id}", "boolean", title: light.displayName
			}
		}

		section("Dimmers") {
			lights.each {light ->
				input "level_${light.id}", "enum", title: light.displayName, options: levels, description: "", required: false
			}
		}

		section("Colors (hue/saturation)") {
			lights.each {light ->
				input "color_${light.id}", "text", title: light.displayName, description: "", required: false
			}
		}
		section("Soft white: (23/56)") 
		section("White: (52/19)") 
		section("Daylight: (53/91)") 
		section("Warm white: (20/80)") 
		section("Orange: (10/100)") 
		section("Yellow: (25/100)") 
		section("Green: (39/100)") 
		section("Blue: (70/100)") 
		section("Purple: (75/100)") 
		section("Pink: (83/100)") 
		section("Red: (100/100)") 
	}
}

/*************************
 * Installation & update *
 *************************/

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
	subscribe(inputSwitch, "switch", switchHandler)

	if (modes) {
		subscribe(location, modeChangeHandler)
	}
}

/******************
 * Event handlers *
 ******************/

def appTouchHandler(evt) {
	log.info "app started manually"
    activateHue()
}

def switchHandler(evt) {
	log.trace "switchHandler()"
	def current = inputSwitch.currentValue('switch')
	def switchValue = inputSwitch.find{it.currentSwitch == "on"}
    def waitMode = 100
	if (allOk && switchValue) {
        activateHue()
        pause(waitMode)
        activateHue()
    }
	else if (switchValue) {
    	log.info "Wrong mode to activate anything"
    }
	else {
        log.info "Nothing to do..."
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler()"
	def current = inputSwitch.currentValue('switch')
	def switchValue = inputSwitch.find{it.currentSwitch == "on"}
    def waitStart = 500
    def waitMode = 100
	pause(waitStart)
	if (evt.value in modes && switchValue) {
    	log.trace "do it!"
		activateHue()
        pause(waitMode)
        activateHue()
	}
    else if (switchValue) {
    	log.info "Wrong mode to activate anything"
	}
    else {
    	log.info "Nothing to do..."
	}
}

/******************
 * Helper methods *
 ******************/

private closestLevel(level) {
	level ? "${Math.round(level/5) * 5}%" : "0%"
}

private activateHue() {
	log.trace "Activating!"
	state.lastStatus = "on"
    def wait = 10
    log.debug wait

	getDeviceCapabilities()
	
	lights.each {light ->
		def type = state.lightCapabilities[light.id]

		def isOn = settings."onoff_${light.id}" == "true" ? true : false
		log.debug "${light.displayName} is '$isOn'"
		if (isOn) {
			light.on()
			pause(wait)
			light.on()
			pause(wait)
			light.on()
		}
		else {
			light.off()
			pause(wait)
			light.off()
			pause(wait)
			light.off()
		}

		if (type != "switch") {
			def level = switchLevel(light)

			if (type == "level") {
				log.debug "${light.displayName} level is '$level'"
				if (level != null) {
					light.setLevel(level)
                    pause(wait)
					light.setLevel(level)
					pause(wait)
					light.setLevel(level)
				}
			}
			else if (type == "color") {
				def segs = settings."color_${light.id}"?.split("/")
				if (segs?.size() == 2) {
					def hue = segs[0].toInteger()
					def saturation = segs[1].toInteger()
					log.debug "${light.displayName} color is level: $level, hue: $hue, sat: $saturation"
					if (level != null) {
						light.setColor(level: level, hue: hue, saturation: saturation)
						pause(wait)
						light.setColor(level: level, hue: hue, saturation: saturation)
						pause(wait)
						light.setColor(level: level, hue: hue, saturation: saturation)
					}
					else {
						light.setColor(hue: hue, saturation: saturation)
						pause(wait)
						light.setColor(hue: hue, saturation: saturation)
						pause(wait)
						light.setColor(hue: hue, saturation: saturation)
					}
				}
				else {
					log.debug "${light.displayName} level is '$level'"
					if (level != null) {
						light.setLevel(level)
						pause(wait)
						light.setLevel(level)
						pause(wait)
                        light.setLevel(level)
					}
				}
			}
			else {
				log.error "Unknown type '$type'"
			}
		}
	}
}

private switchLevel(light) {
	def percent = settings."level_${light.id}"
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
			log.debug "colorlight"
            caps[it.id] = "color"
		}
		else if (it.hasCapability("Switch Level")) {
			log.debug "levellight"
            caps[it.id] = "level"
		}
		else {
			log.debug "switchlight"
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

private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}

private getAllOk() {
	modeOk && daysOk && timeOk
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
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
