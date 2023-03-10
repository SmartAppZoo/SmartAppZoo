definition(
    name: "Split A/C Thermostat",
    namespace: "",
    author: "minollo@minollo.com",
    description: "App to handle split a/c thermostat",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Temperature control") {
        input "appEnabled", "bool", title: "Enabled?", required: true
        input "mode", "enum", metadata:[values:['cool', 'heat', 'off']], required: true
        input "outsideTemperature", "capability.temperatureMeasurement", title: "Outside Temperature", required:false
		input "heatingSetpoint", "number", title: "Heating temp (F)?", required: true
        input "maxExternalTempForHeat", "number", title: "Maximum outside temp for heating (F)?"
		input "coolingSetpoint", "number", title: "Cooling temp (F)?", required: true
        input "minExternalTempForCool", "number", title: "Minimum outside temp for cooling (F)?"
    }
}

def installed()
{
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)
    if (outsideTemperature)
		subscribe(outsideTemperature, "temperature", outsideTemperatureHandler)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
    runIn(600, keepAlive)
    log.debug "Scheduling initialize"
    runIn(60, initialize)
}

def updated()
{
	unsubscribe()
    unschedule()
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "coolingSetpoint", coolingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)
    if (outsideTemperature)
		subscribe(outsideTemperature, "temperature", outsideTemperatureHandler)
    if (pollerDevice) {
        subscribe(pollerDevice, "battery", pollerEvent)
    }
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
    runIn(600, keepAlive)
    log.debug "Scheduling initialize"
    runIn(60, initialize)
}

def pollerEvent(evt) {
	log.debug "[PollerEvent]"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 900000) {
    	log.error "Waking up timer"
    	keepAlive()
    }
}

def initialize() {
	log.debug "Initializing"
    doUpdateTempSettings(location.mode)
}

def heatingSetpointHandler(evt)
{
	log.debug "heatingSetpoint: $evt.value, $settings"
}

def coolingSetpointHandler(evt)
{
	log.debug "coolingSetpoint: $evt.value, $settings"
}

def temperatureHandler(evt)
{
	log.debug "temperature: $evt.value, $settings"
}

def outsideTemperatureHandler(evt)
{
	if(appEnabled) {
        log.debug "Outside temperature: $evt.value, $settings; thermostatMode == ${thermostat.currentValue("thermostatMode")}"
        if (state.lastChangeTime && (now() - state.lastChangeTime) < 3600000) {	//no more than one change every hour
            log.warn "Ignoring event as state was changed less than one hour ago"
        } else if (mode == "cool" && toDouble(evt.value) < (toDouble(minExternalTempForCool)) && thermostat.currentValue("thermostatMode") != "off") {
            log.info "Outside temperature below minimum set temperature; shutting down unit"
            state.lastChangeTime = now()
            thermostat.off()
            thermostat.poll()
        } else if (mode == "heat" && toDouble(evt.value) > toDouble(maxExternalTempForHeat) && thermostat.currentValue("thermostatMode") != "off") {
            log.info "Outside temperature above maximum set temperature; shutting down unit"
            state.lastChangeTime = now()
            thermostat.off()
            thermostat.poll()
        } else if (mode == "cool" && toDouble(evt.value) >= (toDouble(minExternalTempForCool) + 1) && thermostat.currentValue("thermostatMode") == "off") {
            log.info "Outside temperature in working range; resuming unit to cool"
            state.lastChangeTime = now()
            thermostat.on()
            thermostat.poll()
        } else if (mode == "heat" && toDouble(evt.value) <= (toDouble(maxExternalTempForHeat) - 1) && thermostat.currentValue("thermostatMode") == "off") {
            log.info "Outside temperature in working range; resuming unit to heat"
            state.lastChangeTime = now()
            thermostat.on()
            thermostat.poll()
        }
	}
}

def thermostatModeHandler(evt)
{
	log.debug "thermostatModeHandler: $evt.value, $settings"
}

def changedLocationMode(evt)
{
    log.debug "Now it's ${new Date(now())}"
	log.debug "changedLocationMode: $evt, $settings"
}

def doUpdateTempSettings(currentMode)
{
	if(appEnabled) {
        log.debug "doUpdateTempSettings; currentMode == ${currentMode}"
        def tMode = thermostat.currentValue("thermostatMode")
        log.debug "Thermostat Mode is '${tMode}'"
        if (mode == "cool") {
            log.debug "Setting to cool at ${coolingSetpoint}F"
            if (tMode == "off") thermostat.on()
            thermostat.setCoolingSetpoint(coolingSetpoint)
    //    	thermostat.cool()
        } else if (mode == "heat") {
            log.debug "Setting to heat at ${heatingSetpoint}F"
            if (tMode == "off") thermostat.on()
            thermostat.setHeatingSetpoint(heatingSetpoint)
    //        thermostat.heat()
        } else {
            if (tMode != "off") {
                log.debug "Shutting down"
                thermostat.off()
            }
        }
	}
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
	doUpdateTempSettings(location.mode)
}

def keepAlive()
{
	log.debug "keepAlive"
    runIn(600, keepAlive)
    state.keepAliveLatest = now()
    thermostat.poll()
}

// catchall
def event(evt)
{
//	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}

private toDouble(anObj) {
	if (anObj instanceof String) {
    	Double.parseDouble(anObj)
    } else {
    	anObj
    }
}
