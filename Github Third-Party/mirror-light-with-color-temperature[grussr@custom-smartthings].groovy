definition (
	author: "Ryan Gruss",
	name: "Mirror Light with Color Temperature",
    
    category: "My Apps",
	description: "Wake up the room in the morning and turn it off at night",
    namespace: "grussr",
    
    iconUrl: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png",
    iconX2Url: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png",
    iconX3Url: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png"
)

// Setup the UI
preferences {
	section("Master Switch") {
    	paragraph("This is the master switch which will be used to control the state of the entire room. When this device is off, all of the registered devices will be off, and similarly when it is on.")
		input(name: "master", required: true, title: "Switch", type: "capability.color temperature")
	}
    
    section("SmartThings Devices") {
    	paragraph("You can select one or more devices which will be turned off and on in tandem with the master switch. The toggle devices will always have their state flipped when the master switch changes state. The explict off and on devices are helpful if you want certain devices to power on and others to power off when the master switch changes.")
        input(name: "controlledDevices", multiple: true, required: false, title: "Controlled Devices", type: "capability.color temperature")
    }
        
    section("App Settings") { }
}


// Master switch event subscriptions
def installed() {
	subscribe(master, "switch.off", off)
	subscribe(master, "switch.on", on)
	subscribe(master, "switch.setlevel", level)
    subscribe(master, "level", level)
    log.debug "subscribe to temperature"
    subscribe(master, "colorTemperature", temperature)
}

def updated() {
	unsubscribe()
	installed()
}

// Switch event handlers
def off(evt) {
	log.debug "turning off devices"
    controlledDevices.each {
    	it.off()
    }
}

def on(evt) {
	log.debug "turning on devices"
	controlledDevices.each {
    	it.on()
    }
}

def level(evt) {
	log.debug "setting device level"
	controlledDevices.each {
    	it.setLevel(evt.value)
    }
}

def temperature(evt) {
	log.debug "Setting color temp"
	controlledDevices.each {
    	it.setColorTemperature(evt.value)
    }
}
