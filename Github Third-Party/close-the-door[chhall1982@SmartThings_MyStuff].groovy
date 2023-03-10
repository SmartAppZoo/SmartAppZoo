/**
 *  Close The Door
 *
 *  Copyright 2018 Chris Hall
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License..
 *
 */
definition(
    name: "Close The Door",
    namespace: "chhall1982",
    author: "Chris Hall",
    description: "If a closable door is left open for a set amount of time, close it.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Door to monitor:") {
	    input "thedoor", "capability.garageDoorControl", required: true, title: "Door?"
    }
       section("Close when door has been open for:") {
           input "minutes", "number", required: true, title: "Minutes?"
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
	subscribe(thedoor, "door.open", doorOpenedHandler)
}

def doorOpenedHandler(evt) {
	log.debug "doorOpenedHandler called: $evt"
    runIn((minutes * 60) + 10, checkDoor)
}

def checkDoor() {
    log.debug "In checkDoor scheduled method"
    
    // get the current state object for the door
    def doorState = thedoor.currentState("door")
    
    if (doorState.value == "open") {
        // get the time elapsed between now and when the door reported opened
        def elapsed = now() - doorState.date.time
        
        //elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes
                
            if (elapsed >= threshold) {
                    log.debug "Door has been open long enough since last check ($elapsed ms): closing door"
                    thedoor.close()
            } else {
                log.debug "Door has not been open long enough since last check ($elapsed ms): doing nothing"
            }
   
    } else {
           // Door closed; just log it and do nothing
           log.debug "Door is not open, do nothing"
    }
}