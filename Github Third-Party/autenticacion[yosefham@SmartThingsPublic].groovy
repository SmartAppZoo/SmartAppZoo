/**
 *  autenticacion
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
    name: "autenticacion", 
    namespace: "IOT",
    author: "yusef",
    description: "no se sabe todavia",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section() {
		// TODO: put inputs here
        input "elswitch", "capability.switch", multiple: true
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

// TODO: implement event handlers
mappings{
	path("/switch"){
    	action: [
        	GET: "listadeSwitch"
        ]
    }
	path("/switch/:command"){
    	action: [
        	PUT: "actualizarSwitch"
        ]
    }

}
def listadeSwitch(){
	def map = [] 
    elswitch.each {
    	resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return each
}
def actualizarSwitch (){
	def cmd = params.command
    switch(cmd){
    	case "on":
        	elswitch.on()
            break
        case "off":
        	elswitch.off()
            break
        default: 
        	httpError(501, "$command no es un comando valido")
    }
}