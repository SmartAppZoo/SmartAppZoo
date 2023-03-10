/**
 *  SmartThingsAppBackEnd
 *
 *  Copyright 2016 Pete Lewis
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
    name: "Smart Things App Back-End",
    namespace: "PJayB",
    author: "Pete Lewis",
    description: "SmartThings Apps Back-End",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Allow unofficial SmartThings apps access to these Actuators:") {
    	input "actuators", "capability.actuator", multiple: true, required: false
    }
    section("Allow unofficial SmartThings apps access to these Sensors:") {
    	input "sensors", "capability.sensor", multiple: true, required: false
    }
}

mappings {
	path("/sensors") {
    	action: [
        	GET: "listSensors"
        ]
    }
	path("/actuators") {
    	action: [
        	GET: "listActuators"
        ]
    }
    path("/actuate/:id/:command") {
    	action: [
        	PUT: "updateActuator"
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
    log.debug "Installed with settings: ${settings}"
}

// returns a list of JSON key value pairs representing the current things and their capabilities
private listThings(things) {
	def response = []
    things.each {thing ->
    	def capabilities = []
        thing.capabilities.each {capability ->
        	//def attributes = []
           	//capability.attributes.each {attribute -> 
            //	attributes << [name: attribute.name, dataType: attribute.dataType, values: attribute.values]
            //}
            //def commands = []
            //capability.commands.each {command ->
           	//	def arguments = []
            //    command.arguments.each { arg ->
            //    	arguments << arg
            //    }
            //	commands << [name: command.name, arguments: arguments]
            //}
        	capabilities << capability.name //, attributes: attributes, commands: commands]
        }
        def supportedAttributes = []
        thing.supportedAttributes.each {attribute ->
           	supportedAttributes << [name: attribute.name, dataType: attribute.dataType, values: attribute.values]
        }
        def supportedCommands = []
        thing.supportedCommands.each {command ->
            def arguments = []
            command.arguments.each { arg ->
                arguments << arg
            }
            supportedCommands << [name: command.name, arguments: arguments]
        }
    	response << [
        	id: thing.id,
        	name: thing.name,
            displayName: thing.displayName, 
            capabilities: capabilities, 
            supportedAttributes: supportedAttributes, 
            supportedCommands: supportedCommands]
    }
    return response
}


// returns a list of JSON key value pairs representing the current sensors
// e.g. [[name: "Kitchen lamp", value: "off"], [name: "Downstairs hallway", value: "on"]]
def listSensors() {
	log.debug "Query for sensors received..."

	return listThings(sensors)
}

def listActuators() {
	log.debug "Query for actuators received..."

	return listThings(actuators)
}


def updateActuator() {
	log.debug "Put command received"

	// Get the ":id" and ":command" parameters
    def id = params.id
	def command = params.command

	log.debug "... Command for ${id}: ${command}"
    
    // Get the device
    def target = null
    actuators.each {actuator ->
    	if (actuator.id == id) {
        	target = actuator
        }
    }
    
    if (target == null) {
    	httpError(400, "$id is not a recognized device")
        return
    }
    
    if (!target.hasCommand(command)) {
    	httpError(400, "${target.displayName} does not support '${command}'")
        return
    }

	// TODO: pass function parameters!
    def func = null
    target.supportedCommands.each {cmd ->
    	if (cmd.name == command) {
        	func = cmd
        }
    }
    
    if (func == null) {
    	httpError(400, "${target.displayName} does not support '${command}'")
        return
    }   	
    
    log.debug "Executing '${func.name}' on ${target.displayName}"
    //def evt = [name: command]
    //sendEvent(target, evt)
    target.("${command}")()
}