/**
     *  WC Fan On/Off
     *
     *  Copyright 2015
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
        name: "Automatic WC Fan",
        namespace: "Fuzzy Universe",
        author: "CAC",
        description: "Turn fan on after light on",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
    
    preferences {
    	section("When this switch is turned on...") {
    		input "switch1", "capability.switch", title: "Which?", required: true, multiple: false
    	}
        
        section("This many minutes later...") {
        	input "minutesOn", "number", title: "How many?", required: true, multiple: false
        }
        
        section("Then turn on this fan switch...") {
        	input "fanSwitch", "capability.switch", title: "Which?", required: true, multiple: false
        }
        
        section("And then turn it off this many minutes...") {
        	input "minutesOff", "number", title: "after the switch turns off", required: true, multiple: false
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
    log.debug "Bath Fan App Init"
    subscribe(switch1, "switch.on", switchOnHandler)
    subscribe(switch1, "switch.off", switchOffHandler)
    subscribe(fanSwitch, "switch", fanHandler)

    switch1.off()
    fanSwitch.off()
    state.fanOn = false
}

def fanHandler(evt) {
    if (evt.value == "on") {
    	state.fanOn = true 
        log.debug "Fan switch turned on!"
    } else if (evt.value == "off") {
	    state.fanOn = false
        log.debug "Fan switch turned off!"
    }
}
 
def switchOnHandler(evt) {
    log.debug "Light Switch turned on. Is fan on? ${state.fanOn}"
    state.runFan = true
    if(minutesOn >= 0) runIn(minutesOn*60, fanOn)
}

def switchOffHandler(evt) {
    log.debug "Light Switch turn off. Is fan on? ${state.fanOn}"
    state.runFan = false
    if(minutesOff >= 0) runIn(minutesOff*60, fanOff)
}

def fanOn() {
    if(!state.fanOn && state.runFan) {
    	fanSwitch.on()
    	state.fanOn = true
    }
}

def fanOff() {
    if(state.fanOn) fanSwitch.off()
    state.fanOn = false
}