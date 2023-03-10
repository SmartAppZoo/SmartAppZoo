/**
 *  The Big Off Switch
 *
 *  Author: steve.sell@gmail.com
 *  Adapted from "the big switch" by SmartThings
 *
 *  Date: 2013-10-15
 */

// Automatically generated. Make future change here.
definition(
    name: "The Big Off Switch",
    namespace: "steve28",
    author: "steve.sell@gmail.com",
    description: "Turns off any number of switches when a momentary contact switch is pushed.  Only tested with the Momentary Button Tile device type.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("When this switch is pushed") {
		input "master", "capability.momentary", title: "Where?"
	}
	section("Turn off all of these switches") {
		input "switches", "capability.switch", multiple: true, required: false
	}
}

def installed()
{
	subscribe(master, "momentary.pushed", offHandler)
}

def updated()
{
	unsubscribe()
	subscribe(master, "momentary.pushed", offHandler)
}

def offHandler(evt) {
	//log.debug evt.value
    log.debug "Turning off: " + switches
	switches?.off()
}

