/**
 *  Notify When On Too Long
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
    name: "Notify When On Too Long",
    namespace: "BrianJerolleman",
    author: "Brian Jerolleman",
    description: "Notifies you when a switch is on for a specified number of hours.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Switch to monitor:") {
        input "monitorSwitch", "capability.switch", required: true
    }
    section("Notify after this number of hours:") {
        input "hours", "number", required: true, title: "Hours?"
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
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
	log.debug "Updated with settings: ${settings}"

    subscribe(monitorSwitch, "switch.on", monitorSwitchOnHandler)
    subscribe(monitorSwitch, "switch.off", monitorSwitchOffHandler)

    resetTimer()
}

def monitorSwitchOnHandler(evt) {
    log.debug "monitorSwitchOnHandler called: $evt"

    state.monitorSwitchOn = new Date().format("dd/MM/yyyy hh:mm:ss a Z")
    runIn(state.secondsLeft, monitorSwitchCheckHandler)
}

def monitorSwitchOffHandler(evt) {
    log.debug "monitorSwitchOffHandler called: $evt, state.monitorSwitchOn = $state.monitorSwitchOn"

    def diff = (new Date().getTime() - Date.parse("dd/MM/yyyy hh:mm:ss a Z", state.monitorSwitchOn).getTime()) / 1000
    state.secondsLeft = state.secondsLeft - diff

    log.debug "monitorSwitchOffHandler called: state.secondsLeft = $state.secondsLeft, diff = $diff"
}

def monitorSwitchCheckHandler(evt) {
    log.debug "monitorSwitchCheckHandler called: $evt"

    if (state.secondsLeft < 0) {
        resetTimer()
        sendNotification()
    }
}

private resetTimer() {
    state.secondsLeft = hours * 60 * 60

    if (monitorSwitch.currentState("switch").value == "on") {
        state.monitorSwitchOn = new Date()
        runIn(state.secondsLeft, monitorSwitchCheckHandler)
    }
}

private sendNotification() {
    def message = "The ${monitorSwitch.displayName} has been on for $hours hours."
    if (location.contactBookEnabled && recipients) {
        log.debug "Contact Book enabled!"
        sendNotificationToContacts(message, recipients)
    } else {
        log.debug "Contact Book not enabled"
        if (phone) {
            sendSms(phone, message)
        }
    }
}
