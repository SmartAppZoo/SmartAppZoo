/**
* Candle Flicker
*
* Copyright 2015 Kristopher Kubicki
*
*/
definition(
	name: "Candle Flicker",
	namespace: "KristopherKubicki",
	author: "kristopher@acm.org",
	description: "Use a low quality LED to fake a low oxygen flickering gas lantern",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

	preferences {
		section("Select Dimmable Lights...") {
		input "dimmers", "capability.switchLevel", title: "Lights", required: true, multiple: true
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
	if(switches.currentValue("switch") == "on") {
		for (dimmer in dimmers) {      
                	def lowLevel= Math.abs(new Random().nextInt() % 3) + 59
                	def upLevel= Math.abs(new Random().nextInt() % 10) + 90
                	def upDelay = Math.abs(new Random().nextInt() % 500)
                	def lowDelay = upDelay + Math.abs(new Random().nextInt() % 200)
            		//log.debug "low: $lowLevel $lowDelay high: $upLevel $upDelay"
           
			dimmer.setLevel(upLevel,[delay: upDelay])
                	dimmer.setLevel(lowLevel,[delay: lowDelay])
        	}
        	def sleepTime = Math.abs(new Random().nextInt() % 200)
        	pause(sleepTime)
        	runIn(1,"eventHandler")
	}
}
