/**
 *  Night Lighting
 *
 *  Copyright 2015 Greg Peterson
 *
 */
definition(
    name: "Night Lighting",
    namespace: "gpete",
    author: "Greg Peterson",
    description: "Smarter night lighting",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage") {
        section("Turn on these lights") {
            input(name: "lights", type: "capability.switch", title: "Which lights?", multiple: true)
        }
        section("To this brightness") {
            input(name: "brightnessA", type: "number", title: "Brightness percentage")
        }
        section("When there's motion on these sensors") {
            input(name: "mainMotion", type: "capability.motionSensor", title: "Which motion sensors are in the room?", multiple: true)
        }
        section("Dim to this level") {
        	input(name: "brightnessA", type: "number", title: "Brightness percentage")
        }
        section("Dim after") {
        	
        }
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
}

// TODO: implement event handlers