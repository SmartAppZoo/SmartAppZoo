/**
 *  Accelerometer Light
 *
 *  Author: Chuck Norris, modified by Slonob
 *
 */
definition(
    name: "XLR8 Light Plus",
    namespace: "slonob",
    author: "The Norris, Jarrod Stenberg",
    description: "Turns on lights when a knock is detected. Can delay turning on multiple lights to simulate someone being home. Turns lights off when it becomes light or some time after motion ceases. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
    section("First light(s):"){
        input "lights1", "capability.switch", multiple: true
    }
    section("First light delay to simulate occupancy (recommend 5-10):"){
        input "delayStartSec1", "number", title: "Seconds:"
    }
    section("Blink first light when already on:"){
        input "lights1Blink", "bool", required: true
    }
    section("Second light(s):"){
        input "lights2", "capability.switch", multiple: true, required: false
    }
    section("Second light delay to simulate occupancy (recommend 8-15):"){
        input "delayStartSec2", "number", title: "Seconds:", required: false
    }
    section("Blink second light when already on:"){
        input "lights2Blink", "bool", required: false
    }
    section("Triggering sensor:"){
        input "accel", "capability.accelerationSensor", title: "Which acceleration sensor?", required: false
        input "motio", "capability.motionSensor", title: "Which motion sensor?", required: false
    }
    section("Turn lights off when there's been no movement after:"){
        input "delayStopSec", "number", title: "Seconds:"
    }
    
    
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
	if (accel) {
		subscribe(accel, "acceleration", accelHandler)
    }
    if (motio) {
	    subscribe(motio, "motion", accelHandler)
	}
    state.switchWasOn = [1,2]
}

def accelHandler(evt) {
    log.debug "accelHandler event: $evt.name: $evt.value descriptionText:$evt.descriptionText"
    if (evt.value == "active") {
        log.debug "light1 original state is $lights1.currentSwitch"
		if (lights2) {
			log.debug "light2 original state is $lights2.currentSwitch"
        }
		log.debug "turning on lights due to motion with $delayStartSec1, $delayStartSec2 seconds delay"

		runIn(delayStartSec1, turnOnLights1, [overwrite: false])            
		if (lights2) {
	        if (delayStartSec2) {
            	runIn(delayStartSec2, turnOnLights2, [overwrite: false])            
			} else {
            	runIn(delayStartSec1 + 5, turnOnLights2, [overwrite: false])            
            }
		}
		state.motionStopTime = null
    }
    else {
        state.motionStopTime = now()
        if(delayStopsec) {
            runIn(delayStopSec, turnOffMotionAfterDelay, [overwrite: false])
        } else {
            runIn(59, turnOffMotionAfterDelay, [overwrite: false])
        }
    }
}

def turnOnLights1() {
    // either switch is "on" or "off"
    for (light in lights1) {
    	state.switchWasOn[light.getId()] = light.currentSwitch.contains("on");
    	log.debug "light.currentSwitch = $light.currentSwitch -- state.switchWasOn[light.getId()]= " + state.switchWasOn[light.getId()]
		if (lights1 && state.switchWasOn[light.getId()]) { 
    		log.debug "light was already on."
			if (lights1Blink) {
	    		log.debug "Blinking lights1."
		    	light.off()
            	light.on()
        	}
		} else {
    		log.debug "lights1 were off. Turning on."
	    	light.on()
		}
    }
	state.lastStatus = "on"
}

def turnOnLights2() {
    // either switch is "on" or "off"
    for (light in lights2) {
    	state.switchWasOn[light.getId()] = light.currentSwitch.contains("on");
    	log.debug "light.currentSwitch = $light.currentSwitch -- state.switchWasOn[light.getId()]= " + state.switchWasOn[light.getId()]
		if (lights1 && state.switchWasOn[light.getId()]) { 
    		log.debug "light was already on."
			if (lights2Blink) {
	    		log.debug "Blinking lights2."
		    	light.off()
            	light.on()
        	}
		} else {
    		log.debug "lights2 were off. Turning on."
	    	light.on()
		}
    }
}

def turnOffMotionAfterDelay() {
    log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    if (state.motionStopTime && state.lastStatus != "off") {
 		for (light in light1) {
			if (state.switchWasOn[light.getId()] == false) {
	    		log.debug "Turning off light."
				light.off()
			}
        }
		if (lights2) {
			for (light in light2) {
				if (state.switchWasOn[light.getId()] == false) {
                	log.debug "Turning off light."
	            	light.off()
				}
            }
		}
		state.lastStatus = "off"
        state.motionStopTime = null
	}
}