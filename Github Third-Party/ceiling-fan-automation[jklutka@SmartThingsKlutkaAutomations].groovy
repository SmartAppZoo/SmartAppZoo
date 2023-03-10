/**
 *  Copyright 2016 Justin Klutka
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
 *  Ceiling Fan Automation
 *
 *  Author: Justin Klutka
 */
 
 /*
 	Version History:
    
    1.0 - 5/29/2016 - Basic version release.  You may specifiy a thermostat and a set of fans
    1.1 - 11/23/2016 - Migrated to a new repo and removed the testing field for snooze actions
    1.2 - 7/13/2017 - Added Sleep time window to avoid fan coming on when it is unwanted. Code refactor.
    1.3 - 7/16/2017 - Logic bug fix
    1.4 - 7/24/2017 - Fixed a bug with the sleep time monitor logic.
    1.5 - 8/1/2017 - Added a sleep mode based on motion from one sensor.
    1.6 - 6/3/2018 - Code clean up to simplify exception rule logic.
    
    
    Future Plans:
    - Add conditional preferences. Ex: Mode of the house, time of day, day of week, if certain people are home
 */

definition(
    name: "Ceiling Fan Automation",
    namespace: "klutka",
    author: "Justin Klutka",
    description: "Designed to coordinate device operating state for a thermostat to a set of switches.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn@2x.png"
)

preferences() {
	section("Thermostat Setup") {
		input "thermostat", "capability.thermostat", required: true, title: "Select your thermostat"
        input "triggerStates", "enum", title: "Select your trigger operating states", multiple: true, required: true, options:
          [["heating": "heating"], ["cooling": "cooling"], ["fan only": "fan only"], ["idle": "idle"]]
	}
    section("Fan Setup") {
    	input "fans", "capability.switch", required: true, multiple: true, title: "Select your Fans or Fan Switches"
    }
    section("Exception Conditions") {
    	input "sleepTimeStart", "time", title: "Do NOT run Starting:", required: false
        input "sleepTimeEnd", "time", title: "Ending:", required: false
        input "sleepMotion", "capability.motionSensor", title: "Do NOT run when motion has occured here:", required: false, multiple: false
        input "sleepMotionTimer", "integer", title: "Minutes since last motion (Default 15):", required: false, 	multiple: false
    }
    
    
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {	
	if (thermostat) {
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	}
    
	if (fans) {
    	subscribe(fans, "switch.on", switchOnHandler)
        subscribe(fans, "switch.off", switchOffHandler)
	}
    
    if (sleepMotion) {
    	subscribe(sleepMotion, "motion.active", motionSleepHandler)
    }
    
    //default state values
    state.ManualOnRequested = false
    state.ManualOffRequested = false
    
	evaluateAutomation()
}

def operatingStateHandler(evt) {
	log.debug("The thermostat operating state changed at ${new Date()}.") 
    //clear manual operating assumptions from state when state of hvac changes
    state.ManualOnRequested = false
    state.ManualOffRequested = false
    evaluateAutomation()
}

def switchOnHandler(evt) {
	log.debug("Switch turned on at ${new Date()}.")
    //capture a state variable indicationg a manual on was requested
    state.ManualOnRequested = true
}

def switchOffHandler(evt) {
	log.debug("Switch manually turned off at ${new Date()}.")
    //capture a state variable indicating a manual off was requested
    state.ManualOffRequested = true 
}

def motionSleepHandler(evt) {
	log.debug("Motion detected at a motion sensor setup as an exception.")
	evaluateAutomation()
}

//core function to evaluate if fans should be automated
def evaluateAutomation() {	    
	log.debug("Evaluation of Automation happened at ${new Date()}.")
    
    //determine if fans should be on
	if (fansRequired()) {
    	log.debug("Manual on state value is ${state.ManualOnRequested}.")
        //evaluate if an exception rule is active or if a manual on state was requested
        if (isExceptionRuleActive()) {
            //shut off fans if they are on now that an exception rule is active
            switchesOff()
            //set a recheck in 15 minutes
            runEvery15Minutes(evaluateAutomation)
            log.debug("Active Exception Rules. A 15 minute recheck was scheduled at ${new Date()}.")
            return;
        }
        else {
            //fans are required and no exception rules are active
            switchesOn()
        }
    }
	else {
    	//the fans are not needed and should be shut off
    	switchesOff()
	}
    
}

//Returns if operating state requires fans to come on
def fansRequired () {
	def currentOperatingState
    
    if (!thermostat) {
    	log.debug("A thermostat poll will be requested because the thermostat was null.")
    	thermostat.poll()
    }
    
	currentOperatingState = thermostat.currentState("thermostatOperatingState")?.value
    log.debug("Current operating state: ${currentOperatingState.toString()}.") 
        
    //evaluate if an operating state requiring fans is present
    log.debug("Seeking operating state: ${triggerStates.toString()}.") 
    if (triggerStates.contains(currentOperatingState.toString())) {                       	
        return true
    }
    //else if (state.ManualOffRequested == true ) {
    //	return false
    //}
    else {
        //no fans are required
        return false
    }
}

//Evaluate if sleep time needs to be observed
def isExceptionRuleActive () {
	def timeOfDayException = false
    def motionException = false
    
    //evaluate time of day rule
    if (sleepTimeStart != null && sleepTimeEnd != null) {
        timeOfDayException = timeOfDayIsBetween(sleepTimeStart, sleepTimeEnd, new Date(), location.timeZone)      
    }
    
    //evaluate if a motion 
    if (sleepMotion) {
        //get the most recent motion event for the motion sensor
        def recentEvents = sleepMotion.events([max:1])

        //capture motion lookback setting and handle null
        def motionLookBack = sleepMotionTimer == null ?: 15

        //evaluate the motion rule
        if (recentEvents.size > 0) {
            log.debug("The most recent active motion was ${recentEvents[0].date.time}.")
            if (now() - recentEvents[0].date.time <= 15 * 60000) {
                log.debug("Exiting due to sleep motion trigger event ${now() - recentEvents[0].date.time}.")
                motionException = true
            }
        }
	}
    
    if (timeOfDayException || motionException)
    	return true
    else
    	return false

}

//Turns on all fans
private switchesOn() {	
	log.debug("Fans powered on at ${new Date()}.")
	fans.each {
		log.debug("Fan switch is ${it.currentState("switch")?.value}.")
    	if (it.currentState("switch")?.value != "on") {
			it.on()
		}
    }
}

//Turns off all fans
private switchesOff() {
	log.debug("Fans powered off at ${new Date()}.")
	fans.each {
		log.debug("Fan switch is ${it.currentState("switch")?.value}.")
		if (it.currentState("switch")?.value != "off") {
			it.off()
		}
    }
}