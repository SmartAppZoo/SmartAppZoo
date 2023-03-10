/**
 *  GarageLocation
 *
 *  Copyright 2016 Neil MacMillan
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
    name: "GarageLocation",
    namespace: "macmillan",
    author: "Neil MacMillan",
    description: "If members leave the area, make sure the garage is closed - if not message everybody",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "MacHome", displayLink: ""])


preferences {
	section("Garage to Monitor") {
        input "contact1", "capability.contactSensor", title: "Which door?"
        input "presence1", "capability.presenceSensor", title: "Who?", multiple:true
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
    subscribe(contact1, "contact", contactHandler)
    subscribe(presence1, "presence", presenceHandler)
}

def accelerationHandler(evt) {
	log.debug "accel event $evt"
}

def contactHandler(evt) {
	log.debug "contact event $evt"
}

def presenceHandler(evt) {
	log.debug "presence event $evt.value"
    
    if (evt.value == "not present") {
    	if (contact1.currentState("contact").value == "open") {
        	log.debug "User has left with the door open"
            sendPush("Whoa! The garage door is still open!")
        }
        else {
            log.debug "User has left with the door closed"
        }
    }
    else {
    	log.debug "User has arrived"
    }
}
