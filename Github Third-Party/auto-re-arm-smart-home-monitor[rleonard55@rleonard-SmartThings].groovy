/**
 *  SHM Rearm
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
    name: "Auto Re-Arm Smart Home Monitor",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Arms the SHM after a disarm.",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home4-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@3x.png")

preferences {
	page(name: "page1")
	page(name: "page2")
}

def page1() {
	dynamicPage(name: "page1",
    title: "Settings", 
    nextPage: "page2", 
    install: false, 
    uninstall: true) {
        TimeSection()
        DayOfWeekSection()
 		section("Wait for ...") {
			input "contactSensors", "capability.contactSensor", title: "Contact Sensor?", multiple: true, required: false
        	input("presenceSensors", "capability.presenceSensor", title: "Use these presence sensors to determine security mode", multiple: true, required: false)
            input("arrivalSleepMin","number", 
				title: "Number of minutes to wait after disarm to rearm (>4)", 
        		range: "5..*",
        		defaultValue:5,
        		required:true)
        }
    }
}
def page2() {
	dynamicPage(name: "page2", title: "Additional Settings", install: true, uninstall: true) {
		LoggingSection()
        NameModeSection()
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
    
	initialize()
}
def initialize() {
	subscribe(location,"alarmSystemStatus",OnAlarmStatusChange)
}

def OnAlarmStatusChange(evt) {
    if(!PreCheck())
    	return
        
    if (evt.value == "off")  {
		logDebug ("Caught alarm status change: "+evt.value)
		logDebug ("Scheduleing security re-arm in ${arrivalSleepMin} min(s)")
        runIn(arrivalSleepMin*60, rearmSecurity)
		subscribe(contactSensors, "contact.closed", OnContactClosed)
	} else {
        unschedule()
        unsubscribe(OnContactClosed)
    }    
}
def OnContactClosed(evt){
    if(contactSensors.any {c-> 
    	c.currentValue("contact")=="open"
    }) return
    
	logDebug ("All doors are closed, sending rearm command in 10 seconds")

	unschedule()
	runIn(10, rearmSecurity)
}

def PreCheck() {
	logTrace "Entering 'PreCheck'"
    
    if(!emumContainsOrNull(daysToRun,new Date().format("EEEE"))) {
    	logDebug("Not correct day, skipping")
    	return false
    }

    if(!timeToRun()) {
    	logDebug("Not time to run, skipping")
        return false
    }

	logTrace "Exiting 'PreCheck'"
    return true
}

def rearmSecurity(){
	unsubscribe(OnContactClosed)
    def result = presenceSensors.any {p -> 
        p.currentValue("presence") == "present" }
    
    def action = "away"
    if(result) { 
    	action = "stay"
        logDebug("Identified presence, arming in home mode")
    } else {
    	logDebug("No presence identified, arming in away mode")
    }
    
    sendSHMEvent(action)
    //execRoutine(action)
}
private sendSHMEvent(String shmState){
	def event = [name:"alarmSystemStatus", value: shmState, 
    			displayed: true, description: "System Status is ${shmState}"]
    sendLocationEvent(event)
}
private execRoutine(armMode) {
	if (armMode == 'away') location.helloHome?.execute(settings.armRoutine)
    else if (armMode == 'stay') location.helloHome?.execute(settings.stayRoutine)
    else if (armMode == 'off') location.helloHome?.execute(settings.disarmRoutine)    
}

private getDebugOutputSetting() {
	return (settings?.debugOutput || settings?.debugOutput == null)
}
private getTraceOutputSetting() {
	return (settings?.traceOutput || settings?.traceOutput == null)
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

private dayToRun() {
	logTrace "Entering 'dayToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    logDebug("Today is ${(new Date().format("EEEE"))}")
    
    if(daysToRun == null) return true;
	return daysToRun.contains(new Date().format("EEEE"))
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'dayToRun'"
}
private timeToRun() {
	logTrace "Entering 'timeToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    if(startingX == null) return true
    
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
private emumContainsOrNull(enumToCheck, desiredValue) {
	logTrace "Entering 'dayToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    def returnValue = false
    
    logDebug "Desired Value: ${desiredValue} | Actual Value: ${enumToCheck}"
    if(enumToCheck == null || enumToCheck.contains(desiredValue))
		returnValue = true
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'dayToRun'"
    return returnValue
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