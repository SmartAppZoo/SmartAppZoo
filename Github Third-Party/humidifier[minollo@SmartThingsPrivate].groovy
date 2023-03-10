/**
 *  Humidity handler
 *
 *  Author: Minollo
 */

// Automatically generated. Make future change here.
definition(
    name: "Humidifier",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Humidifier",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Choose a humidity sensor... "){
		input "sensor", "capability.relativeHumidityMeasurement", title: "Sensor"
		input "weather", "capability.relativeHumidityMeasurement", title: "Weather Station", required: false
		input "humiditySetpoint", "number", title: "Humidity Target"
		input "awayMode", "mode", title: "Away mode", required: false
	}
	section("Select the humidifier switch(es)... "){
		input "humidifiers", "capability.switch", title: "Humidifiers", multiple: true
	}
	section("Select the thermostat controlling the temperature..."){
		input "thermostat", "capability.thermostat", required: false
	}
}

def installed()
{
	subscribe(sensor, "humidity", humidityHandler)
    subscribe(location, modeChangeHandler)
    if (weather != null) {
		subscribe(weather, "humidity", weatherHumidityHandler)
    }
    if (thermostat != null) {
        subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
        subscribe(thermostat, "temperature", temperatureHandler)
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	}    
    runIn(60, initialize)
}

def updated()
{
	unsubscribe()
	subscribe(sensor, "humidity", humidityHandler)
    subscribe(location, modeChangeHandler)
    if (thermostat != null) {
        subscribe(thermostat, "heatingSetpoint", heatingSetpointHandler)
        subscribe(thermostat, "temperature", temperatureHandler)
        subscribe(thermostat, "thermostatOperatingState", operatingStateHandler)
	}    
    runIn(60, initialize)
}

def initialize() {
	log.debug "Initializing..."
	handleHumidifiers(thermostat?thermostat.currentValue("heatingSetpoint"):0, thermostat?thermostat.currentValue("temperature"):0, sensor.currentValue("humidity"), thermostat?thermostat.currentValue("thermostatOperatingState"):null, location.mode)
}

def modeChangeHandler(evt) {
	handleHumidifiers(thermostat?thermostat.currentValue("heatingSetpoint"):0, thermostat?thermostat.currentValue("temperature"):0, sensor.currentValue("humidity"), thermostat?thermostat.currentValue("thermostatOperatingState"):null, location.mode)
}

def operatingStateHandler(evt) {
	log.debug "Operating state event received: $evt.value"
	if (evt.value != "heating") {
    	log.info "Thermostat is not heating: make sure humidifiers are off"
    	humidifiers.off()
    } else {
		handleHumidifiers(thermostat.currentValue("heatingSetpoint"), thermostat.currentValue("temperature"), sensor.currentValue("humidity"), evt.value, location.mode)
    }
}

def temperatureHandler(evt) {
	log.debug "Temperature event received: ${evt}"
    if (evt.value) {
		handleHumidifiers(thermostat.currentValue("heatingSetpoint"), evt.value, sensor.currentValue("humidity"), thermostat.currentValue("thermostatOperatingState"), location.mode)
    }
}

def heatingSetpointHandler(evt) {
	log.debug "Heating setpoint event received: ${evt}"
    if (evt.value) {
		handleHumidifiers(evt.value, thermostat.currentValue("temperature"), sensor.currentValue("humidity"), thermostat.currentValue("thermostatOperatingState"), location.mode)
    }
}

def humidityHandler(evt) {
	log.debug "Humidity event received: ${evt}"
    if (evt.value) {
		handleHumidifiers(thermostat?thermostat.currentValue("heatingSetpoint"):0, thermostat?thermostat.currentValue("temperature"):0, evt.value, thermostat?thermostat.currentValue("thermostatOperatingState"):null, location.mode)
    }
    if (thermostat != null) thermostat.poll()
}

def weatherHumidityHandler(evt) {
	log.debug "Weather humidity event received: ${evt}"
    if (evt.value) {
		handleHumidifiers(thermostat?thermostat.currentValue("heatingSetpoint"):0, thermostat?thermostat.currentValue("temperature"):0, sensor.currentValue("humidity"), thermostat?thermostat.currentValue("thermostatOperatingState"):null, location.mode)
    }
}

private handleHumidifiers(setpoint, temperature, humidity, operatingState, locMode) {
	def externalTempF = getExternalTemperature()
    def adjustedHumiditySetpoint = humiditySetpoint
    if (externalTempF < 10) {
    	adjustedHumiditySetpoint -= 4
    } else if (externalTempF < 15) {
    	adjustedHumiditySetpoint -= 3
    } else if (externalTempF < 21) {
    	adjustedHumiditySetpoint -= 2
    } else if (externalTempF < 26) {
    	adjustedHumiditySetpoint -= 1
    } else if (externalTempF > 42) {
    	adjustedHumiditySetpoint += 3
    } else if (externalTempF > 36) {
    	adjustedHumiditySetpoint += 2
    } else if (externalTempF > 32) {
    	adjustedHumiditySetpoint += 1
    }
   
	log.debug "handleHumidifiers(${toDouble(setpoint)}, ${toDouble(temperature)}, ${toDouble(humidity)}, $operatingState); humidity setpoint: $humiditySetpoint; outside temp: ${externalTempF}; adjusted humidity setpoint: ${adjustedHumiditySetpoint}; location mode: ${locMode}"
    if (locMode == awayMode) {
    	log.info "Location mode is away; turn off humidifiers"
        humidifiers.off()
    } else if (thermostat != null && operatingState != "heating") {
    	log.info "Thermostat is not heating: make sure humidifiers are off"
        humidifiers.off()
    } else if (thermostat != null && toDouble(setpoint) - toDouble(temperature) < 1.0) {
    	log.info "Temperature close to setpoint; shut down humidifiers"
    	humidifiers.off()
    } else if (toDouble(humidity) - toDouble(adjustedHumiditySetpoint) > 0.5) {
    	log.info "Humidity passed setpoint; shut down humidifiers"
    	humidifiers.off()
    } else if (toDouble(adjustedHumiditySetpoint) - toDouble(humidity) > 0.5) {
    	if (humidifiers[0].currentValue("switch") != "on") {
	    	log.info "Humidity low and temperature well below setpoint; turn on humidifiers"
    		humidifiers.on()
        } else {
	    	log.info "Humidity low and temperature well below setpoint; humidifiers are already on"
        }
    } else {
    	log.info "Humidity not low and not too high; do nothing"
    }
}

private getExternalTemperature() {
	def externalTempF = 32
	try {
        externalTempF = getWeatherFeature("conditions").current_observation.temp_f
    } catch(e1) {
    	def errorInfo = "Error fetching external temperature: ${e}"
		log.error errorInfo
    }
    log.debug "External temperature: ${externalTempF}"
    externalTempF
}

private toDouble(anObj) {
	if (anObj instanceof String) {
    	Double.parseDouble(anObj)
    } else {
    	anObj
    }
}

