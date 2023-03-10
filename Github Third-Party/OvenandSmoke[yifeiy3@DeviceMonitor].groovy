/**
 *  Oven and Smokealarm
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
    name: "Oven and Smokealarm",
    namespace: "yifeiy3",
    author: "Eric Yang",
    description: "Arbitrary set smokealarm to ring after oven has been on for a period of time. For testing purposes.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When ... has been operating") {
		input "ooven",
        "capability.ovenOperatingState",
        required:true
	}
    section("For ... minutes"){
    	input "timer",
        "number",
        required: true
    }
    section("Set smoke alarm ... to sense smoke"){
    	input "smokeAlarm",
        "capability.alarm",
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
	// TODO: subscribe to attributes, devices, locations, etc.
    ooven.setMachineState("paused")
    subscribe(app, apphandler)
}

def apphandler(evt){
	log.debug "App has been called"
    ooven.setMachineState("running")
    runIn(timer, changeState)
}

def changeState(evt){
	log.debug "Smoke alarm rang"
	smokeAlarm.siren()
}
// TODO: implement event handlers