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
 *  Left It Open-MultiContacts (from original code of Left it Open)
 *
 *  Author: SmartThings and Modified by Mariano Colmenarejo 2021-02-08
 *  Date: 2013-05-09
 */
definition(
    name: "Left It Open-MultiContacts",
    namespace: "smartthings",
    author: "SmartThings Mod By MCC",
    description: "Notifies you when you have left one or various door or window open longer that a specified amount of time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

  section("Monitor these doors or windows") {
    input(name: "contacts", type: "capability.contactSensor", title: "Devices to Control", description: null, multiple: true, required: true, submitOnChange: true)
  }

  section("And notify me if it's open for more than this many minutes (default 10)") {
    input "openThreshold", "number", description: "Number of minutes", required: false
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
  atomicState.runCounter = 0
  subscribe(contacts, "contact.open", doorOpen)
  subscribe(contacts, "contact.closed", doorClosed)
}

//*** Wait for contact open event openThreshold ***
def doorOpen(evt) {
  for (int i = 0; i < contacts.size(); i++) {
   log.debug "${contacts[i].displayName}= ${contacts[i].currentState("contact").value}"
  }  
  def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
  runIn(delay, doorOpenTooLong, [overwrite: true])
}

//*** Check if all contacts are closed ***
def doorClosed(evt) {
  def numContactsClosed = 0
  for (int i = 0; i < contacts.size(); i++) {
  log.debug "${contacts[i].displayName}= ${contacts[i].currentState("contact").value}"
   if (contacts[i].currentState("contact").value == "closed") {
    numContactsClosed = numContactsClosed + 1
   }
  } 
   if (numContactsClosed == contacts.size() && atomicState.runCounter > 0) {
    def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
    def msg = "All Selected Contacts Sensors are Closed Now. Time Warning Restart to: ${minutes} minutes."
    log.info msg
  if (location.contactBookEnabled) {
    sendNotificationToContacts(msg, recipients)
  } else {
    if (phone) {
      sendSms phone, msg
    } else {
      sendPush msg
    }
     unschedule(doorOpenTooLong)
     atomicState.runCounter = 0
   }  
 }
}

//*** Check if every contact is open toolong ***
def doorOpenTooLong() {
 for (int i = 0; i < contacts.size(); i++) {
  log.debug "${contacts[i].displayName}= ${contacts[i].currentState("contact").value}"
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600
  def contactState = contacts[i].currentState("contact")

  if (contactState.value == "open") {
    def elapsed = now() - contactState.rawDateCreated.time
    def threshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= threshold) {
      log.debug "Contact has stayed open long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage(i,elapsed)
      atomicState.runCounter = atomicState.runCounter +1
      runIn(freq, doorOpenTooLong, [overwrite: false])
    } else {
      log.debug "Contact has not stayed open long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "doorOpenTooLong() called but contact is closed:  doing nothing"
   }
 }
}

//*** Send message for each contact open toolong ***
void sendMessage(i,elapsed) {
   def minutes = new BigDecimal((elapsed + 1000) / 60000).setScale(1, BigDecimal.ROUND_HALF_UP)
   def msg = "${contacts[i].displayName} has been left open for ${minutes} minutes."
  //def msg = "${contacts[i].displayName}: Lleva ${minutes} minutos Abierta."

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
