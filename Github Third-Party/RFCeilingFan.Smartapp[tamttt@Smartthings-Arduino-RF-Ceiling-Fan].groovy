/*
RF Ceiling Fan Samrtapp
Original version by Tom Forti https://github.com/tomforti/relay_hampton_bay_fan/blob/master/FanSmartapp.groovy
*/
definition(
    name: "RF Ceiling Fan Smartapp",
    namespace: "tamttt",
    author: "tamttt",
    description: "Smartthings sends request to Arduino RF Transmister/Receiver",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

//set the fans and arduino to the app
preferences {
	section("Connect these virtual Fans to the Arduino") {
		input "fan1", title: "Fan 1 for Arduino", "capability.switchLevel"
        input "fan2", title: "Fan 2 for Arduino", "capability.switchLevel", required: false  
		input "fan3", title: "Fan 3 for Arduino", "capability.switchLevel", required: false
		input "fan4", title: "Fan 4 for Arduino", "capability.switchLevel", required: false
	}
    section("Which Arduino board to control?") {
		input "arduino", "capability.switch", required: true
    }    
}

def installed() {
	subscribe(fan1, "fan", fan1val)
    subscribe(fan1, "light", light1val)
    subscribe(fan2, "fan", fan2val)
    subscribe(fan2, "light", light2val)   
	subscribe(fan3, "fan", fan3val)
    subscribe(fan3, "light", light3val)
	subscribe(fan4, "fan", fan4val)
    subscribe(fan4, "light", light4val)
}

//checks for changes from the vitural fan switch
def updated() {
	unsubscribe()
	subscribe(fan1, "fan", fan1val)
    subscribe(fan1, "light", light1val)
    subscribe(fan2, "fan", fan2val)
    subscribe(fan2, "light", light2val)   
	subscribe(fan3, "fan", fan3val)
    subscribe(fan3, "light", light3val)
	subscribe(fan4, "fan", fan4val)
    subscribe(fan4, "light", light4val)
    log.info "subscribed to all of Fan events"
}

//takes value from vitrual device and uses it to trigger fan
def fan1val(evt) {		
	int level = fan1.currentValue("level")
	log.debug "level: $level"	
    
    if ((level >= 0) && (level < 25)) {
		log.debug "Fan 1 level from smartapp: 0"
    	arduino.fan1Off() //tell the arduino to turn trigger this arduino switch    	
    }	
    if ((level >= 25) && (level < 50)) {
		log.debug "Fan 1 level from smartapp: 25"
    	arduino.fan1Low()    	
    }
    if ((level >= 50) && (level < 75)) {
		log.debug "Fan 1 level from smartapp: 50"
    	arduino.fan1Medium()    	
    }
    if (level >= 75) {
		log.debug "Fan 1 level from smartapp: 75"
    	arduino.fan1High()    	
    }
}

def light1val(evt) {	
	if ((evt.value == "on") || (evt.value == "off" )) {
    	log.debug "Light 1 from smartapp: $evt.value"
		arduino.light1Toggle()
	}
}

def fan2val(evt) {	
	int level = fan2.currentValue("level")
	log.debug "level: $level"
    
    if ((level >= 0) && (level < 25)) {
    	arduino.fan2Off()    	
    }
    if ((level >= 25) && (level < 50)) {
    	arduino.fan2Low()    	
    }
    if ((level >= 50) && (level < 75)) {
    	arduino.fan2Medium()    	
    }
    if (level >= 75) {
    	arduino.fan2High()    	
    }
}

def light2val(evt) {
	log.debug "Light 2 from smartapp: $evt.value"
	if ((evt.value == "on") || (evt.value == "off" )) {
		arduino.light2Toggle()
	}
}

def fan3val(evt) {	
	int level = fan3.currentValue("level")
	log.debug "level: $level"
    
    if ((level >= 0) && (level < 25)) {
    	arduino.fan3Off()    	
    }
    if ((level >= 25) && (level < 50)) {
    	arduino.fan3Low()    	
    }
    if ((level >= 50) && (level < 75)) {
    	arduino.fan3Medium()    	
    }
    if (level >= 75) {
    	arduino.fan3High()    	
    }
}

def light3val(evt) {
	log.debug "Light 3 from smartapp: $evt.value"
	if ((evt.value == "on") || (evt.value == "off" )) {
		arduino.light3Toggle()
	}
}

def fan4val(evt) {	
	int level = fan4.currentValue("level")
	log.debug "level: $level"
    
    if ((level >= 0) && (level < 25)) {
    	arduino.fan4Off()    	
    }
    if ((level >= 25) && (level < 50)) {
    	arduino.fan4Low()    	
    }
    if ((level >= 50) && (level < 75)) {
    	arduino.fan4Medium()    	
    }
    if (level >= 75) {
    	arduino.fan4High()    	
    }
}

def light4val(evt) {
	log.debug "Light 4 from smartapp: $evt.value"
	if ((evt.value == "on") || (evt.value == "off" )) {
		arduino.light4Toggle()
	}
}

