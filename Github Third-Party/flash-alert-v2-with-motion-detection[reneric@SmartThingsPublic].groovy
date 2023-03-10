/**
 *  Flash Alert V2 - With Motion Detection
 *
 *  Author: reneric
 *  Date: 2016-01-10
 */
definition(
    name: "Flash Alert V2 - With Motion Detection",
    namespace: "reneric",
    author: "Ren Eric Simmons",
    description: "Flashes a set of lights at set times or when motion is detected.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-contact@2x.png"
)

preferences {
  section("When these motion sensors detect motion..."){
    input "motion", "capability.motionSensor", title: "Motion Sensor?", required: false
  }
  section("Or at these times...") {
    input "time1", "time", title: "When?", required: false
    input "time2", "time", title: "When?", required: false
  }
  section("On these days...") {
    input "everyDay", "bool", title: "Everyday?", required: false
    input("days", "enum", options: [
                "1":"Sunday",
                "2":"Monday",
                "3": "Tuesday",
                "4": "Wednesday",
                "5": "Thursday",
                "6": "Friday",
                "7": "Saturday"], required: false, multiple: true)
  }
  section("Then flash..."){
    input "switches", "capability.switch", title: "These lights", multiple: true
    input "numFlashes", "number", title: "This number of times (default 3)", required: false
  }
  section("Time settings in milliseconds (optional)..."){
    input "onFor", "number", title: "On for (default 1000)", required: false
    input "offFor", "number", title: "Off for (default 1000)", required: false
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  subscribe()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unschedule()
  unsubscribe()
  subscribe()
}

def subscribe() {
  if (motion) {
    subscribe(motion, "motion.active", motionActiveHandler)
  }
  if (time1) {
    schedule(time1, scheduleHandler)
  }
  if (time2) {
    schedule(time2, scheduleHandler)
  }
}

def scheduleHandler() {
  Calendar date = Calendar.getInstance()
  int dayOfTheWeek = date.get(Calendar.DAY_OF_WEEK)
  log.debug "Calendar day: ${Calendar.DAY_OF_WEEK}"
  log.debug "Is one of selected days: ${days.contains(Calendar.DAY_OF_WEEK.toString())}"
  if(everyDay == true || days.contains(Calendar.DAY_OF_WEEK.toString())) {
    log.debug "scheduled time to flash lights"
    flashLights()
  }
}
def motionActiveHandler(evt) {
  log.debug "motion $evt.value"
  if(everyDay == true || days.contains(Calendar.DAY_OF_WEEK.toString())) {
    flashLights()
  }
}

private flashLights() {
  def onFor = onFor ?: 1000
  def offFor = offFor ?: 1000
  def numFlashes = numFlashes ?: 3
  def doFlash = true
  log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
  if (state.lastActivated) {
    def elapsed = now() - state.lastActivated
    def sequenceTime = (numFlashes + 1) * (onFor + offFor)
    doFlash = elapsed > sequenceTime
    log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
  }

  if (doFlash) {
    log.debug "FLASHING $numFlashes times"
    state.lastActivated = now()
    log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
    def initialActionOn = switches.collect{it.currentSwitch != "on"}
    def delay = 0L
    numFlashes.times {
      log.trace "Switch on after  $delay msec"
      switches.eachWithIndex {s, i ->
        if (initialActionOn[i]) {
          s.on(delay: delay)
        }
        else {
          s.off(delay:delay)
        }
      }
      delay += onFor
      log.trace "Switch off after $delay msec"
      switches.eachWithIndex {s, i ->
        if (initialActionOn[i]) {
          s.off(delay: delay)
        }
        else {
          s.on(delay:delay)
        }
      }
      delay += offFor
    }
  }
}
