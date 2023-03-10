/**
*  Garage Door Left Open?
*
*  Copyright 2017 Dave Watson
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
    name: "Garage Door Left Open?",
    namespace: "drdavew.co.uk",
    author: "Dave Watson",
    description: "Is the garage door open when I leave?",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Garage door:") {
        input "theDoor", "capability.contactSensor", required: true, title: "Which door sensor?"
    }
    section() {
        input "minutes", "decimal", required: true, title: "How long to wait with the door open (in minutes)?"
    }
    section("Presence:") {
        input "thePresence", "capability.presenceSensor", required: true, title: "Which presence sensor?"
    }
    section("Send Notifications?") {
        input "phone", "phone", title: "Phone number for warning SMS (include '+44')",
            description: "Phone Number", required: true

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
    subscribe(thePresence, "presence", presenceHandler)
    subscribe(theDoor, "contact", doorHandler)
    log.debug "Subscribed."
    // Cancel any old scheduled event.
    unschedule(checkDoor)

    // Check the state of the door now.
    def currentState = theDoor.currentState("contact")
    def doorIs = currentState.value
    log.debug "The door is ${doorIs}."
    if (doorIs == "open") {
        log.debug "The door is open. Start the clock."
        // Check back in X minutes to see if it is still open.
        runIn(60 * minutes, checkDoor)
    }
}

def doorHandler(evt) {
    log.debug "doorHandler ${evt.value}."
    if (evt.value == "open") {
        log.debug "The door has been opened. Start the clock."
        // Check back in X minutes to see if it is still open.
        runIn(60 * minutes, checkDoor)
    } else {
        log.debug "The door has been closed. Call off the dogs."
        unschedule(checkDoor)
    }        
}

def checkDoor() {
    log.debug "The door is STILL open!"
    def message = "Hey there! The door garage door has been open for ${minutes}"
    if (minutes == 1) {
        message = message + " minute."
    } else {
        message = message + " minutes."
    }
    log.debug message
    sendSms(phone, message)
}


def presenceHandler(evt) {
    log.debug "presenceHandler ${evt.value}."

    if (evt.value == "present") {
        log.debug "Not doing anything. Present."
    } else {
        log.debug "Ah ha! Gone away."
        // Prepare the time.
        def dateFormatter = new java.text.SimpleDateFormat("HH:mm:ss")
        // Ensure the new date object is set to local time zone
        dateFormatter.setTimeZone(location.timeZone)
        def time = dateFormatter.format(new Date())

        // Check the state of the door.
        def currentState = theDoor.currentState("contact")
        def doorIs = currentState.value
        log.debug "The door is ${doorIs}."
        if (doorIs == "open") {
            def message = "OMG! The door has been left open! (${time})"
            log.debug message
            sendSms(phone, message)
        } else {
            def message = "Relax, the garage is closed. (${time})"
            sendPush(message)
        }
    }

}