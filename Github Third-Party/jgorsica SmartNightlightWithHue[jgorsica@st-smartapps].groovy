/**
 *  Smart Nightlight
 *
 *  Author: John Gorsica
 *
 */
definition(
    name: "Smart Nightlight with Hue",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Turns on lights when it's dark and motion is detected. Turns lights off when it becomes light or some time after motion ceases.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png"
)

preferences {
	section("Control these lights..."){
		input "lights", "capability.switch", multiple: true, required: false
	}
    section("And control these color bulbs...") {
		input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
	}
    section("Choose light effects...")
		{
			input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: ["Red","Green","Blue","Yellow","Orange","Purple","Pink"]
			input "lightLevel", "enum", title: "Light Level?", required: true, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
		}
	section("Turning on when it's dark and there's movement..."){
		input "motionSensor", "capability.motionSensor", title: "Where?"
	}
	section("And then off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using either on this light sensor (optional) or the local sunrise and sunset"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
		input "zipCode", "text", title: "Zip code", required: false
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
	subscribe(motionSensor, "motion", motionHandler)
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
	else {
		subscribe(location, "position", locationPositionChange)
		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		astroCheck()
	}
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active") {
		if (enabled()) {// && state.lastStatus!="on") {
			log.debug "turning on lights due to motion"
            if(lights){
				lights.on()
            }
            if(hues){
                //hues*.on()
                def hueColor = 0
                if(color == "Blue")
                    hueColor = 70//60
                else if(color == "Green")
                    hueColor = 39//30
                else if(color == "Yellow")
                    hueColor = 13//16
                else if(color == "Orange")
                    hueColor = 10
                else if(color == "Purple")
                    hueColor = 75
                else if(color == "Pink")
                    hueColor = 83
                def newValue = [hue: hueColor, saturation: 60, level: lightLevel as Integer ?: 100]
				log.debug "new value = $newValue"
                hues*.setColor(newValue)
             }   
			state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {
		state.motionStopTime = now()
		if(delayMinutes) {
			runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
		} else {
			turnOffMotionAfterDelay
		}
	}
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
	if (lastStatus != "off" && evt.integerValue > 50) {
    	if(hues){
        	hues.each {
                if(it.currentValue("level")==lightLevel as Integer && it.currentValue("hue")==hueColor){
                	it.off()
                }
            }
        }
        if(lights){
        	lights.off()
        }
		state.lastStatus = "off"
	}
	else if (state.motionStopTime) {
		if (lastStatus != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
				if(hues){
                    hues.each {
                        if(it.currentValue("level")==lightLevel as Integer && it.currentValue("hue")==hueColor){
                            it.off()
                        }
                    }
                }
                if(lights){
                    lights.off()
                }
				state.lastStatus = "off"
			}
		}
	}
	else if (lastStatus != "on" && evt.value < 30){
		if(lights){
            lights.on()
        }
        if(hues){
            //hues*.on()
            def hueColor = 0
            if(color == "Blue")
            hueColor = 70//60
            else if(color == "Green")
                hueColor = 39//30
            else if(color == "Yellow")
                hueColor = 25//16
            else if(color == "Orange")
                hueColor = 10
            else if(color == "Purple")
                hueColor = 75
            else if(color == "Pink")
                hueColor = 83
			def newValue = [hue: hueColor, saturation: 60, level: (lightLevel as Integer) ?: 100]
            log.debug "new value = $newValue"
            hues*.setColor(newValue)
        }
		state.lastStatus = "on"
	}
}

def turnOffMotionAfterDelay(){
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        log.trace "elapsed = $elapsed"
		if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
        	log.debug "Turning off lights"
			if(hues){
            	state.current = [:]
                hues.each {
                    state.current[it.id] = [
                        "switch": it.currentValue("switch"),
                        "level" : it.currentValue("level"),
                        "hue": it.currentValue("hue"),
                        "saturation": it.currentValue("saturation")
                    ]
                }
                hues.each {
                	log.trace "$lightLevel, current = $state.current[it.id].level"
                    if(state.current[it.id].level==(lightLevel as Integer)){
                        it.off()
                    }
                }
            }
            if(lights){
                lights.off()
            }
			state.lastStatus = "off"
		}
	}
}

def scheduleCheck() {
	log.debug "In scheduleCheck - skipping"
	//turnOffMotionAfterDelay()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}

private enabled() {
	def result
	if (lightSensor) {
		result = lightSensor.currentIlluminance < 30
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
    //true
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
