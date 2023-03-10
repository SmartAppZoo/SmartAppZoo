/**
 *  Grouped Lights Macro
 *
 *  Copyright 2017 Michael McGarry
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
    name: "Grouped Lights",
    namespace: "mcgarryplace-michael",
    author: "Michael McGarry",
    description: "Uses a Stateless Virtual Switch to turn on/off a group of lights",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Stateless Virtual Switch") {
        input "svsMaster", "capability.switch", required: true
    }
    
    section("Lights") {
        input "lights", "capability.switch", multiple: true
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
    subscribe(svsMaster, "switch.on", svsMasterOnHandler)
    subscribe(svsMaster, "switch.off", svsMasterOffHandler)
}

def svsMasterOnHandler(evt) {
    log.debug "svsMasterOnHandler called: $evt"
    lights?.on()
}

def svsMasterOffHandler(evt) {
    log.debug "svsMasterOffHandler called: $evt"
    lights?.off()
}
