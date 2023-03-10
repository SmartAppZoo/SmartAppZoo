/*
	Tstat Master
    
 	Author: Rodney Rowen

        Based on Mike Maxwell work
	    
	This software if free for Private Use. You may use and modify the software without distributing it.
 
	This software and derivatives may not be used for commercial purposes.
	You may not distribute or sublicense this software.
	You may not grant a sublicense to modify and distribute this software to third parties not included in the license.

	The software is provided without warranty and the software author/license owner cannot be held liable for damages.        
        
*/
import groovy.transform.Field

// enummaps
@Field final Map      LOG = [
    ERROR:   5,
    WARN:    4,
    INFO:    3,
    TRACE:   2,
    DEBUG:   1
]

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
    COOLING:   "cooling",
    HEATING:   "heating",
    FAN:       "fan only",
    PEND_COOL: "pending cool",
    PEND_HEAT: "pending heat",
    VENT_ECO:  "vent economizer",
    IDLE:      "idle"
]

@Field final Map SETPOINT_TYPE = [
    COOLING: "cooling",
    HEATING: "heating"
]

@Field final List HEAT_ONLY_MODES = [MODE.HEAT, MODE.EHEAT]
@Field final List COOL_ONLY_MODES = [MODE.COOL]
@Field final List DUAL_SETPOINT_MODES = [MODE.AUTO]
@Field final List RUNNING_OP_STATES = [OP_STATE.HEATING, OP_STATE.COOLING]

// config - TODO: move these to a pref page
@Field List SUPPORTED_MODES = [MODE.OFF, MODE.HEAT, MODE.AUTO, MODE.COOL]
@Field List SUPPORTED_FAN_MODES = [FAN_MODE.OFF, FAN_MODE.ON, FAN_MODE.AUTO, FAN_MODE.CIRCULATE]

@Field final Float    THRESHOLD_DEGREES = 1.0
@Field final Integer  SIM_HVAC_CYCLE_SECONDS = 15
@Field final Integer  DELAY_EVAL_ON_MODE_CHANGE_SECONDS = 3

@Field final Integer  MIN_SETPOINT = 35
@Field final Integer  MAX_SETPOINT = 95
@Field final Integer  AUTO_MODE_SETPOINT_SPREAD = 4 // In auto mode, heat & cool setpoints must be this far apart
// end config

// derivatives
@Field final IntRange FULL_SETPOINT_RANGE = (MIN_SETPOINT..MAX_SETPOINT)
@Field final IntRange HEATING_SETPOINT_RANGE = (MIN_SETPOINT..(MAX_SETPOINT - AUTO_MODE_SETPOINT_SPREAD))
@Field final IntRange COOLING_SETPOINT_RANGE = ((MIN_SETPOINT + AUTO_MODE_SETPOINT_SPREAD)..MAX_SETPOINT)
@Field final IntRange DELAY_SETPOINT_RANGE = (1..12)

// defaults
@Field final String   DEFAULT_MODE = MODE.OFF
@Field final String   DEFAULT_FAN_MODE = FAN_MODE.AUTO
@Field final String   DEFAULT_OP_STATE = OP_STATE.IDLE
@Field final String   DEFAULT_PREVIOUS_STATE = OP_STATE.HEATING
@Field final String   DEFAULT_SETPOINT_TYPE = SETPOINT_TYPE.HEATING
@Field final Integer  DEFAULT_TEMPERATURE = 52
@Field final Integer  DEFAULT_OUT_TEMP = 49
@Field final Integer  DEFAULT_HEATING_SETPOINT = 48
@Field final Integer  DEFAULT_COOLING_SETPOINT = 13
@Field final Integer  DEFAULT_COOLING_DELAY_MAX = 12
@Field final Integer  DEFAULT_THERMOSTAT_SETPOINT = 70
@Field final String   DEFAULT_FAN_STATE = "off"

definition(
    name: "Tstat Master",
    singleInstance: true,
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Smart Thermostat with zones.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
    )


preferences {
	page(name: "main")	
	page(name: "schedules")	
    page(name: "zones")
}

def main(){
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
        ,title		: "Tstat Settings"
        ,nextPage   : "schedules"
        ,uninstall  : true
        ){
            section("Temperature Sensors") {
                input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", multiple: true
            }
            section("House Thermostat") {
                input "houseThermostat", "capability.thermostat", title: "House Thermostat"
                input name: "houseDisable", title: "Disable Tstat Control", type: "bool", defaultValue: false, required: true
            }

            section("Debug") {
                input("traceLevel", "number",title: "3=Normal,2=Debug,1=Verbose",  range: "1..3", defaultValue: 1,
                    description: "optional" )  
            }
        }
}

def schedules(){
	return dynamicPage(
    	name		: "schedules"
        ,title		: "Tstat Schedules"
        ,nextPage   : "zones"
        ,uninstall  : true
        ){
            if (installed){
            	section("Schedules"){
                    app(name: "childSchedules", appName: "Tstat Schedule", namespace: "rodneyrowen", title: "Create New Schedule...", multiple: true)
                }
            } else {
            	section(){
                	paragraph("Tap done to finish the initial installation.\nRe-open the app from the smartApps flyout to create your zones.")
                }
            }
            section("Default Setpoints (if no schedule applies") {
                input "coolingTemp", "number", title: "Cooling Setpoint"
                input "heatingTemp", "number", title: "Heating Setpoint"
            }
        }
}

def zones(){
	return dynamicPage(
    	name		: "zones"
        ,title		: "Tstat Zones"
        ,install    : true
        ,uninstall  : true
        ){
             if (installed){
            	section("Tstat Zones") {
                    app(name: "childZones", appName: "Tstat Zone", namespace: "rodneyrowen", title: "Create New Zone...", multiple: true)
            	}
				section (getVersionInfo()) { }
            } else {
            	section(){
                	paragraph("Tap done to finish the initial installation.\nRe-open the app from the smartApps flyout to create your zones.")
                }
            }
        }
}

def installed() {
    debugLog(LOG.TRACE, "Installed with settings: ${settings}")
    initialize()
}

def updated() {
    debugLog(LOG.TRACE, "Updated with settings: ${settings}")
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
	state.vParent = "1.0.0"
    def deviceID = "${app.id}"
    def zName = "Tstat Thermostat"
    def tstatThermostat = getChildDevice(deviceID)
    if (!tstatThermostat) {
    	debugLog(LOG.DEBUG, "create Tstat Theromstat")
        tstatThermostat = addChildDevice("rar", "Tstat Thermostat", deviceID, getHubID(), [name: zName, label: zName, completedSetup: true])
    } else {
    	debugLog(LOG.DEBUG, "Tstat Theromstat exists")
    }
    //tstatThermostat.inactive()
    // Subscribe to things in the the thermostat
    subscribe(tstatThermostat, "thermostatMode", modeHandler)
    //subscribe(tstatThermostat, "thermostatFanMode", fanModeHandler)
    subscribe(tstatThermostat, "coolingSetpoint", coolingSetpointHandler)
    subscribe(tstatThermostat, "heatingSetpoint", heatingSetpointHandler)
    subscribe(tstatThermostat, "thermostatSetpoint", setpointHandler)
    subscribe(location, "mode", modeChangeHandler)

    runEvery5Minutes(poll)

    // save defaults to state
    // Initialize the mode based on the house thermostats current mode
    def houseMode = houseThermostat.currentValue('thermostatMode')
    debugLog(LOG.DEBUG, "Save defaults and set thermostat to house mode ${houseMode}")
    evaluateMode(houseMode)

    changeSchedule("Default", settings.coolingTemp, settings.heatingTemp) 

}

def modeChangeHandler(evt) {
    debugLog(LOG.DEBUG, "Location Mode Change ${location.mode}")
    evaluateState()
}

def modeHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def mode = tstatThermostat.currentValue('thermostatMode')
    debugLog(LOG.DEBUG, "Got Mode ${mode}")
    evaluateMode(mode)
}

def setpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('thermostatSetpoint')
    updateSetpoint(setpoint)
}

def coolingSetpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('coolingSetpoint')
    debugLog(LOG.DEBUG, "Cooling Setpoint Changed ${setpoint}")
    evaluateState()
}

def heatingSetpointHandler(evt) {
	def tstatThermostat = getChildDevice("${app.id}")
    def setpoint = tstatThermostat.currentValue('heatingSetpoint')
    debugLog(LOG.DEBUG, "Heating Setpoint Changed ${setpoint}")
    evaluateState()
}

private double getTemparture() {
    def sum     = 0
    def count   = 0
    def average = 0

    for (sensor in settings.inTemp) {
        count += 1
        sum   += sensor.currentTemperature
    }

    average = Math.round((sum/count)*10.0)/10.0
    return average
}

def Integer getThermostatSetpoint() {
    def ts = state.heatingSetpoint
    if (state.setpointType == SETPOINT_TYPE.COOLING) {
    	ts = state.coolingSetpoint
    }
    return ts ? ts : DEFAULT_THERMOSTAT_SETPOINT
}

def poll() {
	// Periodic poller since event listening does not seem to be working
    evaluateState()
}

def zonePoll(what){
    debugLog(LOG.DEBUG, "zonePoll ${what}")
    if (what == "poll") {
	    evaluateState()
    }
}

private evaluateMode(def newMode) {
	//if (newMode != state.mode) {
        debugLog(LOG.lRACE, "Set mode: ${newMode}")
        switch (newMode) {
            case MODE.AUTO:
                changeMode(MODE.AUTO, OP_STATE.HEATING, SETPOINT_TYPE.HEATING)
                break;
            case MODE.HEAT:
                changeMode(MODE.HEAT, OP_STATE.HEATING, SETPOINT_TYPE.HEATING)
                break;
            case MODE.COOL:
                changeMode(MODE.COOL, OP_STATE.COOLING, SETPOINT_TYPE.COOLING)
                break;
            case MODE.OFF: 
                changeMode(MODE.OFF, OP_STATE.IDLE, SETPOINT_TYPE.HEATING)
                break;
            default:
                changeMode(MODE.OFF, OP_STATE.IDLE, SETPOINT_TYPE.HEATING)
                debugLog(LOG.WARN, "'$newMode' is not a supported state. Please set one of ${MODE.values().join(', ')}")
                break;
        }
        evaluateState()
    //}
}

private evaluateState() {
    evaluateChildren()
    doProcessing()
    updateZones()
    // Indicate we ran the last update
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.updateLastUpdate()
}

private evaluateChildren() {
	def scheduleName = "Default"
	def bestState = 0
    def coolingSet = settings.coolingTemp
    def heatingSet = settings.heatingTemp
    
	def tstatThermostat = getChildDevice("${app.id}")
    state.temperature = getTemparture()
    state.zoneTemp = state.temperature as Double;
    tstatThermostat.setTemperature(state.temperature)

	// Want current state before we make any adjustments
    state.zoneMaxDelta = state.setpoint - state.temperature
    
    childApps.each {child ->
        def childName = child.label.split('-')
        def type = childName[0]
        def value = childName[1]
        if (type == "Schedule") {
            debugLog(LOG.DEBUG, "Evalute Schedule type for: ${value}")
            def state = child.isActive()
            if (state > bestState) {
                bestState = state
                scheduleName = value
                coolingSet = child.getCoolingSetpoint()
                heatingSet = child.getHeatingSetpoint()
                debugLog(LOG.TRACE, "Selected new Schedule ${scheduleName} Cool: ${coolingSet} Heat: ${heatingSet}")
            }
        } else if (type == "Zone") {
            debugLog(LOG.DEBUG, "Evalute Zone type for: ${value}")
            def needsHvacOn = child.requiresHvac()
            if (needsHvacOn) {
                def zoneDelta = child.getRoomDelta()
                if (state.setpointType == SETPOINT_TYPE.COOLING) {
    				if (tempDelta > state.zoneMaxDelta) {
                    	state.zoneMaxDelta = zoneDelta
                        state.zoneTemp = child.getTemperature
                    }
                } else {
    				if (tempDelta < state.zoneMaxDelta) {
                    	state.zoneMaxDelta = zoneDelta
                        state.zoneTemp = child.getTemperature
                    }
                }
                debugLog(LOG.DEBUG, "Zone ${value} returned ${zoneDelta}  maxDelta ${state.zoneMaxDelta}")
            }
        } else {
            debugLog(LOG.WARN, "Unknown Child type: ${child.label}")
        }
    }

    if (scheduleName != state.scheduleName)
    {
        changeSchedule(scheduleName, coolingSet, heatingSet)
    }
    
    debugLog(LOG.DEBUG, "Max Zone Delta = ${state.zoneMaxDelta}")

}


private changeMode(newMode, newOpState, newSetpointType) {
    debugLog(LOG.INFO, "Change Mode: ${newMode} op ${newOpState} type ${newSetpointType}")
    // Save it to the state
    state.mode = newMode
    state.opState = newOpState
    state.setpointType = newSetpointType

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setOperatingState(newOpState)
}

private changeSchedule(scheduleName, coolingSet, heatingSet) {
    debugLog(LOG.INFO, "Change Schedule: ${scheduleName} cool ${coolingSet} heat ${heatingSet}")
    // Save it to the state
    state.scheduleName = scheduleName
    state.coolingSetpoint = coolingSet
    state.heatingSetpoint = heatingSet
    updateSetpoint(getThermostatSetpoint())

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setSchedule(scheduleName)
    tstatThermostat.setThermostatSetpoint(state.setpoint)
    tstatThermostat.setCoolingSetpoint(coolingSet)
    tstatThermostat.setHeatingSetpoint(heatingSet)

}

private updateSetpoint(setpoint) {
    debugLog(LOG.INFO, "Change Setpoint: ${setpoint}")
    // Save it to the state
    state.setpoint = setpoint

    // push it to the theromstat device
	def tstatThermostat = getChildDevice("${app.id}")
    tstatThermostat.setThermostatSetpoint(state.setpoint)
    
    //if (state.setpointType == SETPOINT_TYPE.COOLING) {
    //	state.coolingSetpoint = setpoint
	//    tstatThermostat.setCoolingSetpoint(setpoint)
    //} else {
    //	state.heatingSetpoint = setpoint
	//    tstatThermostat.setHeatingSetpoint(setpoint)
    //}
    doProcessing()
    updateZones()
}

private String determineHouseMode(mode) {
	// For right now
    def houseMode = MODE.OFF
    switch (mode) {
        case MODE.COOL:
    		houseMode = MODE.COOL
            break;
        case MODE.AUTO:
        case MODE.HEAT:
    		houseMode = MODE.HEAT
            break;
        case MODE.OFF: 
    	default:
        	houseMode = MODE.OFF
            break;
        }
	return houseMode       
}

private doProcessing() {
    // Read the inputs to the processing
    def houseMode = houseThermostat.currentValue('thermostatMode')
    def houseTemp = houseThermostat.currentValue('temperature')
    def houseSetpoint = houseThermostat.currentValue('thermostatSetpoint')
    def houseCooling = houseThermostat.currentValue('coolingSetpoint')
    def houseHeating = houseThermostat.currentValue('heatingSetpoint')
    debugLog(LOG.INFO, "Evalutate: ${mode} temp ${state.temperature}/${state.setpoint} house ${houseTemp}/${houseSetpoint}(${houseHeating}-${houseCooling})")

	// Treat a house mode of Emergency heat like heat
	if (houseMode == MODE.EHEAT) {
    	houseMode = MODE.HEAT
	    debugLog(LOG.DEBUG, "Convert EHEAT Mode to ${houseMode}")
    }
    
    def neededHouseMode = determineHouseMode(state.mode)
    if (houseMode != neededHouseMode) {
    	if (!houseDisable) {
            debugLog(LOG.DEBUG, "Setting House Mode to ${neededHouseMode}")
        	houseThermostat.setThermostatMode(neededHouseMode)
        } else {
            debugLog(LOG.DEBUG, "(Disabled) Setting House Mode to ${neededHouseMode}")
        }
    }

	def houseDelta = houseSetpoint - houseTemp
    def setpointAdj = state.zoneMaxDelta - houseDelta
    if (setpointAdj < -10) {
        setpointAdj = -10
    } else if (setpointAdj > 10) {
        setpointAdj = 10
    } 

    def newHeating = Math.round(houseHeating + setpointAdj)
    def newCooling = Math.round(houseCooling + setpointAdj)
   	if (!houseDisable) {
        debugLog(LOG.VERBOSE, "Setting House setpoints to ${newHeating}/${state.heatingSetpoint} and ${newCooling}/${state.coolingSetpoint}")
        if (newHeating != houseHeating) {
        	houseThermostat.setHeatingSetpoint(newHeating)
        	houseThermostat.setCoolingSetpoint(newCooling)
        }
    } else {
        debugLog(LOG.VERBOSE, "(Disabled) Setting House setpoints to ${newHeating}/${state.heatingSetpoint} and ${newCooling}/${state.coolingSetpoint}")
    }
}

private updateZones() {
    childApps.each {child ->
        def childName = child.label.split('-')
        def type = childName[0]
        def value = childName[1]
        if (type == "Zone") {
            debugLog(LOG.DEBUG, "Updating Zone ${value}: ${state.mode} ${state.opState} ${state.heatingSetpoint} ${state.coolingSetpoint}")
            child.setThermostatMode(state.mode)
			child.setOperatingState(state.opState)
			child.setThermostatSetpoint(state.setpoint)
            child.setHeatingSetpoint(state.heatingSetpoint)
            child.setCoolingSetpoint(state.coolingSetpoint)
        }
    }
}

def getVersionInfo(){
	return "Version Tstat:\n\tTsat Master: ${state.vParent ?: "No data available yet."}\n\tZone Version: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}


def debugLog(LogFilter, message) {

    if (LogFilter >= settings.traceLevel) {
        switch (LogFilter) {
            case LOG_ERROR:
                log.error "${message}"
            break
            case LOG_WARN:
                log.warn "${message}"
            break
            case LOG_INFO:
                log.info  "${message}"
            break
            case LOG_TRACE:
                log.trace "${message}"
            break
            case LOG_DEBUG:
            default:
                log.debug "${message}"
            break
        }  /* end switch*/              
    } /* end if displayEvent*/
}