/**
 *  zoneMotionChild v 1.0 2015-09-27
 *
 *  Copyright 2015 Mike Maxwell
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
    name: "zoneMotionChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "Triggers Simulated Motion Sensor using multiple physical motion sensors.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Zone configuration") {
		input(
            	name		: "motionSensors"
                ,title		: "Motion sensors:"
                ,multiple	: true
                ,required	: true
                ,type		: "capability.motionSensor"
            )
            input(
            	name		: "simMotion"
                ,title		: "Virtual Motion Sensor for this zone:"
                ,multiple	: false
                ,required	: true
                ,type		: "device.simulatedMotionSensor"
            )
            input(
            	name		: "activateOnAll"
                ,title		: "Activate Zone when:"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: [0:"Any Sensor",1:"All Sensors"]
            )
            input(
            	name		: "activationWindow"
                ,title		: "Activation window time, seconds. (used for All Sensors Option):"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["1","2","3","4","5","6","7","8","9","10"]
            )
            input(
            	name		: "lights"
                ,title		: "Select switch for testing..."
                ,multiple	: false
                ,required	: false
                ,type		: "capability.switch"
            )
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
	subscribe(motionSensors, "motion", motionHandler)
    //state.activeDevices = motionSensors.size()
    //log.info "activeDevices:${state.activeDevices}"
}
def motionHandler(evt) {
	def state
    def states
    def motionOK = true
    def activateOnAll = settings.activateOnAll == "1"
    def window = settings.activationWindow.toInteger()
    def crnt = new Date()
    def wStart = new Date(crnt.time - (window * 2000))
    def stateMap = [:]
    def wSec
	//log.debug "activteOnAll: ${activateOnAll}"
	if (evt.value == "active") {
    	//log.debug "fired: ${evt.displayName}"
        if (activateOnAll) {
    		motionSensors.each{ m ->
        		stateMap << [(m.displayName):settings.activationWindow.toInteger() + 1]
            	states = m.statesSince("motion", wStart)
            	states.each{s ->
             		if (s.value == "active"){
                		wSec = (evt.date.getTime() - s.date.getTime()) / 1000
                		//log.info "${m.displayName} s:${wSec}"
                    	if (wSec < stateMap.(m.displayName)) {
                    		stateMap.(m.displayName) = wSec
                    	}
                	}
            	}
        	}
        	motionOK = stateMap.every{ s -> s.value < window }    
			if (motionOK) {
				activateZone()
			} else {
        		//log.warn "some sensors in motion"
            	stateMap.each{ s -> 
            		if (s.value < window) {
                		log.warn "${s.key} in motion..."
                	}
            	}
        	}
        } else {
    		activateZone()        
        }
	} else {
    	if (activateOnAll || allInactive()) {
			inactivateZone()
		}
	}
}
def inactivateZone(){
	if (simMotion.currentValue("motion") == "active") {
		log.info "Zone: ${simMotion.displayName} is inactive."
		if (lights) lights.off()
   		simMotion.inactive()
    }
}
def activateZone(){
	if (simMotion.currentValue("motion") == "inactive") {
		log.info "Zone: ${simMotion.displayName} is active."
   		if (lights) lights.on()
   		simMotion.active()
    }
}

def allInactive () {
	def state = true	 
    if (motionSensors*.currentValue("motion").contains("active")){
    	state = false 
    }
    //log.debug "allInactive: ${state}"
	return state
}

