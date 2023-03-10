/**
 *  Switch Sync
 *
 *  Copyright 2017 Josh Bush
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
	name: "Switch Sync",
	namespace: "digitalbush",
	author: "Josh Bush",
	description: "Keep switches in sync",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Switches to keep in sync") {
		input "switches", "capability.switch", title: "Choose Switches", multiple: true
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(switches, "switch", switchHandler)
}

def switchHandler(evt) {
    if (evt.type != "physical") {
    	return;
    }
    
    def switchesToFlip = findSwitchesNotInState( evt.value );
    switchesToFlip.each { s ->
	    s."$evt.value"()	
    }
}


def findSwitchesNotInState( state ) {
	def currSwitches = switches.currentSwitch
	switches.findAll { it.currentSwitch != state }
}