/**
 *  HarrisTribe Bedroom Button
 *
 *  Copyright 2019 Robert Harris
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
    name: "HarrisTribe Bedroom Button",
    namespace: "harrisra",
    author: "Robert Harris",
    description: "Extra capability",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Button") {
		input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
	}    
    
	section("Bedroom Light") {
		input "bedroomLightDevice", "capability.swicth", title: "Bedroom Light", multiple: false, required: true
	}    
    
	section("Bedside Light") {
		input "bedsideLightDevice", "capability.swicth", title: "Bedside Light", multiple: false, required: true
	} 
    
	section("Desk Light") {
		input "deskLightDevice", "capability.swicth", title: "Desk Light", multiple: false, required: false
	}     
    
	section("Fan") {
		input "deskLightDevice", "capability.swicth", title: "Fan", multiple: false, required: false
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
	subscribe(buttonDevice, "button", buttonEvent)
}

// TODO: implement event handlers


def buttonEvent(evt) {

	// has the same event occured within the last 3 seconds
    def buttonPresses = buttonDevice.eventsSince(new Date(now() - 3000))
    								.findAll { (it.value == "pushed" || it.value == "double") && it.data == evt.data }
                                    .sum { if(it.value == "double") 2 else 1  }
    
    log.debug "Found ${buttonPresses} button press in past 3 seconds"   
    
	switch(evt.value) {
    	case "pushed": 	log.debug "single press detected" 
        				break
        case "double": 	log.debug "double press detected" 
        				break
        case "held":   	log.debug "hold press detected" 
        				break
        default: log.debug "unknown event name: "+evt.name 
    }
//	log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"

    def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
    def value = evt.value
/*
    if(recentEvents.size <= 1) {
        //executeHandlers(1, value)
    } else {
        log.debug "Found recent button press events for $buttonNumber with value $value"
    } */
    
}

def toggleBedsideLight