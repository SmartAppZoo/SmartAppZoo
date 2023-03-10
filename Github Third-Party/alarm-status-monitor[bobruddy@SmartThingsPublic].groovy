/**
 *  Alarm Status Monitor
 *
 *  Copyright 2017 Robert Ruddy
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
    name: "Alarm Status Monitor",
    namespace: "ruddy.Alarm Monitor",
    author: "Robert Ruddy",
    description: "Update the color of a bulb based on alarm status",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Use the following lights...") {
		input "switches", "capability.switch", multiple: true, required: true
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
	subscribe(location, "alarmSystemStatus", alarmHandler)
}

// TODO: implement event handlers
def alarmHandler(evt) {
  log.debug "Alarm Handler value: ${evt.value}"
  log.debug "alarm state: ${location.currentState("alarmSystemStatus")?.value}"
  
  if( location.currentState("alarmSystemStatus")?.value == "off" ) {
  	for (s in switches) {
  		s.off()
	}
  } else {
	for (s in switches) {
  		s.on()
	}
  }
}