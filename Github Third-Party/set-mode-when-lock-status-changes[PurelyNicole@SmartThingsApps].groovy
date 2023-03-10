/**
 *  Set Mode When Lock Status Changes
 *
 *  Copyright 2017 Nicole
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
    name: "Set Mode When Lock Status Changes",
    namespace: "PurelyNicole",
    author: "Nicole",
    description: "Set the mode for your home when the lock status is changed. For example, set the mode to home when the door is unlocked.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("When any of these locks..."){
		input "doorLock", "capability.lock", title: "Lock?", required: true, multiple: true
	}
    
    section("Changes to this status..."){
    	input "lockStatus", "enum", title: "Status?", options: ["Locked", "Unlocked"], required: true
    }

	section("Change to this mode.") {
		input "newMode", "mode", title: "Mode?", required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribe(doorLock, "lock", onLockChange)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    subscribe(doorLock, "lock", onLockChange)
}

def onLockChange(evt) {
	log.debug("Lock status has been changed."){
    	log.info("Set mode to $newMode.")
        setLocationMode(newMode)
    }
}
