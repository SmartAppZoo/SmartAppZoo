/**
 *  Lights on Arrival with Timeout
 *
 *  Author: John Gorsica
 *
 */
definition(
    name: "Lights on Arrival with Timeout",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Turn switch on for a set duration when arriving",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When one of these persons arrives") {
		input "people", "capability.presenceSensor", multiple: true
	}
    section("Turn on these lights...") {
		input "switch1", "capability.switch", multiple: true, title: "Where?"
	}
    section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
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
	subscribe(people, "presence", presenseHandler)
}

def presenseHandler(evt) {
    if (evt.value == "present") {
        switch1.on()
        if(delayMinutes) {
			runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
		}
    }
}

def turnOffMotionAfterDelay(){
	log.debug "Turning off lights"
	switch1.off()
}
