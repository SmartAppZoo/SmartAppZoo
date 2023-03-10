/**
 *  Blinkt Alarm Status
 *
 *  Copyright 2017 Todd Garrison
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
    name: "Blinkt Alarm Status",
    namespace: "frameloss",
    author: "Todd Garrison",
    description: "REST API endpoint for getting alarm state, consumed by a python script on a raspberry pi to show light status on a blinkt (https://shop.pimoroni.com/products/blinkt) LED array.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section(title: "Select Devices") {
        input "alarm", "capability.alarm", title: "Select an Alarm", required: false, multiple:false
    }
}

def installed() {
 state.activeAlarm = "off"
 subscribe(alarm, "alarm", alarmHandler)
}
def updated() {}

mappings {
    path("/mode") {
        action: [
            GET: "getMode"
        ]
    }
}

def alarmHandler(evt) {
    state.activeAlarm = evt.value
}

def getMode() {
    return [ state: location.currentState("alarmSystemStatus"), alarm: state.activeAlarm ]
}
