/**
 *  Auto Lighting
 *
 *  Author: zach@beamlee.com
 *  Date: 2013-08-02
 */
preferences {
	section("Motion Sensor(s)") {
		input "motion", "capability.motionSensor", title: "Pick your sensors", multiple: true
	}
    section("Lights"){
    	input "lights", "capability.switch", title: "Pick your swithces", multiple: true
    }
    section("Misc."){
    	input "thresh", "decimal", title: "Time before lights turn off automatically"
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
	subscribe(motion, "motion.active", motionActiveHandler)
    subscribe(motion, "motion.inactive", motionInactiveHandler)
}

def motionActiveHandler(evt){
	unschedule(turnOff)
    lights.on()
}

def motionInactiveHandler(evt){
	def motionValue = motion.find{it.currentValue == "active"}
    if(!motionValue){
    	log.debug "there's legit no motion anywhere...scheduling lights off"
    	def threshold = thresh * 60
        runIn(threshold, turnOff)
    }
}

def turnOff(){
	lights.off()
}
