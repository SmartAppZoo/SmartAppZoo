/**
 *  Trigger My Lights
 *
 *  1.0.3 - 07/26/16
 *   -- Feature: Added the ability to use a switch or set switches as a trigger.
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
	name: "Trigger My Lights",
	namespace: "ericvitale",
	author: "ericvitale@gmail.com",
	description: "Set on/off, level, color, and color temperature of a set of lights based on motion, acceleration, and a contact sensor.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	page name: "mainPage"
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
    
    	section("Switches") {
			input "switches", "capability.switch", title: "Switches", multiple: true, required: false
    	}
        
        section("Dimmers") {
        	input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
            input "selectedDimmersLevel", "number", title: "Dimmer Level", description: "Set your dimmers to...", required: false, defaultValue: 100
        }
        
        /*section("Color Lights") {
        	input "colorLights", "capability.colorControl", title: "Color Lights", multiple: true, required: false
            input "selectedColorLightsColor", "enum", title: "Select Color", required: false, options: ["Blue", "Green", "Red", "Yello", "Orange", "Pink", "Purple", "Random"]
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
   	
    	section("Triggers") {
	        input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
            input "accSensors", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true, required: false
            input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
            input "tSwitches", "capability.switch", title: "Switches", multiple: true, required: false
            input "tSwitchesEvents", "enum", title: "Switch Trigger", required: false, multiple: false, options: ["On", "Off"]
            input "tSwitchesUnschedule", "bool", title: "Unschedule for Opposite Trigger?", required: false, defaultValue: true
		}
        
        section("Modes / Routines") {
        	input "modes", "mode", title: "When Changes to Mode(s)", multiple: true, required: false
            input "routine", "text", title: "When Routine is Executed", multiple: false, required: false
            //input "routines", "routine", title: "When Routine(s) is Executed", multiple: true, required: false
            
        }
        
        section("Follow the Sun") {
        	//input "modes", "mode", title: "Only in Modes", multiple: true, required: false
            input "useTheSun", "bool", title: "Follow sunset / sunrise?", required: true, defaultValue: false
            input "sunriseOffset", "number", title: "Sunrise Offset", range: "-720..720", required: true, defaultValue: 0
           	input "sunsetOffset", "number", title: "Sunset Offset", range: "-720..720", required: true, defaultValue: 0            
        }
        
        section("Time Range") {
            input "useTimeRange", "bool", title: "Use Custom Time Range?", required: true, defaultValue: false
            input "startTime", "time", title: "Start Time", required: false
            input "endTime", "time", title: "End Time", required: false
        }
    
	    section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false)
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    	}
	}
}

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
                log.error "TML -- ${device.label} -- Invalid Log Setting"
        }
    }
}

def installed() {   
	log("Begin installed.", "DEBUG")
	initalization() 
    log("End installed.", "DEBUG")
}

def updated() {
	log("Begin updated().", "DEBUG")
	unsubscribe()
    unschedule()
	initalization()
    setAllLights("off")
    log("End updated().", "DEBUG")
}

def initalization() {
	log("Begin intialization().", "DEBUG")
    
    log("useTimer = ${useTimer}.", "INFO")
    log("active = ${active}.", "INFO")
    log("timer = ${timer}.", "INFO")
    log("useTheSun = ${useTheSun}.", "INFO")
    
    if(useTheSun == true) {
    	if(sunriseOffset == null) { sunriseOffset = 0 }
        if(sunsetOffset == null) { sunsetOffset = 0 }
   	}
    
    log("sunsetOffset = ${sunsetOffset} ---> ${getOffsetString(sunsetOffset)}.", "INFO")
    log("sunriseOffset = ${sunriseOffset} ---> ${getOffsetString(sunriseOffset)}.", "INFO")
    log("Sunrise with Offset of ${sunriseOffset} = ${getSunrise(getOffsetString(sunriseOffset))}.", "INFO")
    log("Sunset with Offset of ${sunsetOffset} = ${getSunset(getOffsetString(sunsetOffset))}.", "INFO")
    
    log("Use Time Range = ${useTimeRange}.", "DEBUG")
    
    if(useTimeRange == true) {
	    if(startTime == null || endTime == null) {
    		useTimeRange = false
            log("Invalid start/end time, turning time range control off.", "ERROR")
    	} else {
        	log("Start Time = ${startTime}.", "DEBUG")
            log("End Time = ${endTime}.", "DEBUG")
        }
    }
    
    if(useTheSun == true && useTimeRange == true) {
    	log("Both 'Use the Sun' & 'Use Time Range' enabled, defaulting to 'Use the Sun', check your settings!", "ERROR")
        useTimeRange = false
    }
    
    if(active) {
    	subscribe(motionSensors, "motion.active", motionHandler)
        subscribe(accSensors, "acceleration.active", accelerationHandler)
        subscribe(contacts, "contact.open", contactHandler)
        subscribe(location, modeHandler)
        subscribe(location, "routineExecuted", routineHandler)
        subscribe(tSwitches, "switch", switchHandler)
        
        log("Subscriptions to devices made.", "INFO")   
    } else {
    	log("App is set to inactive in settings.", "INFO")
    }
    
    setRoomActive(false)

    log("End initialization().", "DEBUG")
}

def switchHandler(evt) {
	log("Begin switchHandler(evt).", "DEBUG")
	if(evt.value in tSwitchesEvents) {
    	triggerLights()
    } else {
    	if(tSwitchesUnschedule) {
            log("Opposite trigger, unscheduling due to switch change.", "INFO")
    		unschedule()
        }
    }
    log("End switchHandler(evt).", "DEBUG")
}

def motionHandler(evt) {
	log("Begin motionHandler(evt).", "DEBUG")
    
    log("isRoomActive = ${isRoomActive()}.", "DEBUG")
    
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
    
    def currentDate = new Date()
    log("currentDate = ${currentDate}.", "DEBUG")
    log("sunrise = ${getSunrise(getOffsetString(sunriseOffset))}.", "DEBUG")
    log("sunset = ${getSunset(getOffsetString(sunsetOffset))}.", "DEBUG")
    
    def sunrise = getSunrise(getOffsetString(sunriseOffset))
    def sunset = getSunset(getOffsetString(sunsetOffset))
    
    log("isAfter = ${isAfter(currentDate, sunset)}.", "DEBUG")
    log("isBefore = ${isBefore(currentDate, sunrise)}.", "DEBUG")
    
    if(useTheSun) {
        if(isAfter(currentDate, getSunset(getOffsetString(sunsetOffset))) || isBefore(currentDate, getSunrise(getOffsetString(sunriseOffset)))) {
        	log("The sun is down! OK!", "DEBUG")
        } else {
        	log("Does not meet useTheSun criteria!", "DEBUG")
            return
        }
    }
    
    if(useTimeRange) {
    	if(isBefore(currentDate, inputDateToDate(startTime)) && isAfter(currentDate, inputDateToDate(endTime))) {
        	log("Time is outside of time range, ignoring triggers.", "DEBUG")
            return
        } else {
        	log("Time is within time range!", "DEBUG")
        }
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
    	//if(it.currentValue("switch").toLowerCase() == "off") {
    		it.on()
        //}
    }
    
    log("End setSwitches().", "DEBUG")
}

def setDimmers(valueLevel) {
    
    dimmers.each { it->
   		//if(it.currentValue("switch").toLowerCase() == "off") {
    		//it.on()
            it.setLevel(valueLevel)
        //}
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
        it.on()
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
    	//if(it.currentValue("switch").toLowerCase() == "off") {
	        it.setLevel(valueLevel)
    	    it.setColorTemperature(valueColorTemperature)
            it.on()
        //} else {
        //	log("Ignoring ${it.label} as it is already on.", "INFO")
        //}
    }
    
    log("End setColorTemperatureLights(onOff, valueLevel, valueColorTemperature).", "DEBUG")
}

def setAllLightsOff() {
	log("Begin setAllLightsOff().", "DEBUG")
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
    	runIn(timer*60, setAllLightsOff)
        log("Setting timer to turn off lights in ${timer} minutes.", "INFO")
    }
    log("End setSchedule().", "DEBUG")
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

/////// Begin Time / Date Methods ///////////////////////////////////////////////////////////

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

def getSunset() {
	return getSunset("00:00")
}

def getSunrise() {
	return getSunrise("00:00")
}

def getSunset(offset) {
	return getSunriseAndSunset(sunsetOffset: offset).sunset
}

def getSunrise(offset) {
	return getSunriseAndSunset(sunriseOffset: offset).sunrise
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

def beforeSunrise() {
	def currentDate = new Date()
    
    if(isBefore(currentDate, getSunrise())) {
    	return true
    } else {
    	return false
    }
}

/////// End Time / Date Methods ///////////////////////////////////////////////////////////