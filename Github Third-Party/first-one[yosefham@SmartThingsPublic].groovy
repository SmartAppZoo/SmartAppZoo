/**
 *  first one
 *
 *  Copyright 2016 yusef
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
    name: "first one",
    namespace: "IOT",
    author: "yusef",
    description: "control de bombilla por presencia",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input "elsensor","capability.motionSensor",title: "hay movimiento??", required: true, multiple: true
        input "elswitch", "capability.switch",title: "cambiamos??", required: true, multiple: true
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
    subscribe(elsensor[0],"motion",movimiento0)
    subscribe(elsensor[1],"motion",movimiento1)
    subscribe(elsensor[2],"motion",movimiento2)
}

// TODO: implement event handlers
def movimiento0(evt){
  if("active" == evt.value) {
    elswitch[0].on()
  } else if("inactive" == evt.value) {
    elswitch[0].off()
  }
}
def movimiento1(evt){
  if("active" == evt.value) {
    elswitch[1].on()
  } else if("inactive" == evt.value) {
    elswitch[1].off()
  }
}
def movimiento2(evt){
  if("active" == evt.value) {
    elswitch[2].on()
  } else if("inactive" == evt.value) {
    elswitch[2].off()
  }
}
