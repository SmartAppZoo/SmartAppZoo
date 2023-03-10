/**
 *  I'm awake!
 *
 *  Copyright 2015 Peter Dunshee
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
    name: "I'm awake!",
    namespace: "Petezah",
    author: "Peter Dunshee",
    description: "Switch to your 'awake' mode when certain switches are used in your home.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", install: true, uninstall: true) {
    
    	section() {
			input(name: "switches", type: "capability.switch", title: "Switches", description: "Switches to monitor for activity", multiple: true, required: true)
			input(name: "awakePhrase", type: "enum", title: "Execute a phrase when I'm awake", options: listPhrases(), required: true)
			input(name: "applicableModes", type: "mode", title: "Do this only if I am in one of these modes", description: "The modes that this app is applicable to", multiple: true, required: true)
        }
	}
}

// Lifecycle management
def installed() {
	log.debug "<I'm awake> Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "<I'm awake> Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(switches, "switch.on", switchHandler)
}

// Event handlers
def switchHandler(evt) {
	log.debug "<I'm awake> switchHandler: $evt"

	if (allOk) {
		if (awakePhrase) {
			log.debug "<I'm awake> executing: $awakePhrase"
			executePhrase(awakePhrase)
		}
    }
}

// Helpers
private listPhrases() {
	location.helloHome.getPhrases().label
}

private executePhrase(phraseName) {
	if (phraseName) {
		location.helloHome.execute(phraseName)
		log.debug "<I'm awake> executed phrase: $phraseName"
	}
}

private getAllOk() {
	modeOk
}

private getModeOk() {
	def result = !applicableModes || applicableModes.contains(location.mode)
	log.trace "<I'm awake> modeOk = $result"
	result
}

