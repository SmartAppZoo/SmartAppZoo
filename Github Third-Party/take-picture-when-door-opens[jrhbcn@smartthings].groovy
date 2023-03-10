/**
 *  SnapPicture when Trigger 
 *
 *  Copyright 2016 Javier Ruiz Hidalgo
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
    name: "Take Picture when door opens",
    namespace: "jrhbcn",
    author: "Javier Ruiz Hidalgo",
    description: "Takes a picture when a door opens in Away mode",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Preferences:") {
        input "thedoor", "capability.contactSensor", title: "Door?", required: true
        input "thecamera", "capability.imageCapture", title: "Camera?", required: true
		input "burstCount", "number", title: "How many? (default 5)", defaultValue:5
		input "delaySeconds", "decimal", title: "How many seconds between images? (default 1.0)", defaultValue: 1.0
		input "sendPush", "bool", title: "Send Push Notifications?", required: false
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(thedoor, "contact.open", contactHandler)
}

def contactHandler(evt) {
    log.debug "contactHandler called: $evt"

	thecamera.take()
	(1..((burstCount ?: 5) - 1)).each {
		thecamera.take(delay: (1000 * (delaySeconds ?: 1.0) * it))
	}

	if (sendPush) {
        sendPush("Door has been opened!")
    }
}

