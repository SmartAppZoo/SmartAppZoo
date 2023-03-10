/**
 *  Relay Switch
 *
 *  Copyright 2017 Simone
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
    name: "Relay Switch",
    namespace: "Sanfe75",
    author: "Simone",
    description: "This app allows you to use redundant off and/or on switch presses to control secondary lights.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@3x.png")


preferences {

    section("The Master switch whose on and/or off buttons will serve as toggles") {
    paragraph "NOTE: Plain on/off switches are preferable to dimmers.  Be mindful that dimmers may trigger unexpected toggles when turned off or dimmed to 0 (zero).  You've been warned!"
        input "master", "capability.switch", title: "Select", required: true
    }

    section("Redundant presses will toggle") {
        input "slaves", "capability.switch", multiple: true, required: true, title: "Select"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {

    log.debug "Master Switch Changed State: ${evt.isStateChange()}"
    
    if (!evt.isStateChange()) {
        log.debug "Press is redundant, master is ${master.currentSwitch} toggling slaves"
        if (master.currentSwitch == "on") {
            slaves*.on()
        } else {
            slaves*.off()
        }
    }
}