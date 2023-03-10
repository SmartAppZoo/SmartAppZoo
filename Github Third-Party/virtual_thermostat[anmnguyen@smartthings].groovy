/**
*  Virtual Thermostat - Nest without a Nest
*
*  Author: An Nguyen
*
*/
definition(
    name: "Virtual Thermostat - Nest without a Nest",
    namespace: "anmnguyen",
    author: "An Nguyen",
    description: "A virtual thermostat for homes not compatible with Nest. Get enhanced control over your heating and cooling devices with temperature readings from multiple sensors, mode-based thermostat settings, and if you have a humidity sensor, feels-like temperatures. Based on the original Virtual Thermostat and Green Thermostat apps.",
    category: "Green Living",
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
        section("When home during the day,") {
                input "homeTemp",  "decimal", title:"Set temperature to", required:true
        }
        section("When home at night") {
            input "nightTemp", "decimal", title:"Set temperature to", required: true
        }
        section("When away") {
            input "awayTemp", "decimal", title:"Set temperature to", required: true
        }
    }
    page(name: "sensitivityPage", title:"Sensitivity", nextPage:"sensorsPage") {
        section(){
            paragraph "Let's tell the virtual thermostat how sensitive to be to temperature changes. The more sensitive the thermostat, the more it will turn on and off the outlets."
            input "sensitivity", "enum", title: "Thermostat should keep temperature should be within ", metadata: [values: ["1 degree","2 degrees", "3 degrees"]], required:true   
        } 
    }
    page(name: "sensorsPage", title:"Sensors", nextPage: "switchesPage") {
        section(){
            paragraph "Let's tell the virtual thermostat what sensors to use."
            input "temperatureSensors", "capability.temperatureMeasurement", title: "Get temperature readings from these sensors", multiple: true, required: true
            input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Get humidity readings from these sensors", multiple: true, required: false
        }
    }
    page(name: "switchesPage", title:"Switches", nextPage: "SmartThingsPage") {
        section(){
            paragraph "Let's tell the virtual thermostat what outlets to use."
            input "coolOutlets", "capability.switch", title: "Control these switches when cooling", multiple: true
            input "heatOutlets", "capability.switch", title: "Control these switches when heating ", multiple: true
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
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize()
{
    for (sensor in temperatureSensors)
        subscribe(sensor, "temperature", evtHandler)
    for (sensor in humiditySensors)
        subscribe(sensor, "humidity", evtHandler)
    subscribe(location, changedLocationMode)
    subscribe(app, appTouch)
    subscribe(heatOutlets, "switch", switchHandler)
    subscribe(coolOutlets, "switch", switchHandler)
    
    def temp = getReadings("temperature")
    log.debug "Temp: $temp"

    def humidity = getReadings("humidity")
    log.debug "Humidity: $humidity"   

    def feelsLike = getFeelsLike(temp, humidity)
    log.debug "Feels Like: $feelsLike"       
    
    setSetpoint(feelsLike)
}

//Function getReadings: Gets readings from sensors and averages
def double getReadings(type)
{
    def currentReadings = 0
    def numSensors = 0
    
    def sensors = temperatureSensors
    if (type == "humidity")
        sensors = humiditySensors

    for (sensor in sensors)
    {
        if (sensor.latestValue(type) != null)
        {
            currentReadings = currentReadings + sensor.latestValue(type)
            numSensors = numSensors + 1
        }
    }
    //Only average if there are multiple readings and sensors
    if (currentReadings != 0 && numSensors != 0)
    {
        currentReadings = currentReadings / numSensors
    }
    
    return currentReadings
}

//Function getFeelsLike: Calculates feels-like temperature based on Wikipedia formula
def double getFeelsLike(t,h)
{
    //Formula is derived from NOAA table for temperatures above 70F. Only use the formula when temperature is at least 70F and humidity is greater than zero (same as at least one humidity sensor selected)
    if (t >=70 && h > 0) {
        def feelsLike = 0.363445176 + 0.988622465*t + 4.777114035*h -0.114037667*t*h - 0.000850208*t**2 - 0.020716198*h**2 + 0.000687678*t**2*h + 0.000274954*t*h**2
        log.debug("Feels Like Calc: $feelsLike")
        //Formula is an approximation. Use the warmer temperature.
        if (feelsLike > t)
            return feelsLike
        else
            return t
    }
    else
        return t
}

//Function setSetpoint: Determines the setpoints based on mode
def setSetpoint(temp)
{
    if (location.mode == "Home" ) {
        evaluate(temp, homeTemp)
    }
    if (location.mode == "Away" ) {
        evaluate(temp, awayTemp)
    }
    if (location.mode == "Night" ) {
        evaluate(temp, nightTemp)
    }
}

//Function evtHandler: Main event handler
def evtHandler(evt)
{
    def temp = getReadings("temperature")
    log.info "Temp: $temp"

    def humidity = getReadings("humidity")
    log.info "Humidity: $humidity"

    def feelsLike = getFeelsLike(temp, humidity)
    log.info "Feels Like: $feelsLike"

    setSetpoint(feelsLike)
}

//Function changedLocationMode: Event handler when mode is changed
def changedLocationMode(evt)
{
    log.info "changedLocationMode: $evt, $settings"
    evtHandler(evt)
}

//Function appTouch: Event handler when SmartApp is touched
def appTouch(evt)
{
    log.info "appTouch: $evt, $lastTemp, $settings"
    evtHandler(evt)
}

//Function evaluate: Evaluates temperature and control outlets
private evaluate(currentTemp, desiredTemp)
{
    log.debug "Evaluating temperature ($currentTemp, $desiredTemp, $mode)"
    def onThreshold = 1
    def offThreshold = 1
    switch (sensitivity) {
        case "2 degrees":
            onThreshold = 2
            offThreshold = 2
            break
        case "3 degrees":
            onThreshold = 3
            offThreshold = 3
            break
    }

    if (mode == "Cooling") {
        // Cooling
        if (currentTemp - desiredTemp >= onThreshold && state.outlets != "on") {
            coolOutlets.on()
            log.debug "Need to cool: Turning outlets on"
        }
        else if (desiredTemp - currentTemp >= offThreshold && state.outlets != "off") {
            coolOutlets.off()
            log.debug "Done cooling: Turning outlets off"
        }
    }
    else {
        // Heating
        if (desiredTemp - currentTemp >= onThreshold && state.outlets != "on") {
            heatOutlets.on()
            log.debug "Need to heat: Turning outlets on"
        }
        else if (currentTemp - desiredTemp >= offThreshold && state.outlets != "off") {
            heatOutlets.off()
            log.debug "Done heating: Turning outlets off"
        }
    }
}

//Function switchHandler: Sets state of outlets to on/off if one or more outlets is on/off
def switchHandler(evt) {
    if (evt.value == "on") {
        state.outlets = "on"
    } else if (evt.value == "off") {
        state.outlets = "off"
    }
}

// Event catchall
def event(evt)
{
    log.info "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}