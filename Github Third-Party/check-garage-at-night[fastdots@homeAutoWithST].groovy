/**
 *  Check Garage At Night
 *
 *  Copyright 2019 AJEY TATAKE
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
    name: "Check Garage At Night",
    namespace: "tatake_labs",
    author: "AJEY TATAKE",
    description: "Smart routine to check if the garage door is open",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select Garage Door") {
        input "garageDoor", "capability.contactSensor", required: true
        input "theTime", "time", title: "Time to execute every day"
    }
    
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: true
            input "phone2", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
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
	schedule(theTime, dailyCheck)
}


def dailyCheck (evt) {
    log.debug "recipients configured: $recipients"
    //def currentState = garageDoor.currentState("contact").value
    def currentContact = garageDoor.currentContact
    //log.debug "currentState: $currentState"
    log.debug "currentContact: $currentContact"
    sendSms (phone, "Garage door is $currentContact")
    if (phone2) {
        sendSms (phone2, "Garage door is $currentContact")
    }
   
}
