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
 * Turn Off In...
 *
 * Turn off a device in a specified amount of time. I personally use this for my dehydrator, but I'm sure there
 * are all sorts of other practical applications
 */
preferences {
    section("Select switches to turn off...") {
        input name: "switches", type: "capability.switch", multiple: true
    }
    section("Turn them off at...") {
        input name: "offTimeHr", title: "Turn Off In (hrs)...", type: "number"
        input name: "offTimeMin", title: "Turn Off In (mins)...", type: "number"
    }
    section("Send a notification?"){
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialized()
}

def initialized () {
    def offTime = offTimeHr * 60 * 60 + offTimeMin * 60
    log.debug "timer set to ${offTime} minutes"
    runIn(offTime, "startTimerCallback")
}

def updated(settings) {
    unschedule()
    initialized()
}

def startTimerCallback() {
    log.debug "Turning off switches"
    switches?.off()
    if (sendPushMessage == "Yes") sendPush "Turning off ${switches.displayName}"
}