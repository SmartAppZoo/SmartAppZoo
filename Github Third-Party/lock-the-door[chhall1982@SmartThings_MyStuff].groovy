/**
 *  Lock The Door
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
    name: "Lock The Door",
    namespace: "chhall1982",
    author: "Chris Hall",
    description: "If a lock is left unlocked for a set amount of time, lock it.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Lock to monitor:") {
	    input "thelock", "capability.lock", required: true, title: "Lock?"
        input "thedoor", "capability.contactSensor", required: true, title: "Door?"
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
	subscribe(thelock, "lock.unlocked", lockUnlockedHandler)
    subscribe(thedoor, "contact.open", doorOpenHandler)
}

def lockUnlockedHandler(evt) {
	log.debug "lockUnlockedHandler called: $evt"
    runIn((minutes * 60) + 10, checkLock)
}

def doorOpenHandler(evt) {
    log.debug "doorOpenHandler called: $evt"
}

def checkLock() {
    log.debug "In checkLock scheduled method"
    
    // get the current state object for the lock
    def lockState = thelock.currentState("lock")
    def doorState = thedoor.currentState("contact")
    
    if (lockState.value == "unlocked") {
        // get the time elapsed between now and when the lock reported opened
        def elapsed = now() - lockState.date.time
        
        //elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes
                
            if (elapsed >= threshold) {
                    if (doorState.value == "closed") {
                        log.debug "Lock has been unlocked long enough since last check ($elapsed ms) and door is closed: locking"
                        thelock.lock()
                    } else {
                        log.debug "Lock has been open long enough ($elapsed ms), but door is open.  Waiting 5 minutes."
                        runIn(30, checkLock)
                    }
            } else {
                log.debug "Lock has not been unlocked long enough since last check ($elapsed ms): doing nothing"
            }
   
    } else {
           // Lock locked; just log it and do nothing
           log.debug "Lock is not locked, do nothing"
    }
}