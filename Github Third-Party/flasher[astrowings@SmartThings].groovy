/**
 *  Flasher
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
 *    v0.10 (05-Dec-2019) - developing
 *
*/

definition(
    name: "Flasher",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Flash lights when an alarm is triggered.",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@3x.png"
)


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 1.00" }
def		versionDate()			{ return "12-Dec-2019" }     
def		gitAppName()			{ return "flasher" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
int		C_MAXFLASH()			{ return 15 }			//max duration (seconds) for continuous flashing sequence in order to avoid execution timeout error
int		C_FLASHPAUSE()			{ return 2 }			//pause duration (seconds) between continuous flash sequences if a longer flash period is desired
int		C_MSON_DEFAULT()		{ return 1000 }			//set the default value for msOn (flash-on duration, seconds) if none is selected by user
int		C_MSOFF_DEFAULT()		{ return 400 } 			//set the default value for msOff (flash-off duration, seconds) if none is selected by user


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
    int maxFlash = C_MAXFLASH()
    int flashPause = C_FLASHPAUSE()
    int msOn_default = C_MSON_DEFAULT()
    int msOff_default = C_MSOFF_DEFAULT()
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section(){
        	paragraph "", title: "This SmartApp flashes selected lights when an alarm is triggered."
        }
        section("Monitor this alarm (lights will flash when it goes off):") {
            input "theAlarm", "capability.alarm", title: "Which alarm?", required: true, multiple: false //TODO: make multiple?
        }
        section("Set the lights' flashing options (note that too short on/off cycles may result in irregularities in " +
        	"the flashing pattern if the switches can't react fast enough. Also, due to the system's limitations, continuous flashing sequence " +
        	"is limited to ${maxFlash} seconds. If a longer flashing duration is selected, flashing will pause for " +
            "approximately ${flashPause} seconds after every ${maxFlash}-second flash sequence and repeat until desired time is " +
            "achieved. If no time (or zero) is selected, the lights will keep flashing following the ${maxFlash}/${flashPause} " +
            "pattern until the alarm is dismissed):") {
            input "theLights", "capability.switch", title: "Which lights?", required: true, multiple: true
            input "sFlashDuration", "number", title: "Flash the light(s) for how long (seconds)?", required: false, defaultValue: 30
            input "msOn", "number", title: "Flash on for how long (ms)?", required: false, defaultValue: msOn_default
            input "msOff", "number", title: "Flash off for how long (ms)?", required: false, defaultValue: msOff_default
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
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += installTime ? "  └ last install date: ${new Date(installTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += initializeTime ? "  └ last initialize date: ${new Date(initializeTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "  └ current mode: ${location.currentMode}\n"
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
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "initialize()", duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def subscribeToEvents() {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "subscribeToEvents()"]
    debug "subscribing to events", "trace", 1
    subscribe(location, "position", locationPositionChange) //update settings if the hub location changes
    subscribe(theAlarm, "alarm", alarmHandler)
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

def alarmHandler(evt) {
    def mName = "alarmHandler()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "alarmHandler event description: ${evt.description}", "trace", 1
    //debug "alarmHandler event date: ${evt.date}", "debug"
    //debug "alarmHandler event device: ${evt.device}", "debug"
    //debug "alarmHandler event name: ${evt.name}", "debug"
    //debug "alarmHandler event value: ${evt.value}", "debug"
    def alarmMode = evt.value
    boolean flashOk = alarmMode == "both" || alarmMode == "siren" || alarmMode == "strobe"
    boolean alarmOff = alarmMode == "off"
    debug "the alarm mode changed to '${alarmMode}'", "debug"
    if (flashOk) {
        int msFlashDuration = sFlashDuration ? sFlashDuration * 1000 : 0
        def data = [swInit: theLights.currentSwitch, msMaxflash: C_MAXFLASH() * 1000, sPause: C_FLASHPAUSE(), msTimeLeft: msFlashDuration]
        flashGo(data)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}


//   -------------------
//   ***   METHODS   ***

def flashGo(data) {
    def mName = "flashGo()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    def swInit = data.swInit
    int msMaxflash = data.msMaxflash
    int sPause = data.sPause
    int msTimeLeft = data.msTimeLeft
    boolean timeLimit = msTimeLeft > 0 //check if user entered a flash duration (sFlashDuration)
	debug "executing flashGo(swInit: ${swInit}, msMaxflash: ${msMaxflash}, sPause: ${sPause}, msTimeLeft: ${msTimeLeft})", "trace", 1
    int msOn = settings.msOn ?: C_MSON_DEFAULT()
    int msOff = settings.msOff ?: C_MSOFF_DEFAULT()
    int msElapsed = 0
	while (msElapsed < msMaxflash) {
        if (theAlarm.currentAlarm == "off") {
        	debug "alarmOff condition detected by flashGo"
            break
        } else {
            turnOn(msOn)
            turnOff(msOff)
            msElapsed = now() - startTime
        }
	}
    msTimeLeft -= msElapsed //subtract the time it took to execute this flash sequence from the total time selected by the user
	boolean alarmOn = theAlarm.currentAlarm != "off"
    boolean runAgain =  !timeLimit || msTimeLeft > (msElapsed + (sPause * 1000)) //just keep on flashing if user didn't input a flash duration (!timeLimit),
    																			 //otherwise check if there's enbough time left to run another sequence
    if (runAgain && alarmOn) {
    	data.msTimeLeft = msTimeLeft
        debug "scheduling to run again in ${sPause} seconds with data: ${data}"
        runIn(sPause, flashGo, [data: data]) //schedule the next flash sequence to run after a pause to avoid a timeout exception error, pass remaining time left in the data
    } else {
    	resetSwitch(swInit)
    }
	def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def resetSwitch(swInit) {
    def mName = "resetSwitch()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
	debug "executing resetSwitch(swInit: ${swInit})", "trace", 1
	unschedule(flashGo)
    debug "swInit: ${swInit}"
    swInit.eachWithIndex { it, i ->
    	debug "${theLights[i].label}'s initial switch state: ${it}"
        if (it == "on") {
        	debug "turning the ${theLights[i].label} back on"
            theLights[i].on()
        } else {
        	theLights[i].off()
        }
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}   

def turnOff(int ms) {
    theLights.off()
    pause(ms)
}

def turnOn(int ms) {
    theLights.on()
    pause(ms)
}

def sendSOS() {
def swval = switch1.currentSwitch
log.debug "sendSOS tu=$cmson swval=$swval"
if(swval == "on") {
turnOff(7*cmson)  /* seven time units between words. For five words/minute set Morse timing unit(cmson) = 240ms.*/
}
def dit = { turnOn(cmson);   turnOff(cmson); }
def dah = { turnOn(3*cmson); turnOff(cmson); }
def ics = { pause(2*cmson) } /* Inter character spacing is three time units, but one unit has elapsed already */
dit(); dit(); dit(); ics()
dah(); dah(); dah(); ics()
dit(); dit(); dit() 
pause(6*cmson)   
resetSwitch(swval) 
}


//   -------------------------
//   ***   APP FUNCTIONS   ***



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