/**
 *  Remote Control
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
    name: "Remote Control",
    namespace: "etrnls",
    author: "Shuai Wang",
    description: "Control devices with remote",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    input "remote", "capability.button", title: "Remote"
    input "off", "number", title: "Off"
    section("Light0") {
        input "light0", "capability.switch", title: "Light", required: false
        input "light0_up", "number", title: "Up", required: false
        input "light0_down", "number", title: "Down", required: false
    }
    section("Light1") {
        input "light1", "capability.switch", title: "Light", required: false
        input "light1_up", "number", title: "Up", required: false
        input "light1_down", "number", title: "Down", required: false
    }
    section("Light2") {
        input "light2", "capability.switch", title: "Light", required: false
        input "light2_up", "number", title: "Up", required: false
        input "light2_down", "number", title: "Down", required: false
    }
    section("Light3") {
        input "light3", "capability.switch", title: "Light", required: false
        input "light3_up", "number", title: "Up", required: false
        input "light3_down", "number", title: "Down", required: false
    }
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
    log.trace "initialize()"
    subscribe(remote, "button", button, [filterEvents: false])
}

def button(evt) {
    log.trace "button(${evt.value} ${evt.data})"
    def buttonNumber = parseJson(evt.data).buttonNumber
    if (settings["off"] == buttonNumber) {
        log.debug "Turning all off"
        (0..3).each { settings["light${it}"]?.off() }
    }
    (0..3).each {
        if (settings["light${it}_up"] == buttonNumber) {
            log.debug "Turning on light ${it}"
            settings["light${it}"].on()
        } else if (settings["light${it}_down"] == buttonNumber) {
            log.debug "Turning off light ${it}"
            settings["light${it}"].off()
        }
    }
}
