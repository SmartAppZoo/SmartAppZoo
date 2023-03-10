/**
 *  Alarm Automations
 *
 *  Copyright 2016 Randy Shaddach
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
    name: "Alarm Automations",
    namespace: "rjshadd",
    author: "Randy Shaddach",
    description: "Performs various activities based on the change in state of the Smart Home Monitor alarm.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true)


preferences {
	page(name: "awayPage", title: "Away Mode", install: false, uninstall: true, nextPage: "stayPage") {
    	//away mode triggers
        section("Set Smart Home Monitor to AWAY when...") {
        	input "awayMomentaryTrigger", "capability.momentary", title: "This momentary switch is pressed", multiple: false, required: false
    	}
        //away mode actions
        section("When Smart Home Monitor is set to AWAY...") {
            input "awayMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "awaySwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "awaySwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
	}
    
    page(name: "stayPage", title: "Stay Mode", install: false, uninstall: true, nextPage: "disarmPage") {
    	//stay mode triggers
        section("Set Smart Home Monitor to STAY when...") {
        	input "stayMomentaryTrigger", "capability.momentary", title: "This momentary switch is pressed", multiple: false, required: false
    	}
        //stay mode actions
        section("When Smart Home Monitor is set to STAY...") {
            input "stayMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "staySwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "staySwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
	}
    
    page(name: "disarmPage", title: "Disarm Mode", install: true, uninstall: true) {
        //disarm mode triggers
        section("Set Smart Home Monitor to DISARM when...") {
        	input "disarmMomentaryTrigger", "capability.momentary", title: "This momentary switch is pressed", multiple: false, required: false
    	}
        //disarm mode actions
        section("When Smart Home Monitor is set to DISARM...") {
            input "disarmMode", "mode", title: "Change to this mode", multiple: false, required: false
            input "disarmSwitchesOn", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "disarmSwitchesOff", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
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

//subscribe to alarm state changes
def initialize() {
	//subscribe to away mode triggers
    if (awayMomentaryTrigger) {
    	subscribe(awayMomentaryTrigger, "switch.on", awayTriggerHandler)
    }
    
    //subscribe to stay mode triggers
    if (stayMomentaryTrigger) {
    	subscribe(stayMomentaryTrigger, "switch.on", stayTriggerHandler)
    }
    
    //subscribe to disarm mode triggers
    if (disarmMomentaryTrigger) {
    	subscribe(disarmMomentaryTrigger, "switch.on", disarmTriggerHandler)
    }
    
    //subscribe to shm alarm mode changes
	subscribe(location, "alarmSystemStatus", alarmHandler)
}

//event handler for actions that trigger SHM away
def awayTriggerHandler(evt) {
	log.info "AlarmAutomations changing SHM to AWAY (${evt.name} changed to ${evt.value})"
    sendLocationEvent(name: "alarmSystemStatus", value: "away")
}

//event handler for actions that trigger SHM stay
def stayTriggerHandler(evt) {
	log.info "AlarmAutomations changing SHM to STAY (${evt.name} changed to ${evt.value})"
    sendLocationEvent(name: "alarmSystemStatus", value: "stay")
}

//event handler for actions that trigger SHM disarm
def disarmTriggerHandler(evt) {
	log.info "AlarmAutomations changing SHM to DISARM (${evt.name} changed to ${evt.value})"
    sendLocationEvent(name: "alarmSystemStatus", value: "off")
}

//alarm state change event handler
def alarmHandler(evt) {
	//log.debug "AlarmAutomations handler value: ${evt.value}"
	//log.debug "alarm state: ${location.currentState("alarmSystemStatus")?.value}"
    
    switch (evt.value) {
    	case "away":
            //log.debug "AlarmAutomations handling away mode actions"
        
            changeMode(settings.awayMode)
            changeSwitches(settings.awaySwitchesOn, true)
            changeSwitches(settings.awaySwitchesOff, false)
            break
		case "stay":
        	//log.debug "AlarmAutomations handling stay mode actions"
        
            changeMode(settings.stayMode)
            changeSwitches(settings.staySwitchesOn, true)
            changeSwitches(settings.staySwitchesOff, false)
			break
		case "off":
        	//log.debug "AlarmAutomations handling disarmed mode actions"
        
            changeMode(settings.disarmMode)
            changeSwitches(settings.disarmSwitchesOn, true)
            changeSwitches(settings.disarmSwitchesOff, false)   
            break
        default:
        	log.warn "AlarmAutomations received unknown alarm state: ${evt.value}"
	}
}

// function to change the mode
def changeMode(newMode) {
	// only perform actions if a new mode is specified
    if (newMode) {
    	// check if we are already in the desired mode
        if (location.mode != newMode) {
            if (location.modes?.find{it.name == newMode}) {
            	log.info "AlarmAutomations changing mode to $newMode"
                setLocationMode(newMode)
            } else {
                log.warn "Tried to change to undefined mode '${newMode}'"
            }
        }
    } 
}

// function to change switches either on (state = true) or off (state = false)
def changeSwitches(switches, state) {
    if (switches) {
    	if (state == true) {
    		log.info "AlarmAutomations turning switches ${switches} on"
            switches.on()
		} else if (state == false) {
        	log.info "AlarmAutomations turning switches ${switches} off"
            switches.off()
        } else {
        	log.warn "AlarmAutomations received invalid switch state request"
        }
    }
}