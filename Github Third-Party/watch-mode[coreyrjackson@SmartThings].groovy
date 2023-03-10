/**
 *  Watch Mode
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
    name: "Watch Mode",
    namespace: "coreyrjackson",
    author: "Corey Jackson",
    description: "Watch for mode changes and perform actions based on the new mode",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Vacation Settings") {
		// TODO: put inputs here
	      }  
   	section("Choose thermostat... 11/30/2015 9:56 AM ") 
    {
		input "thermostat", "capability.thermostat"
	}
     
    section("Vacation: Select switches to control...") {
		input name: "VacationSwitches", type: "capability.switch", multiple: true
	}
	section("Vacation: Turn them all on at...") {
		input name: "VacationstartTime", title: "Turn On Time?", type: "time"
	}
	section("Vacation: And turn them off at...") {
		input name: "VacationstopTime", title: "Turn Off Time?", type: "time"
	}

	section("Vacation: Thermostat Temp") {
        input ("ThermostatTemp", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
	}
    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
        subscribe(location, "mode", modeChangeHandler)
        
            //SetVacation()
}

// TODO: implement event handlers

def modeChangeHandler(evt) {
    log.debug "mode changed to ${evt.value}"
	sendNotificationEvent("Watch Mode App: mode changed to ${evt.value}")
    
    //Mode has changed, unschedule events
    unschedule()
    

    
    switch (evt.value)
    {
    	case "Vacation":
        	SetVacation()
    		break
    	default:
    		break
    
    }
}

def SetVacation()
{
    //schedule("2015-01-09T15:36:00.000-0600", handlerMethod)
	unschedule()
	//schedule(startTime, "startTimerCallback")
    	log.debug ("Schedule Start  ${VacationstartTime}")
        sendNotificationEvent("Schedule Start  ${VacationstartTime}")
    schedule(VacationstartTime, "startTimerCallback")
	//schedule(stopTime, "stopTimerCallback")
	log.debug ("schedule Stop ${VacationstopTime}")
    sendNotificationEvent("schedule Stop ${VacationstopTime}")
    schedule(VacationstopTime, "stopTimerCallback")
    	thermostat.setHeatingSetpoint(ThermostatTemp)
            sendNotificationEvent("Temp Set to  $ThermostatTemp!")
    	
}

def startTimerCallback() {
	log.debug "Turning on switches"
	VacationSwitches.on()   
    sendNotificationEvent("The lights on!")
            	thermostat.setHeatingSetpoint(ThermostatTemp)
            sendNotificationEvent("Temp Set to  $ThermostatTemp!")

}

def stopTimerCallback() {
	log.debug "Turning off switches"
	VacationSwitches.off()
    sendNotificationEvent("The lights off!")
        	thermostat.setHeatingSetpoint(ThermostatTemp)
            sendNotificationEvent("Temp Set to  $ThermostatTemp!")
}
