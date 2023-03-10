/**
 *  Trash Day
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
    name: "Trash Day",
    namespace: "caglar10ur",
    author: "S.Çağlar Onur",
    description: "Remind the trash day",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Trash door to monitor") {
        input "contact", "capability.contactSensor", title: "Sensor to monitor", required: true
    }
    section("On Which Days?") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday"]
    }
    section("Between what times?") {
        input "fromTime", "time", title: "From", required: true
        input "toTime", "time", title: "To", required: true
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
    // unschedule all tasks
    unschedule()
    initialize()
}

def initialize() {
    log.trace "Initialized with settings: ${settings}"

    // schedule a cron jobs for every WED at 7:00PM
    schedule("0 0 19 ? * WED *", firstReminder)

    // whether we have scheduled the second reminder
    state.secondReminderScheduled = false
    // whether we have scheduled the last reminder
    state.lastReminderScheduled = false

    subscribe(contact, "contact.closed", closed)
}

def closed(evt) {
    log.trace "closed(${evt})"

    // Ensure the new date object is set to local time zone
    def df = new java.text.SimpleDateFormat("EEEE")
    df.setTimeZone(location.timeZone)

    //Does the preference input Days, i.e., days-of-week, contain today?
    def today = df.format(new Date())
    if (!days.contains(today)) {
  		log.debug "${today} is not in ${days}"
        
        return
    }

    def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    if (!between) {
		log.debug "it is not between ${fromTime} and ${toTime}"
        
        return
    }

    // unschedule the last reminder if it is scheduled
    if (state.secondReminderScheduled) {
		log.debug "unscheduling second reminder"
        
        unschedule(secondReminder)
        state.secondReminderScheduled = false
    }

    // unschedule the last reminder if it is scheduled
    if (state.lastReminderScheduled) {
		log.debug "unscheduling last reminder"

        unschedule(lastReminder)
        state.lastReminderScheduled = false
    }
}

def firstReminder() {
    log.trace "firstReminder()"

    speak("Master, tomorrow is the day. Don't forget to take out the trash. The best preparation for tomorrow is doing your best today.")
    notify("Tomorrow is the trash day. Don't forget to take out the trash.")

    // schedule the second cron jobs for every WED at 9:00PM
    state.secondReminderScheduled = true
    schedule("0 0 21 ? * WED *", secondReminder)
}

def secondReminder() {
    log.trace "secondReminder()"

    speak("Master, tomorrow is the day. Don't forget to take out the trash")
    notify("Tomorrow is the trash day. Don't forget to take out the trash.")

    // schedule a cron job in every WED at 10:00PM as a last resort
    state.lastReminderScheduled = true
    schedule("0 0 22 ? * WED *", lastReminder)
}

def lastReminder() {
    log.trace "lastReminder()"

    speak("Master, tomorrow is the day. Don't forget to take out the trash. This is your last reminder.")
    notify("Tomorrow is the trash day. Don't forget to take out the trash.")
}

private speak(msg) {
    log.trace "speak(${msg})"

    def sound = textToSpeech(msg)
    if (resumePlaying){
        sonos.playTrackAndResume(sound.uri, volume)
    } else {
        sonos.playTrackAndRestore(sound.uri, volume)
    }
}

private notify(msg) {
    log.trace "notify(${msg})"

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients)
	} else {
		if (phone) {
			sendSms phone, msg
		} else {
			sendPush msg
		}
	}
}
