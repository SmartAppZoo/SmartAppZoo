/**
 *  Auto Camera Monitor
 *
 *  Copyright 2015 Shuai Wang
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
    name: "Auto Camera Monitor",
    namespace: "etrnls",
    author: "Shuai Wang",
    description: "Automatically update camera motion monitoring state.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-IsItSafe@2x.png"
)

preferences {
    input "presences", "capability.presenceSensor", title: "Choose which people", multiple: true
    input "cameras", "capability.imageCapture", title: "Choose which cameras", multiple: true
}

def installed() {
    log.trace "installed() settings: ${settings}"
    initialize()
}

def updated() {
    log.trace "updated() settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(presences, "presence", presence)
}

def presence(evt) {
    log.trace "presence(${evt.value})"
    if (evt.value == "present") {
        log.debug "Turning motion monitoring off"
        cameras.motionOff()
        sendPush("Camera motion monitoring off")
    } else if (presences.find{it.currentPresence == "present"} == null) {
        log.debug "Turning motion monitoring on"
        cameras.motionOn()
        sendPush("Camera motion monitoring on")
    }
}
