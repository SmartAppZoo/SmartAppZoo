/**
 *  Humidity control
 *
 *  Copyright 2015 TJ Frevert
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
    name: "Humidity control",
    namespace: "tfrevert",
    author: "TJ Frevert",
    description: "This smartapp detects high humidity and turns on a switch for a specified time.",
    category: "My Apps",
    iconUrl: "http://i.imgur.com/F5abzWR.png",
    iconX2Url: "http://i.imgur.com/F5abzWR.png",
    iconX3Url: "http://i.imgur.com/F5abzWR.png")


preferences {
	section("Select a humidity sensor") {
        input "humiditysensor", "capability.relativeHumidityMeasurement", title: "Humidity sensor", required: true, multiple: false
    }
    section("Select a switch") {
        input "fanswitch", "capability.switch", title: "Switch to turn on", required: true, multiple: false
    }
    section("Input humidity high point") {
        input "maxhumidity", "number", title: "Humidity threshold", required: true
    }
    section("Input fan run time in minutes") {
        input "fantimer", "number", title: "Fan timer", required: true
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
	subscribe(humiditysensor, "humidity", checkTurnOn)
}

def checkTurnOn(evt) {
	// log.debug "Humidity Event: $evt"
	log.debug "Unit reads $evt.value and threshold is set to $maxhumidity"

    def unitvalue = Double.parseDouble("$evt.value")

	if (unitvalue >= maxhumidity) {
		// Turn on switch
		log.debug "Turning on the switch and setting timer to $fantimer minutes."
		fanswitch.on()
		runIn(fantimer*60, turnoff)
    } else {
    	log.debug "Humidity test: pass"
    }
}

def turnoff() {
	fanswitch.off()
}
