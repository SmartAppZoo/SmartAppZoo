/**
 *  Vacation
 *
 *  Copyright 2017 Jason Pullen
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
    name: "Vacation Mode",
    namespace: "Jpullen88",
    author: "Jason Pullen",
    description: "After being in Away mode for a specified duration change to Vacation Mode",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Vacation mode change") {
    	input "objHour", "number", title: "After __ hours in Away Mode switch to Vacation Mode",required:true
    }
    section("Lighting"){
    	input "objSwitch", "capability.switch", title: "Select which lights you want to turn on in Vacation Mode.",multiple:true,required:true
    	input "objBrightness", "number", title: "Choose their level of brigthtness.", required:false
    }
    section("Timing"){
    	input "objTurnOnDelay", "number", title: "How many minutes after sunset to wait before turning on the lights?", required:true
    	input "objTurnOffDelay", "number", title: "How many hours to leave the lights on?", required:true
	}
}
def installed() {
    //runIn(2,handlerLogMode)
    initialize()
}
def updated() {
    //runIn(2,handlerLogMode)
    unsubscribe()
    initialize()
}
def initialize() {
    def sunsetTime = getSunriseAndSunset().sunset
    //On mode change if Away start timer, if Home cancel timer
    subscribe(location, handlerCheckMode)
    
    //Schedule lights to be turned on
    schedule(sunsetTime, handlerLightTime)
    //log.debug "Sunset is at: ${sunsetTime}"
}
//Log Handler
/*
def handlerLogMode() {
	log.debug "Current Mode = $location.currentMode"
}
*/
//Time Handlers
def handlerLightTime(){
    if (location.mode == "Vacation") {
        def sunsetTurnOnTime = getSunriseAndSunset(sunsetOffset: "${objTurnOnDelay}").sunset
    	def sunsetTurnOffTime = getSunriseAndSunset(sunsetOffset: "${objTurnOffDelay*60}").sunset
        
        //log.debug "Vacation Lights scheduled to: Turn on ($sunsetTurnOnTime) and Turn off ($sunsetTurnOffTime)."
        
        runOnce(sunsetTurnOnTime, handlerLightsOn)
    	runOnce(sunsetTurnOffTime, handlerLightsOff)
    }
}
//Light Handlers
def handlerLightsOn() {
	//log.debug "The Vacation lights were turned on."
    objSwitch.on()
}
def handlerLightsOff() {
	//log.debug "The Vacation lights were turned off."
    objSwitch.off()
}

//Mode Handlers
def handlerCheckMode(evt) {
    if (evt.value == "Away") {
        //changes mode to Vacation
        runIn((objHour * 60 * 60),handlerSetVacation)
        //log.debug "The Vacation mode timer has begun."
        //runIn(2,handlerLogMode)
    } 
    else if (evt.value == "Home") {
    	//terminates timer
    	unschedule(handlerSetVacation)
        runIn(1,handlerLightsOff)
        //runIn(2,handlerLogMode)
        //log.debug "The Vacation mode timer has been canceled."
    }
}
def handlerSetVacation() {
	location.setMode("Vacation")
    //runIn(2,handlerLogMode)
    runIn(2,handlerLightTime)
}