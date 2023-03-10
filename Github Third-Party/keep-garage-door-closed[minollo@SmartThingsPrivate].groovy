/**
 *  Keep Garage Door Closed
 *
 *  Author: Minollo
 */

// Automatically generated. Make future change here.
definition(
    name: "Keep garage door closed",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Very simple. It relies on a contact sensor and a relay switch to make sure the garage door stays closed after proper timeouts; different timeouts based on away or not modes",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Garage door") {
		input "contactSensor", "capability.contactSensor", title: "Open/close sensor?"
		input "doorSwitch", "capability.switch", title: "Door switch?"
	}
	section("Away mode") {
		input "awayMode", "mode", title: "Away mode?"
		input "maxOpenTimeWhenAway", "number", title: "Minutes?"
	}
	section("Home mode") {
		input "maxOpenTimeWhenHome", "number", title: "Minutes?"
	}
}

def installed()
{
	subscribe(contactSensor, "contact.open", contactSensorOpenHandler)
	subscribe(contactSensor, "contact.closed", contactSensorClosedHandler)
	subscribe(doorSwitch, "switch.on", doorSwitchHandler)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
    updateState()
}

def updated()
{
	unsubscribe()
	subscribe(contactSensor, "contact.open", contactSensorOpenHandler)
	subscribe(contactSensor, "contact.closed", contactSensorClosedHandler)
	subscribe(doorSwitch, "switch.on", doorSwitchHandler)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
    updateState()
}

def pollerEvent(evt) {
    log.debug "[PollerEvent]"
    if (state.timerLatest && now() - state.timerLatest > (maxOpenTimeWhenAway + 1) * 60 * 1000) {
        log.error "Closing garage (timer was asleep)"
        closeGarage()
    }
}

def contactSensorOpenHandler(evt)
{
	log.debug "Garage just opened; start counting"
	updateState()
}

def contactSensorClosedHandler(evt)
{
   	log.debug "Garage just closed; we are fine"
	try {unschedule(closeGarage)} catch(e) {log.error "Ignoring error: ${e}"}
}

private updateState()
{
	state.timerLatest = null
	try {unschedule(closeGarage)} catch(e) {log.error "Ignoring error: ${e}"}
	if (contactSensor.currentValue("contact") == "open") {
    	if (location.mode == awayMode) {
   	    	log.debug "Close garage in $maxOpenTimeWhenAway minutes"
            state.timerLatest = now()
	    	runIn(maxOpenTimeWhenAway * 60, closeGarage)
    	} else {
   	    	log.debug "Close garage in $maxOpenTimeWhenHome minutes"
            state.timerLatest = now()
    		runIn(maxOpenTimeWhenHome * 60, closeGarage)
        }
    }
}

def closeGarage()
{
	log.debug "Time to close the garage"
	try {unschedule(closeGarage)} catch(e) {log.error "Ignoring error: ${e}"}
	if (contactSensor.currentValue("contact") == "open") {
    	log.debug "Push button"
    	doorSwitch.push()
        updateState()
	}
    state.timerLatest = null
}

def doorSwitchHandler(evt)
{
	log.debug "Button pushed"
}

