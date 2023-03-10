/**
 *  Auto Switch Control -Turn select switches on depending on the amount of time a contact sensor has been opened. Use case is for controlling thermostat temperature with virtual switches, IFTTT and a Honeywell Wifi Thermostat.
 *
 */

// Automatically generated. Make future change  here.
definition(
    name: "Switch Automation from contact sensor",
    namespace: "roadkill",
    author: "mhall@martyrhall.com",
    description: "Automatically on switch(s) when a windows/doors open and different one when it closes.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("Switches to control") {
                input("CloseSwitches", "capability.switch", multiple: true, Title: "Switches to turn on when closed")
                input("OpenSwitches", "capability.switch", multiple: true, Title: "Switches to turn on when opened")
	}
    
    section("Open/Close") {
    	input("sensors", "capability.contactSensor", title: "Sensors", multiple: true)
        input("delay", "number", title: "Delay (minutes) before turning on 'off' switch(s) when Opens", required : false)
        input("delayRestore", "number", title: "Delay (minutes) before turning on 'on' switch(s) when closed", required : false)
        input("toggle", "bool", title: "Toggle Swiches on State Change?", required: false)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	state.changed = false
    subscribe(sensors, 'contact', "sensorChange")
}

def sensorChange(evt) {
	log.debug "Desc: $evt.value , $state"
    if(evt.value == 'open' && !state.changed) {
    	log.debug "Scheduling turn off ($delay minutes)"
        if(state.scheduledRestore) {
             state.scheduledRestore = false
             unschedule("restore")
       }
       if (delay < 1) {
    //      turnOff()
    	// Turn On the 'Open Swiches'
          turnOnOpenSwiches()
       } else {
          state.scheduled = true
      //    runIn(delay * 60, turnOff)
          runIn(delay * 60, turnOnOpenSwiches)
       }
       
    } else if(evt.value == 'closed' && (state.changed || state.scheduled)) {        
        if(!isOpen()) {
          log.debug "Everything is closed, Turn on the Closed switchs ($delayRestore minutes)"
          log.debug "state: $state"
          if(state.scheduled)  {
              state.scheduled = false
              unschedule("turnOnOpenSwiches")
           } else if (delayRestore < 1) {
              restore()
           } else {
              state.scheduledRestore = true
	   		  runIn(delayRestore * 60, restore("ClosedSwitches"))
           }
        } else {
        	log.debug "Something is still open."
        }
    }
}

def isOpen() {
	def result = sensors.find() { it.currentValue('contact') == 'open'; }
    log.debug "isOpen results: $result"
    return result
}

// turn off thermostat based on a contact being opened
def turnOff() {
	log.debug "Preparing to turn off the switch due to contact open"
    if(isOpen()) {
    	log.debug "It's safe. Turning it off."
        switches?.off()
        state.changed = true
        state.scheduled = false
    	log.debug "State: $state"
    } else {
    	log.debug "Just kidding. The platform did something bad."
    }
}

// restore to thermostatMode prior to contact being opened 
def restore() {
 if(!isOpen()) {
    log.debug "Switching the switches to on"
    CloseSwitches?.on()
    state.changed = false
    state.scheduledRestore = false
    if(toggle){
         	OpenSwitches?.off()
         }        
    } else {
        log.debug "A door must have been reopened"
    }
}

def turnOnOpenSwiches() {
 if(isOpen()) {
    	log.debug "Turning on the 'Open Swiches'."
        OpenSwitches?.on()
        state.changed = true
        state.scheduled = false
    	log.debug "State: $state"
        if(toggle) {
         	CloseSwitches?.off()
         }
        
    } else {
    	log.debug "Just kidding. The platform did something bad."
    }
}