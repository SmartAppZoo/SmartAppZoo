/**
 *  Ceiling Fans
 *
 *  Copyright 2016, 2017 Thomas Lawson
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
/**********************************************************************************************************************************************/
definition(
    name    	: "Ceiling Fans",
    namespace	: "LawsonAutomation",
    author      : "Tom Lawson",
    description	: "Child App, DO NOT INSTALL DIRECTLY",
    parent		: "LawsonAutomation:Ceiling Fan Guru",     
	category	: "My apps",
    iconUrl		: "https://raw.githubusercontent.com/lawsonautomation/icons/master/guru-60.png",
    iconX2Url	: "https://raw.githubusercontent.com/lawsonautomation/icons/master/guru-120.png",
    iconX3Url	: "https://raw.githubusercontent.com/lawsonautomation/icons/master/guru-120.png")

/**********************************************************************************************************************************************/
preferences {
    page(name: "mainPage", install: true, uninstall: true) {
    	section("Fans") {
    	    input "myFans", "capability.switch", required: true, multiple: true, title: "Ceiling Fans"
    	}
    	section("Temperature") {
    	    input "myTempSensor", "capability.temperatureMeasurement", required: true, title: "Temperature Sensor"
    	    input "ceilingFanSetpoint", "decimal", title: "Set Point", required: true
    	}
    	section("Motion") {
        	input "myMotionSensors", "capability.motionSensor", required: false, multiple: true, title: "Motion Sensors"
        	input "myInactivityDelay", "number", defaultValue: 30, title: "Inactivity Minutes", required: false
    	}
		section("Modes (default Home)") {
			input "myModes", "mode", title: "Operating Modes", multiple: true, required: false
    	}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
    	section("Users Guide") {
            href(name: "href",
             	title: "Descriptions, Tips and Tricks",
                required: false,
        	    image: "https://raw.githubusercontent.com/lawsonautomation/icons/master/info.png",
                page: "Users Guide")
    		}	
	}
    page(name: "Users Guide", title: "User's Guide", nextPage: "mainPage") { 
        section("Overview") {
        	paragraph "Ceiling Fan Guru turns ceiling fans on when too warm, and off when comfortable or away. "
        }
        section("Ceiling Fans") {
        	paragraph "These fans will turn on when the inside temperature exceeds the specified set point and the other condition described below are met."
        	paragraph "The speed and direction of a ceiling fan must be set on the fan itself as this app assumes a simple on/off switch"
        }
        section("Temperature Sensor") {
        	paragraph "This is an indoor temperature sensor used to control the ceiling fans."
        }
        section("Motion Sensors") {
        	paragraph "If there is motion for any of these sensors, motion is considered to be present. This sensor is optional."
        }
        section("Inactivity Minutes") {
        	paragraph "When no motion is sensed for this amount of time, the ceiling fans are turned off. "
        }
        section("Operating Modes") {
        	paragraph "The mode(s) in which ceiling fans are permitted to turn on per this app. " +
            		  "For example, a ceiling fan in a living room typically only operates during the 'Home' mode. " +
                      "A ceiling fan in a bedroom typically only operates during the 'Night' mode. " +
                      "Multiple modes may be selected. User defined modes are supported. " +
                      "If no Operating Mode is specified, the Home mode is assumed."
        }
        section("Tips and Tricks") {
        	paragraph "If you have some ceiling fans associated with one or more motion sensors and some that are not, " +
            		  "install separate instances of Ceiling Fan Guru for each grouping of motion sensor."
            paragraph "Likewise, if some ceiling fans operatate under different modes, such as living room versus bedroom " +
            		  "ceiling fans, install separate instances of Ceiling Fan Guru for each grouping of operating mode."
        	paragraph "If you manually switch off a fan, it will stay off until the next natural off/on cycle occurs."
        }
    }
}

def installed() {
	initialize()
	LOG "Installed with settings: ${settings}"
}

def updated() {
	unsubscribe()
	initialize()
	LOG "Updated with settings: ${settings}"
}

def initialize() {
    state.setpoint = ceilingFanSetpoint
    // this locks in manual changes, 0 = off, 1 = on, 2 == unknown
    state.fans = 2  
    state.waitingForTimeout = false
    state.hysteresis = cAdj(1.0)
    
	// init inside temperature
    def currentState = myTempSensor.currentState("temperature")
    // give it a sensible default if we can not get the temperature
    state.insideTemp = (currentState) ? currentState.integerValue : ceilingFanSetpoint - 1
	LOG "initial inside temp: ${state.insideTemp}, Set point: ${state.setpoint}"
    
    // init inactivity delay
    state.delay = 30
    LOG "myInactivityDelay: ${myInactivityDelay}"
    // min delay is 1 minute
    state.delay = (myInactivityDelay) ? myInactivityDelay : 1
        
    subscribe(myTempSensor, "temperature", temperatureChangedHandler)
    subscribe(location, "mode", modeChangedHandler)
    if (myMotionSensors) {
    	subscribe(myMotionSensors, "motion", motionHandler)
    }
    
    checkConditions()
}

// event handlers

def temperatureChangedHandler(evt) {
	state.insideTemp = evt.doubleValue
    LOG "Inside Temp: ${state.insideTemp}"
    checkConditions()
}

def modeChangedHandler(evt) {
    LOG "Mode Changed: ${location.mode}"
    checkConditions()
}

def motionHandler(evt) {
    LOG "motionHandler: ${evt.value}"
    if("active" == evt.value) {
        // prevent out of bounds values
    	checkConditions()
    } else if("inactive" == evt.value) {
    	state.waitingForTimeout = true
    	runIn(state.delay * 60, completeTimeout)
    }
}

// utils

def LOG(String text) {
    //log.debug(text)
}

def completeTimeout() {
    state.waitingForTimeout = false
    checkConditions()
}

def checkConditions() {
    // check temp vs threshhold and prevent thrashing
    def motion = motionDetected()
    LOG "checkConditions:  mode ${location.mode}, insideTemp ${state.insideTemp}, state ${state.setpoint}, motionDetected ${motion}, waitingForTimeout ${state.waitingForTimeout}"
	if (inWrongMode() || state.insideTemp < state.setpoint - state.hysteresis || !motion) {  
    	fansOff()
    } 
    else if (state.insideTemp >= state.setpoint) { 
    	fansOn()
    } 
}

def fansOn() {
    LOG("turn fans on")
	// lock in manual changes
	if (state.fans != 1) { 
    	state.fans = 1
    	myFans.on()
        sendNotificationEvent("Ceiling Fan Guru turned on ceiling fans")
    }
}

def fansOff() {
    LOG("turn fans off")
	// lock in manual changes
	if (state.fans != 0) { 
    	state.fans = 0
    	myFans.off()
        sendNotificationEvent("Ceiling Fan Guru turned off ceiling fans")
    }
}

boolean motionDetected() {
	boolean status = false
	if (!state.waitingForTimeout && myMotionSensors) {
    	myMotionSensors.each {
    		def motion = it?.currentValue("motion")
   			LOG "motion sensor: ${motion}"
    		if (motion == "active") {
   				LOG("motion detected!!!!!")
    			status = true
       		}
    	}
    } else {
    	status = true
    }
    return status
}

boolean inWrongMode() {
	boolean status = true
	if (myModes) {
    	myModes.each {
   			LOG("mode sensor: ${mode}, it ${it}")
    		//def mode = it.currentValue("mode")
    		def mode = it
    		if (mode == location.mode) {
   				LOG("mode OK!!!!!")
    			status = false
       		}
    	}
    } else {
    	if (location.mode == "Home") {
   			LOG("mode is Home!!!!!")
    		status = false
        }
    }
    return status
}

// correct an offset if using metric
def cAdj(offset) {
    if (location.temperatureScale == "C") {
    	offset = offset * 0.5555555556
    }
	return offset
}
