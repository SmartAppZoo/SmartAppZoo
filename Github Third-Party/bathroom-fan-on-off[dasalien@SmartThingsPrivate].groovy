/**
     *  Bath Fan On/Off
     *
     *  Copyright 2015 Bruce Ravenel
     *  Modified by Dieter Rothhardt 2016
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
        name: "Bathroom Fan On/Off",
        namespace: "dasalien",
        author: "Dieter Rothhardt",
        description: "Turn bath fan on after some period of time",
        category: "Convenience",
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
    subscribe(switch1, "switch.on", lightSwitchOnHandler)
    subscribe(switch1, "switch.off", lightSwitchOffHandler)
    subscribe(fanSwitch, "switch.on", fanSwitchOnHandler)
    subscribe(fanSwitch, "switch.off", fanSwitchOffHandler)
    
    state.lightOn = false
    state.fanOn = false
    switch1.off()
    fanSwitch.off()
}

//Turn Fan on if more than x minutes of light switch on
def lightSwitchOnHandler(evt) {
	log.debug "lightSwitchOnHandler"
	state.lightOn = true    
    if(minutesOn > 0) runIn(minutesOn*60, fanOn) else fanOn()
}

def lightSwitchOffHandler(evt) {
    log.debug "lightSwitchOffHandler"
    state.lightOn = false
    if(state.fanOn) {
    	log.debug "timing fan off"
    	if(minutesOff > 0) runIn(minutesOff*60, fanOff) else fanOff()
    }
}

def fanSwitchOnHandler(evt) {
	log.debug "fanSwitchOnHandler"
	state.fanOn = true
    if(!state.lightOn) {
    	log.debug "timing fan off"
    	if(minutesOff > 0) runIn(minutesOff*60, fanOff) else fanOff()
    }
}

def fanSwitchOffHandler(evt) {
	log.debug "fanSwitchOffHandler"
    state.fanOn = false
}

def fanOn() {
    log.debug "fanOn"
    //Switch on only, if light is still on
    if(state.lightOn) {
    	fanSwitch.on()
    	state.fanOn = true
    }
}

def fanOff() {
    log.debug "fanOff"
    fanSwitch.off()
    state.fanOn = false
}