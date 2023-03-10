/**
 *  Copyright 2015 SmartThings
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
 *  Keep Me Cozy
 *
 *  Author: SmartThings
 */
definition(
    name: "Thermostat Control w/ Remote Sensor",
    namespace: "crockerg",
    author: "mwoodengr@hotmail.com",
    description: "Provides a 5+2 day HVAC control for one thermostat",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

/**
 *  Automatic HVAC Program
 *
 *  Original Author: mwoodengr@hotmail.com
 *  Date: 2014-01-28
 *
 *  Current Author: greg.crocker@gmail.com
 *  v1.4: 2016-07-17 - Initial capability complete, still need to work out bug related to hold temperature not setting
 *  v1.5: 2017-12-15 - Adding in remote sensor
 */
preferences {

	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}

    section("Optionally choose temperature sensor to use instead of the thermostat's... ") {
		input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: false
	}
    
    section("Top Level") {
        	input ("runMode", "enum", title: "Heat/Cool/Off", options: ["heat", "cool", "off"], defaultValue: "cool")
        	input ("runState", "enum", title: "Schedule/Hold", options: ["schedule", "hold"], defaultValue: "schedule")
    }
        
        
        section("Hold Settings") {
        	input ("holdTemp", "number", title: "Hold Temp")
            input ("holdFan", "enum", title: "Hold Fan", options: ["auto", "on"], defaultValue: "auto")
        }
        
        section("Monday thru Friday Schedule") {
		
        	input ("timeWakeMF", "time", title: "Wake Time of Day")	
			input ("tempSetpointWakeMF", "number", title: "Wake Temp Degrees Fahrenheit?")
        	input ("fanWakeMF", "enum", title: "Wake Fan", options: ["auto", "on"], defaultValue: "auto")
		
    	    input ("timeLeaveMF", "time", title: "Leave Time of Day")
			input ("tempSetpointLeaveMF", "number", title: "Leave Temp Degrees Fahrenheit?")
        	input ("fanLeaveMF", "enum", title: "Leave Fan", options: ["auto", "on"], defaultValue: "auto")
		
        	input ("timeReturnMF", "time", title: "Return Time of Day")
			input ("tempSetpointReturnMF", "number", title: "Return Degrees Fahrenheit?")
        	input ("fanReturnMF", "enum", title: "Return Fan", options: ["auto", "on"], defaultValue: "auto")
		
    	    input ("timeSleepMF", "time", title: "Sleep Time of Day")
			input ("tempSetpointSleepMF", "number", title: "Sleep Degrees Fahrenheit?")
       		input ("fanSleepMF", "enum", title: "Sleep Fan", options: ["auto", "on"], defaultValue: "auto")
    	}
        
    	section("Saturday and Sunday Schedule") {
		
        	input ("timeWakeWE", "time", title: "Wake Time of Day")	
			input ("tempSetpointWakeWE", "number", title: "Wake Temp Degrees Fahrenheit?")
        	input ("fanWakeWE", "enum", title: "Wake Fan", options: ["auto", "on"], defaultValue: "auto")
		
        	input ("timeLeaveWE", "time", title: "Leave Time of Day")
			input ("tempSetpointLeaveWE", "number", title: "Leave Temp Degrees Fahrenheit?")
        	input ("fanLeaveWE", "enum", title: "Leave Fan", options: ["auto", "on"], defaultValue: "auto")
		
        	input ("timeReturnWE", "time", title: "Return Time of Day")
			input ("tempSetpointReturnWE", "number", title: "Return Degrees Fahrenheit?")
        	input ("fanReturnWE", "enum", title: "Return Fan", options: ["auto", "on"], defaultValue: "auto")
		
        	input ("timeSleepWE", "time", title: "Sleep Time of Day")
			input ("tempSetpointSleepWE", "number", title: "Sleep Degrees Fahrenheit?")
        	input ("fanSleepWE", "enum", title: "Sleep Fan", options: ["auto", "on"], defaultValue: "auto")
		}
}

def installed()
{        
	commonInit("installed")
}

def updated()
{
	unsubscribe()
    unschedule()
                
	commonInit("updated")
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()
}

def commonInit(parentFunc) {
    if (runMode == "off") {
    	log.debug "Exiting SmartApp as the HVAC mode is set to OFF"
    	return
    }
    
   	thermostat.setThermostatMode(runMode)
    
    state.lastTemp = 0
    state.lastFanMode = "invalid"
    
    log.debug "Thermostat $thermostat is running SmartApp v1.7 $parentFunc w/o logging"
  	
    subscribe(thermostat, "thermostat", thermostatHandler)

	if (runState == "hold") {
    	log.debug "Running SmartApp hold at $holdTemp with fan $holdFan"
        setTemp(holdTemp, holdFan)
    } else {
    	log.debug "Running SmartApp with Schedule"
        runEvery5Minutes("updateTemp")
   	    updateTemp()
    }
}

// This function determines which day it is, what event triggered, and sets the appropriate temperature
def updateTemp(evt) {
	 
	def calendar = Calendar.getInstance()
	calendar.setTimeZone(location.timeZone)
	def dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
	switch (dayOfWeek) {
    	case Calendar.MONDAY:
        case Calendar.TUESDAY:
        case Calendar.WEDNESDAY:
        case Calendar.THURSDAY:
        case Calendar.FRIDAY:        
            processState(timeWakeMF, timeLeaveMF, timeReturnMF, timeSleepMF, tempSetpointWakeMF, 
            			 tempSetpointLeaveMF, tempSetpointReturnMF, tempSetpointSleepMF,
                         fanWakeMF, fanLeaveMF, fanReturnMF, fanSleepMF)
            break
        
        case Calendar.SATURDAY:
        case Calendar.SUNDAY:
            processState(timeWakeWE, timeLeaveWE, timeReturnWE, timeSleepWE, tempSetpointWakeWE,
            			 tempSetpointLeaveWE, tempSetpointReturnWE, tempSetpointSleepWE,
                         fanWakeWE, fanLeaveWE, fanReturnWE, fanSleepWE)
            break
        
        // Not really sure how this would happen, but for now default to weekend programming
        // and just log a message
        default:
        	log.debug("Reached the default statement in updateTemp() -> switch(dayOfWeek)")
            break   	
	}
    
    // Run the check using the sensor temperature
    evaluate()
}

/**
 * Figure out where we are in the processing by looking at the current time and comparing
 * against the programming values.  Set the temperature accordingly using setTemp
 *
 * TBD - Figure out how to automatically determine the timezone based on location
 */
def processState (timeWake, timeLeave, timeReturn, timeSleep, 
                  tempWake, tempLeave, tempReturn, tempSleep,
                  fanWake, fanLeave, fanReturn, fanSleep) {
	def calendar = Calendar.getInstance()
	calendar.setTimeZone(location.timeZone)
    def nowTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    
//    log.debug "nowTime = $nowTime"
    def ctWake = convertTime(timeWake)
    def ctLeave = convertTime(timeLeave)
    def ctReturn = convertTime(timeReturn)
    def ctSleep = convertTime(timeSleep)
    
    if (ctWake > nowTime) {
        setTemp(tempSleep, fanSleep)
    } else if (ctWake <= nowTime && ctLeave > nowTime) {
        setTemp(tempWake, fanWake)
    } else if (ctLeave <= nowTime && ctReturn > nowTime) {
        setTemp(tempLeave, fanLeave)
    } else if (ctReturn <= nowTime & ctSleep > nowTime) {
        setTemp(tempReturn, fanReturn)
    } else if (ctSleep <= nowTime) {
        setTemp(tempSleep, fanSleep)
    } else {
    	log.debug("Now ($nowTime) was not within programmed range.")
    }
}

/**
 * This function converts the time field provided by the SmartApp into the number of minutes in a day.
 * 
 * Ugh, hard coded numbers, but this is tied to ISO-8601 date string so it is unlikely to change
 */
def convertTime(scheduledTime) {
    def HR_START  = 11
    def HR_END    = 13
    def MIN_START = 14
    def MIN_END   = 16
    
    def sb = new StringBuilder (scheduledTime)
    def hr = sb.substring (HR_START, HR_END)
    def min = sb.substring (MIN_START, MIN_END)
    def minsInDay = Integer.parseInt(hr)*60 + Integer.parseInt(min);

//	log.debug "scheduledTime = $scheduledTime :: convertTime = $minsInDay"

	return minsInDay;
}

// Set the actual temperature based on the heat/cool setting
def setTemp(temp, fanMode) {      
	if (temp != state.lastTemp) {
    	log.debug "Set temp = $temp"
    
   		if (runMode == "heat"){
			thermostat.setHeatingSetpoint(temp) 
            state.heatSetpoint = temp
        }
      	else {
        	thermostat.setCoolingSetpoint(temp)
            state.coolSetpoint = temp
        }
        state.lastTemp = temp
    }
        
    if (fanMode != state.lastFanMode) {
     	thermostat.setThermostatFanMode(fanMode)
        state.lastFanMode = fanMode
        log.debug "Set fan mode = $fanMode"
    }
}

def temperatureHandler(evt)
{
	log.debug ("In temperatureHandler")
    evaluate()
}

private evaluate()
{
	  if (sensor) {
		def threshold = 1.0
		def tm = thermostat.currentThermostatMode
		def ct = thermostat.currentTemperature
		def currentTemp = sensor.currentTemperature
        def heatingSetpoint = state.heatSetpoint
        def coolingSetpoint = state.coolSetpoint
        
		log.trace("evaluate:, mode: $tm -- temp: $ct, heat: $thermostat.currentHeatingSetpoint, cool: $thermostat.currentCoolingSetpoint -- "  +
			"sensor: $currentTemp, heat: $heatingSetpoint, cool: $coolingSetpoint")
		if (tm in ["cool","auto"]) {
			// air conditioner
			if (currentTemp - coolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(ct - 2)
				log.debug "thermostat.setCoolingSetpoint(${ct - 2}), ON"
			}
			else if (coolingSetpoint - currentTemp >= threshold && ct - thermostat.currentCoolingSetpoint >= threshold) {
				thermostat.setCoolingSetpoint(ct + 2)
				log.debug "thermostat.setCoolingSetpoint(${ct + 2}), OFF"
			}
		}
		if (tm in ["heat","emergency heat","auto"]) {
			// heater
			if (heatingSetpoint - currentTemp >= threshold) {
				thermostat.setHeatingSetpoint(ct + 2)
				log.debug "thermostat.setHeatingSetpoint(${ct + 2}), ON"
			}
			else if (currentTemp - heatingSetpoint >= threshold && thermostat.currentHeatingSetpoint - ct >= threshold) {
				thermostat.setHeatingSetpoint(ct - 2)
				log.debug "thermostat.setHeatingSetpoint(${ct - 2}), OFF"
			}
		}
	}
	else {
		thermostat.setHeatingSetpoint(heatingSetpoint)
		thermostat.setCoolingSetpoint(coolingSetpoint)
		thermostat.poll()
	}
}



def thermostatHandler(evt) {
	log.debug ("In thermostatHandler")
}


/**
preferences() {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting..." ) {
		input "heatingSetpoint", "decimal", title: "Degrees"
	}
	section("Air conditioning setting...") {
		input "coolingSetpoint", "decimal", title: "Degrees"
	}
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
	subscribeToEvents()
}


def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()
}
*/

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}


// for backward compatibility with existing subscriptions
def coolingSetpointHandler(evt) {
	log.debug "coolingSetpointHandler()"
}
def heatingSetpointHandler (evt) {
	log.debug "heatingSetpointHandler ()"
}