/**
 *  Vac-O-Matic (Samsung SmartVac)
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
    name: "Vac-O-Matic (Samsung SmartVac)",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Vacuums the floor while you are away.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/samsung/da/RC_ic_rc.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/samsung/da/RC_ic_rc@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/samsung/da/RC_ic_rc@3x.png")


preferences {
	page(name: "page1")
    page(name: "page2")
}
def page1() {
	dynamicPage(name: "page1", title: "Settings", nextPage: "page2", install: false, uninstall: true) {
		PresenceSection()
        VacuumSection()
		
        OncePerDaySection()
		//MonthSection()
        DayOfWeekSection()
		TimeSection()
    }
}
def page2() {
	dynamicPage(name: "page2", title: "Additional Settings", install: true, uninstall: true) {
        //NotificationSection()
        section() {
        	input "enabled", "bool", title: "Enable Vac-O-Matic", defaultValue: true
			input "dustbinReminder", "bool", title: "Remind me to empty the vacuum's dustbin", submitOnChange: true, defaultValue: true
            if(dustbinReminder)
            	input "remindOnce", "bool", title: "Only remind on first arrival"
            input "stopOnArrive", "bool", title: "Stop vacuum(s) on arrival", defaultValue: false
            input "waitMin", "number", title: "Minutes to wait after departure",range: "0..60", required: true, defaultValue: 15
        }
		NameModeSection()
		LoggingSection()
    }
}

// ** Common Sections **
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
	section("Logging", hideable:true, hidden:true) {
             input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: DebugOutputSetting, displayDuringSetup: true, required: false
             input "traceOutput", "bool", title: "Enable trace logging?", defaultValue: TraeOutputSetting, displayDuringSetup: true, required: false
        }
}
def PresenceSection(all = true) {
	if(all) {
    	section("When everyone departs..."){
            input "presenceSensors", "capability.presenceSensor", title: "Presence Sensor(s)", multiple: true, required: true
        }
    } else {
		section("When anyone departs..."){
            input "presenceSensors", "capability.presenceSensor", title: "Presence Sensor(s)", multiple: true, required: true
        }
    }
    
}
def VacuumSection(prompt="Start the Vacuum(s)", itemRequired = true, multiple = false) {
    section(prompt) {
    	//input "myVacuums", "device.samsungRobotVacuum", title: "Vacuum(s)", multiple: multiple, required: itemRequired
        input "myVacuums", "capability.timedSession", title: "Vacuum(s)", multiple: multiple, required: itemRequired
    }
}
def NotificationSection(prompt= "Send Notifications?", itemRequired = false) {
	section(title: prompt) {
        input("recipients", "contact", title: "Send notifications to", required: itemRequired) {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: itemRequired
        }
    }
}
def HtmlSection(url, prompt= "Click here to open", desctiption=null, itemRequired= false) {
    if(description == null)
    {
    	section() {
            href(title: prompt,
                 description: "Opens ${url}",
                 required: itemRequired,  
                 style: "page",
                // image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                 url: url)
        }
    }
    else
    {
        section() {
            href(title: prompt,
                 description: desctiption,
                 required: itemRequired,
                 style: "page",
                // image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                 url: url)
        }
	}
}

def installed() {
	logTrace "Entering 'Installed'"
	logDebug "Installed with settings: ${settings}"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
	initialize()
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲    
    logTrace "Exiting 'Installed'"
}
def updated() {
	logTrace "Entering 'updated'"
	logDebug "Updated with settings: ${settings}"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    unschedule()
	unsubscribeIt()
    
	initialize()

	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲    
    logTrace "Exiting 'updated'"
}
def initialize() {
	logTrace "Entering 'initialize'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    subscribeIt()
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'initialize'"
}

private subscribeIt() {
	logTrace "Entering 'subscribeIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
	
    subscribe(presenceSensors, "presence.present", onPresenceArrive)
	subscribe(presenceSensors, "presence.not present", onPresenceDepart)

	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'subscribeIt'"
}
private unsubscribeIt() {
	logTrace "Entering 'unsubscribeIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
	
    unsubscribe()
    
    //▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'unsubscribeIt'"
}

def onPresenceDepart(evt) {
	logTrace "Entering 'onPresenceDepart'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼

    if(!preCheck())
    	logDebug("preCheck Failed, Skipping")
    else
    {
        runIn(((waitMin*60)+1), doSomething)
    }
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'OnPresenceDepart'"
}
def onPresenceArrive(evt) {
	logTrace "Entering 'onPresenceArrive'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼

    unschedule()
    unsubscribeIt()
    
    logDebug("Vacuums: ${myVacuums}")
    //def result = myVacuums.any { v -> v.currentValue("operationState")=="cleaning" }
    //myVacuums.each{log.debug it.currentValue("operationState")}
    
    if(stopOnArrive)// && result)
    	myVacuums.cancel()

	//log.debug "result: ${result}"
    log.debug "dustbinReminder: ${dustbinReminder}"
    log.debug "RanToday(): ${ranToday()}"
    
    

	if(dustbinReminder && ranToday())
        if(remindOnce) {
            if(state.reminder== false) {
                sendPush("Remember to empty the vacuum's dustbin!") 
                state.reminder=true
			}
		} else {
        	sendPush("Remember to empty the vacuum's dustbin!")
            state.reminder=true
        }

    subscribeIt()   
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'onPresenceArrive'"
}

//def onSwitchedOn(evt) {	
//	logTrace "Entering 'onSwitchedOn'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼

//    log.debug "Vacuum changed to 'On'"
//	state.LastRan = now()
    
    //▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
//	logTrace "Exiting 'onSwitchedOn'"
//}

def preCheck() {
	
    logDebug("Starting Prechecks")
    if(!enabled()) {
    	logDebug "Disabled, skipping"
        return false
    }
    logDebug "EnabledOk = True"
    
	if(anyonePresent()) {
    	logDebug("Others are still present, skipping")
        return false
    }	
    logDebug("PresenceOk = True")
    
    if(oncePerDay && ranToday()) {
    	logDebug("Already ran today, skipping")
    	 return false
    }
    logDebug("RanTodayOk = True")
    
    if(!emumContainsOrNull(monthsToRun,(new Date()).format("MMM", location.timeZone))) {
    	logDebug("Not correct month, skipping")
    	 return false
    }
	logDebug("MonthOk = True")

    if(!emumContainsOrNull(daysToRun,(new Date()).format("EEEE", location.timeZone))) {
    	logDebug("Not correct day, skipping")
    	 return false
    }
	logDebug("DayOk = True")
     
    if(!timeToRun()) {
    	 logDebug("Not time to run, skipping")
         return false
    }
	logDebug("TimeOk = True")
	logDebug("preCheck looks good")
    return true
}
def doSomething() {
	logTrace "Entering 'doSomething'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
	if(preCheck()) {
		myVacuums.start()
        
        // Would like to find event to subscrive to set on "Start"...
        state.LastRan = now()
        state.reminder=false
        
		logDebug("starting")
	}
	else
	{
		log.Debug("Skiping due to precheck failure")
	}
	
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'doSomething'"
}

private enabled(){
	return settings.enabled
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
private ranToday() {

	def theEvents = myVacuums.eventsSince(timeToday("00:00", location.timeZone))
    //theEvents.each{e-> log.debug "name: ${e.name} | value ${e.value}"}
    
    if(theEvents.any{e->e.value == "start"})
    	return true
    return false
    


//	if(state.LastRan == null) 
//    	return false
//	if(state.LastRan > midnight())
//    	return true
//    return false
}
private anyonePresent() {
	return presenceSensors.any {p -> p.currentValue("presence") == "present" }
}
private emumContainsOrNull(enumToCheck, desiredValue) {
	logTrace "Entering 'dayToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    def returnValue = false
    
    logTrace "Desired Value: ${desiredValue} | Actual Value: ${enumToCheck}"
    if(enumToCheck == null || enumToCheck.contains(desiredValue))
		returnValue = true
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'dayToRun'"
    return returnValue
}

private sendNotifications() {
	// display a message in the Notifications feed
	//sendNotificationEvent("Your home talks!")
    
    // sends a push notification, and displays it in the Notifications feed
    // sendNotification("test notification - no params")

    // same as above, but explicitly specifies the push method (default is push)
    // sendNotification("test notification - push", [method: "push"])

    // sends an SMS notification, and displays it in the Notifications feed
    // sendNotification("test notification - sms", [method: "phone", phone: "1234567890"])

    // Sends a push and SMS message, and displays it in the Notifications feed
    // sendNotification("test notification - both", [method: "both", phone: "1234567890"])

    // Sends a push message, and does not display it in the Notifications feed
    // sendNotification("test notification - no event", [event: false])
}
private safeToInt(val, defaultVal=0) {
	return "${val}"?.isInteger() ? "${val}".toInteger() : defaultVal
}
private safeToDec(val, defaultVal=0) {
	return "${val}"?.isBigDecimal() ? "${val}".toBigDecimal() : defaultVal
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

private midnight() {
	return timeToday("00:00", location.timeZone).time
}