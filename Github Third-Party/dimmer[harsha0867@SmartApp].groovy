/**
 *  Dimmer
 *
 *  Copyright 2017 harshadeep veluguleti
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
    name: "Dimmer",
    namespace: "Harsh0567",
    author: "harshadeep veluguleti",
    description: "Dimmer",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section ("Allow external service to control these things...") {
    input "DSwitch", "capability.switchLevel", multiple: true, required: true
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
// mappings for Get & Post Urls is done below.
mappings {
  path("/switches") {
    action: [
      GET: "switchesList"
    ]
  }
  path("/setlevel/:command") {
    action: [
      POST: "updateSwitches"
    ]
  }
}
// Below method get the switch levels which are set
def switchesList() {
    def resp = []
    DSwitch.each {
      resp << [ name: it.displayName, value: it.currentValue("level")]
    }
    return resp
}
//level is set in the switches in the below method.
def updateSwitches() {
    def cmd=params.command;
     DSwitch.each {
      it.setLevel(cmd)
     } 
    }
   


// TODO: implement event handlers