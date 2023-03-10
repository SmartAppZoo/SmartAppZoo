/**
 *  Garage Door Monitor
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
 *    v2.11 (26-Nov-2019) - fix state.debugLevel by moving the reset to the start of initialization method
 *    v2.10 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v2.02 (23-Nov-2018) - wrap procedures to identify last execution and elapsed time
 *                        - add appInfo section in app settings
 *	  v2.01 (09-Aug-2018) - standardize debug log types and make 'debug' logs disabled by default
 *						  - change category to 'convenience'
 *						  - standardize layout of app data and constant definitions
 *    v2.00 (15-Nov-2016) - code improvement: store images on GitHub, use getAppImg() to display app images
 *                        - added option to disable icons
 *                        - added option to disable multi-level logging
 *                        - configured default values for app settings
 *						  - moved 'About' to its own page
 *						  - added link to readme file
 *    v1.32 (05-Nov-2016) - code improvement: update 'convertToHM()'
 *                        - bug fix: fixed calculation for state.numWarning
 *    v1.31 (04-Nov-2016) - update href state & images
 *    v1.30 (04-Nov-2016) - add option to send periodic reminders
 *	  v1.21 (03-Nov-2016) - add link for Apache license
 *    v1.20 (02-Nov-2016) - implement multi-level debug logging function
 *    v1.13 (01-Nov-2016) - code improvement: standardize pages layout
 *	  v1.12 (01-Nov-2016) - code improvement: standardize section headers
 *    v1.11 (26-Oct-2016) - code improvement: added trace for each event handler
 *    v1.10 (26-Oct-2016) - added 'About' section in preferences
 *    v1.00               - initial release, no version tracking up to this point
 *
*/
definition(
    name: "Garage Door Monitor",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Notify if garage door is left open when leaving the house, left open for too long, or if it opens while away.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@3x.png"
)


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 2.11" }
def		versionDate()			{ return "26-Nov-2019" }     
def		gitAppName()			{ return "garage-door-monitor" }
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
        	paragraph "", title: "This SmartApp sends a notification (SMS optional) to notify " +
	        	"that a door is left open when leaving the house, left open for too long, or if it opens while away."
        }
        section("When I leave") {
            input "myself", "capability.presenceSensor", title: "Who?", multiple: false, required: true
        }
        section("Or when all these persons leave") {
            input "everyone", "capability.presenceSensor", title: "Who?", multiple: true, required: false
        }
        section("Send a notification if this door is left open") {
            input "thedoor", "capability.contactSensor", title: "Which door?", multiple: false, required: true
        }
        section("Notify me if the door opens while I'm away") {
            input "warnOpening", "bool", title: "Yes/No?", required: false, defaultValue: true
        }
        section("Let me know anytime it's left open for too long") {
            input "maxOpenMinutes", "number", title: "How long? (minutes)", defaultValue: 15, required: false, submitOnChange: true
            if (maxOpenMinutes) {
                input "remindMinutes", "number", title: "Remind me every x minutes", description: "Optional", defaultValue: 50, required: false
            }
        }
        section("Also send SMS alerts?"){
        input "phone", "phone", title: "Phone number (For SMS - Optional)", required: false
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
    def debugLevel = state.debugLevel
    def numWarning = state.numWarning
    def timeOpen = state.timeOpen
    def datInstall = state.installTime ? new Date(state.installTime) : null
    def datInitialize = state.initializeTime ? new Date(state.initializeTime) : null
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += datInstall ? "  └ last install date: ${datInstall.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datInitialize ? "  └ last initialize date: ${datInitialize.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "  └ door state: ${thedoor.currentContact}"
        strInfo += (thedoor.currentContact == "open") ? " (since ${new Date(timeOpen).format('HH:mm', tz)})\n" : "\n"
        strInfo += "\n • Last initiated execution:\n"
		strInfo += "  └ name: ${lastInitiatedExecution.name}\n"
        strInfo += "  └ time: ${new Date(lastInitiatedExecution.time).format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "\n • Last completed execution:\n"
		strInfo += "  └ name: ${lastCompletedExecution.name}\n"
        strInfo += "  └ time: ${new Date(lastCompletedExecution.time).format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "  └ time to complete: ${lastCompletedExecution.duration}s\n"
        strInfo += "\n • State stored values:\n"
        strInfo += "  └ debugLevel: ${debugLevel}\n"
        strInfo += "  └ numWarning: ${numWarning}\n"
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
    state.numWarning = 0
    subscribeToEvents()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(myself, "presence.not present", iLeaveHandler)
    subscribe(everyone, "presence.not present", allLeaveHandler)
    subscribe(thedoor, "contact", doorHandler)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def iLeaveHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "iLeaveHandler()"]
    debug "iLeaveHandler event: ${evt.descriptionText}", "trace", 1
    if (thedoor.currentContact != "closed") {
    	def message = "${evt.device} has left the house and the ${thedoor.device} is ${thedoor.currentContact}."
        debug "sendPush : ${message}", "warn"
        sendPush(message)
        sendText(message)
    } else {
    	debug "the ${thedoor.device} is ${thedoor.currentContact}; doing nothing"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "iLeaveHandler()", duration: elapsed]
    debug "iLeaveHandler completed in ${elapsed} seconds", "trace", -1
}

def allLeaveHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "allLeaveHandler()"]
    debug "allLeaveHandler event: ${evt.descriptionText}", "trace", 1
    if (thedoor.currentContact == "open") {
        if (everyoneIsAway) {
            def message = "Everyone has left the house and the ${thedoor.device} is ${thedoor.currentContact}."
            debug "sendPush : ${message}", "warn"
            sendPush(message)
            sendText(message)
		} else {
            debug "The ${thedoor.device} is ${thedoor.currentContact} but not everyone is away; doing nothing"
        }
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "allLeaveHandler()", duration: elapsed]
    debug "allLeaveHandler completed in ${elapsed} seconds", "trace", -1
}

def doorHandler(evt) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "doorHandler()"]
    debug "doorHandler event: ${evt.descriptionText}", "trace", 1
    if (evt.value == "open" && warnOpening && imAway) {
    	def msg = "The ${thedoor.device} was opened."
        debug "sendPush : ${msg}", "warn"
        sendPush(msg)
        sendText(msg)
	} 
    if (evt.value == "open" && maxOpenMinutes) {
    	debug "The ${thedoor.device} was opened; scheduling a check in ${maxOpenMinutes} minutes to see if it's still open.", "info"
    	state.timeOpen = now()
        runIn(60 * maxOpenMinutes, checkOpen)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "doorHandler()", duration: elapsed]
    debug "doorHandler completed in ${elapsed} seconds", "trace", -1
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

def checkOpen() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "checkOpen()"]
    debug "executing checkOpen()", "trace", 1
    def updated_numWarning = state.numWarning
    if (thedoor.currentContact == "open") {
        updated_numWarning ++
        debug "updated_numWarning: ${updated_numWarning}"
        sendNotification()
    } else {
    	debug "The ${thedoor.device} is no longer open."
    }
    state.numWarning = updated_numWarning
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "checkOpen()", duration: elapsed]
    debug "checkOpen() completed in ${elapsed} seconds", "trace", -1
}

def sendNotification() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sendNotification()"]
	debug "executing sendNotification()", "trace", 1
    int elapsedOpen = now() - state.timeOpen
    debug "state.numWarning : ${state.numWarning}"
    debug "now() - state.timeOpen = ${now()} - ${state.timeOpen} = ${elapsedOpen}"
    def msg = "The ${thedoor.device} has been opened for ${convertToHM(elapsedOpen)}"
    debug "sendPush : ${msg}", "warn"
    sendPush(msg)
    sendText(msg)
    setReminder()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sendNotification()", duration: elapsed]
    debug "sendNotification() completed in ${elapsed} seconds", "trace", -1
}

def sendText(msg) {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "sendText()"]
    debug "executing sendText(msg: ${msg})", "trace", 1
	if (phone) {
		debug "sending SMS", "info"
		sendSms(phone, msg)
	} else {
    	debug "SMS number not configured"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "sendText()", duration: elapsed]
    debug "sendText() completed in ${elapsed} seconds", "trace", -1
}

def setReminder() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "setReminder()"]
	debug "executing sendReminder()", "trace", 1
    if (remindMinutes) {
    	def remindSeconds = 60 * remindMinutes
        debug "scheduling a reminder check in ${remindMinutes} minutes", "info"
        runIn(remindSeconds, checkOpen)
    } else {
    	debug "reminder option not set"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "setReminder()", duration: elapsed]
    debug "sendReminder() completed in ${elapsed} seconds", "trace", -1
}

//   -------------------------
//   ***   APP FUNCTIONS   ***

def getEveryoneIsAway() {
    def result = true
    for (person in everyone) {
        if (person.currentPresence == "present") {
            result = false
            break
        }
    }
    debug ">> everyoneIsAway : ${result}"
    return result
}

def getImAway() {
	def result = !(myself.currentPresence == "present")
    debug ">> imAway : ${result}"
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

def convertToHM(ms) {
	//def df = new DecimalFormat("00")
    int hours = Math.floor(ms/1000/60/60)
    double dblMin = ((ms/1000/60) - (hours * 60))
    int minutes = dblMin.round()
	def strHr = hours == 1 ? "hour" : "hours"
    def strMin = minutes == 1 ? "minute" : "minutes"
    def result = (hours == 0) ? "${minutes} ${strMin}" : "${hours} ${strHr} and ${minutes} ${strMin}"
    return result
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