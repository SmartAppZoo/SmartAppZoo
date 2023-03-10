definition(
    name: "Night Alarm",
    namespace: "crousky",
    author: "Ben Crouse",
    description: "Activate alarm when there is no motion",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/good-night@2x.png")


preferences {
	section("Security Sensors") {
    	input "motions", "capability.motionSensor", title: "Motion Sensors", multiple: true
        input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true
        input "accelerations", "capability.accelerationSensor", title: "Acceleration Sensors", multiple: true
    }
    
    section("Alarms to set off") {
    	input "lights", "capability.switch", title: "Turn on these lights", multiple: true, required: false
        input "alarms", "capability.alarm", title: "Activate these alarms", multiple: true, required: false
    }
    
    section("Delay after motion") {
    	input name: "motionDelay", type: "number", title: "Time to wait after motion (minutes)?"
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
	state.lastMotion = now()
    subscribe(motions, "motion.active", updateMotion)
	subscribe(contacts, "contact.open", triggerAlarm)
    subscribe(accelerations, "acceleration.active", triggerAlarm)
}

def updateMotion(evt) {
	log.debug("motion detected")
	state.lastMotion = now()
}

def triggerAlarm(evt) {
    def nowWithDelay = now() - (motionDelay * 60 * 1000)
    log.debug("time with added delay $nowWithDelay")
    log.debug("$state.lastMotion")
    if (nowWithDelay > state.lastMotion)
    {
        log.debug("triggering alarm")
        lights?.on()
        alarm?.both()
    }
}