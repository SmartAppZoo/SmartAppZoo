/**
 *  Seinfelder
 *
 *  Copyright 2015 Austin Fonacier
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
    name: "Seinfelder",
    namespace: "austinrfnd",
    author: "Austin Fonacier",
    description: "Don't you love it when you open the door and you can instantly jump in the world of Seinfeld?  Well here's your chance!",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Speakers") {
    	input "door", "capability.contactSensor", title: "Where?", required: true
        input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: true
        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
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
    subscribe(door, "contact.open", doorOpenHandler)
}

def doorOpenHandler(evt) {
	log.info("DOOR OPEN HANDLER ${evt}")
  sonos.playTrack("http://s3-us-west-2.amazonaws.com/blog-spokeo-test/seinfeld.mp3")
}
