/**
 *  IAQ_vent based on CO2_vent from Brian Steere
 *
 *  Copyright 2018 Philippe PORTES
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
    name: "IAQ_vent",
    namespace: "philippeportesppo",
    author: "Philippe PORTES",
    description: "IAQ_vent update",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences { 
	section("IAQ Sensor") {
    input "Air_Mentor_Pro_2", "capability.carbonDioxideMeasurement", title: "IAQ Sensor", required: true
	input "level", "enum", title: "CO2 Level for vents action", options: ["good","moderate","unhealthy sensitive persons", "unhealthy", "very unhealthy"], required: true, multiple: true
}
	section("Ventilation Fan") {
		input "switches", "capability.switch", title: "Switches", required: no, multiple: true
        input "thermostat","capability.thermostat", title: "thermostat", required: no, multiple: true
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
   
    subscribe(Air_Mentor_Pro_2, "IAQ", handleLevel)
}
def handleLevel(evt) {
    if (evt.name=="IAQ" && level.contains(evt.value)) {
        log.debug "Turning on"
        switches.each { it.on(); }
        thermostat.each {it.setThermostatFanMode("on");}
    } else {
        log.debug "Turning off"
        switches.each { it.off(); }
        thermostat.each {it.setThermostatFanMode("auto");}

    }
}