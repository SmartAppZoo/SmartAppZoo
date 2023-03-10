/**
 *  Smarter Garage Opener
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
    name: "Smarter Garage Opener",
    namespace: "tmdevries",
    author: "Tara De Vries",
    description: "Set up presence sensors to open/close the garage door with smarter options. Meant to solve the problem of a presence sensor giving false positives.",
    parent: "tmdevries:Smarter Automations",
    singleInstance: true,
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page name: "setupPage"
    page name: "optionsPage"
}

def setupPage() {
	dynamicPage(name: "setupPage", title: "Automation Setup", nextPage: "optionsPage", uninstall: true) {
    	section {
            paragraph "Select the presence sensors that will affect garage door automation. " +
                      "Garage door will open when any of these sensors arrives, and close when any leaves."
            input "presenceSensorList", "capability.presenceSensor", title: "Which presence devices?", required: true, multiple: true
            input "garageDoor", "capability.garageDoorControl", title: "Select your garage door", required: true
            input "threshold", "number", title: "Indicate the false positive avoidance threshold (in minutes)", required: true
      	}
    }
}

def optionsPage() {
	dynamicPage(name: "optionsPage", title: "More Options", uninstall: true, install: true) {
    	section {
            input "timeThreshold", "bool", title: "Only during a certain time", defaultValue: false, submitOnChange: true
            if (timeThreshold) {
                input "startOption", "enum", title: "Starting at", defaultValue: "a specific time", options: ["a specific time", "sunrise", "sunset"], submitOnChange: true
                if (startOption == "a specific time") {
                    input "startTime", "time", title: "Start time", required: false
                } else if (startOption == "sunset" || startOption == "sunrise") {
                    input "startOffset", "number", title: "Offset in minutes (+/-)", required: false
                }
                input "endOption", "enum", title: "Ending at", defaultValue: "a specific time", options: ["a specific time", "sunrise", "sunset"], submitOnChange: true
                if (endOption == "a specific time") {
                    input "endTime", "time", title: "End time", required: false
                } else if (endOption == "sunset" || endOption == "sunrise") {
                    input "endOffset", "number", title: "Offset in minutes (+/-)", required: false
                }
            }
            input "daysLimit", "enum", title: "Only on certain days of the week", options: ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"], required: false, multiple: true
            mode name: "modes", title: "Only when mode is", required: false 
            input "override", "bool", title: "Allow switch(es) to override automation?", defaultValue: false, required: false, submitOnChange: true
            if (override) {
                input "overrideSwitches", "capability.switch", title: "Which switch(es) will override the automation?", required: false, multiple: true
            }         
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// Set up the state variable that dictates whether the garage door
    // ought to open. Each presence device needs its own boolean because
    // each device has the potential to throw a false positive and the 
    // behavior of the garage door should be independent for each device.
	state.garageDoorShouldOpen = [:]
    presenceSensorList.each {
    	state.garageDoorShouldOpen[it.displayName] = [status: false, timestamp: null]
    }
    if (timeThreshold) {
    	def window = getWindow()
        if (window.end.get(Calendar.HOUR) < window.start.get(Calendar.HOUR)) state.crossesMidnight = true
        else state.crossesMidnight = false
    }
    subscribe(presenceSensorList, "presence", presenceHandler)
    log.debug "Subscribed to $presenceSensorList."
}

def presenceHandler(evt) {
	if (evt.value == "present") {
    	if (state.garageDoorShouldOpen[evt.displayName].status && (garageDoor.currentDoor == "closed" || garageDoor.currentDoor == "closing")) {
        	openDoor(evt.displayName)
    		state.garageDoorShouldOpen[evt.displayName].status = false // reset the status
        }
    } else {
    	state.garageDoorShouldOpen[evt.displayName].timestamp = Calendar.getInstance(location.timeZone, Locale.US).getTimeInMillis()
		log.debug "${state.garageDoorShouldOpen[evt.displayName]}"
        // evt.value == "not present" i.e. the presence sensor has "left"
    	if (garageDoor.currentDoor == "closed") {
        	log.debug "Catching a potential false positive."
            runIn(60*threshold, checkStatus)
        } else if (garageDoor.currentDoor != "unknown") {
        	log.debug "Garage door will open when ${evt.displayName} arrives."
        	state.garageDoorShouldOpen[evt.displayName].status = true
            if (garageDoor.currentDoor == "open") {
            	closeDoor(evt.displayName)
            }
        }
    }
            	
}

def checkStatus() {
	log.debug "Checking on false positive."
	presenceSensorList.each {
    	if (it.currentPresence == "not present" && !state.garageDoorShouldOpen[it.displayName].status) {
        	def now = Calendar.getInstance(location.timeZone, Locale.US)
            log.debug "$it.displayName ${state.garageDoorShouldOpen[it.displayName]}"
            def difference = now.getTimeInMillis() - state.garageDoorShouldOpen[it.displayName].timestamp
            if (difference >= threshold*60*1000) {
            	log.debug "Not a false positive; garage door will open when ${it.displayName} arrives."
            	state.garageDoorShouldOpen[it.displayName].status = true
            }
        }
    }        
}

def isUnhindered() {
	def dayOk = checkDay()
    def timeOk = checkTime()
    def modeOk = checkMode()
    log.debug "dayOk = $dayOk; timeOk = $timeOk; modeOk = $modeOk"
    return dayOk && timeOk && modeOk
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
        	def yesterdayStartWindow = window.start
            def tomorrowEndWindow = window.end
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

def openDoor(device) {
	if (isUnhindered() && !isOverridden()) {
    	log.info "Opening garage door due to arrival of $device."
    	garageDoor.open()
    }
}

def closeDoor(device) {
	if (isUnhindered() && !isOverridden()) {
    	log.info "Closing garage door due to departure of $device."
        garageDoor.close()
    }
}
