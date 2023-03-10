/**
 *  Author: Baur
 */

 

definition(
    name: "Contact Sensor Activated Lighting",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Turn your lights on to set level when contact sensor is opened and then off again once the contact closes after a set period of time.",
    category: "Convenience",
    parent: "baurandr:Baur Lighting",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Turn on when this contact opens..."){
		input "contact1", "capability.contactSensor", title: "Which contact sensor?"
	}
	section("And off when the contact has been closed for..."){
		input "seconds1", "number", title: "Seconds?"
	}
	section("Turn on/off light(s)..."){
		input "switches", "capability.switchLevel", multiple: true
	}
	section("Set dim level..."){
		input "dimLevel", "number", title: "%?"
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
}

def installed() {
	initDimmers()
	subscribe(contact1, "contact", contactHandler)
}

def updated() {
	unsubscribe()
    initDimmers()
	subscribe(contact1, "contact", contactHandler)
}

def contactHandler(evt) {
	log.debug "$evt.name: $evt.value"

	def offSwitches = state.switchesToTurnOff
    if (evt.value == "open"){ //Motion has started
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
        def curMode = location.currentMode
        def modeOK = !modesTurnOnAllowed || modesTurnOnAllowed.contains(curMode)

        log.debug "Current Mode: ${curMode}, Turn on Mode OK: ${modeOK}"
        log.debug "Turn On time frame OK: ${timeOK}"  

        if (timeOK & modeOK) {
            checkAndSetDimmers()
        }
	} else if (evt.value == "closed") {
		if (offSwitches) {
            if(seconds1 <= 0) {
                turnOffDimmers() 
                log.debug "Contact has closed and desired hold time is zero, turning lights off"
            } else {
                runIn(seconds1, scheduleCheck, [overwrite: false])
            }
		}
	}
}

def scheduleCheck() {
	log.debug "schedule check"
	def contactState = contact1.currentState("contact")
    if (contactState.value == "closed") {
        def elapsed = now() - contactState.rawDateCreated.time
    	def threshold = 1000 * seconds1 - 1000
    	if (elapsed >= threshold) {
            log.debug "Contact has stayed closed long enough since last check ($elapsed ms):  turning lights off"
            turnOffDimmers()
    	} else {
        	log.debug "Contact has not stayed closed long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Contact is open, do nothing and wait for closed"
    }
}

def checkAndSetDimmers() {
	def offSwitches = state.switchesToTurnOff
	def newOffSwitches = switches.findAll{it.currentValue("switch") == "off"}
    log.debug "Switches to be turned on by this app: ${newOffSwitches}"
	if(newOffSwitches){
    	newOffSwitches.each {it.setLevel(dimLevel)}
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
    }
    state.switchesToTurnOff = []
}

def initDimmers() {
	state.switchesToTurnOff = []
    log.debug "Initializing" 
}