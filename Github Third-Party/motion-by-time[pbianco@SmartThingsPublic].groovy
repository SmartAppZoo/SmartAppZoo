/**
 *  Lights triggered by Motion Time and Dimmer options
 *
 *  Author: Phil Bianco
 *  Date: 2014-03-25
 *
 */

// Automatically generated. Make future change here.
definition(
    name: "Motion By Time",
    namespace: "",
    author: "phil.bianco@gmail.com",
    description: "Turn on Lights based on Motion.  Enable intensity change as well. ",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Configure") {
		input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", description: "Tap to select", multiple: true
        input "dimmers", "capability.switchLevel", title: "Which Dimming lights?", description: "Tap to select lights", multiple: true
		input "switches", "capability.switch", title: "Which lights?", description: "Tap to select lights", multiple: true
        input "days", "enum", title: "What days would you like it to run on?", description: "Every day (default)", required: false, multiple: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
		input "level", "number", title: "Light level? (10 - 99)", description: "Percent (default)", required: false, multiple: false
        input "Delay", "number", title: "Delay to turn off lights?", description: "Percent (default)", required: true, multiple: false
        input "timeOfDay", "time", title: "When would you like to start?", description: "Time of day"
		input "endTime", "time", title: "When would you like it to end?", description: "Time of day", required: false
	}
}

def installed() {
	log.debug "installed, current mode = ${location.mode}"
	initialize()
    schedule(timeOfDay, motionActiveHandler) 
}

def updated() {
	log.debug "updated, current mode = ${location.mode}"
	unsubscribe()
	initialize()
}

def initialize() {
	log.trace "timeOfDay: $timeOfDay, endTime: $endTime"
	subscribe(motionSensors, "motion.active", motionActiveHandler)
}

def motionActiveHandler(evt){

    def t0 = now()
    def DelayTime = (Delay * 60)
   
    log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
    log.debug "Time-Now: $t0"
    log.debug "timeOfDay: $timeOfDay"
    log.debug "endTime: $endTime"
    

		log.debug "last turned on switches on , today is ${dateString}"
			log.debug "turning on switches"
            log.debug "Setting dimmer: ${dimmer} to level: ${level}"
			switches.on()
}

def turnOffSwitch() {
	switches.off()
    dimmer.off()
}