/**
 *  Smart Block Heater
 *
 *  Copyright 2017 kenobob
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
    name: "Smart Block Heater",
    namespace: "kenobob",
    author: "kenobob",
    description: "This will be a smart block heater",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation6-icn@3x.png")
//    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
//    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
//    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
//http://scripts.3dgo.net/smartthings/icons/


preferences {
    section("On for this amount of time") {
        input (name: "minutes", type: "number", title: "Minutes?", required: true)
    }
    section("Temperature You want to turn on at.") {
        input (name: "onTemperature", type: "number", title: "Temp?", required: true)
    }
    //Time or Mode, not sure yet.
    section("When does your quiet hours start?") {
        input "beforeBedNotificationTime", "time", title: "Time?", required: true
    }
    section("When do you need your car to start?") {
        input "carStartTime", "time", title: "Time?", required: true
    }
    section("Which Switch is your block heater plugged into?") {
        input "switches", "capability.switch", multiple: true, required: true
    }
    section("Temperature Sensor"){
        input "bwsTemperatureMeasurement", "capability.temperatureMeasurement", multiple: false, required: true
    }
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneNumber", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    
    // re-initialize the smartapp with new options
    initalizeTheApp()
}

//TODO: Add in Tempeature Schedule Check
def initialize() {
    log.trace("Executing Initialize")
    
    initalizeTheApp()
	
    log.trace("End Initialize")
}

def initalizeTheApp(){
    log.trace("Executing initalizeTheApp")
    
    //Reset everything!!
    // remove all scheduled executions for this SmartApp install
    unschedule()
    // unsubscribe all listeners.
    unsubscribe()
    
    //Reset Application state variables
    state.onTimeRunOnceDate = null
    state.lastActiveScheduleDate = null
    
    //Start Setting up
    createSubscriptions()
	
    //Schedule back up the daily check
    createDailyScheduler()
	
    //Process current low temp to see if we need to schedule something
    processTemperature(getCurrentLowTemp())
	
    log.trace("End initalizeTheApp")
}

//TODO: Subscribe to Presence sensors for notificaitons when you get home
private def createSubscriptions()
{
    log.trace("Executing Create Subscriptions")
    
    //Subscribe to BWS low tempurature change.
    subscribe(bwsTemperatureMeasurement, "lowtemperature", lowForecastedTemperatureChanges)
    
    //	subscribe(motionSensors, "motion.active", motionActiveHandler)
    //	subscribe(motionSensors, "motion.inactive", motionInactiveHandler)
    //	subscribe(switches, "switch.off", switchOffHandler)
    //	subscribe(location, modeChangeHandler)
    //
    //	if (state.modeStartTime == null) {
    //		state.modeStartTime = 0
    //	}
    log.trace("End Create Subscriptions")
}

////////////////////////////////////////////////////////////////
///// *************Subscription Event Handlers ****************/
////////////////////////////////////////////////////////////////
def lowForecastedTemperatureChanges(evt){
    
    log.trace("Executing lowForecastedTemperatureChanges")
    log.debug ("The Low Changed To: ${evt.numericValue}")
    
    //Process the temperature change to see if we need to do something
    processTemperature(evt.numericValue)
    
    log.trace("End lowForecastedTemperatureChanges")
}


////////////////////////////////////////////////////////////////
///// *************** Create Schedulers *********************/
////////////////////////////////////////////////////////////////
private def createDailyScheduler(){
    log.trace("Executing createDailyScheduler")
	
    def onTime = CalculateReOccuringOnTime()
	
    log.debug("Set Daily On Time: ${onTime}")
	
    //Only the Time is used for the date object
    schedule(onTime, justInCaseCheck)
	
    log.trace("End createDailyScheduler")
}

private def createOneTimeSchedulers(){
    log.trace("Executing createOneTimeSchedulers")
	
    //I'm out of scheduled events somehow, clear them out!
    if(!canSchedule()){
        log.debug("Scheduler Full, clear them out and let's start over")
        clearAllSchedules()
    }
	
    
    def onTime = CalculateOnTime2()
    
    log.debug("Block Heater On Time: ${onTime}")
    
    createNotificationScheduler()
    
    //create scheduler to turn on block hearter(s)
    runOnce(onTime, checkThenTurnOnSwitch)
    
    createStarTimeScheduler(onTime)
	
    state.onTimeRunOnceDate =  convertDatetoISODateString(onTime)
    log.trace("End createOneTimeSchedulers")
}

def createStarTimeScheduler(onTime){
    def onTimeCalendar = convertDateToCalendar(onTime)
    def startTimeCalendar = convertDateToCalendar(carStartTime)
    
    startTimeCalendar.set(Calendar.DATE, onTimeCalendar.get(Calendar.DATE))
    startTimeCalendar.set(Calendar.YEAR, onTimeCalendar.get(Calendar.YEAR))
    startTimeCalendar.set(Calendar.MONTH, onTimeCalendar.get(Calendar.MONTH))
    
    if(onTimeCalendar.get(Calendar.HOUR_OF_DAY) > startTimeCalendar.get(Calendar.HOUR_OF_DAY))
    {
        //Assume start day is tomorrow
        log.debug("Start Time is Tomorrow")
        
        startTimeCalendar.set(Calendar.DATE, onTimeCalendar.get(Calendar.DATE) +1)
    }
    def startTimeDate = startTimeCalendar.getTime()
    
    log.info("Start Time Scheduler: ${startTimeDate}")
    
    //create scheduler to reset variables
    runOnce(startTimeDate, clearVariablesAtStartTime)
}

def createNotificationScheduler(){
    log.trace("Executing createNotificationScheduler")
    if(!isQuietHours())
    {
        log.debug("Set Notification time for ${beforeBedNotificationTime}")
        //Create Reminder to Plug in Car.
        //TODO: Currently Possibility for the OnTime to Occure before the Notificaiton time
        //   Probably something to do with in quiet hours, but assuming same day because of UTC.
        runOnce(beforeBedNotificationTime, notifyUserToPlugIn)
    } else {
        log.info("SSSHHHH No Notification, it's in the quiet times.")
    }
    log.trace("end createNotificationScheduler")
}

////////////////////////////////////////////////////////////////
///// ********************* Calculations **********************/
////////////////////////////////////////////////////////////////

// TODO: Fix Date checking and state saving
// TODO: If alreayd on... do nothing??
def processTemperature(def temperatureToProcess){
    
    if(temperatureToProcess <= onTemperature){
        // So it's oold enough, lets check some stuff out
        
        def rightNowDate = new Date()
        
        if(state.lastActiveScheduleDate == null){
            log.info("Temperature is below threshold")
            createOneTimeSchedulers()
            
            state.lastActiveScheduleDate = convertDatetoISODateString(rightNowDate)
        } else if(!isSameDay(convertISODateStringToDate(state.lastActiveScheduleDate), rightNowDate) && !isSameDay(convertISODateStringToDate(state.onTimeRunOnceDate), rightNowDate)){
            //if(state.lastActiveScheduleDate != todaysDate && state.onTimeRunOnceDate != todaysDate){
            //The low tempurature is going to be cold enough we want to turn on switch.
            log.info("state.lastActiveScheduleDate: ${state.lastActiveScheduleDate}, todays date: ${todaysDate}, state.onTimeRunOnceDate: ${state.onTimeRunOnceDate}")
            log.info("Temperature is below threshold")
            createOneTimeSchedulers()
			
            //Save last scheduled date for later comparisons.
            state.lastActiveScheduleDate = convertDatetoISODateString(rightNowDate)
        } else {
            log.info("Already Scheduled, no need to re-schedule.")
        }
    } else {
        //It's nice and warm.
        log.info("Nice and warm, no worries.")
        
        if(isSameDay(convertISODateStringToDate(state.lastActiveScheduleDate), rightNowDate))
        {
            //I scheduled something for today, but I don't need to any more, the temp changed
            clearTodyasSchedules()
        }
			
    }
}

////////////////////////////////////////////////////////////////
///// *************** Scheduled Job Handlers ******************/
////////////////////////////////////////////////////////////////

def checkThenTurnOnSwitch(){
    log.trace("Executing checkThenTurnOnSwitch")
    def currentTemp = getCurrentTemp()
	
    log.info("Current Temp: ${currentTemp}, On Temp: ${onTemperature}")
	
    //Last Minute Check of tempatrue before turning on
    if(currentTemp <= onTemperature){
        log.info("Turning the Outlets on")
        switches.on()
        //Not sure I'll need state as I probably won't be turning them off in this app... maybe?
        //state.outlets = "on"
    } else{
        log.debug("It's too warm right now, don't need to turn on")
    }
	
    state.onTimeRunOnceDate = null
    
	
    log.trace("End checkThenTurnOnSwitch")
}

def justInCaseCheck(){
    log.trace("Executing justInCaseCheck")
    //Just in case the estimated low is totally differnt than the real temp at start time, lets check.
    checkThenTurnOnSwitch()
    log.trace("End justInCaseCheck")
}

def notifyUserToPlugIn(){
    log.trace("Executing notifyUserToPlugIn")
    log.info("Push Notification Selection ${sendPushMessage}")
    if(sendPushMessage != null && sendPushMessage == "Yes"){
        //sendPush("Plug in your block heater.")
        log.debug("Push Notification: 'Plug in your block heater.'")
    }
    if(phoneNumber){
        sendSms(phoneNumber, "Plug in your block heater.")
    }
    log.trace("End notifyUserToPlugIn")
}

def clearVariablesAtStartTime(){
    log.trace("Executing clearVariablesAtStartTime")
    //Should be null, but just in case    
    state.onTimeRunOnceDate = null
    
    //Need to clear so scheduler works
    state.lastActiveScheduleDate = null
    log.trace("End clearVariablesAtStartTime")
}


////////////////////////////////////////////////////////////////
///// *************** Temperature Helpers *********************/
////////////////////////////////////////////////////////////////
private def getCurrentTemp(){
    return bwsTemperatureMeasurement.latestValue("temperature")
}

private def getCurrentLowTemp(){
    return bwsTemperatureMeasurement.latestValue("lowtemperature")
}

////////////////////////////////////////////////////////////////
///// ***************** Date Helpers **************************/
////////////////////////////////////////////////////////////////
private def CalculateReOccuringOnTime(){
    log.trace("Executing CalculateReOccuringOnTime")
	
    //Convert the start time to Calendar
    def carStartTimeCal = convertDateToCalendar(convertISODateStringToDate(carStartTime))
    carStartTimeCal.set(Calendar.MINUTE, carStartTimeCal.get(Calendar.MINUTE)-minutes)
	
    //Turn back to a date
    def rtvDate = carStartTimeCal.getTime()
    log.trace("End CalculateReOccuringOnTime")
    return rtvDate
}

private def isQuietHours(){
    
    //Convert Everything to Calendars
    def startCalendar = convertDateToCalendar(beforeBedNotificationTime)
    def currentCalendar = convertDateToCalendar(new Date())
    def endCalendar = convertDateToCalendar(carStartTime)
    
    //Convert to minutes for easy comparison
    def startMinutes = startCalendar.get(Calendar.MINUTE) + (startCalendar.get(Calendar.HOUR_OF_DAY) * 60);
    def endMinutes = endCalendar.get(Calendar.MINUTE) + (endCalendar.get(Calendar.HOUR_OF_DAY) * 60);
    def currentMinutes = currentCalendar.get(Calendar.MINUTE) + (currentCalendar.get(Calendar.HOUR_OF_DAY) * 60);
    log.debug("Minutes: start: ${startMinutes} end: ${endMinutes} current: ${currentMinutes}")
    
    if(startMinutes > endMinutes)
    {
        log.info("Quiet Hours Span A Day Into the future")
        //Assuming seperate days
        if(startMinutes < currentMinutes || currentMinutes < endMinutes){
            //Assuming we crossed one day into the future
            sendNotificationEvent("isQuietHours - We crossed into the future: True:  Minutes: start: ${startMinutes} end: ${endMinutes} current: ${currentMinutes}")
            return true
        } else{
            sendNotificationEvent("isQuietHours - We crossed into the future: False:  Minutes: start: ${startMinutes} end: ${endMinutes} current: ${currentMinutes}")
            return false
        }
    } else {
        log.info("Quiet Hours Are on the Same Day")
        //Assume the same day
        
        if(startMinutes < currentMinutes && currentMinutes < endMinutes){
            sendNotificationEvent("isQuietHours - Same Day: True:  Minutes: start: ${startMinutes} end: ${endMinutes} current: ${currentMinutes}")
            return true
        } else {
            sendNotificationEvent("isQuietHours - Same Day: False:  Minutes: start: ${startMinutes} end: ${endMinutes} current: ${currentMinutes}")
            return false
        }
    }
}

private def CalculateOnTime2(){
    log.trace("Executing CalculateOnTime2")
    //TODO Do SOMETHING
    /* Some thoughts
     * - If start time is before Noon
     * - If wakeup time is after noon
     *    - If so following assumptions apply
     * - If even triggers before midnight, but after car start time, assume tomrrow
     * - If even triggers after midnight, but before car start time, assume today
     */
    
    //grab the local timezone
    //def localTimeZone = location.timeZone
    
    //Grab the current time
    def currentTimeCal = convertDateToCalendar(new Date())
	
    //Convert the start time to Calendar
    def carStartTimeCal = convertDateToCalendar(convertISODateStringToDate(carStartTime))
	
    //Convert the notification time to Calendar
    //def beforeBedNOtificationCal = convertDateToCalendar(convertISODateStringToDate(beforeBedNotificationTime))
	
    def carOnTimeCal = convertDateToCalendar(new Date())
	
    //Ensure the days are correct
    def isCarStartTomorrow = false
    //Logic For Tomorrow ... the On Time needs to be less than the current time
    //aka it's already past start time....
    if(//currentTimeCal.get(Calendar.HOUR_OF_DAY) < beforeBedNOtificationCal.get(Calendar.HOUR_OF_DAY) && 
        currentTimeCal.get(Calendar.HOUR_OF_DAY) > carStartTimeCal.get(Calendar.HOUR_OF_DAY))
    {
        log.debug("current hour is great than car start time hour: ${currentTimeCal.get(Calendar.HOUR_OF_DAY)} > ${carStartTimeCal.get(Calendar.HOUR_OF_DAY)}")
        isCarStartTomorrow = true
    } else {
        log.debug("Same Day, this must not be true: ${currentTimeCal.get(Calendar.HOUR_OF_DAY)} > ${carStartTimeCal.get(Calendar.HOUR_OF_DAY)}")
    }
    
    
	
    //log.debug("CalculateOnTime2 - Settings Car Start Time: ${convertISODateStringToDate(carStartTime)}")
    //Make sure date month and year are correct
    carOnTimeCal.set(Calendar.DATE, currentTimeCal.get(Calendar.DATE))
    carOnTimeCal.set(Calendar.YEAR, currentTimeCal.get(Calendar.YEAR))
    carOnTimeCal.set(Calendar.MONTH, currentTimeCal.get(Calendar.MONTH))
    
    //Calculate the Date to make sure it's up to-date: TODO Remove
    def throwMeAway = carOnTimeCal.getTime()
	
    //Set hour and minute
    carOnTimeCal.set(Calendar.HOUR_OF_DAY, carStartTimeCal.get(Calendar.HOUR_OF_DAY))	
    carOnTimeCal.set(Calendar.MINUTE, carStartTimeCal.get(Calendar.MINUTE)-minutes)
	
    //Correct any date offset needed
    if(isCarStartTomorrow && carOnTimeCal.get(Calendar.DATE) <= currentTimeCal.get(Calendar.DATE)){
        //We know the car turns on tomorrow, is block heater on time tomorrow though?
        //Lets get the car start time on the right date
        carStartTimeCal.set(Calendar.DATE, currentTimeCal.get(Calendar.DATE)+1)
        carStartTimeCal.set(Calendar.MONTH, currentTimeCal.get(Calendar.MONTH))
        carStartTimeCal.set(Calendar.YEAR, currentTimeCal.get(Calendar.YEAR))
        //Calculate out the correct date
        def tempCarStartTimeDate = carStartTimeCal.getTime()
        log.info("Car Start Time: ${tempCarStartTimeDate}")
        if(!timeOfDayIsBetween(currentTimeCal.getTime(), tempCarStartTimeDate, carOnTimeCal.getTime(), TimeZone.getTimeZone("UTC")))
        {
            log.debug("CalculateOnTime - Move Date to tomorrow")
            carOnTimeCal.set(Calendar.DATE, currentTimeCal.get(Calendar.DATE)+1)
        } else {
            log.debug("CalculateOnTime - Blockheater on time is the correct day.")
        }
    }
	
    //Check for scheduling in the past problems.
    //if(carOnTimeCal.get(Calendar.DATE) < currentTimeCal.get(Calendar.DATE)){
    //	if(carStartTimeCal.get(Calendar.HOUR_OF_DAY) > carOnTimeCal.get(Calendar.HOUR_OF_DAY)) {
    //		log.info("We are somehow late!")
    //	} else {
    //		log.error("UH HO! We are in the past!")
    //	}
    //SmartThings Build in function
    //def isBetweenTime = timeOfDayIsBetween(carOnTimeCal.getTime(), convertISODateStringToDate(carStartTime), new Date(), location.timeZone)
    //TODO Fix this edge case
    //}
	
    //Turn back to a date
    def rtvDate = carOnTimeCal.getTime()
    log.debug("CalculateOnTime2 - Blockheater On Time: ${rtvDate}")
    
    //log.info("Start Time: ${rtvDate}")
    
    log.debug("currentTimeCal: ${currentTimeCal.getTime()} carStartTimeCal: ${carStartTimeCal.getTime()} carOnTimeCal: ${carOnTimeCal.getTime()} ")
	
    log.trace("End CalculateOnTime2")
    return rtvDate
	
}

private def convertISODateStringToDate(String date){
    try{
        return Date.parse( "yyyy-MM-dd'T'HH:mm:ss.SSSX", date )
    }catch(def e){
        log.error(e)
        return null
    }
}

private def convertDatetoISODateString(Date date){
    try{
        log.debug("Convert Date to String: ${date}")
        def formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        
        //formatter.setTimeZone(location.timeZone)
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
                
        def rtv = formatter.format(date)
                
        log.debug("Convert Date to String Converted: ${rtv}")
        
        return rtv
    }catch(def e){
        log.error(e)
    }
}

private def isSameDay(Date date1, Date date2){

    if(date1 != null && date2 !=null){
        isSameDay(convertDateToCalendar(date1),convertDateToCalendar(date2))
    } else {
        log.error("isSameDay coming back NULL!! date1: ${date1} date2: ${date2}")
        return false
    }
}

private def isSameDay(Calendar cal1, Calendar cal2){
    if(cal1 != null && cal2 !=null){
        //Convert Timzeones to Local (Should be UTC coming in...)
        def localTimeZone = location.timeZone
        cal1.setTimeZone(localTimeZone)
        cal2.setTimeZone(localTimeZone)
        
        //check month, day and year to make sure it's the same day.
        if(
            cal1.get(Calendar.DATE)== cal2.get(Calendar.DATE)
            && cal1.get(Calendar.MONTH)== cal2.get(Calendar.MONTH)
            && cal1.get(Calendar.YEAR)== cal2.get(Calendar.YEAR)
        ) 
        {
            //same day
            return true
        } else {
            //Not the same day
            return false
        }
    } else {
        log.error("isSameDay coming back NULL!! Cal1: ${cal1} Cal2: ${cal2}")
        return false
    }
}

//Convert from string to date.
private def convertDateToCalendar(String date){
    Calendar rtv = null
	
    def d = convertISODateStringToDate(date)
	
    if(d != null){
        rtv = convertDateToCalendar(d) 
    } else {
        log.error("convertDateToCalendar string is null: ${date}")
    }

    return rtv
}

private def convertDateToCalendar(Date date){
    def cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.setTime(date)
	
    //Now Set time from date, have to do it manually - Assume date coming in has been converted to UTC
    //    cal.set(Calendar.DATE, date.getDate())
    //    cal.set(Calendar.MONTH, date.getMonth())
    //    cal.set(Calendar.YEAR, date.getYear())
    //    cal.set(Calendar.HOUR_OF_DAY, date.getHours())
    //    cal.set(Calendar.MINUTE, date.getMinutes())
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    
    return cal
}

//TODO: Do I even need this anymore?
private def getJustDate(date){
	
    //Quick null check
    if(date == null){
        return null
    }
	
    
    def cal = null
    if (date instanceof Date) {
        //get unix time
        cal = convertDateToCalendar(date)
    } else if(date instanceof Calendar){
        cal = date
    }
	
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
	
    //Turn back to a date
    def dateWithoutTime = cal.getTime()
    
    return dateWithoutTime
}


////////////////////////////////////////////////////////////////
///// ***************** Scheduler Helpers *********************/
////////////////////////////////////////////////////////////////

private def clearAllSchedules(){
    log.trace("Executing clearAllSchedules")
    // remove all scheduled executions for this SmartApp install
    unschedule()
	
    //Schedule back up the daily check
    createDailyScheduler()
	
    log.trace("End clearAllSchedules")
}

private def clearTodyasSchedules(){
    log.trace("Executing clearTodyasSchedules")
    log.info("Clearing Schedulers for Daily on and Notifications ")
    // unschedule the schedulers
    unschedule(notifyUserToPlugIn)
    unschedule(checkThenTurnOnSwitch)
    unschedule(clearVariablesAtStartTime)
    
    //Reset State Elements
    state.lastActiveScheduleDate = null 
    state.onTimeRunOnceDate = null
    
    log.trace("End clearTodyasSchedules")
}