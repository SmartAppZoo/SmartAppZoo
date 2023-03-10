/**
* Copyright 2016 McIlroy
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Reminder at or near Sunset.  Example offset for 30 minutes before sunset: -30
*
* Based on https://github.com/SmartThingsCommunity/Code/blob/master/smartapps/sunrise-sunset/turn-on-before-sunset.groovy
*
* This version includes improvements to the above:
*   1) Proper handlng of post-sunset reminders by using two alternating event handlers, avoiding the
*   collision on the runOnce scheduler
*   2) If installing/updating after today's sunset, schedule tomorrow's in addition to today's
*   3) Use event's dateValue() to avoid parsing a date string
*
* Author: McIlroyC
*
* Date: 2016-02-12
*/

definition(
name: "Sunset Reminder",
    namespace: "mcilroyc",
    author: "Cory McIlroy",
    description: "Send push notification at/near sunset.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png"
)

preferences {
    section("Sunset offset (optional)...") {
        input "sunsetOffsetMinutes", "number", title: "Offset in minutes (negative means before sunset)",
            required: true, defaultValue: 10
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
    // listen for sunsetTime so we can schedule tomorrow's sunset
    subscribe(location, "sunsetTime", sunsetTimeHandler)

    // init stateful A/B tracker
    state.toggle = false

    //schedule reminder for today
    def currentSunset = getSunriseAndSunset().sunset
    scheduleReminder(currentSunset)

    // If we've missed todays sunset, then we need to schedule tomorrow's as well
    def now = new Date()
    if (now > currentSunset) {
        def tomorrowSunset = getSunriseAndSunset(date: now + 1).sunset
        scheduleReminder(tomorrowSunset)
    }
}

def sunsetTimeHandler(evt) {
    // At sunset, schedule the reminder for tomorrow
    scheduleReminder(evt.dateValue)
}

def scheduleReminder(sunsetDate) {
    //calculate the offset
    log.debug "Raw reminder time: ${sunsetDate}"
    log.debug "offset: ${sunsetOffsetMinutes}"
    log.debug "toggle: ${state.toggle}"
    
    if (!(sunsetDate instanceof Date)) {
        def m = "Sunset Reminder could not schedule a reminder: bad date provided"
        debugEvent(m)
        sendPush(m)
        return
    }
    
    def sunsetOffsetTime = new Date(sunsetDate.time + (sunsetOffsetMinutes * 60 * 1000))

    debugEvent("Scheduling sunset reminder for: $sunsetOffsetTime (sunset is $sunsetDate)")

    //schedule this to run one time
    if (state.toggle) {
        runOnce(sunsetOffsetTime, reminderAlt)
    } else {
        runOnce(sunsetOffsetTime, reminder)
    }
    state.toggle = !state.toggle
}

// send a push reminder
def reminder() {
    debugEvent("sending push message for sunset reminder")
    sendPush("Sunset!")
}

// A second schedulable reminder, so we can have two scheduled at the same time
def reminderAlt() {
    reminder()
}

def debugEvent (msg) {
    log.info(msg)
    sendEvent(descriptionText:"$sunset reminder: ${msg}", eventType:"SOLUTION_EVENT", displayed: true)
}
