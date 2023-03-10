/**
 *  Smart Lights
 *
 *  Version: 1.2-multi
 *
 *  Copyright 2015 Rob Landry
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
	name: "Smart Lights",
	namespace: "roblandry",
	author: "Rob Landry",
	description: "Turn on/off lights with motion unless overridden.",
	category: "Convenience",
	iconUrl: 	"https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance.png",
	iconX2Url: 	"https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet-luminance@2x.png")


preferences {
	// the info section
	section("Info") {
		paragraph "Author:  Rob Landry"
		paragraph "Version: 1.2-multi"
		paragraph "Date:    2/14/2015"
	}

	// the devices section
	section("Devices") {
		input "motion", "capability.motionSensor", title: "Motion Sensor", multiple: false
		input "lights", "capability.switch", title: "Lights to turn on", multiple: true
	}

	// the preferences section
	section("Preferences") {
		paragraph "Motion sensor delay..."
		input "motionEnabled", "bool", title: "Enable/Disable Motion Control.", required: true, defaultValue: true
		input "delayMinutes", "number", title: "Minutes", required: false, defaultValue: 0
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
	subscribe(motion, "motion", motionHandler)
	subscribe(lights, "switch", switchHandler)

	// Establish Defaults for the lights
	state.thelights = [:]
	lights.each {
		state.thelights[it.id] = 
			["name" : it
			,"currentValue" : [ "switch" : it.currentValue("switch") ]
			,"initialValue" : [ "switch" : it.currentValue("switch") ]
			,"motionCommand" : false ]
	}
	/*lights.each {
		if (it.id == "0274a9fa-f287-4922-a10f-500fd28437f4") {
			it.off()
		}
	}
    
	state.currentLights = lights.currentValue("switch")
	state.lights = (lights.currentValue("switch")[0] == "on") ? true : false
	state.motionCommand = false*/

	// More defaults
	state.motionProgramActive = false
	state.motionStopTime = null
	log.debug "initialize: State: ${state}"
}

def motionHandler(evt) {
	log.debug "Motion Enabled: $motionEnabled"
	log.debug "Motion Handler - ${evt.name}: ${evt.value}, State: ${state}"

	// if motion is not enabled, what are we doing here, get out!
	if (!motionEnabled) {return}

	// if motion event is active
	if (evt.value == "active") {
		log.info "Motion Detected."

		// if the light is off, set the motionCommand and turn it on
		lights.each {
			if (state.thelights[it.id].currentValue.switch == "off") {
				state.thelights[it.id].motionCommand = true
				it.on()
			}
		}
	// if the motion event is inactive
	} else if (evt.value == "inactive") {
		log.info "Motion Ceased."

		// if the light is on, was initially off, and activated by motion,
		// allow the program to turn off
		lights.each {
			if ((state.thelights[it.id].currentValue.switch == "on") && 
				(state.thelights[it.id].initialValue.switch == "off") &&
				(state.thelights[it.id].motionCommand)) {
				state.motionProgramActive = true
			}
		}

		// if the program is active
		if (state.motionProgramActive) {
			state.motionStopTime = now()

			// if we are delaying the turnoff, then schedule it, otherwise do it!
			if(delayMinutes) {

				// This should replace any existing off schedule
				unschedule("turnOffMotionAfterDelay")
				runIn(delayMinutes*60, "turnOffMotionAfterDelay", [overwrite: false])
			} else {
				turnOffMotionAfterDelay()
			}
		}
	}
}

def switchHandler(evt) {
	log.debug "Switch Handler Start: ${evt.name}: ${evt.value}, State: ${state}"

	// if we were delaying the turnoff, unschedule it
	if (delayMinutes) { unschedule ("turnOffMotionAfterDelay") }

	// if we are turning off the light
	if (evt.value == "off") {
		state.motionProgramActive = false

		// update initial values and reset the motion command
		lights.each {
			if (state.thelights[it.id].motionCommand) { 
				log.info "Turning ${it} off, using MOTION." 
			} else {
				log.info "Turning ${it} off, using SWITCH." 
			}
			state.thelights[it.id].initialValue.switch =  it.currentValue("switch")
			state.thelights[it.id].motionCommand = false
		}

	// if we are turning it on, let us know
	} else if (evt.value == "on") {
		lights.each {
			if (state.thelights[it.id].motionCommand) { 
				log.info "Turning ${it} on, using MOTION." 
			} else {
				log.info "Turning ${it} on, using SWITCH." 
			}
		}
	}

	// reset the current value to actual state
	lights.each {
		state.thelights[it.id].currentValue.switch =  it.currentValue("switch")
	}

	/*state.currentLights = lights.currentValue("switch")
	state.lights = (lights.currentValue("switch")[0] == "on") ? true : false*/
	log.debug "Switch Handler End: ${evt.name}: ${evt.value}, State: ${state}"
}

def turnOffMotionAfterDelay() {
	log.debug "turnOffMotionAfterDelay: State: ${state}"

	// if there is a motion allowed, we are in an active program, and a time was specified...
	if (state.motionStopTime && state.motionProgramActive && motionEnabled) {

		// get the time elapsed
		def elapsed = now() - state.motionStopTime

		// if we are at the time to turn off...
		if (elapsed >= (delayMinutes ?: 0) * 60000L) {

			// if the light is on, and was initially off,
			// set the motion command to true, and turn off
			lights.each {
				if ((state.thelights[it.id].currentValue.switch == "on") && 
					(state.thelights[it.id].motionCommand) && 
					(state.thelights[it.id].initialValue.switch == "off")) {
					it.off()
				}
			}
		}
	}
}