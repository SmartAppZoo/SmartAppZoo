/**
 *  Bed Lamp
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
 *    v1.31 (26-Nov-2019) - fix state.debugLevel by moving the reset to the start of initialization method
 *    v1.30 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v1.20 (27-May-2019) - add event handler based on presence to activate the scheduling
 *                          when someone in thePeople arrives after the mode was set to Home
 *                          by someone not in thePeople, in which case the scheduling would
 *                          not activate
 *    v1.12 (23-Nov-2018) - wrap procedures to identify last execution and elapsed time
 *                        - add appInfo section in app settings
 *	  v1.11 (09-Aug-2018) - standardize debug log types and make 'debug' logs disabled by default
 *						  - standardize layout of app data and constant definitions
 *    v1.10 (24-Feb-2018) - added option to turn lights on in the morning
 *    v1.02 (26-Mar-2017) - removed unused reference to pageSchedule from preferences section
 *    v1.01 (01-Jan-2017) - added call to timeCheck() during initialization
 *                        - moved 'thePeople' input to pageSettings
 *    v1.00 (31-Dec-2016) - initial release
 *    v0.10 (27-Nov-2016) - developing
 *
*/
definition(
    name: "Bed Lamp",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Automatically turn on selected lights after dark and turn them off when the mode changes to Night.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light2-icn@3x.png"
)


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 1.31" }
def		versionDate()			{ return "26-Nov-2019" }     
def		gitAppName()			{ return "bed-lamp" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSchedule")
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
        	paragraph "", title: "This SmartApp turns on selected lights after dark (or at a preset time) and turns them off when the mode changes to Night."
        }
        section() {
            input "theLights", "capability.switch", title: "Which lights?", description: "Choose the lights to turn on", multiple: true, required: true, submitOnChange: true
        }
		section() {
			if (theLights) {
	            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            }
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
		}
    }
}

def pageSettings() {
	dynamicPage(name: "pageSettings", install: false, uninstall: false) {
        section() {
			label title: "Assign a name", defaultValue: "${app.name}", required: false
            href "pageUninstall", title: "", description: "Uninstall this SmartApp", image: getAppImg("trash-circle-red-512.png"), state: null, required: true
		}
        section("Enter a time if you wish to override the illuminance and turn lights on at a specific time, regardless of whether it's dark out.") {
        	input "presetOnTime", "time", title: "Turn-on time?", required: false
        }
        if (!theLuminance) {
            section("If desired, you can adjust the amount of time after sunset when the app will turn the lights on.") {
                input "sunsetOffset", "number", title: "Sunset offset time", description: "How many minutes?", range: "0..180", required: false
            }
   		}
        section("You can also have the lights turn on for a set amount of time in the morning (i.e. when the mode changes from Night to Home).") {
        	input "morningOn", "bool", title: "Turn on in the morning?", required: false, submitOnChange: true
            if (morningOn) {
            	input "morningDuration", "number", title: "How many minutes?", description: "description", range: "1..60", required: true
            }
        }
        section("Optionally, you can choose to enable this SmartApp only when selected persons are home; if none selected, it will run whenever the mode is set to Home.") {
            input "thePeople", "capability.presenceSensor", title: "Who?", description: "Only when these persons are home", multiple: true, required: false
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

def appInfo() {
	def tz = location.timeZone
    def mapSun = getSunriseAndSunset()
    def debugLevel = state.debugLevel
    def lightsOn = state.lightsOn
    def lightsOnTime = state.lightsOnTime
    def datInstall = state.installTime ? new Date(state.installTime) : null
    def datInitialize = state.initializeTime ? new Date(state.initializeTime) : null
    def datSchedOn = state.schedOnTime ? new Date(state.schedOnTime) : null
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
        strInfo += "  └ number of stored log entries: ${numLogs}\n"
        strInfo += "  └ lightsOn: ${lightsOn}"
        strInfo += (lightsOn && lightsOnTime) ? " (since ${new Date(lightsOnTime).format('HH:mm', tz)})\n" : "\n"
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
    state.lastMode = location.mode
    subscribeToEvents()
    if (presetOnTime) { //if the user set an ON time, schedule the switch
    	schedule(presetOnTime, turnOn)
    }
    timeCheck()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    if (!presetOnTime) { //if the user set an ON time, the sunset is not required
    	subscribe(location, "sunsetTime", sunsetTimeHandler)	//triggers at sunset, evt.value is the sunset String (time for next day's sunset)
    }
    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    subscribe(thePeople, "presence", presenceHandler) //introduced at v1.20
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def sunsetTimeHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sunsetTimeHandler()"]
    debug "sunsetTimeHandler event: ${evt.descriptionText}", "trace", 1
    timeCheck()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sunsetTimeHandler()", duration: elapsed]
    debug "sunsetTimeHandler completed in ${elapsed} seconds", "trace", -1
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
	debug "modeChangeHandler event: from ${state.lastMode} to ${evt.value}", "trace", 1
    if (isMorning && morningOn) {
        turnOn()
		runIn(morningDuration*60, turnOff)
	}
    if (modeOk) {
    	timeCheck()
    } else {
    	turnOff()
    }
    state.lastMode = evt.value
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "modeChangeHandler()", duration: elapsed]
	debug "modeChangeHandler completed in ${elapsed} seconds, setting lastMode to ${state.lastMode}", "trace", -1
}

def presenceHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "presenceHandler()"]
	debug "presenceHandler event: ${evt.descriptionText}", "trace", 1
    for (person in thePeople) {
	    if (person.currentPresence == "present") {
        	timeCheck()
            break
        }
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "presenceHandler()", duration: elapsed]
	debug "presenceHandler completed in ${elapsed} seconds", "trace", -1
}


//   -------------------
//   ***   METHODS   ***

def timeCheck() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "timeCheck()"]
    debug "executing timeCheck()", "trace", 1
	def nowDate = new Date()
    debug "nowDate: ${nowDate}"
    def onTime = getTurnOnTime()
    state.schedOnTime = onTime.time
    debug "onTime: ${onTime}"
    if (onTime > nowDate) {
    	debug "onTime > nowDate; scheduling turnOn for ${onTime}", "info"
    	schedule(onTime, turnOn)
    } else {
    	debug "nowDate >= onTime; calling turnOn()", "info"
        turnOn()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "timeCheck()", duration: elapsed]
    debug "timeCheck() completed in ${elapsed} seconds", "trace", -1
}

def turnOn() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "turnOn()"]
    debug "executing turnOn()", "trace", 1
    if (modeOk && presenceOk) {
    	debug "conditions met; turning lights on", "info"
        theLights.on()
        state.lightsOn = true
	    state.lightsOnTime = now()
    } else {
    	debug "conditions not met; wait for next call"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "turnOn()", duration: elapsed]
    debug "turnOn() completed in ${elapsed} seconds", "trace", -1
}

def turnOff() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "turnOff()"]
    debug "executing turnOff()", "trace", 1
    theLights.off()
    state.lightsOn = false
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "turnOff()", duration: elapsed]
    debug "turnOff() completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getModeOk() {
    def nowMode = location.mode
    def result = (nowMode == "Home") ? true : false
    return result
}

def getPresenceOk() {
	def result = true
    if (thePeople) {
        for (person in thePeople) {
            if (person.currentPresence == "not present") {
            	result = false
                break
            }
		}
    }
    return result
}

def getIsMorning() {
	def nowMode = location.mode
    def lastMode = state.lastMode
    def result = (lastMode == "Night" && nowMode == "Home") ? true : false
    return result
}

def getTurnOnTime() {
    def tz = location.timeZone
    def result = new Date()
    if (presetOnTime) {
        result = timeToday(presetOnTime, tz)
    } else {
    	def offset = sunsetOffset ?: 0
        def sunTime = getSunriseAndSunset(sunsetOffset: offset)
    	result = sunTime.sunset
    }
    return result
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

def getWebData(params, desc, text=true) {
	try {
		debug "trying getWebData for ${desc}"
		httpGet(params) { resp ->
			if(resp.data) {
				if(text) {
					return resp?.data?.text.toString()
				} else { return resp?.data }
			}
		}
	}
	catch (ex) {
		if(ex instanceof groovyx.net.http.HttpResponseException) {
			debug "${desc} file not found", "warn"
		} else {
			debug "getWebData(params: $params, desc: $desc, text: $text) Exception:", "error"
		}
		return "an error occured while trying to retrieve ${desc} data"
	}
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