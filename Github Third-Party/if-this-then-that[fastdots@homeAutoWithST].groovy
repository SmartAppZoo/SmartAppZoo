/**
 *  If this then that
 *
 *  Copyright 2020 AJEY TATAKE
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
    name: "If this then that",
    namespace: "tatake_labs",
    author: "AJEY TATAKE",
    description: "ST version of IFTTT",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("What:") {
        input "doorLock", "capability.lock", title: "Door Lock", required: true
	}

	section("When Locked Then Turn these On:") {
        input "Lockedonswitch1", "capability.switch", required: false
        input "Lockedonswitch2", "capability.switch", required: false
        input "Lockedonswitch3", "capability.switch", required: false
        input "Lockedonswitch4", "capability.switch", required: false
	}

	section("When Locked Then Turn these Off:") {
        input "Lockedoffswitch1", "capability.switch", required: false
        input "Lockedoffswitch2", "capability.switch", required: false
        input "Lockedoffswitch3", "capability.switch", required: false
        input "Lockedoffswitch4", "capability.switch", required: false
	}

	section("When Opened Then Turn these On:") {
        input "unlockedonswitch1", "capability.switch", required: false
        input "unlockedonswitch2", "capability.switch", required: false
        input "unlockedonswitch3", "capability.switch", required: false
        input "unlockedonswitch4", "capability.switch", required: false
	}

	section("When Opened Then Turn these Off:") {
        input "unlockedoffswitch1", "capability.switch", required: false
        input "unlockedoffswitch2", "capability.switch", required: false
        input "unlockedoffswitch3", "capability.switch", required: false
        input "unlockedoffswitch4", "capability.switch", required: false
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
    subscribe (doorLock, "lock.locked", lockedHandler)
    subscribe (doorLock, "lock.unlocked", unlockedHandler)
}

// event handlers
def lockedHandler (evt) {
	log.debug "Turned on handler with event: $evt"

	setSwitchOn (Lockedonswitch1)
	setSwitchOn (Lockedonswitch2)
	setSwitchOn (Lockedonswitch3)
	setSwitchOn (Lockedonswitch4)

	setSwitchOff (Lockedoffswitch1)
	setSwitchOff (Lockedoffswitch2)
	setSwitchOff (Lockedoffswitch3)
	setSwitchOff (Lockedoffswitch4)}


def unlockedHandler (evt) {
	log.debug "Turned off handler with event: $evt"

	setSwitchOn (unlockedonswitch1)
	setSwitchOn (unlockedonswitch2)
	setSwitchOn (unlockedonswitch3)
	setSwitchOn (unlockedonswitch4)

	setSwitchOff (unlockedoffswitch1)
	setSwitchOff (unlockedoffswitch2)
	setSwitchOff (unlockedoffswitch3)
	setSwitchOff (unlockedoffswitch4)
}





def setSwitchOn (sw)
{
	if (sw) {
    	sw.on()
    }
}


def setSwitchOff (sw)
{
	if (sw) {
    	sw.off()
    }
}