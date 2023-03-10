/**
 *  Away Light
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0											*/
 	       def urlApache() { return "http://www.apache.org/licenses/LICENSE-2.0" }			/*
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *   --------------------------------
 *   ***   VERSION HISTORY  ***
 *
 *    v2.70 (04-Dec-2019) - add option to leave the light on when the mode changes to home even if 'Home' is not a selected mode
 *    v2.61 (01-Dec-2019) - add minimum delay when calling to switch the light on/off to fix the execution timeout issue (finally working!)
 *                        - undo v2.60 (add delay in turnOn to wait until the light actually turns on before invoking schedTurnOff)
 *    v2.60 (29-Nov-2019) - add delay in turnOn to wait until the light actually turns on before invoking schedTurnOff in attempt to fix the execution timeout issue
 *    v2.51 (28-Nov-2019) - use runOnce instead of runIn to call turnOff()
 *    v2.50 (27-Nov-2019) - add appOk to enable crash-check
 *    v2.42 (26-Nov-2019) - remove redundant random computation of offForDelay in schedTurnOn() because it's already computed and passed from turnOff()
 *                        - move schedOffDate() to be part of turnOn()
 *                        - add square brackets to enclose delay when sending delayed device commands
 *                        - add ? to date variables that may be null so as not to get an error when trying to format a null object
 *                        - replace the runOnce in schedTurnOn where offForDelayMS > 0 to a runIn to troubleshoot the execution timeout issue
 *                        - fix state.debugLevel by moving the reset to the start of initialization method
 *                        - general code cleanup and validation
 *    v2.41 (18-Nov-2019) - explicitly define int variable types
 *                        - cast division result to int to prevent error when converting Unix time to date
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *    v2.40 (14-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *    v2.34 (08-Nov-2019) - wrap procedures to identify last execution and elapsed time
 *                        - add appInfo section in app settings
 *                        - revert to using state instead of atomicState
 *	  v2.33 (26-Sep-2019) - use atomicState instead of state in an effort to fix a bug where the light doesn't turn off because state.appOn wasn't set to true
 *    v2.32 (27-May-2019) - integrate debug option to skip appOn check before turning off the light
 *	  v2.31 (09-Aug-2018) - standardize debug log types
 *						  - change category to 'convenience'
 *						  - standardize layout of app data and constant definitions
 *						  - convert hard-coded value for 'onNowRandom' into constant
 *    v2.30 (18-Oct-2017) - add call to terminate() method to turn light off when mode changes to one that isn't enabled
 *    v2.21 (09-Jan-2017) - add schedule to run schedTurnOn() daily
 *    v2.20 (29-Dec-2016) - add user-configurable activation delay after mode changes
 *	  v2.10 (14-Nov-2016) - create reinit() method to allow parent to re-initialize all child apps
 *						  - bug fix: specify int data type to strip decimals when using the result of a division
 * 							to obtain a date, which returns the following error if trying to convert a decimal:
 *							Could not find matching constructor for: java.util.Date(java.math.BigDecimal)
 *    v2.00 (14-Nov-2016) - code improvement: store images on GitHub, use getAppImg() to display app images
 *                        - added option to disable icons
 *                        - added option to disable multi-level logging
 *                        - configured default values for app settings
 *						  - moved 'About' to its own page
 *						  - added link to readme file
 *						  - bug fix: removed log level increase for modeChangeHandler() event because it was causing the log
 *							level to keep increasing without ever applying the '-1' at the end to restore the log level
 *						  - added parent definition to convert into child app
 *						  - list current configuration settings in link to configuration page
 *						  - moved sunset offset setting to parent app
 *						  - moved debugging options settings to parent app
 *    v1.31 (04-Nov-2016) - update href state & images
 *	  v1.30 (03-Nov-2016) - add option to configure sunset offset
 *    v1.21 (02-Nov-2016) - add link for Apache license
 *    v1.20 (02-Nov-2016) - implement multi-level debug logging function
 *	  v1.14 (01-Nov-2016) - code improvement: standardize pages layout
 *	  v1.13 (01-Nov-2016) - code improvement: standardize section headers
 *	  v1.12 (27-Oct-2016) - change layout of preferences pages, default value for app name
 *    v1.11 (26-Oct-2016) - code improvement: added trace for each event handler
 *    v1.10 (26-Oct-2016) - added 'About' section in preferences
 *    v1.00               - initial release, no version tracking up to this point
 *
*/
definition(
    parent: "astrowings:Away Lights",
    name: "Away Light",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn a light on/off to simulate presence while away",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn@3x.png")


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 2.70" }
def		versionDate()			{ return "04-Dec-2019" }     
def		gitAppName()			{ return "away-light" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
def		C_ON_NOW_RANDOM()		{ return 2 } 			//set the max value (integer) for the random delay to be applied when requesting to turn on now (minutes)
def		C_OFF_NOW_RANDOM()		{ return 2 } 			//set the max value (integer) for the random delay to be applied when requesting to turn off now (minutes)
def		C_MIN_DELAY()			{ return 200}			//set the minimum delay for when calling to switch the light immediately (a delay of 0 seems to be causing timeout exception errors)


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
	page(name: "pageSchedule")
}


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
	dynamicPage(name: "pageMain", install: true, uninstall: false) {
        section() {
            input "theLight", "capability.switch", title: "Which light?", multiple: false, required: true, submitOnChange: true
            if (theLight) {
                href "pageSchedule", title: !theModes ? "Set scheduling options" : "Scheduling options:", description: schedOptionsDesc, image: getAppImg("office7-icn.png"), required: true, state: theModes ? "complete" : null
        	}
		}
		section() {
			if (theLight) {
            	label title: "Assign a name for this automation", defaultValue: "${theLight.label}", required: false
            }
        }
        section("Debugging Tools", hideable: true, hidden: true) {
            paragraph title: "Application info", appInfo()
        }
    
    }
}

def pageSchedule() {
	dynamicPage(name: "pageSchedule", install: false, uninstall: false) {
        section(){
        	paragraph title: "Scheduling Options",
            	"Use the options on this page to set the scheduling options for the ${theLight.label}"
        }
        section("Restrict automation to certain times (optional)") {
            input "userOnTime", "time", title: "Start time?", required: false
            input "userOffTime", "time", title: "End time?", required: false
        }
        section("Set light on/off duration - use these settings to have the light turn on and off within the activation period") {
            input "onFor", "number", title: "Stay on for (minutes)?", required: false, defaultValue: 25 //If set, the light will turn off after the amount of time specified (or at specified end time, whichever comes first)
            input "offFor", "number", title: "Leave off for (minutes)?", required: false, defaultValue: 40 //If set, the light will turn back on after the amount of time specified (unless the specified end time has passed)
        }
        section("Random factor - if set, randomize on/off times within the selected window") {
        	input "randomMinutes", "number", title: "Random window (minutes)?", required: false, defaultValue: 20
        }
        section("Enable only for certain days of the week? (optional - will run every day if nothing selected)") {
        	input "theDays", "enum", title: "On which days?", options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"], required: false, multiple: true
        }
        section("Select activation modes (automation disabled if none selected) and time to wait (minutes) following mode change before the light automation " +
        	"activates. You can also choose to leave the light on when the mode changes to 'Home' (i.e. if the light was turned on by this app while away, " +
            "don't turn it off when you return home as it normally would if 'Home' is not selected as a mode of operation.") {
            input "theModes", "mode", title: "Select the mode(s)", multiple: true, required: false, submitOnChange: true
            if (theModes) {
                input "activationDelay", "number", title: "Activation delay", required: false, defaultValue: 2, range: "0..60"
                input "leaveOn", "bool", title: "Leave on when returning home?", required: false, defaultValue: false
            }
        }
    	section("Enable even during daytime") {
        	input "daytime", "bool", title: "Yes/No?", required: false, defaultValue: false
        }
        /*
        if (!daytime) {
            section("This SmartApp uses illuminance as a criteria to trigger actions; select the illuminance-capable " +
                    "device to use (if none selected, sunset/sunrise times will be used instead.",
                    hideWhenEmpty: true, required: true, state: (theLuminance ? "complete" : null)) {
                //TODO: implement use of illuminance device
                input "theLuminance", "capability.illuminanceMeasurement", title: "Which illuminance device?", multiple: false, required: false, submitOnChange: true
            }
        }
        */
	}
}


//   ---------------------------------
//   ***   PAGES SUPPORT METHODS   ***

def getSchedOptionsDesc() {
    def strUserOnTime = userOnTime?.substring(11,16)
    def strUserOffTime = userOffTime?.substring(11,16)
    def strDesc = ""
    strDesc += (strUserOnTime || strUserOffTime)		? " • Time:\n" : ""
    strDesc += (strUserOnTime && !strUserOffTime)	? "   └ from: ${strUserOnTime}\n" : ""
    strDesc += (!strUserOnTime && strUserOffTime)	? "   └ until: ${strUserOffTime}\n" : ""
    strDesc += (strUserOnTime && strUserOffTime)		? "   └ between ${strUserOnTime} and ${strUserOffTime}\n" : ""
    strDesc += (onFor || offFor)				? " • Duration:\n" : ""
    strDesc += (onFor && !offFor)				? "   └ stay on for: ${onFor} minutes\n" : ""
    strDesc += (onFor && offFor)				? "   └ ${onFor} minutes on / ${offFor} off\n" : ""
    strDesc += randomMinutes					? " • ${randomMinutes}-min random window\n" : ""
    strDesc += theDays							? " • Only on selected days\n   └ ${theDays}\n" : " • Every day\n"
    strDesc += theModes							? " • While in modes:\n   └ ${theModes}\n   └ delay: ${activationDelay} minutes\n" : ""
    strDesc += daytime	 						? " • Enabled during daytime\n" : ""
    strDesc += leaveOn	 						? " • Leave light on when mode changes to Home\n" : ""
    return theModes ? strDesc : "No modes selected; automation disabled"
}

def appInfo() { 
    def tz = location.timeZone
    def mapSun = getSunriseAndSunset()
    def debugLevel = state.debugLevel
    def appOn = state.appOn
    def switchOnTime = state.switchTime?.on
    def switchOffTime = state.switchTime?.off
    def installTime = state.installTime
    def initializeTime = state.initializeTime
    def turnOnTime = state.scheduled?.turnOn
    def schedTurnOnTime = state.scheduled?.schedTurnOn
    def turnOffTime = state.scheduled?.turnOff
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
    def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += installTime ? "  └ last install date: ${new Date(installTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += initializeTime ? "  └ last initialize date: ${new Date(initializeTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "  └ appOn: ${appOn}\n"
        strInfo += "  └ appOk: ${appOk}\n"
        strInfo += "\n • Last scheduled jobs:\n"
        strInfo += schedTurnOnTime ? "  └ schedTurnOn: ${new Date(schedTurnOnTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += turnOnTime ? "  └ turnOn: ${new Date(turnOnTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += turnOffTime ? "  └ turnOff: ${new Date(turnOffTime).format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "\n • Last initiated execution:\n"
		strInfo += lastInitiatedExecution ? "  └ name: ${lastInitiatedExecution.name}\n" : ""
        strInfo += lastInitiatedExecution ? "  └ time: ${new Date(lastInitiatedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += "\n • Last completed execution:\n"
		strInfo += lastCompletedExecution ? "  └ name: ${lastCompletedExecution.name}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time: ${new Date(lastCompletedExecution.time).format('dd MMM HH:mm:ss', tz)}\n" : ""
        strInfo += lastCompletedExecution ? "  └ time to complete: ${lastCompletedExecution.duration}s\n" : ""
		strInfo += "\n • Environment:\n"
        strInfo += "  └ sunrise: ${mapSun.sunrise.format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "  └ sunset: ${mapSun.sunset.format('dd MMM HH:mm:ss', tz)}\n"
        strInfo += "\n • State stored values:\n"
        strInfo += "  └ debugLevel: ${debugLevel}\n"
        strInfo += switchOnTime ? "  └ switchOnTime: ${new Date(switchOnTime).format('HH:mm', tz)}\n" : ""
        strInfo += switchOffTime ? "  └ switchOffTime: ${new Date(switchOffTime).format('HH:mm', tz)}\n" : ""
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
	if (state.appOn) {
        state.appOn = false
        state.switchTime.off = now()
    	theLight.off()
        }
    debug "application uninstalled", "trace", 0
}

def initialize() {
    state.debugLevel = 0
    def mName = "initialize()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "initializing", "trace", 1
    unschedule()
    state.initializeTime = now()
    state.appOn = false
    state.scheduled = [turnOn: null, schedTurnOn: now(), turnOff: null, schedTurnOff: null]
    state.switchTime = [on: null, off: null]
    theLight.off()
    subscribeToEvents()
    schedule("55 44 3 1/1 * ?", schedTurnOn) //run schedTurnOn at 03:44:55 daily
	schedTurnOn()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "initialization completed in ${elapsed} seconds", "trace", -1
}

def reinit() {
    debug "refreshed with settings ${settings}", "trace", 0
    initialize()
}

def subscribeToEvents() {
    debug "subscribing to events", "trace", 1
    subscribe(location, "mode", modeChangeHandler)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    debug "subscriptions complete", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def modeChangeHandler(evt) {
    def mName = "modeChangeHandler()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "modeChangeHandler event: ${evt.descriptionText}", "trace", 1
    if(modeOk) {
        int delay = activationDelay ? activationDelay * 60 : 5
        debug "mode changed to ${evt.value}; calling schedTurnOn() in ${delay} seconds", "info"
        state.scheduled.schedTurnOn = now() + (1000 * delay)
        runIn(delay,schedTurnOn)
    } else {
        debug "mode changed to ${evt.value}; cancelling scheduled tasks", "info"
        state.scheduled.turnOn = null
        state.scheduled.turnOff = null
        unschedule(turnOn)
        unschedule(turnOff)
		if (leaveOn && evt.value == "Home") {
            debug "leaving the light on as per user settings", "debug"
            state.appOn = false
        } else {
	        terminate()
		}
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def locationPositionChange(evt) {
    def mName = "locationPositionChange()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "locationPositionChange(${evt.descriptionText})", "warn"
	initialize()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
}


//   -------------------
//   ***   METHODS   ***

def schedTurnOn(offForDelayMS) { //determine turn-on time and schedule the turnOn() that will verify the remaining conditions before turning the light on
    def mName = "schedTurnOn()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "executing schedTurnOn(offForDelayMS: ${offForDelayMS})", "trace", 1
	
    def tz = location.timeZone	
    def random = new Random()
    def nowDate = new Date()
    
    if (offForDelayMS) { //method was called from turnOff() to turn the light back on after the "offFor" delay
        def onDate = new Date(now() + offForDelayMS)
        debug "calculated ON time for turning the light back on after the 'off for' delay of ${convertToHMS(offForDelayMS)} : ${onDate.format('dd MMM HH:mm:ss', tz)}", "info"
        state.scheduled.turnOn = onDate.time
        //runOnce(onDate, turnOn) ** TODO: replace permanently with runIn (below) if succesful in avoiding timeout exception errors
        //                                 (which also entails passing on the offForDelay in seconds rather than having to convert back from ms)
        int offForDelayS = (int)(offForDelayMS/1000)
        runIn(offForDelayS, turnOn)
	} else {   
        def onDate = userOnTime ? timeToday(userOnTime, tz) : null
        if (randomMinutes && onDate) { //apply random factor to onDate
            int rdmOffset = random.nextInt(randomMinutes)
            onDate = new Date(onDate.time - (randomMinutes * 30000) + (rdmOffset * 60000))
            debug "random-adjusted turn-on time : ${onDate.format('dd MMM HH:mm:ss', tz)}"
        } else {
            debug "no random factor configured in preferences"
        }
        
        //set a random delay of up to 'C_ON_NOW_RANDOM()' min to be applied if requesting to turn on now
        int onNowRandom = C_ON_NOW_RANDOM()
        int onNowRandomMS = onNowRandom * 60 * 1000
        int onNowDelayMS = random.nextInt(onNowRandomMS)
        
        if (!onDate) { //no turn-on time set, call method to turn light on now; whether or not it actually turns on will depend on dow/mode
            debug "no turn-on time specified; calling to turn the light on in ${convertToHMS(onNowDelayMS)}", "info"
            state.scheduled.turnOn = now()
            turnOn(onNowDelayMS)
        } else {
            if (onDate < nowDate) {
                debug "scheduled turn-on time of ${onDate.format('dd MMM HH:mm:ss', tz)} has already passed; calling to turn the light on in ${convertToHMS(onNowDelayMS)}", "info"
                state.scheduled.turnOn = now()
                turnOn(onNowDelayMS)
            } else {
                debug "scheduling the light to turn on at ${onDate.format('dd MMM HH:mm:ss', tz)}", "info"
                state.scheduled.turnOn = onDate.time
                runOnce(onDate, turnOn)
            }
        }
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def turnOn(delayMS) { //check conditions and turn the light on
    def mName = "turnOn()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
    debug "executing turnOn(delayMS: ${delayMS})", "trace", 1

    def tz = location.timeZone
    def random = new Random()
	def strDOW = nowDOW
    def DOWOk = !theDays || theDays?.contains(strDOW)
    def darkOk = daytime || itsDarkOut
    
	if (modeOk && DOWOk && darkOk) {
        def nowDate = new Date(now() + (randomMinutes * 30000)) //add 1/2 random window to current time to enable the light to come on around the sunset time
        def offDate = userOffTime ? timeToday(userOffTime, tz) : null
        if (!daytime) { //get the earliest of user-preset start time and sunrise time
            def sunriseString = location.currentValue("sunriseTime") //get the next sunrise time string
            def sunriseDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
            debug "comparing end time (${offDate?.format('dd MMM HH:mm:ss', tz)}) to sunrise time (${sunriseDate.format('dd MMM HH:mm:ss', tz)})"
            offDate = offDate && (offDate < sunriseDate) ? offDate : sunriseDate
        }
        debug "calculated turn-off time : ${offDate?.format('dd MMM HH:mm:ss', tz)}"
        if (randomMinutes && offDate) {
            //apply random factor to offDate
            int rdmOffset = random.nextInt(randomMinutes)
            def offTime = offDate.time - (randomMinutes * 30000) + (rdmOffset * 60000)
            offDate = new Date(offTime)
            debug "random-adjusted turn-off time : ${offDate.format('dd MMM HH:mm:ss', tz)}"
        } else {
            debug "no random factor configured in preferences"
        }
        if (!offDate || offDate > nowDate) {    	
            delayMS = delayMS ?: C_MIN_DELAY()
            debug "we're good to go; turning the light on in ${convertToHMS(delayMS)}", "info"
            state.appOn = true
            state.switchTime.on = now()
            int timerStart = now()
            theLight.on([delay: delayMS])
            int timerEnd = now()
	        debug "it took ${(timerEnd - timerStart)} ms to execute theLight.on([delay: ${delayMS}])", "debug"
            int delayS = delayMS ? (int)(delayMS/1000) : 5
            def offTime = offDate ? offDate.time : null
            schedTurnOff(delayMS, offTime)
        } else {
            debug "the light's turn-off time has already passed"
        }
    } else {
        if (!modeOk) {
    		debug "light activation is not enabled in current mode; check again at mode change"
    	} else if (!DOWOk) {
            debug "light activation is not enabled on ${strDOW}"
        } else if (!darkOk) {
        	def sunTime = getSunriseAndSunset(sunsetOffset: parent.sunsetOffset)
            def sunsetDate = sunTime.sunset
			if (randomMinutes) {
                def rdmOffset = random.nextInt(randomMinutes)
                sunsetDate = new Date(sunsetDate.time - (randomMinutes * 30000) + (rdmOffset * 60000))
            }
            debug "light activation is not enabled during daytime; check again at sunset (${sunsetDate.format('dd MMM HH:mm:ss', tz)})" //TODO: if using illuminance, subscribe to the sensor and check again when dark
            state.scheduled.schedTurnOn = sunsetDate.time
            runOnce(sunsetDate, schedTurnOn)
        }
	}
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}	

def schedTurnOff(onDelayMS, offTime) { //determine turn-off time and schedule the turnOff()
    def mName = "schedTurnOff()"
    def startTime = now()
    def tz = location.timeZone
    state.lastInitiatedExecution = [time: startTime, name: mName]
    def offDate = offTime ? new Date(offTime) : null
    debug "executing schedTurnOff(onDelayMS: ${onDelayMS}, offDate: ${offDate?.format('dd MMM HH:mm:ss', tz)})", "trace", 1
    def random = new Random()

    if (onFor) { //re-calculate the turn-off time if a light-on duration was specified in the app preferences
    	int onForMS = onFor * 60 * 1000
        if (randomMinutes) {
            int rangeUser = randomMinutes * 60 * 1000 //convert user random window from minutes to ms
            int rangeMax = 2 * onForMS //the maximum random window is 2 * onForMS
            int range = rangeUser < rangeMax ? rangeUser : rangeMax //limit the random window to 2 * onForMS
            int rdmOffset = random.nextInt(range)
            onForMS = (int)(onForMS - range/2 + rdmOffset)
		}
        def endOnFor = new Date(now() + onForMS + onDelayMS)
        debug "calculated OFF time for turning the light off after the 'on for' delay of ${convertToHMS(onForMS)} : ${endOnFor.format('dd MMM HH:mm:ss', tz)}", "info"
        offDate = offDate && (offDate < endOnFor) ? offDate : endOnFor
    }
    
    if (offDate) {
	    def nowTime = now()
        offTime = offDate.time
        if (offTime > nowTime) {
            int offDelayS = (int)((offTime - nowTime)/1000)
            state.scheduled.turnOff = offTime
            debug "scheduling turn-off of the light to occur at ${offDate.format('dd MMM HH:mm:ss', tz)}", "info"
            runIn(offDelayS, turnOff)
        } else {
        	int maxDelay = 2 * 60 * 1000 //set a delay of up to 2 min to be applied when requested to turn off now
            int delayOffNow = random.nextInt(maxDelay)
            debug "the calculated turn-off time has already passed; calling for the light to turn off in ${convertToHMS(delayOffNow)}", "info"
            state.scheduled.turnOff = now() + delayOffNow
            turnOff(delayOffNow)
        }
    } else {
        debug "no turn-off time specified"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def turnOff(delayMS) {
    def mName = "turnOff()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
	debug "executing turnOff(delayMS: ${delayMS})", "trace", 1
    
    if (state.appOn == true || parent.debugAppOn == true) {
        delayMS = delayMS ?: C_MIN_DELAY()
        debug "turning off the light in ${convertToHMS(delayMS)}", "info"
        state.appOn = false
        state.switchTime.off = now()
        int timerStart = now()
        theLight.off([delay: delayMS])
        int timerEnd = now()
        debug "it took ${(timerEnd - timerStart)} ms to execute theLight.off([delay: ${delayMS}])", "debug"
        if (offFor) {
            int offForMS = offFor * 60 * 1000
            if (randomMinutes) {
                def random = new Random()
                int rangeUser = randomMinutes * 60 * 1000 //convert user random window from minutes to ms
                int rangeMax = 2 * offForMS //limit the random window to 2 * offForMS to ensure that offForMS remains > 0
                int range = rangeUser < rangeMax ? rangeUser : rangeMax
                int rdmOffset = random.nextInt(range)
                offForMS = (int)(offForMS - range/2 + rdmOffset)
            }
            state.scheduled.schedTurnOn = now()
            schedTurnOn(offForMS)
        } else {
            debug "the light isn't scheduled to turn back on today"
        }
    } else {
		debug "the light wasn't turned on by this app; doing nothing"
    }
    
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}

def terminate() { //For each configured light that was turned on by this app, turn the light off after a random delay. 
				  //Called when it's detected that the conditions are no longer valid.
    def mName = "terminate()"
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: mName]
	debug "executing ${mName}", "trace", 1
    debug "state.appOn: ${state.appOn}"
   	if (state.appOn == true || parent.debugAppOn == true) {
        def random = new Random()
        int offNowRandom = C_OFF_NOW_RANDOM()
        int maxDelay = offNowRandom * 60 * 1000
        int delayMS = random.nextInt(maxDelay)
        debug "turning off the light in ${convertToHMS(delayMS)}", "info"
        state.appOn = false
        state.switchTime.off = now()
        int timerStart = now()
        theLight.off([delay: delayMS])
        int timerEnd = now()
        debug "it took ${(timerEnd - timerStart)} ms to execute theLight.off([delay: ${delayMS}])", "debug"
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: mName, duration: elapsed]
    debug "${mName} completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getModeOk() {
	def result = theModes?.contains(location.mode)
	debug ">> modeOk : $result"
	return result
}

def getNowDOW() { //method to obtain current weekday adjusted for local time
    def javaDate = new java.text.SimpleDateFormat("EEEE, dd MMM yyyy @ HH:mm:ss")
    def javaDOW = new java.text.SimpleDateFormat("EEEE")
    if (location.timeZone) {
    	//debug "location.timeZone = true"
        javaDOW.setTimeZone(location.timeZone)
    } else {
        //debug "location.timeZone = false"
        //javaDate.setTimeZone(TimeZone.getTimeZone("America/Edmonton"))
    }
    def strDOW = javaDOW.format(new Date())
    debug ">> nowDOW : $strDOW"
    return strDOW
}

def getAppOk() { //used to determine if app has crashed
    //debug "start evaluating appOk()", "debug", 1
    def appOn = state.appOn //true = command was sent to turn light on
	def lightOn = theLight?.currentSwitch == "on"
    def result = !appOn || lightOn //check that if appOn, switch is also on
    //debug "appOn: ${appOn} - lightOn: ${lightOn}", "debug"
	if (appOn) { //check if light is scheduled to turn off
        def wantOff = onFor || userOffTime //check if light should be scheduled to turn off
        if (wantOff) {
            def turnOffTime = state.scheduled?.turnOff //time the light is scheduled to turn off
            result = turnOffTime > now() //check if light is scheduled to turn off in the future
        }
    }
    //debug "finished evaluating appOk()", "debug", -1
    return result
}


//   ------------------------
//   ***   COMMON UTILS   ***

def getItsDarkOut() { //implement use of illuminance capability
    def sunTime = getSunriseAndSunset(sunsetOffset: parent.sunsetOffset)
    def nowDate = new Date(now() + 2000) // be safe and set current time for 2 minutes later
    def result = false
    def desc = ""
	
    if(sunTime.sunrise < nowDate && sunTime.sunset > nowDate){
    	desc = "it's daytime"
        result = false
    } else {
    	desc = "it's nighttime"
        result = true
    }
    debug ">> itsDarkOut : $result ($desc)"
    return result
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
    return (!parent.noAppIcons || forceIcon) ? "$imgPath/$imgName" : ""
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
	
    def debugging = parent.debugging
	if (!debugging) {
		return
	}
    
    lvl = lvl ?: "debug"
	if (!parent["log#$lvl"]) {
		return
	}
	
    def multiEnable = (parent.setMultiLevelLog == false ? false : true) //set to true by default
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