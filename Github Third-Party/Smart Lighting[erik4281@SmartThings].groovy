/**
 *  Hue Multi Lighting
 *
 *  Author: Erik Vennink (based on SmartThings)
 *  *
 *  Date: 2015-02-13
 */

/************
 * Metadata *
 ************/

definition(
    name: "Smart Lighting",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Sets the colors and brightness level of your Philips Hue lights to match your mood.",
    category: "My apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png")

/**********
 * Setup  *
 **********/

preferences {
    page(name: "switchPage", title: "Switch on lights when this happens:", nextPage: "lightSelectPage", uninstall: true) 
    page(name: "lightSelectPage", title: "Turn on these lights:", nextPage: "optionsPage", uninstall: true) 
    page(name: "optionsPage", title: "Use these options:", install: true, uninstall: true) 
}

def switchPage() {
	dynamicPage(name: "switchPage") {
        section("Monitor sensors..."){
            input "motionSensor", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
            input "contactSensor", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			input "delayMinutes", "number", title: "Off after x minutes", required: false
        }
        section("Switch ON..."){
            input "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
            input "timeOfDay", "time", title: "At a Scheduled Time", required: false
        }
        section("Switch OFF..."){
            input "triggerModesOff", "mode", title: "System Changes Mode", required: false, multiple: true
            input "timeOfDayOff", "time", title: "At a Scheduled Time", required: false
        }
		section("Monitor illuminance sensor") {
			input "lightSensor", "capability.illuminanceMeasurement", title: "Sensor(s)?", required: false
			input "lightOnValue", "number", title: "On at < (Lux, empty = 100)?", required: false
			input "lightOffValue", "number", title: "Off at > (Lux, empty = 150)?", required: false
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
    }
}

def optionsPage() {
	dynamicPage(name: "optionsPage") {
		section("Timing options") {
			input "starting", "time", title: "Starting from", required: false
			input "ending", "time", title: "Ending at", required: false
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
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
	subscribe(motionSensor, "motion", motionHandler)
	subscribe(contactSensor, "contact", contactHandler)

	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}

	if (triggerModes || triggerModesOff) {
		subscribe(location, modeChangeHandler)
	}
	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
	if (timeOfDayOff) {
		schedule(timeOfDayOff, scheduledTimeOffHandler)
	}

}

/******************
 * Event handlers *
 ******************/

def appTouchHandler(evt) {
	log.trace "app started manually"
    activateHue()
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	if (motionValue) {
		state.motionStopTime = null
	}
	else {
    	state.motionStopTime = now()
    }
    if(delayMinutes) {
        runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
        log.info "Delay: $delayMinutes minutes"
	} 
	else {
		turnOffMotionAfterDelay()
	}
}

def motionHandler(evt) {
	log.trace "motionHandler"
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	if (allOk) {
        if (motionValue) {
            state.motionStopTime = null
            if (allOkExtra) {
	            activateHue()
            }
        }
        else {
            state.motionStopTime = now()
            if(delayMinutes) {
                runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
                log.info "Delay (motion): $delayMinutes minutes"
            } 
            else {
                turnOffMotionAfterDelay()
            }
        }
    }
    else {
        if (motionValue) {
            state.motionStopTime = null
        }
        else {
            state.motionStopTime = now()
            runIn(30*60, turnOffMotionAfterDelay, [overwrite: false])
            log.info "Delay (motion): 30 minutes (backup off switch)"
        }
    }
}

def contactHandler(evt) {
	log.trace "contactHandler"
	def current = contactSensor.currentValue("contact")
	def contactValue = contactSensor.find{it.currentContact == "open"}
	if (allOk) {
        if (contactValue) {
            state.motionStopTime = null
            if (allOkExtra) {
	            activateHue()
            }
        }
        else {
            state.motionStopTime = now()
            if(delayMinutes) {
                runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
                log.info "Delay (contact): $delayMinutes minutes"
            } 
            else {
                turnOffMotionAfterDelay()
            }
        }
    }
    else {
        if (contactValue) {
            state.motionStopTime = null
        }
        else {
            state.motionStopTime = now()
            runIn(30*60, turnOffMotionAfterDelay, [overwrite: false])
            log.info "Delay (contact): 30 minutes (backup off switch)"
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
        else if (state.motionStopTime) {
            if (state.lastStatus != "off") {
                def elapsed = now() - state.motionStopTime
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
	if (evt.value in triggerModes) {
		activateHue()
	}
	if (evt.value in triggerModesOff) {
		deactivateHue()
	}
}

def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	activateHue()
}

def scheduledTimeOffHandler() {
	log.trace "scheduledTimeOffHandler()"
	deactivateHue()
}

/******************
 * Helper methods *
 ******************/

def turnOffMotionAfterDelay() {
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
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
        lightSelect*.setColor(newValue)
        lightSelect*.setColor(newValue)
        lightSelect*.setColor(newValue)
        lightSelect*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lightSelect*.on()
    	lightSelect*.on()
    	lightSelect*.on()
        lightSelect*.on()
    	lightSelect*.on()
    }
}

def stopHue(lightSelect) {
	log.trace "Deactivating Hue '$lightSelect'"

	lightSelect*.off()
	lightSelect*.off()
	lightSelect*.off()
	lightSelect*.off()
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
