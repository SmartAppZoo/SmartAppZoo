/**
 *  Good Night App
 *
 *  Copyright 2016 Todd Erickson
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
    name: "Good Night App",
    namespace: "terickson",
    author: "Todd Erickson",
    description: "This app is linked to a good night routine momentary button tile in order to run a good night routine.  It is necessary in order to have Alexa be able to execute a good night routine that not only turns off lights but also locks doors.  ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this switch is turned on") {
		input "master", "capability.switch", title: "Where?"
	}
    
	section("And turn off all of these switches") {
		input "offSwitches", "capability.switch", multiple: true, required: false
	}
    
	section("And close these garage doors") {
		input "offGDoors", "capability.garageDoorControl", multiple: true, required: false
	} 
    
	section("And lock All these locks") {
		input "offLocks", "capability.lock", multiple: true, required: false
	}    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(master, "button.push", onHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(master, "switch.on", onHandler)
}

def onHandler(evt) {
	log.debug evt.value
	log.debug offSwitches
	log.debug offLocks
	log.debug offGDoors
	offSwitches*.off()
	offLocks*.lock()
	offGDoors*.close()
}
