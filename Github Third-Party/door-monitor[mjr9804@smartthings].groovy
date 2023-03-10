/**
 *  Door Monitor
 *
 *  Copyright 2016 Michael Robertson
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
    name: "Door Monitor",
    namespace: "mjr9804",
    author: "Michael Robertson",
    description: "Monitor doors/locks and get an alert when something unexpected happens\n\nRequires locks to use https://github.com/mjr9804/smartthings/tree/master/devicetypes/mjr9804/custom-zwave-lock.src for alarm detection to work correctly.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "pageOne", title: "Monitor these things", nextPage: "pageTwo", install: false, uninstall: true) {
        section() {
            input "doors", "capability.contactSensor", title: "Doors", required: false, multiple: true
            input "locks", "capability.lock", title: "Locks", required: false, multiple: true
        }
    }
    page(name: "pageTwo", title: "Use these emergency contact methods", nextPage: "pageThree", uninstall: true) {
        section () {
            input "pushNotif", "bool", title: "Send a push notification", defaultValue: false, required: false
            input "textNotif1", "bool", title: "Send a text message to Phone #1", defaultValue: false, required: false
            input "phone1", "phone", title: "Phone #1", required: false
            input "textNotif2", "bool", title: "Send a text message to Phone #2", defaultValue: false, required: false
            input "phone2", "phone", title: "Phone #2", required: false
        }
    }
    page(name: "pageThree", title: "When I'm home...", nextPage: "pageFour", uninstall: true) {
        section("If an alarm goes off") {
            input "homeAlarmAlert", "bool", title: "Alert me", defaultValue: false, required: false
            input "homeAlarmClose", "bool", title: "Close all doors", defaultValue: false, required: false
            input "homeAlarmLock", "bool", title: "Lock all doors", defaultValue: false, required: false
        }
    }
    page(name: "pageFour", title: "When I leave...", nextPage: "pageFive", uninstall: true) {
        section("If a door was left open") {
            input "leaveDoorOpenAlert", "bool", title: "Alert me", defaultValue: true, required: false
        }
        section("If a lock was left unlocked") {
            input "leaveLockUnlockedAlert", "bool", title: "Alert me", defaultValue: true, required: false
        }
    }
    page(name: "pageFive", title: "When I'm away...", nextPage: "pageSix", uninstall: true) {
        section("If an alarm goes off") {
            input "awayAlarmAlert", "bool", title: "Alert me", defaultValue: true, required: false
            input "awayAlarmClose", "bool", title: "Close all doors", defaultValue: false, required: false
            input "awayAlarmLock", "bool", title: "Lock all doors", defaultValue: false, required: false
        }
        section("If a door unlocks") {
            input "awayUnlockAlert", "bool", title: "Alert me", defaultValue: true, required: false
            input "awayUnlockDisarm", "bool", title: "Disable the alarm", defaultValue: false, required: false
        }
        section("If a door opens") {
            input "awayOpenAlert", "bool", title: "Alert me", defaultValue: true, required: false
            input "awayOpenDelay", "number", title: "With a delay of (seconds)", defaultValue: 60, required: false
        }
    }
    page(name: "pageSix", title: "When it's night...", install: true, uninstall: true) {
        section("If an alarm goes off") {
            input "nightAlarmAlert", "bool", title: "Alert me", defaultValue: true, required: false
            input "nightAlarmClose", "bool", title: "Close all doors", defaultValue: false, required: false
            input "nightAlarmLock", "bool", title: "Lock all doors", defaultValue: false, required: false
        }
        section("If a door unlocks") {
            input "nightUnlockAlert", "bool", title: "Alert me", defaultValue: true, required: false
        }
        section("If a door opens") {
            input "nightOpenAlert", "bool", title: "Alert me", defaulValuet: true, required: false
        }
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(locks, "tamper", evtHandler)
    subscribe(locks, "lock", evtHandler)
    subscribe(doors, "contact", evtHandler)
    subscribe(location, "mode", evtHandler)
}

def sendAlert(text) {
    if (pushNotif == true) {
        log.debug "Sending push notification"
        sendPush(text)
    }
    if (textNotif1 == true) {
        log.debug "Sending SMS to phone1"
        sendSms(phone1, text)
    }
    if (textNotif2 == true) {
        log.debug "Sending SMS to phone2"
        sendSms(phone2, text)
    }
}

def checkDoorsAndAlert() {
    def openDoors = []
    for (door in settings.doors) {
        log.debug "door ${door}"
        log.debug "state ${door.contactState.value}"
        if (door.contactState.value == "open") {
            openDoors.add(door)
        }
    }
    if (openDoors.size() > 0) {
        log.debug "Doors left open! ${openDoors}"
        sendAlert("Doors left open! ${openDoors}")
    }
}

def checkLocksAndAlert() {
    def unlockedLocks = []
    for (lock in settings.locks) {
        log.debug "lock ${lock}"
        log.debug "state ${lock.lockState.value}"
        if (lock.lockState.value == "unlocked") {
            unlockedLocks.add(lock)
        }
    }
    if (unlockedLocks.size() > 0) {
        log.debug "Locks left unlocked! ${unlockedLocks}"
        sendAlert("Locks left unlocked! ${unlockedLocks}")
    }
}

def delayedModeCheck(data) {
    log.debug "delayedModeCheck has fired"
    location.mode != data.originalMode ?: sendAlert(data.alertText)
}

def takeAction(event, text, name) {
   def currMode = location.mode
   log.debug "current mode is $currMode" // "Home", "Away", "Night"
   switch (currMode) {
       case "Home":
          if (event == "detected" && homeAlarmAlert == true) {
              if (text.contains("keypad temporarily disabled")) {
                  text = "Tamper alarm! "+text
              }
              sendAlert(text)
          }
          
          if (event == "detected" && homeAlarmClose == true) {
              for (door in doors) {
                  for (command in door.supportedCommands) {
                      if ("close" == command.getName()) {
                          log.debug "Alarm detected, closing "+door
                          door.close()
                      }
                  }
              }
          }
          
          if (event == "detected" && homeAlarmLock == true) {
              log.debug "Alarm detected, locking "+locks
              locks.lock()
          }
          
          break
       case "Away":
          if (event == "unlocked" && awayUnlockDisarm == true) {
              locks.setAlarmMode(0x0)
              locks.setAlarmSensitivity(0x0)
          }
          if (event == "unlocked" && awayUnlockAlert == true) {
              sendAlert(text)
          }
          else if (event == "open" && awayOpenAlert == true) {
              runIn(awayOpenDelay, delayedModeCheck, [data: [originalMode: "Away", alertText: text]]);
          }
          else if (event == "detected" && awayAlarmAlert == true) {
              if (text.contains("keypad temporarily disabled")) {
                  text = "Tamper alarm! "+text
              }
              sendAlert(text)
          }
          
          if (event == "detected" && awayAlarmClose == true) {
              for (door in doors) {
                  for (command in door.supportedCommands) {
                      if ("close" == command.getName()) {
                          log.debug "Alarm detected, closing "+door
                          door.close()
                      }
                  }
              }
          }
          
          if (event == "detected" && awayAlarmLock == true) {
              log.debug "Alarm detected, locking "+locks
              locks.lock()
          } 
          
          if (name == "mode" && leaveDoorOpenAlert == true) {
              checkDoorsAndAlert()
          }
          if (name == "mode" && leaveLockUnlockedAlert == true) {
              checkLocksAndAlert()
          }
          
          break
       case "Night":
          if (event == "unlocked" && nightUnlockAlert == true) {
              sendAlert(text)
          }
          else if (event == "open" && nightOpenAlert == true) {
              sendAlert(text)
          }
          else if (event == "detected" && nightAlarmAlert == true) {
              if (text.contains("keypad temporarily disabled")) {
                  text = "Tamper alarm! "+text
              }
              sendAlert(text)
          }
          
          if (event == "detected" && nightAlarmClose == true) {
              for (door in doors) {
                  for (command in door.supportedCommands) {
                      if ("close" == command.getName()) {
                          log.debug "Alarm detected, closing "+door
                          door.close()
                      }
                  }
              }
          }
          
          if (event == "detected" && nightAlarmLock == true) {
              log.debug "Alarm detected, locking "+locks
              locks.lock()
          } 
          
          break
   }
}

def evtHandler(evt) {
  def descText = evt.descriptionText.replaceAll('^\\{\\{ [a-zA-Z_]+ \\}\\}', "${evt.device}") // Use device name for descriptions that have variables
  log.debug "event name: ${evt.name}"
  log.debug "display name: ${displayName}"
  log.debug "desc text: ${descText}"
  log.debug "string value: ${evt.stringValue}"
  log.debug "device: ${evt.device}"
  takeAction(evt.stringValue, descText, evt.name)
}