/**
 *  Copyright 2015 SmartThings
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
 *  boilerEnable
 *  Author: doIHaveTo
 */

definition(
    name: "switchMonitor",
    namespace: "doIHaveTo",
    author: "doIHaveTo",
    description: "Monitor devices and turn on/off switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Monitor...") {
		input "monSwitches", "capability.switch", required: false, multiple:true
	}

	section("Turn on/off things...") {
		input "conSwitches", "capability.switch", required: false, multiple:true
	}
}

def installed() {
	log.debug "Installed with settings ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings ${settings}"
	unsubscribe()
    initialize()
}

def initialize() {
	subscribe(monSwitches, "switch", monSwitchCheck)
	subscribe(conSwitches, "switch", conSwitchCheck)
    runEvery15Minutes(conSwitchCheckNE)
}


def monSwitchCheck(evt) {
	log.trace "$evt.displayName changed to $evt.value"
    
    def currSwitches = monSwitches.currentSwitch
    def numberOfSwitches = currSwitches.size()
    def offSwitches = currSwitches.findAll { switchVal ->
    	switchVal == "off" ? true : false
    }
    def numberOfOffSwitches = offSwitches.size()
    
	if (numberOfSwitches == numberOfOffSwitches) {
	/* All switches are off so boiler should be off*/
    	log.debug "mon: All selected switches are off ${monSwitches} ${conSwitches} ${numberOfSwitches} ${numberOfOffSwitches} ${currSwitches}"
    	conSwitches.off()
    }
    else {
    /* Not all switches are off so boiler should be on*/
    	log.debug "mon: Not all selected switches are off ${monSwitches} ${conSwitches} ${numberOfSwitches} ${numberOfOffSwitches} ${currSwitches}"
       	conSwitches.on()
        
    }
}

def conSwitchCheck(evt) {
	// capture events where the beSwitches turn off but the monitored switches are still on
	log.trace "$evt.displayName changed to $evt.value"
    if (evt.value == "off") {
        def currSwitches = monSwitches.currentSwitch
        def numberOfSwitches = currSwitches.size()
        def offSwitches = currSwitches.findAll { switchVal ->
            switchVal == "off" ? true : false
        }
        def numberOfOffSwitches = offSwitches.size()

        if (numberOfSwitches != numberOfOffSwitches) {
            log.debug "con: Not all selected switches are off ${monSwitches} ${currSwitches}"
            conSwitches.on()
   	 	}    
	}
    else {
    	log.debug "$evt.displayName is already on no need to check monitor switches ${monSwitches}"
    }
}

def conSwitchCheckNE() {
	// every 15 mins check if switches should be on (they turn off somemtimes every hour) there is no event to check
    
    def currSwitches = monSwitches.currentSwitch
    def numberOfSwitches = currSwitches.size()
    def offSwitches = currSwitches.findAll { switchVal ->
        switchVal == "off" ? true : false
    }
    def numberOfOffSwitches = offSwitches.size()

    if (numberOfSwitches != numberOfOffSwitches) {
        log.debug "con: Not all selected switches are off ${monSwitches} ${conSwitches} ${numberOfSwitches} ${numberOfOffSwitches} ${currswitches}"
        conSwitches.on()
    } 
}
