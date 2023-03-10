
definition(
    name: "Virtual Thermostat Haas",
    namespace: "drandyhaas",
    author: "Andy Haas (stolen from ST)",
    description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, and control like a thermostat",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
    section("Choose a thermostat to follow... "){
		input "thermo", "capability.thermostat", title: "Thermostat"
	}
	section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
	section("Set the desired temperature offset (amount added to the setpoint from the thermostat)..."){
		input "setpointoffset", "decimal", title: "Set Temp Offset"
	}
    section("When there's been movement from (optional, leave blank to not require motion)..."){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Within this number of minutes..."){
		input "minutes", "number", title: "Minutes", required: false
	}
	section("But never go below (or above if A/C) this value with or without motion..."){
		input "emergencySetpoint", "decimal", title: "Emer Temp", required: false
	}
	section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool"]
	}
}

def installed()
{
    log.debug "installed"
	subscribe(sensor, "temperature", temperatureHandler)
    subscribe(thermo, "heatingSetpoint", heatingSetpointHandler)
    subscribe(thermo, "coolingSetpoint", coolingSetpointHandler)
	//subscribe(app, hasBeenRecentMotion)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
    state.setpoint = setpoint
    log.debug "setpoint now saved as $setpoint and ${state.setpoint} "
}

def updated()
{
    log.debug "updated"
	unsubscribe()
	installed()
}

def heatingSetpointHandler(evt)
{
    if (mode == "cool") {
      log.debug "dont care about cooling setpoint ${evt.value} since we are a cooler"
      return;
    }
    log.debug "set setpoint from ${state.setpoint} to ${evt.value} + $setpointoffset because we are a heater"
    state.setpoint = (evt.doubleValue + setpointoffset).round(0)
    log.debug "state.setpoint is now ${state.setpoint} "    
    evaluate(state.lasttemp, state.setpoint) // check if we should turn on or off now
}

def coolingSetpointHandler(evt)
{
    if (mode == "heat") {
      log.debug "dont care about cooling setpoint ${evt.value} since we are a heater"
      return;
    }
    log.debug "set setpoint from ${state.setpoint} to ${evt.value} + $setpointoffset because we are a cooler"
    state.setpoint = (evt.doubleValue + setpointoffset).round(0)
    log.debug "state.setpoint is now ${state.setpoint} "
    evaluate(state.lasttemp, state.setpoint) // check if we should turn on or off now
}

def temperatureHandler(evt)
{
	def isActive = hasBeenRecentMotion()
	if (isActive || emergencySetpoint) {
        state.lasttemp=evt.doubleValue
		evaluate(evt.doubleValue, isActive ? state.setpoint : emergencySetpoint)
	}
	else {
		outlets.off()
	}
}

def motionHandler(evt)
{
	if (evt.value == "active") {
		def lastTemp = sensor.currentTemperature
		if (lastTemp != null) {
			evaluate(lastTemp, setpoint)
		}
	} else if (evt.value == "inactive") {
		def isActive = hasBeenRecentMotion()
		log.debug "INACTIVE($isActive)"
		if (isActive || emergencySetpoint) {
			def lastTemp = sensor.currentTemperature
			if (lastTemp != null) {
				evaluate(lastTemp, isActive ? setpoint : emergencySetpoint)
			}
		}
		else {
			outlets.off()
		}
	}
}

def evaluate(currentTemp, desiredTemp)
{
	log.debug "EVALUATE($currentTemp, $desiredTemp)"
    
    def tmode = thermo.currentThermostatMode
    log.debug("current thermo mode is $tmode")
    
	def threshold = 1.0
	if (mode == "cool") {
		// air conditioner
        if (tmode=="cool"){
        	log.debug("mode is cool and we're cooling")
			if (currentTemp - desiredTemp >= threshold) {
				outlets.on()
			}
			else if (desiredTemp - currentTemp >= threshold) {
				outlets.off()
			}
        }
	}
	else {
		// heater
		if (tmode=="heat"){
        	log.debug("mode is heat and we're heating")
			if (desiredTemp - currentTemp >= threshold) {
				outlets.on()
			}
			else if (currentTemp - desiredTemp >= threshold) {
				outlets.off()
			}
        }
	}
}

def hasBeenRecentMotion()
{
	def isActive = false
	if (motion && minutes) {
		def deltaMinutes = minutes as Long
		if (deltaMinutes) {
			def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
			log.debug "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
			if (motionEvents.find { it.descriptionText.contains("detected motion") }) {
                log.debug "Found an active one"
				isActive = true
			}
		}
	}
	else {
		isActive = true
	}
	isActive
}
