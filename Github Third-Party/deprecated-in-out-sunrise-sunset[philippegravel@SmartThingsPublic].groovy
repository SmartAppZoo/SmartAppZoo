/**
 *  In/Out/Sunrise/Sunset
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
    name: "Deprecated - In/Out/Sunrise/Sunset",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Deprecated - Set mode when arrive and leaving",
    category: "Family",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png"
)

preferences {

	page(name: "selectPeople")
    page(name: "Things")
}

def selectPeople() {
	dynamicPage(name: "selectPeople", title: "First, select who", nextPage: "Things", uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
        }

		section("When people arrive and leave..."){
			input "peopleToWatch", "capability.presenceSensor", title: "Who?", multiple: true, required: true
		}
        
        section("Delay to keep front light on and unlock door") {
        	input "delay", "number", title: "Number in minutes?", required: true
        }
        
        section("Time to set morning event") {
        	input "weekDayTime", "time", title: "Week day"
            input "weekEndTime", "time", title: "Week end"
        }
        
        section("Minute after sunset to preset windows") { 
        	input "minuteAfterSunset", "number", title: "Minutes after sunset"
        }
        
        section("Time to set night event") {
			input "nightTime", "time", title: "Night Time"
		}

		section("Visitor") {
        	input "visitorSwitch", "capability.switch", title: "Visitor Switch?", required: true
        }

//        section("Test Sunset/Sunrise") {
//        	input "testSwitch", "capability.switch", title: "Switchs?", multiple: false, required: false
//        }
    }
}

def Things() {
	dynamicPage(name: "Things", title: "Set the things", install: true, uninstall: true) {    
		section() {
			input "avant", "capability.switch", title: "Lumiere Avant", required: true
            input "entree", "capability.switch", title: "Entree", required: true
            input "comptoir", "capability.switch", title: "Comptoir", required: true
//            input "aquarium", "capability.switch", title: "Aquarium", required: true
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

	subscribe(peopleToWatch, "presence", presenseHandler)
	subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)

	def timeZone = location.timeZone
	def currentTime = timeToday(weekDayTime, timeZone)
	def timeHour = currentTime.format("H", timeZone)
	def timeMinute = currentTime.format("m", timeZone)
    log.debug "Week day: $timeHour:$timeMinute"
	schedule("0 $timeMinute $timeHour ? * MON-FRI *", weekDayMorningSetupHandler)
    
	currentTime = timeToday(weekEndTime, timeZone)
 	timeHour = currentTime.format('H', timeZone)
    timeMinute = currentTime.format('m', timeZone)
 	log.debug "Week End: $timeHour:$timeMinute"
    schedule("0 $timeMinute $timeHour ? * SAT,SUN *", weekEndMorningSetupHandler)

	currentTime = timeToday(nightTime, timeZone)
 	timeHour = currentTime.format('H', timeZone)
    timeMinute = currentTime.format('m', timeZone)
 	log.debug "Night Time: $timeHour:$timeMinute"
    schedule("0 $timeMinute $timeHour ? * * *", nightSetupHandler)
    
//    subscribe(testSwitch, "switch.on", onTestHandler)
    subscribe(testSwitch, "switch.off", offTestHandler)
}

def presenseHandler(evt) {
	
	log.debug "presenceHandler $evt.name: $evt.value, $evt.displayName"
//    sendNotificationEvent("presenceHandler $evt.name: $evt.value, $evt.displayName")

    def now = new Date()
    def sunTime = getSunriseAndSunset()
    log.debug "sunrise and sunset: $sunTime"
    def inNight = (now > sunTime.sunset)
    def delayForLight = false
    def delayForDoor = false
    
    if (evt.value == "not present") {
		log.debug "Someone left"
//			sendNotificationEvent("Someone left")

        def presenceValue = peopleToWatch.find{it.currentPresence == "present"}
        if (presenceValue) {
        	log.debug "Still somebody home - nothing to do"
//				sendNotificationEvent("Still somebody home - nothing to do")
		} else {
        	log.debug "Everybody as left - Do Goodbye!"
//				sendNotificationEvent("Everybody as left - Do Goodbye!")
            
			if (!visitorAtHome()) {
                portelock.lock()
                shades.close()
                setLocationMode("Away")
            	location.helloHome.execute("All Off")
            }
        } 
	} else {
    	log.debug "Someone arrive"
//			sendNotificationEvent("Someone arrive")

		if (notAway()) {
        	log.debug "Somebody already home"
//				sendNotificationEvent("Somebody already home")
            
            if (inNight && avant.currentValue("switch") == "off") {
            	avant.on()
                delayForLight = true
            }
            
		} else {
        	log.debug "First arrive - Do Hello!"
//				sendNotificationEvent("First arrive - Do Hello!")
            
            if (inNight) {
                log.debug "Change Mode to Night"
//                    sendNotificationEvent("Change Mode to Night")

				setLocationMode("Night") 
            } else {
                log.debug "Change Mode to Home"
//                    sendNotificationEvent("Change Mode to Home")
                shades.open()
                
            	setLocationMode("Home")
            }

			if (inNight) {
            
            	comptoir.setLevel(20)
                entree.setLevel(100)
                
                if (avant.currentValue("switch") == "off") {
                	avant.on()
					delayForLight = true	
                }
            }
        }

        if (inNight) {
			def lockstatus = portelock.currentValue("lock")
    		if (lockstatus == "locked") {
            	delayForDoor = true
            }
        }
    	
        portelock.unlock()
        
        if (delayForLight) {
        	if (delayForDoor) {
            	runIn(delay * 60, globalDelay)
            } else {
				runIn(delay * 60, closeSwitchsDelay)
            }
        } else if (delayForDoor) {
        	runIn(delay * 60, lockDoorDelay)
        }
    }
}

def globalDelay() {
	lockDoorDelay()
	closeSwitchsDelay()
}

def closeSwitchsDelay() {	
    avant.off()
}

def lockDoorDelay() {
	portelock.lock()
}

def visitorAtHome() {

	return (visitorSwitch.currentSwitch == "on")
}

def notAway() {
	def currMode = location.mode // "Home", "Away", etc.
    log.debug "Not Away - current mode is $currMode"

	return (currMode != "Away")
}

def sunsetHandler(evt) {
    log.debug "Sun has set!"
    sendNotificationEvent("Sun has set!")

    if (notAway()) {
	    setLocationMode("Night")
	}        
    
 	def timeZone = location.timeZone
	def nightScheduleTime = timeToday(nightTime, timeZone)
       
    def sunTime = new Date()
    def timeAfterSunset = new Date(sunTime.time + (minuteAfterSunset * 60 * 1000))
    log.debug "Sunset is at $sunTime. Sunset + $minuteAfterSunset = $timeAfterSunset. Schedule time is $nightScheduleTime"
//    def sunTime = getSunriseAndSunset()
//    def timeAfterSunset = new Date(sunTime.sunset.time + (minuteAfterSunset * 60 * 1000))
//	log.debug "Sunset is at $sunTime.sunset. Sunset + $minuteAfterSunset = $timeAfterSunset. Schedule time is $nightScheduleTime"

	if (timeAfterSunset.before(nightScheduleTime)) {
    	log.debug "Shades Preset in $minuteAfterSunset min"
    	runIn(minuteAfterSunset * 60, setEveningScene)
	} else {
    	log.debug "Shades Close in $minuteAfterSunset min"
    	runIn(minuteAfterSunset * 60, setNightScene)
	}
}

def setEveningScene() {
	shades.presetPosition()
}

def setNightScene() {
	shades.close()
}

def sunriseHandler(evt) {
    log.debug "Sun has risen!"
    sendNotificationEvent("Sun has risen!")
    
    if (notAway()) {
        setLocationMode("Home")
    
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
    }
}

def weekDayMorningSetupHandler(evt) {
	log.debug "weekDayMorningSetupHandler"
    sendNotificationEvent("Week Day Morning Setup!")

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    
	if (now >= sunTime.sunrise) {
    	shades.open()
	}    
}

def weekEndMorningSetupHandler(evt) {
	log.debug "weekEndMorningSetupHandler"
    sendNotificationEvent("Week End Morning Setup!")

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    
	if (now >= sunTime.sunrise) {
    	shades.open()
	}    
}

def nightSetupHandler(evt) {
	log.debug ("NightSetupHandler")
    sendNotificationEvent("Night Setup!")

	def now = new Date()
    def sunTime = getSunriseAndSunset()
    def timeAfterSunset = new Date(sunTime.sunset.time + (minuteAfterSunset * 60 * 1000))
	log.debug "Sunset is at $sunTime.sunset. Sunset + $minuteAfterSunset = $timeAfterSunset. Current time is $now"

	if (timeAfterSunset.before(now)) {
		log.debug "Sunset + $minuteAfterSunset before Night Setup"
        shades.close()
	}

    portelock.lock()
}

def onTestHandler(evt) {
	log.debug "Switch On simulate Sunrise!"
    
    sunriseHandler(evt)
}

def offTestHandler(evt) {
	log.debug "Switch Off simulate Sunset!"
    
    sunsetHandler(evt)
}