/**
 *  Hue Multi Lighting
 *
 *  Author: Erik Vennink (based on SmartThings)
 *  *
 *  Date: 2015-02-13
 */
definition(
    name: "Hue Multi Lighting",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Sets the colors and brightness level of your Philips Hue lights to match your mood.",
    category: "My apps",
    iconUrl: "http://icons.iconarchive.com/icons/zerode/plump/128/Control-Panel-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/zerode/plump/128/Control-Panel-icon.png"
)

preferences {
    page(name: "switchOnPage", title: "Switch on lights when this happens:", nextPage: "switchOffPage", uninstall: true) 
    page(name: "switchOffPage", title: "Switch off lights when this happens:", nextPage: "optionsPage", uninstall: true) 
    page(name: "optionsPage", title: "Use these options:", nextPage: "lightSelectPage", uninstall: true) 
    page(name: "lightSelectPage", title: "Turn on these lights:", install: true, uninstall: true) 
}

def switchOnPage() {
	dynamicPage(name: "switchOnPage") {
        section("Select..."){
            input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
            input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
            input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
            input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
            input "mySwitchOnOn", "capability.switch", title: "Switch Turned On", required: false, multiple: true
            input "mySwitchOffOn", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
            input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
            input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
            input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
            input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
            input "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
            input "timeOfDay", "time", title: "At a Scheduled Time", required: false
        }
	}
}

def switchOffPage() {
	dynamicPage(name: "switchOffPage") {
        section("Select..."){
            input "motionOff", "capability.motionSensor", title: "Motion Stops Here", required: false, multiple: true
            input "contactOff", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
            input "contactClosedOff", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
            input "accelerationOff", "capability.accelerationSensor", title: "Acceleration Stopped", required: false, multiple: true
            input "mySwitchOnOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
            input "mySwitchOffOff", "capability.switch", title: "Switch Turned On", required: false, multiple: true
            input "arrivalPresenceOff", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
            input "departurePresenceOff", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
            input "smokeOff", "capability.smokeDetector", title: "No Smoke Detected", required: false, multiple: true
            input "waterOff", "capability.waterSensor", title: "Water Sensor Dry", required: false, multiple: true
            input "triggerModesOff", "mode", title: "System Changes Mode", required: false, multiple: true
            input "timeOfDayOff", "time", title: "At a Scheduled Time", required: false
        }
	}
}

def optionsPage() {
	dynamicPage(name: "optionsPage") {
		section("Turn off after no action") {
			input "delayMinutes", "number", title: "Minutes", required: false
		}
		section("Monitor illuminance sensor") {
			input "lightSensor", "capability.illuminanceMeasurement", title: "Sensor(s)?", required: false
			input "lightOnValue", "number", title: "On at < (Lux, empty = 100)?", required: false
			input "lightOffValue", "number", title: "Off at > (Lux, empty = 150)?", required: false

		}

		section("Timing options") {
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			input "starting", "time", title: "Starting from", required: false
			input "ending", "time", title: "Ending at", required: false
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
	}
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
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
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
	subscribe(mySwitchOnOn, "switch.on", eventHandler)
	subscribe(mySwitchOnOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)

	if (triggerModes || triggerModesOff) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}

	subscribe(contactOff, "contact.closed", eventOffHandler)
	subscribe(contactClosedOff, "contact.open", eventOffHandler)
	subscribe(accelerationOff, "acceleration.inactive", eventOffHandler)
	subscribe(motionOff, "motion.inactive", eventOffHandler)
	subscribe(mySwitchOffOn, "switch.off", eventOffHandler)
	subscribe(mySwitchOffOff, "switch.on", eventOffHandler)
	subscribe(arrivalPresenceOff, "presence.not present", eventOffHandler)
	subscribe(departurePresenceOff, "presence.present", eventOffHandler)
	subscribe(smokeOff, "smoke.clear", eventOffHandler)
	subscribe(smokeOff, "carbonMonoxide.clear", eventOffHandler)
	subscribe(waterOff, "water.dry", eventOffHandler)

	if (timeOfDayOff) {
		schedule(timeOfDayOff, scheduledTimeOffHandler)
	}
    
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}

}

def eventHandler(evt) {
	//log.trace "eventHandler($evt.name: $evt.value)"
	log.trace "eventHandler"
	if (allOkExtra) {
		log.trace "allOkExtra"
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					state.actionStopTime = null
                    takeAction(evt)
				}
			}
			else {
				state.actionStopTime = null
                takeAction(evt)
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}
	}
    else { 
    	log.trace "all NOK, only running to set state.actionStopTime to 0" 
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					state.actionStopTime = null
				}
			}
			else {
				state.actionStopTime = null
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}        
    }
}

def eventOffHandler(evt) {
	//log.trace "eventOffHandler($evt.name: $evt.value)"
	log.trace "eventOffHandler"
	if (allOk) {
		log.trace "allOk"
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					state.actionStopTime = now()
                    takeOffAction(evt)
				}
			}
			else {
				state.actionStopTime = now()
                takeOffAction(evt)
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}
	}
}

def illuminanceHandler(evt) {
	log.trace "illuminanceHandler()"
    if (allOk) {
    	log.info "state.lastStatus: $state.lastStatus"
        log.info "evt.integerValue: $evt.integerValue"
        log.info "state.actionStopTime: $state.actionStopTime"
        if (state.lastStatus != "off" && evt.integerValue > (lightOffValue ?: 150)) {
            deactivateHue()
        }
        else if (state.actionStopTime) {
            if (state.lastStatus != "off") {
                def elapsed = now() - state.actionStopTime
                if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
                    deactivateHue()
                }
            }
        }
        else if (state.lastStatus != "on" && evt.integerValue < (lightOnValue ?: 100)){
            activateHue()
        }
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value"// ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
	if (evt.value in triggerModesOff) {
		eventOffHandler(evt)
	}
}

def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	eventHandler(evt)
}

def scheduledTimeOffHandler() {
	log.trace "scheduledTimeOffHandler()"
	eventOffHandler(evt)
}

def appTouchHandler(evt) {
	log.trace "app started manually"
    activateHue()
}

private takeAction(evt) {
	
    log.trace "take action"
	if (frequency) {
		state[frequencyKey(evt)] = now()
	}
    activateHue()
}

private takeOffAction(evt) {
	log.trace "take offaction"
	if(delayMinutes) {
		runIn(delayMinutes*60, turnOffAfterDelay, [overwrite: false])
		log.info "Delay: $delayMinutes minutes"
	} 
	else {
    	deactivateHue()
	}
}

def turnOffAfterDelay() {
	log.trace "In turnOffAfterDelay, state.actionStopTime = $state.actionStopTime, state.lastStatus = $state.lastStatus"
	if (state.actionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.actionStopTime
		if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
			if (frequency) {
				state[frequencyKey(evt)] = now()
			}
			deactivateHue()
		}
	}
}

def activateHue() {
    if (lights1) {
        startHue(lights1, color1, lightLevel1)
    }
    if (lights2) {
        startHue(lights2, color2, lightLevel2)
    }
    if (lights3) {
        startHue(lights3, color3, lightLevel3)
    }
	state.lastStatus = "on"
}

def deactivateHue() {
    if (lights1) {
        stopHue(lights1)
    }
    if (lights2) {
        stopHue(lights2)
    }
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
        lightSelect*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lightSelect*.on()
    }
}

def stopHue(lightSelect) {
	log.trace "Deactivating Hue '$lightSelect'"

	lightSelect*.off()
}

private frequencyKey(evt) {
	"lastActionTimeStamp"
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

private oncePerDayOk(Long lastTime) {
	def result = true
	if (oncePerDay) {
		result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
		log.trace "oncePerDayOk = $result"
	}
	result
}

private getAllOkExtra() {
	modeOk && daysOk && timeOk && darkOk
}

private getAllOk() {
	modeOk && daysOk && timeOk
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
