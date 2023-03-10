/**
 *  Impuls Switcher
 *
 *  Copyright 2020 Hussein Khalil
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
name: "Impuls Switcher", namespace: "husseinmohkhalil", author: "Hussein Khalil", description: "Impuls Switcher", category: "My Apps", iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png", iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png", iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("Which virtual switch You are going to use") {
    input(name: "virtualSwitch", type: "capability.switch", title: "Which switch?", required: true)
  }
  section("Which switch to act as Impuls ") {
    input(name: "targetSwitch", type: "capability.switch", title: "Target switch", multiple: false, required: true)
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
  subscribe(virtualSwitch, "switch.on", SwitchOnHandler)
  subscribe(virtualSwitch, "switch.off", SwitchOffHandler)
}
             
def SwitchOnHandler(evt) {
  log.debug "Updated with settings: ${evt.value}"
    targetSwitch.on()
    runIn(1000, targetSwitch.off())
}

def SwitchOffHandler(evt) {
  log.debug "Updated with settings: ${evt.value}"
    targetSwitch.on()
    runIn(1000, targetSwitch.off())
}