/**
 *  Welcome Home Notification
 *
 *  Copyright (c) 2017 Scott Gulliver
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:

 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.

 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 *
 *
 *
 *  TODO:
 */
definition(
    name: "Welcome Home Notification",
    namespace: "scottg1989",
    author: "Scott Gulliver",
    description: "Play a notification when entering the house.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When this person..."){
        input "person", "capability.presenceSensor"
    }
    section("Enters this door...") {
		input "contactSensor", "capability.contactSensor"
    }
    section("Play this notification...") {
    	input "soundName", "text", defaultValue: "WelcomeName"
    }
    section("On this speaker...") {
		input "speaker", "capability.audioNotification"
    }
    section("Other settings") {
		input "entryTimeout", "number", title: "Time allowed between presence and entry (minutes)"
		input "audioTimeDelay", "number", title: "Audio time delay (seconds)"
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
	subscribe(person, "presence", presence)
	subscribe(contactSensor, "contact.open", contactOpen)
}

def presence(evt) {
    if (evt.value == "present") {
        atomicState.waitingForEntry = true
        runIn(60*entryTimeout, entryTimeoutHandler)
    } else {
        atomicState.waitingForEntry = false
    }
}

def contactOpen(evt) {
    if (atomicState.waitingForEntry) {
        atomicState.waitingForEntry = false

        if (audioTimeDelay) {
            runIn(audioTimeDelay, playNotification)
        } else {
            playNotification()
        }
    }
}

def entryTimeoutHandler() {
    atomicState.waitingForEntry = false
}

def playNotification() {
	log.trace "Playing sound '$soundName' here: $speaker"
	speaker.playTrack(soundName)
}