
/**
 *  Virtual Thermostat
 *
 *  Copyright 2016 kenobob
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Better Virtual Thermostat",
    namespace: "kenobob",
    author: "kenobob",
    description: "A virtual thermostat for homes not compatible with Nest. Get enhanced control over your heating and cooling devices with temperature readings from multiple sensors, mode-based thermostat settings, and if you have a humidity sensor, feels-like temperatures, windows and other smart features. Based on the original Virtual Thermostat, Green Thermostat, and Anmnguyen's apps.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
    page(name: "modePage", title:"Mode", nextPage:"thermostatPage", uninstall: true) {
        section () {
            paragraph "Let's tell the virtual thermostat what mode to be on."
            input "mode", "enum", title: "Set thermostat mode to", metadata: [values: ["Heating","Cooling"]], required:true   
        }
    }
    page(name: "thermostatPage", title:"Thermostat", nextPage: "sensitivityPage") {
        section () {
            paragraph "Let's tell the virtual thermostat about your desired temperatures."
        }
		section("Home Day Modes"){
				input "homeDayModes", "mode", title: "select a home day mode(s)", multiple: true
		}
		section("Home Night Modes"){
				input "homeNightModes", "mode", title: "select a home night mode(s)", multiple: true
		}
		section("Away Modes"){
				input "awayModes", "mode", title: "select a away mode(s)", multiple: true
		}
        section("When home during the day,") {
                input "homeHeat",  "decimal", title:"Set heating temperature to (ex. 72)", required:true
                input "homeCool",  "decimal", title:"Set cooling temperature to (ex. 76)", required:true
        }
        section("When home at night") {
            input "nightHeat", "decimal", title:"Set heating temperature to (ex. 68)", required: true
            input "nightCool", "decimal", title:"Set cooling temperature to (ex. 80)", required: true
        }
        section("When away") {
            input "awayHeat", "decimal", title:"Set heating temperature to (ex. 60)", required: true
            input "awayCool", "decimal", title:"Set cooling temperature to (ex. 85)", required: true
        }
    }
    page(name: "sensitivityPage", title:"Sensitivity", nextPage:"sensorsPage") {
        section(){
            paragraph "Let's tell the virtual thermostat how sensitive to be. Most thermostats use 2 degree changes to control switches. Smaller values lead to more consistent temperatures, but will turn switches on and off more often."
        } 
        section(){
            paragraph "Let's tell the thermostat when to turn switches on."
            input "onThreshold", "decimal", title: "Turn on when temperature is this far from setpoint", required: true
        }
        section(){
            paragraph "Let's tell the thermostat when to turn off."
            input "offThreshold", "decimal", title: "Turn off when temperature reaches & goes this far beyond setpoint", required: true
        }
    }
    page(name: "sensorsPage", title:"Sensors", nextPage: "windowsPage") {
        section(){
            paragraph "Let's tell the virtual thermostat what sensors to use. We will average the temperature between these sensors to do our calculations"
            input "temperatureSensors", "capability.temperatureMeasurement", title: "Get temperature readings from these sensors", multiple: true, required: true
            input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Get humidity readings from these sensors", multiple: true, required: false
        }
    }
    page(name: "windowsPage", title:"Windows", nextPage: "switchesPage") {
        section(){
            paragraph "Select Window Sensors that when opened should turn off your devices.."
            input "coolWindows", "capability.contactSensor", title: "Turn-off Devices When cooling", multiple: true, required: false
            input "heatWindows", "capability.contactSensor", title: "Turn-off Devices When heating ", multiple: true, required: false
			input "windowDelaySeconds", "number", title:"Seconds of Delay to evaluate and Shut off Devices", defaultValue: 30, required: true
        }
    }
    page(name: "switchesPage", title:"Switches", nextPage: "SmartThingsPage") {
        section(){
            paragraph "Let's tell the virtual thermostat what outlets to use."
            input "coolOutlets", "capability.switch", title: "Control these switches when cooling", multiple: true, required: false
            input "heatOutlets", "capability.switch", title: "Control these switches when heating ", multiple: true, required: false
        }
    }
    page(name: "SmartThingsPage", title: "Name app and configure modes", install: true, uninstall: true) {
        section() {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def installed()
{
    logtrace("Executing 'installed'")
    log.debug("Installed with settings: ${settings}")
    initialize()
    logtrace("End Executing 'installed'")
}

def updated()
{
    logtrace("Executing 'updated'")
    log.debug("Updated with settings: ${settings}")
    unsubscribe()
    initialize()
    logtrace("End Executing 'updated'")
}

def initialize()
{
    logtrace("Executing 'initialize'")
	//Subscribe to Sensor Changes
    for (sensor in temperatureSensors)
	{
        subscribe(sensor, "temperature", evtHandler)
	}
    for (sensor in humiditySensors)
	{
        subscribe(sensor, "humidity", evtHandler)
	}
	for (sensor in coolWindows)
	{
		log.debug("Subscribing Windows")
        subscribe(sensor, "contact", contactChangeEventHandler)
	}
	for (sensor in heatWindows)
	{
        subscribe(sensor, "contact", contactChangeEventHandler)
	}
	
	//Subscribe to mode changing
	subscribe(location, "mode", modeChangeHandler)
	
	//Subscribe to Smart App changes
    subscribe(app, appTouch)
    
    def temp = getReadings("temperature")
    log.debug("Temp: $temp")

    def humidity = getReadings("humidity")
    log.debug("Humidity: $humidity")

    def feelsLike = getFeelsLike(temp, humidity)
    log.debug("Feels Like: $feelsLike")
	
    //Keeps track of whether or not the outlets are turned on by the app. This is to 
    //prevent sending too many commands if it is taking awhile to cool or heat. Note: If  
    //the user manually  changes the state of an outlet, it will stay in that state until 
    //the threshold is triggered again.
	//TODO: Need to make this user configurable to capture outlet state-change for this
	state.outlets = ""
    
    setSetpoint(feelsLike)
	
    logtrace("End Executing 'initialize'")
}

//Function getReadings: Gets readings from sensors and averages
def double getReadings(type)
{
    logtrace("Executing 'getReadings'")
    def currentReadings = 0
    def numSensors = 0
    
    def sensors = temperatureSensors
    if (type == "humidity")
	{
        sensors = humiditySensors
	}

    for (sensor in sensors)
    {
        if (sensor.latestValue(type) != null)
        {
            currentReadings = currentReadings + sensor.latestValue(type)
			log.debug("${type} Sensor Readings: ${sensor.latestValue(type)}")
            numSensors = numSensors + 1
        }
    }
    //Only average if there are multiple readings and sensors
    if (currentReadings != 0 && numSensors != 0)
    {
        currentReadings = currentReadings / numSensors
    }
    
    logtrace("End Executing 'getReadings'")
    return currentReadings
}

//Function getFeelsLike: Calculates feels-like temperature based on Wikipedia formula
def double getFeelsLike(t,h)
{
    logtrace("Executing 'getFeelsLike'")
    //Formula is derived from NOAA table for temperatures above 70F. Only use the formula when temperature is at least 70F and humidity is greater than zero (same as at least one humidity sensor selected)
    if (t >=70 && h > 0) {
        def feelsLike = 0.363445176 + 0.988622465*t + 4.777114035*h -0.114037667*t*h - 0.000850208*t**2 - 0.020716198*h**2 + 0.000687678*t**2*h + 0.000274954*t*h**2
        log.debug("Feels Like Calc: $feelsLike")
        //Formula is an approximation. Use the warmer temperature.
        if (feelsLike > t)
		{
            return feelsLike
		}
        else
		{
            return t
		}
    }
    else
	{
        return t
	}
}

//Function setSetpoint: Determines the setpoints based on mode
def setSetpoint(temp)
{
    logtrace("Executing 'setSetpoint'")
	def rtvHomeDayModes = homeDayModes.findAll{ mde -> mde==location.mode }
	def rtvHomeNightModes = homeNightModes.findAll{ mde -> mde==location.mode }
	def rtvAwayModes = awayModes.findAll{ mde -> mde==location.mode }
	
    if (rtvHomeDayModes) {
		log.info("Home Day Mode for Virtual Thermostat")
        evaluate(temp, homeHeat, homeCool)
    }
    if (rtvHomeNightModes) {
		log.info("Home Night Mode for Virtual Thermostat")
        evaluate(temp, nightHeat, nightCool)
    }
    if (rtvAwayModes) {
		log.info("Away Mode for Virtual Thermostat")
        evaluate(temp, awayHeat, awayCool)
    }
	
    logtrace("End Executing 'setSetpoint'")
}

def contactChangeEventHandler(evt)
{
    logtrace("Executing 'contactChangeEventHandler'")
	def isWindowOpen = false;
	
    if (mode == "Cooling") {
		isWindowOpen = coolWindows.currentContact.find{it == "open"} ? true : false
	} else {
		isWindowOpen = heatWindows.currentContact.find{it == "open"} ? true : false
	}
	
	log.debug("Window Open: ${isWindowOpen}")
	
	if(!state.waitingForWindowDelayTime && isWindowOpen){
		//kick off window wait time
		scheduleRunIn()
	} 
	else if(state.waitingForWindowDelayTime && !isWindowOpen){
		log.debug("Waiting for delay time, but the window isn't open")
	}else if(!state.waitingForWindowDelayTime && !isWindowOpen){
		scheduleRunIn()
		log.debug("Not currently waiting for assessment, and window closed")
	} else if(state.waitingForWindowDelayTime && isWindowOpen){
		log.debug("Waiting for delay time, and the window is open")
	}else{
		log.warn("Missed something in Contact Event Handler")
	}
	
    logtrace("End Executing 'contactChangeEventHandler'")
}

private def scheduleRunIn(){
    logtrace("Executing 'scheduleRunIn'")
	if(canSchedule()){
		try{
			state.waitingForWindowDelayTime = true
			runIn(windowDelaySeconds, contactChangeAfterWait, [overwrite: true])
			log.debug("Creating runIN event for ${windowDelaySeconds} seconds")
		}catch(all){
			log.error(all)
		}
	} else {
		log.error("Cannot schedule anymore events.");
		contactChangeAfterWait(evt);
	}
    logtrace("End Executing 'scheduleRunIn'")
}

def contactChangeAfterWait(evt){
    logtrace("Executing 'contactChangeAfterWait'")
	log.debug("*****Evaluating Window State After Delay****")
	def isWindowOpen = false;
	if (mode == "Cooling") {
        // Cooling
		isWindowOpen = coolWindows.currentContact.find{it == "open"} ? true : false
    }
    else {
        // Heating
		isWindowOpen = heatWindows.currentContact.find{it == "open"} ? true : false		
    }
	log.info("Windows Open: ${isWindowOpen}")
		
	if(isWindowOpen){
		//Windows Opened Up, turn off
		state.offForWindows = true
		log.debug("Window Opened, turning off")
	} else {
		state.offForWindows = false
	}
	
	//No longer waiting for this even to fire :)
	state.waitingForWindowDelayTime = false;
	
	//Send to standard event handler
	evtHandler("Window State Change Timer Event")
    logtrace("End Executing 'contactChangeAfterWait'")
}

//Function evtHandler: Main event handler
def evtHandler(evt)
{
    logtrace("Executing 'evtHandler'")
	log.debug("Event: ${evt}")
    def temp = getReadings("temperature")
    log.info ("Temp: $temp")

    def humidity = getReadings("humidity")
    log.info("Humidity: $humidity")

    def feelsLike = getFeelsLike(temp, humidity)
    log.info("Feels Like: $feelsLike")

    setSetpoint(feelsLike)
    logtrace("End Executing 'evtHandler'")
}

//Function modeChangeHandler: Event handler when mode is changed
def modeChangeHandler(evt)
{
    logtrace("Executing 'modeChangeHandler'")
    log.info("modeChangeHandler: $evt, $settings")
    evtHandler(evt)
    logtrace("End Executing 'modeChangeHandler'")
}

//Function appTouch: Event handler when SmartApp is touched
def appTouch(evt)
{
    logtrace("Executing 'appTouch'")
    log.info("appTouch: $evt, $lastTemp, $settings")
    evtHandler(evt)
    logtrace("End Executing 'appTouch'")
}

//Function evaluation: Evaluates temperature and control outlets
private evaluate(currentTemp, desiredHeatTemp, desiredCoolTemp)
{
    log.debug ("Evaluating temperature ($currentTemp, $desiredHeatTemp, $desiredCoolTemp, $mode)")
	
		
    if (mode == "Cooling") {
        // Cooling
		if(state.offForWindows){
			//Windows Opened Up, turn off
			if(state.outlets != "off"){
				coolOutlets.off()
				state.outlets = "off"
				log.debug("Window Open: Turning outlets off")
			} else {
				log.debug("Window Open: No need to change outlet state")
			}			
		}else if (currentTemp - desiredCoolTemp >= onThreshold && state.outlets != "on" && !state.offForWindows) {
            coolOutlets.on()
            state.outlets = "on"
            log.debug("Need to cool: Turning outlets on")
        }
        else if (desiredCoolTemp - currentTemp >= offThreshold && state.outlets != "off") {
            coolOutlets.off()
            state.outlets = "off"
            log.debug("Done cooling: Turning outlets off")
        } else {
			log.info("No Change. Current Temp ${currentTemp}, Desired Temp: ${desiredCoolTemp}, outlet state ${state.outlets}")
		}
    }
    else {
        // Heating
		if(state.offForWindows){
			//Windows Opened Up, turn off
			if(state.outlets != "off"){
				heatOutlets.off()
				state.outlets = "off"
				log.debug("Window Open: Turning outlets off")
			} else {
				log.debug("Window Open: No need to change outlet state")
			}			
		}else if (desiredHeatTemp - currentTemp >= onThreshold && state.outlets != "on" && !state.offForWindows) {
            heatOutlets.on()
            state.outlets = "on"
            log.debug("Need to heat: Turning outlets on")
        }
        else if (currentTemp - desiredHeatTemp >= offThreshold && state.outlets != "off") {
            heatOutlets.off()
            state.outlets = "off"
            log.debug("Done heating: Turning outlets off")
        } else {
			log.info("No Change. Current Temp ${currentTemp}, Desired Temp: ${desiredCoolTemp}, outlet state ${state.outlets}")
		}
    }
}

private def logtrace(message){
	if(false){
		log.trace(message)
	}
}

// Event catchall
def event(evt)
{
    log.info("value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}")
}
