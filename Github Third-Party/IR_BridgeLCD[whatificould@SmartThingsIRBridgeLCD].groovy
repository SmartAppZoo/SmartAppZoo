/**
 *  IR Receiver
 *
 *  Author: gilbert@whatificould.net
 *  Date: 03.03.2014
 */
preferences {
	section("Pick an IR device...") {
    	input "irDevice", "device.myirbridge"
    }
	section("Button A turns on or off..."){
		input "switch1", "capability.switch", title: "This light", required: false
	}
	section("Button B turns on or off..."){
		input "switch2", "capability.switch", title: "This light", required: false
	}    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(irDevice, "button.B", handleB)
    subscribe(irDevice, "button.A",handleA)
    
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
        subscribe(irDevice, "button.B", handleB)
        subscribe(irDevice, "button.A",handleA)
}

def handleA(evt) {
	log.debug "received button A"
    if (switch1.currentValue("switch") == "on") {
        switch1.off()
		irDevice.off()
        log.debug "SmartApps Aoff"
    }
    else {
        switch1.on()
    	irDevice.on()
    	log.debug "SmartApps Aon"
    }
}

def handleB(evt) {
	log.debug "received button B"
    if (switch2.currentValue("switch") == "on") {
        switch2.off()
      	irDevice.Boff()
    }
    else {
        switch2.on()
      	irDevice.Bon()
    }
}