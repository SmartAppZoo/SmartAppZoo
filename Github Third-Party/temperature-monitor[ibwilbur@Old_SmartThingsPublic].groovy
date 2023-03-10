/**
 *  Temperature Monitor
 *
 *  Copyright 2016 Will Shelton
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
    name: "Temperature Monitor",
    namespace: "ibwilbur",
    author: "Will Shelton",
    description: "Monitors temperatures and takes actions accordingly",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png")


preferences {   
	section("Choose a temperature sensor..."){
		input "sensor", "capability.temperatureMeasurement", title: "Temprature sensor", multiple: false, required: false
	}
	section("Select the fan to control..."){
		input "fan", "capability.switch", title: "Fan", multiple: false, required: false
	}
	section("Turn on/off at what temperature?"){
		input "setpoint", "number", title: "Temperature"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"    
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	//subscribe(sensor, "temperature", temperatureHandler, [filterEvents: false])
    schedule("0 0/1 * * * ?", checkTemperature)
}

def temperatureHandler(e) {
	checkTemperature()
}

def checkTemperature() {
	def currentTemp = sensor.currentValue("temperature")
    def currentState = fan.currentState("switch").value
    //log.trace "Temperature: $currentTemp, State: $currentState"
    
    if (currentTemp > setpoint && currentState == "off") {
    	fan.on()
        log.trace "Current temperature is $currentTemp, target temp is $setpoint.  Turning fan on."
    }
    else if (currentTemp < setpoint && currentState == "on") {
    	fan.off()
        log.trace "Current temperature is $currentTemp, target temp is $setpoint.  Turning fan off."
    }
}