/**
 *  Siren On Delay
 *
 *  Copyright 2017 Brian Jerolleman
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
    name: "Siren On Delay",
    namespace: "BrianJerolleman",
    author: "Brian Jerolleman",
    description: "Turns the siren on after a delay.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-AudioVisualSmokeAlarm@2x.png")

preferences {
    section("Turn on siren when this is turned on:") {
        input "theTriggerSiren", "capability.alarm", required: true, title: "Siren?"
    }
    section("Turn off after this number of seconds:") {
        input "seconds", "number", required: true, title: "Seconds?"
    }
    section("Turn on this siren:") {
        input "theRealSiren", "capability.alarm", required: true
    }
    section("Blink these switches:") {
        input "blinkSwitches", "capability.switch", required: true, multiple: true
    }
}

def installed() {
    log.debug "siren delay installed"
    initialize()
}

def updated() {
    log.debug "siren delay updated"
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "siren delay initialize"
    subscribe(theTriggerSiren, "alarm.both", sirenStartedHandler)
    subscribe(theTriggerSiren, "alarm.off", sirenStoppedHandler)
}

def sirenStartedHandler(evt) {
    log.debug "sirenStartedHandler called: $evt"
    runIn(seconds, startSiren)
    
    blinkSwitches*.off([delay: 3000])
    blinkSwitches*.on([delay: 6000])
    blinkSwitches*.off([delay: 9000])
    blinkSwitches*.on([delay: 12000])
}

def startSiren() {
    log.debug "startSiren called: " + theTriggerSiren.currentState("alarm").value
    if (theTriggerSiren.currentState("alarm").value == "both") {
        log.debug "startSiren starting siren"
        theRealSiren.both()
    } else {
        log.debug "Alarm was cancelled"
        sendPush("Alarm was cancelled")
    }
}

def sirenStoppedHandler(evt) {
    log.debug "sirenStoppedHandler called: $evt"
    theRealSiren.off()
}
