/**
 *  Restored Power Outage
 *
 *  Copyright 2019 Paul Ng
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
    name: "Restored Power Outage",
    namespace: "ng-family",
    author: "Paul Ng",
    description: "Selecting a 'canary' bulb, perform actions when bulb is on",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
	section("Canary Bulb") {
        input "canarybulb", "capability.switch", title: "Which bulb?"
    }
    section("Devices you want to turn off if the Canary lights up") {    
        input "zigbeebulbs","capability.switch", multiple: true
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
	subscribe(canarybulb,"switch.on", checkCanary)
    pollCanary()
    runEvery5Minutes(checkCanary)
}

def checkCanary(evt) {
    log.debug "Checking Canary Bulb"
    pollCanary()
    log.debug "Canary Bulb is ${canarybulb.currentSwitch}"
    if ("on" == canarybulb.currentSwitch) {
        log.debug "Turning off Zig Bee bulbs"
        zigbeebulbs?.each {
            log.debug "Turning $it.label off"
            it.off()
        }
        log.debug "Turning $canarybulb.label off"
        canarybulb.off()
    }
}

private pollCanary() {
    def hasPoll = canarybulb.hasCommand("poll")
    if (hasPoll) {
        canarybulb.poll()
    }
    else
    {
        canarybulb.refresh()
    }
}

