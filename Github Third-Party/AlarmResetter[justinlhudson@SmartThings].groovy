// Note: 20 sec (current cloud setting) timeout, watch the pause().

definition(
  name: "Alarm Resetter",
  namespace: "Operations",
  author: "justinlhudson",
  description: "Resets Alarming (incase someone comes invited without unlocking)",
  category:  "Safety & Security",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("Alarming...") {
    input "alarms", "capability.alarm", title:"Reset alarms", multiple:true, required:false
    input "switches_reset", "capability.switch", title: "Reset switches", required: false, multiple: true
    input "switches_off", "capability.switch", title: "Off switches", required: false, multiple: true
    input "reset", "number", title:"Reset (seconds)", defaultValue:15
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def resetHandler(evt)
{
  if (state.lock == false) {
    updated()
  }
}


def appTouch(evt)
{
  clear()
}

def clear(evt) {
  log.debug "clear"
 
  alarms_off()
  switches_off()

  state.lock = false
  log.debug "unlocked"

  notify("Alarm(s) Reset...")
}

def alarmHandler(evt)
{
  log.debug "${evt.value}"
  if( state.lock == false) {
    state.lock = true
    log.debug "locked"

    notify("Alarm(s) Active!")
    
    // Throwing un handled exception?  but yet still runs...
    // try a few then cross fingers actually worked. WT...?!?!
    def x = 3 * 4
    x.times { n ->
      try {
        runIn(settings.reset, clear, [overwrite: true])
        return
      } catch (all) {
        pause(250)
        // EAT IT!
        //log.error "Something went horribly wrong!\n${all}"        
      }      
    }

  }
}

private def alarms_off() {
  log.debug "alarms_off"
  def x = 3
  x.times { n ->
    try {
      settings.alarms.each {
        //if ( it != null && it.latestValue("alarm") != "off") {
          it.off()
        //}
      }
    }
    catch (all) {
      log.error "Something went horribly wrong!\n${all}"
    }
    if( n > 0) {
      pause(1500)
    }
  }
}

private def switches_off() {
  log.debug "switches_off"
  def x = 3
  x.times { n ->
    try {
      settings.switches_reset.each {
        //if ( it != null && it.currentSwitch != "off") {
          it.off()
        //}
      }
    }
    catch (all) {
      log.error "Something went horribly wrong!\n${all}"
    }
    if( n > 0) {
      pause(1500)
    }
  }
}

private def notify(message) {
  try {
    sendNotificationEvent(message)
  } catch (all) {
    log.error "Something went horribly wrong!\n${all}"
  }
}

private def initialize() {
  state.lock = false
  state.cycle = 0

  subscribe(switches_off, "switch", clear)
  subscribe(switches_reset, "switch.on", alarmHandler)
  subscribe(alarms, "alarm", alarmHandler)
  subscribe(app, appTouch)

  // HACK: keep alive
  subscribe(location, "sunset", resetHandler)
  subscribe(location, "sunrise", resetHandler)
}
