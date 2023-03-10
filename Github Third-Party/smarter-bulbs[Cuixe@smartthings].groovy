definition(
	name: "Smarter Bulbs",
	namespace: "smartthings",
	author: "nick@sweet-stuff.cc",
	description: "Save the state of a bunch of bulbs and reset when 'Canary' bulb turns on",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)
preferences {
    section("Canary Bulb") {
        input "canary", "capability.switch", title: "Who sings?"
    }
    section("Zigbee bulbs to monitor") {    
        input "slaves","capability.switch", multiple: true
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
    subscribe(slaves, "switch", saveStates)
    subscribe(canary,"switch.on", checkRestore)
	runEvery5Minutes(checkRestore)
    saveStates()
}

def saveStates(evt) {
	if ("off" == canary.currentSwitch ) {
    	def lightsOff = [:]
    	slaves?.each {
			if (it.currentSwitch == "off"){
        	log.debug "${it.id} value ${it.currentSwitch}" 
        	lightsOff[it.id]="off"
        	}
		}
   	state.lOff = lightsOff
	}
}

def checkRestore(evt) {
   
    log.debug "Canary is ${canary.currentSwitch}"
    if ("on" == canary.currentSwitch) { 
    	log.debug "Turning stuff off"
        restoreState()
        }
    canary.off()
}

private restoreState() {
  slaves?.each {
				if (state.lOff[it.id] == "off") {
                log.debug "turning $it.label off"
				it.off()
                               }
			}
		}