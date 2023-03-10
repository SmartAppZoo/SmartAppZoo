/**
 *  Check If It's Locked
 *
 *  Copyright 2014 Greg
 *  Copyright 2016 Chris Valenti
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
    name: "Check If It's Locked",
    namespace: "abcvalenti",
    author: "Chris Valenti",
    description: "Check whether a lock is locked on mode change or at a specific time.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Which lock(s) should I check?") {
		input "locks", "capability.lock", title: "Which?", multiple: true, required: true
    }
    
    section("Which mode changes trigger the check?") {
		input "newMode", "mode", title: "Which?", multiple: true, required: false
    }

    section("When should I check?") {
		input "timeToCheck", "time", title: "At this time", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    if (newMode != null) {
		subscribe(location, modeChangeHandler)
    }

    if (timeToCheck != null) {
    	schedule(timeToCheck, checkLocks)
    }
}

def modeChangeHandler(evt) {
    log.debug "Mode change to: ${evt.value}"

    // Have to handle when they select one mode or multiple
    if (newMode.any{ it == evt.value } || newMode == evt.value) {
		checkLocks()
    }
}

def checkLocks() {
	log.debug "checking locks"
    
	def unLockedMessage = StringBuilder.newInstance()
    
    // Loop through each lock to see if it's unlocked.
    locks.each {
    	log.debug "${it.displayName} is ${it.currentLock}"
        
		if (it.currentLock == "unlocked") {
        	// Build the list of lock(s) which are unlocked to send one notification.
			unLockedMessage.append("${it.displayName} is unlocked. ")
            
            log.debug "$unLockedMessage"
		}
	}

	log.debug "${unLockedMessage.size()}"
    
    // Only sending message if there were locks that were unlocked.
    if (unLockedMessage.size() > 0) {
		log.debug unLockedMessage

		// sendPush can only send strings, so need to convert unLockedMessage to a string.
        def strUnLockedMessage = unLockedMessage.toString()
		sendPush(strUnLockedMessage)
    }
}