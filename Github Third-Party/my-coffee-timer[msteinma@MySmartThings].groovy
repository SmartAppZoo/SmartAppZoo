/**
 *  My Coffee Timer
 *
 *  Copyright 2018 Maurice Steinman
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
    name: "My Coffee Timer",
    namespace: "msteinma",
    author: "Maurice Steinman",
    description: "Turn on the coffee maker at specified time",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-SmarterMorningCoffee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-SmarterMorningCoffee@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-SmarterMorningCoffee@2x.png")


preferences {
	section("Configuration settings") {
	        
        input "coffeeSwitch", "capability.switch", title: "Which switch controls the coffee maker?"
        input "coffeeTime","time", title: "When do you want the coffee maker to turn on?"
        input "coffeeTimerEnabled", "bool", title:"Enable?", defaultValue:false
        
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
    if (coffeeTimerEnabled) {
    	coffeeSwitch.off()
    	schedule(coffeeTime, turniton)
    }
}

def turniton() {
     log.debug "turniton called at ${new Date()}"
    coffeeSwitch.on()
    
}

// TODO: implement event handlers