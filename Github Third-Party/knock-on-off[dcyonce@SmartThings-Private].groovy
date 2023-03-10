/**
 *  Toggle light on Knock
 *
 */

definition(
    name: "Knock On/Off",
    namespace: "dcyonce",
    author: "Don Yonce",
    description: "Toggle a Lamp On/Off when a Multisensor senses a knock.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("Knock Sensor?") {
    input name: "knockSensor", type: "capability.accelerationSensor", required: true, title: "MultiSensor?"
  }

  section("Lamps to control?") {
    input "lamp", "capability.switch", title: "Select one or more lights", required: true, multiple: true
	}
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(knockSensor, "acceleration.active", knockEvent)
}

def knockEvent(evt) {
	log.debug("evt.value=${evt.value}")
    
    def isOn = lamp.currentswitch.contains("on") == true
    
	log.debug("lamp=${lamp.currentswitch}, ${isOn}")
	if(evt.value == "active") 
    {
    	if(isOn) 
        	lamp.off() 
    	else 
        	lamp.on()
    }
}

