/**
 *  Smart Switch for Toilet
 *
 *  Copyright 2017 Duy Nguyen
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
    name: "Smart Switch for Toilet",
    namespace: "com.aduyng",
    author: "Duy Nguyen",
    description: "Turn on the fan automatically when someone in the toilet for more than x minutes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input("theSensor", "capability.motionSensor", title: "Which motion sensor?", required: true)
        input("theSwitch", "capability.switch", title: "Which switch?", required: true)
        input("turnOnTimeout", "number", title: "How long should the fan be turned on?", required: true, defaultValue: 1)
        input("turnOffTimeout", "number", title: "How long should the fan be turned off?", required: true, defaultValue: 5)
        input("interval", "number", title: "What is the interval (second)", required: true, defaultValue: 60)
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
	subscribe(theSensor, "motion.active", theSensorActiveHandler)
    subscribe(theSensor, "motion.inactive", theSensorInactiveHandler)
}

def theSensorActiveHandler(event){
	log.debug "sensor is active"
    unschedule()
    runIn(interval*turnOnTimeout, turnTheSwitchOn);
}

def theSensorInactiveHandler(event){
	log.debug "sensor is INACTIVE"
    unschedule()
    runIn(interval*turnOffTimeout, turnTheSwitchOff);
}

def turnTheSwitchOn(){
	theSwitch.on()
    unschedule()
}

def turnTheSwitchOff(){
	theSwitch.off()
    unschedule()
}