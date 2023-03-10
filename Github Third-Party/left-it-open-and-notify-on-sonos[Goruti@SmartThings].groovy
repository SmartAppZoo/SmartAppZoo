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
 *  Left It Open
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 */
definition(
    name: "Left It Open and notify on SONOS",
    namespace: "DiegoAntonino",
    author: "SmartThings",
    description: "Notifies on SONOS you when you have left a door or window open longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {
	page(name: "mainPage", title: "Play message on your speaker when a door left open", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section("Play message when") {
            input "contact", "capability.contactSensor", multiple: false
        }
        section("And notify me if it's open for more than this many minutes (default 5)") {
            input "openThreshold", "number", description: "Number of minutes", required: false
        }
        section("Delay between notifications (default 5 minutes") {
            input "frequency", "number", description: "Number of minutes", required: false
        }
        section {
            input "sonos", "capability.audioNotification", title: "On this Speaker player", required: true
        }
        section("More options", hideable: true, hidden: true) {
            input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
            input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
        }
        section("Select Home Modes when you want to run this rule...") {
            input "HomeMode", "mode", required: false, title: "Home Modes?", multiple: true
        }
            section() {
            label title: "Assign a name", required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
	subscribe()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe()
}

def subscribe() {
    subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", doorOpen)
}

def appTouchHandler(evt){

    log.debug "evt = ${evt}"
    def msg = "Test message for $app.label"
    log.debug "msg = ${msg}"
    state.sound = textToSpeech(msg)

    if (resumePlaying){
        sonos.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
    }
    else if (volume) {
        sonos.playTrackAtVolume(state.sound.uri, volume)
    }
    else {
        sonos.playTrack(state.sound.uri)
    }
}

def doorOpen(evt)
{
    def curMode = location.mode
    log.trace "curMode: $curMode"

    if (HomeMode != '' || HomeMode == null || HomeMode?.find{it == curMode}) {
        log.trace "eventHandler($evt.name: $evt.value)"
        def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 300
        runIn(delay, doorOpenTooLong)
    }
}

def doorOpenTooLong() {
	def contactState = contact.currentState("contact")

    def freq = (frequency != null && frequency != "") ? frequency * 60 : 300

	if (contactState.value == "open") {
		def elapsed = now() - contactState.rawDateCreated.time
		def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 30000) - 1000
		if (elapsed >= threshold) {
			log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
            sendMessage(elapsed)
            runIn(freq, doorOpenTooLong, [overwrite: false])
		} else {
			log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
		}
	} else {
		log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
	}
}

void sendMessage(elapsed)
{
    loadText(elapsed)

    if (resumePlaying){
        sonos.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
    }
    else if (volume) {
        sonos.playTrackAtVolume(state.sound.uri, volume)
    }
    else {
        sonos.playTrack(state.sound.uri)
    }

}

private loadText(elapsed){
    def minutes = Math.round(elapsed / 60000)
    def msg = "${contact.displayName} has been left open for ${minutes} minutes."

    log.debug "msg = ${msg}"
    state.sound = textToSpeech(msg) //This generate the mp3 on \
    // StateSound = [uri:https://s3.amazonaws.com/smartapp-media/tts/0e5d6b9432d2dff1717dd1c2b6faf059f99edbad.mp3, duration:3]

}
