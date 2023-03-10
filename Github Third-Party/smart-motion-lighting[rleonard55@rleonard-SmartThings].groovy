/**
 *  Smart Motion Lighting
 *
 *  Copyright 2017 Rob Leonard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Smart Motion Lighting",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Motion Lighting ",
    category: "",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn@2x.png")


preferences {
    page(name: "page1")
    page(name: "page2")
}

def page1() {
	dynamicPage(name: "page1", title: "Settings", nextPage: "page2", install: false, uninstall: true) {
    	section("Turn On these switches", hideWhenEmpty: true) {
            input "switches", "capability.switch", multiple: true, submitOnChange: true, required:true
            if(switches.any{it.hasCommand('setLevel')}) {
            	input "level", "number",title: "Set to this level", range: "1..100", defaultValue: 30
            }
            input "ifOffOrLower", "bool", title: "Only if switch is off or lower", defaultValue:true
            input "motions", "capability.motionSensor", title: "When there is motion here...", multiple:true
            
            input "lightSensor","capability.illuminanceMeasurement",required:false,title: "When this is light sensor...",submitOnChange: true
            if(settings?.lightSensor != null){
            	input "lightLevel", "number", title: "Below this LUX level", range: "0..*", defaultValue: 70
            }
            input "turnOffAfterMotion", "bool", title: "Turn off / restore level when motion ends", defaultValue: false,submitOnChange: true
            if(turnOffAfterMotion) {
            	input "turnOffMin", "number", title: "After x minute(s)", range: "0..*", defaultValue: 15
            }
            //input "slowTurnOff", "bool", title: "slowly turn down / off", defaultValue: false
        }
    }
}
def page2() {
	dynamicPage(name: "page2", title: "Additional Settings", install: true, uninstall: true) {
        TimeSection()
        DayOfWeekSection()
        //MonthSection()
        //OncePerDaySection()
        section(""){
        	input "enabled", "bool", title:"Enabled", defaultValue:true
        }
        NameModeSection()
        LoggingSection()
        //NotificationSection()
    }
}

def TimeSection(prompt= "Only between these times", itemRequired = false) {
    section() {
        input "startingX", "enum", title: prompt, options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: itemRequired
        if(startingX == "A specific time") 
        input "starting", "time", title: "Start time", required: true
        else if(startingX == "Sunrise") 
            input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
        else if(startingX == "Sunset") 
            input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
        if(startingX != null) {
            input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: true
            if(endingX == "A specific time") 
            input "ending", "time", title: "End time", required: true
            else if(endingX == "Sunrise") 
                input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
            else if (endingX == "Sunset") 
                input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0 
        }
    }
}
def DayOfWeekSection(prompt= "Only on these day(s)", itemRequired = false, multiple =true) {
	section() {
    	input "daysToRun", "enum", title: prompt, options: ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday", "Saturday"], submitOnChange: true, required: itemRequired, multiple: multiple
	}
}
def MonthSection(prompt= "Only during these month(s)", itemRequired = false, multiple =true) {
	 section() {
     	input "monthsToRun", "enum", title: prompt, options: ["Jan","Feb","Mar","Apr","May","Jun", "Jul", "Aug", "Sep","Oct", "Nov", "Dec"], submitOnChange: true, required: itemRequired, multiple: multiple
	}
}
def OncePerDaySection(prompt = "Only once per day", initalValue = false) {
	 section() {
     	input "oncePerDay", "bool", title: prompt, defaultValue: initalValue
     }
}
def NameModeSection() {
    section([mobileOnly:true]) {
        label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
    }
}
def LoggingSection() {
	section("Logging",hideWhenEmpty:true) {
             input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: DebugOutputSetting, displayDuringSetup: true, required: false
             input "traceOutput", "bool", title: "Enable trace logging?", defaultValue: TraeOutputSetting, displayDuringSetup: true, required: false
        }
}
def NotificationSection(prompt= "Send Notifications?", itemRequired = false) {
	section(title: prompt) {
        input("recipients", "contact", title: "Send notifications to", required: itemRequired) {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: itemRequired
        }
    }
}

def installed() {
	logDebug "Installed with settings: ${settings}"

	initialize()
}
def updated() {
	logDebug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
    
	initialize()
}
def initialize() {
	subscribe(motions, "motion.active", onMotion)
}

def onMotion(evt) {
	
    if(!enabled) {
    	logDebug "Disabled, skipping"
        return
    }
    	
    unschedule(turnOffRestore)
    switches.each {
    	state.(it.id) = null
    }
    
    def timeOk = timeToRun()
    logDebug "TimeOk =${timeOk}"
	if(timeOk== false && luxLevelToRun() == false) return
    
    def dayOk = dayToRun()
    logDebug "DayOk =${dayOk}"
    if(!dayOk) return
    
    switches.each {
        if(ifOffOrLower)
        {
        	if(it.currentSwitch  == "off") {
                if(it.hasCommand('setLevel') == false ){
                    logDebug "1) Turning switch ${it.displayName} ON"
                    it.on()
                }
                else {
                	 logDebug "Setting ${it.displayName} to ${settings.level}%"
                	 it.setLevel(settings.level)
                     //it.on()
                }
        	} else if(it.hasCommand('setLevel') && settings.level > it.currentValue("level")) {
				state.(it.id) = it.currentValue("level")
                logDebug "${it.displayName} is at ${state.(it.id)}%"
            	logDebug "Raising ${it.displayName} to ${settings.level}%"
                it.setLevel(settings.level)
                //it.on()
        	}
        }
        else
        {
            if(it.hasCommand('setLevel')) {
                it.setLevel(settings.level)
                logDebug "Setting ${it.displayName} level to ${settings.level}"
            }
            else {
            	logDebug "2) Turning switch ${it.displayName} ON"
                it.on()
            }
        }
    }
    
    if(turnOffAfterMotion) {
    	logDebug "Waiting for motion to stop"
    	subscribe(motions, "motion.inactive", onMotionStop)
    }
    
    subscribe(switches, "switch.off",onTurnOff)
    subscribe(switches, "switch.level",onLevelChange)
}
def onMotionStop(evt){
	logDebug "Motion stopped scheduleing off/restore"
	
    unsubscribe(onTurnOff)
    
    if(turnOffAfterMotion)
		runIn((60*turnOffMin)+1,turnOffRestore)
    
    unsubscribe(onMotionStop)
}
def turnOffRestore(){
	logDebug "Turning off/restoring switches"
    switches.each{
    	if (state?.(it.id)!= null && state?.(it.id)>0) {
        	logDebug "Restoreing ${it.displayName} to ${state.(it.id)}%"
			it.setLevel(state.(it.id))
        }
		else it.off()
    }
    
    unsubscribe(onTurnOff)
    unsubscribe(onLevelChange)
}

def scheduleSlowTurnDown(){
	logDebug "Scheduleing Slow Turn Down"
    
}
def slowTurnDownStep(){
	
}

def onTurnOff(evt){
	logDebug "Manual turn off detected, killing scheduled actions"
	unsubscribe(onMotionStop)
    unsubscribe(onTurnOff)
    unsubscribe(onLevelChange)
    unschedule()
}
def onLevelChange(evt){
	logDebug "Manual level change detected, killing scheduled actions"
	unsubscribe(onMotionStop)
    unsubscribe(onTurnOff)
    unsubscribe(onLevelChange)
    unschedule()
}

private luxLevelToRun(){
    logTrace "Entering 'luxLevelToRun'"
    logDebug "Lights {"+ settings?.lightSensor+ "} are at {"+settings?.lightSensor.currentValue("illuminance") +"}"

    def run = false

    if(settings?.lightSensor !=null) {
        if(settings?.lightSensor.currentValue("illuminance") <= settings.lightLevel) {
			logDebug "Illuminance is low enough to run lights"
            run = true
        }
    }
    else           
        run = false

    logTrace "Exiting 'luxLevelToRun'"
    return run
}

private timeToRun() {
	logTrace "Entering 'timeToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
	def start = null
	def stop = null
    
    if(startingX =="A specific time" && starting!= null)
    	start = timeToday(starting,location.timeZone)
    if(endingX == "A specific time" && ending!= null)
        stop = timeToday(ending,location.timeZone)
        
    if(startingX == "Sunrise")
    	start = s.sunrise
     if(startingX == "Sunset")
    	start = s.sunset
     if(endingX == "Sunrise")  
      	stop = s.sunrise
     if(endingX == "Sunset")
     	stop = s.sunset
	
    if(start == null || stop == null)
    	return true
    
     if(stop < start) 
     	stop = stop + 1
    
    logDebug ("start: ${start} | stop: ${stop}")
    //▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'timeToRun'"
    return timeOfDayIsBetween(start, stop, (new Date()), location.timeZone)
}
private dayToRun() {
	logTrace "Entering 'dayToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    logDebug("Today is ${(new Date().format("EEEE"))}")
    
    if(daysToRun == null) return true;
	return daysToRun.contains(new Date().format("EEEE"))
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'dayToRun'"
}

private logDebug(msg) {
	if (debugOutputSetting) {
		log.debug "$msg"
	}
}
private logTrace(msg) {
	if (traceOutputSetting) {
	 log.trace "$msg"
	}
}
private getDebugOutputSetting() {
	return (settings?.debugOutput || settings?.debugOutput == null)
}
private getTraceOutputSetting() {
	return (settings?.traceOutput || settings?.traceOutput == null)
}