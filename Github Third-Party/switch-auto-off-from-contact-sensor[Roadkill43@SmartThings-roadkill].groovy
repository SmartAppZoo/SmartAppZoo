/**
 *  Auto Switch off - Turn a switch on and off based on a contact sensor being in an open state for a specific amount of time.
 *
 */

// Automatically generated. Make future change h ere.
definition(
    name: "Switch Auto Off from contact sensor",
    namespace: "roadkill",
    author: "mhall@martyrhall.com",
    description: "Automatically turn off a switch when a windows/doors open. Turn it back on when everything is closed up.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
	section("Switches to control") {
                input "switches", "capability.switch", multiple: true
	}
    
    section("Open/Close") {
    	input("sensors", "capability.contactSensor", title: "Sensors", multiple: true)
        input("delay", "number", title: "Delay (minutes) before turning switch(s) off", required : false)
        input("delayRestore", "number", title: "Delay (minutes) before restoring switch(s) when closed", required : false)
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
          turnOff()  
       } else {
          state.scheduled = true
          runIn(delay * 60, turnOff)
       }
       
    } else if(evt.value == 'closed' && (state.changed || state.scheduled)) {        
        if(!isOpen()) {
          log.debug "Everything is closed, restoring the switch ($delayRestore minutes)"
          log.debug "state: $state"
          if(state.scheduled)  {
              state.scheduled = false
              unschedule("turnOff")
           } else if (delayRestore < 1) {
              restore()
           } else {
              state.scheduledRestore = true
	   		  runIn(delayRestore * 60, restore)
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
    switches?.on()
    state.changed = false
    state.scheduledRestore = false
    } else {
        log.debug "A door must have been reopened"
    }
}