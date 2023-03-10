/**
 *  Whole House Fan
 *
 *  Copyright 2014 Brian Steere
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
    name: "Circulate Air when Thermostat is on",
    namespace: "dasalien",
    author: "Dieter Rothhardt",
    description: "Switch on fan devices when the thermostat kicks on. Remember previous setting",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
)


preferences {
    section("Fans") {
        input "fans", "capability.switchLevel", title: "Ceiling Fan", multiple: true
    }
    
    section("Thermostat") {
    	input "thermostat", "capability.thermostat", title: "Thermostat"
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
    subscribe(thermostat, "thermostatMode", runFansOpState);
    subscribe(thermostat, "thermostatOperatingState", runFansOpState);
}

def runFansOpState(evt) {
    log.debug "runFansOpState Received event"
    def thermostatMode = settings.thermostat.currentValue('thermostatMode')
    def thermostatOpState = settings.thermostat.currentValue('thermostatOperatingState')
	log.debug "thermostatMode: $thermostatMode"
	log.debug "thermostatOpState: $thermostatOpState"
	
    def fanlevel = fans*.currentValue('level')
	def fanstate = fans*.currentValue('switch')
    def statestring = ""
    def levelstring = ""
	def idxstate = 0
    def idxlevel = 0

    if(thermostatOpState != 'idle') {
		//Remember current status
		fanlevel.eachWithIndex { val, idx ->
            statestring = "FanState${idx}"
            levelstring = "FanLevel${idx}"
            state["${statestring}"] = fanstate.getAt(idx)
            state["${levelstring}"] = val
            log.debug "Index: ${idx} State: ${fanstate.getAt(idx)} Level: ${val}"
            fans[idx].setLevel(40)
		}
    } else {
    	log.debug "Not running due to thermostat mode"
		//Restore previous settings
		fanlevel.eachWithIndex { val, idx ->
            statestring = "FanState${idx}"
            idxstate = state["${statestring}"]
            levelstring = "FanLevel${idx}"
            idxlevel = state["${levelstring}"]
            log.debug "Index: ${idx} Stored State: ${idxstate} Level: ${idxlevel}"
            log.debug "Index: ${idx} Current State: ${fanstate.getAt(idx)} Level: ${val}"
            
            //Reset if not already different settings            
            if(idxlevel < val) {
            	log.debug "Setting level to ${idxlevel}"
            	fans[idx].setLevel(idxlevel)
            }
            if(idxstate == "on") {
            	log.debug "idxstate is on"
             	if(fanstate.getAt(idx) == "on") { 
                	log.debug "fan is currently on"
                }
            } else {
				log.debug "idxstate is off - turn fan off"
				fans[idx].off()
            }
		}
    }

}


def runFans(evt) {
    log.debug "runFans Received event: ${evt}"
    def thermostatMode = settings.thermostat.currentValue('thermostatMode')
	log.debug "Thermostat: $thermostatMode"
    
    def fanlevel = fans*.currentValue('level')
	def fanstate = fans*.currentValue('switch')
    def statestring = ""
    def levelstring = ""
	def idxstate = 0
    def idxlevel = 0

	log.debug "Thermostat: $thermostatMode"

    if((thermostatMode != 'off') || (thermostatMode != 'idle')){
		//Remember current status
		fanlevel.eachWithIndex { val, idx ->
            statestring = "FanState${idx}"
            levelstring = "FanLevel${idx}"
            state["${statestring}"] = fanstate.getAt(idx)
            state["${levelstring}"] = val
            fans[idx].setLevel(80)
            fans[idx].on()
		}
    } else {
    	log.debug "Not running due to thermostat mode"
		//Restore previous settings
		fanlevel.eachWithIndex { val, idx ->
            statestring = "FanState${idx}"
            idxstate = state["${statestring}"]
            levelstring = "FanLevel${idx}"
            idxlevel = state["${levelstring}"]
            //log.debug "Index: ${idx} State: ${idxstate} Level: ${idxlevel}"
            fans[idx].setLevel(idxlevel)
            if(idxstate == "on") {
             	fans[idx].on()
            } else {
              	fans[idx].off()
            }
		}
    }
}