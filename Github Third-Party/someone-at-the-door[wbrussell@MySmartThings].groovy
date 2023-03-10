/**
 *  Someone is at the door
 *
 *  Based on Door Knocker by brian@bevey.org
 *
 *  Let me know when someone comes to the door, but ignore
 *  when someone is opening the door or has exited the door.
 *  v1.0
 */

definition(
    name: "Someone At The Door",
    namespace: "wbrussell",
    author: "Brian Russell",
    description: "Alert if someone approaches door, but door is not opened or exited.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("When Someone Approaches Door?") {
    input name: "motionSensor", type: "capability.motionSensor", title: "Where?"
  }

  section("Door to monitor?") {
    input name: "openSensor", type: "capability.contactSensor", title: "Where?"
  }

  section("Motion Delay (defaults to 5s)?") {
    input name: "motionDelay", type: "number", title: "How Long?", required: false
  }

  section("Door Open Exit Delay (defaults to 30s)?") {
    input name: "exitDelay", type: "number", title: "Delay on exiting door?", required: false
  }
  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  state.lastClosed = 0
  state.lastOpened = 0
  state.inhibitMotion = false
  subscribe(motionSensor, "motion.active", handleMotionEvent)
  subscribe(openSensor, "contact.closed", doorClosed)
  subscribe(openSensor, "contact.open", doorOpened)
}

def doorClosed(evt) {
  state.lastClosed = now()
}

def doorOpened(evt) {
  state.lastOpened = now()
  state.inhibitMotion = true
  def delay = exitDelay ?: 30
  runIn(delay, "allowMotion")
  log.debug("inhibit = ${state.inhibitMotion}, lastopen = ${state.lastOpened}")
}

def allowMotion()
{
  state.inhibitMotion = false
  log.debug("inhibit = ${state.inhibitMotion}")
}

def doorMotion() {
  if((openSensor.latestValue("contact") == "closed") &&
     (now() - (60 * 1000) > state.lastClosed) && !state.inhibitMotion) 
     {
    log.debug("${motionSensor.label ?: motionSensor.name} detected motion.")
    send("(${location.name}) Someone is at the door - ${openSensor.label ?: openSensor.name}.")
  }

  else 
  {
    if(!state.inhibitMotion) 
     {
        log.debug("${motionSensor.label ?: motionSensor.name} detected motion, but looks like it was just someone entering the door.")
     }
     else
     {
        log.debug("${motionSensor.label ?: motionSensor.name} detected motion, but it was inhibited by an exit.")
     }
  }
}

def handleMotionEvent(evt) {
  def delay = motionDelay ?: 5
  if (!state.inhibitMotion)
  {
    runIn(delay, "doorMotion")
  }
  else
  {
    log.debug("motion was inhibited by door opening.")
  }
}

private send(msg) {
  if(sendPushMessage != "No") {
    log.debug("Sending push message")
    sendPush(msg)
  }

  if(phone) {
    log.debug("Sending text message")
    sendSms(phone, msg)
  }

  log.debug(msg)
}