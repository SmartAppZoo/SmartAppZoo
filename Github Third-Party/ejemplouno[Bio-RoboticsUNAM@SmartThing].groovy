/**
 *  EjemploUno
 *
 *  Copyright 2016 FI UNAM
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
    name: "EjemploUno",
    namespace: "EjemploconArduino",
    author: "FI UNAM",
    description: "Implementacion de codigo groovy y arduino",
    category: "SmartThings Labs",
    iconUrl: "https://image.freepik.com/iconos-gratis/robot-con-brazos-y-piernas-flexibles_318-64397.png",
    iconX2Url: "https://image.freepik.com/iconos-gratis/robot-con-brazos-y-piernas-flexibles_318-64397.png",
    iconX3Url: "https://image.freepik.com/iconos-gratis/robot-con-brazos-y-piernas-flexibles_318-64397.png")


preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
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
    subscribe(themotion, "motion.active", motionDetectedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
}

// TODO: implement event handlers