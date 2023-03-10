/**
 *  Car tracker
 *
 *  Copyright 2017 Praveen Potturu
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
    name: "Vehicle tracker",
    namespace: "nvmkpk",
    author: "Praveen Potturu",
    description: "Tracks arrival and departure of vehicles",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Allow external service to control these things...") {
        input "presenceSensors", "capability.presenceSensor", multiple: true, required: true
    }
}
    
mappings {
    path("/sensors/:sensorId/:command") {
        action: [
            PUT: "updateSensor"
        ]
    }
    path("/location") {
        action: [
            GET: "getLocationDetails"
        ]
    }
}

def getLocationDetails() {
    return [id:location?.id,name:location?.name,latitude:location?.latitude,longitude:location?.longitude]
}

def updateSensor() {
    log.debug "location.id: ${location.id}"
    log.debug "Name: ${location.name}"
    log.debug "Latitude: ${location.latitude}"
    log.debug "Longitude: ${location.longitude}"

    def sensorId = params.sensorId
    log.debug "sensorId: $sensorId"

    def command = params.command;
    log.debug "command: $command"
    
    def sensor;

    presenceSensors.each {
        log.debug "It is ${it.name}"
        if (sensorId == it.name) {
            sensor = it
        }
    }

    switch(command) {
        case "arrived":
            log.debug "${sensor} has arrived"
            sensor.arrived()
            break

        case "departed":
            log.debug "${sensor} has departed"
            sensor.departed()
            break

        default:
            httpError(400, "$command is not a valid command for a presence sensor")
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
