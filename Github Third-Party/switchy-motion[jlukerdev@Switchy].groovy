/**
 *  SwitchyMotion
 *
 *  Copyright 2017 Josh Luker
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
    parent: "jlukerdev:Switchy.Motions",
    name: "Switchy.Motion",
    namespace: "jlukerdev",
    author: "Josh Luker",
    description: "Contols lights with a motion sensor. Leaves lights on when motion stops if the light was already on when motion was detected",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png")


preferences
{
	// dynamic pages... page name must match the method name that loads the page
	page(name: "setupMonitoring") // main page
    page(name: "setTimeRangeOptions") // scheduling options
    
    page(name: "setupName", title: "Set Name", install: true, uninstall: false) // app name
    {
        section([mobileOnly:true]) 
        {
            label required: true
        }
    }
}

def setupMonitoring()
{
	// defines the main Page
    
	// set text to show in scheduling href dec
    def timeFromTo = "Tap to set"
    if (isScheduleSet())
    {
        // show the existing set time in href desc
        timeFromTo = getStartAndEndTimeDesc()
    }
        
	dynamicPage(name: "setupMonitoring", nextPage: "setupName", uninstall: true) 
    {
    	section("When there's movement...") 
        {
            input "motionSensor", "capability.motionSensor", required: true, title: "Where?"
        }
        
        section("Turn on these lights...") 
        {
            input "lights", "capability.switch", required: true, multiple: true
        }
        
        section("Override motion detection with...") 
        {
        	paragraph "If this switch (or light) is on when motion is detected, the motion detector will not turn the lights off when motion stops."
            input "ovrSwitch", "capability.switch", required: true, title: "Switch"
        }
        
        section("Turn off lights when movement stops after...") 
        {
            input "seconds", "number", required: true, title: "Seconds"
        }
              
    	section("Only during this time...")
        {
			href(name: "href",
                 required: false,
                 title: "Schedule",
                 description: "$timeFromTo",
                 page: "setTimeRangeOptions") // this = method name that loads the page
    	}
    }
}

def setTimeRangeOptions() 
{
	// setup the time-range page
    // note that each input has submitOnChange: true so their values will be available in the main page
    
    dynamicPage(name: "setTimeRangeOptions", title: "Set start and end times") 
    {
    	// get starting time
    	section()
        {
			input(name: "startTimeOption", title: "Starting at", type: "enum", options: ["A Specific Time","Sunrise","Sunset"], required: false, submitOnChange: true)
		}
        
        // if a start-time is set, add options for handling the time
        if (isScheduleSet())
        {	
            if (isTimeOptionATime(startTimeOption))
            {
            	// get entered time
                section() {
                	input(name: "startingTime", type: "time", required: true, submitOnChange: true);
                }
            }
            else
            {
            	// get entered sun offset ( + or -)
                section() {
                	input(name: "startingOffset", title: "$startTimeOption offset in minutes (+ -)?", type: "number", required: false, submitOnChange: true);
                }
            }
            
            // get ending time
            section()
            {
                input(name: "endTimeOption", title: "Ending at", type: "enum", options: ["A Specific Time","Sunrise","Sunset"], required: true, submitOnChange: true)
            }
            
            // if a end-time is set, add options for handling the time
            if (endTimeOption)
            {
            	if (isTimeOptionATime(endTimeOption))
                {
                    // get entered time
                    section() {
                        input(name: "endingTime", type: "time", required: true, submitOnChange: true);
                    }
                }
                else
                {
                    // get entered sun offset ( + or -)
                    section() {
                        input(name: "endingOffset", title: "$endTimeOption offset in minutes (+ -)?", type: "number", required: false, submitOnChange: true);
                    }
                }
            }
        }
    }
}


def installed() 
{
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() 
{
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() 
{
	subscribe(motionSensor, "motion.active", motionActiveHandler)
    subscribe(motionSensor, "motion.inactive", motionInactiveHandler)
}

def motionActiveHandler(evt) 
{
	// check if override switch is currently on
    // this will prevent no-motion from turning the light back off
    def switchState = ovrSwitch.currentValue("switch") // = "on" or "off"

	if (switchState == "off")
    {
    	// see if motion falls within a set schedule
    	if (!checkTimeInterval())
        	return;
  
        logEvent("Motion Detected, turning lights on", true)
    	state.didMotionTurnOn = true // flag that motion turned switch on
        lights.on()
    }
    else if (state.didMotionTurnOn != true)
    {
    	// switch was already on before motion started
   		logEvent("override switch is on, ignoring motion", true)
    	state.didMotionTurnOn = false
    }
}

def motionInactiveHandler(evt) 
{
	if (state.didMotionTurnOn == true) // did motion detector turn the light on ?
    {
    	logEvent("Motion Stopped")
        runIn(seconds, checkMotion)
    }
}

def checkMotion()
{
	// turn off if still no motion
	logEvent("In checkMotion scheduled method")

    // get the current state object for the motion sensor
    def motionState = motionSensor.currentState("motion")

    if (motionState.value == "inactive") 
    {
    	logEvent("turning lights off", true)
        lights.off()
        state.didMotionTurnOn = false
    } 
    else 
    {
    	// Motion active; just log it and do nothing
        logEvent("Motion is active, do nothing and wait for inactive", true)
    }
}

def checkTimeInterval()
{
	// check to see if event falls within a set schedule
    
	if (!isScheduleSet())
    	return true; // no schedule is set
        
    def currentTime = new Date(now())
    def sunrise
    def sunset
    Date fromTime
    Date toTime
     
    // are start and end times a sun event ?
    def isStartASunTime = !isTimeOptionATime(startTimeOption) 
    def isEndASunTime = !isTimeOptionATime(endTimeOption) 
    
    if (isStartASunTime || isEndASunTime)
    {
		// get sunrise and sunset times
        def sunRiseSet = getSunriseAndSunset() // built-in func
        sunrise = sunRiseSet.sunrise
        sunset = sunRiseSet.sunset
        logEvent("sunrise time is " + toShortDateTimeString(sunrise))
    	logEvent("sunset time is "  + toShortDateTimeString(sunset))
	}  
       
    // get time values
    if (!isStartASunTime)
    {
    	logEvent("Using specific time for start")
    	fromTime = timeToday(startingTime) // get time for today's date
    }
    else
    {
    	logEvent("Using sun-time for start")
       	fromTime = getSunTimeToUse(startTimeOption, sunrise, sunset, startingOffset)
    }
    
    if (!isEndASunTime)
    {
    	logEvent("Using specific time for end")
    	toTime = timeToday(endingTime) // get time for today's date
    }
    else
    {
    	logEvent("Using sun-time for end")
        toTime = getSunTimeToUse(endTimeOption, sunrise, sunset, endingOffset)
    }
    
    // see if toTime should be pushed to tomorrow
    toTime = timeTodayAfter(to24HourTimeString(fromTime), to24HourTimeString(toTime), location.timeZone)
    
    // determine if current time falls within the set schedule
    def isBetween = timeOfDayIsBetween(fromTime, toTime, currentTime, location.timeZone)
    
    logEvent("Current time is " + toShortDateTimeString(currentTime))
   	logEvent("Starting time is " + toShortDateTimeString(fromTime))
    logEvent("Ending time is " + toShortDateTimeString(toTime))
    logEvent("isBetween time range = $isBetween")
    
    return isBetween
}

def getSunTimeToUse(sunOption, sunrise, sunset, offset)
{
	// get the sun-time based on the set option
    Date targetTime
    logEvent("sunOption = $sunOption")
 	if (sunOption == "Sunrise")
    {
    	targetTime = sunrise
    }
    else
    {
    	targetTime = sunset
    }
   
   	if (offset && offset != 0)
    {
    	def offsetMS = timeOffset(offset) // get offset in ms
        targetTime = new Date(targetTime.time + offsetMS) // put offset into the sun-time
    }
   
    return targetTime
}

def isScheduleSet()
{
	// is a schedule set ?
    return startTimeOption != null
}

def isTimeOptionATime(option)
{
	return option == "A Specific Time"
}

def getStartAndEndTimeDesc()
{
	// gets the starting and endtimes as descriptive string
    // shown in the mobile app UI to display an existing set schedule instead of "tap to set"
    def start = getTimeString(startTimeOption, startingTime, startingOffset)
    def end = getTimeString(endTimeOption, endingTime, endingOffset)
    return "$start to $end"
}

def getTimeString(option, specTime, sunOffset)
{
	// get formatted time-string for display in UI
    
	if (isTimeOptionATime(option))
    {
    	return toShortTimeString(specTime)
    }
    else
    {
    	// sun-event time. shows any offset
    	def minutes = ""
        if (sunOffset)
        {
        	if (sunOffset > 0)
            	minutes = " +$sunOffset"
            else if (sunOffset < 0)
            	minutes = " $sunOffset"
                
            if (minutes != "")
             	minutes += "m"
        }
    	return "$option$minutes"
    }

}

def toShortTimeString(time)
{       
	// returns time (string) as formatted short time-string
	// toDateTime is the magic fairy dust for converting the timestring to a Date
    return toDateTime(time, location.timeZone).format("h:mm a", location.timeZone)
}

def to24HourTimeString(time)
{
	// returns time (string) as formatted short time-string in 24hr
	// toDateTime is the magic fairy dust for converting the timestring to a Date
    return toDateTime(time, location.timeZone).format("HH:mm", location.timeZone)
}

def toShortDateTimeString(dateTime)
{
	return dateTime.format("M/d/YY h:mm a", location.timeZone)
}

def logEvent(text, alwaysLog = false)
{
	// set to false to disable general logging
	def isLoggingEnabled = (false || alwaysLog)
    if (isLoggingEnabled)
    {
    	log.debug(text)
    }
}