/**
 * Better Auto-lock
 *
 * Author: Spiros Papadimitriou
 *
 * Based on "Enhanced Auto Lock" by user "Arnaud":
 *   https://github.com/SmartThingsCommunity/SmartThingsPublic/smartapps/lock-auto-super-enhanced
 * The main differences try to make it more secure, particularly by trying
 * to address problems that may occur due to the lock jamming because the
 * door was not fully closed: (i) removes functionality to unlock an open door,
 * and (ii) tries to verify that door was succesfully locked and alert
 * users if something seems out of order!
 *
 * This file is released under the MIT License:
 * https://opensource.org/licenses/MIT
 *
 * This software is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied.
 */


definition(
  name: "Better Auto Lock Door",
  namespace: "spapadim",
  author: "Spiros Papadimitriou",
  description: "Automatically locks a specific door after X minutes when closed and checks for problems.",
  category: "Safety & Security",
  iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
  iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
  pausable: true
)

preferences{
  page name: "mainPage", install: true, uninstall: true
}

def mainPage() {
  dynamicPage(name: "mainPage") {
    section("Select the door lock:") {
      input "thelock", "capability.lock", required: true
    }
    section("Select the door contact sensor:") {
      input "contact", "capability.contactSensor", required: true
    }
    section("Automatically lock the door when closed...") {
      input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section("Verify door succesfully locked") {
      input "notifyError", "bool", title: "Verify lock and notify when problems occur", defaultValue: true
      input "verifyDelay", "number", title: "Verification check delay (in seconds)", defaultValue: 60
    }
    if (location.contactBookEnabled || phoneNumber) {
      section("Action notifications") {
        input("recipients", "contact", title: "Send notifications to", required: false) {
          input "phoneNumber", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
      }
    }
    section([mobileOnly:true]) {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)"
    }
  }
}

def installed(){
  initialize()
}

def updated(){
  unsubscribe()
  unschedule()
  initialize()
}

def initialize(){
  log.debug "Settings: ${settings}"
  subscribe(thelock, "lock", doorHandler, [filterEvents: false])
  subscribe(thelock, "unlock", doorHandler, [filterEvents: false])  
  subscribe(contact, "contact.open", doorHandler)
  subscribe(contact, "contact.closed", doorHandler)
}

def lockDoor() {
  log.debug "Locking the door."
  if (thelock.latestValue("lock") != "unlocked") {  // Extra paranoia sanity/safety check -- spapadim
    log.debug "OOOPS: Door is not in unlocked state, aborting lock!"
    if (notifyError) {
      sendPush("[DANGER] ${thelock} was not unlocked; lock attempt aborted!")
    }
    return
  }
  thelock.lock()
  if (location.contactBookEnabled && recipients) {
    log.debug "Sending action notifications..."
    sendNotificationToContacts("${thelock} locked after ${contact} was closed for ${minutesLater} minutes!", recipients)
  }
  if (phoneNumber) {
    log.debug "Sending action text message..."
    sendSms(phoneNumber, "${thelock} locked after ${contact} was closed for ${minutesLater} minutes!")
  }
  unschedule(verifyLocked)  // TODO -- RTFM on runIn() and remove if redundant - spapadim
  runIn(verifyDelay, verifyLocked)
}

def verifyLocked() {
  if (thelock.latestValue("lock") != "locked") {  // not "locked" -- latch may have jammed
    log.debug "DANGER: Something went wrong...!"
    if (notifyError) {
      log.debug "Sending push notifications about failure to lock"
      sendPush("[DANGER] ${thelock} still unlocked, despite attempt to lock!")
    }
  } else if (contact.latestValue("contact") != "closed") {  // it is "locked" but not "closed" -- latch may have forced door open!
    log.debug "DANGER: Something went *very* wrong...!"
    if (notifyError) {
      log.debug "Sending push notification about lock attempt opening door"
      sendPush("[*DANGER*] Door opened by attempt to lock ${thelock}!")
    }
  }
}

def doorHandler(evt){
  if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
    unschedule(lockDoor) // ...we don't need to lock it later.
  } else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
    unschedule(verifyLocked)
    runIn(minutesLater*60, lockDoor) 
  } else if ((thelock.latestValue("lock") == "unlocked") && (evt.value == "open")) { // If a person opens an unlocked door...
    unschedule(lockDoor) // ...we don't need to lock it later.
  } else if ((thelock.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
    unschedule(verifyLocked)
    runIn(minutesLater*60, lockDoor)
  }
//
//  else { //Opening or Closing door when locked (in case you have a handle lock)
//    log.debug "Unlocking the door."
//    thelock.unlock()
//    if (location.contactBookEnabled) {
//      if (recipients) {
//        log.debug "Sending Push Notification..."
//        sendNotificationToContacts("${thelock} unlocked after ${contact} was opened or closed when ${thelock} was locked!", recipients)
//      }
//    }
//    if ( phoneNumber ) {
//      log.debug "Sending text message..."
//      sendSms(phoneNumber, "${thelock} unlocked after ${contact} was opened or closed when ${thelock} was locked!")
//    }
//  }
}
