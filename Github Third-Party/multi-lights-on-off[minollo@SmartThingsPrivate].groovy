/**
 *  The Big Switch
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-01
 */

// Automatically generated. Make future change here.
definition(
    name: "Multi lights on-off",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Multi lights on-off",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("When this switch is turned on or off") {
		input "master", "capability.switch", title: "Where?"
	}
	section("Turn on or off all of these switches as well") {
		input "switches", "capability.switch", multiple: true, required: false
	}
	section("And turn off but not on all of these switches") {
		input "offSwitches", "capability.switch", multiple: true, required: false
	}
	section("And turn on but not off all of these switches") {
		input "onSwitches", "capability.switch", multiple: true, required: false
	}
	section("Shut everything down in this mode") {
		input "awayMode", "mode", title: "Away mode", required: false

	}
}

def installed()
{
	subscribe(master, "switch.on", onHandler, [filterEvents: false])
	subscribe(master, "switch.off", offHandler, [filterEvents: false])
	subscribe(location, modeChangeHandler)
	log.debug "[Multi lights on-off] Installed"
}

def updated()
{
    unsubscribe()
	subscribe(master, "switch.on", onHandler, [filterEvents: false])
	subscribe(master, "switch.off", offHandler, [filterEvents: false])
    subscribe(location, modeChangeHandler)
	log.debug "[Multi lights on-off] Updated"
}

def modeChangeHandler(evt) {
	log.debug "[Multi lights on-off] Mode change handler"
    if (awayMode) {
        if (evt.value == awayMode) {
            log.debug "[Multi lights on-off] Shutting down everything"
            master.off()
            offSwitches()?.off()
        }        
    }
}

def onHandler(evt) {
	log.debug "[Multi lights on-off] Turning on: ${onSwitches()}"
	onSwitches()?.on()
}

def offHandler(evt) {
	if (true) {	//evt.isPhysical || evt.isStateChange) {
        log.debug "[Multi lights on-off] Turning off: ${offSwitches()}"
        offSwitches()?.off()
    }
}

private onSwitches() {
    if(switches && onSwitches) { switches + onSwitches }
    else if(switches) { switches }
    else { onSwitches }
}

private offSwitches() {
    if(switches && offSwitches) { switches + offSwitches }
    else if(switches) { switches }
    else { offSwitches }
}
