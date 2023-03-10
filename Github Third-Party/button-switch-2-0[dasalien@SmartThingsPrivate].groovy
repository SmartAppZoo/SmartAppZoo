/**
 *  Button Switch Test
 *
 *  Copyright 2016 Dieter Rothhardt
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
    name: "Button Switch 2.0",
    namespace: "dasalien",
    author: "Dieter Rothhardt",
    description: "Turn device on/off from a different switch's off position",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    	section("When this switch is pushed to off while off...") {
    		input "buttonswitch1", "capability.button", title: "Which?", required: true, multiple: true
    	}
        
        section("Then toggle this light switch...") {
        	input "hallSwitch", "capability.switch", title: "Which?", required: true, multiple: true
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
    subscribe(buttonswitch1, "button", toggleButtonSwitchHandler)
    state.hallLightOn = false    
}

//Turn hall light off if on
def toggleButtonSwitchHandler(evt) {
	log.debug "Event: $evt"
    log.debug "toggleButtonSwitchHandler"
    
    if(state.hallLightOn) {
    	hallSwitch.off()
    	state.hallLightOn = false;
    } else {
    	hallSwitch.on()
        state.hallLightOn = true;
    }
}