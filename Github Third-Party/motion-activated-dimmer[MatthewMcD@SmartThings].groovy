/**
 *  Motion Activated Dimmer
 *
 *  Copyright 2017 Matthew McDermott
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
 *  Version: 1.0
 *
 */
definition(
    name: "Motion Activated Dimmer",
    namespace: "MatthewMcD",
    author: "Matthew McDermott",
    description: "Improved motion activated light control for dimmers and switches.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Categories/lightsAndSwitches@2x.png")


preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on these switches") {
        input "switches", "capability.switch", required: false, multiple:true
    }
    section("Turn on these dimmers") {
        input "dimmers", "capability.switchLevel", required: false, multiple:true
        input "level", "number", required: false, title: "Dimming level?", defaultValue:"10", range: "1..100"
        input "rate", "number", required: false, title: "Dimming rate?", defaultValue:"1", range: "1..10"
    }

    //Add Timing - http://docs.smartthings.com/en/latest/smartapp-developers-guide/time-methods.html
    section("Active between what times?") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
    }
    section("Change to what mode?"){
    	input "newMode", "mode", title: "select a mode"
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
    subscribe(themotion, "motion.active", motionDetectedHandler)
}

// TODO: implement event handlers
def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    if (switches){
    	log.debug "will send the on() command to ${switches.size()} switches"
    }
    
    if (dimmers){
		log.debug "will send the level:$level and $rate command to ${dimmers.size()} dimmers"
	}
	//if the light is on, don't change it
	//http://docs.smartthings.com/en/latest/ref-docs/device-ref.html#device-current-state
    def dimmerOn = isOn(dimmers)
    def switchOn = isOn(switches)
    
    boolean between = true;
    log.debug "no current setting for time between: ${between.toString()}"
    if ((fromTime) && (toTime)){
    	between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        log.debug "current setting for time $fromTime : $toTime and ${between.toString()}"
    } else {
    	log.debug "no current setting for time between: ${between.toString()}"
    }
    //Only change light if it is off and between the target times
    log.debug "Current Status before changes : switchOn $switchOn and dimmerOn: $dimmerOn"
    if ((between)&&(!switchOn)&&(!dimmerOn)){
    	if (dimmers){
        	log.debug "SetLevel(1) Before"
        	dimmers.setLevel(1)
	        log.debug "Send setLevel($level,$rate) command to ${dimmers.size()} dimmers"
    	    dimmers.setLevel(level,rate)
    	}
        if (switches){
    		log.debug "Send the on() command to ${switches.size()} switches"
    		switches.on()
        }
    } else {
       log.debug "Motion ignored because switches or dimmers are on."
    }
    if (newMode){
    	changeMode()
    }
    
}

def changeMode() {
    log.debug "changeMode, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"

    if (location.mode != newMode) {
        if (location.modes?.find{it.name == newMode}) {
            setLocationMode(newMode)
        }  else {
            log.warn "Tried to change to undefined mode '${newMode}'"
        }
    }
}

def isOn(arr){
    // returns true if any devices in the map are on.
    boolean retVal = false;
    if (arr) {
    	def currSwitches = arr.currentSwitch
    	def onSwitches = currSwitches.findAll { switchVal ->
        	switchVal == "on" ? true : false
    	}

    	log.debug "isOn function: ${onSwitches.size()} out of ${arr.size()} switches are on"
    	retVal = onSwitches.size() > 0 //? true : false
   }
   return retVal
}