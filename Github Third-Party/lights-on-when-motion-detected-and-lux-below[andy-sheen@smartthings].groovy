/**
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
 *  Light Follows Me
 *
 *  Author: SmartThings
 */

definition(
    name: "Lights on when motion detected and lux below",
    namespace: "Thestfield",
    author: "Andy Sheen",
    description: "Turn your lights on when motion is detected and light is below a threshold and then off again once the motion stops for a set period of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    section("Turn on when there's movement from..."){
        input "motion1", "capability.motionSensor", title: "Where?"
    }
    section("And off when there's been no reported movement for..."){
        input "minutes1", "number", title: "Minutes?"
    }
    section("And light level is below..."){
        input "lightlevel1", "number", title: "Lux"
    }
    section("From lightmeter..."){
        input "lightmeter1", "capability.illuminanceMeasurement", title: "Light meter?"
    }
    section("Turn on the following light(s)..."){
        input "onswitches", "capability.switch", multiple: true
    }
    section("Turn off the following light(s)..."){
        input "offswitches", "capability.switch", multiple: true
    }
}

def lightsOff() {
    offswitches.off()
    log.debug "lights off..."
}

def installed() {
    log.debug "Installing handler..."
    subscribe(motion1, "motion", motionHandler)
    lightsOff()
}

def updated() {
    log.debug "Updated configuration. Unsubscribing and re-installing..."
    unsubscribe()
    installed()
}

def motionHandler(evt) {
    def currentLux = lightmeter1.currentValue("illuminance")

    def currSwitches = offswitches.currentSwitch
    def swThatAreOn = currSwitches.findAll { switchVal ->
        switchVal == "on" ? true : false
    }

    def swOn = swThatAreOn.size()

    if ((evt.value == "active") && (currentLux <= lightlevel1)) {
        log.debug "Motion detected: current lux level: $currentLux, threshold for switch on: $lightlevel1 -> lights on (and unschedule any switchoff)"
        onswitches.on()
        unschedule(lightsOff)
    } else if ((evt.value == "active") && (swOn > 0)) { // Light above threshold, and we have lights on....
        log.debug "Motion detected: $swOn lights on. Cancelling any light off event outstanding. Current lux: $currentLux"
        unschedule(lightsOff)
    } else if ((evt.value == "inactive") && (swOn > 0)) {
        if (minutes1 >= 1) {
            runIn(minutes1 * 60, lightsOff)
            log.debug "Motion inactive: scheduling switchoff of $swOn light(s) in $minutes1 mins."
        } else {
            lightsOff()
        }
    } else if ((evt.value == "inactive") && (swOn == 0)) {
        log.debug "Motion inactive: no lights on, so not scheduling switchoff."
    } else { // Active but > lightLevel1
        log.debug "Motion detected: doing nothing. lux: $currentLux > $lightlevel1, switches on: $swOn"
    }


}
