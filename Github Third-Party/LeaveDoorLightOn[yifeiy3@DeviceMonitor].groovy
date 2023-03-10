/**
 *  Leave Door Light On
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
    name: "Leave Door Light On",
    namespace: "Yifeiy3",
    author: "Eric Yang",
    description: "When mode is set to away, leave door light on to make people think there is people home.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Door Light") {
		input "switches", 
        "capability.switch",
        multiple: true
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
	subscribe(location, "mode.Away", modeChangeHandler)
 	subscribe(app, appTouch)
}

def appTouch(evt){
	//simulate such mode change event
    location.setMode("Home")
    log.debug "set location mode to $location.mode"
   	location.setMode("Away")
    log.debug "set location mode to $location.mode"
}

def modeChangeHandler(evt){
	switches?.on()
    log.debug "$switches has been turned on"
}