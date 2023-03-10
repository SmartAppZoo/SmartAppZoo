/**
 *  Piputer - Raspberry Pi PC Controler for SmartThings
 *
 *  Copyright 2016 Ryan Coodey
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
    name: "Piputer",
    namespace: "rcoodey",
    author: "Ryan Coodey",
    description: "Receives input to toggle PC state via Raspberry Pi on LAN",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
    section() { 
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false
	}
}

mappings { 
    path("/piputer/allPCStatusesEvent") { 
        action: [PUT: "updateAllPCStatuses"] 
    }
    path("/piputer/toggleSwitchEvent/:id") { 
        action: [GET: "toggleSwitch"] 
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
}

//Event handlers
void updatePCStatus(state, id) { 
   def pcdevice = switches.find { it.deviceNetworkId == "Piputer${id}" }
   if (pcdevice) {
       if(state.contains("A")) {
           pcdevice.statusEvent(state == "0A" ? "turningOn" : "turningOff")
       }
       else
           pcdevice.statusEvent(state == "1" ? "on" : "off")
       //log.debug id + " status event: " + state
   }
}
void updateAllPCStatuses() { 
   for(json in request.JSON) {
       def id = json.getKey()
       def state = json.getValue()
       updatePCStatus(state, id)
   }
   log.debug "All PC statuses event: " + request.JSON
}
void toggleSwitch() {
   def id = params.id
   toggleSwitch(id)
}
void toggleSwitch(id) { 
   def switchdevice = switches.find { it.deviceNetworkId == "Piputer${id}" } 
   if (switchdevice) {
       if(switchdevice.currentSwitch == "off")
           switchdevice.on()
       else
           switchdevice.off()
       log.debug "Switch ${id} state: " + switchdevice.currentSwitch
   }
}