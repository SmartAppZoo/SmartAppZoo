definition (
	author: "Oliver Spryn",
	name: "Mirror",
    
    category: "Convenience",
	description: "Wake up the room in the morning and turn it off at night",
    namespace: "oliverspryn",
    
    iconUrl: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png",
    iconX2Url: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png",
    iconX3Url: "https://d30y9cdsu7xlg0.cloudfront.net/png/889233-200.png"
)

// Setup the UI
preferences {
	section("Master Switch") {
    	paragraph("This is the master switch which will be used to control the state of the entire room. When this device is off, all of the registered devices will be off, and similarly when it is on.")
		input(name: "master", required: true, title: "Switch", type: "capability.switch")
	}
    
    section("SmartThings Devices") {
    	paragraph("You can select one or more devices which will be turned off and on in tandem with the master switch. The toggle devices will always have their state flipped when the master switch changes state. The explict off and on devices are helpful if you want certain devices to power on and others to power off when the master switch changes.")
        input(name: "devicesOff"   , multiple: true, required: false, title: "Always Turn Off", type: "capability.switch")
        input(name: "devicesOn"    , multiple: true, required: false, title: "Always Turn On" , type: "capability.switch")
        input(name: "devicesToggle", multiple: true, required: false, title: "Toggle Devices" , type: "capability.switch")
    }
    
    section("IFTTT Devices") {
    	paragraph("This section is similar to the SmartThings devices section above, except instead of controlling devices directly through SmartThings, IFTTT will control them by calling the URLs you specify below. You can call multiple URLs in each text box by seperating them with a comma.")
        input(autoCorrect: false, name: "iftttOff"   , required: false, title: "Off URLs"   , type: "text")
        input(autoCorrect: false, name: "iftttOn"    , required: false, title: "On URLs"    , type: "text")
        input(autoCorrect: false, name: "iftttToggle", required: false, title: "Toggle URLs", type: "text")
    }
    
    section("App Settings") { }
}

// URL mapping
mappings {
  path("/room") {
    action: [
      GET: "roomStatus"
    ]
  }
  
  path("/room/:command") {
    action: [
      PUT: "roomUpdate"
    ]
  }
}

// Master switch event subscriptions
def installed() {
	subscribe(master, "switch.off", off)
	subscribe(master, "switch.on", on)
}

def updated() {
	unsubscribe()
	installed()
}

// URL handlers
def roomStatus() {
    return "{ 'status': '${master.currentValue("switch")}' }"
}

def roomUpdate() {
    def command = params.command

    switch(command) {
    	case "off":
            master.off()
            break
            
        case "on":
            master.on()
            break
            
        case "toggle":
            toggle()
            break
            
        default:
            httpError(404, "{ 'command': '$command', 'message': 'Command is not valid', 'status': '${master.currentValue("switch")}' }")
            return "{ }"
    }
    
    return "{ 'command': '$command', 'message': 'Success', 'status': '${master.currentValue("switch")}' }"
}

// Switch event handlers
def off(evt) {
    devicesOff.each {
    	it.off()
    }
    
    devicesToggle.each {
    	it.currentValue("switch") == "off" ? it.on() : it.off()
    }
    
    if(iftttOff != null) iftttRun(iftttOff.tokenize(","))
    if(iftttToggle != null) iftttRun(iftttToggle.tokenize(","))
}

def on(evt) {
	devicesOn.each {
    	it.on()
    }
    
    devicesToggle.each {
    	it.currentValue("switch") == "off" ? it.on() : it.off()
    }
    
    if(iftttOn != null) iftttRun(iftttOn.tokenize(","))
    if(iftttToggle != null) iftttRun(iftttToggle.tokenize(","))
}

def toggle() {
	master.currentValue("switch") == "off" ? master.on() : master.off()
}

// Helper methods
def iftttRun(list) {
    list.each {
        try {
            httpGet(it.trim()) { resp ->
				log.debug "Finished making a call to IFTTT: $it"
            }
        } catch (e) {
            log.error "Could not make a call to IFTTT: $e"
        }
    }
}
