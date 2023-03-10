/**
 *  Switch Toggler
 *  Turn on a switch when a contact sensor opens and then turn it back off when the sensor is closed
 *
 *  Author: mxrugg
 */
definition(
	name: "Switch Toggler",
	namespace: "mxrugg",
	author: "mxrugg",
	description: "Turn a switch on/off when a contact sensor is opened/closed.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png")

preferences {
	section("When it opens..."){
		input "contact1", "capability.contactSensor"
	}
	section("Turn on a switch..."){
		input "switch1", "capability.switch"
	}
}

// When the app is first installed
def installed() {
	initialize("Installed")
}

// When the app preferences are modified after install, remove any previous
// event listeners to existing inputs in case the user has changed them
def updated(settings) {
	unsubscribe()
	initialize("Updated")
}

// Central point for handling all event listeners
def initialize(type) {
	log.debug type + " with settings ${settings}"

	// Listen to any contact and switch events
	subscribe(contact1, "contact", contactHandler)
	subscribe(switch1, "switch", switchHandler)
}

// Handles the open or closed events of the contact sensor
def contactHandler(event) {
	switch (event.stringValue) {
		case "open":
			switch1.on()
			break
		case "closed":
			switch1.off()
			break
		default:
			log.warn "Contact Sensor: State is not open or closed"
	}
}

// Handles the on or off events of the switch
def switchHandler(event) {
	switch (event.stringValue) {
		case "on":
			log.debug("Switch is turned on")
			break
		case "off":
			log.debug("Switch is turned off")
			break
		default:
			log.warn "Switch: State is not on or off"
	}
}
