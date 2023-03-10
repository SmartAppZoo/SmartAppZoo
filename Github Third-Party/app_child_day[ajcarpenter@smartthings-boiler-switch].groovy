/**
 *  Thermostat Days
 *
 *  Copyright 2016 Andrew Carpenter
 *
 */
definition(
	name: "Thermostat Days",
	namespace: "ajcarpenter",
	author: "Andrew Carpenter",
	description: "Days child app for Thermostat",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

def days = [ 
	(Calendar.instance.MONDAY): "Monday", 
	(Calendar.instance.TUESDAY): "Tuesday", 
	(Calendar.instance.WEDNESDAY): "Wednesday", 
	(Calendar.instance.THURSDAY): "Thursday", 
	(Calendar.instance.FRIDAY): "Friday", 
	(Calendar.instance.SATURDAY): "Saturday", 
	(Calendar.instance.SUNDAY): "Sunday" 
]

preferences {
	section("Title") {
		paragraph  "TEST"
	}
	
	section("Days") {
		input(name: "days", type: "enum", options: days, title: "Day Pattern", multiple: true)
	}
	
	section("Times") {
		app(name: "times", appName: "Thermostat Times", namespace: "ajcarpenter", title: "Time Ranges", multiple: true)
	}
}

def getTimeZone() {
	location.timeZone ?: TimeZone.getDefault()
}

def getActiveTime() {
	getChildApps().sort { 
		nextOccurrence(it.start)
	}.last()
}

def defaultLabel() {
	settings.days.join(", ")
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
	getActiveTime()
}

// TODO: implement event handlers