/**
 *  Keep Me Warm
 *
 *  Copyright 2016 Jason Bestor
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
    name: "Keep Me Warm",
    namespace: "Jbestor",
    author: "Jason Bestor",
    description: "Turn on a heater at specified temperature and turn if off once another temp has been reached",
    category: "Convenience",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x")


preferences {
	section("Monitor the temperature...") {
		input "tempSensor", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "coldTemp", "number", title: "Temperature?"
	}
   	section("When the temperature climbs above...") {
		input "hotTemp", "number", title: "Temperature?"
	}
    section("Control a heater...") {
		input "heaterSwitch", "capability.switch"
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
	subscribe(tempSensor, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def currentTemp = evt.doubleValue
	def tooCold = coldTemp
    def tooHot = hotTemp
	def mySwitch = settings.heaterSwitch
    def switchState = heaterSwitch.currentSwitch
    log.debug "Switch ${switchState}"
    
	if (currentTemp <= tooCold) {
		if (switchState == "on") {
        	log.debug "Current Temp is ${currentTemp}, ${mySwitch} is already on"
        	//sendNotificationEvent("Current Temp is ${currentTemp}, ${mySwitch} is already on")
        } else {
        	log.debug "Current temp is ${currentTemp}, Turning on ${mySwitch}"
        	sendNotificationEvent("Current Temp is ${currentTemp}, Turning on ${mySwitch}")
        }
        heaterSwitch?.on()
	}

    if (currentTemp >= tooHot) {
		if (switchState == "off") {
        	log.debug "Current Temp is ${currentTemp}, ${mySwitch} is already off"
        	//sendNotificationEvent("Current Temp is ${currentTemp}, ${mySwitch} is already off")
        } else {
        	log.debug "Current Temp is ${currentTemp}, Turning off ${mySwitch}"
        	sendNotificationEvent("Current Temp is ${currentTemp}, Turning off ${mySwitch}")
        }
        heaterSwitch?.off()
	}
}