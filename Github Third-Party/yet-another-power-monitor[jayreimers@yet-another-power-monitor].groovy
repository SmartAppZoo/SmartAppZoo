/**
 *  Yet Another Power Monitor
 *
 *  Copyright 2015 Elastic Development
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
 *  The latest version of this file can be found at:
 *  https://github.com/jpansarasa/SmartThings/blob/master/SmartApps/YetAnotherPowerMonitor.groovy
 *
 *  Revision History
 *  ----------------
 *
 *  2015-01-04: Version: 1.0.0
 *  Initial Revision
 *  2015-01-05: Version: 1.0.1
 *  Reorganized preferences section
 *  2015-01-18: Version: 1.1.0
 *  Added option to disable polling
 *  2015-10-02: Version: 1.2.0
 *  Removed the code to set the icon since it crashed the app on the phone
 *  2016-07-11: Version: 1.3.0
 *  Switched to using contact book for notifications
 *  2016-07-11: Version: 1.4.0
 *  Added option to include the cycle duration
 *  2016-08-14: Version: 1.5.0
 *  Eliminated duplicate notifications
 *  2017-04-04: Version 1.6.0
 *  Added spoken notifications
 *
 */

definition(
        name: "Yet Another Power Monitor",
        namespace: "reimersjc",
        author: "James P & Jason R",
        description: "Using power monitoring switch, monitor for a change in power consumption, and alert when the power draw stops.",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("About") {
        paragraph "Using power monitoring switch, monitor for a change in power consumption, and alert when the power draw stops."
        paragraph "Version 1.6"
    }

    section ("When this device stops drawing power") {
        input "meter", "capability.powerMeter", multiple: false, required: true
    }

    section ("Advanced options", hidden: true, hideable: true) {
        input "upperThreshold", "number", title: "start when power raises above (W)", description: "10", defaultValue: 10, required: true
        input "lowerThreshold", "number", title: "stop when power drops below (W)", description: "5", defaultValue: 5, required: true
    }

    section ("Send this message") {
        input "message", "text", title: "Notification message", description: "Washer is done!", required: true
        input "includeDuration", "boolean", title: "Include the duration in the message?", defaultValue: false
    }

    section ("Notification method") {
        input "sendPushMessage", "boolean", title: "Send a push notification?", defaultValue: true
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Notify with text message (optional)", description: "Phone Number", required: false
        }
    }

    section ("Sonos") {
        input "sonos", "capability.musicPlayer", title: "On this Sonos player", required: false, multiple: true
        input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
    }

    section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
        input "enablePolling", "boolean", title: "Enable polling?", defaultValue: false
        input "interval", "number", title: "Polling interval in minutes:", description: "5", defaultValue: 5
        input "debugOutput", "boolean", title: "Enable debug logging?", defaultValue: false
    }
}

def installed() {
    log.trace "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.trace "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

/**
 *	Initialize the script
 *
 *	Create the scheduled event subscribe to the power event
 */
def initialize() {
    //Set the initial state
    state.cycleStart = null
    state.debug = (debugOutput) ? debugOutput.toBoolean() : false
    state.duration = (includeDuration) ? includeDuration.toBoolean() : false

    //Schedule the tickler to run on the defined interval
    def pollingInterval = (interval) ? interval : 5
    def ticklerSchedule = "0 0/${pollingInterval} * * * ?"

    if (state.debug) {
        if (enablePolling && enablePolling.toBoolean()) {
            log.debug "Polling every ${pollingInterval} minutes"
        }
        else {
            log.debug "Polling disabled"
        }
    }
    if (enablePolling && enablePolling.toBoolean()) {
        schedule(ticklerSchedule, tickler)
    }
    subscribe(meter, "power", powerHandler)

    if (sonos) {
        loadText()
    }
}

/**
 *	Scheduled event handler
 *
 *	Called at the specified interval to poll the metering switch.
 *	This keeps the device active otherwise the power events do not get sent
 *
 *	evt		The scheduler event (always null)
 */
def tickler(evt) {
    meter.poll()

    def currPower = meter.currentValue("power")
    if (state.debug && currPower > upperThreshold) {
        log.debug "Power ${currPower}W above threshold of ${upperThreshold}W"
    }
    else if (state.debug && currPower <= lowerThreshold) {
        log.debug "Power ${currPower}W below threshold of ${lowerThreshold}W"
    }
}

/**
 *	Power event handler
 *
 *	Called when there is a change in the power value.
 *
 *	evt		The power event
 */
def powerHandler(evt) {
    if (state.debug) {
        log.debug "power evt: ${evt}"
        log.debug "state: ${state}"
    }

    def currPower = meter.currentValue("power")
    log.trace "Power: ${currPower}W"

    //If cycle is not on and power exceeds upper threshold, start the cycle
    if ((currPower > upperThreshold) && !atomicState.cycleStart) {
        atomicState.cycleStart = now()
        log.trace "Cycle started."
    }
    // If the device stops drawing power, the cycle is complete, send notification.
    else if ((currPower <= lowerThreshold) && atomicState.cycleStart) {
        def duration = now() - state.cycleStart
        atomicState.cycleStart = null
        log.trace "Cycle ended after ${duration} milliseconds."
        if (state.duration) {
            def d = new Date(duration)
            def h = d.getHours()
            def m = d.getMinutes()
            def s = d.getSeconds()
            def msg = "${message} - Cycle ended after " + "$h:".padLeft(3,'0') + "$m:".padLeft(3,'0') + "${s}".padLeft(2,'0') + " (HH:MM:SS)"
            send(msg)
        }
        else {
            send(message)
        }
        if (sonos) {
            speakMessage()
        }
    }
}

/**
 *	Sends the messages to the subscribers
 *
 *	msg		The string message to send to the subscribers
 */
private send(msg) {
    if (sendPushMessage.toBoolean()) {
        sendPush(msg)
    }

    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else if (phone) { // check that the user did select a phone number
        sendSms(phone, msg)
    }

    if (state.debug) {
        log.debug msg
    }
}

/**
 * Speaks the message
 * @param msg
 */
private speakMessage(msg) {
    if (resumePlaying) {
        sonos.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
    } else {
        sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
    }
}

private loadText() {
    state.sound = textToSpeech(message instanceof List ? message[0] : message) // not sure why this is (sometimes) needed)
}

/**
 * Enables/Disables the optional section
 */
private hideOptionsSection() {
    (interval || debugOutput) ? false : true
}
//EOF