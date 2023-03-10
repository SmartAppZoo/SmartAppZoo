/**
 *  johns doubletap for the rest of us
 *
 *  Copyright 2015 John Lord
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
    name: "Control Lamps with On/Off Doubletap",
    namespace: "",
    author: "John Lord",
    description: "some switches don't report when you turn them on and then hit the button again, because they are already on.  This lets you toggle the switch off and on within 4 seconds and this will trigger a different lamp to toggle.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	
    // What door should this app be configured for?
    section ("When this switch is toggled on and off...") {
        input "switch1", "capability.switch",
              title: "Where?"
    }
  
    
	
    // What light should toggle with a "doubletap"
    section ("Turn on/off this light with doubletap") {
        input "switch2", "capability.switch", multiple: true
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.offTime = now();
    state.onTime = now();
 
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(switch1, "Switch", contactHandler)
}

// event handlers are passed the event itself
def contactHandler(evt) {
    log.debug "$evt.value"


    if (evt.value == "on") {
        if( now() - state.onTime <= 8000)
           {switch2.on();}
        
        state.onTime = now()
    } else if (evt.value == "off") {
        if(now() - state.offTime <= 8000)
           {switch2.off();}
    
        state.offTime = now()
        log.debug state.offTime
    }
   }