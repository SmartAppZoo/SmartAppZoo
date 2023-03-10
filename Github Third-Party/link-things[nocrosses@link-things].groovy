/**
 *  Link Things
 *
 *  Copyright 2017 Matt Bodily
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
    name: "Link Things",
    namespace: "nocrosses",
    author: "nocrosses",
    description: "Link devices together, when one is activated the rest are activated. Deactivated, same thing.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When one of these devices changes state so will the others.") {
		input "master", "capability.switch", multiple: true, title: "Things?"
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
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	log.debug "switch changed"
    log.info evt.deviceId;
    
     log.info "Master ids: $master*.id";
    
    def changeDevices = master.findAll { it ->
        it.id != evt.deviceId && it.currentSwitch != evt.value};
    
    log.info "Change devices ids: $changeDevices*.id";
    
    if( evt.value == "off")
    {
        log.debug "switch off"
        changeDevices*.off();
    } else if (evt.value =="on")
    {
        log.debug "switch on"
        changeDevices*.on();
    }
}
