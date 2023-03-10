/**
 *  SmartThings Web Server
 *
 *  Copyright 2015 Rob McDonald
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
    name: "SmartThings Web Server",
    namespace: "robmcd527",
    author: "Rob McDonald",
    description: "Basic web server to allow external systems to control smartthings devices",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section ("Allow external service to control these things...") {
    	input "switches", "capability.switch", multiple: true, required: true
	}
}

mappings {
	path("/switches") {
		action: [
			GET: "listSwitches"
		]
  	}
  
  	path("/switches/:id/:command") {
    	action: [
      		PUT: "updateSwitches"
    	]
  	}
}


// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {

	def status
    def contentType
    def data
    def headers = [:]

    def resp = []
    switches.each {
        resp << [name: it.displayName, id: it.id, value: it.currentValue("switch")]
    }
    return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command
    def devices = params.id.tokenize(',')
    def success = false
    
    if (! command) {
    	httpError(501, "Unable to find command!");
    }
    
    if (! devices || devices.size == 0) {
    	httpError(501, "Unable to find any devices!");
    }
    
    log.debug "command: $command and deviceIds: " + devices.toListString()

    // For each switch, check to see if its one we want to toggle
    // if so, do it!
    switches.each {
        def did = it.id
        if (devices.find({it == did}) != null && it.hasCommand(command)) {
        	log.debug "Turning $did $command"
            it."$command"()
            success = true
        } 
    }
    
    if (! success) {
    	httpError(404, "Unable to find any devices with requested Ids!")
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	// unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

