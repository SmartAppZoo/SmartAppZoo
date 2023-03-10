/**
 *  Keep Me Cozy (cooling removed)
 *
 *  Author: SmartThings
 */
definition(
    name: "Set Heating Temperature",
    namespace: "dadlersan",
    author: "David",
    description: "Set the heating temperature for the thermostat",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@3x.png")

preferences {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heat setting...") {
		input "heatingSetpoint", "number", title: "Degrees Fahrenheit?"
	}
}

def installed()
{
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(location)
	subscribe(app)
}

def updated()
{
	unsubscribe()
	subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
	subscribe(thermostat, "temperature", temperatureHandler)
	subscribe(location)
	subscribe(app)
}

def heatingSetpointHandler(evt)
{
	log.debug "heatingSetpoint: $evt, $settings"
}

def temperatureHandler(evt)
{
	log.debug "currentTemperature: $evt, $settings"
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode: $evt, $settings"

	thermostat.setHeatingSetpoint(heatingSetpoint)
	thermostat.poll()
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"

	thermostat.setHeatingSetpoint(heatingSetpoint)
	thermostat.poll()
}

// catchall
def event(evt)
{
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}
