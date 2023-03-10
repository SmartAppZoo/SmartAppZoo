/**
 *  Garage Door Handler
 *
 *  Copyright 2016 James Guistwite
 *
 */
definition(
    name: "Garage Door Handler",
    namespace: "jgui",
    author: "James Guistwite",
    description: "Manage my garage doors.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Choose Doors:") {
        input "garageDoors", "capability.garageDoorControl", required: true, title: "Garage Doors", multiple:true
    }
    section("Choose Lights:") {
        input "lights", "capability.switch", required: true, title: "Lights", multiple:true
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
  log.debug("GDM: subscribe to ${garageDoors}")
  subscribe(garageDoors, "switch", garageDoorStateHandler)
}

def garageDoorStateHandler(evt) {
    log.debug "GDM: event name: ${evt.name} ${evt.value}"
    def anyOpen = false
    def message = "Home: "
    garageDoors.each {
       //log.debug "GDM: ${it.name} is in state ${it.switchState.value}"
       anyOpen |= ("doorOpen" == it.switchState.value) 
       message = message + "${it.displayName} ";
       if ("doorOpen" == it.switchState.value) {
          message = message + "OPEN. ";
       }
       else {
          message = message + "CLOSED. ";
       }
    }
    //log.debug "GDM: any open? ${anyOpen}"
    lights.each {
      //log.debug "GDM: set ${it.name} on / off"
      if (anyOpen) it.on() else it.off()
    }

    sendPush(message)
}