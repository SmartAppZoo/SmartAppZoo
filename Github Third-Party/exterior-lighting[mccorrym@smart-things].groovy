definition(
    name: "Exterior Lighting",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitor lighting to determine when to turn on or off exterior lighting.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
	section("Choose the light sensor and switches you'd like to control.") {
        input "sensor", "capability.illuminanceMeasurement", required: true, title: "Which light sensor to monitor?"
        input "exterior_switches", "capability.switch", required: true, multiple: true, title: "Which exterior switch(es) to control?"
        input "exterior_target", "enum", required: true, title: "Which lumen value to target for exterior switches?", options: [200, 400, 600, 800]
        input "interior_switches", "capability.switch", required: false, multiple: true, title: "Which interior switch(es) to control? (optional)"
        input "interior_target", "enum", required: true, title: "Which lumen value to target for interior switches?", options: [200, 400, 600, 800]
        input "interior_time", "time", required: false, title: "Choose a time to turn the interior lights off"
	}
}

def installed() {
    initIlluminationState()
    subscribe(sensor, "illuminance", illuminanceChangeHandler)
    subscribe(switches, "switch", switchChangeHandler)
    unschedule(interiorLightsHandler)
    if (interior_switches != null) {
    	schedule(interior_time, interiorLightsHandler)
    }
}

def updated() {
    unsubscribe()
    initIlluminationState()
    subscribe(sensor, "illuminance", illuminanceChangeHandler)
    subscribe(switches, "switch", switchChangeHandler)
    unschedule(interiorLightsHandler)
    if (interior_switches != null) {
    	schedule(interior_time, interiorLightsHandler)
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

    evalIlluminanceAction(lux_measurement, "exterior");
    
    if (interior_switches != null) {
    	evalIlluminanceAction(lux_measurement, "interior");
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

    def valid_hour = timeOfDayIsBetween(timeToday("10:00", location.timeZone), interior_time, new Date(), location.timeZone)

    if (target == "exterior" || (target == "interior" && valid_hour)) {
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
            // Lights are off and lux measurement is < exterior_target. Should we turn the lights on?
            if (state.on_date[target] != null) {
                def current_date = new Date().getTime() / 1000
                // Check to make sure at least 3 minutes have passed (to avoid fluke values from turning the lights on/off in succession)
                if ((current_date - state.on_date[target]) >= 30) {
                    // Reset the exterior_on_date to NULL to prepare for the next event
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

                    def df = new java.text.SimpleDateFormat("H")
                    // Ensure the new date object is set to local time zone
                    df.setTimeZone(location.timeZone)
                    def hour = df.format(new Date())

                    log.trace("Turning ${target} lights ON!")
                    switch_list.each { object ->
                        object.on()
                    }

                    if (hour.toInteger() > 15) {
                        sendPush("Good evening! ${target.capitalize()} lights are turning ON.")
                    } else {
                        sendPush("${target.capitalize()} lights are turning ON due to darkness.")
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
                if ((current_date - state.off_date[target]) >= 30) {
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

                    log.trace("Turning ${target} lights OFF!")
                    switch_list.each { object ->
                        object.off()
                    }

                    if (hour.toInteger() > 15) {
                        sendPush("${target.capitalize()} lights are turning back OFF.")
                    } else {
                        sendPush("Good morning! ${target.capitalize} lights are turning OFF.")
                    }
                }
            } else {
                state.off_date[target] = new Date().getTime() / 1000
                log.trace ("Target lux value has been met. Setting 3 minute timer (${state.off_date[target]})")
            }
        }
    }
}

// This event is run whenever interior lights are chosen for the scene. It will turn the interior switches off at the time specified (interior_time)
def interiorLightsHandler (evt) {
    log.trace ("Time ${interior_time} has been reached. Turning the interior switches OFF.")
	if (interior_switches != null) {
        interior_switches.each { object ->
            log.debug(object.currentSwitch)
            object.off()
        }
    }
}