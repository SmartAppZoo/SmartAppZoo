/**
 *  Its too humid!
 *
 *  Copyright 2016 Jason Bestor
 *  Based on Its too cold code by SmartThings
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
    name: "Humidity Switch",
    namespace: "Jbestor",
    author: "Jason Bestor",
    description: "Turn on a switch when humidity rises above the threshold and off when it falls below.",
    category: "Convenience",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather9-icn?displaySize=2x"
)


preferences {
	section("Monitor the humidity of:") {
		input "humiditySensor1", "capability.relativeHumidityMeasurement"
	}
	section("Too Humid:") {
		input "humidity1", "number", title: "Percentage ?"
	}
	section("Control this switch:") {
		input "switch1", "capability.switch", required: true
	}
}

def installed() {
	subscribe(humiditySensor1, "humidity", humidityHandler)
}

def updated() {
	unsubscribe()
	subscribe(humiditySensor1, "humidity", humidityHandler)
}

def humidityHandler(evt) {
	log.trace "humidity: ${evt.value}"
    log.trace "set point: ${humidity1}"

	def currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	def tooHumid = humidity1
	def mySwitch = settings.switch1
    def switchState = switch1.currentSwitch

	if (currentHumidity >= tooHumid) {
		if (switchState == "on") {
        	log.debug "Current Humidity is ${currentHumidity}, ${mySwitch} is already on"
        	//sendNotificationEvent("Current Humidity is ${currentHumidity}, ${mySwitch} is already on")
        } else {
        	log.debug "Current Humidity is ${currentHumidity}, Turning on ${mySwitch}"
        	sendNotificationEvent("Current Humidity is ${currentHumidity}, Turning on ${mySwitch}")
        }
        switch1?.on()
	}

    if (currentHumidity < tooHumid) {
		if (switchState == "off") {
        	log.debug "Current Humidity is ${currentHumidity}, ${mySwitch} is already off"
        	//sendNotificationEvent("Current Humidity is ${currentHumidity}, ${mySwitch} is already off")
        } else {
        	log.debug "Current Humidity is ${currentHumidity}, Turning off ${mySwitch}"
        	sendNotificationEvent("Current Humidity is ${currentHumidity}, Turning off ${mySwitch}")
        }
        switch1?.off()
	}
}
