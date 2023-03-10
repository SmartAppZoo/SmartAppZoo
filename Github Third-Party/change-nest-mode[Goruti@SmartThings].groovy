/**
 *  Change Nest Mode
 *
 *  Author: Diego Antonino
 *  Date: 5/5/14
 *
 *  Simply marks any thermostat "away" if able (primarily focused on the Nest
 *  thermostat).  This is intended to be used with an "Away" or "Home" mode.
 */

definition(
    name:        "Change Nest Mode",
    namespace:   "DiegoAntonino",
    author:      "Diego Antonino",
    description: "Simply marks any thermostat HOME/ AWAY ",
    category:    "Green Living",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Partner/nest.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Partner/nest@2x.png"
)

preferences {
  section("Change these thermostats modes...") {
    input "thermostats", "capability.thermostat", multiple: true
  }

}

def installed() {
  subscribe(location, "mode", changeMode)
}

def updated() {
  unsubscribe()
  subscribe(location, "mode", changeMode)
}

def changeMode(evt) {
def newMode = location.mode
log.info "Marking $newMode"

  if(newMode == "Away") {
    log.info "Marking thermostat AWAY"
    thermostats?.away()
  } else if(newMode == "Home"){
  		log.info "Marking thermostat HOME"
        thermostats?.present()
  }
}