/**
 *  SNMP UPS Integration
 *
 *  Copyright 2015 Joel Tamkin
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
    name: "SNMP Integration",
    author: "Joel Tamkin",
    description: "Integration for SNMP devices via python/REST on external system",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Joel Tamkin", displayLink: ""])


preferences {
  section("SNMP Devices:") {
    input "devices", "capability.sensor", title: "Which SNMP Devices?", multiple: true, required: false
  }
}


mappings {
  path("/update") {
    action: [ PUT: "updateDeviceStatus" ]
  }
}

def updateDeviceStatus() {
    log.debug "Received update for device name: ${request.JSON.name}"
    devices.eachWithIndex { it, i -> if ( devices[i].deviceNetworkId == request.JSON.name ) devices[i].setPoints(request.JSON.points) }
    return
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    devices.each{ it -> subscribe(it) }
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initializing"
    // TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers