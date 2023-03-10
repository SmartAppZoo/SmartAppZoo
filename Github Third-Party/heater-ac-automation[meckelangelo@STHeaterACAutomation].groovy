definition(
    name: "Heater/AC Automation",
    namespace: "meckelangelo",
    author: "David Meck",
    description: "Automate a heater/AC unit that is plugged into a smart outlet, based on contact sensor and/or motion sensor.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool60x60.png",
    iconX2Url: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool120x120.png",
    iconX3Url: "https://raw.githubusercontent.com/meckelangelo/STHeaterACAutomation/master/HeatCool.png")


preferences {    
    section("Select the outlet(s)...") {
        input "outlet", "capability.switch", title: "Outlet", required: true, multiple: false
    }
    
    section("What is plugged into the outlet?") {
        input "outletMode", "enum", title:"Device", options:["Heater", "AC", "Disabled"], required: true, multiple: false, refreshAfterSelection:true
    }

    section("Choose a temperature sensor...") {
        input "temperatureSensor", "capability.temperatureMeasurement", title: "Sensor", required: true, multiple: false
    }
    
    section("(Optional) Turn the outlet on/off when this contact sensor is opened/closed...") {
        input "contact", "capability.contactSensor", title: "Sensor", required: false, multiple:false
    }
    
    section("Should the outlet turn on or off (or do nothing) when the contact sensor is opened? Choose 'Nothing' if no contact sensor selected.") {
        input "opened", "enum", title: "Opened", required: true, options: ["On", "Off", "Nothing"]
    }
    
    section("Should the outlet turn on or off (or do nothing) when the contact sensor is closed? Choose 'Nothing' if no contact sensor selected.") {
        input "closed", "enum", title: "Closed", required: true, options: ["On", "Off", "Nothing"]
    }
    
    section("Turn the outlet on when motion has been detected by this sensor (and the temperature exceeds the comfort temperature, entered below)...") {
        input "motionSensor", "capability.motionSensor", title: "Sensor", required: true, multiple: false
    }
    
    section("Turn the outlet off when there has been no motion for this number of minutes...") {
        input "minutes", "number", title: "Minutes", required: true
    }
    
    section("Set the COMFORT temperature (this temperature will be maintained when there is activity AND the home is in one of the modes in 'Set for specific mode(s)' below.)...") {
        input "setComfTemp", "number", title: "Degrees Fahrenheit", required: true
    }

    section("Set the VACANT temperature (this temperature will be maintained regardless of activity, BUT the home must be in one of the modes in 'Set for specific mode(s)' below.)...") {
        input "setVacTemp", "number", title: "Degrees Fahrenheit", required: true
    }
    
    section("Regardless of activity, maintain the COMFORT temperature in these modes... (WARNING: You must also select this mode in 'Set for specific mode(s)' - otherwise it will not function properly.)") {
        input "modes", "mode", title: "Mode", multiple: true, required: false
    }
    
    section("Name and modes... WARNING: It is strongly advised not to select 'Away' when choosing modes!"){}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    if (outletMode != "Disabled") {
        initialize()
    }
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    if (outletMode != "Disabled") {
        initialize()
    }
}

def initialize() {
	state.nextMotionCheck = 0
    subscribe(temperatureSensor, "temperature", temperatureHandler)
    if (contact != null && contact != "") {
        subscribe(contact, "contact", contactHandler)
    }
    subscribe(motionSensor, "motion.active", motionHandler)
    subscribe(motionSensor, "motion.inactive", motionStoppedHandler)
    subscribe(location, "mode", modeChangeHandler)
}

def turnOn() {
    def switchValue = outlet.latestValue("switch")

    if (switchValue == "on") {
        log.debug ("Switch is already turned on. No action taken.")
    } else {
        log.debug ("TURNING THE SWITCH ON")
        outlet.on()
    }
}

def turnOff() {
    def switchValue = outlet.latestValue("switch")

	if (switchValue == "off") {
    	log.debug ("Switch is already turned off. No action taken.")
    } else {
        log.debug ("TURNING THE SWITCH OFF")
    	outlet.off()
    }
}

def checkMotion(String event) {
    def motionState = motionSensor.currentState("motion")
    def elapsed = now() - motionState.date.time
    def elapsedMinutes = elapsed / 60 / 1000
    def threshold = minutes * 60 * 1000
    
    log.debug "Checking motion... Last motion occurred $elapsedMinutes minutes ago."
    
    if (motionState.contains("inactive") && elapsed >= threshold) {
    	log.debug ("Last motion occurred outside of the specified threshold ($minutes minutes).")
        checkContact(event)
    } else {
    	log.debug ("Last motion occurred within the specified threshold ($minutes minutes).")
        checkTemperature(true)
    }
}

def checkContact(String event) {
	def contactState = contact.currentState("contact")
    def elapsed = now() - contactState.date.time
    def elapsedMinutes = elapsed / 60 / 1000
    def threshold = minutes * 60 * 1000
    
    log.debug "Checking contact... Last contact activity occurred $elapsedMinutes minutes ago."
    
    if (event == "contact") {
    	if ((contact.latestValue("contact") == "open" && opened == "On") || (contact.latestValue("contact") == "closed" && closed == "On")) {
            log.debug ("Contact event occurred. Evaluate temperatures to determine if switch should be turned on.")
            checkTemperature(true)
        } else {
        	log.debug ("Contact event occurred. Evaluate temperatures to determine if switch should be turned off.")
        	checkTemperature(false)
        }
    } else if (((contact.latestValue("contact") == "open" && opened == "On") || (contact.latestValue("contact") == "closed" && closed == "On")) && elapsed <= threshold) {
    	log.debug ("Contact event occurred recently. Evaluate temperatures to determine if switch should be turned on.")
    	checkTemperature(true)
    } else {
    	log.debug ("Contact event did not occur recently. Evaluate temperatures to determine if switch should be turned off.")
    	checkTemperature(false)
    }
}

def checkTemperature(boolean boolActivity) {
	def currentTemp = temperatureSensor.latestValue("temperature")
    def boolTurnOn = false
    
	log.debug ("Checking temperature... Current temperature is $currentTemp degrees.")
    
    if (outletMode == "Heater") {
        if (boolActivity || modes.contains(location.mode)) {
        	log.debug ("Checking temperature... COMFORT temperature ($setComfTemp degrees) should be met at this time.")
            boolTurnOn = (currentTemp < setComfTemp)
        } else {
            log.debug ("Checking temperature... VACANT temperature ($setVacTemp degrees) should be met at this time.")
            boolTurnOn = (currentTemp < setVacTemp)
        }
    } else if (outletMode == "AC") {
        if (boolActivity || modes.contains(location.mode)) {
            log.debug ("Checking temperature... COMFORT temperature ($setComfTemp degrees) should be met at this time.")
            boolTurnOn = (currentTemp > setComfTemp)
        } else {
            log.debug ("Checking temperature... VACANT temperature ($setVacTemp degrees) should be met at this time.")
            boolTurnOn = (currentTemp > setVacTemp)
        }
    }
    
    if (boolTurnOn) {
        log.debug ("Checking temperature... It is not, switch must be turned on (if it is not already).")
        turnOn()
    } else {
    	log.debug ("Checking temperature... Switch must be turned off (if it is not already).")
        turnOff()
    }
}

def contactHandler(evt) {
	if (contact.latestValue("contact") == "open") {
		log.debug("EVENT: Contact opened...")
    } else if (contact.latestValue("contact") == "closed") {
    	log.debug("EVENT: Contact closed...")
    }
    if (["On", "Off"].contains(opened) || ["On", "Off"].contains(closed)) {
    	checkContact("contact")
    }
}

def modeChangeHandler(evt) {
    log.debug("EVENT: Mode changed...")
    if (modes.contains(location.mode)) {
        checkTemperature(false)
    } else {
        checkMotion("mode")
    }
}

def motionHandler(evt) {
    if (outlet.latestValue("switch") == "off") {
    	if (state.nextMotionCheck == 0 || now() > state.nextMotionCheck) {
            log.debug("EVENT: Motion started... Checking temperature.")
            state.nextMotionCheck = now() + (minutes * 60 * 1000)
            checkTemperature(true)
        } else {
        	log.debug("EVENT: Motion started... Checked temperature within past $minutes minutes, skipping this time.")
        }
    }
}

def motionStoppedHandler(evt) {
	unschedule()
    if (outlet.latestValue("switch") == "on") {
		log.debug("EVENT: Motion stopped...")
        runIn(minutes * 60, checkMotion, [event: "motionStopped"])
	}
}

def temperatureHandler(evt) {
	def currentTemp = temperatureSensor.latestValue("temperature")
    
    log.debug("EVENT: Temperature changed...")
    
    if ((outletMode == "Heater" && currentTemp >= setComfTemp) || (outletMode == "AC" && currentTemp <= setComfTemp)) {
    	log.debug("Checking temperature... Switch must be turned off (if it is not already).")
        turnOff()
    } else {
	    checkMotion("temperature")
    }
}