/**
 *  Level dimming test
 *
 *  Copyright 2018 DCVN
 *
 */
definition(
    name: "Level dimming test",
    namespace: "OurPlace",
    author: "DCVN",
    description: "Level dimming test",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {														// stuff for android app
	section("Create Device to monitor list") {
		input "devices", "capability.switchLevel", multiple: true
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
    subscribe(devices, "switch.on", switchEventHandler)						// any switch event
    subscribe(devices, "switch.off", switchEventHandler)
    
    subscribe(devices, "switch", levelEventHandler)
    subscribe(devices, "level", levelEventHandler)
    subscribe(devices, "setLevel", levelEventHandler)
    subscribe(devices, "switch.setLevel", levelEventHandler)
	}

def levelEventHandler(evt) {
	log.debug "Level change ${evt}"
    }
    
    
def switchEventHandler(evt) {
	log.debug "switch change ${evt}"
	}