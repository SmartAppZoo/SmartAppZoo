/**
 *  Water Sensor
 *
 *  Copyright 2019 Matt
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
    name: "Water Sensor",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitors the water sensors on the network for leaks and/or floods and sends emergency notifications as needed.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/moisture_detected.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/moisture_detected@2x.png")

preferences {
	section("Choose the water sensor(s) you'd like to monitor.") {
		input "sensors", "capability.waterSensor", required: true, multiple: true, title: "Which sensor(s) to monitor?"
	}
}

def installed() {
    subscribe(sensors, "water", waterChangeHandler)
}

def updated() {
    unsubscribe()
    subscribe(sensors, "water", waterChangeHandler)
}

def waterChangeHandler(evt) {
	if (evt.value == "wet") {
    	sendPush("ALERT: Water has been detected near the ${evt.device.getLabel().toLowerCase()}!")
    } else {
    	sendPush("Water is no longer being detected near the ${evt.device.getLabel().toLowerCase()}.")
    }
}