/**
 *  Master Scheduler
 *
 *  Copyright 2015 Corey Jackson
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
    name: "Master Scheduler",
    namespace: "coreyrjackson",
    author: "Corey Jackson",
    description: "Master Scheduler",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
/*
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("Change HVAC mode when the outside temperature is...") {
		input "temperature1", "number", title: "Temp Degrees Fahrenheit?"
	}
    */
    
  section("People to Watch") {
    input "people", "capability.presenceSensor", multiple: true
  }
  
    section("Change to this mode to...") {
    input "newAwayMode",    "mode", title: "Everyone is away"
    input "newSunsetMode",  "mode", title: "At least one person home and nightfall"
    input "newSunriseMode", "mode", title: "At least one person home and sunrise"
  }

  section("Away threshold (defaults to 10 min)") {
    input "awayThreshold", "decimal", title: "Number of minutes", required: false
  }
  
	section("Choose thermostat... 4/3/2016 5:52 PM ") 
    {
		input "thermostat", "capability.thermostat"
	}
    
    section("Monday thru Friday Schedule") 
        {
		input ("time1", "time", title: "Wake Time of Day")	
		input ("tempSetpoint1", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
		//input ("tempSetpointA", "number", title: "Wake Cool Temp Degrees Fahrenheit?")
		input ("time2", "time", title: "Leave Time of Day")
		input ("tempSetpoint2", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
		//input ("tempSetpointB", "number", title: "Leave Cool Temp Degrees Fahrenheit?")
		input ("time3", "time", title: "Return Time of Day")
		input ("tempSetpoint3", "number", title: "Return Heat Degrees Fahrenheit?")
		//input ("tempSetpointC", "number", title: "Return Cool Degrees Fahrenheit?")
		input ("time4", "time", title: "Sleep Time of Day")
		input ("tempSetpoint4", "number", title: "Sleep Heat Degrees Fahrenheit?")
		//input ("tempSetpointD", "number", title: "Sleep Cool Degrees Fahrenheit?")
	}
    
    /*
    section("Saturday and Sunday Schedule") 
    {
		input ("time11", "time", title: "Wake Time of Day")	
		input ("tempSetpoint11", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
		//input ("tempSetpointAA", "number", title: "Wake Cool Temp Degrees Fahrenheit?")
		input ("time21", "time", title: "Leave Time of Day")
		input ("tempSetpoint21", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
		//input ("tempSetpointBB", "number", title: "Leave Cool Temp Degrees Fahrenheit?")
		input ("time31", "time", title: "Return Time of Day")
		input ("tempSetpoint31", "number", title: "Return Heat Degrees Fahrenheit?")
		//input ("tempSetpointCC", "number", title: "Return Cool Degrees Fahrenheit?")
		input ("time41", "time", title: "Sleep Time of Day")
		input ("tempSetpoint41", "number", title: "Sleep Heat Degrees Fahrenheit?")
		//input ("tempSetpointDD", "number", title: "Sleep Cool Degrees Fahrenheit?")
	}
    */
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	state.LastRunDate = new Date()
	CheckForWork()    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
    CheckForWork()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    
	SetSchedule()
    
}

def BuildToDoActions()
{
	/*
	def actions = [
    	[key:0, time:time11, temp: tempSetpoint11, DayofWeek: ["Saturday","Sunday"]],

        [key:1, time:time21, temp: tempSetpoint21, DayofWeek: ["Saturday","Sunday"]],
        [key:2, time:time31, temp: tempSetpoint31, DayofWeek: ["Saturday","Sunday"]],
        [key:3, time:time41, temp: tempSetpoint41, DayofWeek: ["Saturday","Sunday"]]
    	]

    
    // `entry` is a map entry
	actions.each  { item ->
    	log.debug("Name: $item.key $item.time Temperature: $item.temp DayofWeek: $item.DayofWeek")
        
        
        def hour = Date.parseToStringDate(item.time).format("H", location.timeZone)
        
        log.debug("$hour:")
    }
	*/
    
}


// TODO: implement event handlers

def CheckForWork()
{
	log.debug("corey")
	def curMode = location.currentMode
	
	def today = new Date()
    sendNotificationEvent("Master Scheduler: Looking for Work\ntoday=${today}\nPrevious check=${state.LastRunDate}\n  Current Mode=${curMode}\n${time1} \n${time2} \n${time3} \n${time4} ")
    log.debug("LastRunDate=${state.LastRunDate}")
	log.debug("today=${today}")
    log.debug("Current Mode=${curMode}")
    
    log.debug("Do Some Work")
    //Set the correct presence.
    log.debug("Check for preseence")
    AnyoneHome()
    
    //Build list of tasks
    log.debug("Start BuildToDoActions")
    BuildToDoActions()
    log.debug("End BuildToDoActions")
    
    state.LastRunDate = new Date()
    log.debug("New LastRunDate=${state.LastRunDate}")
    
    
}

def AnyoneHome()
{
	log.debug("Current Mode: $location.mode")
  
	if(everyoneIsAway()) 
    {
    	if(location.mode != newAwayMode) 
        {
			def message = "${app.label} changed your mode to '${newAwayMode}' because everyone left home"
			log.debug(message)
			setLocationMode(newAwayMode)
    	}
    	else 
        {
            log.debug("Mode is the same, not evaluating")
        }
	}
	else
    {
    	log.info("Somebody returned home before we set to '${newAwayMode}'")
  	}
}


def SetSchedule()
{
	runEvery5Minutes(CheckForWork)
}



private everyoneIsAway() {
  def result = true

  if(people.findAll { it?.currentPresence == "present" }) {
    result = false
  }

  log.debug("everyoneIsAway: ${result}")

  return result
}
