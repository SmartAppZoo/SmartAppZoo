/**
 *  Getting Stinky
 *
 *  Copyright 2018 Carl Dunkelberger
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
    name: "Getting Stinky",
    namespace: "BearCave",
    author: "Carl Dunkelberger",
    description: "Reports when the kitty&#39;s litterbox is getting stinky.",
    category: "Pets",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Sensor to indicate cleaning...") {
		input "cleanSensor", "capability.contactSensor", title: "Where?"
	}
	section("Report stink level at...") {
		input "time1", "time", title: "When?"
	}
	section("Send stink level to...") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone number?"
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Something"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    logLastCleaning()
}

def logLastCleaning()
{
    def lastCleaning = cleanSensor.getLastActivity()
    log.trace "Last activity: $lastCleaning"
}

// TODO: implement event handlers