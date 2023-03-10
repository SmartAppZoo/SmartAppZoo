/**
 *  His and Hers Nightstand Lamp Button
 *
 *  Copyright 2020 Justin Wildeboer
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
    name: "His and Hers Nightstand Lamp Switch",
    namespace: "gpzj",
    author: "Justin Wildeboer",
    description: "Apply this to a virtual switch (or any switch really) so that if you turn off a targeted light/switch, it will turn off all others, as well. ",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom11-icn@2x.png")

preferences {
	section("Button to Use:") {
        input "myButton", "capability.button", required: true, title: "Who's Button?"
	}
    section("Lamps to Control:") {
        input "myLamp", "capability.switch", required: true, multiple: true, title: "Main Lamp? (Press Button)"
        input "theirLamp", "capability.switch", required: true, multiple: true, title: "Other Person's Lamp? (Double-Press Button)"
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
    // def bothLights = settings.collect {k, device
    subscribe(myButton, "button", mainHandler)
}

def toggleMyLamp() {
    if ($myLamp.SwitchState == on) {
        log.debug "$myLamp is on, turning off."
        $myLamp.off()
    } else {
        log.debug "$myLamp is off, turning on."
        $myLamp.on()
    }
}

def doublePress() {
    if ($myLamp.SwitchState == on) {
        if ($theirLamp.SwitchState == off) {
            $theirLamp.on()
        } else {
            $myLamp.off()
            $theirLamp.off()
	}
    } else {
        // put these in a collection $bothLights
        $myLamp.on()
        $theirLamp.on()
    }
}

def mainHandler(evt) {
    def data = parseJson(evt.data)
    log.debug "event data: ${data}"
    log.debug "switchOnHandler called: $evt"
    if ($evt.value == pressed) {
        log.debug "Toggling $myLamp."
        toggleMyLamp()
    } else if ($evt.value == held) {
        log.debug "Double Pressed."
        doublePress() 
    }
}
