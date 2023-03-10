/**
 *  Thermostat Times
 *
 *  Copyright 2016 Andrew Carpenter
 *
 */
definition(
	name: "Thermostat Times",
	namespace: "ajcarpenter",
	author: "Andrew Carpenter",
	description: "Times child app for Thermostat",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
	}
	
	section("Configure") {
		input(name: "start", title: "Start Time", type: "time", required: true)
		input(name: "heatingSetpoint", title: "Heating Setpoint", type: "decimal", required: true)
	}
}

def defaultLabel() {
	"$start : $heatingSetpoint"
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
	app.updateLabel(defaultLabel())
}

// TODO: implement event handlers