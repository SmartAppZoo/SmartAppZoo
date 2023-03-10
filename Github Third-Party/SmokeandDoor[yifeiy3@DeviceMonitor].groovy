/**
 *  Opens door when smoke detected
 *
 *  Copyright 2020 Eric Yang
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
    name: "Opens door when smoke detected",
    namespace: "yifeiy3",
    author: "Eric Yang",
    description: "When smoke is detected, we open the door to let harmful gas out.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this smokealarm rang...") {
		input "alarms",
        "capability.alarm",
        multiple: false,
        required: true
	}
    section("Unlock door ..."){
    	input "door",
        "capability.lock",
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
    subscribe(alarms, "alarm.siren", detectionhandler)
    subscribe(alarms, "alarm.both", detectionhandler)
    subscribe(app, detectionhandler)
}

def detectionhandler(evt){
	log.debug "smoke is detected"
    door.unlock()
}