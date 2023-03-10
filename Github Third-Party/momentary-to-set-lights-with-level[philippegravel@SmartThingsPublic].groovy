definition(
    name: "Momentary To Set Lights with level",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Toggle a light(s) using a virtual momentary switch.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Select momentary switch to monitor"){
		input "theToggle", "capability.switch", multiple: false, required: true
	}
    section("Toggle these lights..."){
		input "theSwitch", "capability.switch", multiple: false, required: true
        input "level", "number", title: "At level?", required: false
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


def toggleHandler(evt) {
	log.info evt.value
    def cnt = 0
    def curSwitchValue = theSwitch.currentValue("switch")
    def curSwitchLevel = theSwitch.currentValue("level")
    log.info curSwitchValue + " at " + curSwitchLevel

	if (level) {
    	if (curSwitchLevel != level || curSwitchValue == "off") {
    		log.debug "Switch at $level percent"
			theSwitch.setLevel(level)
		}
    } else {
    	if(curSwitchValue == "on") {
        	log.debug "Switch to Off"
        	theSwitch.off()
       	} else {
        	log.debug "Switch On"
            theSwitch.on()
        }
    }
}

def initialize() {
	subscribe(theToggle, "momentary.pushed", toggleHandler)
}