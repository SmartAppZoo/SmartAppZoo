/**
 *  My First App
 *
 *  Copyright 2018 John Nguyen
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
    name: "Devices Gateway",
    namespace: "jnguyenc",
    author: "John Nguyen",
    description: "Provides a connection for webapp to get status and control some physical devices",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section ("Allow external service to control these things...") {
    	input "switches", "capability.switch", multiple: true, required: true
  	}
    section("Control these motion sensors...") {
        input "motions", "capability.motionSensor", multiple:true, required: true
    }
     section("Control these temperature sensors...") {
        input "temps", "capability.thermostat", multiple:true, required: true
    }   
}

mappings {
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/motions") {
    action: [
      GET: "listMotions"
    ]
  }
  path("/temps") {
    action: [
      GET: "listTemps"
    ]
  }
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
    // use the built-in request object to get the command parameter
    def command = params.command
    log.debug "command params ${command}"

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

def listMotions(){
	def resp = []
    motions.each {
      resp << [name: it.displayName, value: it.currentValue("motion")]
    }
    return resp
}

def listTemps(){
	def resp = []
    temps.each {
      //resp << [name: it.displayName, temp: it.currentValue("temperature")]
      //resp << [name: it.displayName, humidity: it.currentValue("humidity")]
      resp << [name: it.displayName, values: [temp: it.currentValue("temperature"), humidity: it.currentValue("humidity")] ]
    }
    return resp
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