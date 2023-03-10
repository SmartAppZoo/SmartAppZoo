/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
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
  name: "Door Lock Auto Lock",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Automatically locks a door after X minutes with an optional contact sensor.",
  category: "Safety & Security",
  iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
  iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg"
)

preferences {
  section("Devices") {
    input "lock", "capability.lock", title: "Door Lock", required: true
    input "contact", "capability.contactSensor", title: "Contact Sensor", required: false
  }
  section("Automatically lock the door...") {
    input "lockAfterMinutes", "number", title: "after X minutes:", required: true, defaultValue: "10"
    input "confirmLockAfterSeconds", "number", title: "confirm lock after X seconds:", required: true, defaultValue: "10"
  }
  section( "Notifications" ) {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
    input "sendTextMessage", "phone", title: "Phone number for text notifications.", required: false
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
  subscribe(lock, "lock", doorHandler, [filterEvents: false])
  subscribe(lock, "unlock", doorHandler, [filterEvents: false])
}

def sendMessage(message) {
  if (sendPushMessage == "Yes")
    sendPush(message)
  if (sendTextMessage && sendTextMessage != "" && sendTextMessage != "0")
    sendSms(sendTextMessage, message)
}

def checkLockDoor() {
  String lockState = lock.latestValue("lock")
  String contactState = contact ? contact.latestValue("contact") : "closed"
  if (lockState == "locked" && contactState == "closed")
    sendMessage("Success, ${lock} was auto-locked")
  else if (lockState == "locked" && contactState == "open")
    sendMessage("Warning, ${lock} was auto-locked with the door open")
  else
    sendMessage("Warning, ${lock} failed to auto-lock (door is ${contactState})")
}

def lockDoor() {
  String lockState = lock.latestValue("lock")
  String contactState = contact ? contact.latestValue("contact") : "closed"
  if (contactState == "open") {
    sendMessage("Warning, ${contact} is ${contactState} and ${lock} is ${lockState}")
  } else if (lockState == "unlocked" && contactState == "closed") {
    lock.lock()
    if (confirmLockAfterSeconds > 0)
      runIn(confirmLockAfterSeconds, checkLockDoor)
    else
      sendMessage("Success, ${lock} was auto-locked")
  }
}

def doorHandler(evt) {
  if (evt.value == "locked" && (!contact || contact.latestValue("contact") == "closed")) {
    unschedule()
  } else if (evt.value == "unlocked") {
    runIn((lockAfterMinutes * 60), lockDoor)
  }
}

