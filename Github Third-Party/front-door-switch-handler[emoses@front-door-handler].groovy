/**
 *  Front Door Switch Handler
 *
 *  Copyright 2017 Evan Moses
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
    name: "Front Door Switch Handler",
    namespace: "emoses",
    author: "Evan Moses",
    description: "Make the front door switch work.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "setupPage")
}

def setupPage() {
    dynamicPage(name: "setupPage", title: "", install: true, uninstall: true) {
        section("Switches and Lights") {
            input "doorSwitch", "capability.button", required: true, title: "When this is pressed"
            input "lights", "capability.switchLevel", multiple: true, title: "Turn these on"
            input "allLights", "capability.switch", multiple: true, title: "...or turn these off"
        }
        // get the available actions
            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            // sort them alphabetically
            	actions.sort()
                section("Select Routine for double-tap") {
                	// use the actions as the options for an enum input
                	input "action", "enum", title: "Select an action to execute", options: actions
                }
            }
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
    subscribe(doorSwitch, "button", pushHandler)
}

def pushHandler(evt) {
    def currentStates = lights.collect { it.currentState("switch") }
    log.debug currentStates
    if (currentStates.any { it.value == "on" }) {
    	log.debug "Some downstairs lights were on, turning everything off"
    	allLights.each {it.off()}
    } else { 
    	log.debug "No downstairs lights were on, turning them on"
        lights.each { it.setLevel(75); it.on()}
    }
}
