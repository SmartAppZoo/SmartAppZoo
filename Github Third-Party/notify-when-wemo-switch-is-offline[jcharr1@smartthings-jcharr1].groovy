/**
 *  Notify when WeMo Switch is Offline
 *
 *  Copyright 2016 Jason Charrier
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
    name: "Notify when WeMo Switch is Offline",
    namespace: "jcharr1",
    author: "Jason Charrier",
    description: "Notify when WeMo Switch is Offline",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Switch") {
		// TODO: put inputs here
        input "theSwitch", "capability.switch", required: true
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
    log.debug "Subscribe to switch"
    subscribe(theSwitch, "switch.offline", offlineHandler)
}

// TODO: implement event handlers
def offlineHandler(evt){
	log.debug "Switch offline: $evt.value"
    //sendNotification("${theSwitch.displayName} is now ${evt.value}. Might want to check the breaker.", [method: "both", phone: "3187152772"])
	sendPush("${theSwitch.displayName} is now ${evt.value}. Might want to check the breaker.")
}