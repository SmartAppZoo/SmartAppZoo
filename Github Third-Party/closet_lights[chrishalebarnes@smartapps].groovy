definition(
    name: "Closet Lights",
    namespace: "chrishalebarnes",
    author: "Chris Barnes",
    description: "Turn on closet light if a door is opened. Turn it off if both are closed.",
    category: "Custom Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("Devices") {
    input "firstDoor", "capability.contactSensor", title: "pick a contact sensor", required: true, multiple: false
    input "secondDoor", "capability.contactSensor", title: "pick a contact sensor", required: true, multiple: false
    input "light", "capability.switch", title: "pick a light", required: true, multiple: false
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
	subscribe(firstDoor, "contact", firstDoorHandler)
  	subscribe(secondDoor, "contact", secondDoorHandler)
}

def firstDoorHandler(evt) {
  if(evt.value == "open"|| secondDoor.currentValue('contact') == "open") {
    log.debug "First contact or second contact is open. Turning on light."
    light.on()
  } else {
    log.debug "Both contacts are closed. Turning off light"
    light.off()
  }
}

def secondDoorHandler(evt) {
  if(evt.value == "open" || firstDoor.currentValue('contact') == "open") {
    log.debug "First contact or second contact is open. Turning on light."
    light.on()
  } else {
    log.debug "Both contacts are closed. Turning off light"
    light.off()
  }
}
