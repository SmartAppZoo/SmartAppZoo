/**
 *  Mode presence detect
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

/************
 * Metadata *
 ************/

definition(
    name: "Mode presence detect",
    namespace: "evennink",
    author: "Erik Vennink",
    description: "Control the set mode based on presence sensors and sunrise/sunset.",
    category: "SmartThings Labs",
    iconUrl: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png",
    iconX3Url: "http://icons.iconarchive.com/icons/iconshock/super-vista-general/128/home-icon.png")

/**********
 * Setup  *
 **********/

preferences {
	section("When these presence sensors arrive or leave..."){
		input "people", "capability.presenceSensor", title: "Who?", multiple: true, required: false
	}
	section ("When away...") {
		input "awayMode", "mode", title: "Change mode to?", required: true
		input "awayOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "awayOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
        input "falseAlarmThreshold", "decimal", title: "Delay: Number of minutes (default 10 min)", required: false
	}
	section ("When at home after sunrise...") {
		input "sunriseMode", "mode", title: "Change mode to?", required: true
		input "sunriseOffsetValue", "number", title: "Time offset (minutes after)", required: false
		input "sunriseOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sunriseOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
	}
	section ("When at home after sunset...") {
		input "sunsetMode", "mode", title: "Change mode to?", required: true
		input "sunsetOffsetValue", "number", title: "Time offset (minutes before)", required: false
		input "sunsetOn", "capability.switch", title: "Turn on switches?", required: false, multiple: true
		input "sunsetOff", "capability.switch", title: "Turn off switches?", required: false, multiple: true
	}
	section ("Don't change day/night when in this mode...") {
		input "manualMode", "mode", title: "Mode?", required: false
	}
}

/*************************
 * Installation & update *
 *************************/

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
	subscribe(people, "presence", presenceHandler)
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", scheduleSunrise)
	subscribe(location, "sunsetTime", scheduleSunset)
	scheduleSunrise(location.currentValue("sunriseTime"))
	scheduleSunset(location.currentValue("sunsetTime"))
}

/******************
 * Event handlers *
 ******************/

def locationPositionChange(evt) {
	log.trace "locationChange()"
}

def scheduleSunrise(sunriseString) {
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)
    def sunriseOffset = (sunriseOffsetValue != null) ? sunriseOffsetValue * 60000 : 0
    def timeOffsetSunrise = new Date(sunriseTime.time + sunriseOffset)
    log.debug "Scheduling for: $timeOffsetSunrise (sunrise is $sunriseTime)"
    schedule(timeOffsetSunrise, sunriseHandler)
}

def scheduleSunset(sunsetString) {
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    def sunsetOffset = (sunsetOffsetValue != null) ? sunsetOffsetValue * 60000 : 0
    def timeOffsetSunset = new Date(sunsetTime.time - sunsetOffset)
    log.debug "Scheduling for: $timeOffsetSunset (sunset is $sunsetTime)"
    schedule(timeOffsetSunset, sunsetHandler)
}

def presenceHandler(evt) {
    log.debug "evt.name: $evt.value"
    def refTime = new Date()
	def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunriseTime"))
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunsetTime"))
    def sunriseOffset = (sunriseOffsetValue != null) ? sunriseOffsetValue * 60000 : 0
    def sunsetOffset = (sunsetOffsetValue != null) ? sunsetOffsetValue * 60000 : 0
    def timeOffsetSunrise = new Date(sunriseTime.time + sunriseOffset)
    def timeOffsetSunset = new Date(sunsetTime.time - sunsetOffset)
	
	if (everyoneIsAway()) {
        def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60
        log.info "Setting to away after the delay (${delay}s) has passed."
        runIn (delay, "awayHandler")
    }
    else if (location.mode == awayMode) {
        log.info "Set to present + give notification"
        state.quietNotify = null
        if (timeOffsetSunset < timeOffsetSunrise && refTime < timeOffsetSunset) {
            log.info "Setting to home day mode."
            sunriseHandler()
        }
		else {
            log.info "Setting to home night mode."
            sunsetHandler()
		}
        state.quietNotify = null
    }
    else {
        log.info "Set to present"
		state.quietNotify = true
		if (timeOffsetSunset < timeOffsetSunrise && refTime < timeOffsetSunset) {
            log.info "Setting to home day mode."
            sunriseHandler()
        }
		else {
            log.info "Setting to home night mode."
            sunsetHandler()
		}
        state.quietNotify = null
    }
}

def sunriseHandler() {
	if (everyoneIsAway()) {		
		log.info "NOW Not executing sunrise handler, because mode is away."
    }
    else {
    	log.info "NOW Executing sunriseHandler."
		if (location.mode == manualMode) {
		}
		else {
            if (state.quietNotify) {
            }
            else {
            	log.info "Now sending SUNRISE notification"
                sendNotificationEvent("Home-mode set to '${sunriseMode}'.")
            }
            changeMode(sunriseMode)
            if (sunriseOn) {
                sunriseOn.on()
            }
            if (sunriseOff) {
                sunriseOff.off()
            }
            state.quietNotify = null
		}
	}
}

def sunsetHandler() {
	if (everyoneIsAway()) {		
		log.info "NOW Not executing sunset handler, because mode is away."
	}
	else{
		log.info "NOW Executing sunsetHandler."
		if (location.mode == manualMode) {
		}
		else {
            if (state.quietNotify) {
            }
            else {
	            log.info "Now sending SUNSET notification"
                sendNotificationEvent("Home-mode set to '${sunsetMode}'.")
            }
            changeMode(sunsetMode)
            if (sunsetOn) {
                sunsetOn.on()
            }
            if (sunsetOff) {
                sunsetOff.off()
            }
            state.quietNotify = null
		}
	}
}

def awayHandler() {
	if (everyoneIsAway()) {		
		log.info "NOW Executing away handler."
        log.info "Now sending AWAY notification"
        sendNotificationEvent("Home-mode set to '${awayMode}'.")
        changeMode(awayMode)
		if (awayOn) {
			awayOn.on()
		}
		if (awayOff) {
        	awayOff.off()
		}
	}
	else{
		log.info "NOW Executing away handler, but not started, because people are present."
	}
}

/******************
 * Helper methods *
 ******************/

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
		}
		else {
		}
	}
}

private everyoneIsAway() {
    def result = true
    for (person in people) {
        if (person.currentPresence == "present") {
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
}
