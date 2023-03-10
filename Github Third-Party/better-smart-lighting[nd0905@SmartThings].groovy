/**
 *
 *  Copyright 2020 Nick Davidson 
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
 *	Current Features:
 *		Auto Turn on when arrival (after sunset)
 *		Auto Turn on when motion detected (after sunset)
 *		Auto Turn off after set time
 *		Auto Aware to leave lights on when set to on manually
 *		Force Switch to always turn off after being on for set time
 *
 *	Future Plans:
 *		Fix bug where motion detection dosent reset the turn off timer
 *
 */
definition(
    name: "better-smart-lighting",
    namespace: "nd0905",
    author: "Nick Davidson",
    description: "Turns on and off switches based on proximity sensors and timers",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Select Devices") {
		input "switches", "capability.switch", title: "Select Lights", multiple: true
        input "motion1", "capability.motionSensor", title: "Select Sensors", multiple: true, required: false
	}
	section("Turn it off in... ") {
		input "minutesLater", "number", title: "Minutes?"
	}
    section("When who arrives:") {
		input "people", "capability.presenceSensor", multiple: true, required: false
    }
    section("Always turn off after that long?") {
    	input "force", "BOOLEAN", title: "true = Force, false = Only on Trigger", required: true, defaultValue: false
    }
    section("Never Turn Off After Event?") {
    	input "neverTime", "BOOLEAN", title: "true = Turn Off After Time, false = Never Turn Off", required: true, defaultValue: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(people, "presence", arrivalHandler)
    subscribe(motion1, "motion", motionHandler)
    subscribe(switches, "switch", timedOff)
    state.status = null
}

def timedOff(evt) {
	log.debug "$evt.name: $evt.value"
	if( evt.value == "on" && force == "true") {
        for ( swich in switches ) {
        	if ( swich.currentValue("switch") == "off" ){
        		swich.on()
        		if ( neverTime == "true" ) { swich.off([delay: 1000 * 60 * minutesLater]) }
            }
        }
    }
}

def arrivalHandler(evt) {
	log.debug "$evt.name: $evt.value"
	def now = new Date()
	def sunTime = getSunriseAndSunset();
	if( evt.value == "not present" && everyoneIsAway()) {
		switches.off()
        unschedule()
        state.status = null
    } else if( evt.value == "present" && (now > sunTime.sunset) ) {
    	log.debug "turning on lights: current State is $state.status"
    	if ( state.status != "pending" ) { saveState() }
        switches.on()
        if ( neverTime == "true" ) { 
        	log.debug "running in $minutesLater"
        	runIn(60 * minutesLater, restoreState)
            state.status = "pending"
        }
	}
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"
	def now = new Date()
	def sunTime = getSunriseAndSunset();
	if (evt.value == "active" && (now > sunTime.sunset)) {
		log.debug "turning on lights: current State is $state.status"
		if (state.status != "pending" ) {saveState()}
        switches.on()  
	}
    else if (evt.value == "inactive" && (now > sunTime.sunset)) {
		if ( neverTime == "true" ) { 
        	runIn(60 * minutesLater, scheduleCheck) 
            state.status = "pending"
        }
    }
}

private everyoneIsAway() {
	log.debug "everyone is away"
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

private saveState()
{
	def hold = "cur"
	def map = state[hold] ?: [:]

	switches?.each {
		map[it.id] = [switch: it.currentSwitch, level: it.currentLevel]
	}

	state[hold] = map
	log.debug "saved state for ${hold}: ${state[hold]}"
	log.debug "state: $state"
}

def restoreState()
{
	def hold = "cur"
	def map = state[hold] ?: [:]
    log.debug "Restoring state: $state"
	switches?.each {
		def value = map[it.id]
		if (value?.switch == "on") {
			def level = value.level
			if (level) {
				log.debug "setting $it.label level to $level"
				it.setLevel(level)
			}
			else {
				log.debug "turning $it.label on"
				it.on()
			}
		}
		else if (value?.switch == "off") {
			log.debug "turning $it.label off"
			it.off()
		}
	}
    state.status = null
}

def scheduleCheck() {
	log.debug "schedule check"
	def motionState = motion1.currentState("motion")
    log.debug "current motion state is: $motionState.value"
    def elapsed = now() - motionState.rawDateCreated.time
   	def threshold = 1000 * 60 * minutesLater - 1000
    if (elapsed >= threshold) {
        log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning lights off"
        restoreState()
    } else {
    	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing resetting check"
        if (state.status == "pending" ) { runIn(60*minutesLater,scheduleCheck) }
    }
}