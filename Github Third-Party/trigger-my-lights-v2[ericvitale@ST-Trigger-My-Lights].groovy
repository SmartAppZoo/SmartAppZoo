/**
 *  Trigger My Lights v2
 *
 *  2.0.2 - 08/22/16
 *   -- Ability to schedule triggers to be enabled / disabled based on a time range.
 *  2.0.1 - 08/20/16
 *   -- Resolved issue where lights would turn off after the timer if the motion sensor never went inactive.
 *   -- Updated icon.
 *  2.0.0 - 08/12/16
 *   -- Parent child app.
 *   -- Dimmable lights do not adjust if they are already on.
 *  1.0.2 - 07/20/16
 *   -- Bug Fix: Resolved issue with Sunset Sunrise settings.
 *  1.0.1 - 07/18/16
 *   -- Feature: Ability to only execute between sunset and sunrise.
 *   -- Behavior Change: If motion lights will not turn off while there is still motion in the room / area with selected 
 *      motion sensors.
 *   -- Feature: Ability to only execute between a specified time range.
 *  1.0.0 - 07/11/16
 *   -- Initial Release
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
 *  You can find this smart app @ https://github.com/ericvitale/ST-Trigger-My-Lights
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
    name: "${appName()}",
    namespace: "ericvitale",
    author: "Eric Vitale",
    description: "Set on/off, level, color, and color temperature of a set of lights based on motion, acceleration, and a contact sensor.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/ev-public/st-images/trigger-my-lights-1x.png",
    iconX2Url: "https://s3.amazonaws.com/ev-public/st-images/trigger-my-lights-2x.png",
    iconX3Url: "https://s3.amazonaws.com/ev-public/st-images/trigger-my-lights-3x.png")


preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: true) {
        section("Create a new child app.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New Lighting Automation", multiple: true)
        }
    }
}

def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
    
    	section("Switches") {
			input "switches", "capability.switch", title: "Switches", multiple: true, required: false
    	}
        
        section("Dimmers") {
        	input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
            input "selectedDimmersLevel", "number", title: "Dimmer Level", description: "Set your dimmers to...", required: false, defaultValue: 100
        }
        
        /*section("Color Lights") {
        	input "colorLights", "capability.colorControl", title: "Color Lights", multiple: true, required: false
            input "selectedColorLightsColor", "enum", title: "Select Color", required: false, options: ["Blue", "Green", "Red", "Yellow", "Orange", "Pink", "Purple", "Random"]
            input "selectedColorLightsLevel", "number", title: "Level", required: false, defaultValue: 100
        }*/
        
        section("Color Temperature Lights") {
        	input "colorTemperatureLights", "capability.colorTemperature", title: "Color Temperature Lights", multiple: true, required: false
            input "selectedColorTemperatureLightsTemperature", "number", title: "Color Temperature", description: "2700 - 9000", range: "2700..9000", required: false
            input "selectedColorTemperatureLightsLevel", "number", title: "Level", defaultValue: 100, required: false
        }
        
        section("Schedule") {
        	input "useTimer", "bool", title: "Turn Off After", required: true, defaultValue: true
        	input "timer", "number", title: "Minutes", required: false, defaultValue: 10
        }
   	
    	section("Sensors") {
	        input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
            input "accSensors", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true, required: false
            input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
		}
        
        section("Modes / Routines") {
        	input "modes", "mode", title: "When Changes to Mode(s)", multiple: true, required: false
            input "routine", "text", title: "When Routine is Executed", multiple: false, required: false
        }
        
        section("Time Range") {
            input "useTimeRange", "bool", title: "Use Time Range?", required: true, defaultValue: false, submitOnChange: true
        	if(useTimeRange) {
                input "startTimeSetting", "enum", title: "Start Time", required: true, defaultValue: "None", options: ["None", "Sunrise", "Sunset", "Custom"], submitOnChange: true
                if(startTimeSetting == "Custom") {
                    input "startTimeInput", "time", title: "Custom Start Time", required: true
                } else if(startTimeSetting == "Sunrise" || startTimeSetting == "Sunset") {
                    input "startTimeOffset", "number", title: "Offset ${startTimeType} by (Mins)...", range: "*..*"
                }
                input "endTimeSetting", "enum", title: "End Time", required: true, defaultValue: "None", options: ["None", "Sunrise", "Sunset", "Custom"], submitOnChange: true
                if(endTimeSetting == "Custom") {
                    input "endTimeInput", "time", title: "Custom End Time", required: true
                } else if(endTimeSetting == "Sunrise" || endTimeSetting == "Sunset") {
                    input "endTimeOffset", "number", title: "Offset ${endTimeType} by (Mins)...", range: "*..*"
                }
            }
        }
    
	    section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    	}
	}
}

private def appName() { return "${parent ? "Light Automation" : "Trigger My Lights v2"}" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "TML -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "TML -- Invalid Log Setting"
        }
    }
}

def installed() {
	log("Begin installed.", "DEBUG")
	initialization() 
    log("End installed.", "DEBUG")
}

def updated() {
	log("Begin updated().", "DEBUG")
	unsubscribe()
    unschedule()
	initialization()
    setAllLights("off")
    log("End updated().", "DEBUG")
}

def initialization() {
	log("Begin initialization().", "DEBUG")
    
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }
    
    log("End initialization().", "DEBUG")
}

def initParent() {
	log.debug "initParent()"
}

def initChild() {
	log("Begin intialization().", "DEBUG")
    
    unsubscribe()
    unschedule()
    
    log("useTimer = ${useTimer}.", "INFO")
    log("active = ${active}.", "INFO")
    log("timer = ${timer}.", "INFO")
    
    if(useTimeRange) {
    	setupTimes()
    }
    
    if(motionSensors == null) {
    	state.motion = false
    } else {
    	state.motion = true
        motionSensors.each { it->
    		log("Selected motion sensors type = ${it.name} and label = ${it.label}.", "INFO")
    	}
    }
    
    if(contacts == null) {
    	state.contact = false
    } else {
	    state.contact = true
		contacts.each { it->
    		log("Selected contact sensor type = ${it.name} and label = ${it.label}.", "INFO")
    	}
    }
    
    if(accSensors == null) {
    	state.acceleration = false
    } else {
	    state.acceleration = true
		accSensors.each { it->
    		log("Selected acceleration sensor type = ${it.name} and label = ${it.label}.", "INFO")
    	}
    }
    
    if(modes == null) {
    	state.modes = false
    } else {
	    state.modes = true
		modes.each { it->
    		log("Selected mode = ${it.name}.", "INFO")
    	}
    }
    
    if(routines == null) {
    	state.routines = false
    } else {
	    state.routines = true
		routines.each { it->
    		log("Selected routines = ${it.name}.", "INFO")
    	}
    }
    
    if(active) {
    	if(state.motion) {
  	    	subscribe(motionSensors, "motion.active", motionHandler)
            log("Subscribed to --motion.active--.", "INFO")
        }
        if(state.acceleration) {
        	subscribe(accSensors, "acceleration.active", accelerationHandler)
            log("Subscribed to --acceleration.active--.", "INFO")
        }
        if(state.contact) {
        	subscribe(contacts, "contact.open", contactHandler)
            log("Subscribed to --contact.open--.", "INFO")
        }
        if(state.mode) {
        	subscribe(location, modeHandler)
            log("Subscribed to --mode.change--.", "INFO")
        }
        if(state.routine) {
        	subscribe(location, "routineExecuted", routineHandler)
            log("Subscribed to --routine.executed--.", "INFO")
        }
        
        log("Subscriptions to required devices made.", "INFO")   
    } else {
    	log("App is set to inactive in settings.", "INFO")
    }
    
    setRoomActive(false)

    log("End initialization().", "DEBUG")
}

def motionHandler(evt) {
	log("Begin motionHandler(evt).", "DEBUG")
    
    if(isRoomActive()) {
    	if(useTimer) {
			log("Room is still active, reseting OFF time.", "DEBUG")
			unschedule()
            setSchedule()
        } else {
        	runIn(60, resetRoomStatus)
        }
    } else {
    	triggerLights()
    }
    
    log("End motionHandler(evt).", "DEBUG")
}

def accelerationHandler(evt) {
	log("Begin accelerationHandler(evt).", "DEBUG")
    triggerLights()
    log("End accelerationHandler(evt).", "DEBUG")
}

def contactHandler(evt) {
	log("Begin contactHandler(evt).", "DEBUG")
	triggerLights()
    log("End contactHandler(evt).", "DEBUG")
}

def modeHandler(evt) {
	log("Begin modeHandler(evt).", "DEBUG")
	log("Mode changed to ${evt.value}.", "DEBUG")
    
    modes.each { it-> 
    	if(it.toLowerCase() == evt.value.toLowerCase()) {
        	log("Mode: ${it} matches input selection, triggering lights.", "DEBUG")
        	triggerLights()
            return
        }
    }
    
	log("End modeHandler(evt).", "DEBUG")
}

def routineHandler(evt) {
    log("Begin routineHandler(evt).", "DEBUG")
    
    log("routine = ${routine}.", "DEBUG")
    log("event = ${evt.displayName}.", "DEBUG")
    
    if(routine.toLowerCase() == evt.displayName.toLowerCase()) {
    	log("Routine: ${it} matches input selection, triggering lights.", "DEBUG")
        triggerLights()
     	return
    }
    log("End routineHandler(evt).", "DEBUG")
}

def triggerLights() {
	log("Begin triggerLights().", "DEBUG")
    
    log("isRoomActive = ${isRoomActive}.", "DEBUG")
    
    if(outOfRange()) {
    	log("Current time is out of range, ignoring.", "INFO")
        return
    }

    if(!isRoomActive()) {
    	setSwitches()
        setDimmers(selectedDimmersLevel)
        //setColorLights(selectedColorLightsLevel, selectedColorLightsColor)
        setColorTemperatureLights(selectedColorTemperatureLightsLevel, selectedColorTemperatureLightsTemperature)
		setRoomActive(true)
        
        if(useTimer) {
        	setSchedule()
        } else {
        	runIn(60, reset)
        }
        
        log("Lights triggered.", "INFO")
        
    } else {
    	log("Room is active, ignorining command.", "DEBUG")
    }

	log("End triggerLights().", "DEBUG")
}

def setSwitches() {
	log("Begin setSwitches().", "DEBUG")
    
    switches.each { it->
    	it.on()
    }
    
    log("End setSwitches().", "DEBUG")
}

def setDimmers(valueLevel) {
    
    dimmers.each { it->
        if(it.currentValue("switch") != "on") {
            it.setLevel(valueLevel)
        } else {
        	log("Light is already on.", "DEBUG")
        }
    }
    
    log("End setDimmers(onOff, value).", "DEBUG")
}

def setColorLights(valueLevel, valueColor) {
	log("Begin setColorLights(onOff, valueLevel, valueColor).", "DEBUG")
    def colorMap = getColorMap(valueColor)
    
	log("Color = ${valueColor}.", "DEBUG")
    log("Hue = ${colorMap['hue']}.", "DEBUG")
    log("Saturation = ${colorMap['saturation']}.", "DEBUG")
    
    colorLights.each { it->
        //it.on()
        //it.setColor(colorMap)
    	it.setHue(colorMap['hue'])
        it.setSaturation(colorMap['saturation'])
        it.setLevel(valueLevel)
    }
    
    
    log("End setColorLights(onOff, valueLevel, valueColor).", "DEBUG")
}

def setColorTemperatureLights(valueLevel, valueColorTemperature) {
	log("Begin setColorTemperatureLights(, valueLevel, valueColorTemperature).", "DEBUG")
    
    colorTemperatureLights.each { it->
    	if(it.currentValue("switch") != "on") {
            it.setLevel(valueLevel)
            it.setColorTemperature(valueColorTemperature)
        } else {
        	log("Light is already on.", "DEBUG")
        }
    }
    
    log("End setColorTemperatureLights(onOff, valueLevel, valueColorTemperature).", "DEBUG")
}

def setAllLightsOff() {
	log("Begin setAllLightsOff().", "DEBUG")
    	if(state.motion) {
        	motionSensors.each { it->
            	if(it.currentValue("motion") == "active") {
                	unschedule()
                	setSchedule()
                    log("Motion still detected, rescheduling check.", "INFO")
                    return
                }
            }
        }
        
   	setAllLights("off")
    log("Turned lights off per the schedule.", "INFO")
    
    log("End setAllLightsOff().", "DEBUG")
}

def setAllLights(onOff) {
	log("Begin setAllLights(onOff)", "DEBUG")
    
    if(onOff.toLowerCase() == "off") {
    	switches?.off()
        dimmers?.off()
        colorTemperatureLights?.off()
        colorLights?.off()
    } else {
    	switches?.on()
        dimmers?.on()
        colorTemperatureLights?.on()
        colorLights?.on()
    }
    
    setRoomActive(false)
    
    log("End setAllLights(onOff)", "DEBUG")
}

def setSchedule() {
	log("Begin setSchedule().", "DEBUG")
    if(useTimer) {
        runIn(timer*60, scheduleElapseTrigger)
        log("Setting timer to turn off lights in ${timer} minutes.", "INFO")
    }
    log("End setSchedule().", "DEBUG")
}

def scheduleElapseTrigger(){
	//See if any of the motion sensors still have motion, if they do, don't turn off.
    if(motionSensors != null) {
    	def isMotion = false
        motionSensors.each { it->
        	if(it.currentValue("motion") == "active") {
            	isMotion = true
            }
        }
        
        if(isMotion) {
        	runIn(timer*60, scheduleElapseTrigger)
            log("Motion sensor is still active. Resetting timer.", "INFO")
        } else {
        	log("Last check for motion detected no motion, turning off.", "INFO")
            setAllLightsOff()
        }
    } else {
    	setAllLightsOff()
    }
}

def reschedule() {
	log("Begin reschedule().", "DEBUG")
    unschedule()
    setSchedule()
    log("End reschedule().", "DEBUG")
}

def isRoomActive() {
	log("Begin isRoomActive() -- Has return value.", "DEBUG")
    
    if(state.roomActive == null) {
    	state.roomActive = false
    }
    
    return state.roomActive
}

def setRoomActive(val) {
	log("Being setRoomActive().", "DEBUG")
    state.roomActive = val
    log("End setRoomActive().", "DEBUG")
}

def resetRoomStatus() {
	log("Begin resetRoomStatus().", "DEBUG")
	setRoomActive(false)
	log("End resetRoomStatus().", "DEBUG")
}

def getColorMap(val) {
	
    def colorMap = [:]
    
	switch(val.toLowerCase()) {
    	case "blue":
        	colorMap['hue'] = "240"
            colorMap['saturation'] = "100"
            colorMap['level'] = "50"
            break
        case "red":
        	colorMap['hue'] = "0"
            colorMap['saturation'] = "100"
            colorMap['level'] = "50"
            break
        case "yellow":
            colorMap['hue'] = "60"
            colorMap['saturation'] = "100"
            colorMap['level'] = "50"
        default:
            colorMap['hue'] = "60"
            colorMap['saturation'] = "100"
            colorMap['level'] = "50"	
    }
    
	return colorMap
}

//// Begin Time Getters / Setters ////
def getStartTime() {
	return state.theStartTime
}

def setStartTime(val) {
	state.theStartTime = val
}

def getEndTime() {
	return state.theEndTime
}

def setEndTime(val) {
	state.theEndTime = val
}

def getStartTimeType() {
    return state.theStartTimeSetting
}

def setStartTimeType(val) {
    state.theStartTimeSetting = val
}

def getEndTimeType() {
    return state.theEndTimeSetting
}

def setEndTimeType(val) {
    state.theEndTimeSetting = val
}

def getStartTimeOffset() {
	if(state.theStartTimeOffset == null) {
    	return 0
    } else {
    	return state.theStartTimeOffset
    }
}

def setStartTimeOffset(val) {
	if(val == null) {
    	state.theStartTimeOffset = 0
    } else {
    	state.theStartTimeOffset = val
    }
}

def getEndTimeOffset() {
	if(state.theEndTimeOffset == null) {
    	return 0
    } else {
    	return state.theEndTimeOffset
    }
}

def setEndTimeOffset(val) {
	if(val == null) {
    	state.theEndTimeOffset = 0
    } else {
    	state.theEndTimeOffset = val
    }
}

def getUseStartTime() {
	return state.UsingStartTime
}

def getUseEndTime() {
	return state.UsingEndTime
}

def setUseStartTime(val) {
	state.UsingStartTime = val
}

def setUseEndTime(val) {
	state.UsingEndTime = val
}	

def getCalculatedStartTime() {
	if(getStartTimeType() == "Custom") {
    	return inputDateToDate(getStartTime())
    } else if(getStartTimeType() == "Sunset") {
    	return getSunset(getStartTimeOffset())
    } else if(getStartTimeType() == "Sunrise") {
    	return getSunrise(getStartTimeOffset())
    }
}

def getCalculatedEndTime() {
	if(getEndTimeType() == "Custom") {
    	return inputDateToDate(getEndTime())
    } else if(getEndTimeType() == "Sunset") {
    	return getSunset(getEndTimeOffset())
    } else if(getEndTimeType() == "Sunrise") {
    	return getSunrise(getEndTimeOffset())
    }	
}

//// End Time Getters / Setters ////

/////// Begin Time / Date Methods ///////////////////////////////////////////////////////////

def outOfRange() {
    if(getUseStartTime() && getUseEndTime() && useTimeRange) {
        if(isBetween(getCalculatedStartTime(), getCalculatedEndTime(), getNow())) {
       		return false
        } else {
	        return true
        }
    }
}


def setupTimes() {
	setStartTimeType(startTimeSetting)
    setEndTimeType(endTimeSetting)
    
    if(getStartTimeType() == "None") {
        setUseStartTime(false)
    } else if (getStartTimeType() == "Sunrise") {
    	setUseStartTime(true)
        setStartTimeOffset(startTimeOffset)
    } else if (getStartTimeType() == "Sunset") {
    	setUseStartTime(true)
        setStartTimeOffset(startTimeOffset)
    } else if (getStartTimeType() == "Custom") {
    	setUseStartTime(true)
        setStartTime(startTimeInput)
    }
    
    if(endTimeType == "None") {
    	setUseEndTime(false)
    } else if (endTimeType == "Sunrise") {
    	setUseEndTime(true)
        setEndTimeOffset(endTimeOffset)
    } else if (endTimeType == "Sunset") {
    	setUseEndTime(true)
        setEndTimeOffset(endTimeOffset)
    } else if (endTimeType == "Custom") {
    	setUseEndTime(true)
        setEndTime(endTimeInput)
    }
}

def getNow() {
	return new Date()
}

def minutesBetween(time1, time2) {
	return (time1.getTime() - time2.getTime())/1000/60
}

def isBefore(time1, time2) {
	if(minutesBetween(time1, time2) <= 0) {
    	return true
    } else {
    	return false
    }
}

def isAfter(time1, time2) {
	if(minutesBetween(time1, time2) > 0) {
    	return true
    } else {
    	return false
    }
}

def isBetween(time1, time2, time3) {
	if(isAfter(time1, time2)) {
        time2 = time2 + 1
    }
    
    if(isAfter(time3, time1) && isBefore(time3, time2)) {
    	return true
    } else {
    	return false
    }
}

def getSunset() {
	return getSunset(0)
}

def getSunrise() {
	return getSunrise(0)
}

def getSunset(offset) {
	def offsetString = getOffsetString(offset)
	return getSunriseAndSunset(sunsetOffset: offsetString).sunset
}

def getSunrise(offset) {
	def offsetString = getOffsetString(offset)
	return getSunriseAndSunset(sunriseOffset: "${offsetString}").sunrise
}

def getOffsetString(offsetMinutes) {
	int hours = Math.abs(offsetMinutes) / 60; //since both are ints, you get an int
	int minutes = Math.abs(offsetMinutes) % 60;
    def sign = (offsetMinutes >= 0) ? "" : "-"
	def offsetString = "${sign}${hours.toString().padLeft(2, "0")}:${minutes.toString().padLeft(2, "0")}"
	return offsetString
}

def inputDateToDate(val) {
	return Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", val)
}

def dateToString(val) {
	log("val = ${val}.", "DEBUG")
	return val.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}

/////// End Time / Date Methods ///////////////////////////////////////////////////////////