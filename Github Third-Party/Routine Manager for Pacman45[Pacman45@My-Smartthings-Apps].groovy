/**
 *  Routine Director
 *
 *
 *  Changelog
 *
 *  2015-09-01
 *     --Added Contact Book
 *     --Removed references to phrases and replaced with routines
 *     --Added bool logic to inputs instead of enum for "yes" "no" options
 *     --Fixed halting error with code installation
 *
 *  Copyright 2015 Tim Slagle
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
    name: "Routine Director",
    namespace: "Pacman45",
    author: "Matt Newman",
    description: "Monitor a set of presence sensors and activate routines based on whether your home is empty or occupied.  Each presence status change will check against the current day of week and time of day to run routines based on occupancy and day of week and time of day.",
    category: "Convenience",
    iconUrl: "http://icons.iconarchive.com/icons/icons8/ios7/512/Very-Basic-Home-Filled-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/icons8/ios7/512/Very-Basic-Home-Filled-icon.png"
)

preferences {
    page(name: "selectRoutines")

    page(name: "Settings", title: "Settings", uninstall: true, install: true) {
        section("False alarm thresholds (defaults to 0 mins for arrivals, 240 mins for departures)") {
            input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
        }
        section("Vacation Mode delay threshold (defaults to 24 hours after last departure)") {
            input "vacationModeDelayThreshold", "decimal", title: "Number of hours", required: false
        }
/*      section("Zip code (for sunrise/sunset)") {
            input "zip", "text", required: true
        }  */

        section("Daytime Routines Start and Stop Times") {
            input "timeA", "time", title: "Enter daytime routine start time:"
            input "timeB", "time", title: "Enter daytime routine stop time"
        }

        section("Notifications") {
            input "sendPushMessage", "bool", title: "Send notifications when house is empty?"
            input "sendPushMessageHome", "bool", title: "Send notifications when home is occupied?"
        }
        section("Send Notifications?") {
            input("recipients", "contact", title: "Send notifications to") {
                input "phone", "phone", title: "Send an SMS to this number?", required:false
            }
        }

        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
            label title: "Assign a name", required: false
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        }
    }

def selectRoutines() {
    def configured = (settings.awayDay && settings.awayNight && settings.homeDay && settings.homeNight)
    dynamicPage(name: "selectRoutines", title: "Configure", nextPage: "Settings", uninstall: true) {
        section("Who?") {
            input "people", "capability.presenceSensor", title: "Monitor These Presences", required: true, multiple: true, submitOnChange: true
        }

        def routines = location.helloHome?.getPhrases()*.label
        if (routines) {
            routines.sort()
            section("Run This Routine When...") {
                    log.trace routines
                    input "awayDay", "enum", title: "Everyone Is Away And It's Day", required: true, options: routines, submitOnChange: true
                    input "awayNight", "enum", title: "Everyone Is Away And It's Night", required: true, options: routines, submitOnChange: true
                    input "awayVacation", "enum", title: "Everyone is Away for Extended Amount of Time", required: true, options: routines, submitOnChange: true
                    input "homeDay", "enum", title: "At Least One Person Is Home And It's Day", required: true, options: routines, submitOnChange: true
                    input "homeNight", "enum", title: "At Least One Person Is Home And It's Night", required: true, options: routines, submitOnChange: true
                }
                /*    section("Select modes used for each condition.") { This allows the director to know which rotuine has already been ran so it does not run again if someone else comes home. 
        input "homeModeDay", "mode", title: "Select Mode Used for 'Home Day'", required: true
        input "homeModeNight", "mode", title: "Select Mode Used for 'Home Night'", required: true
    }*/
        }
    }
}

def installed() {
	log.debug "Updated with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(people, "presence", presence)
/*  checkSun()  ** Use absolute time instead of sunrise sunset */
    checkDaytime()
 /* subscribe(location, "sunrise", setSunrise)
    subscribe(location, "sunset", setSunset)  */
    schedule(timeA, setDaytimeModeHandler)
    schedule(timeB, setNighttimeModeHandler)
    state.homestate = null
    state.setVacationAwayScheduled = false
}

def checkDaytime() {
    def df = new java.text.SimpleDateFormat("EEEE")
//  Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def between = timeOfDayIsBetween(timeA, timeB, new Date(), location.timeZone)

    if (between) {
        state.daytimeMode = "daytime"
    }
    else {
        state.daytimeMode = "nighttime"
    }
}
//check current sun state when installed.
/* def checkSun() {
    def zip = settings.zip as String
    def sunInfo = getSunriseAndSunset(zipCode: zip)
    def current = now()

    if (sunInfo.sunrise.time < current && sunInfo.sunset.time > current) {
        state.sunMode = "sunrise"
        runIn(60,"setSunrise")
    }
    else {
        state.sunMode = "sunset"
        runIn(60,"setSunset")
    }
}  */

//change to dayTimeMode at timeA
def setDaytimeModeHandler() {
    state.daytimeMode = "daytime";
    log.debug "Current time of day mode is ${state.daytimeMode}"
}

// change to nightTimeMode at timeB
def setNightTimeModeHandler() {
    state.daytimeMode = "nightime";
    log.debug "Current time of day mode is ${state.daytimeMode}"
}
/* Not using sunrise sunset logic
//change to sunrise mode on sunrise event
def setSunrise(evt) {
    state.sunMode = "sunrise";
    changeSunMode(newMode);
    log.debug "Current sun mode is ${state.sunMode}"
}

//change to sunset mode on sunset event
def setSunset(evt) {
    state.sunMode = "sunset";
    changeSunMode(newMode)
    log.debug "Current sun mode is ${state.sunMode}"
}   */

//change mode on sun event
def changeSunMode(newMode) {
    if (allOk) {

        if (everyoneIsAway()) /*&& (state.sunMode == "sunrise")*/ {
            log.info("Home is Empty  Setting New Away Mode")
            def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 240 * 60
            setAway()
        }
/*
        else if (everyoneIsAway() && (state.sunMode == "sunset")) {
            log.info("Home is Empty  Setting New Away Mode")
            def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
            setAway()
        }*/
        else if (anyoneIsHome()) {
            log.info("Home is Occupied Setting New Home Mode")
            setHome()


        }
    }
}

//presence change run logic based on presence state of home
def presence(evt) {
    def homeMode = location.mode
    if (allOk) {
        if (evt.value == "not present") {
            log.debug("Checking if everyone is away")

            if (everyoneIsAway()) {
                log.info("Nobody is home, running away sequence")
                def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 240 * 60
                def vacationModeDelayinSeconds = (vacationModeDelayThreshold) ? vacationModeDelayThreshold * 3600 : 24 * 3600
                if (homeMode != "Home-Bitter Cold") {
                    runIn(delay, setAway)
                    runIn(vacationModeDelayinSeconds, setVacationAway) //Temps are not bitter cold, so use Vacation mode settings for extended absence
                    state.setVacationAwayScheduled = true                //Vacation mode is scheduled to run
                } else runIn(vacationModeDelayinSeconds, setAway)  //It is bitter cold, so set moderate away limits for Vacation Away
            }
        } else {
            //def lastTime = state[evt.deviceId]
            //if (lastTime == null || now() - lastTime >= 1 * 60000) {
                log.info("Someone is home, running home sequence")
                unschedule(scheduledSetAway)
                if (state.setVacationAwayScheduled) {
                    unschedule(scheduledSetVacationAway)
                    state.setVacationAwayScheduled = false
                }
                location.setMode("Home")
                setHome()
            //}
                state[evt.deviceId] = now()

        }
        
    }
}

//if empty set home to one of the away modes
def setAway() {
    if (everyoneIsAway()) {
        if (state.daytimeMode == "nighttime") {
/*      if (state.sunMode == "sunset") {     ** not using sunMode in this installation  */
            def message = "Performing \"${awayNight}\" for you as requested."
            log.info(message)
            sendAway(message)
            location.helloHome.execute(settings.awayNight)
            state.homestate = "away"

        }
/*      else if (state.sunMode == "sunrise") {    ** not using sunMode   */
        else if (state.daytimeMode == "daytime") {
            def message = "Performing \"${awayDay}\" for you as requested."
            log.info(message)
            sendAway(message)
            location.helloHome.execute(settings.awayDay)
            state.homestate = "away"
        }
        else {
            log.debug("Mode is the same, not evaluating")
        }
    }
}

//if empty for extended period of time as defined by vacationModeDelayThreshold, set home to awayVacation
def setVacationAway() {
    def message = "Performing \"${awayVacation}\" for you as requested."
    log.info(message)
    sendaway(message)
    location.helloHome.execute(settings.awayVacation)
    state.homestate = "away"
}

//set home mode when house is occupied
def setHome() {
    log.info("Setting Home Mode!!")
    if (anyoneIsHome()) {
        if (state.daytimeMode == "nighttime") {
 /*     if (state.sunMode == "sunset") {  ** not using sunMode in this installation */
            if (state.homestate != "homeNight") {
                def message = "Performing \"${homeNight}\" for you as requested."
                log.info(message)
                sendHome(message)
                location.helloHome.execute(settings.homeNight)
                state.homestate = "homeNight"
            }
        }
        if (state.daytimeMode == "daytime") {
/*      if (state.sunMode == "sunrise") {  ** not using sunMode in this installation  */
            if (state.homestate != "homeDay") {
                def message = "Performing \"${homeDay}\" for you as requested."
                log.info(message)
                sendHome(message)
                location.helloHome.execute(settings.homeDay)
                state.homestate = "homeDay"
            }
        }
    }
}

private everyoneIsAway() {
  def result = true

  if(people.findAll { it?.currentPresence == "present" }) {
    result = false
  }

  log.debug("everyoneIsAway: ${result}")

  return result
}

private anyoneIsHome() {
  def result = false

  if(people.findAll { it?.currentPresence == "present" }) {
    result = true
  }

  log.debug("anyoneIsHome: ${result}")

  return result
}

def sendAway(msg) {
    if (sendPushMessage) {
    	if (recipients) {
        	sendNotificationToContacts(msg, recipients)
        }
        else {
        	sendPush(msg)
        	if(phone){
            		sendSms(phone, msg)
        	}	
        }    
    }

    log.debug(msg)
}

def sendHome(msg) {
    if (sendPushMessageHome) {
    	if (recipients) {
        	sendNotificationToContacts(msg, recipients)
        }
        else {
        	sendPush(msg)
                if(phone){
            		sendSms(phone, msg)
        	}
        }    
    }

    log.debug(msg)
}

private getAllOk() {
    modeOk && daysOk && timeOk
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    log.trace "modeOk = $result"
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    log.trace "daysOk = $result"
    result
}

private getTimeOk() {
    def result = true
    if (starting && ending) {
        def currTime = now()
        def start = timeToday(starting, location?.timeZone).time
        def stop = timeToday(ending, location?.timeZone).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
    log.trace "timeOk = $result"
    result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone?:timeZone(time))
    f.format(t)
}

private getTimeIntervalLabel() {
    (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z"): ""
}

private hideOptionsSection() {
    (starting || ending || days || modes) ? false: true
}
