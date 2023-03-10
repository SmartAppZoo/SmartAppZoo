/**
 *  Smart Indicator Nightlight
 *
 */
 
definition(
    name: "Smart Indicator Nightlight",
    namespace: "KristopherKubicki",
    author: "kristopher@acm.org",
    description: "Turns on indicator lights when motion is detected. Turns indicator lights off when it becomes light or some time after motion ceases.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these indicator lights..."){
		input "lights", "capability.indicator", multiple: true
	}
	section("Turning on when there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()
	subscribe(motionSensor, "motion", motionHandler)
}


def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
    if(evt.value == "inactive") {
    	return noMotionHandler(evt)
    }
    state.lastStatus = "on"
    
    for(light in lights) { 
    	if(light.currentValue("switch") == "on") { 
        	light.indicatorWhenOn()
        }
    	else {
        	light.indicatorWhenOff()
        }
    }
    runIn(5,"blinkOff", [overwrite: true])
}

def blinkOff() {
	 for(light in lights) { 
    	if(light.currentValue("switch") == "on") { 
			light.indicatorNever()
        }
     }
}

def noMotionHandler(evt) {
    state.lastStatus = "off"
    runIn(30,"goDark", [overwrite: true])
}    
    
def goDark() { 
	if(state.lastStatus == "on") {
    	return
    }
	lights?.indicatorNever()
}
