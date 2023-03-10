/**
 *  OhmHour
 *
 *  Copyright 2018 Bryan Li
 *  Version 1.1 2/14/18
 *  - Continually monitor slave switches
 *  - Remove dimmer section - with LED bulbs, this doesn't make sense (saves less than a watt).
 *
 *  Based on initial work by Mark West
 *  Version 1.0 8/8/15
 *  - Initial version posted to SmartThings forums
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
    name: "OhmHour",
    namespace: "joyfulhouse",
    author: "Bryan Li",
    description: "Integration into OhmConnect to turn off plugs/switches during peak energy events",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select") {
		input "masters", "capability.switch", 
			multiple: false, 
			title: "Ohm Connect Switch...", 
			required: true
	}
    section("During OhmHour turn off these switches...") {
		input "slaves", "capability.switch", 
			multiple: true, 
			title: "On/Off Switch(es)...", 
			required: false
	}
	section("During OhmHour disable these Central AC units..."){
		input "acCentral", "capability.thermostat",
			multiple: true,
			title: "On/Off Thermostat(s)...",
			required: false
	}
	section("During OhmHour disable these Split AC units..."){
		input "acSplit", "capability.thermostat",
			multiple: true,
			title: "On/Off Thermostat(s)...",
			required: false
	}
    section("After OhmHour always turn on these switches...") {
		input "onSlaves", "capability.switch", 
			multiple: true, 
			title: "On/Off Switch(es)...", 
			required: false
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
	subscribe(masters, "switch.on", switchOnHandler)
	subscribe(masters, "switch.off", switchOffHandler)
    subscribe(slaves, "switch.on", slaveSwitchOnHandler)
}

def switchOnHandler(evt) {
	log.info "switchOnHandler Event: ${evt.value}"
    
    state.switchprevious = [:]
	slaves.each {
		state.switchprevious[it.id] = [
        "switch": it.currentValue("switch")
        ]
	}
    
	acCentral?.off()
	acSplit?.off()
	slaves?.off()
    onSlaves?.off()
}


def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
    
    // Turn devices back on, recheck in 30 seconds as well
    resetDeviceState()
    runIn(30, resetDeviceState)
    
    // Request to turn on devices once again in onSlaves after 60 seconds
    runIn(60, turnOnSlaves)
}

def slaveSwitchOnHandler(evt) { 
	def evtSwitch = evt.device
	masters.each {
    	if(it.currentValue("switch") == "on"){
        	evtSwitch.off()
        }
    }
}

def resetDeviceState(){
	log.info "Resetting device state prior to OhmHour"
	slaves.each {
    	if(state.switchprevious[it.id].switch != it.currentValue("switch")) {
        	it.on()
        }
    }
    
    // onSlaves always turn on after an OhmHour Event has ended
    onSlaves.each {
    	it.on()
    }
	
	// turn the central AC units back on
	acCentral.each {
		it.auto()	
	}
}

def turnOnSlaves() {
	onSlaves.each {
    	it.on()
    }
}