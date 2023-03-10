/**
 *  Nest Status Monitor
 *
 *  Copyright 2016 Jon Scheiding
 *
 */
definition(
    name: "Nest Status Monitor",
    namespace: "jonscheiding",
    author: "Jon Scheiding",
    description: "Nest Status Monitor",
    category: "",
    iconUrl: "https://png.icons8.com/material/30/000000/thermometer-automation.png",
    iconX2Url: "https://png.icons8.com/material/60/000000/thermometer-automation.png",
    iconX3Url: "https://png.icons8.com/material/90/000000/thermometer-automation.png")


preferences {
	section("Title") {
		input "thermostat", "capability.thermostatOperatingState", title: "Thermostat", required: true
	}
}

def getDisplayTime(seconds) {
	def minutes = (seconds / 60).toInteger()
    def hours = (minutes / 60).toInteger()
    
	if(seconds < 60) return "${seconds} seconds"
    if(minutes == 1) return "1 minute"
    if(minutes < 60) return "${minutes} minutes"
    if(hours == 1) return "1 hour"
    return "${hours} hours"
}

def onTemperatureChange(evt) {
    def timeInCurrentStateThreshold = 15 * 60
    def timeInCurrentState = (now() - state.timeEnteredCurrentState) / 1000
    
    log.info "Current temperature is ${thermostat.currentTemperature}."
    
    def warn = false
    switch(thermostat.currentThermostatOperatingState) {
    	case "heating":
        	warn = (thermostat.currentTemperature < state.lastTemperature)
        	break
    	case "cooling":
        	warn = (thermostat.currentTemperature > state.lastTemperature)
            break
    }
    
    if(warn) {
    	def message = 
        	"Thermostat has been ${thermostat.currentThermostatOperatingState} " +
            "for ${getDisplayTime(timeInCurrentState)}, " +
            "but the temperature has changed " + 
            "from ${state.lastTemperature} " +
            "to ${thermostat.currentTemperature}."
            
        log.info message
        
        if(timeInCurrentState >= timeInCurrentStateThreshold) {
	        sendPush message
        }
    }
    
    state.lastTemperature = thermostat.currentTemperature

}

def onOperatingStateChange(evt) {
    log.info "Operating state changed to ${thermostat.currentThermostatOperatingState}."
    state.timeEnteredCurrentState = now()
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
	subscribe(thermostat, 'temperature', onTemperatureChange)
    subscribe(thermostat, 'thermostatOperatingState', onOperatingStateChange)

    state.lastTemperature = thermostat.currentTemperature
    state.timeEnteredCurrentState = now()
    
    log.debug state
}
