definition(
    name: "Auto Dimmer",
    namespace: "oariel",
    author: "Oren Ariel",
    description: "Automatically set dimmer values accorduing to amount of ambient light",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/areas-active.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/areas-active@2x.png"
)

preferences {
    section ("Light Sensor") {
    	input "lightMeter", "capability.illuminanceMeasurement", title: "Light Meters", required: true, multiple: false
        input "luxStep", "decimal", title: "Lux step", defaultValue: 25, required: false
    }
    section("Select Dimmers to Use") {
        input "switches", "capability.switchLevel", title: "Dimmer Switches", required: true, multiple: true
        input "dependentSwitch", "capability.switch", title: "Override switch", multiple: false
        input "dependentSwitchState", "enum", title: "Switch state", options: ["on", "off"], required: false
    }
    section("When any of the following members are present") {
		input "presenceSensors", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: true
	}
}

def installed() {
    log.debug "Installed with settings: ${settings} DelayMin: $DelayMin"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}  DelayMin: $DelayMin"
    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize()"
    subscribe(lightMeter, "illuminance", handleLuxChange)
}

def handleLuxChange(evt) {
    state.luxMeasurement = evt.integerValue
    log.debug "Current lux is $state.luxMeasurement"
    
    log.debug "dependentSwitchState is $dependentSwitchState"
    log.debug "luxStep is $luxStep"
    
    if (dependentSwitch.currentValue("switch") == dependentSwitchState)
    	return
        
    def presentCounter = 0
    presenceSensors.each {
    	if (it.currentValue("presence") == "present") {
        	presentCounter++
        }
    }
    
    if ( presentCounter > 0 ) {
        if (state.luxMeasurement < luxStep) {
            log.debug "Dimmers to 100%"
            switches?.setLevel(100)
        }

        if (state.luxMeasurement >= luxStep && state.luxMeasurement < 2*luxStep) { 
            log.debug "Dimmers to 75%"
            switches?.setLevel(75)
        }

        if (state.luxMeasurement >= 2*luxStep && state.luxMeasurement < 3*luxStep) { 
            log.debug "Dimmers to 50%"
            switches?.setLevel(50)
        }
        
        if (state.luxMeasurement >= 3*luxStep && state.luxMeasurement < 4*luxStep) { 
            log.debug "Dimmers to 25%"
            switches?.setLevel(25)
        }

        if (state.luxMeasurement >= 4*luxStep) {
            log.debug "Dimmers to 0%"
            switches?.setLevel(0)
        }
     }
     else {
        log.debug "No-one is present"
     }
}

private getLabel() {
	app.label ?: "SmartThings"
}