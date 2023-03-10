/**
 *  Front Light Automation
 *
 *  Copyright 2016 Blake Buck
 *
 */
definition(
    name: "Front Light Automation",
    namespace: "blakebuckit",
    author: "Blake Buck",
    description: "A better front light control.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Turn on when opened:") {
		input "contact", "capability.contactSensor", required: false, title: "What?"
    }
	section("When there's motion on any of these sensors") {
		input "motion", "capability.motionSensor", required: false
	}
	section("Turn on these lights") {
		input "light", "capability.switch", required: true, title: "Which?"
	}
	section("Turn light off when no motion for ") {
		input "timeOn", "number", description: "Number of minutes", required: true, title: "Minutes"
	}
    section("Turn light off if already on?") {
    	input "offAlreadyOn", "enum", options: ["Yes", "No"], title: "Default: Yes", defaultValue: "Yes"
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
	subscribe(contact, "contact.open", turnLightOn)
	subscribe(motion,  "motion.active", turnLightOn)
}

def turnLightOn(evt){
	state.lightPrevState = light.currentValue("switch")
    log.debug "Previous State: $state.lightPrevState"
	if ("open" == evt.value || "active" == evt.value){
		if (nightTime()){
			light?.on()
			lightTimer()
		}		
	}
}

def lightTimer(){
	def delay = (timeOn != null && timeOn != "") ? timeOn * 60 : 600
	runIn(delay, turnLightOff)
}


def turnLightOff(){
	if ((state.lightPrevState == "on" && offAlreadyOn == "Yes") || state.lightPrevState == "off"){
    	light?.off()
    }	
}

def nightTime(){
	if(getSunriseAndSunset().sunrise.time < now() && getSunriseAndSunset().sunset.time > now()){
		// Daytime
		return false
	}
	else {
		return true
	}
}
