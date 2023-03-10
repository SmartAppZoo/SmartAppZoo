/**
 *  Multiple Dimmer Switch
 *
 *  Copyright 2020 Hussein Khalil
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
    name: "Multiple Dimmer Switch",
    namespace: "husseinmohkhalil",
    author: "Hussein Khalil",
    description: "Multiple Dimmer Switch ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	 section("Which Virtual Dimmer You are going to use"){
    input(name: "virtualDimmer", type: "capability.switchLevel", title: "Which switch?", required: true)
   }
   section("Which device(s) to control "){
    input(name: "targets", type: "capability.switchLevel", title: "Target dimmer switch(s)", multiple: true, required: true)
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
   subscribe(virtualDimmer, "switch.on", SwitchOnHandler)
   subscribe(virtualDimmer, "switch.off", SwitchOffHandler)
   subscribe(virtualDimmer, "level", SwitchLevelChangedHandler)
   
}


def SwitchOnHandler(evt) {
   	log.debug "Updated with settings: ${evt.value}"
 targets.on()
}

def SwitchOffHandler(evt) {
		log.debug "Updated with settings: ${evt.value}"
targets.off()
}

def SwitchLevelChangedHandler(evt) {
	log.debug "Updated with settings: ${evt.value}"

      targets.setLevel(evt.value)
    }

// TODO: implement event handlers