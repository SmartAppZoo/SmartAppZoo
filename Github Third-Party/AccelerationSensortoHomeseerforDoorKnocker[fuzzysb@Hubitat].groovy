/**
 *  Door Knocker
 *
 *  Author: stuart@broadbandtap.co.uk
 *  Date: 05/12/15
 *
 *  Let me know when someone knocks on the door, but ignore
 *  when someone is opening the door.
 */

definition(
    name: "Door Knocker",
    namespace: "fuzzysb	",
    author: "Stuart Buchanan",
    description: "Alert if door is knocked, but not opened.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("When Someone Knocks?") {
    input name: "knockSensor", type: "capability.accelerationSensor", title: "Where?"
  }

  section("But not when they open this door?") {
    input name: "openSensor", type: "capability.contactSensor", title: "Where?"
  }

  section("Knock Delay (defaults to 5s)?") {
    input name: "knockDelay", type: "number", title: "How Long?", required: false
  }

  section("Minimum time between messages (optional, defaults to every message)") {
    input "frequency", "number", title: "Minutes", required: false
  }
  
  section("Virtual Switch to Turn On") {
    input "vSwitch", "capability.switch", title: "Switch Device", required: true
  }
 
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  init()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  init()
}

def init() {
  state.lastClosed = 0
  subscribe(knockSensor, "acceleration.active", handleEvent)
  subscribe(openSensor, "contact.closed", doorClosed)
}

def doorClosed(evt) {
  state.lastClosed = now()
}


def handleEvent(evt) {
  def delay = knockDelay ?: 5
  runIn(delay, "doorKnock")
}

def doorKnock() {
  def frequency = frequency ?: 1
  log.debug "entering doorknock method, frequency is $frequency "
  if((openSensor.latestValue("contact") == "closed") && (now() - (60 * 1000) > state.lastClosed)) {
    log.debug("${knockSensor.label ?: knockSensor.name} detected a knock.")
    vSwitch.on()
    log.debug "end of doorknock method"
  }
  else {
    log.debug("${knockSensor.label ?: knockSensor.name} knocked, but looks like it was just someone opening the door.")
  }
}