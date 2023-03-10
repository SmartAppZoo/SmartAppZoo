/**
 *  Monitor a switch for other actions
 *
 *  Copyright 2015 Phil Bianco
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
    name: "Monitor a switch for other actions",
    namespace: "pbianco",
    author: "Phil Bianco",
    description: "This application will monitor a switch status and turn on other devices.  There will also be an optional timer. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Monitor this switch..."){
		input "switch1", "capability.switch", multiple: false, required: true
    }
    section("Turn on this device..."){
        input "switch2", "capability.switch", multiple: true, required: true
    }
    section("Timer to turn off device...") {
        input "delayMinutes", "number", title: "Minutes:", required: false
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
	
    subscribe(switch1, "switch", switchHandler, [filterEvents: false])
}

def switchHandler (evt){

	log.debug "In switchHandler" 
    
    if (evt.value == "on"){
       switch2.on()
       if (delayMinutes != null) {
          log.debug "Delay has been set to ${delayMinutes}"
          runIn(delayMinutes*60, turnOffDevice, [overwrite: false])
       }
    }
    if (evt.value == "off"){
       switch2.off()
    }
}

def turnOffDevice() {
   
   log.debug "In turnOffDevice"
   switch2.off()
   
}
    
    