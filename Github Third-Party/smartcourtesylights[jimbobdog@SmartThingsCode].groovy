/**
 *  SmartCourtesyLights
 *
 *  Copyright 2015 James Simmonds
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
    name: "SmartCourtesyLights",
    namespace: "jimbobdog",
    author: "James Simmonds",
    description: "Smart Courtesy Lights, switch on if dark (sun is down) and off after N minutes (after last motion) unless the switch is already on (courtesy mode)",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("When motion from...") {
         input "sourceMotion", "capability.motionSensor", required: true, multiple: true
	}
    
    section("Main courtesy switch...") {
    	input "targetSwitch", "capability.switch", required: true
        input "targetSwitchOffAfterMinutes", "number", title: "Off after (minutes)?", defaultValue: "2"
    }
    
    section("Optional switches") {
    	input "optionalSwitches", "capability.switch", required: false, multiple: true
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
	init()
	subscribe(location, "sunrise", sunRiseHandler)
    subscribe(location, "sunset", sunSetHandler)
    subscribe(sourceMotion, "motion.active", motionActiveHandler)
    subscribe(sourceMotion, "motion.inactive", motionInActiveHandler)
}

def init() {
	state.appPrefix = "** SCL **: "
	state.sundown = getSunDownState();
    state.autoSwitchOffRequired = false;
}

def getSunDownState() {
    def ssToday = getSunriseAndSunset() 
    def now = new Date()
        
    def sundown = (now >= ssToday.sunset) || (now < ssToday.sunrise)
    log.debug "${state.appPrefix} sunrise: ${ssToday.sunrise}, sunset: ${ssToday.sunset}, sundown: ${sundown}"

    return sundown
}

def motionActiveHandler(evt) {		
	def currentSwitchState = targetSwitch.switchState.value
	log.debug "${state.appPrefix} motion detected! targetSwitch state: ${currentSwitchState}"
        
    unschedule()
    
    if (state.sundown && currentSwitchState == "off") {
    	switchAllOn()
    } else {
    	log.debug "${state.appPrefix} it's either daylight or the switch is already on, switch-off not required!"
    }    
}

def motionInActiveHandler(evt) {
	log.debug "${state.appPrefix} motion ceased; autoSwitchOffRequired=${state.autoSwitchOffRequired}"
    
	if (state.autoSwitchOffRequired) {
		log.debug "${state.appPrefix} scheduling switch off in ${targetSwitchOffAfterMinutes} minute(s)..."
    	runIn(targetSwitchOffAfterMinutes * 60, switchAllOff)
    }
}

def switchAllOn() {
	targetSwitch.on()
    
    if (optionalSwitches) {
    	optionalSwitches.on()
    }
    
    state.autoSwitchOffRequired = true;
    log.debug "${state.appPrefix} switched everything on!"
}

def switchAllOff() {
	targetSwitch.off()
    
    if (optionalSwitches) {
    	optionalSwitches.off()
    }
    
    state.autoSwitchOffRequired = false;
    log.debug "${state.appPrefix} switched everything off!"
}

def sunRiseHandler(evt) {
	state.sundown = false;
    log.debug "${state.appPrefix} sunrise! sundown=${state.sundown}"
}

def sunSetHandler(evt) {
	state.sundown = true;
    log.debug "${state.appPrefix} sunset! sundown=${state.sundown}"
}
