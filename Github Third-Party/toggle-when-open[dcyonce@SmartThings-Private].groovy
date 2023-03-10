/**
 *  Toggle When Open
 *
 */
definition(
    name: "Toggle When Open",
    namespace: "dcyonce",
    author: "Bruce Ravenel",
    description: "Toggle a switch when a contact opens",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select Contact and Switch") {
	    input "myContact", "capability.contactSensor", title: "Choose your contact sensor", required: true, multiple: false
            input "mySwitch", "capability.switch", title: "Choose your switch", required: true, multiple: false
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
	subscribe(myContact, "contact", contactHandler)
}

def contactHandler(evt) {
	if(evt.value == "open") if(mySwitch.currentSwitch == "off") mySwitch.on() else mySwitch.off()
}