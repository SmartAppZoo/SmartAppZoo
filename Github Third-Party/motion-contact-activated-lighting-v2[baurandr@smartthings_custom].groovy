/**
 *  Author: Baur
 */
 // TODO
 // Add 'disable timer' for motion, i.e. deactivate turn back on due to motion to be used for 'stealth mode' etc
 // Change minutes to seconds?
 // Add 'blackout' times for motion detection?
 // Add requirements for other lights to be off?
 // DO not turn light on if light was turned off, or double pressed, etc...
 // debounce for motion when shutting off
 // multiple OR'd motion sensors
 // multiple AND'd motion sensors
 

definition(
    name: "Motion & Contact Activated Lighting v2",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Turn your lights on to set level when motion is detected and then off again once the motion stops for a set period of time.",
    category: "Convenience",
    parent: "baurandr:Baur Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Turn on when there's activity..."){
		input "motion1", "capability.motionSensor", title: "Which motion sensor(s)?", multiple: true, required: false
        input "contact1", "capability.contactSensor", title: "Which contact sensor(s)?", multiple: true, required: false
	}
	section("Settings When Mode is NOT Sleeping"){
	    input "switches", "capability.switchLevel", title: "Which Dimmers?", multiple: true
		input "dimLevel", "number", title: "Set Dim Level To What %?"
		input "minutes1", "number", title: "Turn off how long after activity stops (minutes)?", range: "0..15"
	}
	section("Settings When Mode IS Sleeping (optional - leave blank to use same settings as NOT sleeping"){
        input "switchesSleep", "capability.switchLevel", title: "Which Dimmers?", multiple: true, required: false
		input "dimLevelSleep", "number", title: "Set Dim Level To What %?", required: false
		input "minutes1SleepSetting", "number", title: "Turn off how long after activity stops (minutes)?", range: "0..15", required: false
	}    

    section("Turn on between what times? Both absolute and sun event based times must be true to turn lights on.") {
        input "fromTime", "time", title: "Start of allowed time window", required: false
        input "toTime", "time", title: "End of allowed time window", required: false
        input "onOffset", "number", title: "Start of allowed time based on Sunset offset (+ = after, - = before)", required: false
        input "offOffset", "number", title: "End of allowed time based on Sunrise offset (+ = after, - = before)", required: false

	}
    section("Turn on during what modes?") {
    	input "modesTurnOnAllowed", "mode", title: "select a mode(s)", multiple: true, required: false
    }
    section("Turn on allowed during what Sun States?") {
		input(name: "sunStateTurnOnAllowed", type: "enum", title: "Allowed Sun States", options: ["Dawn","Day","Dusk","Night"], multiple: true, required: false)
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
    log.debug "Initializing"
	state.switchesToTurnOff = []
    subscribe(motion1, "motion", eventHandler)
    subscribe(contact1, "contact", eventHandler)
    runEvery5Minutes(supervisorCheck)
}

def eventHandler(evt) {
	log.debug "New Event: $evt.device,  $evt.name: $evt.value"

	def offSwitches = state.switchesToTurnOff
    def curMode = location.currentMode
    if (evt.value == "active" || evt.value == "open"){ //Motion has started or contact open
        //check time
		def timeOK = true
        if(fromTime && toTime){ 
            timeOK = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
		}
        if(onOffset && offOffset && timeOK){ 
        	def sunTimes = getSunriseAndSunset()
            def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunTimes.sunset)
            def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunTimes.sunrise)
            
            //calculate the offset
    		def timeAfterSunset = new Date(sunsetTime.time + (onOffset * 60 * 1000))
    		def timeAfterSunrise = new Date(sunriseTime.time + (offOffset * 60 * 1000))
            
            timeOK = timeOfDayIsBetween(timeAfterSunset, timeAfterSunrise, new Date(), location.timeZone)
		}
        
		//check mode
        def modeOK = !modesTurnOnAllowed || modesTurnOnAllowed.contains(curMode)
        
        //check sunState
        def sunStateOK = !sunStateTurnOnAllowed || sunStateTurnOnAllowed.contains(parent.sunStateDevice.currentValue("sunState"))

        log.debug "Current Mode: ${curMode}, Turn on Mode OK: ${modeOK}, Turn On time frame OK: ${timeOK}, Turn On Sun State OK: ${sunStateOK}"

        if (timeOK & modeOK & sunStateOK) {
        	int dimLevelFinal = dimLevel
            def switchesFinal = switches
        	if (curMode == "Sleeping"){
            	if(dimLevelSleep){
            		dimLevelFinal = dimLevelSleep
                }
            	if(switchesSleep){
            		switchesFinal = switchesSleep 
                }
            }
            checkAndSetDimmers(switchesFinal,dimLevelFinal)
        }
	} else if (evt.value == "inactive" || evt.value == "closed") {
    	def allQuiet = allSensorsQuiet()
        log.debug "All Quiet = ${allQuiet}"
		if (offSwitches && allQuiet) {
        	int minutes1Final = minutes1
        	if (curMode == "Sleeping"){
                try { //this is because for some reason a zero is similar to NULL
                    int minutes1SleepSettingInt = minutes1SleepSetting
                    minutes1Final = minutes1SleepSetting
                }
                catch(e){}
            }

            if(minutes1Final <= 0) {
                turnOffDimmers() 
                log.debug "Activity has stopped and desired hold time is zero, turning lights off"
            } else {
                runIn(minutes1Final * 60, scheduleCheck, [overwrite: true])
            }
		}
	}
}

def allSensorsQuiet(){
	def openContacts = contact1.any{it.currentValue("contact") == "open"}
	def activeMotion = motion1.any{it.currentValue("motion") == "active"}
/*    
    def bQuiet = true
    for (tItem in contact1){
        bQuiet = bQuiet && tItem.currentValue("contact") == "closed"
    }
    for (tItem in motion1){
        bQuiet = bQuiet && tItem.currentValue("motion") == "inactive"
    }
    log.debug "bQuiet = ${bQuiet}"
*/    
    if (!openContacts && !activeMotion) {
	    //log.debug "No active sensors"
    	return true
    } else {
    	//log.debug "Active sensors"
    	return false
    }
}

def scheduleCheck() {
	log.debug "Schedule Check"
    def allQuiet = allSensorsQuiet()
    log.debug "All Quiet Schedule Check = ${allQuiet}"
    if (allQuiet){
        log.debug "Activity has stayed inactive long enough.  Turning lights off"
        turnOffDimmers()
    } else {
    	log.debug "Activity, do nothing and wait for inactive"
    }
}

def checkAndSetDimmers(switchesFinal, dimLevelFinal) {
	def offSwitches = state.switchesToTurnOff
	def newOffSwitches = switchesFinal.findAll{it.currentValue("switch") == "off"}
    log.debug "Switches to be turned on by this app: ${newOffSwitches}"
	if(newOffSwitches){
    	newOffSwitches.each {it.setLevel(dimLevelFinal)}
        //add the switches that will be turned on to the list to turn off when motion stops
        newOffSwitches.each {offSwitches << it.displayName}	
        state.switchesToTurnOff = offSwitches
	}
}

def turnOffDimmers() {
    def offSwitches = state.switchesToTurnOff
    log.debug "Switches to be turned off by this app: ${offSwitches}"
    if(offSwitches){
    	switches.each {
            if(offSwitches.contains(it.displayName)){
            	it.setLevel(0)
			}
        }
    	switchesSleep.each {
            if(offSwitches.contains(it.displayName)){
            	it.setLevel(0)
			}
        }
    }
    state.switchesToTurnOff = []
}

def supervisorCheck(){
    log.debug "Supervisor Check"
    def offSwitches = state.switchesToTurnOff
    def lastStates = []
    if(offSwitches){
        motion1.each {
            lastStates << it.latestState("motion")
        }
        contact1.each {
            lastStates << it.latestState("contact")
        }
        def elapsed = 0
        def minElapsed = 600000000
        def bAllQuiet = true
        lastStates.each {
            if (bAllQuiet) {
                if(it.value == "closed" || it.value == "inactive"){
                    elapsed = now() - it.rawDateCreated.time
                    if (elapsed < minElapsed) {
                        minElapsed = elapsed
                    }
                } else {
                    log.debug "ACTIVE SENSOR FOUND"
                    bAllQuiet = false
                    minElapsed = 0
                }
            }
            log.debug "${it.date}-${it.name}-${it.value}-${it.device}-${elapsed}"
        }
        if (bAllQuiet) {
            int minutes1Final = minutes1
            if (curMode == "Sleeping"){
                try { //this is because for some reason a zero is similar to NULL
                    int minutes1SleepSettingInt = minutes1SleepSetting
                    minutes1Final = minutes1SleepSetting
                }
                catch(e){}
            }
            if (minElapsed > minutes1Final * 60 * 1000) {
                log.debug "Timer has elapsed, lights should have turned off - Turn lights off"
                turnOffDimmers()
            } else {
                log.debug "Timer has NOT elapsed, lights should still be on - DO Nothing"
            }
        } else {
            log.debug "NOT all quiet - do nothing"
        }
    } else {
        log.debug "No switches to be turned off - do nothing"
    }
     
    	//def elapsed = now() - motionState.rawDateCreated.time
        /*
    	def threshold = 1000 * 60 * minutes1 - 1000
        def recentStatesMotion = motion1.statesBetween(now(), now()-threshold)
        def recentStatesContact = contact1.statesBetween(now(), now()-threshold)
        
        statesBetween("contact", previousLog, currentLog, [max: 50])
       	statesSince(new Date(now() - 4000)
        def theStates = theswitch.statesSince("switch", new Date() -3)
        def recentStatesContact = contact1.statesSince("contact", new Date(now() - 4000))
        
        log.debug "Recent Motion: ${recentStatesMotion}"
        log.debug "Recent Contact: ${recentStatesContact}"
		if (!recentStatesMotion && !recentStatesContact) {
       
        def threshold = 1000 * 60 * minutes1 - 1000
        def recentStatesContact = contact1.statesSince("contact", new Date(now() - threshold)).findAll{it.value == "open"}
        def recentStatesMotion = motion1.statesSince("motion", new Date(now() - threshold)).findAll{it.value == "active"}
        log.debug "Recent Motion: ${recentStatesMotion}"
        log.debug "Recent Contact: ${recentStatesContact}"
        
        def latestMotionState = motion1.latestState("motion")
		log.debug "latest state value: ${latestDeviceState.value}"
         */
}