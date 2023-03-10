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
  name: "Multi Zone Mode Changer",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Change modes based on one or more switches being 'on' in multiple zones.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page name: "pageRoot"
  page name: "pageSwitches"
  page name: "pagePeople"
  page name: "pageModes"
  page name: "pageRoutines"
}

def pageRoot() {
  dynamicPage(name: "pageRoot", install: true, uninstall: true) {
    section {
      href "pageSwitches", title:"Configure Switches", description:"Tap to open"
      href "pagePeople", title:"Configure People", description:"Tap to open"
      href "pageModes", title:"Configure Modes", description:"Tap to open"
      href "pageRoutines", title:"Configure Routines", description:"Tap to open"
    }
    section {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def pageSwitches() {
  dynamicPage(name: "pageSwitches") {
    section("Zones") {
      paragraph "If a device in a given zone is on, then the zone is active"
      input "upstairsSwitches", "capability.switch", title: "Upstairs", multiple: true, required: false
      input "downstairsSwitches", "capability.switch", title: "Downstairs", multiple: true, required: false
    }
    section("Status") {
      paragraph "If set, these switches are turned on/off based on each zone being active"
      input "upstairsStatus", "capability.switch", title: "Upstairs", multiple: false, required: false
      input "downstairsStatus", "capability.switch", title: "Downstairs", multiple: false, required: false
    }
  }
}

def pagePeople() {
  dynamicPage(name: "pagePeople") {
    section("Person 1"){
      input "person1presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
      input "person1sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
    }
    section("Person 2"){
      input "person2presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
      input "person2sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
    }
    section("Person 3"){
      input "person3presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
      input "person3sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
    }
  }
}

def pageModes() {
  dynamicPage(name: "pageModes") {
    section {
      input "modeHome", "mode", title: "All Zones Active (Home)", defaultValue: "Home", required: false
      input "modeAway", "mode", title: "No Zones Active (Away)", defaultValue: "Away", required: false    
      input "modeNight", "mode", title: "Upstairs Active (Night)", defaultValue: "Night", required: false
      input "modeDay", "mode", title: "Downstairs Active (Day)", defaultValue: "Day", required: false
    }
  }
}

def pageRoutines() {
  dynamicPage(name: "pageRoutines") {
    def actions = [""] + location.helloHome?.getPhrases()*.label?.sort()
    section {
      paragraph "Run the specified routine when the mode changes based on the following rules"
      input "routineToNight", "enum", title: "Good Night (* to Night)", options: actions, required: false
      input "routineToAway", "enum", title: "Goodbye (* to Away)", options: actions, required: false
      input "routineFromNight", "enum", title: "Good Morning (Night to *)", options: actions, required: false
      input "routineFromAway", "enum", title: "I'm Back (Away to *)", options: actions, required: false      
    }
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  //unschedule()
  initialize()
  runNow()
}

def initialize() {
  subscribe(upstairsSwitches, "switch", changeHandler)
  subscribe(downstairsSwitches, "switch", changeHandler)
  subscribe(person1presence, "presence", changeHandler)
  subscribe(person1sleep, "presence", changeHandler)
  subscribe(person2presence, "presence", changeHandler)
  subscribe(person2sleep, "presence", changeHandler)
  subscribe(person3presence, "presence", changeHandler)
  subscribe(person3sleep, "presence", changeHandler)
}

def myDebug(message) {
  log.debug message
}

def changeHandler(evt) {
  runNow()
}

def runDelayed() {
  if (state.is_running) {
    unschedule()
    state.is_running = false
  }
  def oldMode = location.mode
  def newMode = getNewMode()
  if (oldMode == newMode) {
    myDebug "runDelayed() - Keep Mode as $oldMode"
    return
  }
  myDebug "runDelayed() - Change Mode from $oldMode to $newMode"
  
  runIn(30, runNow)
  updateStatus()
  state.is_running = true
}

def runNow() {
  def oldMode = location.mode
  def newMode = getNewMode()
  if (oldMode == newMode) {
    state.is_running = false
    myDebug "runNow() - Keep Mode as $oldMode"
    return
  }
  myDebug "runNow() - Change Mode from $oldMode to $newMode"

  setLocationMode(newMode)
  runRoutine(oldMode, newMode)
  updateStatus()
  state.is_running = false
}

def updateStatus() {
  if (upstairsStatus) {
    if (upstairsActive()) {
      upstairsStatus.on()
    } else {
      upstairsStatus.off()
    }
  }
  if (downstairsStatus) {
    if (downstairsActive()) {
      downstairsStatus.on()
    } else {
      downstairsStatus.off()
    }
  }
}

def runRoutine(oldMode, newMode) {
  if (newMode == modeAway) { // * -> Away
    location.helloHome?.execute(routineToAway)
  } else if (newMode == modeNight) { // * -> Night
    location.helloHome?.execute(routineToNight)
  } else { // * -> Day or Home
    if (oldMode == modeAway) { // Away -> *
      location.helloHome?.execute(routineFromAway)
    } else if (oldMode == modeNight) { // Night -> *
      location.helloHome?.execute(routineFromNight)
    }
  }
}

def getNewMode() {
  def newMode = location.mode
  if (upstairsActive() && downstairsActive() && modeHome) {
    newMode = modeHome
  } else if (!upstairsActive() && !downstairsActive() && modeAway) {
    newMode = modeAway
  } else if (upstairsActive() && !downstairsActive() && modeNight) {
    newMode = modeNight
  } else if (!upstairsActive() && downstairsActive() && modeDay) {
    newMode = modeDay
  } else {
  }
  newMode
}

def upstairsActive() {
  def upstairsSwitchesActive = settings.upstairsSwitches.any{ it.currentValue('switch') == 'on' }
  def upstairsPerson1Active = settings.person1sleep.latestValue("presence") == "present"
  def upstairsPerson2Active = settings.person2sleep.latestValue("presence") == "present"
  def upstairsPerson3Active = settings.person3sleep.latestValue("presence") == "present"
  def upstairsActive = upstairsSwitchesActive || upstairsPerson1Active || upstairsPerson2Active || upstairsPerson3Active
  upstairsActive
}

def downstairsActive() {
  def downstairsSwitchesActive = settings.downstairsSwitches.any{ it.currentValue('switch') == 'on' }
  def upstairsPerson1Active = settings.person1sleep.latestValue("presence") == "present"
  def downstairsPerson1Active = !upstairsPerson1Active && (settings.person1presence.latestValue("presence") == "present")
  def upstairsPerson2Active = settings.person2sleep.latestValue("presence") == "present"
  def downstairsPerson2Active = !upstairsPerson2Active && (settings.person2presence.latestValue("presence") == "present")
  def upstairsPerson3Active = settings.person3sleep.latestValue("presence") == "present"
  def downstairsPerson3Active = !upstairsPerson2Active && (settings.person3presence.latestValue("presence") == "present")
  def downstairsActive = downstairsSwitchesActive || downstairsPerson1Active || downstairsPerson2Active || downstairsPerson3Active
  downstairsActive
}
