definition(
	name: "The Big Dimmer",
	namespace: "petermajor",
	author: "Peter Major",
	description: "Based on 'The Big Switch'. Turns on, off and dim a collection of lights based on the state of a specific dimmer.",
	category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
preferences {
	section("When this switch is turned on, off or dimmed") {
		input "master", "capability.switch", title: "Where?"
	}
	section("Dim these switches") {
		input "dimSwitches", "capability.switchLevel", multiple: true, required: false
	}    
}

def installed()
{   
	initialize()
}

def updated()
{
	unsubscribe()
	initialize() 
}

def initialize()
{   
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	subscribe(master, "level", dimHandler)   
}

def onHandler(evt) {
	log.debug "On"
	log.debug "Dim level: $master.currentLevel"
	dimSwitches?.on()
  	dimSwitches?.setLevel(master.currentLevel)
}

def offHandler(evt) {
	log.debug "Off"
	dimSwitches?.off()
}

def dimHandler(evt) {

	def newLevel = evt.value.toInteger()
	log.debug "Dim level: $newLevel"

	dimSwitches?.setLevel(newLevel)
}