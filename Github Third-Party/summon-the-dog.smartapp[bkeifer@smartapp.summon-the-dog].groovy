/**
 *  Summon the Dog
 *
 *  Copyright 2014 Brian Keifer
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
    name: "Summon the Dog",
    namespace: "",
    author: "Brian Keifer",
    description: "Turn lights on and off 3 times.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	section("Lights to blink:") {
		input "switches", "capability.switch", multiple: true
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
	subscribe(app, appTouch)
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
    switches?.off()
    pause(500)
	switches?.on()
    pause(500)
    switches?.off()
    pause(500)
	switches?.on()
    pause(500)
    switches?.off()
    pause(500)
	switches?.on()
}

