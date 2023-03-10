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
 *  Left It On
 *
 *  Author: SmartThings
 *  Date: 2013-05-09
 */
definition(
    name: "Left It On",
    namespace: "slushpupie",
    author: "Slushpupie",
    description: "Notifies you when you have left a switch on longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

  section("Monitor this switch") {
    input "theSwitch", "capability.switch"
  }

  section("And notify me if it's on for more than this many minutes (default 10)") {
    input "onThreshold", "number", description: "Number of minutes", required: false
  }

  section("Delay between notifications (default 10 minutes") {
    input "frequency", "number", title: "Number of minutes", description: "", required: false
  }

  section("Via text message at this number (or via push notification if not specified") {
    input("recipients", "contact", title: "Send notifications to") {
      input "phone", "phone", title: "Phone number (optional)", required: false
    }
  }
}

def installed() {
  log.trace "installed()"
  subscribe()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  subscribe()
}

def subscribe() {
  subscribe(theSwitch, "switch.on", switchOn)
  subscribe(theSwitch, "switch.closed", switchOff)
}

def switchOn(evt) {
  log.trace "switchOn($evt.name: $evt.value)"
  def delay = (onThreshold != null && onThreshold != "") ? onThreshold * 60 : 600
  runIn(delay, switchOnTooLong, [overwrite: true])
}

def switchOff(evt) {
  log.trace "switchOff($evt.name: $evt.value)"
  unschedule(switchOnTooLong)
}

def switchOnTooLong() {
  def switchState = theSwitch.currentState("switch")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (switchState.value == "on") {
    def elapsed = now() - switchState.rawDateCreated.time
    def threshold = ((onThreshold != null && onThreshold != "") ? onThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Switch has stayed on long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage()
      runIn(freq, switchOnTooLong, [overwrite: false])
    } else {
      log.debug "Switch has not stayed on long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "switchOnTooLong() called but switch is closed:  doing nothing"
  }
}

void sendMessage() {
  def minutes = (onThreshold != null && onThreshold != "") ? onThreshold : 10
  def msg = "${theSwitch.displayName} has been left on for ${minutes} minutes."
  log.info msg
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