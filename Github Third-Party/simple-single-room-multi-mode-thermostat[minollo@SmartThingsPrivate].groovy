/**
 *  Virtual Thermostat
 *
 *  Author: SmartThings
 */

// Automatically generated. Make future change here.
definition(
    name: "Simple single room, multi-mode thermostat",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Set heat temperatures for two modes, plus emergency for any other mode",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Person(s) who typically occupies this room") {
		input "roomOwners", "capability.presenceSensor", multiple: true
	}
    section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Mode #1 temperature") {
		input "mode1", "mode", title: "Mode(s) #1?", multiple: true
	}
	section("Set the desired temperature in mode #1..."){
		input "setpoint1", "decimal", title: "Set Temp"
	}
	section("Mode #2 temperature") {
		input "mode2", "mode", title: "Mode(s) #2?", multiple: true
	}
	section("Set the desired temperature in mode #2..."){
		input "setpoint2", "decimal", title: "Set Temp"
	}
	section("Never go below this value in any mode..."){
		input "emergencySetpoint", "decimal", title: "Emer Temp", required: false
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

private initialize() {
	subscribe(sensor, "temperature", temperatureHandler)
	subscribe(roomOwners, "presence", presenceHandler)    
    subscribe(location, modeHandler)
	subscribe(app, appTouch)
    state.bManualPresence = false
	evaluate(sensor.currentValue("temperature"))
}

def presenceHandler(evt)
{
	evaluate(sensor.currentValue("temperature"))
}

def temperatureHandler(evt)
{
	evaluate(evt.doubleValue)
}

def modeHandler(evt)
{
	evaluate(sensor.currentValue("temperature"))
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
    state.bManualPresence = true
	evaluate(sensor.currentValue("temperature"))
}

private evaluate(value)
{
	def bEveryoneIsAway = everyoneIsAway()
    if (bEveryoneIsAway != state.bCurrentPresence) {
    	state.bManualPresence = false
        state.bCurrentPresence = bEveryoneIsAway
    }
	log.debug "Evaluating on mode '${location.mode}' for temperature '${value}'; owner is away: ${bEveryoneIsAway}; manual presence: ${state.bManualPresence}"
    if (bEveryoneIsAway && !state.bManualPresence)
    	if (emergencySetpoint)
    		setTemperature(value, emergencySetpoint)
    	else
			outlets.off()
	else if(state.bManualPresence)
		setTemperature(value, [setpoint1, setpoint2].max())
    else if (mode1.contains(location.mode))
		setTemperature(value, setpoint1)
	else if (mode2.contains(location.mode))
    	setTemperature(value, setpoint2)
    else if (emergencySetpoint)
    	setTemperature(value, emergencySetpoint)
    else
		outlets.off()
}

private setTemperature(currentTemp, desiredTemp)
{
	log.debug "EVALUATE($currentTemp, $desiredTemp)"
    if (currentTemp) {
        def threshold = 0.1
        // heater
        if ((double)desiredTemp - (double)currentTemp >= threshold) {
            outlets.on()
        }
        else if ((double)currentTemp - (double)desiredTemp >= threshold) {
            outlets.off()
        }
    }
}

private everyoneIsAway()
{
	def result = true
	for (person in roomOwners) {
		if (person.currentPresence == "present") {
			result = false
			break
		}
	}
	log.debug "everyoneIsAway: $result"
	return result
}
