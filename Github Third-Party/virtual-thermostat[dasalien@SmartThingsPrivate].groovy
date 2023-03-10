definition(
    name: "Virtual Thermostat",
    namespace: "acrosscable12814",
    author: "Dieter Rothhardt",
    description: "Instance of a virtual Thermostat. Uses a SmartThings temperature sensor",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH/master/logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH/master/logo.png",
	parent: "acrosscable12814:Virtual Thermostat Manager",
)

preferences {
	section("Choose a temperature sensor(s)... (If multiple sensors are selected, the average value will be used)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true
	}
	section("Only operate when contact(s) arent open (optional, leave blank to not require contact sensor)..."){
		input "motion", "capability.contactSensor", title: "Contact", required: false, multiple: true
	}
	section("Never go below this temperature: (optional)"){
		input "emergencySetpoint", "decimal", title: "Emergency Temp", required: false
	}
	section("Temperature Threshold (Don't allow heating to go above or below this amount from set temperature)") {
		input "threshold", "decimal", title: "Temperature Threshold", required: false, defaultValue: 1.0
	}
}

def installed()
{
    log.debug "running installed"
    state.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
}

def createDevice() {
    def thermostat
    def label = app.getLabel()
    

    log.debug "create device with id: pmvt$state.deviceID, named: $label" //, hub: $sensor.hub.id"
    try {
        thermostat = addChildDevice("acrosscable12814", "Virtual Thermostat Device", "pmvt" + state.deviceID, null, [label: label, name: label, completedSetup: true])
    } catch(e) {
        log.error("caught exception", e)
    }
    return thermostat
}

def shouldHeatingBeOn(thermostat) {    
    //if temperature is below emergency setpoint
    if(emergencySetpoint && emergencySetpoint > getAverageTemperature()) {
    	return true;
    }
    
	//if thermostat isnt set to heat
	if(thermostat.currentValue('thermostatMode') != "heat") {
    	return false;
    }
    
    //if any of the contact sensors are open
    if(motion) {
    	for(m in motion) {
			if(m.currentValue('contact') == "open") {
            	return false;
            }
        }
    }
    
    //average temperature across all temperateure sensors is above set point
    if (thermostat.currentValue("heatingSetpoint") - getAverageTemperature() <= threshold) {
    	return false;
    }
    
    return true;
}

def getHeatingStatus(thermostat) {    
    //if temperature is below emergency setpoint
    if(emergencySetpoint > getAverageTemperature()) {
    	return 'heating';
    }
    
	//if thermostat isnt set to heat
	if(thermostat.currentValue('thermostatMode') != "heat") {
    	return 'idle';
    }
    
    //if any of the contact sensors are open
    if(motion) {
    	for(m in motion) {
			if(m.currentValue('contact') == "open") {
            	return 'pending heat';
            }
        }
    }
    
    //average temperature across all temperateure sensors is above set point
    if (thermostat.currentValue("thermostatSetpoint") - getAverageTemperature() <= threshold) {
    	return 'idle';
    }
    
    return 'heat';
}

def getAverageTemperature() {
	def total = 0;
    def count = 0;
    
    //total all sensors temperature
	for(sensor in sensors) {
    	total += sensor.currentValue("temperature")
        thermostat.setIndividualTemperature(sensor.currentValue("temperature"), count, sensor.label)
        count++
    }
    
    //divide by number of sensors
    return total / count
}

def handleChange() {
	def thermostat = getThermostat()

	//update device
    thermostat.setHeatingStatus(getHeatingStatus(thermostat))
    thermostat.setVirtualTemperature(getAverageTemperature())
}

def getThermostat() {
	def child = getChildDevices().find {
    	d -> d.deviceNetworkId.startsWith("pmvt" + state.deviceID)
  	}
    return child
}

def uninstalled() {
    deleteChildDevice("pmvt" + state.deviceID)
}

def updated()
{
    log.debug "running updated: $app.label"
	unsubscribe()
    unschedule()
    
    //get or add thermostat
    def thermostat = getThermostat()
    if(thermostat == null) {
        thermostat = createDevice()
    }
    
    //subscribe to temperature changes
	subscribe(sensors, "temperature", temperatureHandler)
    
    //subscribe to contact sensor changes
	if (motion) {
		subscribe(motion, "contact", motionHandler)
	}
    
    //subscribe to virtual device changes
    subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)
    
    //reset some values
    thermostat.clearSensorData()
    thermostat.setVirtualTemperature(getAverageTemperature())
}

def temperatureHandler(evt)
{
    handleChange()
}

def motionHandler(evt)
{
    handleChange()
}

def thermostatTemperatureHandler(evt) {
	handleChange()
}

def thermostatModeHandler(evt) {
	handleChange()
}