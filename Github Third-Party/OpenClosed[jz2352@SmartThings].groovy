/**
 *  On when door is opened, while second door is closed
 *
 *  Copyright 2015 Jason Ziemba
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
    name: "On When Door is Opened, w/2nd Door Closed",
    namespace: "the2352",
    author: "Jason Ziemba",
    description: "Turns light on when door is opened, while a second door remains closed.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Info") {
    	paragraph "Author:	Jason Ziemba"
        paragraph "Date:	2.17.2015"
    }   
    section("When this door opens...") {
		input "contact1", "capability.contactSensor", title: "Which Door?", required: true
	}
    section("And this door is closed...") {
    	input "contact2", "capability.contactSensor", title: "Which Door?", required: true
    }    
	section("Turn on this light...") {
		input "switch1", "capability.switch", title: "Which Switch?", required: true
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
    subscribe(contact1, "contact", "contactOpenHandler")
    subscribe(contact2, "contact", "contactClosedHandler")
    subscribe(switch1, "switch", "switchHandler")
}

def contactOpenHandler(evt) {
	log.debug ("contact1 = $evt.value")
    	checkOff()
	} 

def contactClosedHandler(evt) {
	log.debug ("contact2 = $evt.value")
    }
   
def switchHandler(evt) {
	log.debug ("switch1 = $evt.value")
    }

def checkOff() {
    def contact1Open = contact1.currentValue("contact") == "open"
    def contact2Closed = contact2.currentValue("contact") == "closed"
    def switchOff = switch1.currentValue("switch") == "off"
    log.debug("light on : $contact1Open : $contact2Closed : $switchOff :")
    if (contact1Open && contact2Closed && switchOff) {
        switch1.on()
    }
}
