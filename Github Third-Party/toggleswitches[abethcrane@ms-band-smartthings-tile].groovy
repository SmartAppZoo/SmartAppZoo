/**
 *  Toggle Switches
 *
 *  Copyright 2015 Beth Crane
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
    name: "Toggle Switches",
    namespace: "abethcrane",
    author: "Beth Crane",
    description: "Users select which switches they want to provide access to, and can toggle each one on and off.",
    category: "My Apps",
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
  path("/switches/:command") {
    action: [
      GET: "updateSwitches"
    ]
  }
  path("/set/:command") {
    action: [
      GET: "setSwitches"
    ]
  }
  path("/toggle/:name") {
    action: [
      GET: "toggleSwitch"
    ]
  }
  path("/set/:name/:command") {
    action: [
      GET: "setSwitch"
    ]
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

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
    def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}

void updateSwitches() {
	log.trace "updateSwitches!"

    // use the built-in request object to get the command parameter
    def command = params.command

    if (command) {

        // check that the switch supports the specified command
        // If not, return an error using httpError, providing a HTTP status code.
        switches.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }

        // all switches have the comand
        // execute the command on all switches
        // (note we can do this on the array - the command will be invoked on every element
        switches."$command"()
    }
}

void toggleSwitch() {
    // use the built-in request object to get the command parameter
    def name = params.name
    
    if (!name) {
    	httpError(501, "A name parameter is required")
    }

	def selectedSwitch = switches.find {it.displayName == name}
    if (!selectedSwitch) {
        httpError(501, "A switch with name '$name' could not be found in the list of switches - $switches")
    }
    
    if ("on" == selectedSwitch.currentSwitch) {
    	selectedSwitch.off()
    } else {
    	selectedSwitch.on()
    }
}

void setSwitch() {
    // use the built-in request object to get the command parameter
    def name = params.name
    def command = params.command

    if (!name) {
    	httpError(501, "A name parameter is required")
    }

	def selectedSwitch = switches.find {it.displayName == name}
    if (!selectedSwitch) {
        httpError(501, "A switch with displayName $name could not be found")
    }

    if (command) {       
        if (!selectedSwitch.hasCommand(command)) {
            httpError(501, "$command is not a valid command for the switch specified")
        }
        selectedSwitch."$command"()
    }
}
