/**
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	1-2-3 Button Controller
 *
 *	Author: Claudio Consolmagno
 *	Date: 2019-04-08
 */
definition(
        name: "1-2-3 Button Click Controller",
        namespace: "claudio.dev",
        author: "claudio",
        description: "Button clicks determine which of 3 switches will be switched on while switching off any others.",
        category: "Convenience"
)

preferences {
    section("Select Button(s) and Switches They Will Control"){
        input "buttons", "capability.button", multiple: true, title: "Buttons(s)"
        input "switchA", "capability.switch", title: "Single Click Switch (x1)"
        input "switchB", "capability.switch", title: "Double Click Switch (x2)"
        input "switchC", "capability.switch", title: "Triple Click Switch (x3)"
    }
}

def installed() {
    log.info "Installing - Started"
    initialize()
    log.info "Installing - Completed"
}

def updated() {
    log.info "Updating - Started"
    initialize()
    log.info "Updating - Completed"
}

def initialize() {
    unsubscribe()
    subscribe(buttons, "button.pushed", buttonsHandler)
    return
}

def buttonsHandler(evt) {
    def buttonPressed = evt.jsonData.buttonNumber
    log.info "Button ${buttonPressed} pushed"

    def currentStateA = switchA.currentValue("switch")
    def currentStateB = switchB.currentValue("switch")
    def currentStateC = switchC.currentValue("switch")
    log.debug "switchA status is ${currentStateA}"
    log.debug "switchB status is ${currentStateB}"
    log.debug "switchC status is ${currentStateC}"

    if (buttonPressed == 1) {
        currentStateA == 'on' ? switchA.off() : switchA.on()
        switchB.off()
        switchC.off()
    } else if (buttonPressed == 2) {
        switchA.off()
        currentStateB == 'on' ? switchB.off() : switchB.on()
        switchC.off()
    } else if (buttonPressed == 3) {
        switchA.off()
        switchB.off()
        currentStateC == 'on' ? switchC.off() : switchC.on()
    } else if (buttonPressed == 4) {
        switchA.off()
        switchB.off()
        switchC.off()
    } else {
        log.info "Unknown push action"
    }

}
