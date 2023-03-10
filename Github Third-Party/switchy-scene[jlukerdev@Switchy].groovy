/**
 *  Switchy.Scene
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
	parent: "jlukerdev:Switchy.Scenes",
    name: "Switchy.Scene",
    namespace: "jlukerdev",
    author: "Josh Luker",
    description: "Automatically sets a lighting scene when light is turned on",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png")


preferences
{
	// dynamic pages... page name must match the method name that loads the page
	page(name: "setupScene") // main page
    page(name: "setTimeRangeOptions") // scheduling options
    page(name: "setCapabilityOptions") //capability options
    
    page(name: "setupName", title: "Set Name", install: true, uninstall: false) // app name
    {
        section([mobileOnly:true]) 
        {
            label required: true
        }
    }
}

def setupScene()
{
	// defines the main Page
    
    // set text to show in setCapabilityOptions href desc
    def capabilityTypeDesc = "Tap to set"
    if (capabilityType)
    {
		capabilityTypeDesc = capabilityType
	}
  
  	// get what options are to be available
  	def isLevelAvail = isLevelCapabilitySet()
    def isTempAvail = isTemperatureCapabilitySet()
    
	// set text to show in scheduling href desc
    def timeFromTo = "Tap to set"
    if (isScheduleSet())
    {
        // show the existing set time in href desc
        timeFromTo = getStartAndEndTimeDesc()
    }
    
	dynamicPage(name: "setupScene", nextPage: "setupName", uninstall: true) 
    {
    	section("Set the scene options...")
        {
			href(name: "href",
                 required: false,
                 title: "Scene Type",
                 description: "$capabilityTypeDesc",
                 page: "setCapabilityOptions") // this = method name that loads the page
    	}
   
   		// if capabilityType is set, show the next options
        if (capabilityType)
        {
        	// show value inputs based on the capability-type selected
        	// section("Set the scene..."){}
            if (isLevelAvail)
            {
            	section() {
                	input "lightLevelValue", "number", required: true, title: "Level (%)"
                }
            }
            if (isTempAvail)
            {
				section() {
                	input "lightTempValue", "number", required: true, title: "Temperature (K)"
                }
            }
        
        	// show light selections based on the capability-type selected
            if (isLevelAvail | isTempAvail)
            {
                if (isLevelAvail)
                {
                    section("Set the lights to control Level for...")
                    {
                        input "lightsLevel", "capability.switchLevel", required: true, multiple: true
                    }
                }
                
                if (isTempAvail)
                {
                	section("Set the lights to control Temperature for...")
                    {
                        if (!useOneLightsList)
                        {
                            // dont show if option below is set
                            input "lightsTemp", "capability.colorTemperature", required: true, multiple: true
                        }
                        if (isLevelAvail)
                        {
                            // option to use the same lights as selected for level
                            input "useOneLightsList", "bool", title: "Use same lights as Level", required: false, submitOnChange: true
                        }
                    }
                } 
            }
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

def setCapabilityOptions()
{
	//setup the lighting capabilities for the scene
    dynamicPage(name: "setCapabilityOptions") 
    {
    	section()
        {
        	input(name: "capabilityType", type: "enum", title: "Scene type", options: ["Level", "Temperature", "Level and Temperature"], required: true, multiple: false, submitOnChange: true)
        }
    }
}
def isLevelCapabilitySet()
{
	return capabilityType == "Level" || capabilityType == "Level and Temperature"
}
def isTemperatureCapabilitySet()
{
	return capabilityType == "Temperature"  || capabilityType == "Level and Temperature"
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
	if (lightLevelValue)
    {
        subscribe(lightsLevel, "switch.On", lightTurnedOn)
        logEvent("Subscribed level event for $lightsLevel", true)
    }

    if (lightTempValue)
    {
        if (useOneLightsList)
        {
			logEvent("Using same lights-list for level and temperature", true)
        }
        else
        {
			subscribe(lightsTemp, "switch.On", lightTurnedOn)
        	logEvent("Subscribed temp event for $lightsTemp", true)
        }
    }
}

def lightTurnedOn(evt)
{
	// see if event falls within a set schedule
    if (!checkTimeInterval())
	{
		logEvent("Event not in schedule", true)
        return;
	}
	
    def lightDev = evt.getDevice()
    def devId = lightDev.getId()
	logEvent("${lightDev.displayName} was turned on", true)
    
    // set the device states according to the scene
    def isLevelAvail = isLevelCapabilitySet()
    def isTempAvail = isTemperatureCapabilitySet()
    
    // level
    if (isLevelAvail)
    {	
    	// make sure device is in the list of level-lights
        if (isDeviceInList(lightsLevel.getId(), devId))
        {
            def curLevel = lightDev.currentLevel
            logEvent("Current level is ${curLevel}%")
            if (curLevel != lightLevelValue)
            {
            	def setToLevel = lightLevelValue
                if (setToLevel < 0)
                    setToLevel = 0
                else if (setToLevel > 100)
                    setToLevel = 100

                logEvent("Setting level to $setToLevel")
                lightDev.setLevel(setToLevel)
            }
            else
            {
                logEvent("Level was not changed")
            }
        }
    }
	
	// set temp, if applicable
	if (isTempAvail)
	{
    	def setTemp = true
    	if (!useOneLightsList)
    		setTemp = isDeviceInList(lightsTemp.getId(), devId) // use temp-lights list
        else
        	setTemp = isDeviceInList(lightsLevel.getId(), devId) // use same list as level-lights
        	
        if (setTemp)
        {
            logEvent("Checking for temperature capability...")
            if (lightDev.hasAttribute("colorTemperature"))
            {
                def currentTemp = lightDev.latestValue("colorTemperature")
                logEvent("Current temp is ${currentTemp}K")
                if (currentTemp != lightTempValue)
                {
                	def setToTemp = lightTempValue
                	if (setToTemp < 3000)
                        setToTemp = 3000
                    else if (setToTemp > 6500)
                        setToTemp = 6500
                
                    logEvent("Setting temp to $setToTemp")
                    lightDev.setColorTemperature(setToTemp)
                }
                else
                {
                    logEvent("Temp was not changed")
                }
            }
        }
	}
}

def isDeviceInList(idsList, id)
{
	logEvent("Id's list is $idsList")
    return idsList.contains(id)
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