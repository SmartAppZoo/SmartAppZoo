/**
 *  Sunrise/Sunset
 *
 *  Copyright 2016 Philippe Gravel
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
    name: "Sunrise/Sunset",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Set mode from Sunset and Sunrise",
    category: "Family",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png"
)

preferences {
	page(name: "selectTime")
    page(name: "selectThings")
}

def selectTime() {
	dynamicPage(name: "selectTime", title: "First, select time", nextPage: "selectThings", uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
        }

        section("Time to set morning event") {
        	input "weekDayTime", "time", title: "Week day"
            input "weekEndTime", "time", title: "Week end"
        }
        
        section("Minutes after sunset to preset windows") { 
        	input "minuteAfterSunset", "number", title: "Minutes after sunset"
        }
        
        section("Time to set night event") {
			input "nightTime", "time", title: "Night Time"
		}

    	section("Send Notifications?") {
        	input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)
    	}
//        section("Test Sunset/Sunrise") {
//        	input "testSwitch", "capability.switch", title: "Switchs?", multiple: false, required: false
//        }
    }
}

def selectThings() {
	dynamicPage(name: "selectThings", title: "Set the things", install: true, uninstall: true) {    
		section() {
            input "portelock", "capability.lock", title: "Porte Avant", required: true
            input "shades", "capability.windowShade", title: "Fenetres", required: true
		}    
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

	subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)

	def timeZone = location.timeZone
	def currentTime = timeToday(weekDayTime, timeZone)
	def timeWeekDayHour = currentTime.format("H", timeZone)
	def timeWeekDayMinute = currentTime.format("m", timeZone)
    log.debug "Week day: $timeWeekDayHour:$timeWeekDayMinute"
	schedule("0 $timeWeekDayMinute $timeWeekDayHour ? * MON-FRI *", weekDayMorningSetupHandler)
    
	currentTime = timeToday(weekEndTime, timeZone)
 	def timeWeekEndHour = currentTime.format('H', timeZone)
    def timeWeekEndMinute = currentTime.format('m', timeZone)
 	log.debug "Week End: $timeWeekEndHour:$timeWeekEndMinute"
    schedule("0 $timeWeekEndMinute $timeWeekEndHour ? * SAT,SUN *", weekEndMorningSetupHandler)

	currentTime = timeToday(nightTime, timeZone)
 	def timeNightHour = currentTime.format('H', timeZone)
    def timeNightMinute = currentTime.format('m', timeZone)
 	log.debug "Night Time: $timeNightHour:$timeNightMinute"
    schedule("0 $timeNightMinute $timeNightHour ? * * *", nightSetupHandler)

    sendNotificationToContacts("Week Day at $timeWeekDayHour:$timeWeekDayMinute\nWeek End at $timeWeekEndHour:$timeWeekEndMinute\nNight at $timeNightHour:$timeNightMinute", recipients)

//    subscribe(testSwitch, "switch.on", onTestHandler)
//    subscribe(testSwitch, "switch.off", offTestHandler)
}

def notAway() {
	def currMode = location.mode // "Home", "Away", etc.
    log.debug "Not Away - current mode is $currMode"

	return (currMode != "Away")
}

def sunsetHandler(evt) {
    log.debug "Sun has set!"
    sendNotificationEvent("Sun has set!")
    
    def messages = "Sun has set!"

    if (notAway()) {
	    setLocationMode("Evening")
        messages = messages + "\n- Set Mode Evening"
	}        
    
 	def timeZone = location.timeZone
	def nightScheduleTime = timeToday(nightTime, timeZone)
       
    def sunTime = new Date()
    def timeAfterSunset = new Date(sunTime.time + (minuteAfterSunset * 60 * 1000))
    log.debug "Sunset is at $sunTime. Sunset + $minuteAfterSunset = $timeAfterSunset. Schedule time is $nightScheduleTime"
    messages = messages + "\nSunset is at $sunTime. Sunset + $minuteAfterSunset = $timeAfterSunset.\nSchedule time is $nightScheduleTime"

	if (canSchedule()) {
        if (timeAfterSunset.before(nightScheduleTime)) {
            log.debug "Shades Preset in $minuteAfterSunset min"
            messages = messages + "\nShades Preset in $minuteAfterSunset min"
            runIn(minuteAfterSunset * 60, setEveningScene)
        } else {
            log.debug "Shades Close in $minuteAfterSunset min"
            messages = messages + "\nShades Close in $minuteAfterSunset min"
            runIn(minuteAfterSunset * 60, setNightScene)
        }
	} else {
    	sendNotificationEvent("sunsetHandler: Reach Max Schedule!")
        messages = messages + "\nERROR Reach Max Schedule!"
    }
    
	sendNotificationToContacts(messages, recipients)
}

def setEveningScene() {
	shades.presetPosition()
}

def setNightScene() {
	shades.close()
}

def sunriseHandler(evt) {
    log.debug "Sun has risen!"
//    sendNotificationEvent("Sun has risen!")
	def messages = "Sun has risen!";
    
    if (notAway()) {
        setLocationMode("Home")
        messages = messages + "\n- Set Mode Home"
    
        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK);
        def todayOpenTime = null
//        log.debug "Current Day of Week: $currentDayOfWeek"
        if ((currentDayOfWeek == Calendar.instance.MONDAY) ||
            (currentDayOfWeek == Calendar.instance.TUESDAY) ||
            (currentDayOfWeek == Calendar.instance.WEDNESDAY) ||
            (currentDayOfWeek == Calendar.instance.THURSDAY) ||
            (currentDayOfWeek == Calendar.instance.FRIDAY)) {
//			log.debug "In week"
		    todayOpenTime = timeToday(weekDayTime, location.timeZone)
        } else {
 //       	log.debug "Week End"
            todayOpenTime = timeToday(weekEndTime, location.timeZone)
        }

	    def now = new Date()
//		log.debug "Now time: $now"
//        log.debug "week day time: $weekDayTime"
//        log.debug "week End time: $weekEndTime"
//        log.debug "Today open time: $todayOpenTime"
        if (now > todayOpenTime) {
//        	log.debug "Open Shade"
        	shades.open()
        }
        messages = messages + "\nNow: $now\nToday open Time: $todayOpenTime"
    }
    
    sendNotificationToContacts(messages, recipients)
}

def weekDayMorningSetupHandler(evt) {
	log.debug "weekDayMorningSetupHandler"
//    sendNotificationEvent("Week Day Morning Setup!")
    sendNotificationToContacts("Week Day Morning Setup!", recipients)

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    
	if (now >= sunTime.sunrise) {
    	shades.open()
	}    
}

def weekEndMorningSetupHandler(evt) {
	log.debug "weekEndMorningSetupHandler"
//    sendNotificationEvent("Week End Morning Setup!")
    sendNotificationToContacts("Week End Morning Setup!", recipients)

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    
	if (now >= sunTime.sunrise) {
    	shades.open()
	}    
}

def nightSetupHandler(evt) {
	log.debug ("NightSetupHandler")
//    sendNotificationEvent("Night Setup!")
	def messages = "Night Setup!"

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    def timeAfterSunset = new Date(sunTime.sunset.time + (minuteAfterSunset * 60 * 1000))
	log.debug "Sunset is at $sunTime.sunset. Sunset + $minuteAfterSunset = $timeAfterSunset. Current time is $now"
    messages = messages + "\nSunset is at $sunTime.sunset.\nSunset + $minuteAfterSunset = $timeAfterSunset.\nCurrent time is $now"

	if (timeAfterSunset.before(now)) {
		log.debug "Sunset + $minuteAfterSunset before Night Setup"
        messages = messages + "\nSunset + $minuteAfterSunset before Night Setup"
        shades.close()
	}

    portelock.lock()
    
    sendNotificationToContacts(messages, recipients)
}

def onTestHandler(evt) {
	log.debug "Switch On simulate Sunrise!"
    
    sunriseHandler(evt)
}

def offTestHandler(evt) {
	log.debug "Switch Off simulate Sunset!"
    
    sunsetHandler(evt)
}