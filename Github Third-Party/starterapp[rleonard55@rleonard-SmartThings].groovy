/**
 *  StarterApp
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
    name: "StarterApp",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "This is a SmartApp Templete",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "page1")
    page(name: "page2")
}
def page1() {
	dynamicPage(name: "page1", title: "Settings", nextPage: "page2", install: false, uninstall: true) {
    	section("Choose One...", hideWhenEmpty: true) {
        	 input "switches",title: "Switch" , "capability.switch", hideWhenEmpty: true, multiple: true
        }
		//HtmlSection("https://google.com")
        ///...
    }
}
def page2() {
	dynamicPage(name: "page2", title: "Additional Settings", install: true, uninstall: true) {
		
        //TimeSection()
        //DayOfWeekSection()
        //MonthSection()
        //OncePerDaySection()
        
        //NameModeSection()
        //LoggingSection()
        //NotificationSection()
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
	
    unscheduleIt()
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

def PreCheck() {
	logTrace "Entering 'PreCheck'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
	
    if(oncePerDay && ranToday()) {
    	logDebug("Already ran today, skipping")
    	return false
    }
    
    if(!emumContainsOrNull(monthsToRun,new Date().format("MMM"))) {
    	logDebug("Not correct month, skipping")
    	return False
    }

    if(!emumContainsOrNull(daysToRun,new Date().format("EEEE"))) {
    	logDebug("Not correct day, skipping")
    	return False
    }

    if(!timeToRun()) {
    	logDebug("Not time to run, skipping")
        return false
    }

	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'PreCheck'"
    return true
}
def DoSomething(evt) {
	logTrace "Entering 'DoSomething'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    if(PreCheck()) {
    	logDebug ("*** Did it ***")
    	state.LastRan = new Date()
    }
	
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'DoSomething'"
}



private scheduleIt() {
	logTrace "Entering 'scheduleIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    //runEvery1Minute()
    //runEvery5Minutes()
    //runEvery10Minutes()
    //runEvery15Minutes()
    //runEvery30Minutes()
    
    //runEvery1Hour()
    //runEvery3Hours()
    
    
    //runIn(5, scheduleHandler, [data: [flag:true]])
    
	logTrace "Exiting 'scheduleIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
}
private unscheduleIt() {
	logTrace "Entering 'unscheduleIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    // unschedule(scheduleHandler)
    unschedule()
    
    logTrace "Entering 'unscheduleIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
}
def scheduleHandler(data) {
    if (data.flag) {
        switches.off()
    }
}

private subscribeIt() {
	logTrace "Entering 'subscribeIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼

	//subscribe(switches, "switch.on", subscribeHandler)
	//subscribe(switches, "switch.off", subscribeHandler)
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'subscribeIt'"
}
private unsubscribeIt() {
	logTrace "Entering 'unsubscribeIt'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
	
    //unsubscribe(subscribeHandler)
    //unsubscribe(switches)
    unsubscribe()
    
    //▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'unsubscribeIt'"
}
def subscribeHandler(evt) {
	logTrace "Entering 'subscribeHandler'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    //logDebug("switch(s) is ${evt.value}")
    //scheduleIt()
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'subscribeHandler'"
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

private monthToRun() {
	logTrace "Entering 'monthToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    logDebug("This is ${new Date().format("MMM")}")
    if(monthsToRun == null) return true;
	return monthsToRun.contains(new Date().format("MMM"))
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	logTrace "Exiting 'monthToRun'"
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
	if(state.LastRan == null) return false
	def midnightToday = timeToday("00:00", TimeZone.getTimeZone('UTC'))
    def last = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.LastRan)
    if(last > midnightToday)
    	return true
    return false
}

private sendNotifications()
{
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