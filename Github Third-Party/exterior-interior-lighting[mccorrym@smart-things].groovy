definition(
    name: "Exterior/Interior Lighting",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitor light sensor(s) to determine when to turn on or off selected interior and/or exterior lighting.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
	section("Choose the light sensor and switches you'd like to control.") {
        input "sensor", "capability.illuminanceMeasurement", required: true, title: "Which light sensor to monitor?"
        input "exterior_switches", "capability.switch", required: true, multiple: true, title: "Which exterior switch(es) to control?"
        input "exterior_target", "enum", required: true, description: "All exterior lights will be turned ON or OFF once this lumen value has been met.", title: "Which lumen value to target for exterior switches?", options: [200, 400, 600, 800]
        input "interior_switches", "capability.switch", required: false, multiple: true, title: "Which interior switch(es) to control? (optional)"
        input "interior_target", "enum", required: true, description: "All interior lights will be turned ON or OFF once this lumen value has been met.", title: "Which lumen value to target for interior switches?", options: [200, 400, 600, 800]
        input "interior_time_off", "time", required: false, description: "All interior lights chosen above will be turned ON at this time if they have not already been turned on due to low light.", title: "Choose a time to turn the interior lights off at night"
        input "interior_time_on", "time", required: false, description: "All interior lights chosen above will be turned OFF at this time if they have not already been turned off due to meeting the minimum interior lumen specified.", title: "Choose a time to turn the interior lights on in the morning"
        input "interior_exceptions", "capability.switch", required: false, multiple: true, description: "These are interior switches that do not turn ON in the morning or OFF at night based on the times indicated above. Instead, they turn on based on light detected and must be turned off manually.", title: "Choose any interior switch(es) that are exceptions (optional)"
	}
}

def installed() {
    initIlluminationState()
    subscribe(sensor, "illuminance", illuminanceChangeHandler)
    subscribe(switches, "switch", switchChangeHandler)
    unschedule(interiorLightsOffHandler)
    unschedule(interiorLightsOnHandler)
    if (interior_switches != null) {
    	schedule(interior_time_off, interiorLightsOffHandler)
        schedule(interior_time_on, interiorLightsOnHandler)
    }
}

def updated() {
    unsubscribe()
    initIlluminationState()
    subscribe(sensor, "illuminance", illuminanceChangeHandler)
    subscribe(switches, "switch", switchChangeHandler)
    unschedule(interiorLightsOffHandler)
    unschedule(interiorLightsOnHandler)
    if (interior_switches != null) {
    	schedule(interior_time_off, interiorLightsOffHandler)
        schedule(interior_time_on, interiorLightsOnHandler)
    }
}

def initIlluminationState() {
    state.on_date = [:]
    state.off_date = [:]
    state.on_date["exterior"] = null
    state.on_date["interior"] = null
    state.off_date["exterior"] = null
    state.off_date["interior"] = null
}

def switchChangeHandler (evt) {
    state.switch_value = evt.value
}

def illuminanceChangeHandler (evt) {
    def lux_measurement = evt.integerValue

    evalIlluminanceAction(lux_measurement, "exterior")
    
    if (interior_switches != null) {
    	evalIlluminanceAction(lux_measurement, "interior")
    }
}

def evalIlluminanceAction(lux_measurement, target) {
    log.trace ("Exterior target is ${exterior_target}. Interior target is ${interior_target}")
    
    def lux_target = null
    def switch_list = null
    
    switch(target) {
    	case "exterior":
            lux_target = exterior_target.toInteger()
            switch_list = exterior_switches
            break
        case "interior":
            lux_target = interior_target.toInteger()
            switch_list = interior_switches
            break
    }
    
    def valid_hour = timeOfDayIsBetween(interior_time_on, interior_time_off, new Date(), location.timeZone)

    if (state.on_date[target] != null && lux_measurement >= lux_target) {
        // False alarm. Keep the lights OFF and reset the 3 minute timer.
        log.trace ("False alarm - lux value is now ${lux_measurement}. Keeping ${target} lights off.")
        state.on_date[target] = null
    } else if (state.off_date[target] != null && lux_measurement <= lux_target) {
        // False alarm. Keep the lights ON and reset the 3 minute timer.
        log.trace ("False alarm - lux value is now ${lux_measurement}. Keeping ${target} lights on.")
        state.off_date[target] = null
    } else {
        log.trace ("Lux value is ${lux_measurement}. Target is ${lux_target}.")
    }

    if (lux_measurement < lux_target) {
        // Lights are off and lux measurement is < target. Should we turn the lights on?
        if (state.on_date[target] != null) {
            def current_date = new Date().getTime() / 1000
            // Check to make sure at least 3 minutes have passed (to avoid fluke values from turning the lights on/off in succession)
            if ((current_date - state.on_date[target]) >= 180) {
                // Reset the on_date to NULL to prepare for the next event
                state.on_date[target] = null

                // Check the status of the switches in the array. If all switches are already ON, exit the routine.
                def switches_on = true
                switch_list.any { object ->
                    if (object.currentSwitch == "off") {
                        switches_on = false
                        return true
                    }
                }
                if (switches_on) {
                    return
                }
                
                // Determine the current day of the week so that we won't turn the lights on in the morning on weekends
                def week_day_date = new java.text.SimpleDateFormat("u")
                // Ensure the new date object is set to local time zone
                week_day_date.setTimeZone(location.timeZone)
                def week_day = week_day_date.format(new Date())
 
                def current_hour_date = new java.text.SimpleDateFormat("H")
                // Ensure the new date object is set to local time zone
                current_hour_date.setTimeZone(location.timeZone)
                def hour = current_hour_date.format(new Date())
                
                // If the target is interior switches:
                // 		If the current time falls between the interior_time_off and interior_time_on, do nothing
                //		If the current time is before 8AM on a weekend, do nothing
                //		If the current time is before 12PM and the switch is in the list of interior_exceptions, do nothing
                //	Otherwise, turn the switch on
                if (target == "interior") {
                	if (!valid_hour) {
                		return
                    } 
                    if (hour.toInteger() < 8 && (week_day.toInteger() == 6 || week_day.toInteger() == 7)) {
                    	return
                    }
                }
                
                if (location.mode == "Keep Lights Off") {
                    sendNotificationEvent("[LIGHTING] Keeping lights OFF due to the Keep Lights Off mode being set.")
                } else {
                    log.trace("Turning ${target} lights ON!")
                    switch_list.each { object ->
                        def interior_exception = false
                        if (interior_exceptions != null) {
                            interior_exceptions.each { exception ->
                                if (exception.getLabel() == object.getLabel()) {
                                    interior_exception = true
                                    return
                                }
                            }
                        }
                        if (interior_exception && hour.toInteger() < 12) {
                        	// If it's before 12PM and the interior switch is in the list of exceptions, do nothing
                        } else {
                        	object.on()
                        }
                    }
            
                    if (hour.toInteger() > 15) {
                        sendPush("Good evening! ${target.capitalize()} lights are turning ON.")
                    } else {
                    	// Only send a notification of darkness during weekdays
                    	if (week_day.toInteger() < 6) {
                        	sendPush("${target.capitalize()} lights are turning ON due to darkness.")
                        }
                    }
                }
            }
        } else {
            state.on_date[target] = new Date().getTime() / 1000
            log.trace ("Target lux value has been met. Setting 3 minute timer (${state.on_date[target]})")
        }
    }
    if (lux_measurement > lux_target) {
        // Lights are on and lux measurement is > lux_target. Should we turn the lights off?
        if (state.off_date[target] != null) {
            def current_date = new Date().getTime() / 1000
            // Check to make sure at least 3 minutes have passed (to avoid fluke values from turning the lights on/off in succession)
            if ((current_date - state.off_date[target]) >= 180) {
                // Reset the off_date to NULL to prepare for the next event
                state.off_date[target] = null

                // Check the status of the switches in the array. If all switches are already OFF, exit the routine.
                def switches_off = true
                switch_list.any { object ->
                    if (object.currentSwitch == "on") {
                        switches_off = false
                        return true
                    }
                }
                if (switches_off) {
                    return
                }

                def df = new java.text.SimpleDateFormat("H")
                // Ensure the new date object is set to local time zone
                df.setTimeZone(location.timeZone)
                def hour = df.format(new Date())
                
                if (location.mode == "Keep Lights On") {
                    sendNotificationEvent("[LIGHTING] Keeping lights ON due to the Keep Lights On mode being set.")
                } else {
                    log.trace("Turning ${target} lights OFF!")
                    switch_list.each { object ->
                        object.off()
                    }

                    if (hour.toInteger() > 15) {
                        sendPush("${target.capitalize()} lights are turning back OFF.")
                    }
                }
            }
        } else {
            state.off_date[target] = new Date().getTime() / 1000
            log.trace ("Target lux value has been met. Setting 3 minute timer (${state.off_date[target]})")
        }
    }
}

// This event is run whenever interior lights are chosen for the scene. It will turn the interior switches off at the time specified (interior_time_off)
// Interior lights are NOT turned off if:
//		1. The switch is in the list of interior_exceptions.
// These switches are left on indefinitely and must be turned off manually.
def interiorLightsOffHandler(evt) {
    if (location.mode == "Keep Lights On") {
        sendNotificationEvent("[LIGHTING] Keeping lights ON due to the Keep Lights On mode being set.")
    } else {
        log.trace ("Time ${interior_time_off} has been reached. Turning the interior switches OFF.")
        if (interior_switches != null) {
            interior_switches.each { object ->
                def interior_exception = false
                if (interior_exceptions != null) {
                    interior_exceptions.each { exception ->
                        if (exception.getLabel() == object.getLabel()) {
                            interior_exception = true
                            return
                        }
                    }
                }
                if (!interior_exception) {
                    object.off()
                }
            }
        }
    }
}

// This event is run whenever interior lights are chosen for the scene. It will turn the interior switches on at the time specified (interior_time_on) if the current light is <= the value of interior_target
// Interior lights are NOT turned on if:
//		1. It's a weekend day before 8AM. These exceptions will turn on based on light availability after 8AM via evalIlluminanceAction().
//		2. The switch is in the list of interior_exceptions. These exceptions will turn on based on light availability after 12PM via evalIlluminanceAction().
def interiorLightsOnHandler(evt) {
    // Determine the current day of the week so that we won't turn the lights on in the morning on weekends
    def week_day_date = new java.text.SimpleDateFormat("u")
    // Ensure the new date object is set to local time zone
    week_day_date.setTimeZone(location.timeZone)
    def week_day = week_day_date.format(new Date())
    
	if (week_day.toInteger() < 6) {
        def lux_measurement = sensor.currentValue("illuminance").toInteger()
        def lux_target = interior_target.toInteger()

        if (lux_measurement < lux_target) {
            if (location.mode == "Keep Lights Off") {
                sendNotificationEvent("[LIGHTING] Keeping lights OFF due to the Keep Lights Off mode being set.")
            } else {
                log.trace ("Time ${interior_time_on} has been reached. Turning the interior switches ON.")
                if (interior_switches != null) {
                    interior_switches.each { object ->
                        def interior_exception = false
                        if (interior_exceptions != null) {
                            interior_exceptions.each { exception ->
                                if (exception.getLabel() == object.getLabel()) {
                                    interior_exception = true
                                    return
                                }
                            }
                        }
                        if (!interior_exception) {
                            object.on()
                        }
                    }
                }
            }
        }
    }
}