/**
 *
		update 2015-02-06 mike maxwell
        	added selectable lux settings to UX
 *
 */
definition(
    name: "Bath Auto Lights",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "bla bla bla",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true
	}
	section("Turn on when it's dark and there's movement..."){
		input "motionDevice", "capability.motionSensor", title: "Where?", required: true
	}
	section("Using this lux sensor"){
		input "luxDevice", "capability.illuminanceMeasurement", required: true
	}
    section("Lux dark threshold for light sensor, defaults to 10") {
    	input "luxCutoff","enum", title: "LUX?", required: false, options: ["5","10","50","100","500","1000","2000","4000"]
    }
	section("And off when there's been no movement for..."){
		input "delayMinutesMotion", "enum", title: "Minutes?", options:["1","5","10","15","20","30"]
	}
	section("Unless humidity is detected... "){
		input "activeHumidityDevices", "capability.relativeHumidityMeasurement", title: "Where?", multiple: true
	}
	section("Using this humidity sensor as the base line"){
		input "baseHumidityDevice", "capability.relativeHumidityMeasurement", title: "Which?"
	}
	section("Humidity setpoint (above base line)"){
		input "humiditySetPoint", "enum", title: "Percent?" , options:["5","10","15","20","25","30"]
	}
	section("When humidity is detected, extend on time by... "){
		input "delayMinutesHumidity", "enum", title: "Minutes?" , options:["5","10","15","20","25","30"]
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
	subscribe(motionDevice, "motion", motionHandler)
	//subscribe(activeHumidityDevices, "humidity", humidityHandler)
    subscribe(app, appTouch)
}
def appTouch(evt) {
	log.debug "humitity:${humidityEnabled()} lux:${luxEnabled()}"
}

def motionHandler(evt) {
	//log.debug "humitity:${humidityEnabled()} lux:${luxEnabled()}"
    
    
    log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		if (luxEnabled()) {
			log.debug "motionHandler- turning on lights due to motion"
			lights.on()
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {
    	log.debug "motionHandler- motion stopped"
		state.motionStopTime = now()
        runIn(delayMinutesMotion.toInteger() * 60, nowWhat)
	}
    
}


def nowWhat() {

	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = ((now() - state.motionStopTime) / 1000).toInteger() 
        
        log.trace "nowWhat- elapsed:${elapsed} timeout:${delayMinutesMotion.toInteger() * 60}"
        // elapsed:45929 timeout:60 
		
        if ( elapsed >= (delayMinutesMotion.toInteger() * 60)) {
        	if (humidityEnabled) {
            	//state.humidActivated = now()
                log.debug "nowWhat- Humidity override enabled"
            	//extend off time
                runIn(delayMinutesHumidity.toInteger() * 60, nowWhat)
            } else {
        		log.debug "nowWhat- Turning lights off"
                //reset humid flag??
				lights.off()
				state.lastStatus = "off"
            }
		}
	}
}

private luxEnabled() {
	
    def result = luxDevice.currentIlluminance < luxCutoff.toInteger()
    log.debug "luxEnabled- ${result} crntLux:${luxDevice.currentIlluminance}"
	return result
}

private humidityEnabled() {
	def result
	if (state.humidActive){
        result = false
    } else {
    	def maxCrnt = activeHumidityDevices.currentValue("humidity").max()
    	if (maxCrnt > baseHumidityDevice.currentHumidity + humiditySetPoint.toInteger()) {
      		result = true      
        } else {
            result = false
        }
    }
    state.humidActive = result
 	//def maxCrnt = activeHumidityDevices.currentValue("humidity").max()
    //log.debug "crntHumidities:${activeHumidityDevices.currentValue("humidity")}"
 	log.debug "humidityEnabled- ${result} bathMax:${maxCrnt} base:${baseHumidityDevice.currentHumidity}"
	
    
	//return maxCrnt > baseHumidityDevice.currentHumidity + humiditySetPoint.toInteger()
    
    return result
}


