/**
 *  Virtual Switch to Disable Code Unlock
 *
 *  Copyright 2015 Kevin Tierney
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
    name: "Virtual Switch to Disable Code Unlock",
    namespace: "tierneykev",
    author: "Kevin Tierney",
    description: "Use a virtual switch to disable/enable code unlocking for schlage lock",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
		section("When this switch is toggled...") { 
		input "theSwitch", "capability.switch", 
			multiple: false, 
			required: true
	}

	section("Toggle Code Unlock on this lock...") {
		input "theLock", "capability.lock", 
			multiple: false, 
			required: true
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
	subscribe(theSwitch, "switch.on", switchOnHandler)
	subscribe(theSwitch, "switch.off", switchOffHandler)

    
}
def switchOnHandler(evt){
	theLock.enableCodeunlock()
}


def switchOffHandler(evt){
	theLock.disableCodeunlock()
}

