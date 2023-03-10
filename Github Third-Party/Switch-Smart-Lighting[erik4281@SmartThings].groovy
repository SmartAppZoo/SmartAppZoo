/**
 *  Switch Smart Lighting
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
    name: "Switch Smart Lighting",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Light settings activated by (virtual) switch. By using time/mode limits it is possible to set different colors for different times/modes. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png")


/**********
 * Setup  *
 **********/

preferences {
    page(name: "lightSelectPage", title: "Turn on these lights:", nextPage: "optionsPage", uninstall: true) 
    page(name: "optionsPage", title: "Use these options:", install: true, uninstall: true) 
}

def lightSelectPage() {
	dynamicPage(name: "lightSelectPage") {
        section("Control these lights (set 1)") {
            input "lights1", "capability.colorControl", title: "Which bulbs?", required:true, multiple:true
            input "color1", "enum", title: "Set this color?", required: false, multiple:false, options: [
                "On - Custom Color",
                "Soft White",
                "White",
                "Daylight",
                "Warm White",
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevel1", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
        }
        section("Control these lights (set 2)") {
            input "lights2", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
            input "color2", "enum", title: "Set this color?", required: false, multiple:false, options: [
                "On - Custom Color",
                "Soft White",
                "White",
                "Daylight",
                "Warm White",
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevel2", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
        }
        section("Control these lights (set 3)") {
            input "lights3", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
            input "color3", "enum", title: "Set this color?", required: false, multiple:false, options: [
                "On - Custom Color",
                "Soft White",
                "White",
                "Daylight",
                "Warm White",
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevel3", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
        }
    }
}

def optionsPage() {
	dynamicPage(name: "optionsPage") {
        section("Input switch..."){
            input "inputSwitch", "capability.switch", title: "Switches", required: true, multiple: true
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
	if (allOk && switchValue) {
        activateHue()
    }
	else if (switchValue) {
    	log.info "Wrong mode to activate anything"
    }
	else {
        deactivateHue()
    }
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler()"
	def current = inputSwitch.currentValue('switch')
	def switchValue = inputSwitch.find{it.currentSwitch == "on"}
	if (evt.value in modes && switchValue) {
    	log.trace "do it!"
		activateHue()
	}
    else if (switchValue) {
    	log.info "Wrong mode to activate anything"
	}
    else {
    	deactivateHue()
	}
}

/******************
 * Helper methods *
 ******************/

def activateHue() {
    def waiting = 250
    log.debug waiting
    if (lights1) {
        startHue(lights1, color1, lightLevel1)
    }
	pause(waiting)
    if (lights2) {
        startHue(lights2, color2, lightLevel2)
	}
	pause(waiting)
    if (lights3) {
        startHue(lights3, color3, lightLevel3)
    }
	state.lastStatus = "on"
}

def deactivateHue() {
    def waiting = 250
    log.debug waiting
    if (lights1) {
        stopHue(lights1)
    }
	pause(waiting)
    if (lights2) {
        stopHue(lights2)
    }
	pause(waiting)
    if (lights3) {
        stopHue(lights3)
    }
	state.lastStatus = "off"
}

def startHue(lightSelect, colorSelect, levelSelect) {
	log.trace "Activating Hue '$lightSelect', with color '$colorSelect' and level '$levelSelect'"

    def hueColor = 70
	def saturation = 100

	switch(colorSelect) {
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
				saturation = 80 //83
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

	if (colorSelect != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: levelSelect as Integer ?: 100]
        def wait = 50
        log.debug wait
        lightSelect*.setColor(newValue)
        pause(wait)
        lightSelect*.setColor(newValue)
        pause(wait)
        lightSelect*.setColor(newValue)
        pause(wait)
        lightSelect*.setColor(newValue)
        pause(wait)
        lightSelect*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
        def wait = 50
        log.debug wait
    	lightSelect*.on()
        pause(wait)
    	lightSelect*.on()
        pause(wait)
    	lightSelect*.on()
        pause(wait)
        lightSelect*.on()
        pause(wait)
    	lightSelect*.on()
    }
}

def stopHue(lightSelect) {
	log.trace "Deactivating Hue '$lightSelect'"
    def wait = 50
    log.debug wait
	lightSelect*.off()
	pause(wait)
	lightSelect*.off()
	pause(wait)
	lightSelect*.off()
	pause(wait)
	lightSelect*.off()
	pause(wait)
	lightSelect*.off()
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
