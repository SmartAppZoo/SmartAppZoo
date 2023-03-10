/*
 *  Copyright 2015 SmartThings
 *  Copyright 2016 S.Çağlar Onur
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
    name: "Left It Open",
    namespace: "caglar10ur",
    author: "S.Çağlar Onur",
    description: "Watch contact sensor and notify if left open",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    section("Monitor this door or window") {
        input "contact", "capability.contactSensor", title: "Sensor to monitor", required: true
    }
    section("And notify me if it's open for more than this many minutes (default 10)") {
        input "openThreshold", "number", title: "Number of minutes", defaultValue: 10, required: false
    }
    section {
        input "sonos", "capability.musicPlayer", title: "On this Sonos player", required: true
    }
    
    section("Delay between notifications (default 10 minutes") {
        input "frequency", "number", title: "Number of minutes", defaultValue: 10, required: false
    }
    section("More options", hideable: true, hidden: true) {
  		input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone number (optional)", required: false
        }
        input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
    }
}

def installed() {
    log.trace "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.trace "Updated with settings: ${settings}"

    unsubscribe()
    // unschedule all tasks
    unschedule()

    initialize()
}

def initialize() {
    subscribe(contact, "contact.open", opened)
    subscribe(contact, "contact.closed", closed)
}

def opened(evt) {
    log.trace "opened($evt.name: $evt.value)"

    def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
    runIn(delay, tooLong, [overwrite: false])
}

def closed(evt) {
    log.trace "closed($evt.name: $evt.value)"
}

def tooLong() {
    log.trace "tooLong()"

    def contactState = contact.currentState("contact")
    if (contactState.value == "open") {
        def elapsed = now() - contactState.rawDateCreated.time
        def threshold = (openThreshold != null && openThreshold != "") ? openThreshold : 10
        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        if (elapsed >= (threshold * 60000) - 1000) {
            def freq = (frequency != null && frequency != "") ? frequency * 60 : 600
            // schedule the next notification
            runIn(freq, doorOpenTooLong, [overwrite: false])

			def msg = "${contact.displayName} has been left open for ${threshold} minutes."
            speak(msg)
            notify(msg)
        } else {
            log.debug "Contact has not stayed open long enough since last check ($elapsed ms): doing nothing"
        }
    } else {
        log.debug "doorOpenTooLong() called but contact is closed: doing nothing"
    }
}

private speak(msg) {
    log.trace "speak(${msg})"

    def sound = textToSpeech(msg)
    if (resumePlaying){
        sonos.playTrackAndResume(sound.uri, volume)
    } else {
        sonos.playTrackAndRestore(sound.uri, volume)
    }
}

private notify(msg) {
    log.trace "sendMessage(${msg})"

    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    } else {
        if (phone) {
            sendSms phone, msg
        } else {
            sendPush msg
        }
    }
}
