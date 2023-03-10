/**
 *  Routine Automation
 *
 *  Copyright 2016 Randy Shaddach
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
	name: "Routine Automation",
	namespace: "rjshadd/smart routines",
	author: "Randy Shaddach",
	description: "Child of the Smart Routines SmartApp. Executes a routine based on a simple trigger.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	parent: "rjshadd/smart routines:Smart Routines"
	)


preferences {
	page name: "mainPage", title: "Automate a Routine", install: false, uninstall: true, nextPage: "namePage"
	page name: "namePage", title: "Automate a Routine", install: true, uninstall: true
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
	// if the user did not override the label, set the label to the default
	if (!overrideLabel) {
		app.updateLabel(defaultLabel())
	}
	// schedule the switch handler to trigger the routine
	log.debug "Subscribe to $theSwitch turning $switchState"
	subscribe(theSwitch, "switch.$switchState", handler)
}

// main page to select the trigger and routine to run
def mainPage() {
	dynamicPage(name: "mainPage") {
		section("Select the control switch and state") {
			input "theSwitch", "capability.switch", required: true
			input "switchState", "enum", title: "Switch state", options: ["on","off"], required: true
		}

		def actions = location.helloHome?.getPhrases()*.label
		if (actions) {
			actions.sort()
			
			section("Select the routine to run") {
				log.trace actions
				input "theRoutine", "enum", title: "Routine to execute when turned on", options: actions, required: true
			}
		}
	}
}

// page for allowing the user to give the automation a custom name
def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

// a method that will set the default label of the automation.
// It uses the switch and routine to create the automation label
def defaultLabel() {
    "Run $theRoutine when $theSwitch turns $switchState"
}

// the switch handler method that runs the routine 
def handler(evt) {
	log.debug "${settings.theSwitch} (${settings.switchState}) triggers routine ${settings.theRoutine}"
	location.helloHome?.execute(settings.theRoutine)
}
