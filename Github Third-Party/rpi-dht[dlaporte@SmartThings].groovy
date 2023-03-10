/**
 *  RPI Temperature/Humidity Sensor
 *
 *  Copyright 2016 David LaPorte
 *  based on code originally written by Paul Cifarelli
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
    name: "RPi DHT11/22 Temperature/Humidity Sensor",
    namespace: "dlaporte",
    author: "David LaPorte",
    description: "REST endpoint for RPi temperature/humidity script",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "davidlaporte.org", displayLink: ""])
 
preferences {
    section("Allow Endpoint to Control This Thing") {
        input "tdevice", "capability.temperatureMeasurement", title: "Which Simulated Temperature Sensor?", multiple: false
    }
}
  
mappings {
    path("/update/temperature/:temperature/:units") {
        action: [
            PUT: "updateTemperature"
        ]
    }
    path("/update/humidity/:humidity") {
        action: [
            PUT: "updateHumidity"
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

void updateTemperature() {
    updateT(tdevice)
}

void updateHumidity() {
    updateH(tdevice)
}

private void updateT(device) {
     log.debug "update temperature, request: params: ${params['temperature']} ${params['units']} ${device.name}"
     def t = 0
     
     if (location.temperatureScale == params['units']) {
        log.debug "yes, the temperatureScale is the same (${params['units']})"
        t = Double.parseDouble(params['temperature'])
     } else if (params['units'] == "F") {
        // so location is set for C
        t = 5 * ( Double.parseDouble(params['temperature']) - 32 ) / 9
     } else if (params['units'] == "C") {
        // so location is set for F
        t = 9 * Double.parseDouble(params['temperature']) / 5 + 32
     }
     def x = Math.round(t * 100) / 100
     tdevice.setTemperature(t)
}

private void updateH(device) {
     def h = Double.parseDouble(params['humidity'])
     //def h = Double.parseDouble(params['humidity'])
     log.debug "update humidity, request: params: ${params['humidity']} ${device.name}"
     def x = Math.round(h * 100) / 100
     tdevice.setHumidity(h)
}
