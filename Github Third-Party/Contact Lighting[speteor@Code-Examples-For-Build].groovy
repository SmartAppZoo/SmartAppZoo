/**
 *  Auto Lighting [Contact Version]
 *
 *  Author: zach@beamlee.com
 *  Date: 2013-08-02
 */
preferences {
	section("Contact Sensor") {
		input "contact", "capability.contactSensor", title: "Pick your sensors"
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
	subscribe(contact, "contact.open", contactOpenHandler)
    subscribe(contact, "contact.closed", contactClosedHandler)
}

def contactOpenHandler(evt){
	unschedule(turnOff)
    lights.on()
}

def contactClosedHandler(evt){
    	def threshold = thresh * 60
        log.debug "threshold is $threshold seconds"
        runIn(threshold, turnOff)
        
}

def turnOff(){
	log.debug "turning off"
	lights.off()
}

// TODO: implement event handlers