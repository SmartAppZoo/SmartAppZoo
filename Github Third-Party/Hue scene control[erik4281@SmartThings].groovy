/**
 *  Hue scene control
 *
 *  Copyright 2015 Erik Vennink
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
    name: "Hue scene control",
    namespace: "",
    author: "Erik Vennink",
    description: "Hue lights can be set (color & intensity) with motion sensors. Lights can be switched off after motion has stopped (optionally with delay). A brightness sensor can also be used to trigger the actions. ",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png")


preferences {
	section("Control these lights (set 1)") {
    	input "lights1", "capability.colorControl", title: "Which bulbs?", required:true, multiple:true
        input "color1", "enum", title: "Set this color?", required: false, multiple:false, options: [
            "On - Custom Color",
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel1", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
	}
	section("Control these lights (set 2)") {
    	input "lights2", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
        input "color2", "enum", title: "Set this color?", required: false, multiple:false, options: [
            "On - Custom Color",
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel2", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
	}
	section("Control these lights (set 3)") {
    	input "lights3", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
        input "color3", "enum", title: "Set this color?", required: false, multiple:false, options: [
            "On - Custom Color",
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel3", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
	}
	section("Control these lights (set 4)") {
    	input "lights4", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
        input "color4", "enum", title: "Set this color?", required: false, multiple:false, options: [
            "On - Custom Color",
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel4", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
	}
	section("Control these lights (set 5)") {
    	input "lights5", "capability.colorControl", title: "Which bulbs?", required:false, multiple:true
        input "color5", "enum", title: "Set this color?", required: false, multiple:false, options: [
            "On - Custom Color",
            "Soft White",
            "White",
            "Daylight",
            "Warm White",
            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
        input "lightLevel5", "enum", title: "And this light Level?", required: false, options: ["10","20","30","40","50","60","70","80","90","100"]
	}
    section("Choose Motion Sensor(s)...") {
		input "motionSensor", "capability.motionSensor", title: "Motion", multiple:true
	}
	section("Switch lights off when it's light or there's been no movement for..."){
		input "delayMinutes", "number", title: "Minutes?"
	}
	section("Using this light sensor (optional)"){
		input "lightSensor", "capability.illuminanceMeasurement", required: false
		input "lightValue", "number", title: "Switch at intensity (Lux)?", required: false
	}
    section("Only during certain modes"){
		input "modeSelect", "mode", title: "Mode", required: false, multiple:true
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
	//unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(app, appTouchHandler)
    subscribe(motionSensor, "motion", motionHandler)
	if (lightSensor) {
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
}

private darkCheck() {
	def darkResult
    state.darkCheck = null
	if (lightSensor) {
		darkResult = lightSensor.currentIlluminance + 20 < (lightValue ?: 100)
	}
	else {
		darkResult = "Always on (no brightness sensor)"
	}
    state.darkCheck = darkResult
    log.info "DarkCheck: $state.darkCheck"
}

private timeCheck() {
	def timingResult
    state.modeCheck = null
    state.timingCheck = null
    state.timeCheck = null
    if (modeSelect) {
        def modeCheck = modeSelect.find{it == location.mode}
        if (modeCheck) {
        	state.modeCheck = "1"
        }
        else {
        	state.modeCheck = null
        }
    }
    else {
        state.modeCheck = "1"
    }
    if (starting && ending) {
        def currTime = now()
        def start = timeToday(starting).time
       	def stop = timeToday(ending).time
        if (start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start) {
        	state.timingCheck = "1"
        }
        else {
        	state.timingCheck = null
        }
    }
    else {
        state.timingCheck = "1"
    }
    log.debug "modeCheck = $state.modeCheck"
    log.debug "timingCheck = $state.timingCheck"
    if (state.modeCheck && state.timingCheck) {
    	timingResult = "1"
    }
    state.timeCheck = timingResult
    log.info "Timingresult: $state.timeCheck"
}

def appTouchHandler(evt) {
	log.trace "evt: ${evt}"
	activateHue()
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
	if (motionValue) {
		state.motionStopTime = null
	}
	else {
    	state.motionStopTime = now()
    }
    if(delayMinutes) {
        runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
        log.info "Delay: $delayMinutes minutes"
	} 
	else {
		turnOffMotionAfterDelay()
	}
}

def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
    timeCheck()
	if (state.timeCheck) {
        if (lastStatus != "off" && evt.integerValue > (lightValue ?: 100)) {
                deactivateHue()
        }
        else if (state.motionStopTime) {
            if (lastStatus != "off") {
                def elapsed = now() - state.motionStopTime
                if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
                    deactivateHue()
                }
            }
        }
        else if (lastStatus != "on" && (evt.integerValue + 20) < (lightValue ?: 100)){
            activateHue()
        }
    }
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def current = motionSensor.currentValue("motion")
	def motionValue = motionSensor.find{it.currentMotion == "active"}
    darkCheck()
    timeCheck()
	if (state.timeCheck) {
        if (motionValue) {
            state.motionStopTime = null
            if (state.darkCheck) {
                activateHue()
            }
        }
        else {
            state.motionStopTime = now()
            if(delayMinutes) {
                runIn(delayMinutes*60, turnOffMotionAfterDelay, [overwrite: false])
                log.info "Delay: $delayMinutes minutes"
            } 
            else {
                turnOffMotionAfterDelay()
            }
        }
    }
    else {
        if (motionValue) {
            state.motionStopTime = null
        }
        else {
            state.motionStopTime = now()
            runIn(30*60, turnOffMotionAfterDelay, [overwrite: false])
            log.info "Delay: 30 minutes"
        }
    }
}

def turnOffMotionAfterDelay() {
	log.trace "In turnOffMotionAfterDelay, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
	if (state.motionStopTime && state.lastStatus != "off") {
		def elapsed = now() - state.motionStopTime
        if (elapsed >= ((delayMinutes ?: 0) * 60000L) - 2000) {
        	deactivateHue()
		}
	}
}

def activateHue() {
    if (lights1) {
        startHue(lights1, color1, lightLevel1)
    }
    if (lights2) {
        startHue(lights2, color2, lightLevel2)
    }
    if (lights3) {
        startHue(lights3, color3, lightLevel3)
    }
    if (lights4) {
        startHue(lights4, color4, lightLevel4)
    }
    if (lights5) {
        startHue(lights5, color5, lightLevel5)
    }
	state.lastStatus = "on"
}

def deactivateHue() {
    if (lights1) {
        stopHue(lights1)
    }
    if (lights2) {
        stopHue(lights2)
    }
    if (lights3) {
        stopHue(lights3)
    }
    if (lights4) {
        stopHue(lights4)
    }
    if (lights5) {
        stopHue(lights5)
    }
	state.lastStatus = "off"
}

def startHue(lightSelect, colorSelect, levelSelect) {
	log.trace "Activating Hue '$lightSelect', with color '$colorSelect' and level '$levelSelect'"

    def hueColor = 70
	def saturation = 100

	switch(colorSelect) {
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

	if (colorSelect != "On - Custom Color")
    {
        def newValue = [hue: hueColor, saturation: saturation, level: levelSelect as Integer ?: 100]
        lightSelect*.setColor(newValue)
		log.debug "new value = $newValue"
    }
    else
    {
    	lightSelect*.on()
    }
}

def stopHue(lightSelect) {
	log.trace "Deactivating Hue '$lightSelect'"

	lightSelect*.off()
}
