/**
 *  Test
 *
 *  Copyright © 2016 Phil Maynard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0												*/
 	       private urlApache() { return "http://www.apache.org/licenses/LICENSE-2.0" }			/*
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *	VERSION HISTORY										*/
 	 private versionNum() {	return "version 2.00" }
     private versionDate() { return "31-Oct-2016" }		/*
 *
 *	  v2.00 (dd-mmm-yyyy) - new feature
 *    v1.10 (dd-mmm-yyyy) - bug fix
 *						  - usage/function improvement
 *    v1.01 (dd-mmm-yyyy) - minor change, performance improvement
 *	  v1.00 (dd-mmm-yyyy) - initial release
 *    v0.10 (dd-mmm-yyyy) - in development
 *
 */
definition(
    name: "Test",
    namespace: "astrowings",
    author: "Phil Maynard",
    description: "Test",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png")


//   ---------------------------
//   ***   APP PREFERENCES   ***

preferences {
	page(name: "pageMain")
    page(name: "pageSettings")
    page(name: "pageLogOptions")
    page(name: "pageAbout")
    page(name: "pageUninstall")
}


//   --------------------------------
//   ***   CONSTANTS DEFINITIONS  ***

private		appImgPath()			{ return "https://raw.githubusercontent.com/astrowings/SmartThings/master/images/" }
private		readmeLink()			{ return "https://github.com/astrowings/SmartThings/blob/master/smartapps/astrowings/test.src/readme.md" }


//   -----------------------------
//   ***   PAGES DEFINITIONS   ***

//TODO: implement href state (exemple: https://github.com/SmartThingsCommunity/Code/blob/master/smartapps/preferences/page-params-by-href.groovy)
def pageMain() {
	dynamicPage(name: "pageMain", title: "Main", install: true, uninstall: false) { //with 'install: true', clicking 'Done' installs/updates the app
    	section(){
        	paragraph "", title: "This SmartApp is used for various tests."
        }
        section("Inputs") {
            /*input "theSwitches", "capability.switch",
            	title: "Select the switches",
                description: "This is a long input description to demonstrate that it will wrap over multiple lines",
                multiple: true,
                required: false,
                submitOnChange: false*/
            input "theDimmer", "capability.switchLevel", required: false
            input "theSwitch", "capability.switch", required: false, multiple: true
            input "theContact", "capability.contactSensor", required: false, multiple: true
        }
        section("Other test parameters") {
        	//input "myTime", "time", title: "What time?"
            //input "myRandom", "number", title: "Random minutes?"
        }
		section() {
            href "pageSettings", title: "App settings", description: "", image: getAppImg("configure_icon.png"), required: false
            href "pageAbout", title: "About", description: "", image: getAppImg("info-icn.png"), required: false
		}
    }
}
    
def pageSettings() {
	dynamicPage(name: "pageSettings", title: "Settings", install: false, uninstall: false) { //with 'install: false', clicking 'Done' goes back to previous page
   		section() {
			label title: "Assign a name", defaultValue: "${app.name}", required: false
            href "pageUninstall", title: "", description: "Uninstall this SmartApp", image: getAppImg("trash-circle-red-512.png"), state: null, required: true
		}
        section("Debugging Options", hideable: true, hidden: true) {
            input "noAppIcons", "bool", title: "Disable App Icons", description: "Do not display icons in the configuration pages", image: getAppImg("disable_icon.png"), defaultValue: false, required: false, submitOnChange: true
            href "pageLogOptions", title: "IDE Logging Options", description: "Adjust how logs are displayed in the SmartThings IDE", image: getAppImg("office8-icn.png"), required: true, state: "complete"
        }
    }
}

def pageAbout() {
	dynamicPage(name: "pageAbout", title: "About this SmartApp", install: false, uninstall: false) { //with 'install: false', clicking 'Done' goes back to previous page
		section() {
        	href url: readmeLink(), title: app.name, description: "Copyright ©2016 Phil Maynard\n${versionNum()}", image: getAppImg("readme-icn.png")
            href url: urlApache(), title: "License", description: "View Apache license", image: getAppImg("license-icn.png")
		}
   		section("Stats") {
            paragraph stateCap(), title: "Memory Usage"
		}
    }
}

def pageLogOptions() {
	dynamicPage(name: "pageLogOptions", title: "IDE Logging Options", install: false, uninstall: false) {
        section() {
	        input "debugging", "bool", title: "Enable debugging", description: "Display the logs in the IDE", defaultValue: false, required: false, submitOnChange: true 
        }
        if (debugging) {
            section("Select log types to display") {
                input "log#info", "bool", title: "Log info messages", defaultValue: true, required: false 
                input "log#trace", "bool", title: "Log trace messages", defaultValue: true, required: false 
                input "log#debug", "bool", title: "Log debug messages", defaultValue: true, required: false 
                input "log#warn", "bool", title: "Log warning messages", defaultValue: true, required: false 
                input "log#error", "bool", title: "Log error messages", defaultValue: true, required: false 
			}
            section() {
                input "setMultiLevelLog", "bool", title: "Enable Multi-level Logging", defaultValue: true, required: false,
                    description: "Multi-level logging prefixes log entries with special characters to visually " +
                        "represent the hierarchy of events and facilitate the interpretation of logs in the IDE"
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


//   ----------------------------
//   ***   APP INSTALLATION   ***

def installed() {
	debug "installed with settings: ${settings}", "trace", 0
	initialize()
}

def updated() {
    debug "updated with settings ${settings}", "trace", 0
	unsubscribe()
    //unschedule()
    initialize()
}

def uninstalled() {
    state.debugLevel = 0
    debug "application uninstalled", "trace", 0
}

def initialize() {
    state.debugLevel = 0
    debug "initializing", "trace", 1
    subscribeToEvents()
    debug "initialization complete", "trace", -1
    //testStart()
}

def subscribeToEvents() {
    debug "subscribing to events", "trace", 1
    subscribe(theContact, "contact", testEvent)
    subscribe(theSwitch, "switch", testDimmer)    
    debug "subscriptions complete", "trace", -1
}

//   --------------------------
//   ***   EVENT HANDLERS   ***

def testEvent(evt) {
    debug "testEvent event: ${evt.descriptionText}", "trace", 1
    debug "testEvent complete", "trace", -1
}

def testDimmer(evt) {
    debug "testDimmer event: ${evt.descriptionText}", "trace", 1
	if (evt.value == "off") {
		theDimmer.setLevel(30)
        theDimmer.setLevel(0)
    } else {
        theDimmer.setLevel(100)
    }
    debug "testDimmer complete", "trace", -1
}

def testProperties(evt) {
	debug "testProperties event: ${evt.descriptionText}", "trace", 1
    debug "eventProperties>data:${evt.data}"
    debug "eventProperties>description:${evt.description}"
    debug "eventProperties>descriptionText:${evt.descriptionText}"
    debug "eventProperties>device:${evt.device}"
    debug "eventProperties>displayName:${evt.displayName}"
    debug "eventProperties>deviceId:${evt.deviceId}"
    debug "eventProperties>installedSmartAppId:${evt.installedSmartAppId}"
    debug "eventProperties>name:${evt.name}"
    debug "eventProperties>source:${evt.source}"
    debug "eventProperties>stringValue:${evt.stringValue}"
    debug "eventProperties>unit:${evt.unit}"
    debug "eventProperties>value:${evt.value}"
    debug "eventProperties>isDigital:${evt.isDigital()}"
    debug "eventProperties>isPhysical:${evt.isPhysical()}"
    debug "eventProperties>isStateChange:${evt.isStateChange()}"
	debug "testProperties complete", "trace", -1
}


//   -------------------
//   ***   METHODS   ***

def testStart() {
	debug "executing testStart()", "trace", 1
	//runIn(30, toggleSwitches)
    deviceProperties()
	debug "testStart() complete", "trace", -1
}

def deviceProperties() {
	debug "testDevice?.displayName:${testDevice.displayName}"
    debug "testDevice?.label:${testDevice.label}"
}

def toggleSwitches() {
	debug "executing toggleSwitch()", "trace", 1
	if (theSwitches) {
    	theSwitches.each {
        	if (it.currentSwitch == "on") {
            	debug "turning off the ${it.name}"
                it.off()
            } else {
            	debug "turning on the ${it.label}"
                it.on()
            }
        }
    } else {
    	debug "no switches selected"
    }
    //runIn(30, toggleSwitches)
	debug "toggleSwitch() complete", "trace", -1
}

def listDevices() {
	debug "executing listDevices()", "trace", 1
    def switchQty = theSwitches ? theSwitches.size() : 0
    debug "switchQty:$switchQty"
    if (switchQty == 1) {
        debug "1 switch selected"
    } else if (switchQty > 1) {
        debug "${switchQty} switches selected"
    } else {
        debug "no switches selected"
    }
	debug "listDevices() complete", "trace", -1
}

private		C_1()						{ return "this is constant1" }
private		getC_2()					{ return "this is constant2" }
private		getSOME_CONSTANT()			{ return "this is some constant" }
private		getC_SOME_OTHER_CONSTANT()	{ return "this is some other constant" }
private		C_NEW_CONSTANT()			{ return "this is a new constant" }

def debugTest() {
	debug "executing debugTest()", "trace", 1
    debug "constant1 : ${C_1()}"//					-> this is constant1 
    debug "constant2a: ${C_2}"//					-> null
    debug "constant2b: ${c_2}"//					-> this is constant2 
    debug "constant3a: ${SOME_CONSTANT}"//			-> this is some constant
    debug "constant4a: ${c_SOME_OTHER_CONSTANT}"//	-> this is some other constant
    debug "constant4b: ${C_SOME_OTHER_CONSTANT}"//	-> null
    debug "constant5a: ${c_NEW_CONSTANT}"//			-> null
    debug "constant5b: ${C_NEW_CONSTANT}"//			-> null
	debug "debugTest() complete", "trace", -1
}

def randomTest() {
	debug "executing randomTest()", "trace", 1
   	debug "a random number between 4 and 16 could be: ${randomWithRange(4, 16)}"
    if (myTime) {
    	def myDate = timeToday(myTime, location.timeZone)
        def randMinutes = myRandom ?: 0
        def randMS = randMinutes * 60000
        def msgMath = "a random (by using Math class: Math.random() ) date could be: ${mathDate(myDate,randMinutes)}"
        def msgRandom1 = "a random (using Random class: new Random()) date could be: ${randomDate_rangeMinutes(myDate,randMinutes)}"
        def msgRandom2 = "another (using Random class: new Random()) date could be: ${randomDate_rangeMS(myDate,randMS)}"
        debug msgMath
        debug msgRandom1
        debug msgRandom2
    }
	debug "randomTest() complete", "trace", -1
}


//   ------------------------
//   ***   COMMON UTILS   ***

/*
 ** see below for idiot-proof version of this **
 
        int randomWithRange(int min, int max)
        {
           //Math.random() returns a random floating point number between 0 (inclusive) and 1 (exclusive)
           int range = (max - min) + 1
           return (int)(Math.random() * range) + min
        }
        
 *
 */

int randomWithRange(int min, int max) {
	int range = Math.abs(max - min) + 1
	return (int)(Math.random() * range) + (min <= max ? min : max)
}

def mathDate(baseDate, int rangeMinutes) {
	long minTime = baseDate.time - (rangeMinutes * 30000)
	long range = rangeMinutes * 60000
	long randomTime = minTime + (long)(Math.random() * range)
	return new Date(randomTime)
}

def randomDate_rangeMinutes(baseDate, int rangeMinutes) {
	def r = new Random()
    long minTime = baseDate.time - (rangeMinutes * 30000)
    int range = rangeMinutes * 60000
    long randomTime = minTime + r.nextInt(range + 1)
    return new Date(randomTime)
}

def randomDate_rangeMS(baseDate, int rangeMS) {
	def r = new Random()
    long randomTime = baseDate.time - (rangeMS/2) + r.nextInt(rangeMS + 1)
    return new Date(randomTime)
}

def stateCap(showBytes = true) {
	def bytes = state.toString().length()
	return Math.round(100.00 * (bytes/ 100000.00)) + "%${showBytes ? " ($bytes bytes)" : ""}"
}

def cpu() {
	if (state.lastExecutionTime == null) {
		return "N/A"
	} else {
		def cpu = Math.round(state.lastExecutionTime / 20000)
		if (cpu > 100) {
			cpu = 100
		}
		return "$cpu%"
	}
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

    if (lvl == "info") {
    	def leftPad = (multiEnable ? ": :" : "")
        log.info "$leftPad$prefix$message", err
	} else if (lvl == "trace") {
    	def leftPad = (multiEnable ? "::" : "")
        log.trace "$leftPad$prefix$message", err
	} else if (lvl == "warn") {
    	def leftPad = (multiEnable ? "::" : "")
		log.warn "$leftPad$prefix$message", err
	} else if (lvl == "error") {
    	def leftPad = (multiEnable ? "::" : "")
		log.error "$leftPad$prefix$message", err
	} else {
		log.debug "$prefix$message", err
	}
}


//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//
//   *******************   TEST ZONE  ********************   //
//   Put new code here before moving up into main sections   //
//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\//