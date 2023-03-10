/**
 *  State Presence Handler
 *
 *  Copyright 2016 tybo27
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
 
 /* ******************************************************************************************
* Definition: Name, namespace, author, description, category, icon						 	*
*********************************************************************************************/
definition(
    name: "State Presence Handler",
    namespace: "tybo27",
    author: "tybo27",
    description: "Makes use of presence sensors to change states",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

/* ******************************************************************************************
* Preferences: Input presences, reset times, and switches/dimmers to turn on			 	*
*********************************************************************************************/
preferences {
    section("Person 1") {
		input "presence1", "capability.presenceSensor", multiple: false, title: "Using whose presence"
        input "dayMode1", "mode", title: "Day mode to transition into when person 1 is present", multiple: false, required: false
        input "eveningMode1", "mode", title: "Evening mode to transition into when person 1 is present", multiple: false, required: false
        input "nightMode1", "mode", title: "Night mode to transition into when person 1 is present", multiple: false, required: false
	}
    section("Person 2") {
        input "presence2", "capability.presenceSensor", multiple: false, title: "Using whose presence"
    	input "dayMode2", "mode", title: "Day mode to transition into when person 2 is present", multiple: false, required: false
        input "eveningMode2", "mode", title: "Evening mode to transition into when person 2 is present", multiple: false, required: false
		input "nightMode2", "mode", title: "Night mode to transition into when person 2 is present", multiple: false, required: false
    }
    section("Joint Modes") {
    	input "dayModeJoint", "mode", title: "Day mode to transition into when both people are present", multiple: false, required: false
        input "eveningModeJoint", "mode", title: "Evening mode to transition into when both people are present", multiple: false, required: false
        input "nightModeJoint", "mode", title: "Night mode to transition into when both people are present", multiple: false, required: false
    }
    section("Away Mode") {
    	input "awayMode", "mode", title: "Mode to transition into when both people are away", multiple: false, required: false
        input "vacationMode", "mode", title: "Mode to transition into when both people are away for over X hours", multiple: false, required: false
        input "vacationModeDelay", "number", title: "Number of hours to transition to vacation mode", multiple: false, required: false
    }
    section("Times") {
    	input "weekdayNightTime", "time", title: "Time to auto transition to night on weekdays", multiple: false, required: false
        input "weekdayDayTime", "time", title: "Time to auto transition to day on weekdays", multiple: false, required: false
        input "weekendNightTime", "time", title: "Time to auto transition to night on weekends", multiple: false, required: false
        input "weekendDayTime", "time", title: "Time to auto transition to day on weekends", multiple: false, required: false
    }
}

/* ******************************************************************************************
* Installed: Initialize	smartApp														 	*
*********************************************************************************************/
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

/* ******************************************************************************************
* Updated: Unsubscribe and reinitialize													 	*
*********************************************************************************************/
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

/* ******************************************************************************************
* Initialize: Subscribe to events														 	*
*********************************************************************************************/
def initialize() {

    // Subscribe to mode changes
    //subscribe(location, modeChangeHandler)
    //subscribe(location, "position", positionChange)
    //subscribe(location, "mode", modeChangeHandler)
    
    // Subscribe to presence changes
    subscribe(presence1, "presence", presenceHandler)
    subscribe(presence2, "presence", presenceHandler)
    
    // Subscribe to / schedule times
    subscribe(location, "sunset", day2Evening)
    schedule(weekdayNightTime, evening2NightWeekday)
    schedule(weekendNightTime, evening2NightWeekend)
    schedule(weekdayDayTime, night2DayWeekday)
    schedule(weekendDayTime, night2DayWeekend)
    //subscribe(location, "sunrise", sunriseHandler)
    //subscribe(location, "sunsetTime", sunsetTimeHandler)
    //subscribe(location, "sunriseTime", sunriseTimeHandler)
    atomicState.timePeriod = getTimePeriod()
    atomicState.awayStart = now()
    log.debug "Initialized with timePeriod=${atomicState.timePeriod}, at time=${atomicState.awayStart}"
}

/* ******************************************************************************************
* PresenceHandler: change mode depending on presences									 	*
*********************************************************************************************/
def presenceHandler (evt) {
	log.debug "presenceHandler: $evt, source is $evt.source and display name $evt.displayName"
	calcNewMode()
}

/* ******************************************************************************************
* calcNewMode: Use timePeriod and presence to calculate the correct Mode				 	*
*********************************************************************************************/
def calcNewMode() {
	def curMode = location.mode
    def newMode
	def timePeriod = atomicState.timePeriod
    log.debug "calcNewMode: curMode=$curMode, timePeriod=$timePeriod"
	if (presence1=="present" && presence2=="present") {
    	// Figure out new Mode
        switch (timePeriod) {
        	case 'day': newMode=dayModeJoint
            case 'evening': newMode=eveningModeJoint
            case 'night': newMode = nightModeJoint
        }
        log.debug "calcNewMode: Both present, newMode=$newMode"
    } else if (presence1=="present") {
    	// Figure out new Mode
        switch (timePeriod) {
        	case 'day': newMode=dayMode1
            case 'evening': newMode=eveningMode1
            case 'night': newMode = nightMode1
        }
        log.debug "calcNewMode: Person1 present, newMode=$newMode"
    } else if (presence2=="present") {
    	// Figure out new Mode
        switch (timePeriod) {
        	case 'day': newMode=dayMode2
            case 'evening': newMode=eveningMode2
            case 'night': newMode = nightMode2
        }
        log.debug "calcNewMode: Person2 present, newMode=$newMode"
    } else {
    	log.debug "calcNewMode: No one present"
        // if not currently in away or vacation, set to away and schedule check in minutes
        if (curMode != awayMode && curMode != vacationMode) {
        	log.debug "calcNewMode: Not yet in Away or Vacation"
            newMode = awayMode
            RunIn(vacationModeDelay*60*60, vacationModeDelay)
            atomicState.awayStart = now()
            log.debug "started away counter"
        } else if (curMode == awayMode) {
        	log.debug "calcNewMode: Currently in Away, awayStart = ${atomicState.awayStart}"
            //if time since awayMode set, change to vacation Mode
			def timeAway = (now() - atomicState.awayStart)/(1000*60*60)
            log.debug "calcNewMode: Currently in Away, now=${now()}, awayStart = ${atomicState.awayStart}, timeAway=$timeAway"
            if (timeAway >= vacationModeDelay) {
            	log.debug "Should update Mode to vacation"
            	newMode=vacationMode
            } else {
            	log.debug "Should keep Mode to Away"
            	newMode=awayMode
            } 
        }
    }
    if (newMode != awayMode) {
        atomicState.awayStart=null
    }
    log.debug "curMode is $curMode, and newMode is $newMode"
    // Send change mode command
	changeMode(newMode)
}

/* ******************************************************************************************
* modeHandler: Capture away date/time													 	*
*********************************************************************************************/
def modeHandler (evt) {

	log.debug "Mode Handler called, do I need to handle self calls?: $evt, source is $evt.source and display name $evt.displayName"
    // set manualChange to true, if not a self call
    atomicState.externalChange = true
	if (evt.value==awayState) {
		if (location.mode != awayState) {
        	atomicState.awayStart = evt.date
        }
	} else {
		atomicState.awayStart = null
	}
}

/* ******************************************************************************************
* changeMode: Change modes if new mode commanded (and mode exists)						 	*
*********************************************************************************************/
def changeMode(newMode) { 
	log.debug "changeMode, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"
	if (location.mode != newMode) { 
		if (location.modes?.find{it.name == newMode}) { 
			setLocationMode(newMode) 
		} else { 
			log.warn "Tried to change to undefined mode '${newMode}'" 
		} 
	}
}

/* ******************************************************************************************
* night2DayWeekday: Handler for scheduled night to day weekday time					 	*
*********************************************************************************************/
def night2DayWeekday(evt) {

    if (isWeekday()) {
    	log.debug "night2DayWeekday: Transitioning timePeriod from Night to Day"
    	atomicState.timePeriod = "day"
        calcNewMode()
    } else {
    	log.debug "night2DayWeekday: No transition, it's a Weekend"
    }
    
}

/* ******************************************************************************************
* night2DayWeekend: Handler for scheduled night to day weekend time					 	*
*********************************************************************************************/
def night2DayWeekend(evt) {
    
    if (isWeekday()) {
    	log.debug "night2DayWeekend: No transition, it's a Weekday"
    } else {
    	log.debug "night2DayWeekend: Transitioning timePeriod from Night to Day"
    	atomicState.timePeriod = "day"
        calcNewMode()
    }
}

/* ******************************************************************************************
* evening2NightWeekday: Handler for scheduled evening to night weekday time			 	*
*********************************************************************************************/
def evening2NightWeekday(evt) {

    if (isWeekday()) {
    	log.debug "evening2NightWeekday: Transitioning timePeriod from Evening to Night"
    	atomicState.timePeriod = "night"
        calcNewMode()
    } else {
    	log.debug "evening2NightWeekday: No transition, it's a Weekend"
    }
}

/* ******************************************************************************************
* evening2NightWeekend: Handler for scheduled evening to night weekend time			 	*
*********************************************************************************************/
def evening2NightWeekend(evt) {

    if (isWeekday()) {
    	log.debug "evening2NightWeekend: No transition, it's a Weekday"
    } else {
    	log.debug "evening2NightWeekend: Transitioning timePeriod from Evening to Night"
    	atomicState.timePeriod = "night"
        calcNewMode()
    }
}

/* ******************************************************************************************
* day2EveningWeekend: Handler for scheduled day to evening time						 	*
*********************************************************************************************/
def day2Evening(evt) {

	log.debug "day2Evening: Transitioning timePeriod from Day to Evening"
    atomicState.timePeriod = "Evening"
    calcNewMode()
}

/* ******************************************************************************************
* isWeekday: Returns a boolean true if day of week is  M,T,W,Th, or F, false otherwie	 	*
*********************************************************************************************/
def isWeekday() {

	def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def curDay = df.format(new Date())
    
    def weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
    def isWeekdayVar = weekdays.contains(curDay)
    
    log.debug "isWeekday: Day is $curDay, isWeekday=$isWeekdayVar"
    return isWeekdayVar
}
    
/* ******************************************************************************************
* getTimePeriod: Categorize time into day, evening, or night	
*********************************************************************************************/
def getTimePeriod () {

    def curTime = new Date()
    def sunset = getSunriseAndSunset().sunset
    // intialize timePediod to day
    def timePeriod = 'day'
    def periodNum = 0

    // handle override (alarm override transition for night)
    if (isWeekday) {
    	if (timeOfDayIsBetween(weekdayDayTime, sunset, curTime, location.timeZone)) {
        	timePeriod = 'day'
        } else if (timeOfDayIsBetween(sunset, weekdayNightTime, curTime, location.timeZone)) {
    		timePeriod = 'evening'
    	} else if (timeOfDayIsBetween(weekdayNightTime, weekdayDayTime, curTime, location.timeZone)) {
    		timePeriod = 'night'
    	}
        log.debug "getTimePeriod: weekdayDayTime=$weekdayDayTime, sunset=$sunset, curTime=$curTime, timePeriod=$timePeriod"
	} else {
    	if (timeOfDayIsBetween(weekendDayTime, sunset, curTime, location.timeZone)) {
        	timePeriod = 'day'
        } else if (timeOfDayIsBetween(sunse, weekendNightTime, curTime, location.timeZone)) {
    		timePeriod = 'evening'
    	} else if (timeOfDayIsBetween(weekendNightTime, weekendDayTime, curTime, location.timeZone)) {
    		timePeriod = 'night'
    	}
        log.debug "getTimePeriod: weekendDayTime=$weekendDayTime, sunset=$sunset, curTime=$curTime, timePeriod=$timePeriod"
    } 
   	return timePeriod
}