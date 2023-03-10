/**
 *  Garage Door Control
 *
 *  Copyright 2017 Dan Goscomb
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
    name: "Garage Door Control",
    namespace: "dangoscomb",
    author: "Dan Goscomb",
    description: "Garage door control",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Sensor") {
		input("sensor","capability.presenceSensor")
	}
    section("Door") {
		input("gdoor","capability.garageDoorControl")
	}
    section("Light") {
		input("glight","capability.switch")
	}
    section("Timeframes") {
    	input "minm", "number", required: true, title: "Min. Minutes Away?"
        input "maxm", "number", required: true, title: "Max. Minutes Away?"
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
	subscribe(sensor, "presence", presence)
    log.debug "sensor: $sensor.currentPresence"
}

def presence(evt) {
    log.debug "evt.name: $evt.value"
    if (evt.value == "not present") {
        log.debug "checking the garage door is closed"
        gdoor.close()
        glight.off()
    }
    else {
        log.debug "present; check we've been out for a while..."
        def hist = sensor.events()
        def diff = ((evt.date.getTime() - hist[1].date.getTime())/1000)/60
       	log.debug "Time away: $diff"
        if(diff > minm && diff < maxm) {
        	log.debug "Timeframe constraint met... opening the garage"
            gdoor.open();
            glight.on();
            glight.setLevel(99);
        }
        else {
			log.debug "Timeframe constraint check failed. $minm - $maxm"
		}
    }
}