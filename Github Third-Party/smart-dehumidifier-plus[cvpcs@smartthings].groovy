/**
 *  Smart Humidifier Plus
 *
 *  Copyright 2019 Austen Dicken
 *. Based on Smart Humidifier by Sheikh Dawood
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
    name: "Smart Dehumidifier Plus",
    namespace: "cvpcs",
    author: "Austen Dicken",
    description: "Turn on/off a dehumidifier based on relative humidity from a sensor.",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather12-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather12-icn?displaySize=2x",
    pausable: true
)

preferences {
	section("Devices:") {
		input("humiditySensor", "capability.relativeHumidityMeasurement", title: "Humidity Sensor", description: "Select a humidity sensor to monitor")
        input("dehumidifierSwitch", "capability.switch", title: "Dehumidifier Switch", description: "Select a switch to control based on the detected humidity")
	}
	section("Thresholds:") {
		input("humidityMax", "number", title: "Humidity Maximum", description: "Humidity % at which the switch will be turned on")
		input("humidityMin", "number", title: "Humidity Minimum", description: "Humidity % at which the switch will be turned off")
	}
    section("Limits:") {
        input("switchActiveMaximum", "number", title: "Humidifier Active Maximum", description: "Maximum number of minutes the switch should remain on once it has been activated", required: false)
        input("sendPushMessage", "bool", title: "Send a push notification if maximum is reached?", required:false)
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
	state.lastStatus = "unknown"
	subscribe(humiditySensor, "humidity", humidityHandler)
}

def humidityHandler(evt) {
	log.debug "humidity: ${evt.value}"
    log.debug "set max point: ${humidityMax}"
    log.debug "set min point: ${humidityMin}"

    // store temporarily to address potential input problems
    def humidityHigh = humidityMax
   	def humidityLow = humidityMin
    
    if (humidityHigh <= humidityLow) {
    	log.warning "humidity high (max) <= humidity low (min), so forcing low = high"
        humidityLow = humidityHigh
    }

	def currentHumidity = Double.parseDouble(evt.value.replace("%", ""))
	if (currentHumidity >= humidityHigh) {
        if (state.lastStatus != "on") {
            log.debug "Humidity Rose Above ${humidityHigh}: activating ${dehumidifierSwitch.label}"
            dehumidifierSwitch?.on()
            runIn(60 * switchActiveMaximum, activeMaximumReachedHandler)
            state.lastStatus = "on"
        }
	} else if (currentHumidity <= humidityLow) {
        if (state.lastStatus != "off") {
            log.debug "Humidity Dropped Below ${humidityLow}: deactivating ${dehumidifierSwitch.label}"
            dehumidifierSwitch?.off()
            unschedule(activeMaximumReachedHandler)
            state.lastStatus = "off"
        }
	}
}

private activeMaximumReachedHandler() {
	if (sendPushMessage) {
        sendPush("Dehumidifier has been active for the maximum alotted time of ${switchActiveMaximum} minutes. Turning off.")
    }
    dehumidifierSwitch?.off()
}