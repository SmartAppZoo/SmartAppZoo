/**
*  Precise Change Thermostat
*  Allows precise thermostat control so you can enter exact temperature
*/

definition(
    name: "Precise Thermostat Control",
    namespace: "automaton",
    author: "automaton",
    description: "Allows precise thermostat control so you can enter exact temperature",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/comfort.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/comfort@2x.png",
)

preferences {
    page(name: "page1", title: "Select thermostat", nextPage: "page2", uninstall: true) {
        section("Set this thermostat") {
            input "thermostat", "capability.thermostat", title: "Which?"
        }
    }

    page(name: "page2", title: "Options", install: true, uninstall: true)
}

def page2() {
    dynamicPage(name: "page2") {
        section("Temperature") {
            input "heatingSetpoint", "decimal", title: "When Heating", defaultValue: thermostat.currentHeatingSetpoint
            input "coolingSetpoint", "decimal", title: "When Cooling", defaultValue: thermostat.currentCoolingSetpoint
        }

        section("Mode") {
            input "thermostatMode", "enum",
                title: "What thermostat mode?",
                required: true,
                options: [
                    'auto',
                    'cool',
                    'heat',
                    'off'
                ],
				defaultValue: thermostat.currentThermostatMode
        }

        section("Fan Mode") {
            input "thermostatFanMode", "enum",
                title: "What fan mode?",
                required: true,
                options: [
                    'auto',
                    'circulate',
                    'on'
                ],
                defaultValue: thermostat.currentThermostatFanMode
        }
    }
}

def installed() {
    log.debug "Installed called with $settings"
    initialize()
}

def updated() {
    log.debug "Updated called with $settings"
    unschedule()
    initialize()
}

def initialize() {
    setTheTemp()
}

def setTheTemp() {

	// Update both temperatures regardless.
    thermostat.setHeatingSetpoint(heatingSetpoint)
    thermostat.setCoolingSetpoint(coolingSetpoint)
    log.debug "$thermostat heat set to '${heatingSetpoint}', cool to '${coolingSetpoint}'"
    
    // Update mode if it differs from original.
    if (thermostatMode != thermostat.currentThermostatMode) {
        if (thermostatMode == 'auto') {
            thermostat.auto()
        } else if (thermostatMode == 'cool') {
            thermostat.cool()
        } else if (thermostatMode == 'heat') {
            thermostat.heat()
        } else if (thermostatMode == 'off') {
            thermostat.off()
        }

        log.debug "$thermostat mode change to ${thermostatMode}"
    }

    // Update fan mode if it differs from original.
    if (thermostatFanMode != thermostat.currentThermostatFanMode) {
        if (thermostatFanMode == 'auto') {
            thermostat.fanAuto()
        } else if (thermostatFanMode == 'circulate') {
            thermostat.fanCirculate()
        } else if (thermostatFanMode == 'on') {
            thermostat.fanOn()
        }

        log.debug "$thermostat fan mode change to ${thermostatFanMode}"
    }
}