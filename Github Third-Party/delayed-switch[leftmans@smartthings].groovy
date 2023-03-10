/**
 *  delayed switch
 *
 *  Copyright 2021 leftmans
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
 *
 * 2021.07.23 add always off feature
 */
 
definition(
    name: "Delayed Switch",
    namespace: "leftmans_smartapps",
    author: "leftmans",
    description: "delayed switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png")


preferences {
    section {
    	input(name: "if_switch", type: "capability.switch", title: "Select source switch", required: true, multiple: false, description: null)
    }
	section {
        input(name: "than_switch", type: "capability.switch", title: "Select target switch", required: true, multiple: false, description: null)
        input(name: "on_delay", type: "number", title: "ON delay time", required: false, description: "in minutes (blank:disable, 0:instant)")
        input(name: "off_delay", type: "number", title: "OFF delay time", required: false, description: "in minutes (blank:disable, 0:instant)")
        input(name: "always_off_delay", type: "number", title: "will be off if sorcue is off", required: false, description: "in minutes (blank:disable)")
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
    //state.ondelay_start = false
    //state.offdelay_start = false
    subscribe(if_switch, "switch", if_switch_handler)
    subscribe(than_switch, "switch", than_switch_handler)
}

// TODO: implement event handlers
def if_switch_handler(evt) {
	unschedule()
	if (evt.value == "on") {
    	log.debug "${if_switch} on event"
        if (on_delay instanceof Number) {
            if (on_delay > 0) {
                runIn(on_delay * 60, delayed_on_work)
            } else {        	
                than_switch.on()
            }
        }
    } else if (evt.value == "off") {
    	log.debug "${if_switch} off event"
        if (on_delay instanceof Number) {
            if (off_delay > 0) {
                runIn(off_delay * 60, delayed_off_work)
            } else  {
                than_switch.off()
            }
        }
    }
}

def delayed_on_work() {
    than_switch.on()
}

def delayed_off_work() {
    than_switch.off()
}

def than_switch_handler(evt) {
    unschedule()
	if (evt.value == "on") {
    	log.debug "${than_switch} on event, unschedule all"
        def ifState = if_switch.currentState("switch")
		log.debug "state value: ${ifState.value}"
        if ((ifState.value == "off") && (always_off_delay instanceof Number)) {
    		log.debug "${than_switch} will be off after ${always_off_delay} min"
            runIn(always_off_delay * 60, delayed_off_work)
        }
    } else if (evt.value == "off") {
    	log.debug "${than_switch} off event, unschedule all"
    }
}
