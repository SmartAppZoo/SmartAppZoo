/*
RF Ceiling Fan Samrtapp
Original version by Tom Forti https://github.com/tomforti/relay_hampton_bay_fan/blob/master/FanSmartapp.groovy
Modified version for RF Ceiling Fan by tamttt@gmail.com
*/
definition(
    name: "RF Ceiling Fan with Etekcity Switch Smartapp",
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
	section("Connect these switches to the Arduino") {
		input "switch1", title: "Switch 1 for Arduino", "capability.switch"
        input "switch2", title: "Switch 2 for Arduino", "capability.switch", required: false  
		input "switch3", title: "Switch 3 for Arduino", "capability.switch", required: false
		input "switch4", title: "Switch 4 for Arduino", "capability.switch", required: false
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
	//
	subscribe(switch1, "switch.on", switch1on)
	subscribe(switch1, "switch.off", switch1off)
	subscribe(switch2, "switch.on", switch2on)
	subscribe(switch2, "switch.off", switch2off)	
	subscribe(switch3, "switch.on", switch3on)
	subscribe(switch3, "switch.off", switch3off)	
	subscribe(switch4, "switch.on", switch4on)
	subscribe(switch4, "switch.off", switch4off)
	
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
	//
	subscribe(switch1, "switch.on", switch1on)
	subscribe(switch1, "switch.off", switch1off)
	subscribe(switch2, "switch.on", switch2on)
	subscribe(switch2, "switch.off", switch2off)	
	subscribe(switch3, "switch.on", switch3on)
	subscribe(switch3, "switch.off", switch3off)	
	subscribe(switch4, "switch.on", switch4on)
	subscribe(switch4, "switch.off", switch4off)
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

def switch1on(evt) {
	log.debug "Switch 1 from smartapp: $evt.value"	
	arduino.switch1On()	
}

def switch1off(evt) {
	log.debug "Switch 1 from smartapp: $evt.value"	
	arduino.switch1Off()	
}

def switch2on(evt) {
	log.debug "Switch 2 from smartapp: $evt.value"	
	arduino.switch2On()	
}

def switch2off(evt) {
	log.debug "Switch 2 from smartapp: $evt.value"	
	arduino.switch2Off()	
}

def switch3on(evt) {
	log.debug "Switch 3 from smartapp: $evt.value"	
	arduino.switch3On()	
}

def switch3off(evt) {
	log.debug "Switch 3 from smartapp: $evt.value"	
	arduino.switch3Off()	
}

def switch4on(evt) {
	log.debug "Switch 4 from smartapp: $evt.value"	
	arduino.switch4On()	
}

def switch4off(evt) {
	log.debug "Switch 4 from smartapp: $evt.value"	
	arduino.switch4Off()	
}

