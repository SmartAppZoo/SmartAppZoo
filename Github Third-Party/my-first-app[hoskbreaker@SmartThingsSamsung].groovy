/**
 *  My first app
 *
 *  Copyright 2018 Jose Carlos Sanchez
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
    name: "My first app",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "This is the first app created with Samsung smartThings",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Selecciona el sensor de movimiento") {
		input "SensorMov","capability.motionSensor", title:"Sensor de movimiento", requires: true, multiple: false
	}
    section("Selecciona la luz a encender/apagar") {
		input "ElInterruptor","capability.switch", title:"Interruptor", requires: true, multiple: false
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
	subscribe(SensorMov,"motion",movHandler)
}

// TODO: implement event handlers
def movHandler(evt){
	if(evt.value == "active"){
        log.debug "movHandler called: $evt.value"
        ElInterruptor.on()
    }else if (evt.value =="inactive"){
    	log.debug "movHandler called: $evt.value"
    	ElInterruptor.off()
    }
}