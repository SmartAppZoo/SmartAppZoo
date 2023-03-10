/**
 *  Closet Light Timer w/Motion
 *
 *  Copyright 2015 Jason Ziemba
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
    name: "Closet Light Timer w/Motion",
    namespace: "the2352",
    author: "Jason Ziemba",
    description: "Turns off light w/delay after motion stops, and resets if motion is detected prior to light turning off",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Info") {
    	paragraph "Author:	Jason Ziemba"
        paragraph "Version:	1.0 (Date: 02/28/15)"
        //paragraph "Change Log: " +
                  ""
    }   
    section("Select motion sensor...") {
		input "motion1", "capability.motionSensor", title: "Which Motion Sensor?", required: true
	}    
	section("Leave this light on when there is motion...") {
		input "switch1", "capability.switch", title: "Which Switch?", required: true
	}
    section("Select the number of minutes to turn off the light after motion stops... (default 15 minutes)") {
    	input "minutes", "number", title: "Minutes?", required: true, defaultValue: 15
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
    subscribe(motion1, "motion.active", "motionOnHandler")
    subscribe(motion1, "motion.inactive", "motionOffHandler")
    subscribe(switch1, "switch", "switchHandler")
}
 
def switchHandler(evt) {
	log.debug ("switch1 = $evt.value")
        checkStatus()
    }

def switchOffHandler(evt) {
	log.debug ("Turning off $switch1 soon...")
        switch1.off()
	}
    
def motionOnHandler(evt) {
	log.debug ("motion1 = $evt.value")
    	checkStatus()
	} 

def motionOffHandler(evt) {
	log.debug ("motion1 = $evt.value")
    	checkStatus()
	} 

def checkStatus() {
    def motion1Active = motion1.currentValue("motion") == "active"
    def motion1Inactive = motion1.currentValue("motion") == "inactive"
    def switchOff = switch1.currentValue("switch") == "off"
    def switchOn = switch1.currentValue("switch") == "on"
    log.debug("light on : $motion1Inactive : $switchOn :")
    if (motion1Inactive && switchOn) {
        log.trace("Turning off $switch1 soon...")
        runIn(60 * minutes, switchOffHandler) 
        }
    log.debug("Montion detected : $motion1Active : $switchOn :")
    if (motion1Active && switchOn) {
        log.trace("Motion detected, keeping $switch1 on...")
        unschedule(switchOffHandler)
    }
}
