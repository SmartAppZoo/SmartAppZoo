/**
 *  Virtual Motion Sensor with 2 conditional switches - Ver 1.0
 *  Copyright 2016 Steve Jackson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *  
 */

definition(
    name: "Virtual Motion Sensor with 2 conditional switches",
    namespace: "Steve Jackson",
    author: "Steve Jackson",
    description: "Turns Simulated Motion Sensor on & off when motion is detected and conditions are met.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Real Motion Sensor to Follow?"){
		input "motion1", "capability.motionSensor", multiple: false
	}
	section("Do Not Disturb Switch#1 (MUST BE ON to disable)?"){
		input "dndswitch1", "capability.switch", multiple: false
	}	
    section("Do Not Disturb Switch#2 (MUST BE ON to disable)?"){
		input "dndswitch2", "capability.switch", multiple: false
	}	
    section("Virtual Motion Sensor to Trigger?"){
		input "virtualmotion", "capability.motionSensor", multiple: false
	}
}


def installed() {
	subscribe(motion1, "motion", motionHandler)
    subscribe(dndswitch1, "switch", switchHandler)
    subscribe(dndswitch2, "switch", switchHandler)
}

def updated() {
	unsubscribe()
	subscribe(motion1, "motion", motionHandler)
    subscribe(dndswitch1, "switch", switchHandler)
    subscribe(dndswitch2, "switch", switchHandler)
}

def motionHandler(evt) { 
	def dndswstate1 = dndswitch1.currentSwitch
    def dndswstate2 = dndswitch2.currentSwitch
    def motionstate = motion1.currentMotion
    log.debug "Current Do Not Disturb Switch State: $dndswstate1 and $dndswstate2"
    log.debug "$evt.name: $evt.value"
    if (dndswstate1 == "off") {
    	if (dndswstate2 == "off") {
			if (motionstate == "active")  {
        		virtualmotion.active()
        		log.debug "Changing State of Simulated Motion Sensor to active, DND switch is OFF"
    }		else if(motionstate == "inactive") {
    			virtualmotion.inactive()
            	log.debug "Changing State of Simulated Motion Sensor to inactive, DND switch is OFF"
    }
}	
}}

def switchHandler(evt) {  
	def dndswstate1 = dndswitch1.currentSwitch
    def dndswstate2 = dndswitch2.currentSwitch
    def motionstate = motion1.currentMotion
    log.debug "Current Do Not Disturb Switch State: $dndswstate1 and $dndswstate2"
    log.debug "$evt.name: $evt.value"
    if (dndswstate1 == "off") {
    	if (dndswstate2 == "off") {
			if (motionstate == "active")  {
        		virtualmotion.active()
        		log.debug "Changing State of Simulated Motion Sensor to active, DND switch is OFF"
    }		else if(motionstate == "inactive") {
    			virtualmotion.inactive()
            	log.debug "Changing State of Simulated Motion Sensor to inactive, DND switch is OFF"
    }
 }	
}}
