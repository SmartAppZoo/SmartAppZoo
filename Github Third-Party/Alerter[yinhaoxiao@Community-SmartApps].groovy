/**
 *  Remote LANdroid Alerter
 *
 *  Note that this requires the SmartThings device handler "LANdroid" (also by KeyBounce), and the Android service
 *  LANdroid downloadable at https://play.google.com/store/apps/details?id=com.keybounce.ttsservice
 *
 *  Copyright 2015 Tony McNamara
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
    name: "Remote LANdroid Alerter",
    namespace: "KeyBounce",
    author: "Tony McNamara",
    description: "Forwards LANdroid Alerter functionality to an Android cell phone",
    category: "Safety & Security",
    iconUrl: "http://www.keybounce.com/resources/LANdroid%20Icon%2060.png",
    iconX2Url: "http://www.keybounce.com/resources/LANdroid%20Icon%20120.png",
    iconX3Url: "http://www.keybounce.com/resources/LANdroid%20Icon.png"
) 


preferences {
    section("Send alerts from... to...") {
        input "smsnumber", "text", title: "Phone number", required: true, description:"Phone running the LANdroid client application, to send SMS to"
        input "landroidref", "capability.tone",  required: true, description:"LANdroid instance to propagate alerts from"
    }    
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    log.debug landroidref.capabilities.inspect()
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
    subscribe(landroidref, "LANdroidSMS", smsEventHandler)
}

def smsEventHandler(messageEvent) {
    log.debug "smsEventHandler called: $messageEvent"
    def SMSPhone = smsnumber
    if (SMSPhone?.trim()) {
        sendSmsMessage(SMSPhone, messageEvent.stringValue)
    }
}
