/**
 *  Momentary Switch to Run Routine
 *
 *  Copyright 2016 Ryan Haack
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
    name: "Switch Runs Routine",
    namespace: "haackr",
    author: "Ryan Haack",
    description: "When a switch turns on, run a routine and immediately turn the switch back off. This is designed to be used with virtual switches so things like IFTTT can be used to run routines.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "appPrefrences")
}

def appPrefrences() {
    dynamicPage(name: "appPrefrences", title: "App Prefrences", install: true, uninstall: true) {
		section(title: "Switch"){
        	input "theSwitch", "capability.switch", requried: true, multiple: false
        }
        // get the available routines
        def routines = location.helloHome?.getPhrases()*.label
        if (routines) {
            // sort them alphabetically
            routines.sort()
            section("Routine") {
                log.trace routines
                // use the routines as the options for an enum input
                input(name: "theRoutine", type: "enum", options: routines, required: true, multiple: false)
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
    subscribe(theSwitch, "switch.on", "switchOnHandler")
}

def switchOnHandler(evt){
	runRoutineAndSwitchOff()
}

def runRoutineAndSwitchOff(){
	//Execute the selected routine and turn the switch back off.
	log.trace("Running $settings.theRoutine and turning off $settings.theSwitch.")
	location.helloHome?.execute(settings.theRoutine)
    theSwitch.off()
}