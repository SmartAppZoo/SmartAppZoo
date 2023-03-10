

definition(
    name: "Light",
    namespace: "shopi",
    author: "Vadzim Asmalouski",
    description: "Test app",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    // What door should this app be configured for?
    section ("When the door opens/closes...") {
        input "contact1", "capability.contactSensor",
              title: "Where?"
    }
    // What light should this app be configured for?
    section ("Turn on/off a light...") {
        input "switch1", "capability.switch"
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(contact1, "contact", contactHandler)
}

// TODO: implement event handlers

// event handlers are passed the event itself
def contactHandler(evt) {
    switch1.off();
}