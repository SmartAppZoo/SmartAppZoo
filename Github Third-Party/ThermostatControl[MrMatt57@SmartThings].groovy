/**
 *  Thermostat Control
 *
 *  Author: mwwalker@gmail.com
 *  Date: 2014-01-11
 */
preferences {

    section("Thermostat to control") {
        input "thermostat", "capability.thermostat"
        input "thermoStatMode", "enum", title: "Thermostate mode", metadata:[values:["Off", "Heating","Cooling"]]
    }
    section("When we are home during the day") {
        input "locationHomeDay", "mode", title: "Mode", required: false
        input "heatingSetpointHomeDay", "number", title: "Heating Degrees Fahrenheit?", required: false
        input "coolingSetpointHomeDay", "number", title: "Cooling Degrees Fahrenheit?", required: false
    }
    section("When we are sleeping") {
        input "locationHomeSleeping", "mode", title: "Mode", required: false
        input "heatingSetpointSleeping", "number", title: "Heating Degrees Fahrenheit?", required: false
        input "coolingSetpointSleeping", "number", title: "Cooling Degrees Fahrenheit?", required: false
    }
    section("When we are away") {
        input "locationAway", "mode", title: "Mode", required: false
        input "heatingSetpointAway", "number", title: "Heating Degrees Fahrenheit?", required: false
        input "coolingSetpointAway", "number", title: "Cooling Degrees Fahrenheit?", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
    modeChanged(null)
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
    modeChanged(null)
}

def initialize() {
    subscribe(location, modeChanged)
}

def modeChanged(evt) {
    def thermoMode = thermostat.latestValue("thermostatMode")
    log.debug "New location: $location.mode, Termostat Mode: $thermoMode"
    
    if(thermoStatMode == "Off")
    {
        setThermostateMode("off")
        return
    }
    if (locationHomeDay) {
        if (location.mode == locationHomeDay) {
            if (thermoStatMode == "Heating") {
                if(heatingSetpointHomeDay) {
                    setThermostateMode("heat")
                    thermostat.setHeatingSetpoint(heatingSetpointHomeDay)
                    log.info "Set $thermostat.displayName heating point to $heatingSetpointHomeDay"
                }
            }
            else { // Cooling
                if(coolingSetpointHomeDay) {
                    setThermostateMode("cool")
                    thermostat.setCoolingSetpoint(coolingSetpointHomeDay)
                    log.info "Set $thermostat.displayName cooling point to $coolingSetpointHomeDay"
                }
            }
        }
    }
    if (locationHomeSleeping) { 
        if (location.mode == locationHomeSleeping) {
            if (thermoStatMode == "Heating") {
                if(heatingSetpointSleeping) {
                    setThermostateMode("heat")
                    thermostat.setHeatingSetpoint(heatingSetpointSleeping)
                    log.info "Set $thermostat.displayName heating point to $heatingSetpointSleeping"
                }
            }
            else { // Cooling
                if(coolingSetpointSleeping) {
                    setThermostateMode("cool")
                    thermostat.setCoolingSetpoint(coolingSetpointSleeping)
                    log.info "Set $thermostat.displayName cooling point to $coolingSetpointSleeping"
                }
            }
        }
    }
    if (locationAway) { 
        if (location.mode == locationAway) {
            if (thermoStatMode == "Heating") {
                if(heatingSetpointAway) {
                    setThermostateMode("heat")
                    thermostat.setHeatingSetpoint(heatingSetpointAway)
                    log.info "Set $thermostat.displayName heating point to $heatingSetpointAway"
                }
            }
            else { // Cooling
                if(coolingSetpointAway) {
                    setThermostateMode("cool")
                    thermostat.setCoolingSetpoint(coolingSetpointAway)
                    log.info "Set $thermostat.displayName cooling point to $coolingSetpointAway"
                }
            }
        }
    }
    thermostat.poll()
}
def setThermostateMode(modeName) {
    def thermoMode = thermostat.latestValue("thermostatMode")
    if (thermoMode  != modeName) {
        thermostat.setThermostatMode(modeName)
        log.info "Changed $thermostat.displayName to $modeName mode."
    }
}
