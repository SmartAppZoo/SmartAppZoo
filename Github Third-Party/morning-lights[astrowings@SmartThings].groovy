/**
 *  Morning Lights
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0												*/
 	       def urlApache() { return "http://www.apache.org/licenses/LICENSE-2.0" }				/*
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *   --------------------------------
 *   ***   VERSION HISTORY  ***
 *
 *    v2.21 (26-Nov-2019) - fix state.debugLevel by moving the reset to the start of initialization method
 *    v2.20 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v2.12 (22-Nov-2018) - wrap procedures to identify last execution and elapsed time
 *                        - check that lights were turned on by this app before turning off on mode change
 *                        - add appInfo section in app settings
 *	  v2.11 (09-Aug-2018) - standardize debug log types and make 'debug' logs disabled by default
 *						  - standardize layout of app data and constant definitions
 *    v2.10 (24-Nov-2016) - add option to specify sunrise time offset
 *						  - add option to turn lights off when leaving
 *						  - removed option to specify default turn-on time
 *    v2.00 (15-Nov-2016) - code improvement: store images on GitHub, use getAppImg() to display app images
 *                        - added option to disable icons
 *                        - added option to disable multi-level logging
 *                        - configured default values for app settings
 *						  - moved 'About' to its own page
 *						  - added link to readme file
 *						  - bug fix: removed log level increase for sunrise handler event because it was causing the log
 *							level to keep increasing without ever applying the '-1' at the end to restore the log level
 *						  - list current schedule/random settings in associated links
 *    v1.41 (04-Nov-2016) - update href state & images
 *	  v1.40 (03-Nov-2016) - new feature: add option to specify turn-off time
 *                        - code improvement: use constants instead of hard-coding
 *    v1.31 (02-Nov-2016) - add link for Apache license
 *    v1.30 (02-Nov-2016) - implement multi-level debug logging function
 *    v1.22 (01-Nov-2016) - code improvement: standardize pages layout
 *	  v1.21 (01-Nov-2016) - code improvement: standardize section headers
 *    v1.20 (29-Oct-2016) - inital version base code adapted from 'Sunset Lights - v1.20'
 *
*/
definition(
    name: "Morning Lights",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn on selected lights in the morning and turn them off automatically at sunrise.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@3x.png")


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 2.21" }
def		versionDate()			{ return "26-Nov-2019" }     
def		gitAppName()			{ return "morning-lights" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
def		C_MIN_TIME_ON()			{ return 10 }			//value to use when scheduling turnOn to make sure lights will remain on for at least this long (minutes) before the scheduled turn-off time


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSchedule")
    page(name: "pageRandom")
    page(name: "pageSettings")
    page(name: "pageLogOptions")
    page(name: "pageAbout")
    page(name: "pageUninstall")
}


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section(){
        	paragraph "", title: "This SmartApp turns on selected lights at a specified time and turns turns them off at sunrise " +
            	"if no turn-off time is set. Different turn-on times can be configured for each day of the week, and they can be " +
                "randomized within a specified window to simulate manual operation."
        }
        section() {
            input "theLights", "capability.switch", title: "Which lights?", description: "Choose the lights to turn on", multiple: true, required: true, submitOnChange: true
            if (theLights) {
            	def startTimeOk = weekdayOn || saturdayOn || sundayOn || defaultOn
                href "pageSchedule", title: "Set scheduling Options", description: schedOptionsDesc, image: getAppImg("office7-icn.png"), required: true, state: startTimeOk ? "complete" : null
                href "pageRandom", title: "Configure random scheduling", description: randomOptionsDesc, image: getAppImg("dice-xxl.png"), required: true, state: "complete"
        	}
        }
		section() {
			if (theLights) {
	            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            }
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
		}
    }
}

def pageSchedule() {
    dynamicPage(name: "pageSchedule", install: false, uninstall: false) {
        section(){
        	paragraph title: "Scheduling Options", "Use the options on this page to set the scheduling preferences."
        }
        section("Set a time to turn on the lights each day " +
                "(optional - lights will not turn on if no time is set for that day)") {
            input "weekdayOn", "time", title: "Mon-Fri", required: false
            input "saturdayOn", "time", title: "Saturday", required: false
            input "sundayOn", "time", title: "Sunday", required: false
        }
        //TODO: use illuminance-capable device instead of sunrise/sunset to detect darkness
        section("Turn the lights off at this time " +
                "(optional - if not set, lights will turn off at sunrise)") {
            input "timeOff", "time", title: "Time OFF?", required: false
        }
        section("Turn the lights off when everyone leaves (i.e. the mode changes to 'Away')") {
            input "awayOff", "bool", title: "Off on Away?", required: false
        }
	}
}

def pageRandom() {
    dynamicPage(name: "pageRandom", install: false, uninstall: false) {
        section(){
        	paragraph title: "Random Scheduling",
            	"Use the options on this page to add a random factor to " +
                "the lights' switching so the timing varies slightly " +
                "from one day to another (it looks more 'human' that way)."
        }
    	section("Specify a window around the scheduled time when the lights will turn on/off " +
        	"(e.g. a 30-minute window would have the lights switch sometime between " +
            "15 minutes before and 15 minutes after the scheduled time.)") {
            input "randOn", "number", title: "Random ON window (minutes)?", required: false, defaultValue: 8
            input "randOff", "number", title: "Random OFF window (minutes)?", required: false, defaultValue: 15
        }
        section("The settings above are used to randomize preset times such that lights will " +
        	"turn on/off at slightly different times from one day to another, but if multiples lights " +
            "are selected, they will still switch status at the same time. Use the options below " +
            "to insert a random delay between the switching of each individual light. " +
            "This option can be used independently of the ones above.") {
            input "onDelay", "bool", title: "Delay switch-on?", required: false, submitOnChange: true, defaultValue: true
            input "offDelay", "bool", title: "Delay switch-off?", required: false, submitOnChange: true, defaultValue: true
            if (onDelay || offDelay) {
            	input "delaySeconds", "number", title: "Switching delay", description: "Choose 1-60 seconds", required: true, defaultValue: 5, range: "1..60"
            }
        }
	}
}

def pageSettings() {
	dynamicPage(name: "pageSettings", install: false, uninstall: false) {
        section() {
			label title: "Assign a name", defaultValue: "${app.name}", required: false
            href "pageUninstall", title: "", description: "Uninstall this SmartApp", image: getAppImg("trash-circle-red-512.png"), state: null, required: true
		}
        if (!theLuminance) {
            section("If desired, you can adjust the amount of time before/after sunrise when the app will turn the lights off " +
                    "(e.g. use '-20' to adjust the sunrise time 20 minutes earlier than actual).") {
                input "sunriseOffset", "number", title: "Sunrise offset time", description: "How many minutes (+/- 60)?", range: "-60..60", required: false
            }
   		}
        section("Debugging Tools", hideable: true, hidden: true) {
            input "noAppIcons", "bool", title: "Disable App Icons", description: "Do not display icons in the configuration pages", image: getAppImg("disable_icon.png"), defaultValue: false, required: false, submitOnChange: true
            href "pageLogOptions", title: "IDE Logging Options", description: "Adjust how logs are displayed in the SmartThings IDE", image: getAppImg("office8-icn.png"), required: true, state: "complete"
            paragraph title: "Application info", appInfo()
        }
    }
}

def pageAbout() {
	dynamicPage(name: "pageAbout", title: "About this SmartApp", install: false, uninstall: false) { //with 'install: false', clicking 'Done' goes back to previous page
		section() {
        	href url: readmeLink(), title: app.name, description: "Copyright ©2016 Phil Maynard\n${versionNum()}", image: getAppImg("readme-icn.png")
            href url: urlApache(), title: "License", description: "View Apache license", image: getAppImg("license-icn.png")
		}
    }
}

def pageLogOptions() {
	dynamicPage(name: "pageLogOptions", title: "IDE Logging Options", install: false, uninstall: false) {
        section() {
	        input "debugging", "bool", title: "Enable debugging", description: "Display the logs in the IDE", defaultValue: true, required: false, submitOnChange: true
        }
        if (debugging) {
            section("Select log types to display") {
                input "log#info", "bool", title: "Log info messages", defaultValue: true, required: false
                input "log#trace", "bool", title: "Log trace messages", defaultValue: true, required: false
                input "log#debug", "bool", title: "Log debug messages", defaultValue: false, required: false
                input "log#warn", "bool", title: "Log warning messages", defaultValue: true, required: false
                input "log#error", "bool", title: "Log error messages", defaultValue: true, required: false
			}
            section() {
                input "setMultiLevelLog", "bool", title: "Enable Multi-level Logging", defaultValue: true, required: false,
                    description: "Multi-level logging prefixes log entries with special characters to visually " +
                        "represent the hierarchy of events and facilitate the interpretation of logs in the IDE"
            }
            section() {
                input "maxInfoLogs", "number", title: "Display Log Entries", defaultValue: 5, required: false, range: "0..50"
                    description: "Select the maximum number of most recent log entries to display in the " +
                        "application's 'Debugging Tools' section. Enter '0' to disable."
            }
        }
    }
}

def pageUninstall() {
	dynamicPage(name: "pageUninstall", title: "Uninstall", install: false, uninstall: true) {
		section() {
        	paragraph "CAUTION: You are about to completely remove the SmartApp '${app.name}'. This action is irreversible. If you want to proceed, tap on the 'Remove' button below.",
                required: true, state: null
        }
	}
}


//   ---------------------------------
//   ***   PAGES SUPPORT METHODS   ***

def getSchedOptionsDesc() {
    def strWeekdayOn = weekdayOn?.substring(11,16)
    def strSaturdayOn = saturdayOn?.substring(11,16)
    def strSundayOn = sundayOn?.substring(11,16)
    def strDefaultOn = defaultOn?.substring(11,16)
    def startTimeOk = weekdayOn || saturdayOn || sundayOn || defaultOn
    def strTimeOff = timeOff?.substring(11,16)
    def strDesc = ""
    strDesc += 				  " • Turn-on time:"
    strDesc += defaultOn	? " ${strDefaultOn}\n" : "\n"
    strDesc += weekdayOn	? "   └ weekdays: ${strWeekdayOn}\n" : ""
    strDesc += saturdayOn	? "   └ saturday: ${strSaturdayOn}\n" : ""
    strDesc += sundayOn		? "   └ sunday: ${strSundayOn}\n" : ""
    strDesc += timeOff		? " • Turn-off time: ${strTimeOff}\n" : " • Turn off at sunrise\n"
    strDesc += awayOff		? " • Turn off when leaving" : ""
    return startTimeOk ? strDesc : "Turn-on time not set; automation disabled."
}

def getRandomOptionsDesc() {
    def delayType = (onDelay && offDelay) ? "on & off" : (onDelay ? "on" : "off")
    def randOn  =  settings.randOn  ? (double) settings.randOn  : null
    def randOff =  settings.randOff ? (double) settings.randOff : null
    double randOnWindow  = randOn  ? (randOn/2).round(1)  : 0
    double randOffWindow = randOff ? (randOff/2).round(1) : 0
    def strDesc = ""
    strDesc += (randOn || randOff)	? " • Random window:\n" : ""
    strDesc += randOn				? "   └ turn on:  +/-${randOnWindow} minutes\n" : ""
    strDesc += randOff				? "   └ turn off: +/-${randOffWindow} minutes\n" : ""
    strDesc += delaySeconds			? " • Light-light delay: ${delaySeconds} seconds\n    (when switching ${delayType})" : ""
    return (randOn || randOff || delaySeconds) ? strDesc : "Tap to configure random settings..."
}

def appInfo() {
	def tz = location.timeZone
    def mapSun = getSunriseAndSunset()
    def debugLevel = state.debugLevel
    def lightsOn = state.lightsOn
    def lightsOnTime = state.lightsOnTime
    def datInstall = state.installTime ? new Date(state.installTime) : null
    def datInitialize = state.initializeTime ? new Date(state.initializeTime) : null
    def datSchedOn = state.schedOnTime ? new Date(state.schedOnTime) : null
    def datSchedOff = state.schedOffTime ? new Date(state.schedOffTime) : null
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += datInstall ? "  └ last install date: ${datInstall.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datInitialize ? "  └ last initialize date: ${datInitialize.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "\n • Last scheduled jobs:\n"
        strInfo += datSchedOn ? "  └ turnOn: ${datSchedOn.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datSchedOff ? "  └ turnOff: ${datSchedOff.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "\n • Last initiated execution:\n"
		strInfo += "  └ name: ${lastInitiatedExecution.name}\n"
        strInfo += "  └ time: ${new Date(lastInitiatedExecution.time).format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "\n • Last completed execution:\n"
		strInfo += "  └ name: ${lastCompletedExecution.name}\n"
        strInfo += "  └ time: ${new Date(lastCompletedExecution.time).format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "  └ time to complete: ${lastCompletedExecution.duration}s\n"
		strInfo += "\n • Environment:\n"
        strInfo += "  └ sunrise: ${mapSun.sunrise.format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "  └ sunset: ${mapSun.sunset.format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "\n • State stored values:\n"
        strInfo += "  └ debugLevel: ${debugLevel}\n"
        strInfo += "  └ lightsOn: ${lightsOn}"
        strInfo += (lightsOn && lightsOnTime) ? " (since ${new Date(lightsOnTime).format('HH:mm', tz)})\n" : "\n"
        strInfo += "  └ number of stored log entries: ${numLogs}\n"
        if (numLogs > 0) {
            strInfo += "\n • Last ${numLogs} log messages (most recent on top):\n"
            for (int i = 0; i < numLogs; i++) {
                def datLog = new Date(debugLog[i].time).format('dd MMM HH:mm:ss', tz)
                def msgLog = "${datLog} (${debugLog[i].type}):\n${debugLog[i].msg}"
                strInfo += " ::: ${msgLog}\n"
            }
        }
    return strInfo
}

//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	debug "installed with settings: ${settings}", "trace", 0
	state.installTime = now()
	initialize()
}

def updated() {
    debug "updated with settings ${settings}", "trace", 0
	unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    state.debugLevel = 0
    debug "application uninstalled", "trace", 0
}

def initialize() {
    state.debugLevel = 0
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "initialize()"]
    debug "initializing", "trace", 1
    state.initializeTime = now()
    state.lightsOn = false
    subscribeToEvents()
	schedTurnOff(location.currentValue("sunriseTime"))
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(location, "sunriseTime", sunriseTimeHandler)	//triggers at sunrise, evt.value is the sunrise String (time for next day's sunrise)
    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def sunriseTimeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sunriseTimeHandler()"]
    debug "sunriseTimeHandler event: ${evt.descriptionText}", "trace", 1
    def sunriseTimeHandlerMsg = "triggered sunriseTimeHandler; next sunrise will be ${evt.value}"
    debug "sunriseTimeHandlerMsg : $sunriseTimeHandlerMsg"
    schedTurnOff(evt.value)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sunriseTimeHandler()", duration: elapsed]
    debug "sunriseTimeHandler completed in ${elapsed} seconds", "trace", -1
}    

def locationPositionChange(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "locationPositionChange()"]
    debug "locationPositionChange(${evt.descriptionText})", "warn"
	initialize()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "locationPositionChange()", duration: elapsed]
}

def modeChangeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "modeChangeHandler()"]
	debug "modeChangeHandler event: ${evt.descriptionText}", "trace", 1
    if(state.lightsOn && awayOff && evt.value == "Away") {
        debug "mode changed to ${evt.value}; calling turnOff()", "info"
        turnOff()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "modeChangeHandler()", duration: elapsed]
    debug "modeChangeHandler completed in ${elapsed} seconds", "trace", -1
}

//   -------------------
//   ***   METHODS   ***

def schedTurnOff(sunriseString) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "schedTurnOff()"]
    debug "executing schedTurnOff(sunriseString: ${sunriseString})", "trace", 1
	
    //convert sunriseString into date
    def datTurnOff = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    if (sunriseOffset) {
    	int offset = sunriseOffset * 60000
        def offsetSunriseTime = datTurnOff.time + offset
        datTurnOff = new Date(offsetSunriseTime)
        debug "sunrise date (with ${sunriseOffset} offset) : ${datTurnOff}"
    } else {
    	debug "sunrise date : ${datTurnOff}"
    }

    if (timeOff) {
    	def userOff = timeTodayAfter("12:00", timeOff, location.timeZone)
        datTurnOff = datTurnOff < userOff ? datTurnOff : userOff //select the earliest of the two turn-off times
    }
    
    //apply random factor
    if (randOff) {
        def random = new Random()
        def randOffset = random.nextInt(randOff)
        datTurnOff = new Date(datTurnOff.time - (randOff * 30000) + (randOffset * 60000))
	}
    state.schedOffTime = datTurnOff.time
    
    // This method gets called at sunrise to schedule next day's turn-off. However it's possible that
    // today's turn-off could be scheduled after sunrise (therefore after this method gets called),
    // so we use [overwrite: false] to prevent today's scheduled turn-off from being overwriten.
	debug "scheduling lights OFF for: ${datTurnOff}", "info"
    runOnce(datTurnOff, turnOff, [overwrite: false])
    schedTurnOn(datTurnOff)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "schedTurnOff()", duration: elapsed]
    debug "schedTurnOff() completed in ${elapsed} seconds", "trace", -1
}

def schedTurnOn(datTurnOff) {
	//fires at sunrise to schedule next day's turn-on
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "schedTurnOn()"]
    debug "executing schedTurnOn(datTurnOff: ${datTurnOff})", "trace", 1
    
    def datTurnOn = DOWTurnOnTime
    
	if (datTurnOn) {
        //check that turn-on is scheduled earlier than turn-off by at least 'minTimeOn' minutes
	    def minTimeOn = C_MIN_TIME_ON()
        def safeOff = datTurnOff.time - (minTimeOn * 60 * 1000) //subtract 'minTimeOn' from scheduled turn-off time to ensure lights will stay on for at least 'minTimeOn' minutes
        if (datTurnOn.time < safeOff) {
            debug "scheduling lights ON for: ${datTurnOn} (${useTime})", "info"
            state.schedOnTime = datTurnOn.time
            runOnce(datTurnOn, turnOn)
        } else {
        	debug "scheduling cancelled because tomorrow's turn-on time (${datTurnOn}) " +
            	"would be later than (or less than ${minTimeOn} minutes before) " +
                "the scheduled turn-off time (${datTurnOff}).", "info"
        }
    } else {
    	debug "user didn't specify turn-on time; scheduling cancelled", "info"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "schedTurnOn()", duration: elapsed]
    debug "schedTurnOn() completed in ${elapsed} seconds", "trace", -1
}

def turnOn() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "turnOn()"]
    debug "executing turnOn()", "trace", 1
    def newDelay = 0L
    def delayMS = (onDelay && delaySeconds) ? delaySeconds * 1000 : 5 //ensure delayMS != 0
    def random = new Random()
    theLights.each { theLight ->
        if (theLight.currentSwitch != "on") {
            debug "turning on the ${theLight.label} in ${convertToHMS(newDelay)}", "info"
            theLight.on(delay: newDelay)
            newDelay += random.nextInt(delayMS) //calculate random delay before turning on next light
        } else {
            debug "the ${theLight.label} is already on; doing nothing"
        }
    }
	state.lightsOn = true
    state.lightsOnTime = now()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "turnOn()", duration: elapsed]
    debug "turnOn() completed in ${elapsed} seconds", "trace", -1
}

def turnOff() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "turnOff()"]
    debug "executing turnOff()", "trace", 1
    def newDelay = 0L
    def delayMS = (offDelay && delaySeconds) ? delaySeconds * 1000 : 5 //ensure delayMS != 0
    def random = new Random()
    theLights.each { theLight ->
        if (theLight.currentSwitch != "off") {
            debug "turning off the ${theLight.label} in ${convertToHMS(newDelay)}", "info"
            theLight.off(delay: newDelay)
            newDelay += random.nextInt(delayMS) //calculate random delay before turning off next light
        } else {
            debug "the ${theLight.label} is already off; doing nothing"
        }
    }
    state.lightsOn = false
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "turnOff()", duration: elapsed]
    debug "turnOff() completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getDOWTurnOnTime() {
    //calculate weekday-specific turn-on time
    //this gets called at sunrise, so when the sun rises on Tuesday, it will
    //schedule the lights' turn-on time for Wednesday morning
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getDOWTurnOnTime()"]
	debug "start evaluating DOWTurnOnTime", "trace", 1

    def tmrDOW = (new Date() + 1).format("E") //find out tomorrow's day of week

    //find out the preset (if entered) turn-on time for tomorrow
    def DOWtimeOn
    if (saturdayOn && tmrDOW == "Sat") {
        DOWtimeOn = saturdayOn
    } else if (sundayOn && tmrDOW == "Sun") {
        DOWtimeOn = sundayOn
    } else if (weekdayOn && tmrDOW == "Mon") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Tue") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Wed") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Thu") {
        DOWtimeOn = weekdayOn
    } else if (weekdayOn && tmrDOW == "Fri") {
        DOWtimeOn = weekdayOn
    }

	if (DOWtimeOn) {
    	//convert preset time to tomorrow's date
    	def tmrOn = timeTodayAfter("12:00", DOWtimeOn, location.timeZone)
        
        //apply random factor to turn-on time
		if (randOn) {
            def random = new Random()
            def randOffset = random.nextInt(randOn)
            tmrOn = new Date(tmrOn.time - (randOn * 30000) + (randOffset * 60000))
            debug "randomized DOW turn-on time: $tmrOn"
        } else {
        	debug "DOW turn-on time: $tmrOn"
        }
        debug "finished evaluating DOWTurnOnTime", "trace", -1
        def elapsed = (now() - startTime)/1000
    	state.lastCompletedExecution = [time: now(), name: "getDOWTurnOnTime()", duration: elapsed]
		return tmrOn
    } else {
    	debug "DOW turn-on time not specified for ${tmrDOW}"
        debug "finished evaluating DOWTurnOnTime", "trace", -1
        def elapsed = (now() - startTime)/1000
    	state.lastCompletedExecution = [time: now(), name: "getDOWTurnOnTime()", duration: elapsed]
		return false
    }
}

//   ------------------------
//   ***   COMMON UTILS   ***

def convertToHMS(ms) {
    int hours = Math.floor(ms/1000/60/60)
    int minutes = Math.floor((ms/1000/60) - (hours * 60))
    int seconds = Math.floor((ms/1000) - (hours * 60 * 60) - (minutes * 60))
    double millisec = ms-(hours*60*60*1000)-(minutes*60*1000)-(seconds*1000)
    int tenths = (millisec/100).round(0)
    return "${hours}h${minutes}m${seconds}.${tenths}s"
}

def getAppImg(imgName, forceIcon = null) {
	def imgPath = appImgPath()
    return (!noAppIcons || forceIcon) ? "$imgPath/$imgName" : ""
}

def debug(message, lvl = null, shift = null, err = null) {
	
    def debugging = settings.debugging
	if (!debugging) {
		return
	}
    
    lvl = lvl ?: "debug"
	if (!settings["log#$lvl"]) {
		return
	}
	
    def multiEnable = (settings.setMultiLevelLog == false ? false : true) //set to true by default
    def maxLevel = 4
	def level = state.debugLevel ?: 0
	def levelDelta = 0
	def prefix = "║"
	def pad = "░"
	
    //shift is:
	//	 0 - initialize level, level set to 1
	//	 1 - start of routine, level up
	//	-1 - end of routine, level down
	//	 anything else - nothing happens
	
    switch (shift) {
		case 0:
			level = 0
			prefix = ""
			break
		case 1:
			level += 1
			prefix = "╚"
			pad = "═"
			break
		case -1:
			levelDelta = -(level > 0 ? 1 : 0)
			pad = "═"
			prefix = "╔"
			break
	}

	if (level > 0) {
		prefix = prefix.padLeft(level, "║").padRight(maxLevel, pad)
	}

	level += levelDelta
	state.debugLevel = level

	if (multiEnable) {
		prefix += " "
	} else {
		prefix = ""
	}

    def logMsg = null
    if (lvl == "info") {
    	def leftPad = (multiEnable ? ": :" : "")
        log.info "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "trace") {
    	def leftPad = (multiEnable ? "::" : "")
        log.trace "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "warn") {
    	def leftPad = (multiEnable ? "::" : "")
		log.warn "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else if (lvl == "error") {
    	def leftPad = (multiEnable ? "::" : "")
		log.error "$leftPad$prefix$message", err
        logMsg = "${message}"
	} else {
		log.debug "$prefix$message", err
        logMsg = "${message}"
	}
    
    if (logMsg) {
    	def debugLog = state.debugLogInfo ?: [] //create list if it doesn't already exist
        debugLog.add(0,[time: now(), msg: logMsg, type: lvl]) //insert log info into list slot 0, shifting other entries to the right
        def maxLogs = settings.maxInfoLogs ?: 5
        def listSize = debugLog.size()
        while (listSize > maxLogs) { //delete old entries to prevent list from growing beyond set size
            debugLog.remove(maxLogs)
            listSize = debugLog.size()
        }
    	state.debugLogInfo = debugLog
    }
}