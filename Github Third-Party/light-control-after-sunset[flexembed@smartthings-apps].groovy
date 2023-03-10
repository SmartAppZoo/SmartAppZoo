/**
 *  Copyright 2017 Steven Fisher + SmartThings
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
 *  Turn Lights on / off after sunset and motion detected
 *
 *  Author: Steven Fisher + SmartThings
 */
definition(
    name: "Light control after sunset",
    namespace: "smartthings",
    author: "Steven Fisher",
    description: "Turn your lights on / off when motion is detected / stops after sunset.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png"
)

preferences {
	section("When there's movement...") {
		input "themotion", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("Turn on a light...") {
		input "theswitch", "capability.switch", multiple: true
        input "onTime", "number", title: "For minutes", defaultValue: "10", required: true  
	}
    	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", required: false
	}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	//unschedule handled in astroCheck method
	initialize()
}

def initialize() {

	subscribe(themotion, "motion.active", motionActiveHandler)

    subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)

	astroCheck()
}

def motionActiveHandler(evt) {
    log.debug "motionActiveHandler() $evt"
    
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
    def nowTime = now.time
    
    
    def riseTimeNext = riseTime.next()
    def setTimeNext = setTime.next()
    
    log.debug "onTime: $onTime"
    def int duration = 60*onTime
    
    log.debug "duration: $duration"
    
    if( now.after(setTime) || now.before(riseTime) ) {
        log.debug "Turn Light on"
        theswitch.on()
        runIn( 60*onTime, switchOffHandler )
    }
    else {
        log.debug "No need to turn on Light"    
    }
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def switchOffHandler() {
    theswitch.off()
}


def astroCheck() {
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
}

def sunriseHandler() {
	log.info "Executing sunrise handler"
}

def sunsetHandler() {
	log.info "Executing sunset handler"

}