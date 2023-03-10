/**
 *  Test SmartApp
 *
 *  Copyright 2015 Kyle Landreth
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
    name: "Test SmartApp",
    namespace: "kyGitHub411",
    author: "Kyle Landreth",
    description: "Testing SmartApp",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Turn on when motion detected:") {
		input "themotion", "capability.motionSensor", required: true, title: "Where?"
	}
    section("Turn off when there's been no movement for") {
		input "minutes", "number", required: true, title: "Minutes?"
	}
    section("Turn on/off this light") {
		input "theswitch", "capability.switch", required: true
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
    //option 1
    subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
    
    //option 2
    //subscribe(themotion, "motion", motionHandler)
}

// TODO: implement event handlers

//option 1 handler
def motionDetectedHandler(evt){
	log.debug "motionDectedHandler called: $evt"
    theswitch.on()
}

//option 1 handler
/*def motionStoppedHandler(evt){
	log.debug "motionStoppedHandler called: $evt"
    theswitch.off()
}*/

//option 1 handler (added time varible to handler)
//once motion has stopped this handler will wait x minutes to checkMotion before turning off the light
def motionStoppedHandler(evt){
	log.debug "motionStoppedHandler called: $evt"
    runIn(60*minutes, checkMotion)
}

def checkMotion(){
	log.debug "In checkMotion scheduled method"
    
    //get the current state object for the motion sensor
    def motionState = themotion.currentState("motion")
    
    if (motionState.value == "inactive"){
    	//get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time
        
        //elapsed time is in milliseconds, so the theshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes
        
        if (elapsed >= threshold){
        	log.debug "Motion has stayed inactive long enough since last check ($elapsed ms): turning switch off"
            theswitch.off()
            
        } else {
        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms): doing nothing"
        }
        
    } else {
    	//motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }    
}

//option 2 handler
def motionHandler(evt){
	log.debug "motionHandler called: $evt"
    if (evt.value == "active"){
    	log.debug "Motion Detected: Switch On"
    	theswitch.on()
    } else if (evt.value == "inactive"){
    	log.debug "Motion NOT Detected: Switch Off"
    	theswitch.off()
    }
}