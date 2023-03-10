/**
 *  Turn It On When I'm Here
 *
 *  Author: SmartThings
 */
preferences {
  section("When the car arrives") {
    input "carPresence", "capability.presenceSensor", multiple: false
  }
  section("Check the battery") {
    input "carBattery", "device.carwings", multiple: false
  }
}

def installed() {
  subscribe(carPresence, "presence", presenceHandler)
}

def updated() {
  unsubscribe()
  subscribe(carPresence, "presence", presenceHandler)
}

def batteryLevel() {
  carBattery.currentValue('battery')
}

def batteryLow() {
  batteryLevel() < 50
}

def charging() {
  carBattery.currentValue('charging') != "NOT_CHARGING"
}

def present() {
  carPresence.find{it.currentPresence == "present"}
}

def checkBattery() {
  //def tenMinutesAgo = (new Date()).time - 1000 * 60 * 10
  //if (state.leftAt == null || state.leftAt < tenMinutesAgo) {
    log.debug "The car is home!"
    carBattery.requestUpdate()
    runIn(5 * 60, "pollAndNotify")
  //}
}

def pollAndNotify() {
  carBattery.poll()
  runIn(60, "notifyIfLowBattery")
}

def notifyIfLowBattery() {
  log.debug batteryLevel()
  log.debug charging()
  if (batteryLow() && !charging()) {
    sendPush('The ' + carBattery.name + ' is at ' + batteryLevel() + '%')
  }
}

def presenceHandler(evt) {
  if(present()) {
    log.debug "Checking battery in 5 minuttes"
    runIn(5 * 60, "checkBattery")
  } else {
    log.debug "The car left"
    state.leftAt = (new Date()).time
  }
}
