/**
 *  Sunset Lights
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
 *    v2.20 (22-Nov-2019) - add option to turn lights off when mode changes to Night
 *							(to be able to make the lights turn off with a random delay when the mode changes
 *							to Night instead of turning them all off simultaneously with the Good Night! routine)
 *						  - list scheduled tasks in debug section
 *    v2.10 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v2.04 (23-Nov-2018) - wrap procedures to identify last execution and elapsed time
 *	  v2.03 (09-Aug-2018) - standardize debug log types and make 'debug' logs disabled by default
 *						  - standardize layout of app data and constant definitions
 *						  - use 'return result' at end of method instead 'return <variable>' thru method to avoid skipping the debug -1 level
 *    v2.02 (03-Aug-2018) - use sunrise offset constant in calculation of turn-off time
 *                        - move random time calculation from weekdayTurnOffTime/defaultTurnOffTime to scheduleTurnOff
 *    v2.01 (30-Jul-2018) - display app info as section label instead of para
 *    v2.00 (15-Nov-2016) - code improvement: store images on GitHub, use getAppImg() to display app images
 *                        - added option to disable icons
 *                        - added option to disable multi-level logging
 *                        - configured default values for app settings
 *						  - moved 'About' to its own page
 *						  - added link to readme file
 *						  - bug fix: removed log level increase for sunset/sunrise handler events because it was causing the log
 *							level to keep increasing without ever applying the '-1' at the end to restore the log level
 *						  - list current schedule/random settings in associated links
 *    v1.33 (04-Nov-2016) - update href state & images
 *    v1.32 (03-Nov-2016) - code improvement: use constants instead of hard-coding
 *    v1.31 (02-Nov-2016) - add link for Apache license
 *    v1.30 (02-Nov-2016) - implement multi-level debug logging function
 *    v1.22 (01-Nov-2016) - code improvement: standardize pages layout
 *	  v1.21 (01-Nov-2016) - code improvement: standardize section headers
 *	  v1.20 (28-Oct-2016) - new feature: add option to insert random delay between the switching of individual lights,
 *                        - code improvement: change method to evaluate which turn-off time to use
 *                        - code improvement: move off-time comparison to turnOn()
 *                        - new feature: add option to apply random factor to ON time
 *    v1.11 (26-Oct-2016) - code improvement: added trace for each event handler
 *    v1.10 (26-Oct-2016) - added 'About' section in preferences
 *    v1.00               - initial release, no version tracking up to this point
 *
*/
definition(
    name: "Sunset Lights",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn on selected lights at sunset and turn them off at a specified time.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light25-icn@3x.png")


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 2.21" }
def		versionDate()			{ return "26-Nov-2019" }     
def		gitAppName()			{ return "sunset-lights" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
def		C_SUNRISE_OFFSET()		{ return -30 }			//offset used for sunrise time calculation (minutes)
def		C_MIN_TIME_ON()			{ return 15 }			//value to use when scheduling turnOn to make sure lights will remain on for at least this long (minutes) before the scheduled turn-off time


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
    	section("This SmartApp turns on selected lights at sunset and turns them off at a specified time." +
            	"Different turn-off times can be configured for each day of the week, and they can be " +
                "randomized within a specified window to simulate manual operation."){
        }
        section() {
            input "theLights", "capability.switch", title: "Which lights?", description: "Choose the lights to turn on", multiple: true, required: true, submitOnChange: true
            if (theLights) {
                href "pageSchedule", title: "Set scheduling Options", description: schedOptionsDesc, image: getAppImg("office7-icn.png"), required: true, state: "complete"
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
        def sunriseOffset = C_SUNRISE_OFFSET()
        def sunriseOffset_minutes = sunriseOffset.abs()
        def sunriseOffset_BeforeAfter = sunriseOffset < 0 ? "before" : "after"
        section(){
        	paragraph title: "Scheduling Options", "Use the options on this page to set the scheduling preferences."
        }
        //TODO: use illuminance-capable device instead of sunrise/sunset to detect darkness
        section("Set the amount of time before/after sunset when the lights will turn on " +
        		"(e.g. use '-20' to enable lights 20 minutes before sunset).") {
                input "sunsetOffset", "number", title: "Sunset offset time", description: "How many minutes (+/- 60)?", range: "-60..60", required: false
        }
    	section("Turn the lights off at this time " +
        		"(optional - lights will turn off ${sunriseOffset_minutes} minutes ${sunriseOffset_BeforeAfter} next sunrise if no time is entered)") {
        	input "timeOff", "time", title: "Time to turn lights off?", required: false, defaultValue: "22:00"
        }
    	section("Set a different time to turn off the lights on each day (optional - lights will turn off at the default time if not set)") {
        	input "sundayOff", "time", title: "Sunday", required: false
            input "mondayOff", "time", title: "Monday", required: false
            input "tuesdayOff", "time", title: "Tuesday", required: false
            input "wednesdayOff", "time", title: "Wednesday", required: false
            input "thursdayOff", "time", title: "Thursday", required: false
            input "fridayOff", "time", title: "Friday", required: false
            input "saturdayOff", "time", title: "Saturday", required: false
        }
        section("Turn lights off when mode changes to Night (overides preset turn-off times above)") {
            input "offAtNight", "bool", title: "Yes/No?", required: false, defaultValue: true
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
            input "onRand", "number", title: "Random ON window (minutes)?", required: false, defaultValue: 8
            input "offRand", "number", title: "Random OFF window (minutes)?", required: false, defaultValue: 25
        }
        section("The settings above are used to randomize preset times such that lights will " +
        	"turn on/off at slightly different times from one day to another, but if multiples lights " +
            "are selected, they will still switch status at the same time. Use the options below " +
            "to insert a random delay between the switching of each individual light. " +
            "This option can be used independently of the ones above.") {
            input "onDelay", "bool", title: "Delay switch-on?", required: false, submitOnChange: true, defaultValue: true
            input "offDelay", "bool", title: "Delay switch-off?", required: false, submitOnChange: true, defaultValue: true
            if (onDelay || offDelay) {
            	input "delaySeconds", "number", title: "Switching delay up to?", description: "choose 1-60 seconds random delay", required: true, defaultValue: 5, range: "1..60"
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
    def sunriseOffset = C_SUNRISE_OFFSET()
    def sunriseOffset_minutes = sunriseOffset.abs()
    def sunriseOffset_BeforeAfter = sunriseOffset < 0 ? "before" : "after"
    def strDefaultOff = timeOff?.substring(11,16)
    def strSundayOff = sundayOff?.substring(11,16)
    def strMondayOff = mondayOff?.substring(11,16)
    def strTuesdayOff = tuesdayOff?.substring(11,16)
    def strWednesdayOff = wednesdayOff?.substring(11,16)
    def strThursdayOff = thursdayOff?.substring(11,16)
    def strFridayOff = fridayOff?.substring(11,16)
    def strSaturdayOff = saturdayOff?.substring(11,16)
    def offTimeOk = timeOff || sundayOff || mondayOff || tuesdayOff || wednesdayOff || thursdayOff || fridayOff || saturdayOff
    def strDesc = ""
    strDesc += sunsetOffset ? " • Sunset offset: ${sunsetOffset} minutes\n" : ""
    strDesc += offTimeOk ? " • Turn-off time:" : ""
    strDesc += timeOff	? " ${strDefaultOff}\n" : "\n"
    strDesc += sundayOff	? "   └ sunday: ${strSundayOff}\n" : ""
    strDesc += mondayOff	? "   └ monday: ${strMondayOff}\n" : ""
    strDesc += tuesdayOff		? "   └ tuesday: ${strTuesdayOff}\n" : ""
    strDesc += wednesdayOff		? "   └ wednesday: ${strWednesdayOff}\n" : ""
    strDesc += thursdayOff		? "   └ thursday: ${strThursdayOff}\n" : ""
    strDesc += fridayOff		? "   └ friday: ${strFridayOff}\n" : ""
    strDesc += saturdayOff		? "   └ saturday: ${strSaturdayOff}" : ""
    return (sunsetOffset || offTimeOk) ? strDesc : "Schedule not set; lights will come on at sunset and turn off ${sunriseOffset_minutes} minutes ${sunriseOffset_BeforeAfter} next sunrise."
}

def getRandomOptionsDesc() {
    def delayType = (onDelay && offDelay) ? "on & off" : (onDelay ? "on" : "off")
    def strDesc = ""
    strDesc += (onRand || offRand)	? " • Random window:\n" : ""
    strDesc += onRand				? "   └ turn on:  +/-${onRand/2} minutes\n" : ""
    strDesc += offRand				? "   └ turn off: +/-${offRand/2} minutes\n" : ""
    strDesc += delaySeconds			? " • Light-light delay: ${delaySeconds} seconds\n    (when switching ${delayType})" : ""
    return (onRand || offRand || delaySeconds) ? strDesc : "Tap to configure random settings..."
}

def appInfo() {
	def tz = location.timeZone
    def mapSun = getSunriseAndSunset()
    def debugLevel = state.debugLevel
    def lightsOn = state.lightsOn
    def lightsOnTime = state.lightsOnTime
    def installTime = state?.installTime
    def initializeTime = state?.initializeTime
    def schedOnTime = state?.schedOnTime
    def schedOffTime = state?.schedOffTime
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += installTime ? "  └ last install date: ${new Date(installTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += initializeTime ? "  └ last initialize date: ${new Date(initializeTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "\n • Last scheduled jobs:\n"
        strInfo += schedOnTime ? "  └ turnOn: ${new Date(schedOnTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += schedOffTime ? "  └ turnOff: ${new Date(schedOffTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
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
	scheduleTurnOn(location.currentValue("sunsetTime"))
    scheduleTurnOff(location.currentValue("sunriseTime"))
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "sunsetTime", sunsetTimeHandler)	//triggers at sunset, evt.value is the sunset String (time for next day's sunset)
    subscribe(location, "sunriseTime", sunriseTimeHandler)	//triggers at sunrise, evt.value is the sunrise String (time for next day's sunrise)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def modeChangeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "modeChangeHandler()"]
    debug "modeChangeHandler event: ${evt.descriptionText}", "trace", 1
    if(offAtNight && evt.value == "Night") {
        debug "mode changed to ${evt.value}; calling to turn lights off", "info"
        turnOff()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "modeChangeHandler()", duration: elapsed]
    debug "modeChangeHandler completed in ${elapsed} seconds", "trace", -1
}

def sunsetTimeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sunsetTimeHandler()"]
    debug "sunsetTimeHandler event: ${evt.descriptionText}", "trace", 1
    debug "next sunset will be ${evt.value}"
	scheduleTurnOn(evt.value)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sunsetTimeHandler()", duration: elapsed]
    debug "sunsetTimeHandler completed in ${elapsed} seconds", "trace", -1
}

def sunriseTimeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sunriseTimeHandler()"]
    debug "sunriseTimeHandler event: ${evt.descriptionText}", "trace", 1
    debug "next sunrise will be ${evt.value}"
    scheduleTurnOff(evt.value)
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


//   -------------------
//   ***   METHODS   ***

def scheduleTurnOn(sunsetString) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "scheduleTurnOn()"]
    debug "executing scheduleTurnOn(sunsetString: ${sunsetString})", "trace", 1
	
    def datSunset = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    debug "sunset date: ${datSunset}"
    //calculate the offset
    def offsetTurnOn = sunsetOffset ? sunsetOffset * 60 * 1000 : 0 //convert offset to ms
	def datTurnOn = new Date(datSunset.time + offsetTurnOn)

    //apply random factor
    if (onRand) {
        debug "Applying random factor to the turn-on time"
        def random = new Random()
        def randOffset = random.nextInt(onRand)
        datTurnOn = new Date(datTurnOn.time - (onRand * 30000) + (randOffset * 60000)) //subtract half the random window (converted to ms) then add the random factor (converted to ms)
	}
    
	debug "scheduling lights ON for: ${datTurnOn}", "info"
    runOnce(datTurnOn, turnOn, [overwrite: false]) //schedule this to run once (it will trigger again at next sunset)
    state.schedOnTime = datTurnOn.time
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "scheduleTurnOn()", duration: elapsed]
    debug "scheduleTurnOn() completed in ${elapsed} seconds", "trace", -1
}

def scheduleTurnOff(sunriseString) {
//schedule next day's turn-off
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "scheduleTurnOff()"]
    debug "executing scheduleTurnOff(sunriseString: ${sunriseString})", "trace", 1
    def datOffDOW = weekdayTurnOffTime
    def datOffDefault = defaultTurnOffTime
    def datOff

    //select which turn-off time to use (1st priority: weekday-specific, 2nd: default, 3rd: sunrise)
    if (datOffDOW) {
    	debug "using the weekday turn-off time"
        datOff = datOffDOW
    } else if (datOffDefault) {
    	debug "using the default turn-off time"
    	datOff = datOffDefault
    } else {
    	debug "user didn't specify turn-off time; using sunrise time"
        def datSunrise = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
        //calculate the offset
        def sunriseOffset = C_SUNRISE_OFFSET()
		def sunriseOffsetMS = sunriseOffset ? sunriseOffset * 60 * 1000 : 0 //convert offset to ms
        datOff = new Date(datSunrise.time + sunriseOffsetMS)
    }
    
    //apply random factor
    if (offRand) {
        debug "Applying random factor to the turn-off time"
        def random = new Random()
        def randOffset = random.nextInt(offRand)
        datOff = new Date(datOff.time - (offRand * 30000) + (randOffset * 60000)) //subtract half the random window (converted to ms) then add the random factor (converted to ms)
	}
    
    state.schedOffTime = datOff.time //store the scheduled OFF time in State so we can use it later to compare it to the ON time
	debug "scheduling lights OFF for: ${datOff}", "info"
    runOnce(datOff, turnOff, [overwrite: false]) //schedule this to run once (it will trigger again at next sunrise)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "scheduleTurnOff()", duration: elapsed]
    debug "scheduleTurnOff() completed in ${elapsed} seconds", "trace", -1
}

def turnOn() {
    //check that the scheduled turn-off time is in the future (for example, if the lights are
    //scheduled to turn on at 20:23 based on the sunset time, but the user had them set to turn
    //off at 20:00, the turn-off will fire before the lights are turned on. In that case, the
    //lights would still turn on at 20:23, but they wouldn't turn off until the next day at 20:00.
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "turnOn()"]
    debug "executing turnOn()", "trace", 1
	
    def minTimeOn = C_MIN_TIME_ON()
    def nowTime = now() + (minTimeOn * 60 * 1000) //making sure lights will stay on for at least 'minTimeOn'
    def offTime = state.schedOffTime //retrieving the turn-off time from State
    if (offTime < nowTime) {
		debug "scheduled turn-off time has already passed; turn-on cancelled", "debug"
    } else {
        debug "turning lights on"
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
        state.lightsOn = true
        state.lightsOnTime = now()
        }
    }
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

def getDefaultTurnOffTime() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getDefaultTurnOffTime()"]
	debug "start evaluating defaultTurnOffTime", "trace", 1
    def result
    if (timeOff) {
        def offDate = timeTodayAfter(new Date(), timeOff, location.timeZone) //convert preset time to today's date
       	debug "default turn-off time: $offDate"
        result = offDate
    } else {
        debug "default turn-off time not specified"
        result = false
	}
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "getDefaultTurnOffTime()", duration: elapsed]
    debug "finished evaluating defaultTurnOffTime in ${elapsed} seconds", "trace", -1
    return result
}

def getWeekdayTurnOffTime() {
    //calculate weekday-specific offtime
    //this executes at sunrise (called from scheduleTurnOff),
    //so when the sun rises on Tuesday, it will
    //schedule the lights' turn-off time for Tuesday night
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getWeekdayTurnOffTime()"]
	debug "start evaluating weekdayTurnOffTime", "trace", 1
	def result
	def nowDOW = new Date().format("E") //find out current day of week

    //find out the preset (if entered) turn-off time for the current weekday
    def offDOWtime
    if (sundayOff && nowDOW == "Sun") {
        offDOWtime = sundayOff
    } else if (mondayOff && nowDOW == "Mon") {
        offDOWtime = mondayOff
    } else if (tuesdayOff && nowDOW == "Tue") {
        offDOWtime = tuesdayOff
    } else if (wednesdayOff && nowDOW == "Wed") {
        offDOWtime = wednesdayOff
    } else if (thursdayOff && nowDOW == "Thu") {
        offDOWtime = thursdayOff
    } else if (fridayOff && nowDOW == "Fri") {
        offDOWtime = fridayOff
    } else if (saturdayOff && nowDOW == "Sat") {
        offDOWtime = saturdayOff
    }

	if (offDOWtime) {
    	def offDOWdate = timeTodayAfter(new Date(), offDOWtime, location.timeZone)
       	debug "DOW turn-off time: $offDOWdate"
        result =  offDOWdate
    } else {
    	debug "DOW turn-off time not specified"
        result = false
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "getWeekdayTurnOffTime()", duration: elapsed]
    debug "finished evaluating weekdayTurnOffTime in ${elapsed} seconds", "trace", -1
    return result
}


//   ------------------------
//   ***   COMMON UTILS   ***

int randomTime(int baseDate, int rangeMinutes) {
   int min = baseDate.time - (rangeMinutes * 30000)
   int range = (rangeMinutes * 60000) + 1
   return new Date((int)(Math.random() * range) + min)
}

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