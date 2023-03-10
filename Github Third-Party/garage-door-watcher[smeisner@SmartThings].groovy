/**
 *  Garage Door Watcher
 *
 *  Author: Steve Meisner
 *
 *  Date: 2016-09-19
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: "Garage Watcher",
    namespace: "meisners.net",
    author: "Steve Meisner",
    description: "If garage door left open, begin telling user after sunset. After notifications, automatically close door.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
    section("Choose garage door to watch") {
        input "doorContact", "capability.contactSensor", title: "Which Sensor?"
        input "theDoor", "capability.doorControl", title: "Which Door?"
    }
    section ("Sunset offset (optional)") {
        input "sunsetOffsetValue", "text", title: "HH:MM", required: false
        input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
    }
    section ("Zip code (optional, defaults to location coordinates)") {
        input "zipCode", "text", title: "Zip Code", required: false
    }
    section( "Notifications" ) {
        input "message", "text", title: "Push message to send:", required: false
        input "smsPhone", "phone", title: "Phone number to notify via SMS", required: false
        input "smsMessage", "text", title: "SMS message to send:", required: false
    }
    section ("Frequency of notifications") {
        input "notificationDelay", "number", title: "Delay between notifications", required: true
        input "notificationCount", "number", title: "How many notifications prior to door close?", required: true
    }
}

def installed() {
    log.trace "Entering installed()"
    initialize()
    log.info "Running astroCheck() once"
    astroCheck()
    log.info "Running doorChecker() once"
    doorChecker()
    subscribe(doorContact, "contact", doorChanged)
    subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)
}

def updated() {
    log.trace "Entering updated()"
}

def initialize() {
    log.trace "Entering initialize()"
    state.NotificationCount = 0
    scheduleAstroCheck()
}

def sunsetHandler(evt) {
    log.debug "Sun has set!"
    state.isNightTime = true
}

def sunriseHandler(evt) {
    log.debug "Sun has risen!"
    state.isNightTime = false
}

def scheduleAstroCheck() {
    log.trace "Entering scheduleAstroCheck()"
    def min = Math.round(Math.floor(Math.random() * 60))
    def exp = "0 $min * * * ?"
    log.trace "scheduleAstroCheck: $exp"
    unschedule (astroCheck)
    schedule(exp, astroCheck) // check every hour since location can change without event?
}

def astroCheck() {
    log.trace "Entering astroCheck()"
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
    def sunsetTime = s.sunset
    def sunriseTime = s.sunrise
    def now = new Date()

    if (state.sunsetTime != sunsetTime.time) {
        //
        // If the above is true, we have changed over the day.
        // Now "sunset" is referring to the next day sunset. So
        // we need to update it. Chances are, it's just past midnight.
        //
        state.sunsetTime = sunsetTime.time

        //
        // Are we in the "notify and defer" mode? If so (where notification
        // count is > 0), do not unschedule doorchecker.
        //
        if (state.NotificationCount == 0) {
            log.trace "Descheduling door checker (NotificationCount = 0)"
            unschedule("doorChecker")
        }

        //
        // If sunset time is after now (we haven't hit sunset yet)...
        //
        if (sunsetTime.after(now)) {
            log.trace "Scheduling sunset handler to run at $sunsetTime"
            runOnce(sunsetTime, doorChecker)
        }
    }

    //
    // Update the 'daytime' flag. Check against the updated
    // sunrise and sunset times. This test is really needed
    // as a starting point for the 'isNightTime' flag. Otherwise,
    // the sunrise & sunset callbacks will be used.
    //
    if (now.time < sunsetTime.time && now.time > sunriseTime.time) {
        state.isNightTime = false
    } else {
        state.isNightTime = true
    }
    log.info "Current isNightTime = $state.isNightTime"
}

def scheduleDoorChecker() {
    log.trace "Entering scheduleDoorChecker()"
    def exp = now() + (60000 * notificationDelay)
    log.info "schedule Door Check time: $exp"
    unschedule("doorChecker")
    schedule(exp, doorChecker)
}

def doorChecker() {
    //
    // Sunset just occured or the door was opened
    //
    log.trace "Entering doorChecker()"
    def now = new Date()
    //
    // Only check the door during nighttime
    //
    if (state.isNightTime) {
        log.info "After sunset; Checking garage door"
        def Gdoor = checkGarage()
        log.info "Door is $Gdoor"
        if (Gdoor == "open") {
            state.NotificationCount = state.NotificationCount + 1
            if (state.NotificationCount > notificationCount) {
                log.info "Notified enough times. Closing door"
                // Avoid the door change callback getting called
                // from us closign the door.
                unsubscribe(doorContact)
                state.NotificationCount = 0
                def devLabel = theDoor.displayName
                log.debug "Closing door now: $devLabel"
                // Notify the user we are closing the door
                send ("Closing door now: $devLabel")
                // and do it!
                theDoor.close()
                // Stop calling this routine to check the door. It's closed!
                unschedule("doorChecker")
                // Now reenable the door change callback
                subscribe(doorContact, "contact", doorChanged)
            } else {
                // Just send the notification
                log.debug "Notifying user and deferring"
                send(message)
                // Schedule another callback
                scheduleDoorChecker()
            }
        } else {
            // Someone (other than us) closed the door. Stop monitoring it.
            log.info "Door is now closed - stop monitoring door"
            state.NotificationCount = 0
            unschedule("doorChecker")
        }
    } else {
        // It's daytime...stop checking the door. The sunset handler will
        // start the notifications again if the door's still open.
        log.trace "It's daytime!! Stop checking door"
        unschedule("doorChecker")
    }
}

def doorChanged(evt) {
    // Callback when door opened or closed
    log.trace "Entering doorChanged($evt.value)"
    if (evt.value == "open") {
        log.info "Door opened"
        if (state.isNightTime) {
            log.info "Between sunset and sunrise; scheduling watcher"
            scheduleDoorChecker()
        }
    } else {
        log.trace "Door closed -- descheduling callbacks"
        state.NotificationCount = 0
        unschedule ("doorChecker")
    }
}

private send(msg) {
    if (message != null) {
        if (msg) {
            log.info ("Sending push message: $msg")
            sendPush(msg)
        } else {
            log.info ("Sending push message: $message")
            sendPush(message)
        }
    }
    if (smsMessage != null) {
        if (msg) {
            log.info ("Sending SMS to $smsPhone : $msg")
            sendSmsMessage (smsPhone, msg)
        } else {
            log.info ("Sending SMS to $smsPhone : $smsMessage")
            sendSmsMessage (smsPhone, smsMessage)
        }
    }
}

def checkGarage(evt) {
    def latestValue = doorContact.currentContact
}

private getSunsetOffset() {
    sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
