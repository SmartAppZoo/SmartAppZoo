/**
 *  Multi-switch Controler (SmartApp)
 *
 *  Author: davidebliss@gmail.com
 *  Date: 2014-01-26
 */
preferences {
	section("Which controller") {
		input "controller", "device.BlissControl ", title: "Which controller?"
	}
	
    section("Switch 1 turns on/off these...") {
		input "s1lights", "capability.switch", multiple: true
	}
    
    section("Switch 2 turns on/off these...") {
		input "s2lights", "capability.switch", multiple: true
	}
    
    section("Switch 3 pauses these...") {
		input "s3soni", "capability.musicPlayer", multiple: true
	}
    
    section("Button 1 skips on these...") {
		input "b1soni", "capability.musicPlayer", multiple: true
	}
    
    section("Button 2 set colors on these...") {
		input "b2hues", "capability.colorControl", multiple: true
	}
    
    section("Knob dims these...") {
		input "klights", "capability.switchLevel", multiple: true
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
	subscribe(controller, "switch1", switch1Handler)
	subscribe(controller, "switch2", switch2Handler)
	subscribe(controller, "switch3", switch3Handler)
	subscribe(controller, "switch4", switch4Handler)
	subscribe(controller, "switch5", switch5Handler)
	subscribe(controller, "level", knob1Handler)
}

def switch1Handler(evt) {
	log.debug "switch1Handler: $evt.value"
	if (evt.value == "on1") {
		for (i in s1lights) i.on()
	} else if (evt.value == "off1") {
		for (i in s1lights) i.off()
	}
}

def switch2Handler(evt) {
	log.debug "switch2Handler: $evt.value"
	if (evt.value == "on2") {
		for (i in s2lights) i.on()
	} else if (evt.value == "off2") {
		for (i in s2lights) i.off()
	}
}

def switch3Handler(evt) {
	log.debug "switch3Handler: $evt.value"
	if (evt.value == "on3") {
		for (i in s3soni) i.play()
	} else if (evt.value == "off3") {
		for (i in s3soni) i.pause()
	}
}

def switch4Handler(evt) {
	log.debug "switch4Handler: $evt.value"
	if (evt.value == "on4") {
		for (i in b1soni) i.nextTrack()
	} 
}

def switch5Handler(evt) {
	log.debug "switch5Handler: $evt.value"
	if (evt.value == "on5") {
		//for (i in b2soni) i.play()
        // i.
        //setHue(number)
		//setSaturation(number)
		//setColor(color_map)
        //
        Random random = new Random()
        for (i in b2hues){
            i.setLevel(60+random.nextInt(10))
			i.setSaturation(60+random.nextInt(20))
			i.setHue(15+random.nextInt(4))
        }
	} 
}

def knob1Handler(evt) {
	log.debug "knob1Handler: $evt.value"
	for (i in klights) i.setLevel(evt.value.toInteger())
}


