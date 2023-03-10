/**

    Energy Saver
    *
    Based on the Energy Saver produced by Samsung, where the main purpose is to turn off plugs where
    appliances are wasting unnecessary energy (e.g devices in standby). 
    *
    */
    definition(
    name: "Energy Monitor",
    namespace: "bottlecap",
    author: "David Kendall",
    description: "Turn things off when the consumption is below a certain threshold",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
    )

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
		input(name: "threshold", type: "number", title: "Reports below...", required: true, description: "in watts.")
		input(name: "minutespassed", type: "number", title: "After time has passed...", required: true, description: "minutes")
	}
	section {
		input(name: "switches", type: "capability.switch", title: "Turn Off These Switches", required: true, multiple: true, description: null)
	}
    section("Turn off between... ") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
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

def initialize() {
	state.lastvalue = Integer.MAX_VALUE
    state.isSwitchOffScheduled = false

	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
	// Check if the current time is within the applicable hours window (if provided)
    def isBetween = true
    log.debug "FromTime: ${fromTime}; ToTime: ${toTime}"
    if (fromTime != null && toTime != null) {
    	log.debug "Checking time of day"
    	isBetween = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    }
    
    log.debug "Inbetween: ${isBetween}"
    
    if (isBetween) {
        def meterValue = evt.value as double
        def thresholdValue = threshold as int
        log.debug "${meter} reported energy consumption of ${meterValue}. Last value is ${state.lastvalue}"

        if (meterValue <= thresholdValue) {
            if (state.isSwitchOffScheduled == false) {
                state.isSwitchOffScheduled = true
                log.debug "${meter} reported energy consumption below ${threshold}. Scheduling to turn off switches."
                runIn(minutespassed * 60, switchOff)
            }
        }
        else {
            state.isSwitchOffScheduled = false
        }

        state.lastvalue = meterValue
    }
}

def switchOff() {
	def thresholdValue = threshold as int
	if (state.lastvalue <= thresholdValue) {

        // Check if the current time is within the applicable hours window (if provided)
        def isBetween = true
        log.debug "FromTime: ${fromTime}; ToTime: ${toTime}"
        if (fromTime != null && toTime != null) {
            log.debug "Checking time of day"
            isBetween = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        }

        if (isBetween) {
    	    log.debug "${meter} reported energy consumption below ${threshold} after ${minutespassed}. Turning off switches."
		    switches.off()
        }
	}
    
    state.isSwitchOffScheduled = false
}