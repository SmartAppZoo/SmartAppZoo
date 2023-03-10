/**
 *  Hue Motion & Light Activated
 *
 *  Copyright 2015-02-11 Erik Vennink (Based on Bedroom Night Lights by Craig Lyons & Smart Nightlight by SmartThings)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Hue Motion & Light Activated",
    namespace: "",
    author: "Erik Vennink",
    description: "Hue lights can be set (color/intensity) with motion sensors. Also lights can be switched off after the motion has stopped. Optionally a brightness sensor can be used to switch the lights.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
)

preferences {
	section("Control these lights (1)...") {
			input "lights1", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
		}
	section("With these light effects (1)...")
			{
				input "color1", "enum", title: "Hue Color?", required: false, multiple:false, options: [
					"On - Custom Color",
                    "Soft White",
					"White",
					"Daylight",
					"Warm White",
					"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
				input "lightLevel1", "enum", title: "Light Level?", required: true, options: ["10","20","30","40","50","60","70","80","90","100"]
			}

	section("Also control these lights (2)... (optional)") {
			input "lights2", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
		}
	section("With light effects (2)...")
			{
				input "color2", "enum", title: "Hue Color?", required: false, multiple:false, options: [
					"On - Custom Color",
                    "Soft White",
					"White",
					"Daylight",
					"Warm White",
					"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
				input "lightLevel2", "enum", title: "Light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
			}

	section("Also control these lights (3)... (optional)") {
			input "lights3", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
		}
	section("With light effects (3)...")
			{
				input "color3", "enum", title: "Hue Color?", required: false, multiple:false, options: [
					"On - Custom Color",
                    "Soft White",
					"White",
					"Daylight",
					"Warm White",
					"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
				input "lightLevel3", "enum", title: "Light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
			}

    section("Choose Motion Sensor(s)...") {
		input "motionSensor", "capability.motionSensor", title: "Motion", multiple:true
	}

	section("Switch lights off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}

	section("Using this light sensor (optional)"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
	}
	section("Using this light level as trigger (default is 50)"){
		input "lightValue", "number", title: "Lux?", required: false
	}

	section("Only during a certain time"){
		input "starting", "time", title: "Starting", required: false
        input "ending", "time", title: "Ending", required: false
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	//unschedule()
    initialize()
}

def initialize() {
	subscribe(app, appTouchHandler)
    subscribe(motionSensor, "motion", motionHandler)
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
}

def motionHandler(evt) {
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		log.info "$currTime"
        log.info "$start"
        log.info "$stop"

		log.info "Start & Stop time set"

		if (start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start) {
			log.info "Within time-window!"
            motionHandlerTimed(evt)
		}
	}

    else {
 		log.info "No timing limits"
        motionHandlerTimed(evt)
	}
}

def motionHandlerTimed(evt) {
	log.debug "$evt.name: $evt.value"
	def current = motionSensor.currentValue("motion")
	log.debug current
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	log.info motionValue
    if (motionValue) {
		if (enabled()) {
			log.debug "turning on lights due to motion"
			activateHue()
            log.info "Activated Hue with motion"
        	//state.lastStatus = "on"
		}
		state.motionStopTime = null
	}
	else {
		state.motionStopTime = now()
		if(delayMinutes) {
            runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
		} else {
			turnOffMotionAfterDelay()
		}
	}
}

def illuminanceHandler(evt) {
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		log.info "$currTime"
        log.info "$start"
        log.info "$stop"

		log.info "Start & Stop time set"

		if (start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start) {
			log.info "Within time-window!"
            illuminanceHandlerTimed(evt)
		}
	}

    else {
 		log.info "No timing limits"
        illuminanceHandlerTimed(evt)
	}
}

def illuminanceHandlerTimed(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
    if (lastStatus != "off" && evt.integerValue > (lightValue ?: 50)) {
		lights1*.off()
		lights2*.off()
		lights3*.off()
		//lights1*.off(delay: 60000)
		//lights2*.off(delay: 60000)
		//lights3*.off(delay: 60000)
		log.info "Switched off Hue because it is too light"
        state.lastStatus = "off"
	}
	else if (state.motionStopTime) {
		if (lastStatus != "off") {
			def elapsed = now() - state.motionStopTime
			if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
				lights1*.off()
				lights2*.off()
				lights3*.off()
				//lights1*.off(delay: 60000)
				//lights2*.off(delay: 60000)
				//lights3*.off(delay: 60000)
                log.info "Switched off Hue because motion stopped"
                state.lastStatus = "off"
			}
		}
	}
	else if (lastStatus != "on" && (evt.integerValue + 20) < (lightValue ?: 50)){
		activateHue()
        log.info "Activated Hue because of motion and darkness"
        //state.lastStatus = "on"
	}
}

def turnOffMotionAfterDelay() {
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        log.trace "elapsed = $elapsed"
		if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
        	log.debug "Turning off lights"
			lights1*.off()
			lights2*.off()
			lights3*.off()
            //lights1*.off(delay: 60000)
            //lights2*.off(delay: 60000)
            //lights3*.off(delay: 60000)
            state.lastStatus = "off"
		}
	}
}

private enabled() {
	def result
	if (lightSensor) {
		result = lightSensor.currentIlluminance + 20 < (lightValue ?: 50)
	}
	else {
		def t = now()
		result = t < state.riseTime || t > state.setTime
	}
	result
}

def activateHue() {
    if (lights1) {
        activateHue1()
    }
    if (lights2) {
        activateHue2()
    }
    if (lights3) {
        activateHue3()
    }
	state.lastStatus = "on"
}

def activateHue1() {
	log.trace "activateHue1"

    def hueColor = 70
	def saturation = 100

	switch(color1) {
			case "White":
				hueColor = 52
				saturation = 19
				break;
			case "Daylight":
				hueColor = 53
				saturation = 91
				break;
			case "Soft White":
				hueColor = 23
				saturation = 56
				break;
			case "Warm White":
				hueColor = 20
				saturation = 80 //83
				break;
	 	 	case "Blue":
				hueColor = 70
				break;
			case "Green":
				hueColor = 39
				break;
			case "Yellow":
				hueColor = 25
				break;
			case "Orange":
				hueColor = 10
				break;
			case "Purple":
				hueColor = 75
				break;
			case "Pink":
				hueColor = 83
				break;
			case "Red":
				hueColor = 100
				break;
	}

	if (color1 != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel1 as Integer ?: 100]
        lights1*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lights1*.on()
    }

}

def activateHue2() {
	log.trace "activateHue2"

    def hueColor = 70
	def saturation = 100

	switch(color2) {
			case "White":
				hueColor = 52
				saturation = 19
				break;
			case "Daylight":
				hueColor = 53
				saturation = 91
				break;
			case "Soft White":
				hueColor = 23
				saturation = 56
				break;
			case "Warm White":
				hueColor = 20
				saturation = 80 //83
				break;
	 	 	case "Blue":
				hueColor = 70
				break;
			case "Green":
				hueColor = 39
				break;
			case "Yellow":
				hueColor = 25
				break;
			case "Orange":
				hueColor = 10
				break;
			case "Purple":
				hueColor = 75
				break;
			case "Pink":
				hueColor = 83
				break;
			case "Red":
				hueColor = 100
				break;
	}

	if (color1 != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel2 as Integer ?: 100]
        lights2*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lights2*.on()
    }

}

def activateHue3() {
	log.trace "activateHue3"

    def hueColor = 70
	def saturation = 100

	switch(color3) {
			case "White":
				hueColor = 52
				saturation = 19
				break;
			case "Daylight":
				hueColor = 53
				saturation = 91
				break;
			case "Soft White":
				hueColor = 23
				saturation = 56
				break;
			case "Warm White":
				hueColor = 20
				saturation = 80 //83
				break;
	 	 	case "Blue":
				hueColor = 70
				break;
			case "Green":
				hueColor = 39
				break;
			case "Yellow":
				hueColor = 25
				break;
			case "Orange":
				hueColor = 10
				break;
			case "Purple":
				hueColor = 75
				break;
			case "Pink":
				hueColor = 83
				break;
			case "Red":
				hueColor = 100
				break;
	}

	if (color3 != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: lightLevel3 as Integer ?: 100]
        lights3*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lights3*.on()
    }

}

def appTouchHandler(evt) {
	log.trace "evt: ${evt}"
	activateHue()
}
