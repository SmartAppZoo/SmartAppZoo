/**
 *  Control a Switch with an API call
 *
 *  Copyright 2015 SmartThings
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
    name: "Control Presence with API calls",
    namespace: "jrhbcn",
    author: "jrhbcn",
    description: "Control Presence with API calls, from https://github.com/SmartThingsCommunity/Code/blob/master/smartapps/hackathon-demo/restful-switch.groovy",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
    section("which presence sensors?") {
        input "theSensors", "capability.presenceSensor", multiple: true
    }
}

mappings {  
  // GET requests to endpoint /presenceSensors/<id> go to getSensor
  // PUT requests to endpoint /presenceSensors/<id> go to updateSensor
  path("/presenceSensors/:id") {
    action: [
        GET: "getSensor",
        PUT: "updateSensor"
    ]
  }
}

def getSensor() {
    log.debug "getSensor: look for sensor with id ${params.id}"
    theSensors.each {
        log.debug "getSensor: ${it.id}"
    }
    def theSensor = theSensors.find{it.id == params.id}
    [theSensor.displayName, theSensor.currentPresence]
}

// execute the command specified in the request
// return a 400 error if a non-supported command 
// is specified (only on, off, or toggle supported)
// assumes request body with JSON in format {"command" : "<value>"}
def updateSensor() {
    log.debug "updateSensor: look for sensor with id ${params.id}"
    def theSensor = theSensors.find{it.id == params.id}
    doCommand(theSensor, request.JSON.command)
}

def doCommand(theSensor, command) {
    if (command == "toggle") {
        if (theSensor.presence == "present") {
            log.debug "will try and turn sensor ${theSensor.displayName} to not present"
            theSensor.departed()
        } else {
            log.debug "will try and turn sensor ${theSensor.displayName} to present"
            theSensor.arrived()
        }
    } else if (command == "arrived" || command == "departed") {
        theSensor."$command"()
    } else {
        httpError(400, "Unsupported command - only 'toggle', 'arrived', and 'departed' supported")
    }
}

// called when SmartApp is installed
def installed() {
    log.debug "Installed with settings: ${settings}"
}

// called when any preferences are changed in this SmartApp. 
def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
}