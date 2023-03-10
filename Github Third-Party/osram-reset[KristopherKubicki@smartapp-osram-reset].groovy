/**
* Osram Reset
*
* Copyright 2015 Kristopher Kubicki
*
*/
definition(
	name: "Osram Reset",
	namespace: "KristopherKubicki",
	author: "kristopher@acm.org",
	description: "Flip a switch and initiate factory reset on a connected Osram bulb",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

	preferences {
		section("Fix this switch") {
		input "atarget", "capability.switch", title: "Lights", required: true, multiple: false
	}

	section("Activate the flicker when this switch is on...") {
		input "switches", "capability.switch", title: "Switch", required: true, multiple: false
	}
}


def installed() {
	initialize()
}

def updated() {	
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule() 
	subscribe(switches, "switch.on", eventHandler)
}


def eventHandler(evt) {

	atarget?.on()
    pause(5000)
    atarget?.off()
    pause(5000)
    atarget?.on()
    pause(5000)
    atarget?.off()
    pause(5000)
    atarget?.on()
    pause(5000)
    atarget?.off()
    pause(5000)
    atarget?.on()
    pause(5000)
    atarget?.off()
    pause(5000)
    atarget?.on()
    pause(5000)
    atarget?.off()
    pause(5000)
    atarget?.on()
	switches?.off()
}
