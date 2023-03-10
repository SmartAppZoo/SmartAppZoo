/**
 *  I'll be back
 *
 *  Copyright 2016 Austin Pritchett
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
    name: "I'll be back",
    namespace: "ajpri",
    author: "Austin Pritchett",
    description: "Uses a button to turn off all lights in a room. Will turn back on when there's motion. ",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")



preferences {
	section("Trigger to Arm the motion Sensor") {
        //input(name: "remoteType", type: "enum", title: "RType", options: ["Single-button Remote","4-button Remote","Switch"])
		input "trigger", "capability.switch"        
	}
    
    section("Select Lights...") {
    	input "lights", "capability.switch", multiple: true
    }
    
    section("What motion sensor will turn back on lights?") {
    	input "motionTrigger", "capability.motionSensor"
    }
    
    section("Delay before arming") {
        input "delay", "decimal", title: "Number of minutes", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.isArmed = false
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(trigger, "switch", switchHandler)
    subscribe(motionTrigger, "motion", motionHandler)
}

def switchHandler(evt) {
    log.debug "$evt.value"
    if (evt.value == "on") {
        saveState()
        lights.off()
    	runIn(30, setArmed)
    } else if (evt.value == "off") {
		state.sysArmed = false
	}
}

def setArmed() {
		state.sysArmed = true
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		if (state.sysArmed == true) {		
        	log.debug "turning on lights"
        	trigger.off()
			//lights.on()
            restoreState()
            state.sysArmed = false
        }
	} else if (evt.value == "inactive") {

	}
}

private restoreState()
{
	def map = state["lights"] ?: [:]
	lights?.each {
		def value = map[it.id]
		if (value?.switch == "on") {
			def level = value.level
			if (level) {
				log.debug "setting $it.label level to $level"
				it.setLevel(level)
			}
			else {
				log.debug "turning $it.label on"
				it.on()
			}
		}
		else if (value?.switch == "off") {
			log.debug "turning $it.label off"
			it.off()
		}
	}
}

private saveState()
{
	def map = state["lights"] ?: [:]

	lights?.each {
		map[it.id] = [switch: it.currentSwitch, level: it.currentLevel]
	}

	state["lights"] = map
}
