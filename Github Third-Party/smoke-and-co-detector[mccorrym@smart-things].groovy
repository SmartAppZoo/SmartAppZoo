/**
 *  Smoke and CO Detector
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
    name: "Smoke and CO Detector",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitor contact sensors connected to relays wired in series with smoke detectors to alert of smoke or CO conditions.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm@2x.png"
)

preferences {
    section("Choose the contact sensors connected to the smoke detectors.") {
	    input "sensors", "capability.sensor", required: true, multiple: true, title: "Which sensors to monitor?"
    }
}

def installed() {
    log.debug sensors.capabilities
    subscribe(sensors, "smoke", sensorChangeHandler)
}

def updated() {
    unsubscribe()
    subscribe(sensors, "smoke", sensorChangeHandler)
}

def sensorChangeHandler (evt) {
   log.debug "Sensor ${evt.device.getLabel()} has changed to: ${evt.value}"
}