definition(
    name: "Mode Locker",
    namespace: "Operations",
    author: "justinlhudson",
    description: "Switch(es) Lock Mode",
    category:  "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    input "lockSwitches", "capability.switch", title:"Select Lock Switches", multiple:true, required:true
    input "lockMode", "mode", title:"Mode Lock", multiple:false, required:true
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()

    initialize()
}

def modeLock() {
    for (device in settings.lockSwitches) {
        if(device.currentValue("switch") == "on") {
            log.debug "${device.name} - on"
            if( location.mode != settings.lockMode ) {
              setLocationMode(settings.lockMode)
            }
        }
        else {
            log.debug "${device.name} - off"
        }
    }
}

def switchOnHandler(evt) {
    modeLock()
}

def modeHandler(evt) {
    log.debug "${evt.value}"
    modeLock()
}

def resetHandler(evt) {
  updated()
}

private def initialize() {
  modeLock()
  runEvery5Minutes("modeLock")
  subscribe(settings.lockSwitches, "switch.on", switchOnHandler)
  subscribe(location, "mode", modeHandler)

  // HACK: keep alive
  subscribe(location, "sunset", resetHandler)
  subscribe(location, "sunrise", resetHandler)
}
