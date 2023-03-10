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
*  Author: LGKahn kahn-st@lgk.com
*  version 2 user defineable timeout before checking if door opened or closed correctly. Raised default to 25 secs. can reduce to 15 if you have custom simulated door with < 6 sec wait.
* https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/746b9a6a38b69aa472296fc57fb6227b28c7c0bd/smartapps/lgkapps/lgk-virtual-garage-door.src/lgk-virtual-garage-door.groovy
*
*  Author: kerbs17
*  Changes:
*      * removed some of the push notifications
*      * added an open threshold to receive a notification when the garage has been open for x minutes
*/

definition(
    name: "LGK Virtual Garage Door",
    namespace: "lgkapps",
    author: "lgkahn kahn-st@lgk.com",
    description: "Sync the Simulated garage door device with 2 actual devices, either a tilt or contact sensor and a switch or relay. The simulated device will then control the actual garage door. In addition, the virtual device will sync when the garage door is opened manually, \n It also attempts to double check the door was actually closed in case the beam was crossed. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
    section("Choose the switch/relay that opens closes the garage?"){
        input "opener", "capability.switch", title: "Physical Garage Opener?", required: true
    }
    section("Choose the sensor that senses if the garage is open closed? "){
        input "sensor", "capability.contactSensor", title: "Physical Garage Door Open/Closed?", required: true
        input "openThreshold", "number", title: "Warn when open longer than (optional)",description: "Number of minutes", required: false
    }
    
    section("Choose the Virtual Garage Door Device? "){
        input "virtualgd", "capability.doorControl", title: "Virtual Garage Door?", required: true
    }
    
    section("Choose the Virtual Garage Door Device sensor (same as above device)?"){
        input "virtualgdbutton", "capability.contactSensor", title: "Virtual Garage Door Open/Close Sensor?", required: true
    }
    
    section("Timeout before checking if the door opened or closed correctly?"){
        input "checkTimeout", "number", title: "Door Operation Check Timeout?", required: true, defaultValue: 25
    }
    
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }

}

def installed() {
    def realgdstate = sensor.currentContact
    def virtualgdstate = virtualgd.currentContact
    //log.debug "in installed ... current state=  $realgdstate"
    //log.debug "gd state= $virtualgd.currentContact"

    subscribe(sensor, "contact", contactHandler)
    subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate) {
        if (realgdstate == "open") {
            virtualgd.open()
        } else {
            virtualgd.close()
        }
    }
}

def updated() {
    def realgdstate = sensor.currentContact
    def virtualgdstate = virtualgd.currentContact
    //log.debug "in updated ... current state=  $realgdstate"
    //log.debug "in updated ... gd state= $virtualgd.currentContact"


    unsubscribe()
    subscribe(sensor, "contact", contactHandler)
    subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate) {
        if (realgdstate == "open") {
            log.debug "opening virtual door"
            mysend("91 Virtual Garage Door Opened!")     
            virtualgd.open()
        } else {
            virtualgd.close()
            log.debug "closing virtual door"
            mysend("96 Virtual Garage Door Closed!")   
        }
    }
}

/**
*  Garage door sensor state changed, sync to the virtual garage door state
*/
def contactHandler(evt) {
    def virtualgdstate = virtualgd.currentContact
    // how to determine which contact
    //log.debug "in contact handler for actual door open/close event. event = $evt"

    if("open" == evt.value) {
        // contact was opened, turn on a light maybe?
        log.debug "Contact is in ${evt.value} state"
        // reset virtual door if necessary
        if (virtualgdstate != "open") {
            //mysend("114 Garage Door Opened Manually syncing with Virtual Garage Door!")   
            virtualgd.open()
        }
        schedule("0 * * * * ?", "doorOpenCheck")
    } else if("closed" == evt.value) {
        // contact was closed, turn off the light?
        log.debug "Contact is in ${evt.value} state"
        //reset virtual door
        if (virtualgdstate != "closed") {
            //mysend("123 Garage Door Closed Manually syncing with Virtual Garage Door!")   
            virtualgd.close()
        }
        if (state.openDoorNotificationSent) {
            mysend("Garage door is now closed")
            state.openDoorNotificationSent = false
        }
        unschedule("doorOpenCheck")
    }
}

def doorOpenCheck() {
    final thresholdMinutes = openThreshold
    if (thresholdMinutes) {
        def currentState = sensor.contactState
        log.debug "doorOpenCheck"
        if (currentState?.value == "open") {
            log.debug "open for ${now() - currentState.date.time}, openDoorNotificationSent: ${state.openDoorNotificationSent}"
            if (!state.openDoorNotificationSent && now() - currentState.date.time > thresholdMinutes * 60 * 1000) {
                def msg = "Garage has been open for ${thresholdMinutes} minutes"
                log.info msg

                mysend(msg)
                state.openDoorNotificationSent = true
            }
        } else {
            state.openDoorNotificationSent = false
        }
    }
}

/**
*  Virtual Garage Door Switch changed, open/close the actual garage door if state doesn't match
*  Also sets a timeout to verify that the open/close actually happened
*/
def virtualgdcontactHandler(evt) {
    // how to determine which contact
    def realgdstate = sensor.currentContact
    //log.debug "in virtual gd contact/button handler event = $evt"
    //log.debug "in virtualgd contact handler check timeout = $checkTimeout"

    if ("open" == evt.value) {
        log.debug "Contact is in ${evt.value} state"
        if (realgdstate != "open") {
            log.debug "opening real gd to correspond with button press"
            mysend("143 Virtual Garage Door Opened syncing with Actual Garage Door!")   
            opener.on()
            runIn(checkTimeout, checkIfActuallyOpened)
        }
    }else if ("closed" == evt.value) {
        log.debug "Contact is in ${evt.value} state"
        if (realgdstate != "closed") {
            log.debug "closing real gd to correspond with button press"
            mysend("152 Virtual Garage Door Closed syncing with Actual Garage Door!")   
            opener.on()
            runIn(checkTimeout, checkIfActuallyClosed)
        }
    }
}

private mysend(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    } else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}

/**
*  Called after the virtual switch is closed to verify that the actual garage has closed and tries a second time
*/
def checkIfActuallyClosed() { 
    def realgdstate = sensor.currentContact
    def virtualgdstate = virtualgd.currentContact
    //log.debug "in checkifopen ... current state=  $realgdstate"
    //log.debug "in checkifopen ... gd state= $virtualgd.currentContact"


    // sync them up if need be set virtual same as actual
    if (realgdstate == "open" && virtualgdstate == "closed") {
        log.debug "opening virtual door as it didnt close.. beam probably crossed"
        mysend("Resetting Virtual Garage Door to Open as real door didn't close (beam probably crossed)!")   
        virtualgd.open()
    }   
}



def checkIfActuallyOpened() {
    def realgdstate = sensor.currentContact
    def virtualgdstate = virtualgd.currentContact
    //log.debug "in checkifopen ... current state=  $realgdstate"
    //log.debug "in checkifopen ... gd state= $virtualgd.currentContact"


    // sync them up if need be set virtual same as actual
    if (realgdstate == "closed" && virtualgdstate == "open") {
        log.debug "opening virtual door as it didnt open.. track blocked?"
        mysend("208 Resetting Virtual Garage Door to Closed as real door didn't open! (track blocked?)")
        virtualgd.close()
    }   
}