definition(
    name: "Mode Switch Activator",
    namespace: "Operations",
    author: "justinlhudson",
    description: "Switches on when mode active set and off otherwise.",
    category:  "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Settings") {
        input "activeMode", "mode", title:"Active Mode", multiple:false, required:true
        input "switches", "capability.switch", title: "Switch", required: false, multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()

    initialize()
    
    // update on reset/restart
    modeHandler([value: location.mode])
}

def modeHandler(evt)
{
    if (evt.value == settings.activeMode) {
        switches_on()
    }
    else {
        switches_off()
    }
}

def resetHandler(evt)
{
  updated()
}

private def initialize() {
   subscribe(location, "mode", modeHandler)
    
  // HACK: keep alive
  subscribe(location, "sunset", resetHandler)
  subscribe(location, "sunrise", resetHandler)
}

private def switches_off() {
  log.debug "switches_off"
  def x = 6
  x.times { n ->
      settings.switches.each {
        if ( it != null && it.currentSwitch != "off") {
          it.off()
        }
      }
      if( n > 0) {
        pause(500)
      }
    }
}

private def switches_on() {
    log.debug "switches_on"
    def x = 6
    x.times { n ->
      settings.switches.each {
        if ( it != null && it.currentSwitch != "on") {
          it.on()          
        }
      }
      if( n > 0) {
        pause(500)
      }
    }
}
