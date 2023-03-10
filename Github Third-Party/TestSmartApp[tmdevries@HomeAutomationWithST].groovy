definition(
        name: "Test SmartApp",
        namespace: "tmdevries",
        author: "Tara De Vries",
        description: "Simple test SmartApp for testing optional conditions common to other SmartApps.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page name: "setupPage"
    page name: "optionsPage"
}

def setupPage() {
    dynamicPage(name: "setupPage", title: "Automation Setup", uninstall: true, nextPage: "optionsPage") {
        section("Turn on this light") {
            input "aSwitch", "capability.switch", title: "Select 1 light", required: true
        }
        section("Turn on when open") {
            input "aContact", "capability.contactSensor", title: "Select contact sensor", required: true
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
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	if (timeThreshold) {
    	def window = getWindow()
        if (window.end.get(Calendar.HOUR_OF_DAY) < window.start.get(Calendar.HOUR_OF_DAY)) state.crossesMidnight = true
        else state.crossesMidnight = false
    }
    subscribe(aContact, "contact.open", contactHandler)
}

def contactHandler(evt) {
    if (isUnhindered() && !isOverridden()) {
    	log.debug "Everything ok, turing $aSwitch on"
    	aSwitch.on()
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
