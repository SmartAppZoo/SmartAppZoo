definition(
    name: "RGB Color Cycle",
    namespace: "bburner",
    author: "Benjamin Burner",
    description: "Changes rgb lights different colors",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("solid color time") {
        input "seconds", "number", required: true, title: "Seconds?"
    }
    section("fade color time") {
        input "secondsFade", "number", required: true, title: "Seconds?"
    }
    section("When this switch is active") {
        input "thetrigger", "capability.switch", required: true
    }
    section("Change the color of this light") {
        input "theswitch", "capability.colorControl", required: true
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
    subscribe(thetrigger, "switch.on", colorChangeStartHandler)
    //subscribe(thetrigger, "switch.off", colorChangeStopHandler)
}

def colorChangeStartHandler(evt) {
	log.debug "switchColorOnHandler called: $evt"
    
    def switchState = thetrigger.currentValue("switch")
    log.debug "the trigger is currently $switchState"
    
    if (thetrigger.currentValue("switch") == "on") {
    	log.debug "in if loop"
    	firstColor()
    	//runIn(seconds * 2, turnOn)
    }
}

def firstColor() {
    log.debug "In first color method"
	theswitch.setHue(100)
    theswitch.setSaturation(100)
    runIn(secondsFade + seconds, secondColor)
}


def secondColor() {
    log.debug "In second color method"
	theswitch.setHue(33)
    theswitch.setSaturation(100)
    runIn(secondsFade + seconds, colorChangeStartHandler)
}
