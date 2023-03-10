/**
 *  Button Manager
 *
 *  Copyright 2019 DAVE MCWATTERS
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
    name: "Button Manager",
    namespace: "daveyiv",
    author: "DAVE MCWATTERS",
    description: "SmartApp to interpret button presses from arduino devices",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


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
      PUT: "updateSwitches"
    ]
  }
  path("/switches/:switchname/:command") {
  	action: [
      PUT: "updateSwitch"
    ]
  }
  path("/switches/:switchname/:command/:value") {
    action: [
      PUT: "updateSwitch"
    ]
  }
}

def listSwitches() {
	def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}

def switchCommand(switchname, command) {
   switches.each {
      if(it.displayName == switchname) {
         if(params.value == null)
         	it."$command"()
         else
            it."$command"(params.value)
      }
   }
}

def updateSwitch() {
	def switchname = params.switchname
    def command = params.command
    def value = params.value

    switch(command) {
    	case "on":
        case "off":
        case "setLevel":
            switchCommand(switchname, command)
            break
    	default:
        httpError(400, "$command is not a valid command for all switches specified")
    }
}

def updateSwitches() {
	// use the built-in request object to get the command parameter
    def command = params.command

    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "on":
            switches.on()
            break
        case "off":
            switches.off()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.error "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers