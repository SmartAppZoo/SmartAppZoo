 /*
	Tstat Zone
    
 	Author: Rodney Rowen

        based on Mike Maxwell
    
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

@Field final Map VENT_STATE = [
    OFF:   "off",
    ON:    "on",
    AUTO_ON: "auto",
    AUTO_OFF: "circulate",
]

@Field final Map ZONE_OCCUPANCY = [
    VACANT:   "unoccupied",
    OCCUPIED: "occupied",
    IDLE:     "disabled",
    ACTIVE:   "active",
    INACTIVE: "inactive"
]

@Field final Map PRESENCE_STATE = [
    PRESENT:     "present",
    NOTPRESENT: "not present"
]

definition(
    name: "Tstat Zone",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "zone application for 'Tstat Master', do not install directly.",
    category: "My Apps",
    parent: "rodneyrowen:Tstat Master",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
    )

preferences {
    page(name: "Zone Sensors", title: "Select Zone Senors", install: true, uninstall: true) {
        section("Schedule Name") {
            input "zoneName", "text", title: "Name of this Zone:", multiple: false, required: true
		}
        section("Temperature Sensors") {
            input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", multiple: true, required: true
        }
        section("Smart Vents..."){
            input "vents", "capability.switch", title: "Vents", multiple: true, required: false
            input "ventClosedLevel", "number", title: "Closed Level?", defaultValue: 25, multiple: false, required: false
        }
        section("Zone Temperature Adjustments...") {
            input "zoneAdjust", "number", title: "Temperature Adjustment:", defaultValue: 0, multiple: false, required: true
	        input "sModes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        section("Room Occupancy...") {
            input "inactiveAdjust", "number", title: "Temperature Adjustment:", defaultValue: 5, multiple: false, required: true
	        input "sOccupancyModes", "mode", title: "Only when mode is", multiple: true, required: false
            input "presence", "capability.presenceSensor", title: "Presence Devices", multiple: true, required: false
  		    input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false
            input "inactiveDelay", "number", title: "No Motion delay:", defaultValue: 60, multiple: false, required: false
        }
        
    }
}

def installed() {
}

def updated() {
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
    state.vChild = "1.0.1"
    parent.updateVer(state.vChild)
    state.nextRunTime = 0
    state.zoneControlMode = "active"
    state.zoneTriggerActive = false
    state.modeMult = 1
    state.zoneOccupancy = ZONE_OCCUPANCY.ACTIVE
    state.lastMotion = null

    subscribe(inTemp, "temperature", temperatureHandler)

    app.updateLabel("Zone-${settings.zoneName}") 
    def deviceID = "${app.id}"
    def zName = "Tstat-${settings.zoneName}"
    def zoneTile = getChildDevice(deviceID)
    if (!zoneTile) {
    	log.info "create Tstat Tile ${zName}"
        zoneTile = addChildDevice("rodneyrowen", "Tstat Zone Tile", deviceID, getHubID(), [name: zName, label: zName, completedSetup: true])
    } else {
    	log.info "Tstat Tile ${zName} exists"
    }
    
    subscribe(zoneTile, "zone", zoneActiveHandler)
    subscribe(zoneTile, "thermostatSetpoint", zoneSetpointHandler)
    subscribe(zoneTile, "thermostatFanMode", zoneVentModeHandler)
    if (motionSensors) {
    	subscribe(motionSensors, "motion.inactive", inactiveHandler)
        subscribe(motionSensors, "motion.active", activeHandler)
    }
    if (presence) {
        subscribe(people, "presence", presenceHandler)
    }

    log.debug "Installed with settings: ${settings}"

	state.tempOffset = 0
    state.temperature = 0
    runEvery5Minutes(poll)
}

def poll() {
	// Periodic poller to read the temperature and adjust the vents
    temperatureHandler()
    roomOccupancyHandler()
    processVents()
}

def temperatureHandler(evt) {
    def sum     = 0
    def count   = 0
    def average = 0
	def temp = 0
	if (settings.inTemp) {
        for (sensor in settings.inTemp) {
            temp = sensor.currentTemperature
            if (temp) {
                count += 1
                sum += temp
            } else {
                log.debug "got Null temp"
            }
        }
        average = Math.round((sum/count)*10.0)/10.0
    } else {
	    log.debug "No temp Sensors available set average to 60"
    	average = 60.0
    }
    
    state.temperature = average
    log.debug "average: ${average}"

    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
	    log.debug "Set Tile Temp to: ${average}"
		zoneTile.setTemperature(average)
   	}
}

def roomOccupancyHandler() {

    def newOccupancy = ZONE_OCCUPANCY.OCCUPIED
    
    if (state.zoneControlMode == "inactive") {
        newOccupancy = ZONE_OCCUPANCY.INACTIVE
    } else if (state.zoneControlMode == "active") {
        newOccupancy = ZONE_OCCUPANCY.ACTIVE
    } else {
    	// mode is set to auto
        if (settings.sOccupancyModes) {
            if (settings.sOccupancyModes.contains(location.mode)) {
                newOccupancy = ZONE_OCCUPANCY.OCCUPIED
            } else {
                newOccupancy = ZONE_OCCUPANCY.IDLE
            }
        }

        // Check the presence sensors to see if still active
        if (newOccupancy != ZONE_OCCUPANCY.IDLE) {
            if (settings.presence) {
                newOccupancy = ZONE_OCCUPANCY.VACANT
                for (presensor in settings.presence) {
                    log.debug "presensor = ${presensor} ${presensor.currentValue("presence")}"
                    if (presensor.currentValue("presence") == PRESENCE_STATE.PRESENT) {
                        newOccupancy = ZONE_OCCUPANCY.OCCUPIED
                        break;
                    }
                }

            }
        }

        if (newOccupancy == ZONE_OCCUPANCY.OCCUPIED) {
            if (settings.motionSensors) {
                for (sensor in settings.motionSensors) {
                    if (sensor.motion == "active") {
                        state.lastMotion = now()
                        break;
                    }
                }

                if (state.lastMotion) {
                    // check elasped time
                    def elapsed = now() - state.lastMotion
                    log.trace "elapsed = $elapsed"
                    if (elapsed >= ((settings.inactiveDelay ?: 0) * 60000L) - 2000) {
                        log.debug "Montion Timer expired - Back to vacant"
                        state.lastMotion = null
                        newOccupancy = ZONE_OCCUPANCY.VACANT
                    }
                } else {
                    newOccupancy = ZONE_OCCUPANCY.VACANT
                }
            }
        }
    }
    
    state.zoneOccupancy = newOccupancy
    // Do the work to determine if the room should be treated as occupied
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
	    log.debug "Set Zone Occupany to: ${state.zoneOccupancy}"
		zoneTile.setZoneOccupancy(state.zoneOccupancy)
   	}
}

def processVents() {
	if (vents) {
        def zoneDelta = getRoomDelta()
        log.debug "processVents -> Mode: ${state.ventMode}: ${zoneDelta}=${state.setPoint}-${state.temperature}"
        if ((state.ventMode == VENT_STATE.AUTO_ON) || (state.ventMode == VENT_STATE.AUTO_OFF)) {
            if (zoneDelta > 0) {
                setVents(VENT_STATE.AUTO_ON)
            } else {
                setVents(VENT_STATE.AUTO_OFF)
            }
        } else {
            // Leave whatever way was set previously
            setVents(state.ventMode);
        }

    	// if (isActive()) {
        //     if ((state.ventMode == VENT_STATE.AUTO_ON) || (state.ventMode == VENT_STATE.AUTO_OFF)) {
        //         def zoneTemp = state.temperature as Double
        //         def zoneSetpoint = state.setPoint as Double
        //         if (zoneTemp > zoneSetpoint) {
        //             // Set vents to closed
        //             setVents(VENT_STATE.AUTO_OFF);
        //         } else {
        //             // Set vents to open
        //             setVents(VENT_STATE.AUTO_ON);
        //         }
        //     } else {
        //         // Leave whatever way was set previously
        //         setVents(state.ventMode);
        //     }
        //     log.debug "processVents -> ${state.ventMode} ${state.temperature} ${state.setPoint}"
        // } else {
        //     // FORCE OFF if in Auto Mode
        //     if (state.ventMode == VENT_STATE.AUTO_ON) {
        //         // Set vents to closed
        //         setVents(VENT_STATE.AUTO_OFF);
        //     }
	    //     log.debug "processVents -> Inactive zone - ${state.ventMode}"
        // }
    }
}

def setVents(newState) {
    log.debug "setVents: ${newState}"
    def zoneTile = getChildDevice("${app.id}")
	if (newState != state.ventMode) {
    	switch (newState) {
        	case VENT_STATE.AUTO_ON:
        		vents.setLevel(100)
                if (zoneTile) {
                    zoneTile.setThermostatFanMode(VENT_STATE.AUTO_ON)
                }
                break;
        	case VENT_STATE.AUTO_OFF:
        		vents.setLevel(ventClosedLevel)
                if (zoneTile) {
                    zoneTile.setThermostatFanMode(VENT_STATE.AUTO_OFF)
                }
                break;
        	case VENT_STATE.ON:
			    log.debug "setVents: open"
        		vents.setLevel(100)
                break;
        	case VENT_STATE.OFF:
            default:
			    log.debug "setVents: closed"
        		vents.setLevel(ventClosedLevel)
            	break;
       }
       state.ventMode = newState
    }
}

def presenceHandler(evt) {
    log.debug "presence event tHandler: $evt.value"
    roomOccupancyHandler()
    processVents()
}

def zoneActiveHandler(evt) {
	log.debug "Zone Control Mode Event: ${evt.name}, ${evt.value}"
    state.zoneControlMode = evt.value
    setThermostatMode(state.mode)
    setOperatingState(state.opState)
    processVents()
}

def setZoneActive(newMode) {
	log.debug "Set Zone Control Mode: ${newMode}"
    state.zoneControlMode = newMode
    setThermostatMode(state.mode)
    setOperatingState(state.opState)
    roomOccupancyHandler()
    processVents()
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
	    zoneTile.setZoneDelta(getZoneAdjustment())
    }
}

def zoneSetpointHandler(evt) {
   	state.setPoint = evt.value as Double
    def zoneAdj = getZoneAdjustment()
    state.tempOffset = state.setPoint - (state.zoneBase + zoneAdj)
	log.debug "Setpoint Event: ${evt.name}, ${evt.value} - new Offset ${state.tempOffset}"
    // Cause the main app to process the data again
    parent.zonePoll("poll")

}

def zoneVentModeHandler(evt) {
   	state.ventMode = evt.value
	log.debug "Vent Mode Event: ${evt.name}, ${state.ventMode}"
	processVents()
}

def setZoneVentMode(mode) {
   	state.ventMode = mode
	log.debug "Set Vent Mode: ${state.ventMode}"
	processVents()
}

def getTemperature() {
    temperatureHandler()
	return state.temperature
}

def Double getRoomDelta() {
    temperatureHandler()
    Double zoneDelta = 0
    //log.debug "getRoomDelta: ${state.setPoint} ${state.temperature} ${isActive()}"
    def zoneAdj = getZoneAdjustment()
    def localSetupPoint = 0  //state.zoneBase + zoneAdj + state.tempOffset
    //log.debug "getRoomDelta: Zone ${localSetupPoint} = Base ${state.zoneBase} Adj ${zoneAdj} Off ${state.tempOffset}"
    if (state.setPoint && state.temperature && isActive()) {
        def sp = state.setPoint as Double 
        zoneDelta = Math.round((sp - state.temperature)*10.0)/10.0
        //log.debug "getRoomDelta Value: ${zoneDelta}"
        if (zoneDelta < -10) {
            zoneDelta = -10
        } else if (zoneDelta > 10) {
            zoneDelta = 10
        } 
    }
	return zoneDelta
}

def Double getZoneAdjustment() {
	def zoneAdj = 0
    if (settings.zoneAdjust)
    {
        if (settings.sModes) {
            if (settings.sModes.contains(location.mode)) {
                zoneAdj = settings.zoneAdjust
            }
        } else {
        	zoneAdj = settings.zoneAdjust
        }
    }
    if (settings.inactiveAdjust && !isActive())
    {
        zoneAdj -= settings.inactiveAdjust
    }
    // Then use the mode multipler
    zoneAdj = zoneAdj * state.modeMult 

    //log.trace "Executing 'getZoneAdjustment' returned ${zoneAdj} with adj=${settings.zoneAdjust} and modes=${settings.sModes}"
	return zoneAdj
}

def Integer isActive() {
    //log.debug "Check Zone Active: ${state.zoneOccupancy}"
	def activeSystem = 0;
    switch (state.zoneOccupancy) {
        case ZONE_OCCUPANCY.VACANT:
        case ZONE_OCCUPANCY.INACTIVE:
        case ZONE_OCCUPANCY.IDLE:
            activeSystem = 0;
            break;
        case ZONE_OCCUPANCY.OCCUPIED:
        case ZONE_OCCUPANCY.ACTIVE:
            activeSystem = 1
            break;
    }
    return activeSystem
}

def Integer isCooling() {
    if (state.mode == MODE.COOL)
    {
        return 1
    } else {
        return 0
    }
}

def Integer requiresHvac() {
    return 0
}

def activeHandler(evt){
    log.trace "active handler fired via [${evt.displayName}] UTC: ${evt.date.format("yyyy-MM-dd HH:mm:ss")}"
    if (state.lastMotion == null) {
        state.lastMotion = now()
        // run proccessing loop
        roomOccupancyHandler()
        processVents()
    } else {
        state.lastMotion = now()
    }
}

def inactiveHandler(evt){
}

def setThermostatMode(String value) {
    state.mode = value
    if (state.mode == MODE.COOL)
    {
        state.modeMult = -1
    } else {
        state.modeMult = 1
    }

    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
    	if (isActive()) {
		    log.trace "Executing 'setThermostatMode' $value"
        	zoneTile.setThermostatMode(value)
        } else {
        	// Force into idle state
		    log.trace "Executing 'setThermostatMode' force Off"
        	zoneTile.setThermostatMode("off")
        }
		
   	}
}

def setOperatingState(String operatingState) {
    state.opState = operatingState
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
    	if (isActive()) {
		    log.trace "Executing 'setOperatingState' $operatingState"
        	zoneTile.setOperatingState(operatingState)
        } else {
        	// Force into idle state
		    log.trace "Executing 'setOperatingState' force idle"
        	zoneTile.setOperatingState("idle")
        }
   	}
}

def setThermostatSetpoint(Double degreesF) {
    log.trace "Executing 'setZoneBase' $degreesF"
    state.zoneBase = degreesF
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setZoneBase(degreesF)
   	}
}

def setHeatingSetpoint(Double degreesF) {
    log.trace "Executing 'setHeatingSetpoint' $degreesF"
    state.heatingSetpoint = degreesF
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setHeatingSetpoint(degreesF)
        zoneTile.setZoneDelta(getZoneAdjustment())
   	}
}

def setCoolingSetpoint(Double degreesF) {
    log.trace "Executing 'setCoolingSetpoint' $degreesF"
    state.coolingSetpoint = degreesF
    def zoneTile = getChildDevice("${app.id}")
	if (zoneTile) {
		zoneTile.setCoolingSetpoint(degreesF)
        zoneTile.setZoneDelta(getZoneAdjustment())
   	}
}

