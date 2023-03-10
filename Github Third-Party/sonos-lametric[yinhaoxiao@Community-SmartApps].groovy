 /**
 *  Display Sonos Now Playing on LaMetric Display
 *
 *  Copyright © 2016 John Rubin
 *
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
	name: "Display Sonos Now Playing on LaMetric",
	namespace: "smartthings",
	author: "John Rubin",
	description: "Displays the currently playing track on LaMetric display.",
	category: "Convenience",
	iconUrl: "https://developer.lametric.com/assets/smart_things/weather_60.png",
	iconX2Url: "https://developer.lametric.com/assets/smart_things/weather_120.png",
	oauth: true
)

preferences {
	section("Choose speaker to monitor...") {
	input "sonos", "capability.musicPlayer", title: "Which Sonos Speaker?", multiple: false, required: true
    }
	section("Enter the Push URL from the LaMetric Developer site:") {
	input "push_url", "text", title: "Push URL:", multiple: false, requried: true
    }
	section("Enter the Access Token from the LaMetric Developer site:") {
	input "access_token", "text", title: "Access Token:", multiple: false, required: true
    }
	section("Enter your icon ID number:") {
	input "icon_id", "text", title: "Icon ID:", multiple: false, required: true
    }
	section("Send text message to...") {
	paragraph "Optionally, send text message containing the URL for activating LaMetric Button to your phone number. The URL will be sent in two parts because it's too long."
        input "phone", "phone", title: "Which phone?", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
	subscribeToEvents()
}

def initialize() {
	subscribe(app, getURL)
	getURL(null)
}

def getURL(e) {
	if (!state.accessToken) {
	createAccessToken()
	log.debug "Creating new Access Token: $state.accessToken"
	}
	def url1 = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/toggle"
	def url2 = "?access_token=${state.accessToken}"
	log.debug "${title ?: location.name} LaMetric Button URL: $url1$url2"
	if (phone) {
		sendSmsMessage(phone, url1)
		sendSmsMessage(phone, url2)
	}
}
mappings {
  path("/toggle") {
    action: [
      GET: "toggleAudio",
    ]
  }
}

def toggleAudio() {
	def currentStatus = sonos.currentValue("status")
	if (currentStatus == "playing") {
	    sonos.pause()
	}
	else {
	    sonos.play()
	}
}

def subscribeToEvents() {
	subscribe(sonos, "trackDescription", eventHandler)
	subscribe(sonos, "switch", eventHandler)
}

def eventHandler(evt) {
	log.debug "Track changed to $evt.value"
	sendHttppost(evt)
}

private sendHttppost(evt) {
	def upd_icon_id = "i" + icon_id
	def sonos_track
	if (evt.value == "off") {
		log.trace "Sonos is paused/stopped"
		sonos_track = " Now Playing: Nothing Playing"
	}
	else if (evt.value == "on") {
		log.trace "Sonos has resumed playing"
		sonos_track = " Now Playing: " + sonos.currentValue('trackDescription')
		log.trace "Sonos is currently playing $sonos_track"
	}
	else {
		log.trace "sonos is now playing song: $evt.value"
		sonos_track = " Now Playing: " + evt.value
	}
    def params = [
        uri: push_url,
        query: method =="POST" ? data : null,
        headers: ["accept" : "application/json","X-Access-Token" : access_token,"Cache-Control" : "no-cache"],
        body: [ frames:  [[text: sonos_track, icon: upd_icon_id]]]
    ]    
    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
	    log.debug "${it.name} : ${it.value}"
            }
            log.debug "Response contentType: ${resp.    contentType}"
            log.trace "Pushing Sonos track: $sonos_track"
	    }	
	} catch (e) {
		log.debug "something went wrong: $e"
	}
}
