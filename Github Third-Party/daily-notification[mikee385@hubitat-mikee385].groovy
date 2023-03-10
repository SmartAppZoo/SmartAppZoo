/**
 *  Daily Notification
 *
 *  Copyright 2020 Michael Pierce
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
 
String getVersionNum() { return "1.1.0" }
String getVersionLabel() { return "Daily Notification, version ${getVersionNum()} on ${getPlatform()}" }

def getDaysOfWeek() { ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"] }

definition(
    name: "Daily Notification",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Sends a notification every day at a specific time.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/apps/daily-notification.groovy")

preferences {
    page(name: "settings", title: "Daily Notification", install: true, uninstall: true) {
        section {
            input "notifier", "capability.notification", title: "Notification Device", multiple: false, required: true
            input "message", "text", title: "Message Text", multiple: false, required: true
        }
        section {
            input "timeToNotify", "time", title: "Time", required: true
        }
        section {
            input "daysToNotify", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: daysOfWeek
            input "people", "capability.presenceSensor", title: "Only when present", multiple: false, required: false
        }
        section {
            label title: "Assign a name", required: true
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    def daysFilter = '*'
    if (daysToNotify) {
        daysFilter = daysToNotify.collect { (daysOfWeek.indexOf(it)+1).toString() }.join(",")
    }
    
    def timeToNotifyToday = timeToday(timeToNotify)
    def currentTime = new Date()
    schedule("$currentTime.seconds $timeToNotifyToday.minutes $timeToNotifyToday.hours ? * $daysFilter *", sendMessage)
}

def sendMessage() {
    if (people.currentValue("presence") == "present") {
        notifier.deviceNotification(message)
    }
}