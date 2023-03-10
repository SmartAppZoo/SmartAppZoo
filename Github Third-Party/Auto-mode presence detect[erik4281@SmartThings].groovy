/**
 *  Presence auto-mode
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
 
 // UPDATE NEEDED: 
 // - Hello Home updates appear when the house-mode does not change (somebody arrives when another person is already there)
 
definition(
    name: "Auto-mode presence detect",
    namespace: "",
    author: "Erik Vennink",
    description: "Control the set mode based on presence sensors and sunrise/sunset.",
    category: "SmartThings Labs",
    iconUrl: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png",
    iconX3Url: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png")


preferences {
	section("When these presence sensors arrive or leave..."){
		input "presenceDetect", "capability.presenceSensor", title: "Which?", multiple: true, required: false
	}
	section("Wait this many minutes before changing to the requested away-mode...") {
		input "falseAlarmThreshold", "number", title: "Number of minutes", required: false
	}
	section ("When away...") {
		input "awayMode", "mode", title: "Change mode to?", required: true
		input "awayOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "awayOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
	}
	section ("When at home after sunrise...") {
		input "sunriseMode", "mode", title: "Change mode to?", required: true
		input "sunriseOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sunriseOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
		input "sunriseOffsetValue", "text", title: "Time offset: HH:MM (optional)", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After (optional)", required: false, options: ["Before","After"]
	}
	section ("When at home after sunset...") {
		input "sunsetMode", "mode", title: "Change mode to?", required: true
		input "sunsetOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sunsetOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
		input "sunsetOffsetValue", "text", title: "Time offset: HH:MM (optional)", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After (optional)", required: false, options: ["Before","After"]
	}
	section ("Don't change day/night when in this mode...") {
		input "manualMode", "mode", title: "Mode?", required: true
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
	subscribe(presenceDetect, "presence", presenceHandler)
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	runIn(60, astroCheck)
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	log.info "Starting astroCheck"
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"

	if (state.riseTime != riseTime.time) {
		unschedule("sunriseHandler")
		if(riseTime.before(now)) {
			riseTime = riseTime.next()
		}
		state.riseTime = riseTime.time
		log.info "scheduling sunrise handler for $riseTime"
		schedule(riseTime, sunriseHandler)
	}
	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")
	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }
		state.setTime = setTime.time
		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
	log.info "Finished astroCheck"
}

def presenceHandler(evt) {
	log.info "Starting presenceHandler"
	def current = presenceDetect.currentValue("presence")
	log.debug current
	def presenceValue = presenceDetect.find{it.currentPresence == "present"}
    state.presence = presenceValue
    log.debug "Presence set to: ${state.presence}"
    
	if(state.presence){
		def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
		def now = new Date()
        def riseTime = s.sunrise
		def setTime = s.sunset
		//sendNotificationEvent("Nest is set to 'Present'.")
        if (riseTime < now && setTime > now) {
            log.info "Setting to home day mode."
            sunriseHandler()
            }
		else {
            log.info "Setting to home night mode."
            sunsetHandler()
		}
	}
	else{
		def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
        log.info "Setting to away after the delay (${delay}s) has passed."
		//sendNotificationEvent("Nest is set to 'Away'.")
		runIn (delay, "awayHandler")
		//awayHandler()
	}
	log.info "Finished presenceHandler"
}

def sunriseHandler() {
	log.info "Starting sunriseHandler"
	def presenceValue = presenceDetect.find{it.currentPresence == "present"}
    state.presence = presenceValue
    if(state.presence){
		log.info "NOW Executing sunriseHandler."
		if (location.mode == manualMode) {
		}
		else {
            sendNotificationEvent("Home-mode set to '${sunriseMode}'.")
            changeMode(sunriseMode)
            if (sunriseOn) {
                sunriseOn.on()
            }
            if (sunriseOff) {
                sunriseOff.off()
            }
		}
	}
	else{
		log.info "NOW Not executing sunrise handler, because mode is away."
    }
	log.info "Finished sunriseHandler"
}

def sunsetHandler() {
	log.info "Starting sunsetHandler"
	def presenceValue = presenceDetect.find{it.currentPresence == "present"}
    state.presence = presenceValue
    if(state.presence){
		log.info "NOW Executing sunsetHandler."
		if (location.mode == manualMode) {
		}
		else {
            sendNotificationEvent("Home-mode set to '${sunsetMode}'.")
            changeMode(sunsetMode)
            if (sunsetOn) {
                sunsetOn.on()
            }
            if (sunsetOff) {
                sunsetOff.off()
            }
		}
	}
	else{
		log.info "NOW Not executing sunset handler, because mode is away."
	}
	log.info "Finished sunsetHandler"
}

def awayHandler() {
	log.info "Starting awayHandler"
	def presenceValue = presenceDetect.find{it.currentPresence == "present"}
    state.presence = presenceValue
    if(state.presence){
		log.info "NOW Executing away handler, but not started, because people are present."
	}
	else{
		log.info "NOW Executing away handler."
        sendNotificationEvent("Home-mode set to '${awayMode}'.")
        changeMode(awayMode)
		if (awayOn) {
			awayOn.on()
		}
		if (awayOff) {
        	awayOff.off()
		}
	}
	log.info "Finished awayHandler"
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
		}
		else {
		}
	}
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
