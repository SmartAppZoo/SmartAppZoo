definition(
    name: "Virtual Thermostat Fan Control",
    namespace: "DevWithAdam",
    author: "Adam",
    description: "Control a fan in conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select dimmer for the Fan(s)... "){
		input "outlets", "capability.switchLevel", title: "Fan", multiple: true
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
	section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["heat","cool","off"]
	}
}

def installed()
{
	if(mode == "off"){
    	log.info("Mode is off")
    }
    else {
    	state.lastFanSpeed = -1
		def tempState = sensor.temperatureState.doubleValue
        evaluate(tempState, setpoint)
        subscribe(sensor, "temperature", temperatureHandler)
        log.info("Fan Control target temp of $setpoint for $mode, Installed")
    }
}

def updated()
{
	unsubscribe()
	if(mode == "off"){
    	log.info("Mode is off")
    }
    else {
    	state.lastFanSpeed = -1
		def tempState = sensor.temperatureState.doubleValue
        evaluate(tempState, setpoint)
		subscribe(sensor, "temperature", temperatureHandler)
        log.info("Fan Control target temp of $setpoint for $mode")
    }
}

def temperatureHandler(evt)
{
	evaluate(evt.doubleValue, setpoint)
}

private evaluate(currentTemp, desiredTemp)
{
	log.debug "EVALUATE($currentTemp, $desiredTemp)"
	def threshold = 0
	if (mode == "cool") {
		// air conditioner
        def delta = currentTemp - desiredTemp
		setFanLevel(delta)
	}
	else {
		// heater
        def delta = desiredTemp - currentTemp
		setFanLevel(delta)
	}
}

private setFanLevel(deltaTemp)
{
    def newLevel = 100
    if (deltaTemp > 3.5) { 
        newLevel = 100
    }
    else if (deltaTemp > 1.75) {
        newLevel = 50
    }
    else if (deltaTemp > 0) {
        newLevel = 20
    }
    else {
        newLevel = 0
    }

    if (state.lastFanSpeed != newLevel){
        outlets.setLevel(newLevel)
        state.lastFanSpeed = newLevel
    } else {
        log.debug "no change in fan speed."
    }
}

 /** 
private setFanLevel(deltaTemp)
{
	if (deltaTemp >= threshold) {
        	def newLevel = 100
            if (delta > 4) { 
            	newLevel = 100
            }
            else if (delta > 1.75) {
            	newLevel = 50
            }
            else {
            	newLevel = 20
            }
            
            if (state.lastFanSpeed != newLevel){
        		outlets.setLevel(newLevel)
                state.lastFanSpeed = newLevel
            } else {
            	log.debug "no change in fan speed."
            }
            
          if (state.lastFanState != 1) {
				outlets.on()
                state.lastFanState = 1
            } else {
            	log.debug "no change in fan state."
            }
		}
		else if (currentTemp - desiredTemp >= threshold) {
        	if (state.lastFanState != 0) {
				outlets.off()
                state.lastFanState = 0
            } else {
            	log.debug "no change in fan state."
            }
		}
}
         **/
