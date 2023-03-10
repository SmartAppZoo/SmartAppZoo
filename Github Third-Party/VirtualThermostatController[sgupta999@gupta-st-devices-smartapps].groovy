/**
 *  Virtual Thermostat Controller App to Create / Control a Virtual Thermostat Device
 * 				Thermostat Interface to Control Heating and Cooling device(s) in 
 *				conjunction with any temperature and humidity sensor(s)
 *
 *  Author
 *	 - sandeep gupta
 *
 *  Copyright 2019
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: "Virtual Thermostat Controller",
    namespace: "gupta/temperature",
    author: "Sandeep Gupta",
    description: "Thermostat Interface to Control Heating and Cooling device(s) in conjunction with any temperature and humidity sensor(s).",
    category: "Green Living",
    iconUrl:  "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/thermostat.jpg",
    iconX2Url:  "https://github.com/sgupta999/GuptaSmartthingsRepository/raw/master/icons/thermostat.jpg",
	parent: "gupta/temperature:Virtual Thermostat Manager",
)

preferences {	
	section("Choose Temperature & Humidity sensor(s)\nAvg will be used for multiple sensors", hideWhenEmpty: true){
		input "sensors", "capability.temperatureMeasurement", title: "Temperature Sensor(s)", multiple: true
		input "humidity", "capability.relativeHumidityMeasurement", title: "Humidity Sensor(s)", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Must be closed to heat/cool", required: false
	}	
	section("Select Heating / Cooling outlet(s)\n")  {
		paragraph "Exclusive Outlet(s) control - for manual on / off reverts  back to thermostat state"
		input "control", "bool", title: "Exclusive Control", required: false, defaultValue: 'false'
		input "heaters", "capability.switch", title: "Heating Outlet(s)", required: false, multiple: true
		input "coolers", "capability.switch", title: "Cooling Outlet(s)", required: false, multiple: true
	}
	section("Operating Parameters") {
		input(name: "units", type: "enum", options: [["F":"Fahrenheit(°F)"],["C":"Centigrade(°C)"]], 
			 title: "Units (°C / °F)", required: true, multiple: false, defaultValue: "F")
		paragraph "Maintain heating/cooling within this amount from set temp"
		input "variance", "decimal", title: "Threshold", required: false, defaultValue: 1
		input "emergencySetpoint", "decimal", title: "Emergency Temperature to maintain", required: false, defaultValue: '50'
	}
}

def installed()
{
    log.debug "running installed"
    atomicState.deviceID = "gupta-"+(new Date()).getTime()+ "-" + Math.abs(new Random().nextInt() % 9999) + 1
	atomicState.hubID = settings.sensors[0].hub.id
    atomicState.contact = true
    atomicState.todayTime = 0
    atomicState.yesterdayTime = 0
    atomicState.date = new Date().format("dd-MM-yy", location.timeZone)
    atomicState.lastOn = 0
}

def createDevice() {
    def thermostat
    def label = app.getLabel()
	app.updateLabel(label+" Controller")
    log.debug "create device with id: deviceID, named: $label"
    try {
        thermostat = addChildDevice("gupta/temperature", "Virtual Thermostat", atomicState.deviceID, atomicState.hubID, [label: label, name: label, completedSetup: true])
    } catch(e) {
        log.error("caught exception", e)
    }
    return thermostat
}

def getThermostat() {
	return getChildDevice(atomicState.deviceID)
}

def uninstalled() {
	deleteChildDevice(atomicState.deviceID)
	unsubscribe()
    unschedule()
}

def updated()
{
    log.debug "running updated: $app.label, default temp scale  ${location.getTemperatureScale()}"
	unsubscribe()
    unschedule()
    def thermostat = getThermostat()
    if(thermostat == null) {
        thermostat = createDevice()
    }
    atomicState.contact = true
    if(atomicState.todayTime == null) atomicState.todayTime = 0
    if(atomicState.yesterdayTime == null) atomicState.yesterdayTime = 0
    if(atomicState.date == null) atomicState.date = new Date().format("dd-MM-yy", location.timeZone)
    if(atomicState.lastOn == null) atomicState.lastOn = 0
	subscribe(sensors, "temperature", temperatureHandler)
	if (contact) {
		subscribe(contact, "contact", contactHandler)
	}
    subscribe(thermostat, "thermostatOperatingState", thermostatOperatingStateHandler)
	thermostat.setTempScale(units)
	thermostat.setEmergencySetpoint(emergencySetpoint)
    thermostat.clearSensorData()
    thermostat.setTemperature(getAverageTemperature())	
	if (humidity != null) {
		subscribe(humidity, "humidity", humidityHandler)
		thermostat.setHumidity(getAverageHumidity() as Integer)
	}
	if (control){
		subscribe(heaters, "switch", heaterHandler)
		subscribe(coolers, "switch", coolerHandler)
	}
	schedule("0 0 * * * ?",updateTimings)
	schedule("0 0 0 * * ?",dayRollover)
}

def getAverageTemperature() {
	def total = 0;
    def count = 0;
	for(sensor in sensors) {
    	total += sensor.currentValue("temperature")
        getThermostat().setTemperatureSensor(sensor.currentValue("temperature"), count, sensor.label)
        count++
    }
    return total / count
}

def getAverageHumidity() {
	def total = 0;
    def count = 0;
	for(hums in humidity) {
    	total += hums.currentValue("humidity")
        getThermostat().setHumiditySensor(hums.currentValue("humidity"), count, hums.label)
        count++
    }
    return total / count
}

def temperatureHandler(evt){ getThermostat().setTemperature(getAverageTemperature())}

def humidityHandler(evt){ getThermostat().setHumidity(getAverageHumidity() as Integer)}

def heaterHandler(evt){ 
	//log.debug "State: ${atomicState.lastState}, control:${control} Event: ${evt.name} - ${evt.value} - ${evt.device}"	
	if ((atomicState.lastState == 'heating') && (evt.value == "off") && control)  evt.device.on() 
	else if ((atomicState.lastState != 'heating') && (evt.value == "on") && control) evt.device.off()
 }

def coolerHandler(evt){	
	if ((atomicState.lastState == 'cooling') && (evt.value == "off") && control)  evt.device.on() 
	else if ((atomicState.lastState != 'cooling') && (evt.value == "on") && control) evt.device.off()
}

def contactHandler(evt){	(evt.value == "closed") ? getThermostat().changeState("restore") :  runIn(60,recheck)}

def recheck(){
	if (contact.currentValue('contact') == 'open') {
		getThermostat().changeState("override")
		sendPush("Sensor ${contact} has been OPEN for more than 1 minute.\n${atomicState.lastState} has been disabled till open: ${contact} is closed")
		log.debug "VTC-APP: Door / Window open for more than 1 minute - turning thermostat off"
	}
}


def thermostatOperatingStateHandler(evt) {	
	atomicState.lastState = evt.value
	log.debug "APP: Operating Mode is : $atomicState.lastState"
	if ((contact.currentValue('contact') == 'open') && ['heating','cooling'].contains(evt.value)){
		sendPush("Sensor ${contact} is still OPEN. You have chosen to continue ${evt.value} with ${contact}:open ")
	}
	switch (evt.value) {			
		case 'heating':
				off(coolers)
				on(heaters)
				break;		
		case 'cooling':
				off(heaters)
				on(coolers)
				break;		
		case 'idle':
				off(coolers)
				off(heaters)
				break;		
		default:
				break;
	}
}

def on(outlets) {
	if (outlets == null) return;
    outlets.on()
    atomicState.current = "on"
	updateTimings()	
}

def off(outlets) {
	if (outlets == null) return;
    outlets.off()
	updateTimings()
    atomicState.current = "off"		
    atomicState.lastOn = 0;
}

def updateTimings() {
    def date = new Date()
    if(atomicState.current == "on") {
		if (atomicState.lastOn == 0) atomicState.lastOn = Math.round(date.getTime() / 1000)	
        atomicState.todayTime = atomicState.todayTime + Math.round(date.getTime() / 1000) - atomicState.lastOn		
		atomicState.lastOn = Math.round(date.getTime() / 1000)
		getThermostat().setTimings((int)atomicState.todayTime, (int)atomicState.yesterdayTime)
    }
}

def dayRollover(){
    def date = new Date()
    if(atomicState.date != date.format("dd-MM-yy", location.timeZone)) {
		if(atomicState.current == "on") {
			if (atomicState.lastOn == 0) atomicState.lastOn = Math.round(date.getTime() / 1000)	
			atomicState.todayTime = atomicState.todayTime + Math.round(date.getTime() / 1000) - atomicState.lastOn		
			atomicState.lastOn = Math.round(date.getTime() / 1000)
		}
        atomicState.yesterdayTime = atomicState.todayTime
        atomicState.date = date.format("dd-MM-yy", location.timeZone)
        atomicState.todayTime = 0
		getThermostat().setTimings((int)atomicState.todayTime, (int)atomicState.yesterdayTime)
    }
}