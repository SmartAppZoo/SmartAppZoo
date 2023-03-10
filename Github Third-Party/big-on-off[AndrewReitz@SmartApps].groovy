/*
 * Copyright (C) 2014 Andrew Reitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *  Big On/Off
 *
 *  Create a group of devices that can be turned on when the app icon is pressed
 *  and turned off again when the app icon is pressed again
 */
preferences {
    section("When I touch the app, turn on/off...") {
        input "switches", "capability.switch", multiple: true
    }
}

def installed() {
    init()
}

def updated() {
    unsubscribe()
    init()
}

def init() {
    subscribe(app, appTouch)
}

def appTouch(evt) {
    log.debug "appTouch: $evt"
    state.enabled = !state.enabled
    if (state.enabled) {
        switches?.on()
    } else {
        switches?.off()
    }
}
