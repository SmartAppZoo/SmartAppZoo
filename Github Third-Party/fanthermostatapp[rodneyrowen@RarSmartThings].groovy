/*
	Whole House Fan Thermostat
    
 	Author: Rodney Rowen 2018
    
	This software is free for Private Use. You may use and modify the software without distributing it.
 
	This software and derivatives may not be used for commercial purposes.
	You may not distribute or sublicense this software.
	You may not grant a sublicense to modify and distribute this software to third parties not included in the license.

	The software is provided without warranty and the software author/license owner cannot be held liable for damages.        
        
*/

import groovy.transform.Field

// enummaps
@Field final Map      MODE = [
    OFF:   "off",
    HEAT:  "heat",
    AUTO:  "auto",
    COOL:  "cool",
    EHEAT: "emergency heat"
]

@Field final Map      FAN_MODE = [
    OFF:       "off",
    AUTO:      "auto",
    CIRCULATE: "circulate",
    ON:        "on"
]

@Field final Map      OP_STATE = [
    INIT:      "Conntected",
    COOLING:   "Fan On Cooling To",
    FAN_ON:    "Fan On",
    FAN_OFF:   "Fan Off",
    PEND_COOL: "Cooling Pending",
    DELAY:     "Fan On - Delay Mode",
    IDLE:      "idle"
]

@Field final Map      FAN_STATE = [
    OFF:       "off",
    ON:        "on",
    MIX:       "mix"
]

@Field final List AUTO_MODES = [MODE.OFF, MODE.COOL]

@Field final List FAN_RUNNING_STATES = [FAN_MODE.ON, FAN_MODE.CIRCULATE]

// config - TODO: move these to a pref page
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.AUTO, FAN_MODE.ON, FAN_MODE.CIRCULATE]

// defaults
@Field final String   DEFAULT_MODE = MODE.OFF
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.OFF
@Field final String   DEFAULT_OP_STATE = OP_STATE.INIT
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.FAN_OFF
@Field final Integer  DEFAULT_TEMPERATURE = 72
@Field final Integer  DEFAULT_OUT_TEMP = 69
@Field final Integer  DEFAULT_COOLING_SETPOINT = 80
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = 70
@Field final String   DEFAULT_FAN_STATE = "off"
@Field final Integer  DELAY_ONE_MINUTE = 60000L
@Field final Integer  DELAY_ONE_HOUR = 60 * 60000L
definition(
    name: "FanThermostatApp",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Smart App for 'FanThermostat'",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
    )

preferences {
    section("Fan Thermostat") {
    	input "thermostat", "capability.thermostat", title: "Fan Thermostat"
    }

	section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer"
	}
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", multiple: true
        input "coolingTemp", "number", title: "Minimum Indoor Temperature"
        input "fans", "capability.switch", title: "Vent Fan", multiple: true
    }
    
    section("Thermostat") {
    	input "houseThermostat", "capability.thermostat", title: "House Thermostat"
    }
    
    section("Window Contants") {
        input name: "windows", title: "Contact Sensors", type: "capability.contactSensor", multiple: true, required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def getHubID(){
    def hubID
    if (myHub){
        hubID = myHub.id
    } else {
        def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL } 
        //log.debug "hub count: ${hubs.size()}"
        if (hubs.size() == 1) hubID = hubs[0].id 
    }
    //log.debug "hubID: ${hubID}"
    return hubID
}

def initialize() {
	state.fanMode = FAN_MODE.OFF
    state.fanRunning = false
    state.delayTime = null
    def deviceID = "${app.id}"
    //def thermostat = getChildDevice(deviceID)
    //if (!thermostat) {
    //	log.info "create Fan Thermostat"
    //    thermostat = addChildDevice("rodneyrowen", "FanThermostat", deviceID, getHubID(), [name: "Fan Thermostat", label: "Fan Thermostat", completedSetup: true])
    //} else {
    //	log.info "Fan Thermostat exists"
    //}
    
    // Set initial values in the thermostat
    subscribe(thermostat, "thermostatFanMode", fanModeHandler)
    subscribe(thermostat, "coolingSetpoint", delayTimeHandler)
    subscribe(thermostat, "thermostatSetpoint", setpointHandler)
    subscribe(outTemp, "temparture", outdoorTempHandler)
    subscribe(inTemp, "temparture", temperatureHandler)
    subscribe(houseThermostat, "thermostatMode", thermostatHandler)
    log.debug "Subscribed to devices:"
    
    // Set the defaults
    //thermostat.setThermostatMode(DEFAULT_MODE)
    thermostat.setThermostatFanMode(DEFAULT_FAN_MODE)
    //thermostat.setCoolingSetpoint(DEFAULT_COOLING_SETPOINT)
    thermostat.setThermostatSetpoint(DEFAULT_THERMOSTAT_SETPOINT)
    thermostat.setOperatingState(DEFAULT_OP_STATE)
    //thermostat.setTemperature(DEFAULT_TEMPERATURE)
    //thermostat.setOutdoorTemp(DEFAULT_OUT_TEMP)
    thermostat.setFanState(DEFAULT_FAN_STATE)
    
    thermostatHandler()
    temperatureHandler()
    outdoorTempHandler()
    
    runEvery5Minutes(poll)
}

def fanModeHandler(evt) {
    def fanMode = thermostat.currentValue('thermostatFanMode')
    log.trace "Got Fan State ${fanMode}"
    // Increment the delay stop time
    if (fanMode == FAN_MODE.CIRCULATE) {
	    def delayHours = thermostat.currentValue('coolingSetpoint')
	    log.trace "Set Delay Hours: ${delayHours}"
        state.delayTime = now()
        state.delayTimeout = delayHours * DELAY_ONE_HOUR + 15000
    }
    processFanModeChange(fanMode)
}

def delayTimeHandler(evt) {
    def delayHours = thermostat.currentValue('coolingSetpoint')
    log.trace "Got Timer change Hours: ${delayHours}"
    state.delayTime = now()
    state.delayTimeout = delayHours * DELAY_ONE_HOUR + 15000
    processFanModeChange(fanMode)
}

def setpointHandler(evt) {
    def setpoint = thermostat.currentValue('thermostatSetpoint')
    log.trace "Setpoint Changed ${setpoint}"
    processAutoMode()
}

def thermostatHandler(evt) {
    def houseMode = houseThermostat.currentValue('thermostatMode')
    log.trace "Got House Mode ${houseMode}"
    thermostat.setThermostatMode(houseMode)
}

def outdoorTempHandler(evt) {
    def outdoorTemp = outTemp.currentTemperature
    log.trace "Outdoor Temp ${outdoorTemp}"
    thermostat.setOutdoorTemp(outdoorTemp)
}

def temperatureHandler(evt) {
    def average = getTemparture()
    log.debug "temp average: $average"

    thermostat.setTemperature(average)
}

def poll() {
	// Periodic poller since event listening does not seem to be working
    processAutoMode()
}

private double getTemparture() {
    def sum     = 0
    def count   = 0
    def average = 0

    for (sensor in settings.inTemp) {
        count += 1
        sum   += sensor.currentTemperature
    }

    average = sum/count
    return average
}

private String getCurrentFansState() {
	// Read all the fans and make sure they are in the correct state
    def fanOn = 0
    def fanOff = 0
    for (fan in settings.fans) {
        if (fan.currentValue('switch') == FAN_STATE.ON) {
        	fanOn += 1
        } else {
        	fanOff += 1
        }
    }

    def fanState = FAN_STATE.MIX
	if (fanOn > 0 && fanOff == 0) {
    	fanState = FAN_STATE.ON
    } else if (fanOn == 0 && fanOff > 0) {
    	fanState = FAN_STATE.OFF
    }
    return fanState
}

private processFanModeChange(String fanMode) {
    log.debug "Set fan mode: $fanMode"
    switch (fanMode) {
    	case FAN_MODE.AUTO:
	        processAutoMode()
        	break;
        case FAN_MODE.ON:
	        thermostat.setOperatingState(OP_STATE.FAN_ON)
        	setFanState(true)
        	break;
        case FAN_MODE.CIRCULATE:
	        processAutoMode()
        	break;
        case FAN_MODE.OFF: 
	        thermostat.setOperatingState(OP_STATE.FAN_OFF)
        	setFanState(false)
        	break;
        default:
	        thermostat.setOperatingState(OP_STATE.FAN_OFF)
        	setFanState(false)
		    log.warn "'$fanMode' is not a supported state. Please set one of ${SUPPORTED_FAN_MODES.values().join(', ')}"
        	break;
    }
    // Save the last fanModeOP_STATE.
    state.lastOperatingState = fanMode
}

private processAutoMode() {
    // Only to auto processing in auto mode so check this first
    def fanMode = thermostat.currentValue('thermostatFanMode')
    def setpoint = thermostat.currentValue('thermostatSetpoint')
    // Read the inputs to the processing
    def houseMode = houseThermostat.currentValue('thermostatMode')
    thermostat.setThermostatMode(houseMode)
    def outdoorTemp = outTemp.currentTemperature
    thermostat.setOutdoorTemp(outdoorTemp)
    def temperature = getTemparture()
    thermostat.setTemperature(temperature)
    // In Auto Mode only turn on if a window is open
    def closedWindow = windows?.currentValue("contact")?.contains("closed")
    
    log.trace "Evalutate: ${houseMode} house ${temperature} outside ${outdoorTemp} setpoint ${setpoint} windows ${openWindow}"
    def opstate
    if (fanMode == FAN_MODE.AUTO) {
        if (houseMode in AUTO_MODES) {
            if ( (outdoorTemp < temperature) && (temperature > setpoint) && !closedWindow) {
            	opstate = "Auto: Cooling to ${setpoint}"
                thermostat.setOperatingState(opstate)
                setFanState(true)
            } else {
                if (windows && closedWindow) {
                	opstate = "Auto: Waiting for windows to open"
                } else if (outdoorTemp >= temperature) {
                	opstate = "Auto: Waiting for ${outdoorTemp} < ${temperature}"
                } else {
                	opstate = "Auto: Reached ${setpoint}"
                }
                thermostat.setOperatingState(opstate)
                setFanState(false)
            }
        } else {
	        thermostat.setOperatingState("Auto: idle")
            setFanState(false)
        }
    } else if (fanMode == FAN_MODE.CIRCULATE) {
        if (state.delayTime) {
            def elapsed = now() - state.delayTime
            def timerTimeout = state.delayTimeout
            def timeString = getTimerValueAsString(timerTimeout - elapsed)
            log.trace "elapsed = ${elapsed} less than ${timerTimeout}"
            if (elapsed <= timerTimeout) {
                opstate = "Fan On: For ${timeString}"
                thermostat.setOperatingState(opstate)
                setFanState(true)
            } else {
                state.startTime = null
                thermostat.setOperatingState(OP_STATE.FAN_OFF)
			    thermostat.setThermostatFanMode(OP_STATE.FAN_OFF)
                setFanState(false)
            }
        }
    }
}

def setFanState(shouldRun) {
	def currentFan = getCurrentFansState()
    if(shouldRun && currentFan != FAN_STATE.ON) {
    	fans.on();
        thermostat.setFanState(FAN_STATE.ON)
        log.debug "Turn Fan On"
    } else if(!shouldRun && currentFan != FAN_STATE.OFF) {
    	fans.off();
        thermostat.setFanState(FAN_STATE.OFF)
        log.debug "Turn Fan Off"
    } else {
        thermostat.setFanState(currentFan)
    }
}

private String getTimerValueAsString(timeout) {
	Integer timeMinutes = timeout / DELAY_ONE_MINUTE
    Integer Hours = timeMinutes / 60
    Integer Mins = timeMinutes % 60
    String timeString
    if (timeout < 0) {
    	return "0:00"
    }
    
    if (Hours > 0) {
    	timeString = "${Hours}:"
    } else {
    	timeString = "0:"
    }
    
    if (Mins > 10) {
    	timeString += Mins
    } else if (Mins > 0) {
    	timeString += "0${Mins}"
    } else {
    	timeString += "00"
    }
    
	return timeString
}

private String nextListElement(List uniqueList, currentElt) {
    if (uniqueList != uniqueList.unique().asList()) {
        throw InvalidPararmeterException("Each element of the List argument must be unique.")
    } else if (!(currentElt in uniqueList)) {
        throw InvalidParameterException("currentElt '$currentElt' must be a member element in List uniqueList, but was not found.")
    }
    Integer listIdxMax = uniqueList.size() -1
    Integer currentEltIdx = uniqueList.indexOf(currentElt)
    Integer nextEltIdx = currentEltIdx < listIdxMax ? ++currentEltIdx : 0
    String nextElt = uniqueList[nextEltIdx] as String
    return nextElt
}
