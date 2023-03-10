/**
 *  Block Heater
 *
 *  Copyright © 2019 Phil Maynard
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
 *    v1.00 (12-Dec-2019) - initial release
 *    v0.10 (10-Dec-2019) - developing
 *
*/

definition(
    name: "Block Heater",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Control an outlet for car block heater operation.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn@3x.png"
)


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 1.00" }
def		versionDate()			{ return "12-Dec-2019" }     
def		gitAppName()			{ return "block-heater" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
int		C_MINWATTS()			{ return 2 }			//minimum power draw below which the outlet will switch off (if selected)


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSettings")
    page(name: "pageAbout")
    page(name: "pageLogOptions")
    page(name: "pageUninstall")
}


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
    def minWatts = C_MINWATTS()
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section(){
        	paragraph "", title: "This SmartApp controls the activation of a smart outlet based on various criteria for the operation of a car engine's block heater."
        }
        section("") {
            input "theOutlet", "capability.switch", title: "Which outlet do you wish to use to turn the block heater on/off?", required: true, submitOnChange: true
        }
        section("It's generally recommended to run the block heater for a period of 2 to 4 hours before " +
            "starting the vehicle when temperatures dip below -10 deg C)") {
            input "onTime", "time", title: "Start time?", required: true
            input "offTime", "time", title: "Stop time?", required: true
        }
        section("Set the turn-on conditions:") {
            input "minTemp", "number", title: "Minimum temperature? Block heater operation is disabled if temperature is above this setting (leave blank to operate regardless of temperature).", required: false, submitOnChange: true, range: "-50..50"
            input "minTempSensor", "capability.temperatureMeasurement", title: "Which temperature sensor? (required if min temp is set).", multiple: false, required: (minTemp != null)
            input "thePeople", "capability.presenceSensor", title: "Which people? Block heater will only operate when at least one of the selected people are present (leave blank to operate regardless of presence).", multiple: true, required: false
            input "theDays", "enum", title: "On which days? Block heater will only operate on selected days of the week (disabled if none selected).", options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"], required: false, multiple: true
            input "theModes", "mode", title: "Which modes? Block heater will only operate in selected modes (disabled if none selected).", multiple: true, required: false
        }
		boolean hasPower = theOutlet.hasCapability("Power Meter")
        if (hasPower) {
            section("Set the turn-off triggers:") {
                input "loPowerOff", "bool", title: "Switch off below 2 watts? This setting automatically switches the outlet off when the power draw drops below ${minWatts} watts (so as not not to leave the extension cord unecessarily powered after the block heater is disconnected, for example).", defaultValue: true, required: false
            }
        }
        section() {
            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
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
        	href url: readmeLink(), title: app.name, description: "Copyright ©2019 Phil Maynard\n${versionNum()}", image: getAppImg("readme-icn.png")
            href url: urlApache(), title: "License", description: "View Apache license", image: getAppImg("license-icn.png")
		}
    }
}

def pageLogOptions() {
	dynamicPage(name: "pageLogOptions", title: "IDE Logging Options", install: false, uninstall: false) {
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
    int debugLevel = state.debugLevel ?: 0
    int installTime = state.installTime ?: 0
    int initializeTime = state?.initializeTime ?: 0
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def appOn = state.appOn
    def minTempActual = minTempSensor?.currentTemperature
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += installTime ? "  └ last install date: ${new Date(installTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += initializeTime ? "  └ last initialize date: ${new Date(initializeTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "  └ appOn: ${appOn}\n"
        strInfo += "  └ current mode: ${location.currentMode}\n"
        strInfo += "\n • Conditions and sensors:\n"
        strInfo += "  └ days: ${theDays} (nowDOW: ${nowDOW})\n"
        if (thePeople) {
        	thePeople.each {
            	strInfo += "  └ ${it.label}: ${it.currentPresence}\n"
            }
        }
        strInfo += theOutlet ? "  └ power draw at the ${theOutlet.label}: ${theOutlet.currentPower} W\n" : ""
        strInfo += minTempActual ? "  └ temp at ${minTempSensor.label}: ${minTempActual} deg\n" : "  └ minimum temperature not set\n"
        strInfo += "\n • Last initiated execution:\n"
		strInfo += lastInitiatedExecution ? "  └ name: ${lastInitiatedExecution.name}\n" : ""
        strInfo += lastInitiatedExecution ? "  └ time: ${new Date(lastInitiatedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += "\n • Last completed execution:\n"
		strInfo += lastCompletedExecution ? "  └ name: ${lastCompletedExecution.name}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time: ${new Date(lastCompletedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time to complete: ${lastCompletedExecution.duration}s\n" : ""
        strInfo += "\n • State stored values:\n"
        strInfo += "  └ debugLevel: ${debugLevel}\n"
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
    subscribeToEvents()
    schedule(onTime, turnOn)
    schedule(offTime, turnOff)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(location, "position", locationPositionChange) //update settings if the hub location changes
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def locationPositionChange(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "locationPositionChange()"]
    debug "locationPositionChange(${evt.descriptionText})", "warn"
	initialize()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "locationPositionChange()", duration: elapsed]
}

def capabilityHandler(evt) {
    def mName = "capabilityHandler()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "${mName} event description: ${evt.description}", "trace", 1
    
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}


//   -------------------
//   ***   METHODS   ***

def turnOn() {
    def mName = "turnOn()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "executing ${mName}", "trace", 1
    if (turnOnOk) {
        debug "turn-on conditions met; turning the ${theOutlet.label} on", "info"
        state.appOn = true
        theOutlet.on()
        watchTriggers()
    } else {
    	debug "turn-on conditions not met; doing nothing" //TODO: do something (wait for conditions, reschedule, subscribe, etc.)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def watchTriggers() {
    def mName = "watchTriggers()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "executing ${mName}", "trace", 1
    if (loPowerOff) {
    	debug "subscribing to theOutlet.powerMeter"
        subscribe(theOutlet, "power", powerHandler)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}
    
def powerHandler(evt) {
    def mName = "powerHandler()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "${mName} event description: ${evt.description}", "trace", 1
    def watts = evt.floatValue
    def minWatts = C_MINWATTS()
    if (watts < minWatts) {
    	debug "low power condition detected (${watts} watts at the ${theOutlet.label}); calling to turn off the outlet", "info"
        turnOff()
    } else {
    	debug "power draw at the ${theOutlet.label}: ${watts} W"
    }
	def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def turnOff() {
    def mName = "turnOff()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "executing ${mName}", "trace", 1
	boolean appOn = state.appOn
    if (appOn) {
    	debug "turning the ${theOutlet.label} off", "info"
        unsubscribe(theOutlet)
        state.appOn = false
        theOutlet.off()
    } else {
    	debug "the ${theOutlet.label} wasn't turned on by this app; doing nothing"
    }
	def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getTurnOnOk() {
    boolean result = modeOk && daysOk && presenceOk &&minTempOk
    return result
}

def getMinTempOk() {
	def nowTemp = minTempSensor?.currentTemperature
    boolean result = !nowTemp || nowTemp < minTemp
    debug ">> minTempOk : $result (nowTemp = ${nowTemp})"
    return result
}

def getPresenceOk() {
	boolean result = true
    if (thePeople) {
    	result = false
        for (p in thePeople) {
            if (p.currentPresence == "present") {
            	result = true
                break
            }
		}
    }
    debug ">> presenceOk : $result"
    return result
}

def getModeOk() {
	boolean result = theModes?.contains(location.mode)
	debug ">> modeOk : $result"
	return result
}

def getDaysOk() {
	boolean result = theDays?.contains(nowDOW)
    debug ">> DOWOk : $result"
    return result
}

def getNowDOW() { //method to obtain current weekday adjusted for local time
    def javaDate = new java.text.SimpleDateFormat("EEEE, dd MMM yyyy @ HH:mm:ss")
    def javaDOW = new java.text.SimpleDateFormat("EEEE")
    if (location.timeZone) {
        javaDOW.setTimeZone(location.timeZone)
    }
    def strDOW = javaDOW.format(new Date())
    debug ">> nowDOW : $strDOW"
    return strDOW
}


//   ------------------------
//   ***   COMMON UTILS   ***

def convertToHMS(ms) {
    int hours = Math.floor(ms/1000/60/60)
    int minutes = Math.floor((ms/1000/60) - (hours * 60))
    int seconds = Math.floor((ms/1000) - (hours * 60 * 60) - (minutes * 60))
    long millisec = ms-(hours*60*60*1000)-(minutes*60*1000)-(seconds*1000)
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
	
    lvl = lvl ?: "debug"
	if (!settings["log#$lvl"]) {
		return
	}
	
    def multiEnable = (settings.setMultiLevelLog == false ? false : true) //set to true by default
    int maxLevel = 4
	int level = state.debugLevel ?: 0
	int levelDelta = 0
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
        int maxLogs = settings.maxInfoLogs ?: 5
        int listSize = debugLog.size()
        while (listSize > maxLogs) { //delete old entries to prevent list from growing beyond set size
            debugLog.remove(maxLogs)
            listSize = debugLog.size()
        }
    	state.debugLogInfo = debugLog
    }
}