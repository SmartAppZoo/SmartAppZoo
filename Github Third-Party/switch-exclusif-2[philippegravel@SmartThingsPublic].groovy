/**
 *  Switch exclusif 2
 *
 *  Copyright 2016 Philippe Gravel
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
    name: "Switch exclusif 2",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Set one switch exclusif into group",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("All Switchs link...") {
		input "exclusifSwitchs", "capability.switch", title: "Switchs Link?", multiple: true, required: true
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
	subscribe(exclusifSwitchs, "switch.on", onHandler)
}

def onHandler(evt) {
	log.debug "Events: " + evt.displayName
    
	for (switches in exclusifSwitchs) {
    	log.debug "Switch: " + switches.displayName
    	if (switches.displayName != evt.displayName) {
        	log.debug "Turn Off"
        	switches.off()
        }
    }
}