/**
 *  Yard Lights
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0												*/
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
 *    v1.12 (03-Jan-2021) - update iconURL's broken links
 *    v1.11 (26-Nov-2019) - fix state.debugLevel by moving the reset to the start of initialization method
 *    v1.10 (18-Nov-2019) - implement feature to display latest log entries in the 'debugging tools' section
 *                        - calculate method completion time before declaring complete so that time may be displayed in the completion debug line
 *	  v1.02 (22 Nov 2018) - in timeDimGo(), removed the check to see if the switch was already on
 *						    because it would sometimes cause the temporary dim setting to not be 
 *							applied when called immediately upon turn on where datTimeDimFrom < datNow
 *                        - process handlers only if state.lightsOn
 *    v1.01 (09 Oct 2018) - fix application of 'doorDimDelayFixed'
 *    v1.00 (10 Aug 2018) - functional release
 *    v0.10 (30 Jul 2018) - developing
 *
*/
definition(
    name: "Yard Lights",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Turn on/off selected lights and dim based on various conditions.",
    category: "Convenience",
    //iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn.png",
    //iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@2x.png",
    //iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light9-icn@3x.png")
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@3x.png")


//   --------------------------------
//   ***   APP DATA  ***

def		versionNum()			{ return "version 1.12" }
def		versionDate()			{ return "03-Jan-2021" }     
def		gitAppName()			{ return "yard-lights" }
def		gitOwner()				{ return "astrowings" }
def		gitRepo()				{ return "SmartThings" }
def		gitBranch()				{ return "master" }
def		gitAppFolder()			{ return "smartapps/${gitOwner()}/${gitAppName()}.src" }
def		appImgPath()			{ return "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/images/" }
def		readmeLink()			{ return "https://github.com/${gitOwner()}/SmartThings/blob/master/${gitAppFolder()}/readme.md" } //TODO: convert to httpGet?
//def	changeLog()				{ return getWebData([uri: "https://raw.githubusercontent.com/${gitOwner()}/${gitRepo()}/${gitBranch()}/${gitAppFolder()}/changelog.txt", contentType: "text/plain; charset=UTF-8"], "changelog") }


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

	 	//name					value					description
def	    C_ON_DELAY_S()          { return 5 }            //random window used to calculate delay between each light's turn-on (minutes) | TODO: convert to user preference
def	    C_OFF_DELAY_S()         { return 5 }            //random window used to calculate delay between each light's turn-off (minutes) | TODO: convert to user preference
def		C_SUNRISE_OFFSET()		{ return -30 }			//offset used for sunrise time calculation (minutes)
def		C_MIN_TIME_ON()			{ return 15 }			//value to use when scheduling turnOn to make sure lights will remain on for at least this long (minutes) before the scheduled turn-off time
def		C_MIN_DIM_DURATION()	{ return 5 }			//minimum duration for temporary dim event (seconds)


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageOn")
    page(name: "pageOff")
    page(name: "pageDim")
    page(name: "pageSettings")
    page(name: "pageLogOptions")
    page(name: "pageAbout")
    page(name: "pageUninstall")
}


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

def pageMain() {
    dynamicPage(name: "pageMain", install: true, uninstall: false) {
    	section("I wrote this SmartApp to control the lights around my shed in the backyard; I wanted " +
        	    "the lights to turn on and off automatically, similarly to what my 'Sunset Lights' " +
                "application does, but I also wanted to be able to program certain conditions to adjust " +
                "the brightness so as not to inconvenience neighbours, while still getting good lighting when needed.\n\n" +
                "This SmartApp turns on selected lights at sunset and turns them off at a specified time. " +
            	"Different on/off times can be configured for each day of the week, and they can be " +
                "randomized within a specified window to simulate manual operation. This SmartApp also supports " +
                "dimmers and can be configured to adjust the brightness of selected lights based on various conditions."){
        }
        section() {
            input "theDimmers", "capability.switchLevel", title: "Which dimmers?", description: "Choose the lights to control", multiple: true, required: true, submitOnChange: true
            if (theDimmers) {
                href "pageOn", title: "Set turn-on conditions", description: onConfigDesc, image: getAppImg("office6-icn.png"), required: true, state: "complete"
                href "pageOff", title: "Set turn-off conditions", description: offConfigDesc, image: getAppImg("office6-icn.png"), required: true, state: "complete"
                href "pageDim", title: "Configure brightness settings", description: dimConfigDesc, image: getAppImg("light11-icn.png"), required: true, state: "complete"
        	}
        }
		section() {
			if (theDimmers) {
	            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            }
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
		}
    }
}

def pageOn() {
    dynamicPage(name: "pageOn", install: false, uninstall: false) {
        section(){
        	paragraph title: "Turn-on Conditions", "Use the options on this page to determine when the selected light(s) will turn on."
        }
        //TODO: use illuminance-capable device instead of sunrise/sunset to detect darkness
        section("Set the initial brightness setting for when the lights get turned on (optional - defaults to 100% if not set).") {
        	input "onDimLvl", "number", title: "Initial brightness", description: "Brightness level (0 - 100)?", range: "0..100", required: false, defaultValue: 100
        }
        section("Set the amount of time before/after sunset when the lights will turn on " +
        		"(e.g. use '-20' to enable lights 20 minutes before sunset; " +
                "optional - lights will turn on at sunset if not set") {
            input "onSunsetOffset", "number", title: "Sunset offset", description: "How many minutes (+/- 60)?", range: "-60..60", required: false
        }
    	section("Turn the lights on at a given time. This setting optional and, if set, overrides the sunset setting above.") {
        	input "onDefaultTime", "time", title: "ON time", description: "Time to turn lights ON?", required: false
        }
        section("You can also specify a different time to turn the lights on for each day of the week. " +
	            "Again, this is optional and, if set, overrides all settings above for that particular day.") {
        	input "onSunday", "time", title: "Sunday", description: "Lights ON time?", required: false
            input "onMonday", "time", title: "Monday", description: "Lights ON time?", required: false
            input "onTuesday", "time", title: "Tuesday", description: "Lights ON time?", required: false
            input "onWednesday", "time", title: "Wednesday", description: "Lights ON time?", required: false
            input "onThursday", "time", title: "Thursday", description: "Lights ON time?", required: false
            input "onFriday", "time", title: "Friday", description: "Lights ON time?", required: false
            input "onSaturday", "time", title: "Saturday", description: "Lights ON time?", required: false
        }
    	section("Finally, you can add a random factor so that the timing varies slightly from one day to another " +
    	        "(it looks more 'human' that way).\nSpecify a window around the scheduled time when the lights will turn on " +
      			"(e.g. a 30-minute window would have the lights switch on sometime between " +
           		"15 minutes before and 15 minutes after the scheduled time.)") {
            input "onRand", "number", title: "Random ON window (minutes)?", description: "Set random window", required: false, defaultValue: 30
        }
	}
}

def pageOff() {
    dynamicPage(name: "pageOff", install: false, uninstall: false) {
        def sunriseOffset = C_SUNRISE_OFFSET()
        def sunriseOffset_minutes = sunriseOffset.abs()
        def sunriseOffset_BeforeAfter = sunriseOffset < 0 ? "before" : "after"
        section(){
        	paragraph title: "Turn-off Conditions", "Use the options on this page to choose when the selected light(s) will turn off."
        }
        //TODO: use illuminance-capable device instead of sunrise/sunset to detect darkness
    	section("Turn the light(s) off at a given time " +
        		"(optional - light(s) will turn off ${sunriseOffset_minutes} minutes ${sunriseOffset_BeforeAfter} next sunrise if no time is entered).") {
        	input "offDefaultTime", "time", title: "OFF time", description: "Time to turn lights OFF?", required: false
        }
    	section("Set a different time to turn off the lights on each day (optional - lights will turn off at the default time if not set).") {
        	input "offSunday", "time", title: "Sunday", description: "Lights OFF time?", required: false
            input "offMonday", "time", title: "Monday", description: "Lights OFF time?", required: false
            input "offTuesday", "time", title: "Tuesday", description: "Lights OFF time?", required: false
            input "offWednesday", "time", title: "Wednesday", description: "Lights OFF time?", required: false
            input "offThursday", "time", title: "Thursday", description: "Lights OFF time?", required: false
            input "offFriday", "time", title: "Friday", description: "Lights OFF time?", required: false
            input "offSaturday", "time", title: "Saturday", description: "Lights OFF time?", required: false
        }
    	section("Finally, you can add a random factor so that the timing varies slightly from one day to another " +
    	        "(it looks more 'human' that way).\nSpecify a window around the scheduled time when the lights will turn off " +
      			"(e.g. a 30-minute window would have the lights switch off sometime between " +
           		"15 minutes before and 15 minutes after the scheduled time.)") {
            input "offRand", "number", title: "Random OFF window (minutes)?", description: "Set random window", required: false, defaultValue: 30
        }
	}
}

def pageDim() {
	dynamicPage(name: "pageDim", install: false, uninstall: false) {
        def minDimDuration = C_MIN_DIM_DURATION()
    	section(){
        	paragraph title: "Brightness Adjustments", "Use the options on this page to set the conditions that will affect brightness level. " +
            	"The various settings are listed in increasing priority order (i.e. the brightness setting based on motion will be applied even if outside the time window)."
        }
        section("Adjust brightness during a specified time window. If only 'From' time is set, brightness setting will apply " +
                "until scheduled turn-off time. Conversely, if only the 'To' time is set, brightness setting will apply from " +
                "turn-on time until the 'To' time, at which point it will revert to the default turn-on brightness. You can " +
                "also chose to apply a random on/off window to these settings (e.g. a 10-minute window would have the brightness " +
                "adjustment occur sometime between 5 minutes before and 5 minutes after the scheduled time.)") {
        	input "timeDimLvl", "number", title: "Brightness during time window", description: "Brightness level (0 - 100)?", range: "0..100", required: false, defaultValue: 100
            input "timeDimFrom", "time", title: "From", description: "Starting when?", required: false
            input "timeDimTo", "time", title: "To", description: "Until when?", required: false
            input "timeDimRand", "number", title: "Random window (minutes)?", description: "Set random window", required: false, defaultValue: 10
        }
        section("Adjust brightness when a door is open.") {
            input "doorDimLvl", "number", title: "Brightness when door open", description: "Brightness level (0 - 100)?", range: "0..100", required: false, defaultValue: 100
            input "doorDimSensors", "capability.contactSensor", title: "Open/Close Sensors", description: "Select which door(s)", multiple: true, required: false
            input "doorDimDelayAfterClose", "number", title: "Reset brightness x seconds after doors close", description: "Seconds (0 - 300)?", range: "0..300", required: false, defaultValue: 5
            input "doorDimDelayFixed", "number", title: "Reset brightness after fixed delay (overrides previous setting)", description: "Seconds (${minDimDuration} - 300)?", range: "${minDimDuration}..300", required: false
        }
        section("Adjust brightness when motion is detected.") {
        	input "motionDimLvl", "number", title: "Brightness when motion detected", description: "Brightness level (0 - 100)?", range: "0..100", required: false, defaultValue: 100
            input "motionDimSensors", "capability.motionSensor", title: "Motion Sensors", description: "Select which sensor(s)", multiple: true, required: false
            input "motionDimDelayAfterStop", "number", title: "Reset brightness x seconds after motion stops", description: "Seconds (0 - 300)?", range: "0..300", required: false, defaultValue: 5
            input "motionDimDelayFixed", "number", title: "Reset brightness after fixed delay (overrides previous setting)", description: "Seconds (${minDimDuration} - 300)?", range: "${minDimDuration}..300", required: false
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
            //leave next line commented; embedded changelog not implemented
            //paragraph title: "What's new...", image: getAppImg("whats_new_icon.png"), changeLog()
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

def getOnConfigDesc() {
    def strDefaultOn = onDefaultTime?.substring(11,16)
    def strSundayOn = onSunday?.substring(11,16)
    def strMondayOn = onMonday?.substring(11,16)
    def strTuesdayOn = onTuesday?.substring(11,16)
    def strWednesdayOn = onWednesday?.substring(11,16)
    def strThursdayOn = onThursday?.substring(11,16)
    def strFridayOn = onFriday?.substring(11,16)
    def strSaturdayOn = onSaturday?.substring(11,16)
    def onTimeOk = onDefaultTime || onSunday || onMonday || onTuesday || onWednesday || onThursday || onFriday || onSaturday
    def strDesc = ""
        strDesc += onDimLvl        ? " • Initial brightness: ${onDimLvl} %\n" : ""
        strDesc += onSunsetOffset  ? " • Sunset offset: ${onSunsetOffset} minutes\n" : ""
        strDesc += onTimeOk        ? " • Turn-on time:" : " • Turn-on time not specified;\n    lights will come on at sunset."
        strDesc += onDefaultTime   ? " ${strDefaultOn}\n" : "\n"
        strDesc += onSunday	       ? "   └ sunday: ${strSundayOn}\n" : ""
        strDesc += onMonday	       ? "   └ monday: ${strMondayOn}\n" : ""
        strDesc += onTuesday       ? "   └ tuesday: ${strTuesdayOn}\n" : ""
        strDesc += onWednesday     ? "   └ wednesday: ${strWednesdayOn}\n" : ""
        strDesc += onThursday      ? "   └ thursday: ${strThursdayOn}\n" : ""
        strDesc += onFriday        ? "   └ friday: ${strFridayOn}\n" : ""
        strDesc += onSaturday      ? "   └ saturday: ${strSaturdayOn}\n" : ""
        strDesc += onRand          ? " • Random window:\n    +/-${onRand/2} minutes" : ""
    return strDesc
}

def getOffConfigDesc() {
    def sunriseOffset = C_SUNRISE_OFFSET()
    def strDefaultOff = offDefaultTime?.substring(11,16)
    def strSundayOff = offSunday?.substring(11,16)
    def strMondayOff = offMonday?.substring(11,16)
    def strTuesdayOff = offTuesday?.substring(11,16)
    def strWednesdayOff = offWednesday?.substring(11,16)
    def strThursdayOff = offThursday?.substring(11,16)
    def strFridayOff = offFriday?.substring(11,16)
    def strSaturdayOff = offSaturday?.substring(11,16)
    def offTimeOk = offDefaultTime || offSunday || offMonday || offTuesday || offWednesday || offThursday || offFriday || offSaturday
    def strDesc = ""
        strDesc += offTimeOk        ? " • Turn-off time:" : " • Turn-off time not specified; lights will turn off ${sunriseOffset.abs()} minutes before sunrise."
        strDesc += offDefaultTime   ? " ${strDefaultOff}\n" : "\n"
        strDesc += offSunday        ? "   └ sunday: ${strSundayOff}\n" : ""
        strDesc += offMonday        ? "   └ monday: ${strMondayOff}\n" : ""
        strDesc += offTuesday       ? "   └ tuesday: ${strTuesdayOff}\n" : ""
        strDesc += offWednesday     ? "   └ wednesday: ${strWednesdayOff}\n" : ""
        strDesc += offThursday      ? "   └ thursday: ${strThursdayOff}\n" : ""
        strDesc += offFriday        ? "   └ friday: ${strFridayOff}\n" : ""
        strDesc += offSaturday      ? "   └ saturday: ${strSaturdayOff}\n" : ""
        strDesc += offRand          ? " • Random window:\n    +/-${onRand/2} minutes" : ""
    return strDesc
}

def getDimConfigDesc() {
    def timeDimSet = (timeDimLvl && (timeDimFrom || timeDimTo))
    def doorDimSet = (doorDimLvl && doorDimSensors && (doorDimDelayAfterClose || doorDimDelayFixed))
    def motionDimSet = (motionDimLvl && motionDimSensors && (motionDimDelayAfterStop || motionDimDelayFixed))
    def strTimeDimFrom = timeDimFrom?.substring(11,16)
    def strTimeDimTo = timeDimTo?.substring(11,16)
    def strDesc = ""
    	strDesc += timeDimSet                                                        ? " • On time:\n" : ""
        strDesc += timeDimSet                                                        ? "   └ dim level: ${timeDimLvl}%\n" : ""
        strDesc += (timeDimSet && timeDimFrom)                                       ? "   └ from: ${strTimeDimFrom}\n" : ""
        strDesc += (timeDimSet && timeDimTo)                                         ? "   └ until: ${strTimeDimTo}\n" : ""
        strDesc += (timeDimSet && timeDimRand)                                       ? "   └ random: ${timeDimRand} minutes\n" : ""
        strDesc += doorDimSet                                                        ? " • On door open:\n" : ""
        strDesc += doorDimSet                                                        ? "   └ contacts: ${doorDimSensors?.size()} selected\n" : ""
        strDesc += doorDimSet                                                        ? "   └ dim level: ${doorDimLvl}%\n" : ""
        strDesc += (doorDimSet && doorDimDelayAfterClose && !doorDimDelayFixed)      ? "   └ until ${doorDimDelayAfterClose} seconds\n         after contacts close\n" : ""
        strDesc += (doorDimSet && doorDimDelayFixed)                                 ? "   └ for ${doorDimDelayFixed} seconds\n" : ""
        strDesc += motionDimSet                                                      ? " • On motion:\n" : ""
        strDesc += motionDimSet                                                      ? "   └ sensors: ${motionDimSensors?.size()} selected\n" : ""
        strDesc += motionDimSet                                                      ? "   └ dim level: ${motionDimLvl}%\n" : ""
        strDesc += (motionDimSet && motionDimDelayAfterStop && !motionDimDelayFixed) ? "   └ until ${motionDimDelayAfterStop} seconds\n         after motion stops" : ""
        strDesc += (motionDimSet && motionDimDelayFixed)                             ? "   └ for ${motionDimDelayFixed} seconds" : ""
    return strDesc
}

def appInfo() {
	def tz = location.timeZone
    def mapSun = getSunriseAndSunset()
    def debugLevel = state.debugLevel
    def lightsOn = state.lightsOn
    def lightsOnTime = state.lightsOnTime
    def timeDimActive = state.timeDimActive
    def datInstall = state.installTime ? new Date(state.installTime) : null
    def datInitialize = state.initializeTime ? new Date(state.initializeTime) : null
    def datSchedOn = state.schedOnTime ? new Date(state.schedOnTime) : null
    def datSchedOff = state.schedOffTime ? new Date(state.schedOffTime) : null
	def datTimeDimFrom = state.timeDimFrom ? new Date(state.timeDimFrom) : null
	def datTimeDimTo = state.timeDimTo ? new Date(state.timeDimTo) : null
    def lastInitiatedExecution = state.lastInitiatedExecution
    def lastCompletedExecution = state.lastCompletedExecution
    def debugLog = state.debugLogInfo
    def numLogs = debugLog?.size()
	def strInfo = ""
        strInfo += " • Application state:\n"
        strInfo += datInstall ? "  └ last install date: ${datInstall.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datInitialize ? "  └ last initialize date: ${datInitialize.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += "\n • Last scheduled jobs:\n"
        strInfo += datSchedOn ? "  └ schedOnTime: ${datSchedOn.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datSchedOff ? "  └ schedOffTime: ${datSchedOff.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datTimeDimFrom ? "  └ timeDimFrom: ${datTimeDimFrom.format('dd MMM YYYY HH:mm', tz)}\n" : ""
        strInfo += datTimeDimTo ? "  └ timeDimTo: ${datTimeDimTo.format('dd MMM YYYY HH:mm', tz)}\n" : ""
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
        strInfo += "  └ timeDimActive: ${timeDimActive}\n"
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
    state.timeDimActive = false
    state.schedOffTime = 0L
    state.dimTime = 0L
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
	//subscribe(app, appTouch)
	if (doorDimSensors && doorDimLvl && (doorDimDelayAfterClose || doorDimDelayFixed)) {
		subscribe(doorDimSensors, "contact", doorHandler)   //adjust brightness when door opens
    }
    if (motionDimSensors && motionDimLvl && (motionDimDelayAfterStop || motionDimDelayFixed)) {
    	subscribe(motionDimSensors, "motion", motionHandler)//adjust brightness when motion is detected
    }
    subscribe(location, "sunsetTime", sunsetTimeHandler)	//triggers at sunset, evt.value is the sunset String (time for next day's sunset)
    subscribe(location, "sunriseTime", sunriseTimeHandler)	//triggers at sunrise, evt.value is the sunrise String (time for next day's sunrise)
    subscribe(location, "position", locationPositionChange) //update settings if hub location changes
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "subscribeToEvents()", duration: elapsed]
    debug "subscriptions completed in ${elapsed} seconds", "trace", -1
}


//   --------------------------
//   ***   EVENT HANDLERS   ***

def appTouch(evt) {
    def startTime = now()
	state.lastInitiatedExecution = [time: startTime, name: "appTouch()"]
	debug "appTouch event: ${evt.descriptionText}", "trace", 1
    initialize()
    turnOn()
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "appTouch()", duration: elapsed]
    debug "appTouch completed in ${elapsed} seconds", "trace", -11
}

def doorHandler(evt) {
    if (!state.lightsOn) {
    	return
    }
    
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "doorHandler()"]
	debug "doorHandler event: ${evt.descriptionText}", "trace", 1 //TODO: 'descriptionText' only displays '{{linkText}}'?
    debug "doorHandler event raw description: ${evt.description}"
    debug "doorHandler event description text: ${evt.descriptionText}"
    debug "doorHandler event display name: ${evt.displayName}"
    debug "doorHandler event source: ${evt.source}"
    debug "doorHandler event value: ${evt.value}"
    debug "doorHandler event string value: ${evt.stringValue}"
    if (evt.value == "open") {
    	doorOpen()
    } else if ((evt.value == "closed") && doorDimDelayAfterClose) {
    	doorClosed()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "doorHandler()", duration: elapsed]
    debug "doorHandler completed in ${elapsed} seconds", "trace", -1
}

def motionHandler(evt) {
    if (!state.lightsOn) {
    	return
    }
    
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "motionHandler()"]
	debug "motionHandler event: ${evt.descriptionText}", "trace", 1 //TODO: 'descriptionText' only displays '{{linkText}}'?
    if (evt.value == "active") {
    	motionActive()
    } else if ((evt.value == "inactive") && motionDimDelayAfterStop) {
    	motionInactive()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "motionHandler()", duration: elapsed]
    debug "motionHandler completed in ${elapsed} seconds", "trace", -1
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

def scheduleTurnOn(sunsetString) { //schedule next day's turn-on
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "scheduleTurnOn()"]
    debug "executing scheduleTurnOn(sunsetString: ${sunsetString})", "trace", 1
    def datOnDOW = weekdayTurnOnTime
    def datOnDefault = defaultTurnOnTime
    def datOn

    //select which turn-on time to use (1st priority: weekday-specific, 2nd: default, 3rd: sunset)
    if (datOnDOW) {
    	debug "using the weekday turn-on time: ${datOnDOW}"
        datOn = datOnDOW
    } else if (datOnDefault) {
    	debug "using the default turn-on time: ${datOnDefault}"
    	datOn = datOnDefault
    } else {
    	debug "user didn't specify turn-on time; using sunset time"
        def datSunset = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
        //calculate the offset
        def offsetTurnOn = onSunsetOffset ? onSunsetOffset * 60 * 1000 : 0 //convert offset to ms
        datOn = new Date(datSunset.time + offsetTurnOn)
	}
    
    //apply random factor
    if (onRand) {
        debug "Applying random factor to the turn-on time"
        def random = new Random()
        def randOffset = random.nextInt(onRand)
        datOn = new Date(datOn.time - (onRand * 30000) + (randOffset * 60000)) //subtract half the random window (converted to ms) then add the random factor (converted to ms)
	}
	
	state.schedOnTime = datOn.time
    debug "scheduling lights ON for: ${datOn}", "info"
    runOnce(datOn, turnOn, [overwrite: false]) //schedule this to run once (it will trigger again at next sunset)
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "scheduleTurnOn()", duration: elapsed]
    debug "scheduleTurnOn() completed in ${elapsed} seconds", "trace", -1
}

def scheduleTurnOff(sunriseString) { //schedule next day's turn-off
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
    def onDelayS = C_ON_DELAY_S()
    def minTimeOn = C_MIN_TIME_ON()
    def onTime = now() + (minTimeOn * 60 * 1000) //making sure lights will stay on for at least 'minTimeOn'
    def offTime = state.schedOffTime //retrieving the turn-off time from State
    if (offTime < onTime) {
		debug "scheduled turn-off time has already passed; turn-on cancelled"
	} else {
        debug "turning lights on"
        def nextDelayMS = 0L //set-up the nextDelayMS variable that will be used to calculate a new time to turn on each light in the group
        def onDelayMS = onDelayS ? onDelayS * 1000 : 5 //ensure onDelayMS != 0
        def random = new Random()
        theDimmers.each { theDimmer ->
            if (theDimmer.currentSwitch != "on") {
				debug "turning on the ${theDimmer.label} at ${onDimLvl}% brightness in ${convertToHMS(nextDelayMS)}", "info"
                theDimmer.setLevel(onDimLvl, [delay: nextDelayMS])
                nextDelayMS += random.nextInt(onDelayMS) //calculate random delay before turning on next light
            } else {
            	debug "the ${theDimmer.label} is already on; doing nothing"
            }
        }
        state.lightsOn = true
        state.lightsOnTime = now()
       	if (timeDimLvl && (timeDimFrom || timeDimTo)) {
        	schedTimeDim() //schedule temporary brightness adjustment if configured
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
    def offDelayS = C_OFF_DELAY_S()
    def nextDelayMS = 0L //set-up the nextDelayMS variable that will be used to calculate a new time to turn off each light in the group
    def offDelayMS = offDelayS ? offDelayS * 1000 : 5 //ensure delayMS != 0
    def random = new Random()
    theDimmers.each { theDimmer ->
        if (theDimmer.currentSwitch != "off") {
            debug "turning off the ${theDimmer.label} in ${convertToHMS(nextDelayMS)}", "info"
            theDimmer.off(delay: nextDelayMS)
            nextDelayMS += random.nextInt(offDelayMS) //calculate random delay before turning off next light
        } else {
            debug "the ${theDimmer.label} is already off; doing nothing"
        }
    }
    state.lightsOn = false
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "turnOff()", duration: elapsed]
    debug "turnOff() completed in ${elapsed} seconds", "trace", -1
}

def schedTimeDim() {
	//called from turnOn()
    //to schedule the user-defined temporary brightness setting

    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "schedTimeDim()"]
    debug "executing schedDimStart()", "trace", 1
    def minDimDuration = C_MIN_DIM_DURATION()
    def nowTime = now() + (minDimDuration * 60 * 1000) //making sure lights will stay on for at least 'minDimDuration'
    def datNow = new Date(nowTime)
    def datTimeDimFrom = timeDimFrom ? timeToday(timeDimFrom, location.timeZone) : null
    def datTimeDimTo = timeDimTo ? timeToday(timeDimTo, location.timeZone) : null
    if (datTimeDimFrom && timeDimRand) {
    	//apply random factor to 'timeDimFrom'
        debug "Applying random factor to the 'dim from' time"
        def random = new Random()
        def randOffset = random.nextInt(timeDimRand)
        datTimeDimFrom = new Date(datTimeDimFrom.time - (timeDimRand * 30000) + (randOffset * 60000)) //subtract half the random window (converted to ms) then add the random factor (converted to ms)
        state.timeDimFrom = datTimeDimFrom.time
    }
    if (datTimeDimTo && timeDimRand) {
    	//apply random factor to 'timeDimTo'
        debug "Applying random factor to the 'dim to' time"
        def random = new Random()
        def randOffset = random.nextInt(timeDimRand)
        datTimeDimTo = new Date(datTimeDimTo.time - (timeDimRand * 30000) + (randOffset * 60000)) //subtract half the random window (converted to ms) then add the random factor (converted to ms)
        state.timeDimTo = datTimeDimTo.time
    }
    debug "schedTimeDim() :: datTimeDimFrom: ${datTimeDimFrom}"
    debug "schedTimeDim() :: datTimeDimTo: ${datTimeDimTo}"
    def timeOk = datNow < datTimeDimTo //check that the scheduled end of the user-defined temporary brightness window hasn't passed yet
    if (!timeOk) {
    	debug "the scheduled end of the user-defined temporary brightness window has passed; doing nothing"
	    def elapsed = (now() - startTime)/1000
    	state.lastCompletedExecution = [time: now(), name: "schedTimeDim()", duration: elapsed]
        debug "schedDimStart() completed in ${elapsed} seconds", "trace", -1
        return
    }
    if (!timeDimFrom) {
        debug "dim start time not specified; applying temporary brightness setting now", "info"
        timeDimGo()
    } else if (datTimeDimFrom < datNow) {
        debug "dim start time already passed; applying temporary brightness setting now", "info"
        timeDimGo()
    } else {
    	debug "scheduling temporary brightness setting to occur at ${datTimeDimFrom}", "info"
        runOnce(datTimeDimFrom, timeDimGo)
    }
    if (timeDimTo) {
        debug "scheduling temporary brightness setting to end at ${datTimeDimTo}", "info"
        runOnce(datTimeDimTo, dimDefault)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "schedTimeDim()", duration: elapsed]
    debug "schedDimStart() completed in ${elapsed} seconds", "trace", -1
}

def timeDimGo() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "timeDimGo()"]
	debug "executing timeDimGo()", "trace", 1
    if (!state.lightsOn) {
    	debug "state is not active; skipping timeDimGo()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "timeDimGo()", duration: elapsed]
	    debug "timeDimGo() completed in ${elapsed} seconds", "trace", -1
        return
    }
    theDimmers.each { theDimmer ->
	    //if (theDimmer.currentSwitch != "off") { this might cause the temporary dim setting to not be applied when called immediately upon turn on where datTimeDimFrom < datNow
        	debug "temporarily setting ${theDimmer.label} to ${timeDimLvl}%", "info"
            theDimmer.setLevel(timeDimLvl)
        //}
    }
    state.timeDimActive = true
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "timeDimGo()", duration: elapsed]
    debug "timeDimGo() completed in ${elapsed} seconds", "trace", -1
}

def dimDefault() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "dimDefault()"]
    debug "executing dimDefault()", "trace", 1
    if (!state.lightsOn) {
    	debug "state is not active; skipping dimDefault()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "dimDefault()", duration: elapsed]
	    debug "dimDefault() completed in ${elapsed} seconds", "trace", -1
        return
    }
    theDimmers.each { theDimmer ->
	    if (theDimmer.currentSwitch != "off") {
        	debug "end of timed brightness adjustment;re-setting ${theDimmer.label} to ${onDimLvl}%", "info"
            theDimmer.setLevel(onDimLvl)
        } else {
        	debug "the ${theDimmer.label} is off; doing nothing"
        }
    }
    state.timeDimActive = false
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "dimDefault()", duration: elapsed]
    debug "dimDefault() completed in ${elapsed} seconds", "trace", -1
}

def dimReset() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "dimReset()"]
    debug "executing dimReset()", "trace", 1
    if (!state.lightsOn) {
    	debug "state is not active; skipping dimDefault()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "dimReset()", duration: elapsed]
	    debug "dimReset() completed in ${elapsed} seconds", "trace", -1
        return
    }
    if (state.timeDimActive) {
    	debug "the triggered brightness period has ended; calling to restore the timed brightness adjustment", "info"
        timeDimGo()
    } else {
    	debug "the triggered brightness period has ended; calling to restore the default brightness adjustment", "info"
    	dimDefault()
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "dimReset()", duration: elapsed]
    debug "dimReset() completed in ${elapsed} seconds", "trace", -1
}

def motionActive() {
    //called from motionHandler when motion is active
    //to apply temporary brightness setting
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "motionActive()"]
    debug "executing motionActive()", "trace", 1 //TODO: specify which sensor detected the motion
    if (!state.lightsOn) {
    	debug "state is not active; skipping motionActive()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "motionActive()", duration: elapsed]
	    debug "motionActive() completed in ${elapsed} seconds", "trace", -1
        return
    }
    state.dimTime = now() //store current time to use later in ensuring MIN_DIM_DURATION()
    theDimmers.each { theDimmer ->
	    if (theDimmer.currentSwitch != "off") {
        	debug "temporarily setting ${theDimmer.label} to ${motionDimLvl}% because motion was detected", "info"
            theDimmer.setLevel(motionDimLvl)
        } else {
        	debug "the ${theDimmer.label} is off; doing nothing"
        }
    }
    if (motionDimDelayFixed) {
    	debug "calling to reset brightness in ${motionDimDelayFixed} seconds", "info"
        def minDimDuration = C_MIN_DIM_DURATION()
        def dimResetDelay = (motionDimDelayFixed < minDimDuration) ? minDimDuration : motionDimDelayFixed
       	runIn(dimResetDelay, dimReset)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "motionActive()", duration: elapsed]
    debug "motionActive() completed in ${elapsed} seconds", "trace", -1
}

def motionInactive() {
    //called from motionHandler when motion stops
    //to reset brightness to default setting after 'motionDimDelayAfterClose'
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "motionInactive()"]
    debug "executing motionInactive()", "trace", 1
    if (!state.lightsOn) {
    	debug "state is not active; skipping motionInactive()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "motionInactive()", duration: elapsed]
	    debug "motionInactive() completed in ${elapsed} seconds", "trace", -1
        return
    }
    def allInactive = true
    for (sensor in motionDimSensors) {
    	if (sensor.motion == "active") {
        	allInactive = false
            debug "the ${sensor.label} is still active; check again when motion stops"
            break
        }
    }
    if (allInactive) {
        def minDimDuration = C_MIN_DIM_DURATION()
        def dimTimeOn = state.dimTime
        def nowTime = now()
        def dimDuration = (nowTime - dimTimeOn)/1000
        def dimResetDelay = (motionDimDelayAfterStop + dimDuration) < minDimDuration ? minDimDuration : motionDimDelayAfterStop
    	debug "calling to reset brightness in ${dimResetDelay} seconds", "info"
        runIn(dimResetDelay, dimReset)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "motionInactive()", duration: elapsed]
    debug "motionInactive() completed in ${elapsed} seconds", "trace", -1
}

def doorOpen() {
    //called from doorHandler when a contact is open
    //to apply temporary brightness setting
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "doorOpen()"]
    debug "executing doorOpen()", "trace", 1 //TODO: specify which contact called this
    if (!state.lightsOn) {
    	debug "state is not active; skipping doorOpen()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "doorOpen()", duration: elapsed]
	    debug "doorOpen() completed in ${elapsed} seconds", "trace", -1
        return
    }
    state.dimTime = now() //store current time to use in doorClosed() for ensuring MIN_DIM_DURATION()
    theDimmers.each { theDimmer ->
	    if (theDimmer.currentSwitch != "off") {
        	debug "temporarily setting ${theDimmer.label} to ${doorDimLvl}% because a door was open", "info"
            theDimmer.setLevel(doorDimLvl)
        } else {
        	debug "the ${theDimmer.label} is off; doing nothing"
        }
    }
    if (doorDimDelayFixed) {
        def minDimDuration = C_MIN_DIM_DURATION()
        def dimResetDelay = (doorDimDelayFixed < minDimDuration) ? minDimDuration : doorDimDelayFixed
    	debug "calling to reset brightness to default setting in ${dimResetDelay} seconds", "info"
        runIn(dimResetDelay, dimReset)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "doorOpen()", duration: elapsed]
    debug "doorOpen() completed in ${elapsed} seconds", "trace", -1
}

def doorClosed() {
    //called from doorHandler when a contact is closed
    //to reset brightness to default setting after 'doorDimDelayAfterClose'
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "doorClosed()"]
    debug "executing doorClosed()", "trace", 1
    if (!state.lightsOn) {
    	debug "state is not active; skipping doorClosed()"
        def elapsed = (now() - startTime)/1000
        state.lastCompletedExecution = [time: now(), name: "doorClosed()", duration: elapsed]
	    debug "doorClosed() completed in ${elapsed} seconds", "trace", -1
        return
    }
    def allClosed = true
    for (door in doorDimSensors) {
    	if (door.contact == "open") {
        	allClosed = false
            debug "the ${door.label} is still open; check again next time a door closes"
            break
        }
    }
    if (allClosed) {
        def minDimDuration = C_MIN_DIM_DURATION()
        def dimTimeOn = state.dimTime
        def nowTime = now()
        def dimDuration = (nowTime - dimTimeOn)/1000
        def dimResetDelay = (doorDimDelayAfterClose + dimDuration) < minDimDuration ? minDimDuration : doorDimDelayAfterClose
    	debug "calling to reset brightness to default setting in ${dimResetDelay} seconds", "info"
        runIn(dimResetDelay, dimReset)
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "doorClosed()", duration: elapsed]
    debug "doorClosed() completed in ${elapsed} seconds", "trace", -1
}


//   -------------------------
//   ***   APP FUNCTIONS   ***

def getDefaultTurnOnTime() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getDefaultTurnOnTime()"]
	debug "start evaluating defaultTurnOnTime", "trace", 1
    def result
    if (onDefaultTime) {
        def onDate = timeTodayAfter("23:59", onDefaultTime, location.timeZone) //convert preset time to tomorrow's date
       	debug "default turn-on time: $onDate"
        result = onDate
    } else {
        debug "default turn-on time not specified"
        result = false
	}
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "getDefaultTurnOnTime()", duration: elapsed]
   	debug "finished evaluating defaultTurnOnTime in ${elapsed} seconds", "trace", -1
    return result
}

def getDefaultTurnOffTime() {
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getDefaultTurnOffTime()"]
	debug "start evaluating defaultTurnOffTime", "trace", 1
    def result
    if (offDefaultTime) {
        def offDate = timeTodayAfter(new Date(), offDefaultTime, location.timeZone) //convert preset time to today's date
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

def getWeekdayTurnOnTime() {
    //calculate weekday-specific on-time
    //this executes at sunset (called from scheduleTurnOn),
    //so when the sun sets on Tuesday, it will
    //schedule the lights' turn-on time for Wednesday
    def startTime = now()
    state.lastInitiatedExecution = [time: startTime, name: "getWeekdayTurnOnTime()"]
	debug "start evaluating weekdayTurnOnTime", "trace", 1
	def result
    def nowDOW = new Date().format("E") //find out current day of week

    //find out the preset (if entered) turn-on time for next day
    def onDOWtime
    if (onSunday && nowDOW == "Sat") {
        onDOWtime = onSunday
    } else if (onMonday && nowDOW == "Sun") {
        onDOWtime = onMonday
    } else if (onTuesday && nowDOW == "Mon") {
        onDOWtime = onTuesday
    } else if (onWednesday && nowDOW == "Tue") {
        onDOWtime = onWednesday
    } else if (onThursday && nowDOW == "Wed") {
        onDOWtime = onThursday
    } else if (onFriday && nowDOW == "Thu") {
        onDOWtime = onFriday
    } else if (onSaturday && nowDOW == "Fri") {
        onDOWtime = onSaturday
    }

	if (onDOWtime) {
    	def onDOWdate = timeTodayAfter("23:59", onDOWtime, location.timeZone) //set for tomorrow
      	debug "DOW turn-on time: $onDOWdate"
        result = onDOWdate
    } else {
    	debug "DOW turn-on time not specified"
        result = false
    }
    def elapsed = (now() - startTime)/1000
    state.lastCompletedExecution = [time: now(), name: "getWeekdayTurnOnTime()", duration: elapsed]
    debug "finished evaluating weekdayTurnOnTime in ${elapsed} seconds", "trace", -1
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
    if (offSunday && nowDOW == "Sun") {
        offDOWtime = offSunday
    } else if (offMonday && nowDOW == "Mon") {
        offDOWtime = offMonday
    } else if (offTuesday && nowDOW == "Tue") {
        offDOWtime = offTuesday
    } else if (offWednesday && nowDOW == "Wed") {
        offDOWtime = offWednesday
    } else if (offThursday && nowDOW == "Thu") {
        offDOWtime = offThursday
    } else if (offFriday && nowDOW == "Fri") {
        offDOWtime = offFriday
    } else if (offSaturday && nowDOW == "Sat") {
        offDOWtime = offSaturday
    }

	if (offDOWtime) {
    	def offDOWdate = timeTodayAfter(new Date(), offDOWtime, location.timeZone)
       	debug "DOW turn-off time: $offDOWdate"
        result = offDOWdate
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