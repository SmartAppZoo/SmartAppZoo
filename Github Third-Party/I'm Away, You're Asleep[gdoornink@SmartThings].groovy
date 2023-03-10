/**
 *  I'm away, You're asleep
 *
 *  Copyright 2017 GTDoor
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
    name: "I'm away, You're asleep",
    namespace: "GTDoor",
    author: "Greg Doornink",
    description: "Set modes based on presence and sleep indicators for two people.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "pageOne", title: "Inputs", nextPage: "pageThreeg", uninstall: true) { // changed pageTwo to pageThree
		section("Greg") {
			input "gregHomeSensor", "capability.presenceSensor", required: true, title: "Home Sensor"
	        input "gregAwakeSensor", "capability.switch", required: true, title: "Awake Sensor"
		}
	    section("Tiffany") {
			input "tiffanyHomeSensor", "capability.presenceSensor", required: true, title: "Home Sensor"
	        input "tiffanyAwakeSensor", "capability.switch", required: true, title: "Awake Sensor"
		}
    }
//    page(name: "pageTwo", title: "Modes", nextPage: "pageThree") {
//	    section("Basics") {
//        	input "gregHomeTiffanyHome", "mode", title: "Both Home", multiple: false, required: false
//	    	input "gregAsleepTiffanyAsleep", "mode", title: "Both Asleep", required: false
//	        input "gregAwayTiffanyAway", "mode", title: "Both Away", required: false
//	    }
//            section("Greg is Home, and...") {
//	    	input "gregHomeTiffanyAway", "mode", title: "Tiffany is Away", required: false
//	    	input "gregHomeTiffanyAsleep", "mode", title: "Tiffany is Asleep", required: false
//	    }
//        section("Tiffany is Home, and...") {
//	    	input "gregAwayTiffanyHome", "mode", title: "Greg is Away", required: false
//	    	input "gregAsleepTiffanyHome", "mode", title: "Greg is Asleep", required: false
//	    }
//        section("Away and Asleep") {
//        	input "gregAwayTiffanyAsleep", "mode", title: "Greg is Away, Tiffany is Asleep", required: false
//	    	input "gregAsleepTiffanyAway", "mode", title: "Tiffany is Away, Greg is Asleep", required: false
//        }
//    }
    page(name: "pageThree", title: "Finishing Touches", install: true) {
    	section() {
			label title: "Assign a name", required: false
        	mode title: "Set for specific mode(s)", required: false
        }
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
	subscribe(gregHomeSensor, "presence", changeDetectedHandler)
    subscribe(gregAwakeSensor, "presence", changeDetectedHandler)
    subscribe(tiffanyHomeSensor, "presence", changeDetectedHandler)
    subscribe(tiffanyAwakeSensor, "presence", changeDetectedHandler)
    log.debug gregAwayTiffanyAway
    log.debug gregHomeTiffanyHome
}

def changeDetectedHandler(evt) {
    if (gregHomeSensor.currentValue("presence") == "not present") {
    	log.debug "Greg is not home."
    	if (tiffanyHomeSensor.currentValue("presence") == "not present") {
        	log.debug "Tiffany is not home."
            location.setMode("Away")
            //location.setMode(gregAwayTiffanyAway)
        } else if (tiffanyAwakeSensor.currentValue("switch") == "off") {
        	log.debug "Tiffany is asleep."
            location.setMode("Night")
            //location.setMode(gregAwayTiffanyAsleep)
        } else {
        	log.debug "Tiffany is home and awake."
            location.setMode("Home")
            //location.setMode(gregAwayTiffanyHome)
        }
    } else if (gregAwakeSensor.currentValue("switch") == "off") {
    	log.debug "Greg is asleep."
		if (tiffanyHomeSensor.currentValue("presence") == "not present") {
        	log.debug "Tiffany is not home."
            location.setMode("Night")
            //location.setMode(gregAsleepTiffanyAway)
        } else if (tiffanyAwakeSensor.currentValue("switch") == "off") {
        	log.debug "Tiffany is asleep."
            location.setMode("Night")
            //location.setMode(gregAsleepTiffanyAsleep)
        } else {
        	log.debug "Tiffany is home and awake."
            location.setMode("Home")
            //location.setMode(gregAsleepTiffanyHome)
        }
    } else {
    	log.debug "Greg is home and awake."
		if (tiffanyHomeSensor.currentValue("presence") == "not present") {
        	log.debug "Tiffany is not home."
            location.setMode("Home")
            //location.setMode(gregHomeTiffanyAway)
        } else if (tiffanyAwakeSensor.currentValue("switch") == "off") {
        	log.debug "Tiffany is asleep."
            location.setMode("Home")
            //location.setMode(gregHomeTiffanyAsleep)
        } else {
        	log.debug "Tiffany is home and awake."
            location.setMode("Home")
            //location.setMode(gregHomeTiffanyHome)
        }
    }
}
