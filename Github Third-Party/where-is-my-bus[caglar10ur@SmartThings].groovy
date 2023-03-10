/**
 *  Where is my the bus?
 *
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
    name: "Where is my bus?",
    namespace: "caglar10ur",
    author: "S.Çağlar Onur",
    description: "Where is my bus?",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Bus-NG details") {
        input "agency", "text", title: "Agency", required: true, defaultValue: "actransit"
        input "route", "text", title: "Route", required: true, defaultValue: "39"
        input "direction", "text", title: "Direction", required: true, defaultValue: "39_16_1"
        input "stop", "text", title: "Stop", required: true, defaultValue: "1010920"
    }
    section("Virtual switch to monitor") {
        input "contact", "capability.momentary", title: "Sensor to monitor", required: true
    }

    section {
        input "sonos", "capability.musicPlayer", title: "On this Sonos player", required: true
    }
    section("More options", hideable: true, hidden: true) {
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
    initialize()
}

def initialize() {
    log.trace "Initialized with settings: ${settings}"

    subscribe(contact, "switch.on", refresh)
}

def refresh($evt) {
    log.trace "Refresh called with ${evt}"

    // construct the url
    def url = "http://bus-ng.10ur.org/agencies/${agency}/routes/${route}/directions/${direction}/stops/${stop}/predictions/"
    try {
        httpGet(url) { resp ->
            if(resp.status == 200 && resp.data) {
                log.debug "Agency: ${resp.data.estimations.agency_title}"
                log.debug "Route: ${resp.data.estimations.route_title}"
                log.debug "Stop: ${resp.data.estimations.stop_title}"

                def msg = ""
                if (resp.data.estimations.predictions.size() == 0) {
                    msg = "Master, looks like there is no bus scheduled for ${resp.data.estimations.route_title}."
                }

                if (resp.data.estimations.predictions.size() > 0) {
                    log.debug "Predictions: ${resp.data.estimations.predictions}"
                    msg = "Master, your bus is ${resp.data.estimations.predictions[0].minutes} minutes away. Your bus is ${resp.data.estimations.route_title}, it goes ${resp.data.estimations.predictions[0].dir_title} and your stop is at ${resp.data.estimations.stop_title} ."
                }

                if (resp.data.estimations.predictions.size() == 1) {
                    msg = msg + "Unfortunately that is the last bus for today ."
                }

                if (resp.data.estimations.predictions.size() > 1) {
                    msg = msg + "If you can not catch this one then you can catch the next bus. The next one is in ${resp.data.estimations.predictions[1].minutes} minutes ."
                }

                def sound = textToSpeech(msg)
                if (resumePlaying){
                    sonos.playTrackAndResume(sound.uri, volume)
                } else {
                    sonos.playTrackAndRestore(sound.uri, volume)
                }
            } else {
                log.error "HTTP Get failed with ${resp.status}"
            }
        }
    } catch (e) {
        log.error "Soomething went wrong: $e"
    }
}
