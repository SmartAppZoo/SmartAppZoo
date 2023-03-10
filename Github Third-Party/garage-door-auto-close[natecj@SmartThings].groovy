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
  name: "Garage Door Auto Close",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Automatically closes an open garage door after X minutes.",
  category: "Safety & Security",
  iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
  iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg"
)

preferences {
  section("Devices") {
    input "door", "capability.switch", title: "Garage Door Switch", required: true
    input "contact", "capability.contactSensor", title: "Contact Sensor", required: true
  }
  section("Automatically close the door...") {
    input "closeAfterMinutes", "number", title: "after X minutes:", required: true, defaultValue: "15"
    input "confirmCloseAfterSeconds", "number", title: "confirm close after X seconds:", required: true, defaultValue: "15"
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
  subscribe(contact, "open", doorHandler, [filterEvents: false])
  subscribe(contact, "closed", doorHandler, [filterEvents: false])
}

def sendMessage(message) {
  if (sendPushMessage == "Yes")
    sendPush(message)
  if (sendTextMessage && sendTextMessage != "" && sendTextMessage != "0")
    sendSms(sendTextMessage, message)
}

def checkCloseDoor() {
  if (contact.latestValue("contact") == "closed")
    sendMessage("Success, ${door} was auto-closed")
  else
    sendMessage("Warning, ${door} failed to auto-close")
}

def closeDoor() {
  if (contact.latestValue("contact") != "closed") {
    door.on()
    if (confirmCloseAfterSeconds > 0)
      runIn(confirmCloseAfterSeconds, checkCloseDoor)
    else
      sendMessage("Success, ${door} was auto-closed")
  }
}

def doorHandler(evt) {
  if (evt.value == "closed") {
    unschedule()
  } else if (evt.value == "open") {
    runIn((closeAfterMinutes * 60), closeDoor)
  }
}

