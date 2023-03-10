/**
 *  Motion Lights
 *
 *  Copyright 2017 Celine Bursztein
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
    name: "Motion Lights",
    namespace: "cealtea",
    author: "Celine Bursztein",
    description: "Turn lights on/off when motion only if other lights are on/off",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select lights to turn on ..."){
		input "switches", "capability.switch", multiple: true, title: "Which lights?"
	}
	section("When motion is detected..."){
		input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("And off when there's been no movement for..."){
		input "minutes1", "number", title: "Minutes?"
	}
	section("Only when selected switch(s) are off"){
		input "lightcondition", "capability.switch", multiple: false, title: "Which switchs?"
	}
}

def installed() {
	
	subscribe(motion1, "motion", motionHandler)
}

def updated() {
	unsubscribe()
	subscribe(motion1, "motion", motionHandler)
}

def motionHandler(evt) {
	if (evt.value == "active" &&   lightcondition.currentState("switch").value == "off") {
		switches.on()
	} else if (evt.value == "inactive") {
		runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
	}
}

def scheduleCheck() {
	def motionState = motion1.currentState("motion")
    
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
    	def threshold = 1000 * 60 * minutes1 - 1000
        if (elapsed >= threshold) {
            switches.off()
    	}
    } 
}
