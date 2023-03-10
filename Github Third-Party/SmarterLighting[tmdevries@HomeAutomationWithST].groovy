/**
 *  Smarter Lighting Automation
 *
 *  Copyright 2016 Tara De Vries
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
    name: "Smarter Lighting Automation",
    namespace: "tmdevries",
    author: "Tara De Vries",
    description: "Set up an automation that can be overriden by a switch such as a a virtual override switch and determine optional conditions.",
    parent: "tmdevries:Smarter Automations",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page name: "setupPage"
    page name: "optionsPage"
    page name: "namePage"
}

def setupPage() {
	dynamicPage(name: "setupPage", title: "Automation Setup", nextPage: "optionsPage", uninstall: true) {
    	section {
            input "lights", "capability.switch", title: "Which lights do you want to control?", multiple: true, required: true, submitOnChange: true
            input "action", "enum", title: "What do you want to do?", required: true, submitOnChange: true, options: actionOptions()
        }
        if (action == "Turn On & Set Level") {
        	section {
            	if (!calculate) input "dimmerLevel", "number", title: "Which level?", required: false, submitOnChange: true
                input "calculate", "bool", title: "Calculate level using illuminance measurement?", defaultValue: false, submitOnChange: true
                if (calculate) {
                	input "lightSensor", "capability.illuminanceMeasurement", title: "Get illuminance measurement from which sensor?", required: true, multiple: false
                	input "darkLevel", "number", title: "What is the highest level at which you want lights to turn on to 100%? (default is 0)", required: true, defaultValue: 0
                    input "brightLevel", "number", title: "At which level do you want lights to begin turning on?", required: true, defaultValue: 100
                }	
            }
        }
        section {
               	input "sensorType", "enum", title: "Select trigger", required: true, options: [
                    "contactSensor": "Open/Closed",
                    "motionSensor": "Motion",
                    "switch": "Switch",
                    "presence": "Presence",
                    "lock": "Lock",
                    "illuminance": "Illuminance",
                    "acceleration": "Acceleration",
                    "sunrise": "At Sunrise",
                    "sunset": "At Sunset",
                    "time": "At a Specific Time",
                    "modeChange": "When Mode Changes"], submitOnChange: true
        }
        if (sensorType) {
            sensorOptions(sensorType)
        }
	}
}

def actionMap() {
    def map = ["Turn On", "Turn Off"]
    if (lights.find{it.hasCommand('setLevel')} != null) {
        map << "Turn On & Set Level"
    }
    map
}

def actionOptions() {
    actionMap()
}


def sensorOptions(sensor) {
	switch(sensor) {
		case "contactSensor":
        	return section {
            	input "contactSensorList", "capability.contactSensor", title: "Which open/close sensor(s)?", multiple: true
                input "contactSensorState", "enum", title: "$action when", submitOnChange: true, options: ["opened":"Opened", "closed":"Closed"], defaultValue: "opened"
            	input "oppositeAction", "bool", title: actionOpposite() + " When " + (contactSensorState == "opened"? "Closed":"Opened"), defaultValue: false
            }
        case "motionSensor":
        	return section {
            	input "motionSensorList", "capability.motionSensor", title: "Which motion sensor(s)?", multiple: true
                input "motionSensorState", "enum", title: "$action When", options: ["motion starts":"Motion Starts", "motion stops":"Motion Stops"], submitOnChange: true
            	if (motionSensorState == "motion starts") {
                	input "oppositeAction", "bool", title: actionOpposite() + " After Motion Stops", defaultValue: false, submitOnChange: true
                }
                if (oppositeAction || motionSensorState == "motion stops") input "motionStopMinutes", "number", title: "After This Number of Minutes", required: false
            }
        case "switch":
        	return section {
            	input "switchList", "capability.switch", title: "Which switch(es)?", multiple: true
                input "switchState", "enum", title: "$action When", defaultValue: "turned on", options: ["turned on":"Turned On", "turned off":"Turned Off"]
            	input "oppositeAction", "bool", title: actionOpposite() + " As Well", defaultValue: false
            }
        case "presence":
        	return section {
            	input "presenceSensorList", "capability.presenceSensor", title: "Which presence sensor(s)?", multiple: true
                input "presenceSensorState", "enum", title: "$action When", defaultValue: "someone arrives", options: ["someone arrives":"Someone Arrives", "everyone leaves":"Everyone Leaves"]
            }
        case "lock":
        	return section {
            	input "lockList", "capability.lock", title: "Which lock(s)?", multiple: true
                input "lockState", "enum", title: "$action When", defaultValue: "unlocked", options: ["unlocked":"Unlocked", "locked":"Locked"]
            }
        case "illuminance":
        	return section {
            	input "lightSensorList", "capability.illuminanceMeasurement", title: "Which illuminance sensor(s)?", multiple: true
                input "lightSensorState", "enum", title: "$action Lights if Illuminance is", submitOnChange: true, options: 
                	["equal or lower":"Equal or Lower", 
                    "equal or higher":"Equal or Higher",
                    "between":"Between"]
            	if (lightSensorState == "between") {
                	input "lightLowerLimit", "number", title: "This Lower Limit", defaultValue: 0
                    input "lightUpperLimit", "number", title: "And This Upper Limit", defaultValue: 0
                } else {
                    input "lightLevel", "number", title: "Than", defaultValue: 0
                }
            }
        case "acceleration":
        	return section {
            	input "accelerationSensorList", "capability.accelerationSensor", title: "Which acceleration sensor(s)?", multiple: true
                input "accelerationSensorState", "enum", title: "$action When", options: ["acceleration starts", "acceleration stops"]
            	input "oppositeAction", "bool", title: actionOpposite() + " When " + (accelerationSensorState == "acceleration starts"? "Acceleration Stops":"Acceleration Starts"), defaultValue: false
            }
        case "sunrise":
        	return section {
            	input "sunriseOffset", "number", title: "Sunrise offset in minutes (+/-)", defaultValue: 0
                input "oppositeAction", "bool", title: "Also " + actionOpposite() + " at Sunset", defaultValue: false, submitOnChange: true
            	if (oppositeAction) input "sunsetOffset", "number", title: "Sunset offset in minutes (+/-)", defaultValue: 0
            }
        case "sunset":
        	return section {
            	input "sunsetOffset", "number", title: "Sunset offset in minutes (+/-)", defaultValue: 0
                input "oppositeAction", "bool", title: "Also " + actionOpposite() + " at Sunrise", defaultValue: false, submitOnChange: true
                if (oppositeAction) input "sunriseOffset", "number", title: "Sunrise offset in minutes (+/-)", defaultValue: 0
            }
        case "time":
        	return section {
            	input "actionTime", "time", title: "$action at"
            	input "oppositeActionTime", "time", title: "Also " + actionOpposite() + " at", required: false
            }
        case "modeChange":
        	return section {
            	mode name: "modes", title: "$action When Mode Changes to", multiple: true
            }
    }
}

def actionOpposite() {
	switch(action) {
    	case "Turn Off":
        	return "Turn On"
        default:
        	return "Turn Off"
    }
}
    	
def optionsPage() {
	dynamicPage(name: "optionsPage", title: "More Options", nextPage: "namePage", uninstall: true) {
    	section {
        	if (action != "time") {
            	input "timeThreshold", "bool", title: "Only during a certain time", defaultValue: false, submitOnChange: true
                if (timeThreshold) {
                	input "startOption", "enum", title: "Starting at", defaultValue: "a specific time", options: ["a specific time", "sunrise", "sunset"], submitOnChange: true
                    if (startOption == "a specific time") {
                    	input "startTime", "time", title: "Start time", required: false
                    } else if (startOption == "sunset" || startOption == "sunrise") {
                    	input "startOffset", "number", title: "Offset in minutes (+/-)", defaultValue: 0, required: false
                    }
                    input "endOption", "enum", title: "Ending at", defaultValue: "a specific time", options: ["a specific time", "sunrise", "sunset"], submitOnChange: true
                    if (endOption == "a specific time") {
                    	input "endTime", "time", title: "End time", required: false
                    } else if (endOption == "sunset" || endOption == "sunrise") {
                    	input "endOffset", "number", title: "Offset in minutes (+/-)", defaultValue: 0, required: false
                    }
                }
            }
            input "daysLimit", "enum", title: "Only on certain days of the week", options: ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"], required: false, multiple: true
            if (action != "modeChange") mode name: "modes", title: "Only when mode is", required: false // prevent a clash in variables
            input "override", "bool", title: "Allow switch(es) to override automation?", defaultValue: false, required: false, submitOnChange: true
            if (override) {
                input "overrideSwitches", "capability.switch", title: "Which switch(es) will override the automation?", required: false, multiple: true
            }
            input "dependence", "bool", title: "Require another sensor to have a specific state?", defaultValue: false, required: false, submitOnChange: true
            if (dependence) {
            	input "dependenceSensor", "enum", title: "Select sensor", required: true, options: [
                    "contact": "Open/Closed",
                    "motion": "Motion",
                    "switch": "Switch",
                    "illuminance" : "Illuminance"], submitOnChange: true
                if (dependenceSensor) {
                	input "dependenceDevice", dependenceCapability(dependenceSensor), title: "Which one?", required: true, multiple: false
                	input "dependenceState", "enum", 
                    	title: "Only Perform Action If Sensor Has This State", 
                        required: true,
                        options: dependenceStateEnum(dependenceSensor)
                    if (dependenceSensor == "illuminance") {
                    	if (dependenceState == "between") {
                			input "lightLowerLimit2", "number", title: "This Lower Limit", defaultValue: 0
                    		input "lightUpperLimit2", "number", title: "And This Upper Limit", defaultValue: 0
                		} else {
                    		input "lightLevel2", "number", title: "Than", defaultValue: 0
                		}
                    }
                }
            }         
        }
    }
}

def dependenceCapability(dependenceType) {
	switch (dependenceType) {
    	case "contact":
        	return "capability.contactSensor"
        case "motion":
        	return "capability.motionSensor"
        case "switch":
        	return "capability.switch"
        case "presence":
        	return "capability.presenceSensor"
        case "lock":
        	return "capability.lock"
        case "illuminance":
        	return "capability.illuminanceMeasurement"
    }
}

def dependenceStateEnum(dependenceType) {
	switch (dependenceType) {
    	case "contact":
        	return ["open":"Opened","closed":"Closed"]
        case "motion":
        	return ["active":"Active","inactive":"Inactive"]
        case "switch":
        	return ["on":"On","off":"Off"]
        case "presence":
        	return ["present":"All Present", "not present":"None Are Present"]
        case "lock":
        	return ["locked":"Locked", "unlocked":"Unlocked"]
        case "illuminance":
        	return ["lower":"Equal or Lower", 
                    "greater":"Equal or Higher",
                    "between":"Between"]
    }
}

def namePage() {
    dynamicPage(name: "namePage", title: "Customize Automation Name", install: true, uninstall: true) {
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
        if (overrideLabel) {
        	section("Automation name") {
            	label name: "newAppName", title: "Enter custom name if desired", defaultValue: (app.label == "Smarter Lighting Automation" ? defaultLabel() : app.label), required: false
            }
        } else {
            section("Automation name") {
                paragraph defaultLabel()
            }
        }
    }
}

def defaultLabel() {
	def lightsList = []
    lights.each {
    	lightsList << it.displayName
    }
    def lightsLabel = lightsList.size == 1 ? lightsList[0] : lightsList[0] + ", etc..."
	"$action $lightsLabel"
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	if (timeThreshold) {
    	def window = getWindow()
        if (window.end.get(Calendar.HOUR_OF_DAY) < window.start.get(Calendar.HOUR_OF_DAY)) state.crossesMidnight = true
        else state.crossesMidnight = false
    }
	switch(sensorType) {
    	case "contactSensor":
        	subscribe(contactSensorList, "contact", contactHandler)
            log.debug "Subscribed to contact sensor(s) $contactSensorList"
            break
        case "motionSensor":
        	subscribe(motionSensorList, "motion", motionHandler)
            log.debug "Subscribed to motion sensor(s) $motionSensorList"
            break
        case "switch":
        	subscribe(switchList, "switch", switchHandler)
            log.debug "Subscribed to switch(es) $switchList"
            break
        case "presence":
        	subscribe(presenceSensorList, "presence", presenceHandler)
            log.debug "Subscribed to presence device(s) $presenceSensorList"
            break
        case "lock":
        	subscribe(lockList, "lock", lockHandler)
            log.debug "Subscribed to lock(s) $lockList"
            break
        case "illuminance":
        	subscribe(lightSensorList, "illuminance", illuminanceHandler)
            log.debug "Subscribed to light sensor(s) $lightSensorList"
            break
        case "acceleration":
        	subscribe(accelerationSensorList, "acceleration", accelerationHandler)
            log.debug "Subscribed to acceleration sensor(s) $accelerationSensorList"
            break
        case "sunrise":
        	subscribe(location, "sunriseTime", sunriseTimeHandler)
            log.debug "Subscribed to sunrise event"
			//schedule it to run today too
   			scheduleTurnOn(location.currentValue("sunriseTime"), sunriseOffset)
            if (oppositeAction) {
            	subscribe(location, "sunsetTime", sunsetTimeHandler)
                log.debug "Subscribed to sunset event too."
				//schedule it to run today too
    			scheduleTurnOn(location.currentValue("sunsetTime"), sunsetOffset)
            }
            break
        case "sunset":
        	subscribe(location, "sunsetTime", sunsetTimeHandler)
            log.debug "Subscribed to sunset event"
			//schedule it to run today too
    		scheduleTurnOn(location.currentValue("sunsetTime"), sunsetOffset)
            if (oppositeAction) {
            	subscribe(location, "sunriseTime", sunriseTimeHandler)
                log.debug "Subscribed to sunrise event too."
				//schedule it to run today too
   				scheduleTurnOn(location.currentValue("sunriseTime"), sunriseOffset)
            }
            break
        case "time":
        	if (action == "Turn Off") {
            	schedule(actionTime, turnOffLights)
               	if (oppositeActionTime) {
                	schedule(oppositeActionTime, turnOnLights)
                }
            } else if (action == "Turn On & Set Level") {
            	schedule(actionTime, turnOnLightsLevel)
                if (oppositeActionTime) {
                	schedule(oppositeActionTime, turnOffLights)
                }
            } else {
            	schedule(actionTime, turnOnLights)
                if (oppositeActionTime) {
                	schedule(oppositeActionTime, turnOffLights)
                }
            }
            break
        case "modeChange":
        	subscribe(location, "mode", modeChangeHandler)
            log.debug "Subscribed to mode change to $modes"
            break
    }
    def appNameChanged = (app.label == defaultLabel() || app.label == newAppName ? true : false)
    if (appNameChanged) {
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = defaultLabel()
            log.debug "Will set default label of $l"
        } else {
            def l = newAppName
            log.debug "New automation name is $newAppName"
        }
        app.updateLabel(l)
    }
}

def contactHandler(evt) {
    // cases include: closed was selected, opened was selected, and if the "opposite action" should happen if the "opposite state" occurs
    if ((evt.value == "closed" && contactSensorState == "closed") || (evt.value == "open" && contactSensorState == "opened")) {
    	executeAutomation()
    } else if ((evt.value == "closed" && contactSensorState == "opened" && oppositeAction) 
       		  || (evt.value == "open" && contactSensorState == "closed" && oppositeAction)) {
        executeOpposite()
    }
}

def motionHandler(evt) {
	// make sure that state matches the desired subscription
    if (evt.value == "active" && motionSensorState == "motion starts") {
    	if (state.scheduledToChange) {
            unschedule()
            state.scheduledToChange = false
		}
        executeAutomation()
    } else if (evt.value == "inactive" && motionSensorState == "motion stops" ) {
    	if (isUnhindered() && !isOverridden()) {
            state.scheduledToChange = true
            log.debug "Scheduling lights to perform action in $motionStopMinutes minute" + (motionStopMinutes == 1? "":"s")
            runIn(motionStopMinutes*60, executeAutomation)  
        } // prevent scheduling if the action is not valid under the specified conditions
    } else if (evt.value == "inactive" && motionSensorState == "motion starts" && oppositeAction) {
    	if (isUnhindered() && !isOverridden()) {
            state.scheduledToChange = true
            log.debug "Scheduling lights to perform action in $motionStopMinutes minute" + (motionStopMinutes == 1? "":"s")
            runIn(motionStopMinutes*60, executeOpposite)
		}
    }
}

def switchHandler(evt) {
	if ((evt.value == "off" && switchState == "turned off") || (evt.value == "on" && switchState == "turned on")) {
    	executeAutomation()
    } else if ((evt.value == "off" && switchState == "turned on" && oppositeAction) 
       		  || (evt.value == "open" && switchState == "turned off" && oppositeAction)) {
        executeOpposite()
    }
}

def presenceHandler(evt) {
	if (evt.value == "present" && presenceSensorState == "someone arrives") {
    	executeAutomation()
    } else {
    	if (presenceSensorState == "everyone leaves") {
            def everyoneGone = true
            presenceSensorList.each {
                if (it.currentPresence == "present") everyoneGone = false
            }
            if (everyoneGone) executeAutomation()
        }
    }
}

def lockHandler(evt) {
	if (evt.value == lockState) executeAutomation()
}

def illuminanceHandler(evt) {
	if ((lightSensorState == "equal or lower" && evt.integerValue <= lightLevel) ||
    	(lightSensorState == "equal or greater" && evt.integerValue >= lightLevel) ||
        (lightSensorState == "between" && evt.integerValue < lightUpperLimit && evt.integerValue > lightLowerLimit)) {
        executeAutomation()
    }
}

def accelerationHandler(evt) {
	if ((evt.value == "active" && accelerationSensorState == "acceleration starts") || (evt.value == "inactive" && contactSensorState == "acceleration stops")) {
    	executeAutomation()
    } else if ((evt.value == "active" && contactSensorState == "acceleration stops" && oppositeAction) 
       		  || (evt.value == "inactive" && contactSensorState == "acceleration starts" && oppositeAction)) {
        executeOpposite()
    }
}

def sunsetTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
    scheduleTurnOn(evt.value, sunsetOffset)
}

def sunriseTimeHandler(evt) {
    //when I find out the sunrise time, schedule the lights to turn on with an offset
    scheduleTurnOn(evt.value, sunriseOffset)
}

def modeChangeHandler(evt) {
	if (checkMode()) executeAction()
}

def scheduleTurnOn(timeString, offset) {
    //get the Date value for the string
    def thisTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timeString)

    //calculate the offset
    def timeToSchedule = new Date(thisTime.time - (offset * 60 * 1000))

    log.debug "Scheduling for: $timeToSchedule"

    //schedule this to run one time
    runOnce(timeToSchedule, turnOnLights)
}

def executeOpposite() {
	executeAutomation(true)
}

def executeAutomation(isOppositeAction = false) {
	if (isUnhindered() && !isOverridden()) {
    	log.debug "This automation is unhindered and not overriden."
		if (action == "Turn On & Set Level") {
        	if (!isOppositeAction) {
                turnOnLightsLevel()
            } else turnOffLights()
        } else if (action == "Turn On") {
        	if (!isOppositeAction) turnOnLights()
            else turnOffLights()
        } else {
        	if (!isOppositeAction) turnOffLights()
            else turnOnLights()
        }
	}        
}

def isUnhindered() {
	def dayOk = checkDay()
    def timeOk = checkTime()
    def modeOk = checkMode()
    def dependenceOk = checkDependence()
    log.debug "dayOk = $dayOk; timeOk = $timeOk; modeOk = $modeOk; dependenceOk = $dependenceOk"
    return dayOk && timeOk && modeOk && dependenceOk
}

def isOverridden() {
	def automationOverridden = false
    if (overrideSwitches) {
        overrideSwitches.each {
            if (it.currentSwitch == "on") {
            	log.debug "${app.label} Automation is overriden by $it"
                automationOverridden = true
            }
        }
    }
    automationOverridden
}

def checkDay() {
	if (daysLimit) {
        def today = Calendar.getInstance(location.timeZone, Locale.US)
        def dayOfWeek = today.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US)
        def todayOk = false
        daysLimit.each {
        	if (dayOfWeek == it) todayOk = true
        }
        return todayOk
    } else return true
}

def checkTime() {
	if (timeThreshold) {
    	def window = getWindow()
        def now = Calendar.getInstance(location.timeZone, Locale.US)
        if (!state.crossesMidnight) {
        	return (now.after(window.start) && now.before(window.end))
        } else {
        	def yesterdayStartWindow = Calendar.getInstance(location.timeZone, Locale.US)
            yesterdayStartWindow.setTime(window.start.getTime())
            def tomorrowEndWindow = Calendar.getInstance(location.timeZone, Locale.US)
            tomorrowEndWindow.setTime(window.end.getTime())
            yesterdayStartWindow.add(Calendar.DATE, -1)
            tomorrowEndWindow.add(Calendar.DATE, 1)
            return ((yesterdayStartWindow.before(now) && now.before(window.end)) || 
            		(now.after(window.start) && tomorrowEndWindow.after(now)))
        }
    } else return true
}

def getWindow() {
	def window = [start: getTime(startOption, startTime, (startOffset ? startOffset : 0)), end:  getTime(endOption, endTime, (endOffset ? endOffset : 0))]
	window
}

def getTime(option, time, offset) {
	def calendar = Calendar.getInstance(location.timeZone, Locale.US)
	switch (option) {
        case "a specific time":
        	def now = Calendar.getInstance(location.timeZone, Locale.US)
            calendar.setTime(Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", time))
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DATE, now.get(Calendar.DATE))
            break
        case "sunrise":
            def sunriseTime = getSunriseAndSunset().sunrise
            calendar.setTimeInMillis(sunriseTime.getTime() + offset * 60 * 1000)
            break
        case "sunset":
            def sunsetTime = getSunriseAndSunset().sunset
            calendar.setTimeInMillis(sunsetTime.getTime() + offset * 60 * 1000)
            break
    }
    calendar
}

def checkMode() {
	if (modes) {
        def currMode = location.mode
        def currModeOk = false
        modes.each {
        	if (currMode == it) currModeOk = true
        }
        return currModeOk
    } else return true
}

def checkDependence() {
	if (dependence) {
    	switch (dependenceSensor) {
        	case "contact":
            	if (dependenceDevice.currentContact == dependenceState) return true
                else return false
            case "motion":
            	if (dependenceDevice.currentMotion == dependenceState) return true
                else return false
            case "switch":
            	if (dependenceDevice.currentSwitch == dependenceState) return true
                else return false
        	case "illuminance": 
                if (dependenceState == "between") {
                    if (dependenceDevice.currentIlluminance >= lightLowerLimit2 && dependenceDevice.currentIlluminance <= lightUpperLimit2) return true
                    else return false
                } else if (dependenceState == "lower") {
                    if (dependenceDevice.currentIlluminance <= lightLevel2) return true
                    else return false
                } else {
                    if (dependenceDevice.currentIlluminance >= lightLevel2) return true
                    else return false
                }
        }
   	} else return true
}

def turnOnLights() {
    if (lights.currentSwitch != "on") {
    	log.debug "${app.label} turning on lights"
    	lights.on()
        state.scheduledToChange = false
    }
}

def turnOnLightsLevel() {
	if (lights.currentSwitch != "on") {
    	def level = 0
    	if (calculate) {
        	level = getCalculatedLevel()
        } else {
        	level = dimmerLevel
        }
        if (level > 0) {
        	lights.each {
            	if (it.hasCommand('setLevel')) {
                    it.setLevel(level)
                } else {
    				it.on()
                }
            }
        }
        log.debug "${app.label} turning on lights to level ${level}"
        state.scheduledToChange = false
    }
}

def turnOffLights() {
	if (lights.currentSwitch != "off") {
        log.debug "${app.label} turning lights off"
        lights.off()
        state.scheduledToChange = false
    }
}

def getCalculatedLevel() {
	def denominator = brightLevel - darkLevel
    def numerator = lightSensor.currentIlluminance - brightLevel
   	Math.min(100, Math.round(numerator/denominator * -100))
}
    
