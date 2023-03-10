/**
 *  Fan Scheduler
 *
 *  Copyright 2017 T&amp;A
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
    name: "Fan Scheduler",
    namespace: "tybo27",
    author: "T&amp;A",
    description: "Schedules an ecobee3 Fan to turn on via a schedule (more flexible than the auto mode)",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png") {
}


preferences {
	section("Pick a Thermostat") {
        //input "thermostat", "capability.thermostatFanMode", required: true, title: "Thermostat"
        input "thermostat", "capability.thermostat", required: true, title: "Thermostat"
		 input "runTime", "number", required: true, title: "Fan Run Time (min)"
         input "runPeriod", "number", required: true, title: "Fan Run Period: 1, 5, 10, 15, 30, 60, or 180 minutes"
         input "ceilingFan", "capability.switch", required: false, title: "Ceiling Fan"
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
   // subscribe(thermostat, "thermostatFanMode", myHandler)
    // Schedule period fanAuto(), fanOn(), or setThermostatFanMode(ENUM mode) auto or on.
    state.fanFlop = 0
    
    switch (runPeriod) {
        case 1: 
        	runEvery1Minute(turnBlowerOn)
            break
        case 5:
        	runEvery5Minutes(turnBlowerOn)
            break
        case 10:
        	runEvery10Minutes(turnBlowerOn)
            break
        case 15:
        	runEvery15Minutes(turnBlowerOn)
            break
        case 30:
        	runEvery30Minutes(turnBlowerOn)
            break
        case 60:
        	runEvery1Hour(turnBlowerOn)
            break
        case 180:
        	runEvery3Hours(turnBlowerOn)
            break
        default:
        	log.debug "Invalid time entered"
            break
    }
}

def turnBlowerOn() {
	log.debug "Turning Blower on"
	thermostat.fanOn()
    
    // If a fan is defined, toggle its state every blower on event
    if (ceilingFan) {
    	if (state.fanFlop == 0) {
        	log.debug "Turning Ceiling Fan on"
        	ceilingFan.on()
            state.fanFlop = 1
        } else if (state.fanFlop == 1) {
        	log.debug "Turning Ceiling Fan off"
        	ceilingFan.off()
            state.fanFlop = 0
        }
    }
    
    // Schedule Blower off per defined run time
    runIn(60*runTime, turnBlowerOff)
}

def turnBlowerOff() {
	log.debug "Turning Blower off (back to auto)"
    thermostat.fanAuto()
}