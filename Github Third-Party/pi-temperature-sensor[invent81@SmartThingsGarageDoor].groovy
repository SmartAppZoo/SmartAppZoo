/**
 *  Pi Temperature Sensor Endpoints
 *
 *  Copyright 2016 Paul Cifarelli
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
    name: "Pi Temperature Sensor",
    namespace: "pcifarelli",
    author: "Paul Cifarelli",
    description: "REST endpoint for Pi to update a Simulated Temperature Sensor",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Cifarelli.net", displayLink: ""])
 
preferences {
    section("Allow Endpoint to Control This Thing") {
        input "tdevice", "capability.temperatureMeasurement", title: "Which Simulated Temperature Sensor?", multiple: false
    }
}
  
mappings {
    path("/update/:temp/:units") {
        action: [
            PUT: "updateTemp"
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
 
// implement event handlers
void updateTemp() {
    update(tdevice)
}
 
private void update(device) {
     log.debug "update, request: params: ${params['temp']} ${params['units']} ${device.name}"
     def t = 0
     
     if (location.temperatureScale == params['units']) {
        log.debug "yes, the temperatureScale is the same (${params['units']})"
        t = Double.parseDouble(params['temp'])
     } else if (params['units'] == "F") {
        // so location is set for C
        t = 5 * ( Double.parseDouble(params['temp']) - 32 ) / 9
     } else if (params['units'] == "C") {
        // so location is set for F
        t = 9 * Double.parseDouble(params['temp']) / 5 + 32
     }
     def x = Math.round(t * 100) / 100
     tdevice.setTemperature(x)
}