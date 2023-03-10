definition(
    name: "HVAC Automation",
    namespace: "miccrun",
    author: "Michael Chang",
    description: "HVAC Automation",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

preferences {
    page(name: "configurations", install: true, uninstall: true)
}

def configurations() {
    dynamicPage(name: "configurations", title: "Configurations") {
        section("Choose thermostat... ") {
            input "thermostat", "capability.thermostat", required: true
        }
        section("Choose temperature sensor to use instead of the thermostat's... ") {
            input "sensor", "capability.temperatureMeasurement", title: "Temperature Sensors", required: true
        }
        section("Choose outside temperature sensor... ") {
            input "outside", "capability.temperatureMeasurement", title: "Outside Temperature Sensors", required: true
        }
        section("Heating threshold..." ) {
            input "heatingThreshold", "decimal", title: "Degrees", required: true
        }
        section("Cooling threshold..." ) {
            input "coolingThreshold", "decimal", title: "Degrees", required: true
        }

        section("Heat setting..." ) {
            input "heatingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Heat setting at sleep..." ) {
            input "sleepHeatingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Heat setting at away..." ) {
            input "awayHeatingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Heat setting before sleep..." ) {
            input "preSleepHeatingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Heat setting before morning..." ) {
            input "preMorningHeatingSetpoint", "decimal", title: "Degrees", required: true
        }

        section("Air conditioning setting...") {
            input "coolingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Air conditioning setting at sleep...") {
            input "sleepCoolingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Air conditioning setting at away...") {
            input "awayCoolingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Air conditioning setting before sleep...") {
            input "preSleepCoolingSetpoint", "decimal", title: "Degrees", required: true
        }
        section("Air conditioning setting before morning...") {
            input "preMorningCoolingSetpoint", "decimal", title: "Degrees", required: true
        }
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            actions.sort()
            section("Routine Bindings") {
                input "automaticRoutine", "enum", title: "Routine to change mode to automatic", options: actions, required: false
                input "offRoutine", "enum", title: "Routine to change mode to off", options: actions, required: false
                input "manualRoutine", "enum", title: "Routine to change mode to manual", options: actions, required: false
            }
        }
    }
}

def installed() {
    subscribeEvents()
    state.mode = "automatic"
}

def updated() {
    unsubscribe()
    unschedule()
    subscribeEvents()
}

def subscribeEvents() {
    subscribe(location, "mode", changedLocationMode)
    subscribe(location, "routineExecuted", routineChanged)
    subscribe(sensor, "temperature", temperatureHandler)
    subscribe(outside, "temperature", temperatureHandler)
    subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatMode", temperatureHandler)
    schedule("19 * * * * ?", temperatureHandler)
}

def changedLocationMode(evt) {
    evaluate()
}

def temperatureHandler(evt) {
    evaluate()
}

def routineChanged(evt) {
    def lastState = state.mode
    if (evt.displayName == automaticRoutine) {
        state.mode = "automatic"
    }
    else if (evt.displayName == offRoutine) {
        state.mode = "off"
    }
    else if (evt.displayName == manualRoutine) {
        state.mode = "manual"
    }
    if (lastState != state.mode) {
        log.debug("mode changed to: ${state.mode}")
        evaluate()
    }
}

private getHeatSetpoint() {
    if (location.mode == "Home" ) {
        return heatingSetpoint
    } else if (location.mode == "Night" ) {
        return sleepHeatingSetpoint
    } else if (location.mode == "Away" ) {
        return awayHeatingSetpoint
    } else if (location.mode == "PreSleep" ) {
        return preSleepHeatingSetpoint
    } else if (location.mode == "PreMorning" ) {
        return preMorningHeatingSetpoint
    }
}

private getCoolSetpoint() {
    if (location.mode == "Home" ) {
        return coolingSetpoint
    } else if (location.mode == "Night" ) {
        return sleepCoolingSetpoint
    } else if (location.mode == "Away" ) {
        return awayCoolingSetpoint
    } else if (location.mode == "PreSleep" ) {
        return preSleepCoolingSetpoint
    } else if (location.mode == "PreMorning" ) {
        return preMorningCoolingSetpoint
    }
}

private evaluate() {
    def threshold = 0.5
    def heatSetpoint = getHeatSetpoint()
    def coolSetpoint = getCoolSetpoint()

    log.debug("Evaluating: home mode: $location.mode, thermostat mode: $thermostat.currentThermostatMode, thermostat temp: $thermostat.currentTemperature, " +
        "thermostat heat setpoint: $thermostat.currentHeatingSetpoint, thermostat cool setpoint: $thermostat.currentCoolingSetpoint, " +
        "remote sensor temp: $sensor.currentTemperature, desire heat setpoint: $heatSetpoint, desire cool setpoint: $coolSetpoint, " +
        "outside temperature: $outside.currentTemperature")

    if (state.mode == "automatic") {
        if (outside.currentTemperature <= heatingThreshold) {
            // change to heating mode
            if (thermostat.currentThermostatMode != "heat") {
                thermostat.heat()
                log.debug("set to heat mode")
            }
        } else if (outside.currentTemperature >= coolingThreshold) {
            // change to cooling mode
            if (thermostat.currentThermostatMode != "cool") {
                thermostat.cool()
                log.debug("set to cool mode")
            }
        } else {
            // good weather, turn off
            if (thermostat.currentThermostatMode != "off") {
                thermostat.off()
                log.debug("turn off")
            }
        }

        if (thermostat.currentThermostatMode in ["cool","auto"]) {
            if (sensor.currentTemperature - coolSetpoint >= threshold) {
                if (thermostat.currentCoolingSetpoint != thermostat.currentTemperature - 2) {
                    thermostat.setCoolingSetpoint(thermostat.currentTemperature - 2)
                    log.debug("cooling to ${thermostat.currentTemperature - 2}")
                }
            }
            else if (coolSetpoint - sensor.currentTemperature >= 0) {
                if (thermostat.currentCoolingSetpoint != thermostat.currentTemperature + 2) {
                    thermostat.setCoolingSetpoint(thermostat.currentTemperature + 2)
                    log.debug("idle to ${thermostat.currentTemperature + 2}")
                }
            }
        }
        if (thermostat.currentThermostatMode in ["heat","emergency heat","auto"]) {
            if (heatSetpoint - sensor.currentTemperature >= threshold) {
                if (thermostat.currentHeatingSetpoint != thermostat.currentTemperature + 2) {
                    thermostat.setHeatingSetpoint(thermostat.currentTemperature + 2)
                    log.debug("heating to ${thermostat.currentTemperature + 2}")
                }
            }
            else if (sensor.currentTemperature - heatSetpoint >= 0) {
                if (thermostat.currentHeatingSetpoint != thermostat.currentTemperature - 2) {
                    thermostat.setHeatingSetpoint(thermostat.currentTemperature - 2)
                    log.debug("idle to ${thermostat.currentTemperature - 2}")
                }
            }
        }
    } else if (state.mode == "off") {
        log.debug("off mode, shut it down")
        if (thermostat.currentThermostatMode != "off") {
            thermostat.off()
            log.debug("turn off")
        }
    } else if (state.mode == "manual") {
        log.debug("manual mode, nothing to do")
    }
}
