definition(
    name: "Use Virtual to Check If On/Off",
        description: "This SmartApp uses a virtual switch to see if a light is on or not before triggering the real light.",
    author: "MJB",iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/alfred-app.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/alfred-app@2x.png",
)

preferences {
    section("Real Switch...") { 
        input "realswitch", "capability.switch", 
	title: "Real Switch...", 
        required: true
    }
    section("Virtual Stand-in...") {
    	input "standin", "capability.switch",
        title: "Stand In Virtual Switch...",
        required: true
    }
}

def installed() {
    subscribe(standin, "switch.on", switchOnHandler)
    subscribe(standin, "switch.off", switchOffHandler)
}

def updated() {
    unsubscribe()
    subscribe(standin, "switch.on", switchOnHandler)
    subscribe(standin, "switch.off", switchOffHandler)
}

def switchOnHandler(evt) {
    state.wasOff = realswitch.currentValue("switch") == "off"
    if(state.wasOff)realswitch.on()
}

def switchOffHandler(evt) {
    if(state.wasOff)realswitch.off()
}