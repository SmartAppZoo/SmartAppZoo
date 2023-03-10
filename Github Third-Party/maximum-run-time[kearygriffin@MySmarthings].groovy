/**
 *  Maximum Run Time
 *
 *  Copyright 2015 Keary Griffin
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
    name: "Maximum Run Time",
    namespace: "kearygriffin",
    author: "Keary Griffin",
    description: "Only allow a switch to run for a certain number of seconds.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When the following is turned on...") {
		input name: "switch1", title: "Which Switch?", type: "capability.switch", required: true
	}
	section("Turn it off after..."){
        input "seconds", "number", title: "Seconds", required: true
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
	subscribe(switch1, "switch.on", onHandler)
}

def onHandler(evt) {
	def delay = seconds
    state.turnOffTime = now() + (seconds-1) * 1000
    log.debug "Scheduling turn off at time " + state.turnOffTime + " in " + delay + " seconds."
	//runIn(delay, "scheduledTurnOff")
    pause(delay * 1000);
    scheduledTurnOff();
}

def scheduledTurnOff() {
//    if (!state.turnOffTime || state.turnOffTime <= now()) {
    	log.debug "Turning off."
		switch1.off()
//    } else {
//       log.debug "Not turning off.  Now: " + now() + ", scheduled: " + state.turnOffTime
//    }
}