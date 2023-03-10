definition(
	name: "Virtual Thermostat With Device and Schedule",
	namespace: "JayUK",
	author: "JayUK",
	description: "Control a heater in conjunction with any temperature sensor, like a SmartSense Multi.",
	category: "Green Living",
	iconUrl: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo-small.png",
	iconX2Url: "https://raw.githubusercontent.com/JayUK/SmartThings-VirtualThermostat/master/images/logo.png",
	parent: "JayUK:Virtual Thermostat Manager",
    // 
    //	Changed the 1 minute EvaluateRoutine not to poll the temperature sensor, in the hope of saving it's battery
    //	Instead, rely on the temperature sensor to trigger an event and update a value when it detects a temperature
    //  change.
)
// ********************************************************************************************************************
preferences {
	section("Temperature sensor(s)... (If multiple sensors are selected, the average value will be used)"){
		input "sensors", "capability.temperatureMeasurement", title: "Sensor", multiple: true, required: true
        input "sensorMinimum", "number", title: "Minimum value that the sensor is deemed valid", required: true, defaultValue: 10
        input "sensorMaximum", "number", title: "Maximum value that the sensor is deemed valid", required: true, defaultValue: 40
	}
	section("Select the heater outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true, required: true
	}
	section("Only heat when a contact isn't open (optional, leave blank to not require contact detection)...", hideWhenEmpty: true){
		input "contacts", "capability.contactSensor", title: "Contact", required: false, multiple: true, hideWhenEmpty: true
        input "contactDuration", "number", title: "Duration a contact has to be open before heating is turning off (Mins: 1-30)", range: "1..30", required: false, hideWhenEmpty: "contacts"
	}
 	section("Only heat when a person is present (optional, leave blank to not require presence detection)...", hideWhenEmpty: true){
		input "presences", "capability.presenceSensor", title: "Presence", required: false, multiple: true, hideWhenEmpty: true
        input "presenceMinimumDuration", "number", title: "Minimum duration a presence stays active for (Mins: 0-30)", range: "0..30", defaultValue: 0, required: true, hideWhenEmpty: "presences"
        input "presenceAwaySetpoint", "decimal", title: "Away Temperature (1-40)", range: "1..40", required: false, hideWhenEmpty: "presences"
	}
   	section("Only heat when a movement is detected (optional, leave blank to not require motion detection)...", hideWhenEmpty: true){
		input "motions", "capability.motionSensor", title: "Motion", required: false, multiple: true, hideWhenEmpty: true
        input "motionDuration", "number", title: "Duration a motion stays active for (Mins: 1-30)", range: "1..30", required: false, hideWhenEmpty: "motions"
        input "motionAwaySetpoint", "decimal", title: "Away Temp (1-40)", range: "1..40", required: false, hideWhenEmpty: "motions"
	}
   	section("Never go below this temperature (even if heating is turned off): (optional)"){
		input "emergencySetpoint", "decimal", title: "Emergency Temp (1-30)", range: "1..30", required: false
	}
    section("Temperature Threshold (Don't allow heating to go above or below this amount from set temperature)") {
		input "aboveThreshold", "decimal", "title": "Above Temperature Threshold (0-10)", range: "0..10", required: true, defaultValue: 0.5
        input "belowThreshold", "decimal", "title": "Below Temperature Threshold (0-10)", range: "0..10", required: true, defaultValue: 0.5
	}
 	section("Monday to Friday Schedule") {
		input "zone1", "time", title: "Zone 1 start time", required: true
		input "zone1Temperature", "decimal", title: "Zone 1 temperature", defaultValue: 15, required: true
        input "zone1Name", "string", title: "Zone 1 name", defaultValue: "Zone 1", required: true
		input "zone2", "time", title: "Zone 2 start time", required: true
		input "zone2Temperature", "decimal", title: "Zone 2 temperature", defaultValue: 15, required: true
        input "zone2Name", "string", title: "Zone 2 name", defaultValue: "Zone 2", required: true
		input "zone3", "time", title: "Zone 3 start time", required: true
		input "zone3Temperature", "decimal", title: "Zone 3 temperature", defaultValue: 15, required: true
        input "zone3Name", "string", title: "Zone 3 name", defaultValue: "Zone 3", required: true
		input "zone4", "time", title: "Zone 4 start time", required: true
		input "zone4Temperature", "decimal", title: "Zone 4 temperature", defaultValue: 15, required: true
        input "zone4Name", "string", title: "Zone 4 name", defaultValue: "Zone 4", required: true
	}
	section("Saturday and Sunday Schedule") {
		input "zone1Weekend", "time", title: "Zone 1 start time", required: true
		input "zone1WeekendTemperature", "decimal", title: "Zone 1 temperature", defaultValue: 15, required: true
        input "zone1WeekendName", "string", title: "Zone 1 name", defaultValue: "Zone 1 Weekend", required: true
		input "zone2Weekend", "time", title: "Zone 2 start time", required: true
		input "zone2WeekendTemperature", "decimal", title: "Zone 2 temperature", defaultValue: 15, required: true
        input "zone2WeekendName", "string", title: "Zone 2 name", defaultValue: "Zone 2 Weekend", required: true
		input "zone3Weekend", "time", title: "Zone 3 start time", required: true
		input "zone3WeekendTemperature", "decimal", title: "Zone 3 temperature", defaultValue: 15, required: true
        input "zone3WeekendName", "string", title: "Zone 3 name", defaultValue: "Zone 3 Weekend", required: true
		input "zone4Weekend", "time", title: "Zone 4 start time", required: true
		input "zone4WeekendTemperature", "decimal", title: "Zone 4 temperature", defaultValue: 15, required: true
        input "zone4WeekendName", "string", title: "Zone 4 name", defaultValue: "Zone 4 Weekend", required: true
	}
    section("Boost") {
		input "boostDuration", "number", title: "Boost duration (5 - 60 minutes)", range: "5..60", defaultValue: 60, required: true
		input "boostTemperature", "decimal", title: "Amount to increase temperature by (1-10)", range: "1..10", defaultValue: 1, required: true
    }
    section("Minimum on time") {
		input "minOnTime", "number", title: "Minimum time the outlets stay on (0-10 minutes)", range: "0..10", defaultValue: 0, required: true
    }

}
// ********************************************************************************************************************
def installed()
{
	log.debug "Installed: Running installed"
	
    // Generate a random number that is used in the ID for the virtual thermostat that is created
    state.deviceID = Math.abs(new Random().nextInt() % 9999) + 1
	
    // Flags to signify what the status of the various modes are 
    state.contact = true
	state.presence = true
    state.motion = true
    state.boost = false
	
    // variables used to store the current zone name/temp before entering the mode
    state.previousZoneNamePresence = null
    state.previousZoneTemperaturePresence = null
    state.previousZoneNameMotion = null
    state.previousZoneTemperatureMotion = null
    state.previousZoneNameBoost = null
    state.previousTemperatureBoost = null
	state.previousZoneNameContact = null
    
    state.presenceAwayScheduled = false

	// variables used to calculate todays and yesterdays on time
	state.todayTime = 0
	state.yesterdayTime = 0
    state.turnOnTime = 0
	state.lastOn = 0
    
    state.date = new Date().format("dd-MM-yy")
	
    // variable used to store what time presence was detected
    state.presenceTime = Math.round(new Date().getTime() / 1000)
	
    // Variable to store the current day (used to detect midnight/day change)
	state.storedDay = ""
    
	// Flags to only allow the temperature to be set once by a zone change (to allow a user to manually override the temp until next Zone) 
	state.zone1Set = false
	state.zone2Set = false
	state.zone3Set = false
	state.zone4Set = false
	state.zone1WeekendSet = false
	state.zone2WeekendSet = false
	state.zone3WeekendSet = false
	state.zone4WeekendSet = false
    
}
// ********************************************************************************************************************
// Create the Thermostat device (used in conjuction the the Virtual Thermostat Device With Schedule Device Handler
def createDevice() {
	def thermostat
	def label = app.getLabel()

	log.debug "CreateDevice: Create device with id: pmvtws$state.deviceID, named: $label"
	
	try {
		thermostat = addChildDevice("JayUK", "Virtual Thermostat Device With Schedule", "pmvtws" + state.deviceID, null, [label: label, name: label, completedSetup: true])
	} catch(e) {
		log.error("CreateDevice: Caught exception", e)
	}
	
	return thermostat
}
// ********************************************************************************************************************
// Function used to find the Virtual Thermostat Device
def getThermostat() {
	def child = getChildDevices().find {
	d -> d.deviceNetworkId.startsWith("pmvtws" + state.deviceID)
	}
	return child
}
// ********************************************************************************************************************
// Routine to delete the Virtual Thermostat Device With Schedule
def uninstalled() {
    	deleteChildDevice("pmvtws" + state.deviceID)
}
// ********************************************************************************************************************
// Routine that is called each time the Virtual Thermostat is configured
def updated() {
	log.debug "Updated: $app.label"
	unsubscribe()
	unschedule()

	// Get the actual Virtual Thermostat device object
	def thermostat = getThermostat()

	// If we havent found the Virtual Thermostat device then we'll create one
	if(thermostat == null) {
		log.debug "Updated: Creating thermostat"
		thermostat = createDevice()
	}

	// Reset the on-time variables to 0 if not set
	if(state.todayTime == null) state.todayTime = 0
	if(state.yesterdayTime == null) state.yesterdayTime = 0
	if(state.date == null) state.date = new Date().format("dd-MM-yy")
	if(state.lastOn == null) state.lastOn = 0
 
 	state.zone1Set = false
    state.zone2Set = false
    state.zone3Set = false
    state.zone4Set = false
    state.zone1WeekendSet = false
    state.zone2WeekendSet = false
    state.zone3WeekendSet = false
    state.zone4WeekendSet = false
    
 	// Subscribe to the temperature sensor objects (specifically the temperature attribute) and run the handler each time the temperature changes
	subscribe(sensors, "temperature", temperatureHandler)
    
    // Subscribe to the Virtual Thermostat boost attribute and run the handler each time it changes
	subscribe(thermostat, "thermostatBoost", thermostatBoostHandler)
    
    // Subscribe to the Virtual Thermostat Setpoint attribute and run the handler each time it changes
	subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
    
    // Subscribe to the Virtual Thermostat mode attribute and run the handler each time it changes
	subscribe(thermostat, "thermostatMode", thermostatModeHandler)
	
    // Clear the temperature sensor(s) data
    thermostat.clearSensorData()
    
    // Get the current temp from the averaged values from the temperature sensors
    state.currentTemp = getAverageTemperature()
    
    // Set the Virtual Thermostat temperature to the average temperature reported by the temperature sensors
	thermostat.setVirtualTemperature(state.currentTemp)
    
    // Set the temp scale to C or F
	thermostat.setTemperatureScale(parent.getTempScale())
	
    // If we have any contact sensors selected we'll subscribe the handler to their contact attribute
    if (contacts) {
		log.debug "Updated: Contact sensor(s) selected"
		subscribe(contacts, "contact", contactHandler)
	} else {
		log.debug "Updated: No contact sensor selected"
	}

	// If we have any presence devices selected we'll subscribe the handler to their presence attribute
	if (presences) {
		log.debug "Updated: Presence sensor(s) selected"
		subscribe(presences, "presence", presenceHandler)
	} else {
		log.debug "Updated: No presence sensor selected"
	}
    
    // If we have any motion sensors selected we'll subscribe the handler to their inactive attribute
    if (motions) {
		log.debug "Updated: Motion sensor(s) selected"
		subscribe(motions, "motion.inactive", motionHandler)
	} else {
		log.debug "Updated: No motion sensor selected"
	}

	// Update the on-time totals every hour
    runEvery1Hour(updateTimings)
    
    // Execute the initilize loop
    initialize()
}
// ********************************************************************************************************************
// Routine that calls itself every minute
def initialize() {

	// Check to see if anything has changed and update accordingly 
	evaluateRoutine()
    
    // Reschedule this routine to run in 60 seconds (making an event based platform into a run/realtime one
    // Can not use runEvery1minute due to it producing an error with the date/time .format command
    runIn(60,initialize)
}
// ********************************************************************************************************************
// Calculate the average temperature from all the selected temperature sensors
def getAverageTemperature() {
	def total = 0;
	def count = 0;
	def sensorTemperature = 0;
    def sensorName = "";
	
	for(sensor in sensors) {
    	sensorTemperature = sensor.currentValue("temperature")
        sensorName = sensor.label
        
        // Only include sensors that are within the specified range (rules out invalid/faulty sensors)
    	if (sensorTemperature >= sensorMinimum && sensorTemperature <= sensorMaximum) {
			total += sensorTemperature
			thermostat.setIndividualTemperature(sensorTemperature, count, sensorName)
			count++
        } else {
        	log.debug "Sensor: $sensorName is out of range ($sensorMinimum to $sensorMaximum): sensorTemperature)"
        }
	}
	return total / count
}
// ********************************************************************************************************************
// Handler for when the temperature sensor data changes
def temperatureHandler(evt) {
	def thermostat = getThermostat()
    
    // Get the current temp from the averaged values from the temperature sensors
    state.currentTemp = getAverageTemperature()
    
	thermostat.setVirtualTemperature(state.currentTemp)

	// Only perform an evaluation if someone is presence/moving/doors closed, or emergency temp is set, or an away temp is set
	if ((state.contact && (state.motion || motionAwaySetpoint != null) && (state.presence || presenceAwaySetpoint != null)) || emergencySetpoint != null) {
		evaluateRoutine()
	} else {
    	// Otherwise turn the heating off
		heatingOff()
	}
}
// ********************************************************************************************************************
// Handler if someone has pressed the 'boost' button
def thermostatBoostHandler(evt) {
	log.debug "ThermostatBoostHandler: Boost has been requested. Boost value: $state.boost"
      
    def thermostat = getThermostat()
    
    // If we arent currently boosted
    if (state.boost == false) {
    	log.debug "ThermostatBoostHandler: Not currently boosted, remembering previous values"
    	state.previousZoneNameBoost = thermostat.currentValue("zoneName")
        state.previousTemperatureBoost = thermostat.currentValue("thermostatSetpoint")
    
    	// Increase the current temp by the specified boost amount
    	def boostTemp = thermostat.currentValue("thermostatSetpoint") + boostTemperature
    
    	def nowtime = now()
        
        // Add boost duration to the current time
		def nowtimePlusBoostDuration = nowtime + (boostDuration * 60000)
        def boostEndTime = new Date(nowtimePlusBoostDuration)

        log.debug "ThermostatBoostHandler: Setting zonename to 'Boosted' and thermostat temperature to $boostTemp"
        
        // Update the thermostat to say we are boosted and set the new boosted temperature
    	setThermostat("Boosted" + "\n" + "(" + boostEndTime.format('HH:mm',location.timeZone) + ")",boostTemp)
       
    	log.debug "ThermostatBoostHandler: Scheduling boost to be removed in $boostDuration minutes"
    	runIn((boostDuration*60),boostOff)
  
  		// Update flag to state we are in boosted mode
  		state.boost = true
  } else {
  	log.debug "ThermostatBoostHandler: Already boosted, not doing anything"
  }    
	
  // Force an evaluation
  evaluateRoutine()
}
// ********************************************************************************************************************
// Boost time has expired, so we'll leave boost mode
def boostOff() {

	// We are boosted, so restoring everything back to pre boost-mode values
	if (state.boost) {
		log.debug "BoostOff: Restoring previous values, Zonename: $state.previousZoneNameBoost Temperature: state.previousTemperatureBoost"
		state.boost = false
        setThermostat(state.previousZoneNameBoost,state.previousTemperatureBoost)
        
        log.debug "BoostOff: Canceling any scheduled boostOff jobs"
        unschedule (boostOff)
	} else {
    	// We aren't boosted
    	log.debug "BoostOff: Dont have to reset boosted Zone names or temperature due to thermostat temperature or zone change during boost period"
    }
}
// ********************************************************************************************************************
// Handler to deal with presence events. It's a bit complicated as it supports a configurable minimum time for a presence 
// to stay active. Eg: if configured for 5 minutes, if it detects a presence and then that presence goes away 3 minutes
// after they arrived, then the status wont change to away for another 2 minutes (3 + 2 = minimum presence was was 5 minutes)
def presenceHandler(evt) {
    def thermostat = getThermostat()
    
    def presenceHere = false
    
    // Lets loop through all the presence sensors and check their status
    for(presenceSensor in presences) {
        if (presenceSensor.currentPresence == "present") {
            log.debug "PresenceHandler: Presence detected, sensor: $presenceSensor"
            presenceHere = true
        } 
    }
  
    if (state.presence == false && presenceHere) {
        // We are already in presence-away mode but now have a presence
        
        if (presenceAwaySetpoint != null) {
        	log.debug "PresenceHandler: We have detected a presence and we had an away temp set, setting zone name and temp back to previous values (temporary until we check what Zone we should be in"
        	setThermostat(state.previousZoneNamePresence,state.previousZoneTemperaturePresence)
        } else {
        	log.debug "PresenceHandler: We have detected a presence and but we dont have an away temp set, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        	thermostat.setZoneName(state.previousZoneNamePresence)
        }
        // Setting the flag to true, storing the time the presence was detected, cancelling the presence away delay (if set)
        state.presence = true
        state.presenceTime = Math.round(new Date().getTime() / 1000)
        unschedule(presenceAway)
        evaluateRoutine()
    } else if (state.presence == false && presenceHere == false) {
    	log.debug "PresenceHandler: Already in away mode and all presence sensors are set as away - Doing nothing"
    } else if(state.presence && presenceHere == false) {
    	log.debug "PresenceHandler: First occurance of all presence sensors being away, so scheduling/rescheduling presenceAway to run"
    	              
        // Store the current Zone name and temperature              
        state.previousZoneNamePresence = thermostat.currentValue("zoneName")  
    	state.previousZoneTemperaturePresence = thermostat.currentValue("thermostatSetpoint")
         
        if (presenceMinimumDuration > 0) {
        	// If we have a minimum presence time, convert it to seconds
        	def presenceMinimumDurationSeconds = presenceMinimumDuration * 60
        
        	// Work out how long the presence has been active for
            def time = Math.round(new Date().getTime() / 1000)
            def presenceDuration = time - state.presenceTime

            if (presenceDuration < presenceMinimumDurationSeconds) {
				// If the presence active time is less than the specified minimum time            
                log.debug "PresenceHandler: Presence duration is below specified minimum - Duration: $presenceDuration Minimum: $presenceMinimumDurationSeconds"
                
                // Work out the difference beetwen the active time and the specified minimum time and convert to milliseconds
                def presenceExtraDurationSeconds = presenceMinimumDurationSeconds-presenceDuration
                def presenceAwayTime = new Date(now() + (presenceExtraDurationSeconds*1000))

				// Update the thermostat to show at what time the presence will go to "away" mode
				thermostat.setZoneName("Presence: Away at ${presenceAwayTime.format('HH:mm')}")
                
                // Due to a limition of the ST scheduler, it doesnt like anything below 60 seconds from now, so we'll hand this
                if (presenceExtraDurationSeconds > 60) {
                	// Time is more than 60 seconds in the future, scheduling the "away" mode change
                    log.debug "PresenceHandler: Presence duration is below specified minimum, scheduling for minimum period - Scheduling to run in: $presenceExtraDurationSeconds seconds"
                    state.presenceAwayScheduled = true
                    runIn(presenceExtraDurationSeconds, presenceAway)
                } else {
                	// Time is less than 60 seconds in the furture, making it 60 seconds
                    log.debug "PresenceHandler: Remaining minimum duration is less than 60 seconds, scheduling presenceAway to run in 60 seconds"
                    state.presenceAwayScheduled = true
                    runIn(60, presenceAway)
                }
            } else {
            	// We have exceeded the minimum specified presence time period, so can set "away" mode straight away
				log.debug "PresenceHandler: Presence duration has exceeded minimum specified value, running presenceAway now"
        		presenceAway()
            }
    	} else {
        	log.debug "PresenceHandler: No minimum duration specified, running presenceAway now"
        	presenceAway()
        }
	} else if (state.presenceAwayScheduled & presenceHere) {
    	// We are in the scheduled/pending "away" mode phase but have now detected a presence, so need to cancel the scheduled "away" mode 
        state.presenceAwayScheduled = false
        state.presenceTime = Math.round(new Date().getTime() / 1000)
        unschedule(presenceAway)

        log.debug "PresenceHandler: We have detected a presence while pending, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        thermostat.setZoneName(state.previousZoneNamePresence)

        evaluateRoutine()
    }
}
// ********************************************************************************************************************
// Handler to deal with any detected motions
def motionHandler(evt) {
               
    log.debug "MotionHandler: Event occured: $evt.value"
    
    def motionDetected = false

	// Loop through each selected motion sensor
	for(motionSensor in motions) {
        if (motionSensor.ActivityStatus == "active") {
            log.debug "MotionHandler: A sensor is showing activity: $motionSensor"
            motionDetected = true
        }
    }

	// We've previously detected no motion, but now there is movement
	if (state.motion == false && motionDetected) {
        log.debug "MotionHandler: Activity detected and we're in away mode. Exiting away mode: Resetting zone details and unscheduling motionOff"
        if (motionAwaySetpoint != null) {
            // Motion away temperature is specified
            log.debug "MotionHandler: We have detected motion and we had an away temp set, setting zone name and temperature back to previous values (temporary until we check what Zone we should be in"
        	setThermostat(state.previousZoneNameMotion,state.previousZoneTemperatureMotion)
        } else {
        	// No motion away temperature has been set
        	log.debug "MotionHandler: We have detected motion and but we dont have an away temperature set, setting just the zone name back to previous value (temporary until we check what Zone we should be in"
        	thermostat.setZoneName(state.previousZoneNameMotion) 
        }
        // Reset the flag, unschedule any pending "motion away" events
        state.motion = true
        unschedule (motionOff)
        evaluateRoutine()
        
    } else if (state.motion == false && motionDetected == false) {
		log.debug "MotionHandler: Motion not detected and already in away mode. Doing nothing"
    } else if (state.motion && motionDetected == false) {
    	// First occurance of no motion, scheduling the "away" mode to happen after the specified delay
    	log.debug "MotionHandler: First occurance of all motion sensors being away, so scheduling/rescheduling motionOff to run from now plus duration time"
        runIn(motionDuration*60, motionOff)
    } else if (state.motion && motionDetected) {
    	log.debug "MotionHandler: Detected motion while in the scheduled 'away' mode phase, cancelling schedule and returning back to normal"
        unschedule (motionOff)
        evaluateRoutine()
    }
}
// ********************************************************************************************************************
// Change the thermostat to Motion Away mode
def motionOff() {
	
    log.debug "MotionOff: Executing"
    
    // Store the current thermostat values
    state.previousZoneNameMotion = thermostat.currentValue("zoneName")  
    state.previousZoneTemperatureMotion = thermostat.currentValue("thermostatSetpoint")
      
    state.motion = false
      
     if (motionAwaySetpoint != null) {
        	log.debug "MotionOff: motionAwaySetpoint: $motionAwaySetpoint - Adjusting thermostat accordingly and leaving heating enabled"
			setThermostat("Motion: Away",motionAwaySetpoint)
            evaluateRoutine()
		} else {
        	log.debug "MotionOff: No away temp set, turning off heating"
        	thermostat.setZoneName("Motion: Away")
            heatingOff()
        }
}
// ********************************************************************************************************************
// Handler to deal with any detected contacts
def contactHandler(evt) {
               
    log.debug "ContactHandler: Event occured: $evt.value"
    
    def contactOpen = false

	// Loop through each selected contact sensor
	for(contactSensor in contacts) {
        if (contactSensor.ContactState == "open") {
            log.debug "ContactHandler: A sensor is showing activity: $contactSensor"
            contactOpen = true
        }
    }

	// We've previously detected an open contact, but now they are all closed
	if (state.contact == false && contactOpen == false) {
        log.debug "ContactHandler: Contacts closed detected and we're in away mode. Exiting away mode: Resetting zone details and unscheduling contactOff"
        thermostat.setZoneName(state.previousZoneNameContact) 
        // Reset the flag, unschedule any pending "contact away" events
        state.contact = true
        unschedule (contactOff)
        evaluateRoutine()     
    } else if (state.contact == false && contactOpen) {
		log.debug "ContactHandler: Contact open and already in away mode. Doing nothing"
    } else if (state.contact && contactOpen) {
    	// First occurance of an open contact, scheduling the "away" mode to happen after the specified delay
    	log.debug "ContactHandler: First occurance of an open contact, so scheduling/rescheduling contactOff to run from now plus duration time"
        runIn(contactDuration*60, contactOff)
    } else if (state.contact && contactOpen == false) {
    	log.debug "ContactHandler: Detected all contacts closed while in the scheduled 'away' mode phase, cancelling schedule and returning back to normal"
        unschedule (contactOff)
        evaluateRoutine()
    }
}
// ********************************************************************************************************************
// Change the thermostat to Contact open mode
def contactOff() {
	
    log.debug "ContactOff: Executing"
    
    // Store the current thermostat values
    state.previousZoneNameContact = thermostat.currentValue("zoneName")  
      
    state.contact = false
      
    log.debug "ContactOff: Turning off heating"
    thermostat.setZoneName("Contact: Open")
    heatingOff()
}
// ********************************************************************************************************************
// Changing the thermostat to Presence Away mode
def presenceAway() {
	
    log.debug "PresenceAway: Executing"
          
    // Reset the various flags and values
    state.presence = false
    state.presenceTime = 0
    state.presenceAwayScheduled = false
    
     if (presenceAwaySetpoint != null) {
     	// We have an away temperature set
        log.debug "PresenceAway: presenceAwaySetpoint: $presenceAwaySetpoint - Adjusting thermostat accordingly and leaving heating enabled"
        setThermostat("Presence: Away",presenceAwaySetpoint)
        evaluateRoutine()
     } else {
     	// We don't have an away temperature set, so turning the heating off
        log.debug "PresenceAway: No away temp set, turning off heating"
        thermostat.setZoneName("Presence: Away")
        heatingOff()
     }
}
// ********************************************************************************************************************
// Function used when temperature on virtual thermostat is changed
def thermostatTemperatureHandler(evt) {
	
    if (state.boost) {
    	// We are in boost mode, if someone manual adjusts the temperature while in this mode then we'll exit it
		log.debug "ThermostatTemperatureHandler: Restoring zone name from 'Boosted' to previous name: $state.previousZoneNameBoost"
		
        // Reset the flag and unschedule the boostoff process
        state.boost = false
        unschedule (boostOff)
        
	    def thermostat = getThermostat()
        
        // Simply reset the zone name back to the original (don't adjust the temp)
        thermostat.setZoneName(state.previousZoneNameBoost)
	} else {
    	log.debug "ThermostatTemperatureHandler: Not in 'boost' mode, nothing to reset"
    }    
    
    evaluateRoutine()
}
// ********************************************************************************************************************
// Routine for when the thermostat mode changes
def thermostatModeHandler(evt) {
	
    def mode = evt.value
	log.debug "ThermostatModeHandler: Mode Changed to: $mode"
    
    if (mode == "heat") {
    	// If the thermostat is in heat mode
        
        if (state.contact && (state.presence || presenceAwaySetpoint != null) && (state.motion || motionAwaySetpoint != null)) {
			// If all contacts are closed and we have presence (or an away temp is set) and we have motion (or an away temp is set)
        	log.debug "ThermostatModeHandler: Contact/Presence is True, performing evaluation"
            evaluateRoutine()
		}
		else {
        	// A contact is open, or everyone is away (and no away temp is set) or there is no motion (and no away temp is set) then we'll turn off the heating
        	log.debug "ThermostatModeHandler: Either no presence (or presence temp not set), or Contact open, no motion (or motion temp not set), turning off heating"
			heatingOff(mode == 'heat' ? false : true)
		}
	} else {
    	// We arent in heating mode
       	log.debug "ThermostatModeHandler: Heating off"
			heatingOff(mode == 'heat' ? false : true)
    }
}
// ********************************************************************************************************************
// Main routine that checks the various conditions (contacts, presence and motion), compares is the current temp matches the required temp
// and whether we need emergency heating
private evaluateRoutine() {
	
	// Make sure we have the right zone name and temp based on the correct day and time
	setRequiredZone()
       
    // Get the desired temp from the virtual thermostat
    def desiredTemp = thermostat.currentValue("thermostatSetpoint")
	
    // Get the current mode from the virtual thermostat
    def heatingMode = thermostat.currentValue('thermostatMode')
    
	log.debug "EvaluateRoutine: Current: $state.currentTemp, Desired: $desiredTemp, Heating mode: $heatingMode"
	       
    if (state.currentTemp <= emergencySetpoint) {
    	// The current temp is below the specified emergency temperature, turning on heating (even if the thermostat is turned off)
    	log.debug "EvaluateRountine: In Emergency Mode, turning on"
        thermostat.setEmergencyMode(true)
        outletsOn()
    } else if ((desiredTemp - state.currentTemp) >= belowThreshold) {
    	// Current temperature is below the desired temperature with the specified threshold
        log.debug "EvaluateRoutine: Current temperature is below desired temperature (with threshold)"
 
 		if(thermostat.currentValue('thermostatMode') == 'heat') {
			log.debug " EvaluateRoutine: Heating is enabled"
            
            if (state.contact && (state.motion || motionAwaySetpoint != null) && (state.presence || presenceAwaySetpoint != null)) {
            	// All contacts are closed and we have motion (or an away temperature is set) and we have presence (or an away temperature is set)
                if (state.presence && presences) {
               		log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed and someone is present - Turning on"
                } else {
                	log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed, no one present but presence away temp set - Turning on"
                }
                if (state.motion && motions) {
               		log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed and someone is moving - Turning on"
                } else {
                	log.debug "EvaluateRoutine: Heating is enabled - All contacts are closed, no one is moving but motion away temp set - Turning on"
                }
                // Turn on the heating
                thermostat.setHeatingStatus(true)
            	outletsOn()
            } else {
	            log.debug "EvaluateRoutine: Heating is enabled - But a contact is open, or no one is present (or not and no away temp set), or no one is moving (or not and no away temp set) - Turning off"
    	        heatingOff()  
            }
        } else {
            log.debug " EvaluateRoutine: Heating is disabled - Turning off"      
            heatingOff()
        }
    } else if ((state.currentTemp - desiredTemp) >= aboveThreshold) {
    	// Current temperature is above the specified temperaure with threshold, turning off heating
        log.debug "EvaluateRoutine: Current temperature is above desired temp (with threshold) - Turning off"    
        heatingOff()
    } else {
    	log.debug "EvaluateRoutine: Current temperature matches desired temperature (within the thresholds) - Doing nothing"
    }
    
    if(state.current == "on") {
    	// Update the values for the time the boiler has been on
        updateTimings()
    }
}
// ********************************************************************************************************************
// Turn the heating off
def heatingOff(heatingOff) {
	  
    // Get the current time (Unix time) i seconds
    def time = Math.round(new Date().getTime() / 1000)
    
    // Calculate how long the heating has been on for 
    def onFor = time - state.turnOnTime
    
    // Convert the specified minimum on time to seconds
    def minOnTimeSeconds = minOnTime * 60
    
    log.debug "HeatingOff: state.turnOnTime: $state.turnOnTime  time: $time   difference: $onFor     minOnTime: $minOnTimeSeconds"
    
	if (state.currentTemp <= emergencySetpoint) {
    	// Current temp is below the specified emergency temperature, turning on the heating
		log.debug "HeatingOff: In Emergency Mode, not turning off"
		outletsOn()
		thermostat.setEmergencyMode(true)
	} else {
    	// Current temperature is above any specified emergency temperature
    	if (onFor >= minOnTimeSeconds) {
        	// The heating has been on for longer than any specified minimum period, turning off
            if (thermostat.currentValue('thermostatMode') == 'heat') {
            	log.debug "HeatingOff: Time on is greater than specified minimum on period - turning off"
                thermostat.setHeatingStatus(false)
            } else {
            	log.debug "HeatingOff: setHeatingOff to True - Thermostat has been turned off"
                thermostat.setHeatingOff(true)
            }
            
            outletsOff()
        } else {
        	// The heating has not yet been on for the specified minimum period
        	log.debug "HeatingOff: Time on is less than than specified minimum on period - doing nothing"
        }
	}
}
// ********************************************************************************************************************
def updateTempScale() {
	thermostat.setTemperatureScale(parent.getTempScale())
}
// ********************************************************************************************************************
// Routine to update the values for todays and yesterdays on time
def updateTimings() {
   	def date = new Date().format("dd-MM-yy")

	if(state.current == "on") {
		// If the outlet is on, update the values
		int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
		state.lastOn = Math.round(new Date().getTime() / 1000)
	}

	if(state.date != date) {
    	// If we have changed to a new day, use today's values to update yesterdays
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}

	// Send the new values to the virtual thermostat
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}
// ********************************************************************************************************************
// Routine to turn on the outlets
def outletsOn() {
	
    outlets.on()
	
    // Get todays date
    def date = new Date().format("dd-MM-yy")
	
	if(state.current == "on") {
		// If the outlet is currently on then get the current time, minus the time it was last on and add the difference to the on-time variable
        int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
	}

	if(state.date != date) {
    	// We have changed days, use today's values to update the yesterday's totals
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}
	
    // Update the various flags and variables with the lastest on-time values
	state.lastOn = Math.round(new Date().getTime() / 1000)
	state.current = "on"
    state.turnOnTime = Math.round(new Date().getTime() / 1000)
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}
// ********************************************************************************************************************
// Turn off the outlets
def outletsOff() {

	outlets.off()
	
    // Get the current date
    def date = new Date().format("dd-MM-yy")

	if(state.current == "on") {
		// If the outlets are currently on, work out how long they have been on for and add that to the running total for the day
		int time = Math.round(new Date().getTime() / 1000) - state.lastOn
		state.todayTime = state.todayTime + time
	}
	
	if(state.date != date) {
    	// We have changed days, use todays values to replace yesterdays totals
		state.yesterdayTime = state.todayTime
		state.date = date
		state.todayTime = 0
	}
	
    // Reset the various flags and variables to clear them
	state.current = "off"
    state.turnOnTime = 0
	state.lastOn = 0
	thermostat.setTimings(state.todayTime, state.yesterdayTime)
}
// ********************************************************************************************************************
// Identify we zone we should be in based on day and time and set the temperature/zone name accordingly
// We will only perform the update once per zone change, this will prevent any user specified temperature being reset
// each time this routine is called (per minute)
def setRequiredZone() {
       
    def calendar = Calendar.getInstance()
    calendar.setTimeZone(location.timeZone)

    def today = calendar.get(Calendar.DAY_OF_WEEK)
    def timeNow = now()
    def midnightToday = timeToday("2000-01-01T23:59:59.999-0000", location.timeZone)

    if (today != state.storedDay) {
        // Reset the zone flags and update the day because of midnight change

        log.debug "setRequiredZone: The day has changed since the last zone change, reseting zone check flags"

        state.zone1Set = false
        state.zone2Set = false
        state.zone3Set = false
        state.zone4Set = false
        state.zone1WeekendSet = false
        state.zone2WeekendSet = false
        state.zone3WeekendSet = false
        state.zone4WeekendSet = false

        state.storedDay = today
    }

    // This section is where the time/temperature schedule is set
    switch (today) {
        // Only Monday
        case Calendar.MONDAY:

        if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4WeekendSet) { 
            // Are we between midnight Sunday and 1st zone Monday, we're still in Sunday zone 4             

            log.debug "SetRequiredZone: Sunday - Zone 4 (after midnight)"
            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = true

            state.boost = false
            unschedule(boostOff)
            
            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4WeekendName
            state.previousTemperatureBoost = zone4WeekendTemperature
            state.previousZoneNamePresence = zone4WeekendName
            state.previousZoneTemperaturePresence = zone4WeekendTemperature
            state.previousZoneNameMotion = zone4WeekendName
            state.previousZoneTemperatureMotion = zone4WeekendTemperature
            state.previousZoneNameContact = zone4WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4WeekendName,zone4WeekendTemperature)     
            }
       } 

        else if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
            // Are we between 1st zone and 2nd zone                

            log.debug "SetRequiredZone: Mon - Zone 1"

            // Set the flag to specify what zone we are
            state.zone1Set = true
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false

            // Disable any boost mode
            state.boost = false

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone1Name
            state.previousTemperatureBoost = zone1Temperature
            state.previousZoneNamePresence = zone1Name
            state.previousZoneTemperaturePresence = zone1Temperature
            state.previousZoneNameMotion = zone1Name
            state.previousZoneTemperatureMotion = zone1Temperature
            state.previousZoneNameContact = zone1Name

            unschedule(boostOff)

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone1Name,zone1Temperature)     
            }
        } 

        else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
            // Are we between 2nd zone and 3rd zone

            log.debug "SetRequiredZone: Mon - Zone 2"

            state.zone1Set = false
            state.zone2Set = true
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            
            state.boost = false
            unschedule(boostOff)
            
            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone2Name
            state.previousTemperatureBoost = zone2Temperature
            state.previousZoneNamePresence = zone2Name
            state.previousZoneTemperaturePresence = zone2Temperature
            state.previousZoneNameMotion = zone2Name
            state.previousZoneTemperatureMotion = zone2Temperature
            state.previousZoneNameContact = zone2Name
            
            if (state.contact && state.motion && state.presence) {
                setThermostat(zone2Name,zone2Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
            // Are we between 3rd zone and 4th zone    

            log.debug "SetRequiredZone: Mon - Zone 3"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = true
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            
            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone3Name
            state.previousTemperatureBoost = zone3Temperature
            state.previousZoneNamePresence = zone3Name
            state.previousZoneTemperaturePresence = zone3Temperature
            state.previousZoneNameMotion = zone3Name
            state.previousZoneTemperatureMotion = zone3Temperature
            state.previousZoneNameContact = zone3Name

			state.boost = false
            unschedule(boostOff)
            
            if (state.contact && state.motion && state.presence) {
                setThermostat(zone3Name,zone3Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
            // Are we between 4th zone and midnight, we're in zone 4    

            log.debug "SetRequiredZone: Mon - Zone 4 (upto midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            unschedule(boostOff)
            
            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        break

        case Calendar.TUESDAY:
        case Calendar.WEDNESDAY:
        case Calendar.THURSDAY:

        if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4Set) { 
            // Are we between midnight yesterday and 1st zone, we're still in zone 4 from perevious day            

            log.debug "SetRequiredZone: Tue-Thu - Zone 4 (after midnight)"
            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            unschedule(boostOff)
            
            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
            // Are we between 1st zone and 2nd zone                

            log.debug "SetRequiredZone: Tue-Thu - Zone 1"

            // Set the flag to specify what zone we are
            state.zone1Set = true
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false

            // Disable any boost mode
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone1Name
            state.previousTemperatureBoost = zone1Temperature
            state.previousZoneNamePresence = zone1Name
            state.previousZoneTemperaturePresence = zone1Temperature
            state.previousZoneNameMotion = zone1Name
            state.previousZoneTemperatureMotion = zone1Temperature
            state.previousZoneNameContact = zone1Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone1Name,zone1Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
            // Are we between 2nd zone and 3rd zone

            log.debug "SetRequiredZone: Tue-Thu - Zone 2"

            state.zone1Set = false
            state.zone2Set = true
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone2Name
            state.previousTemperatureBoost = zone2Temperature
            state.previousZoneNamePresence = zone2Name
            state.previousZoneTemperaturePresence = zone2Temperature
            state.previousZoneNameMotion = zone2Name
            state.previousZoneTemperatureMotion = zone2Temperature
            state.previousZoneNameContact = zone2Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone2Name,zone2Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
            // Are we between 3rd zone and 4th zone    

            log.debug "SetRequiredZone: Tue-Thu - Zone 3"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = true
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone3Name
            state.previousTemperatureBoost = zone3Temperature
            state.previousZoneNamePresence = zone3Name
            state.previousZoneTemperaturePresence = zone3Temperature
            state.previousZoneNameMotion = zone3Name
            state.previousZoneTemperatureMotion = zone3Temperature
            state.previousZoneNameContact = zone3Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone3Name,zone3Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
            // Are we between 4th zone and midnight, we're in zone 4    

            log.debug "SetRequiredZone: Tue-Thu - Zone 4 (upto midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        break

        case Calendar.FRIDAY:

        if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4Set) { 
            // Are we between midnight Thursday and 1st zone on Friday, we schedule Thursday zone 4                

            log.debug "SetRequiredZone: Thursday - Zone 4 (after midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone1, location.timeZone).time && timeNow < timeToday(zone2, location.timeZone).time && !state.zone1Set) { 
            // Are we between 1st zone and 2nd zone    

            log.debug "SetRequiredZone: Friday - Zone 1"

            state.zone1Set = true
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone1Name
            state.previousTemperatureBoost = zone1Temperature
            state.previousZoneNamePresence = zone1Name
            state.previousZoneTemperaturePresence = zone1Temperature
            state.previousZoneNameMotion = zone1Name
            state.previousZoneTemperatureMotion = zone1Temperature
            state.previousZoneNameContact = zone1Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone1Name,zone1Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone2, location.timeZone).time && timeNow < timeToday(zone3, location.timeZone).time && !state.zone2Set) { 
            // Are we between 2nd zone and 3rd zone                

            log.debug "SetRequiredZone: Friday - Zone 2"

            state.zone1Set = false
            state.zone2Set = true
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone2Name
            state.previousTemperatureBoost = zone2Temperature
            state.previousZoneNamePresence = zone2Name
            state.previousZoneTemperaturePresence = zone2Temperature
            state.previousZoneNameMotion = zone2Name
            state.previousZoneTemperatureMotion = zone2Temperature
            state.previousZoneNameContact = zone2Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone2Name,zone2Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone3, location.timeZone).time && timeNow < timeToday(zone4, location.timeZone).time && !state.zone3Set) { 
            // Are we between 3rd zone and 4th zone                

            log.debug "SetRequiredZone: Friday - Zone 3"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = true
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone3Name
            state.previousTemperatureBoost = zone3Temperature
            state.previousZoneNamePresence = zone3Name
            state.previousZoneTemperaturePresence = zone3Temperature
            state.previousZoneNameMotion = zone3Name
            state.previousZoneTemperatureMotion = zone3Temperature
            state.previousZoneNameContact = zone3Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone3Name,zone3Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone4, location.timeZone).time && timeNow < midnightToday.time && !state.zone4Set) { 
            // Are we between 4th zone Friday and midnight, we schedule Friday zone 4

            log.debug "SetRequiredZone: Friday - Zone 4 (upto midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        break

        case Calendar.SATURDAY:

        if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1Weekend, location.timeZone).time && !state.zone4Set) { 
            // Are we between midnight Friday and 1st zone Saturday, we're still in zone 4 from Friday

            log.debug "SetRequiredZone: Friday - Zone 4 (after midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = true
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4Name
            state.previousTemperatureBoost = zone4Temperature
            state.previousZoneNamePresence = zone4Name
            state.previousZoneTemperaturePresence = zone4Temperature
            state.previousZoneNameMotion = zone4Name
            state.previousZoneTemperatureMotion = zone4Temperature
            state.previousZoneNameContact = zone4Name

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4Name,zone4Temperature)     
            }
        }

        else if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
            // Are we between 1st zone and 2nd zone   

            log.debug "SetRequiredZone: Saturday - Zone 1"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = true
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone1WeekendName
            state.previousTemperatureBoost = zone1WeekendTemperature
            state.previousZoneNamePresence = zone1WeekendName
            state.previousZoneTemperaturePresence = zone1WeekendTemperature
            state.previousZoneNameMotion = zone1WeekendName
            state.previousZoneTemperatureMotion = zone1WeekendTemperature
            state.previousZoneNameContact = zone1WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone1WeekendName,zone1WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
            // Are we between 2nd zone and 3rd zone               

            log.debug "SetRequiredZone: Saturday - Zone 2"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = true
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone2WeekendName
            state.previousTemperatureBoost = zone2WeekendTemperature
            state.previousZoneNamePresence = zone2WeekendName
            state.previousZoneTemperaturePresence = zone2WeekendTemperature
            state.previousZoneNameMotion = zone2WeekendName
            state.previousZoneTemperatureMotion = zone2WeekendTemperature
            state.previousZoneNameContact = zone2WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone2WeekendName,zone2WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
            // Are we between 3rd zone and 4th zone                

            log.debug "SetRequiredZone: Saturday - Zone 3"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = true
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone3WeekendName
            state.previousTemperatureBoost = zone3WeekendTemperature
            state.previousZoneNamePresence = zone3WeekendName
            state.previousZoneTemperaturePresence = zone3WeekendTemperature
            state.previousZoneNameMotion = zone3WeekendName
            state.previousZoneTemperatureMotion = zone3WeekendTemperature
            state.previousZoneNameContact = zone3WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone3WeekendName,zone3WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
            // Are we between 4th zone and midnight, schedule zone 4                

            log.debug "SetRequiredZone: Saturday - Zone 4 (upto midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = true
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4WeekendName
            state.previousTemperatureBoost = zone4WeekendTemperature
            state.previousZoneNamePresence = zone4WeekendName
            state.previousZoneTemperaturePresence = zone4WeekendTemperature
            state.previousZoneNameMotion = zone4WeekendName
            state.previousZoneTemperatureMotion = zone4WeekendTemperature
            state.previousZoneNameContact = zone4WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4WeekendName,zone4WeekendTemperature)     
            }
        }

        break

        case Calendar.SUNDAY:

        if (timeNow >= (midnightToday - 1).time && timeNow < timeToday(zone1, location.timeZone).time && !state.zone4WeekendSet) { 
            // Are we between midnight Saturday and 1st zone on Sunday, schedule Saturday Zone 4                

            log.debug "SetRequiredZone: Saturday - Zone 4 (after midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = true
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone4WeekendName
            state.previousTemperatureBoost = zone4WeekendTemperature
            state.previousZoneNamePresence = zone4WeekendName
            state.previousZoneTemperaturePresence = zone4WeekendTemperature
            state.previousZoneNameMotion = zone4WeekendName
            state.previousZoneTemperatureMotion = zone4WeekendTemperature
            state.previousZoneNameContact = zone4WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4WeekendName,zone4WeekendTemperature)     
            }
        }

        if (timeNow >= timeToday(zone1Weekend, location.timeZone).time && timeNow < timeToday(zone2Weekend, location.timeZone).time && !state.zone1WeekendSet) { 
            // Are we between 1st zone and 2nd zone                

            log.debug "SetRequiredZone: Sunday - Zone 1"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = true
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone1WeekendName
            state.previousTemperatureBoost = zone1WeekendTemperature
            state.previousZoneNamePresence = zone1WeekendName
            state.previousZoneTemperaturePresence = zone1WeekendTemperature
            state.previousZoneNameMotion = zone1WeekendName
            state.previousZoneTemperatureMotion = zone1WeekendTemperature
            state.previousZoneNameContact = zone1WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone1WeekendName,zone1WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone2Weekend, location.timeZone).time && timeNow < timeToday(zone3Weekend, location.timeZone).time && !state.zone2WeekendSet) { 
            // Are we between 2nd zone and 3rd zone                

            log.debug "SetRequiredZone: Sunday - Zone 2"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = true
            state.zone3WeekendSet = false
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone2WeekendName
            state.previousTemperatureBoost = zone2WeekendTemperature
            state.previousZoneNamePresence = zone2WeekendName
            state.previousZoneTemperaturePresence = zone2WeekendTemperature
            state.previousZoneNameMotion = zone2WeekendName
            state.previousZoneTemperatureMotion = zone2WeekendTemperature
            state.previousZoneNameContact = zone2WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone2WeekendName,zone2WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone3Weekend, location.timeZone).time && timeNow < timeToday(zone4Weekend, location.timeZone).time && !state.zone3WeekendSet) { 
            // Are we between 3rd zone and 4th zone               

            log.debug "SetRequiredZone: Sunday - Zone 3"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = true
            state.zone4WeekendSet = false
            state.boost = false
            unschedule(boostOff)

            // If a zone change has happened during a boost/presence away/motion away/contact open, then when we leave those states
            // we will reset/return to the new zone settings and not the values prior to that away state
            state.previousZoneNameBoost = zone3WeekendName
            state.previousTemperatureBoost = zone3WeekendTemperature
            state.previousZoneNamePresence = zone3WeekendName
            state.previousZoneTemperaturePresence = zone3WeekendTemperature
            state.previousZoneNameMotion = zone3WeekendName
            state.previousZoneTemperatureMotion = zone3WeekendTemperature
            state.previousZoneNameContact = zone3WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone3WeekendName,zone3WeekendTemperature)     
            }
        }

        else if (timeNow >= timeToday(zone4Weekend, location.timeZone).time && timeNow < midnightToday.time && !state.zone4WeekendSet) { 
            // Are we between 4th time Sunday and midnight, schedule Sunday zone 4                

            log.debug "SetRequiredZone: Sunday - Zone 4 (upto midnight)"

            state.zone1Set = false
            state.zone2Set = false
            state.zone3Set = false
            state.zone4Set = false
            state.zone1WeekendSet = false
            state.zone2WeekendSet = false
            state.zone3WeekendSet = false
            state.zone4WeekendSet = true
            state.boost = false
            unschedule(boostOff)
            state.previousZoneNameBoost = zone4WeekendName
            state.previousTemperatureBoost = zone4WeekendTemperature
            state.previousZoneNamePresence = zone4WeekendName
			state.previousZoneTemperaturePresence = zone4WeekendTemperature
            state.previousZoneNameMotion = zone4WeekendName
            state.previousZoneTemperatureMotion = zone4WeekendTemperature
            state.previousZoneNameContact = zone4WeekendName

            if (state.contact && state.motion && state.presence) {
                setThermostat(zone4WeekendName,zone4WeekendTemperature)     
            }
		}
    break
    }
}
// ********************************************************************************************************************
// Routine to update the virtual thermostat zone name and desired/required temperture
def setThermostat(zoneName,zoneTemperature) {

	thermostat.setHeatingSetpoint(zoneTemperature)
    thermostat.setZoneName(zoneName)
}
// ********************************************************************************************************************