/**
 *  Copyright 2015 SmartThings
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
        name: "Shades Controller",
        namespace: "no.f12.smartthings",
        author: "Anders Sveen (based on original by Luis Pinto)",
        description: "Control your shades with buttons.",
        category: "Convenience",
        iconUrl: "http://www.ezex.co.kr/img/st/window_close.png",
        iconX2Url: "http://www.ezex.co.kr/img/st/window_close.png"
)

preferences {
    section("Control Close Buttons...") {
        input "switchesOpen", "capability.button", multiple: true, title: "Open Buttons", required: true
        input "switchesClose", "capability.button", multiple: true, title: "Close Buttons", required: true
        input "shades", "capability.windowShade", multiple: true, title: "Shades", required: true
    }
}

def installed() {
    subscribe(switchesClose, "button", buttonEventClose)
    subscribe(switchesOpen, "button", buttonEventOpen)
    subscribe(switchesPause, "button", buttonEventPause)
}


def updated() {
    unsubscribe()
    unschedule()
    installed()
}

def buttonEventPause(evt) {
    log.debug "Pausing Shades: $evt"
    shades.pause();
}

def buttonEventClose(evt) {
    log.debug "Closing Shades: $evt"

    if (evt.value == "pushed") {
        shades.open()
    }
}

def buttonEventOpen(evt) {
    log.debug "Opening shades: $evt"

    if (evt.value == "pushed") {
        shades.close()
    }
}
