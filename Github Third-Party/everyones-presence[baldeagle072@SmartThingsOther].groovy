/**
 *  Everyone's Presence
 *
 *  Copyright 2015 Eric Roberts
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
    name: "Everyone's Presence",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "Will set a simulated presence sensor based based on if anybody is home. If no people are home, it will set to 'not present'. If at least one person is home, it will set to 'present'.",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Presence Sensor Group") {
		input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: true
        input "simulatedPresence", "device.simulatedPresenceSensor", title: "Simulated Presence Sensor", multiple: false, required: true
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
	setPresence()
	subscribe(presenceSensors, "presence", "presenceHandler")
}

def presenceHandler(evt) {
	setPresence()
}

def setPresence(){
	def presentCounter = 0
    
    presenceSensors.each {
    	if (it.currentValue("presence") == "present") {
        	presentCounter++
        }
    }
    
    log.debug("presentCounter: ${presentCounter}, simulatedPresence: ${simulatedPresence.currentValue("presence")}")
    
    if (presentCounter > 0) {
    	if (simulatedPresence.currentValue("presence") != "present") {
    		simulatedPresence.arrived()
            log.debug("Arrived")
        }
    } else {
    	if (simulatedPresence.currentValue("presence") != "not present") {
    		simulatedPresence.departed()
            log.debug("Departed")
        }
    }
}
