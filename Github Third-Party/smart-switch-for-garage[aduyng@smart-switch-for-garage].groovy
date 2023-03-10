/**
 *  Smart Switch for Garage
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
    name: "Smart Switch for Garage",
    namespace: "com.aduyng",
    author: "Duy Nguyen",
    description: "Turn a z-wave switch and a tilt sensor to a garage controller",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input("theSensor", "capability.contactSensor", title: "Which contact or tilt sensor?", required: true)
        input("theSwitch", "capability.switch", title: "Which switch?", required: true)
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
	subscribe(theSensor, "contact", theSensorContactHanlder)
}

def theSensorContactHanlder(evt){
	if (evt.value == "open") {
        log.debug "sensor open"
    } else if (evt.value == "closed") {
        log.debug "sensor closed"
    }
	theSwitch.off();
}