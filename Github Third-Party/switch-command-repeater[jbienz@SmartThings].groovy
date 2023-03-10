/**
 *  Switch Command Repeater
 *
 *  Copyright 2017 Jared Bienz
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
    name: "Switch Command Repeater",
    namespace: "jbienz",
    author: "Jared Bienz",
    description: "Allows switch commands to be repeated a number of times. Handy for switches that do not accurately report their status and sometimes need to be commanded multiple times in order to reach the desired state.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Switch to manage:") {
        input "theSwitch", "capability.switch", required: true
    }
    section("Repeat settings") {
        input "repeatTimes", "number", required: true, title: "Number of times?"
        input "repeatDelay", "number", required: true, title: "Seconds between repeats?"
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
	// Reset counter to zero
    state.count = 0
    
    // Subscribe to switch state changes
    subscribe(theSwitch, "switch", switchHandler)
}

// TODO: implement event handlers
def switchHandler(evt) {
    log.debug "switchHandler called: $evt"
    
    // Validate settings
    if (repeatTimes < 1) {
    	log.warn "Invalid value for repeatTimes: $repeatTimes"
        return
    }
    
    if (repeatDelay < 1) {
    	log.warn "Invalid value for repeatDelay: $repeatDelay"
        return
    }

    // Notify of scheduling next repeat
    def curCount = state.count
    log.debug "Scheduling repeat #${curCount + 1} for '${evt.value}' in ${repeatDelay} seconds..."

    // Schedule the repeat command
	runIn(repeatDelay, runCommand, [data: [command: evt.value]])
}

def runCommand(data) {

    // Increment the counter
    state.count = state.count + 1
    def curCount = state.count
   
    // Execute?
    if (curCount <= repeatTimes) {
    
    	// Notify of executing repeat
    	log.debug "Executing repeat #${curCount} for '${data.command}'..."
        
    	// Repeat the command
		if (data.command == "on") {
    		theSwitch.on();
    	} else if (data.command == "off") {
	       	theSwitch.off();
    	}
	}
    
    // Schedule again or reset?
    if (curCount < repeatTimes) {

        // Notify of scheduling next repeat
        log.debug "Scheduling repeat #${curCount + 1} for '${data.command}' in ${repeatDelay} seconds..."
        
        // Schedule the repeat
        runIn(repeatDelay, runCommand, [data: [command: data.command]])
    }
    else {
    
        // Notify of resetting counter
        log.debug "Resetting repeat counter."
        state.count = 0
    }
}